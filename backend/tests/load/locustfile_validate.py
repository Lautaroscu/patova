import os

from locust import HttpUser, between, task


class ValidateUser(HttpUser):
    wait_time = between(0.1, 0.5)

    def on_start(self):
        self.api_key = os.getenv("NUMGUARD_API_KEY", "change-me-local-dev-key")

    @task(5)
    def validate_clean(self):
        self.client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "locust_device_1",
                "call_direction": "INCOMING",
            },
            headers={"X-NumGuard-Key": self.api_key, "Content-Type": "application/json"},
        )

    @task(3)
    def validate_spam(self):
        self.client.post(
            "/v1/validate",
            json={
                "number": "+541199999999",
                "device_id": "locust_device_2",
                "call_direction": "INCOMING",
            },
            headers={"X-NumGuard-Key": self.api_key, "Content-Type": "application/json"},
        )

    @task(2)
    def validate_unknown(self):
        self.client.post(
            "/v1/validate",
            json={
                "number": "+541122222222",
                "device_id": "locust_device_3",
                "call_direction": "INCOMING",
            },
            headers={"X-NumGuard-Key": self.api_key, "Content-Type": "application/json"},
        )

    @task(2)
    def lookup_number(self):
        self.client.get("/v1/number/+541112345678")

    @task(1)
    def list_prefixes(self):
        self.client.get("/v1/prefixes")
