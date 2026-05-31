import os
import random

from locust import HttpUser, between, task

REPEATED_NUMBER = "+541112345678"
FRESH_PATTERN = "+5411%06d"


class ValidateUser(HttpUser):
    wait_time = between(0.05, 0.15)

    def on_start(self):
        self.api_key = os.getenv("PATOVA_API_KEY", "change-me-local-dev-key")

    @task(80)
    def validate_cached_number(self):
        """80%: Repeated number to measure cache hit."""
        self.client.post(
            "/v1/validate",
            json={
                "number": REPEATED_NUMBER,
                "device_id": "locust-cached-dev",
                "call_direction": "INCOMING",
            },
            headers={"X-Patova-Key": self.api_key, "Content-Type": "application/json"},
        )

    @task(15)
    def validate_non_cached_number(self):
        """15%: Fresh numbers that are not cached."""
        suffix = random.randint(100000, 999999)
        number = FRESH_PATTERN % suffix
        self.client.post(
            "/v1/validate",
            json={
                "number": number,
                "device_id": "locust-noncached-dev",
                "call_direction": "INCOMING",
            },
            headers={"X-Patova-Key": self.api_key, "Content-Type": "application/json"},
        )

    @task(5)
    def validate_invalid_prefix(self):
        """5%: Numbers with invalid prefix."""
        invalid_prefixes = [
            "+541552222222",
            "+541662222222",
            "+541772222222",
        ]
        number = random.choice(invalid_prefixes)
        self.client.post(
            "/v1/validate",
            json={
                "number": number,
                "device_id": "locust-invalid-dev",
                "call_direction": "INCOMING",
            },
            headers={"X-Patova-Key": self.api_key, "Content-Type": "application/json"},
        )
