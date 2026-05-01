from fastapi.testclient import TestClient

from numguard.core.config import get_settings

VALID_KEY = get_settings().numguard_api_key


def test_protected_ping_no_key(client: TestClient):
    response = client.get("/v1/protected-ping")
    assert response.status_code == 401


def test_protected_ping_correct_key(client: TestClient):
    response = client.get("/v1/protected-ping", headers={"X-NumGuard-Key": VALID_KEY})
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_protected_ping_wrong_key(client: TestClient):
    response = client.get("/v1/protected-ping", headers={"X-NumGuard-Key": "wrong-key"})
    assert response.status_code == 401
