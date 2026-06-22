from datetime import UTC, datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.models.phone_number import PhoneNumber
from patova.models.report import Report
from patova.models.subscription import Subscription
from patova.services.mp.client import get_mp_client, MercadoPagoError


def mask_e164(number: str) -> str:
    if not number or not number.startswith("+"):
        return number
    digits = number[1:]
    if len(digits) <= 6:
        return number[:3] + "****"
    return number[:5] + "****" + number[-4:]


async def get_stats(session: AsyncSession) -> dict:
    today_start = datetime.now(UTC).replace(hour=0, minute=0, second=0, microsecond=0)

    total_numbers = await session.scalar(
        select(func.count(PhoneNumber.phone_number))
    )

    total_reports = await session.scalar(
        select(func.count(Report.id))
    )

    blocked_today = await session.scalar(
        select(func.count(PhoneNumber.phone_number)).where(
            PhoneNumber.status == "SPAM",
            PhoneNumber.updated_at >= today_start,
        )
    )

    total_imports = await session.scalar(
        select(func.sum(PhoneNumber.import_count))
    ) or 0

    top_stmt = (
        select(PhoneNumber)
        .order_by(PhoneNumber.report_count.desc())
        .limit(10)
    )
    top_result = await session.execute(top_stmt)
    top_numbers = top_result.scalars().all()

    top_reported = [
        {
            "number_e164": p.number_e164,
            "number_e164_masked": mask_e164(p.number_e164),
            "phone_number": p.phone_number,
            "spam_score": p.spam_score,
            "report_count": p.report_count,
            "status": p.status.value if p.status else "UNVERIFIED",
            "is_predicted": p.is_predicted,
            "source": p.source.value if p.source else "SEED",
        }
        for p in top_numbers
    ]

    top_concurrence_stmt = (
        select(PhoneNumber)
        .where(PhoneNumber.import_count > 0)
        .order_by(PhoneNumber.import_count.desc())
        .limit(10)
    )
    top_concurrence_result = await session.execute(top_concurrence_stmt)
    top_concurrence_numbers = top_concurrence_result.scalars().all()

    top_concurrence = [
        {
            "number_e164": p.number_e164,
            "number_e164_masked": mask_e164(p.number_e164),
            "phone_number": p.phone_number,
            "spam_score": p.spam_score,
            "import_count": p.import_count,
            "concurrency_percentage": round((p.import_count / total_imports) * 100, 2) if total_imports > 0 else 0.0,
            "status": p.status.value if p.status else "UNVERIFIED",
            "is_predicted": p.is_predicted,
            "source": p.source.value if p.source else "SEED",
        }
        for p in top_concurrence_numbers
    ]

    # 1. Status Distribution
    status_counts = await session.execute(
        select(PhoneNumber.status, func.count(PhoneNumber.phone_number))
        .group_by(PhoneNumber.status)
    )
    status_dist = {status.value if hasattr(status, "value") else str(status): count for status, count in status_counts.all()}

    # 2. Source Distribution
    source_counts = await session.execute(
        select(PhoneNumber.source, func.count(PhoneNumber.phone_number))
        .group_by(PhoneNumber.source)
    )
    source_dist = {source.value if hasattr(source, "value") else str(source): count for source, count in source_counts.all()}

    # 3. Report Type Distribution
    report_type_counts = await session.execute(
        select(Report.report_type, func.count(Report.id))
        .group_by(Report.report_type)
    )
    report_dist = {rt.value if hasattr(rt, "value") else str(rt): count for rt, count in report_type_counts.all()}

    # 4. Reports over the last 7 days
    from datetime import timedelta
    seven_days_ago = today_start - timedelta(days=7)
    reports_by_day = await session.execute(
        select(func.date(Report.created_at), func.count(Report.id))
        .where(Report.created_at >= seven_days_ago)
        .group_by(func.date(Report.created_at))
        .order_by(func.date(Report.created_at))
    )
    reports_history = {str(day): count for day, count in reports_by_day.all()}
    
    history_labels = []
    history_values = []
    for i in range(6, -1, -1):
        day = (today_start - timedelta(days=i)).date()
        day_str = day.strftime("%Y-%m-%d")
        label_str = day.strftime("%d/%m")
        count = reports_history.get(day_str) or 0
        history_labels.append(label_str)
        history_values.append(count)

    # 5. Billing Stats & Recent Payments
    active_subs_count = await session.scalar(
        select(func.count(Subscription.id)).where(Subscription.status == "ACTIVE")
    ) or 0

    revenue_counts_stmt = (
        select(Subscription.plan_id, func.count(Subscription.id))
        .where(Subscription.status.in_(["ACTIVE", "EXPIRED"]))
        .group_by(Subscription.plan_id)
    )
    revenue_counts_res = await session.execute(revenue_counts_stmt)
    revenue_counts = {plan_id: count for plan_id, count in revenue_counts_res.all()}
    
    monthly_count = revenue_counts.get("premium_monthly", 0)
    annual_count = revenue_counts.get("premium_annual", 0)
    total_revenue = (monthly_count * 1000.0) + (annual_count * 9600.0)

    # Monthly revenue (last 30 days)
    thirty_days_ago = today_start - timedelta(days=30)
    monthly_revenue_stmt = (
        select(Subscription.plan_id, func.count(Subscription.id))
        .where(
            Subscription.status.in_(["ACTIVE", "EXPIRED"]),
            Subscription.started_at >= thirty_days_ago
        )
        .group_by(Subscription.plan_id)
    )
    monthly_revenue_res = await session.execute(monthly_revenue_stmt)
    monthly_revenue_counts = {plan_id: count for plan_id, count in monthly_revenue_res.all()}
    
    monthly_m = monthly_revenue_counts.get("premium_monthly", 0)
    monthly_a = monthly_revenue_counts.get("premium_annual", 0)
    monthly_revenue = (monthly_m * 1000.0) + (monthly_a * 9600.0)

    # List of recent payments
    recent_payments_stmt = (
        select(Subscription)
        .where(Subscription.status.in_(["ACTIVE", "EXPIRED"]))
        .order_by(Subscription.started_at.desc())
        .limit(30)
    )
    recent_payments_res = await session.execute(recent_payments_stmt)
    recent_payments = [
        {
            "id": sub.id,
            "user_id": sub.user_id,
            "payment_id": sub.payment_id,
            "plan_id": sub.plan_id,
            "plan_name": "Premium Mensual" if sub.plan_id == "premium_monthly" else "Premium Anual",
            "amount": 1000.0 if sub.plan_id == "premium_monthly" else 9600.0,
            "status": sub.status,
            "started_at": sub.started_at.strftime("%Y-%m-%d %H:%M") if sub.started_at else "",
        }
        for sub in recent_payments_res.scalars().all()
    ]

    # Fetch real-time payments directly from MercadoPago API
    mp_payments = []
    try:
        mp = get_mp_client()
        if mp.access_token and "YOUR-PROD-MERCADO-PAGO" not in mp.access_token and "TEST-00000000" not in mp.access_token:
            mp_res = await mp.search_payments(limit=30)
            results = mp_res.get("results", [])
            for p in results:
                status = p.get("status", "").upper()
                amount = p.get("transaction_amount", 0.0)
                date_approved = p.get("date_approved") or p.get("date_created") or ""
                
                date_formatted = ""
                if date_approved:
                    try:
                        date_formatted = date_approved.split(".")[0].replace("T", " ")
                        if len(date_formatted) > 16:
                            date_formatted = date_formatted[:16]
                    except Exception:
                        date_formatted = date_approved
                
                ext_ref = p.get("external_reference") or ""
                user_id = ext_ref.rsplit("_", 1)[0] if "_" in ext_ref else ext_ref
                payer_email = p.get("payer", {}).get("email", "")
                payment_method = p.get("payment_method_id", "").upper()
                
                mp_payments.append({
                    "id": str(p.get("id")),
                    "user_id": user_id,
                    "email": payer_email,
                    "amount": amount,
                    "status": status,
                    "date": date_formatted,
                    "payment_method": payment_method
                })
    except Exception:
        pass

    # 6. Monotributo Alert Logic
    MONOTRIBUTO_LIMITS = {
        "A": 10277988.13,
        "B": 15058447.71,
        "C": 21113696.52,
        "D": 26212853.42,
        "E": 30833964.37,
        "F": 38642048.36,
        "G": 46211109.37,
        "H": 70113407.33,
        "I": 78479211.62,
        "J": 89872640.30,
        "K": 108357084.05,
    }
    
    from patova.core.config import get_settings
    settings = get_settings()
    cuit = settings.monotributo_cuit
    categoria = settings.monotributo_categoria.strip().upper() if settings.monotributo_categoria else ""

    twelve_months_ago = today_start - timedelta(days=365)
    yearly_rev_stmt = (
        select(Subscription.plan_id, func.count(Subscription.id))
        .where(
            Subscription.status.in_(["ACTIVE", "EXPIRED"]),
            Subscription.started_at >= twelve_months_ago
        )
        .group_by(Subscription.plan_id)
    )
    yearly_rev_res = await session.execute(yearly_rev_stmt)
    yearly_rev_counts = {plan_id: count for plan_id, count in yearly_rev_res.all()}
    yearly_m = yearly_rev_counts.get("premium_monthly", 0)
    yearly_a = yearly_rev_counts.get("premium_annual", 0)
    yearly_revenue = (yearly_m * 1000.0) + (yearly_a * 9600.0)

    limit_value = MONOTRIBUTO_LIMITS.get(categoria, 0.0)
    remaining_value = max(0.0, limit_value - yearly_revenue) if limit_value > 0 else 0.0
    percentage_used = round((yearly_revenue / limit_value) * 100, 2) if limit_value > 0 else 0.0

    return {
        "total_numbers": total_numbers or 0,
        "total_reports": total_reports or 0,
        "blocked_today": blocked_today or 0,
        "total_imports": total_imports,
        "top_reported": top_reported,
        "top_concurrence": top_concurrence,
        "status_dist": status_dist,
        "source_dist": source_dist,
        "report_dist": report_dist,
        "history_labels": history_labels,
        "history_values": history_values,
        "active_subs_count": active_subs_count,
        "total_revenue": total_revenue,
        "monthly_revenue": monthly_revenue,
        "recent_payments": recent_payments,
        "mp_payments": mp_payments,
        "monotributo_cuit": cuit,
        "monotributo_categoria": categoria,
        "monotributo_limite": limit_value,
        "monotributo_anual_facturado": yearly_revenue,
        "monotributo_restante": remaining_value,
        "monotributo_porcentaje": percentage_used,
    }
