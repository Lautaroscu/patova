import pytest
from httpx import ASGITransport, AsyncClient
from patova.main import create_app
from patova.core.config import get_settings

pytestmark = pytest.mark.unit

async def test_cors_origins():
    # Set settings to allow a specific origin
    settings = get_settings()
    original_cors = settings.cors_origins
    settings.cors_origins = "https://patova.serra.agency,http://localhost:3000"
    
    try:
        app = create_app()
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            # 1. Preflight request from allowed origin
            # We must pass Access-Control-Request-Method and Access-Control-Request-Headers for preflights
            response = await ac.options(
                "/v1/report/public",
                headers={
                    "Origin": "https://patova.serra.agency",
                    "Access-Control-Request-Method": "POST",
                    "Access-Control-Request-Headers": "content-type",
                }
            )
            assert response.status_code == 200
            assert response.headers.get("access-control-allow-origin") == "https://patova.serra.agency"
            
            # 2. Preflight request from disallowed origin
            response = await ac.options(
                "/v1/report/public",
                headers={
                    "Origin": "https://disallowed.com",
                    "Access-Control-Request-Method": "POST",
                    "Access-Control-Request-Headers": "content-type",
                }
            )
            # CORSMiddleware returns 400 for disallowed preflight origins
            assert response.status_code == 400
            
            # 3. Simple request (or actual POST request) from allowed origin
            response = await ac.post(
                "/v1/report/public",
                json={},
                headers={
                    "Origin": "https://patova.serra.agency",
                }
            )
            assert response.headers.get("access-control-allow-origin") == "https://patova.serra.agency"

    finally:
        # Restore original settings
        settings.cors_origins = original_cors

async def test_cors_on_error_response():
    settings = get_settings()
    original_cors = settings.cors_origins
    settings.cors_origins = "https://patova.serra.agency"
    
    try:
        app = create_app()
        
        # Add a route that raises an exception
        @app.get("/error-test")
        def raise_error():
            raise RuntimeError("Something went wrong")
            
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as ac:
            response = await ac.get(
                "/error-test",
                headers={
                    "Origin": "https://patova.serra.agency",
                }
            )
            assert response.status_code == 500
            # Let's see if CORS headers are returned on error responses
            assert response.headers.get("access-control-allow-origin") == "https://patova.serra.agency"
            
    finally:
        settings.cors_origins = original_cors
