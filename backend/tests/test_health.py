from fastapi.testclient import TestClient


def test_health_ok(client: TestClient):
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "numguard-api"
    assert data["version"] == "v1"


def test_v1_health_ok(client: TestClient):
    response = client.get("/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "numguard-api"
    assert data["version"] == "v1"
