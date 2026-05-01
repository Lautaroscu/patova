from fastapi.security import APIKeyHeader

API_KEY_HEADER = APIKeyHeader(name="X-NumGuard-Key", auto_error=False)
