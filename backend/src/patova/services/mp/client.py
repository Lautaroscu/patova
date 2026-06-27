import time

import httpx

from patova.core.config import get_settings


class MercadoPagoError(Exception):
    pass


class MercadoPagoClient:
    PLANS = [
        {
            "id": "premium_monthly",
            "title": "Plan Mensual",
            "subtitle": "Sin compromiso · Cancelá cuando quieras",
            "description": "Menos de lo que sale un café al paso ($1.000/mes) para liberarte del spam.",
            "price": 1000.0,
            "currency": "ARS",
            "formatted_price": "$1.000",
            "button_text": "Suscribirme · $1.000/mes",
            "interval": "month",
            "badge": None,
            "discount": None,
            "equivalent_monthly_price_text": None,
            "is_recommended": False,
            "mp_title": "Patova Premium Mensual",
        },
        {
            "id": "premium_annual",
            "title": "Plan Anual",
            "subtitle": "2 meses gratis · Mejor valor",
            "description": "Equivale a menos de lo que cuesta un alfajor por mes para tener paz mental todo el año.",
            "price": 9600.0,
            "currency": "ARS",
            "formatted_price": "$9.600",
            "button_text": "Suscribirme · $800/mes (anual)",
            "interval": "year",
            "badge": "RECOMENDADO · EL MÁS ELEGIDO",
            "discount": "AHORRÁ 34%",
            "equivalent_monthly_price_text": "equivale a $800/mes",
            "is_recommended": True,
            "mp_title": "Patova Premium Anual",
        }
    ]

    def __init__(self) -> None:
        settings = get_settings()
        self.access_token = settings.mp_access_token
        self.is_sandbox = settings.mp_sandbox
        self.base_url = "https://api.mercadopago.com"
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(
                base_url=self.base_url,
                headers={
                    "Authorization": f"Bearer {self.access_token}",
                    "Content-Type": "application/json",
                },
                timeout=15.0,
            )
        return self._client

    async def close(self) -> None:
        if self._client is not None:
            await self._client.aclose()
            self._client = None

    async def create_preference(
        self,
        plan_id: str,
        email: str,
        user_id: str,
    ) -> dict:
        plan = next((p for p in self.PLANS if p["id"] == plan_id), self.PLANS[0])

        external_ref = f"{user_id}_{int(time.time())}"
        notification_url = self._build_webhook_url()

        settings = get_settings()
        if settings.mp_webhook_base_url:
            base_redirect = f"{settings.mp_webhook_base_url.rstrip('/')}/v1/payments/redirect"
            back_urls = {
                "success": f"{base_redirect}?result=success",
                "failure": f"{base_redirect}?result=failure",
                "pending": f"{base_redirect}?result=pending",
            }
        else:
            back_urls = {
                "success": "patova://checkout/success",
                "failure": "patova://checkout/failure",
                "pending": "patova://checkout/pending",
            }

        payload = {
            "items": [
                {
                    "id": plan_id,
                    "title": plan["mp_title"],
                    "description": f"Suscripcion Patova Premium - {plan_id}",
                    "quantity": 1,
                    "currency_id": "ARS",
                    "unit_price": plan["price"],
                }
            ],
            "payer": {"email": email},
            "external_reference": external_ref,
            "notification_url": notification_url,
            "back_urls": back_urls,
            "auto_return": "all",
            "statement_descriptor": "Patova Premium",
        }

        client = await self._get_client()
        response = await client.post("/checkout/preferences", json=payload)

        if response.status_code not in (200, 201):
            raise MercadoPagoError(
                f"MP create_preference failed: {response.status_code} {response.text}"
            )

        data = response.json()
        init_point = data.get("sandbox_init_point", data["init_point"]) if self.is_sandbox else data["init_point"]
        return {
            "preference_id": data["id"],
            "init_point": init_point,
            "external_reference": external_ref,
        }

    async def get_payment(self, payment_id: str) -> dict:
        client = await self._get_client()
        response = await client.get(f"/v1/payments/{payment_id}")

        if response.status_code != 200:
            raise MercadoPagoError(
                f"MP get_payment failed: {response.status_code} {response.text}"
            )

        return response.json()

    async def search_payments(self, limit: int = 30) -> dict:
        client = await self._get_client()
        response = await client.get(
            "/v1/payments/search",
            params={
                "sort": "date_created",
                "criteria": "desc",
                "limit": limit
            }
        )
        if response.status_code != 200:
            raise MercadoPagoError(
                f"MP search_payments failed: {response.status_code} {response.text}"
            )
        return response.json()

    def _build_webhook_url(self) -> str:
        settings = get_settings()
        if settings.mp_webhook_base_url:
            return f"{settings.mp_webhook_base_url.rstrip('/')}/v1/payments/webhook/mp"
        return ""


_mp_client: MercadoPagoClient | None = None


def get_mp_client() -> MercadoPagoClient:
    global _mp_client
    if _mp_client is None:
        _mp_client = MercadoPagoClient()
    return _mp_client
