import time

import httpx

from numguard.core.config import get_settings


class MercadoPagoError(Exception):
    pass


class MercadoPagoClient:
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
        plan_prices = {
            "premium_monthly": {"title": "Patova Premium Mensual", "price": 1000.0},
            "premium_annual": {"title": "Patova Premium Anual", "price": 9600.0},
        }
        plan = plan_prices.get(plan_id, plan_prices["premium_monthly"])

        external_ref = f"{user_id}_{int(time.time())}"
        notification_url = self._build_webhook_url()

        payload = {
            "items": [
                {
                    "id": plan_id,
                    "title": plan["title"],
                    "description": f"Suscripcion Patova Premium - {plan_id}",
                    "quantity": 1,
                    "currency_id": "ARS",
                    "unit_price": plan["price"],
                }
            ],
            "payer": {"email": email},
            "external_reference": external_ref,
            "notification_url": notification_url,
            "back_urls": {
                "success": "patova://checkout/success",
                "failure": "patova://checkout/failure",
                "pending": "patova://checkout/pending",
            },
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
        return {
            "preference_id": data["id"],
            "init_point": data.get("sandbox_init_point", data["init_point"]),
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
