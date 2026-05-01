from fastapi import FastAPI
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from slowapi.util import get_remote_address

from numguard.core.config import get_settings

_limiter_instance: Limiter | None = None


def get_limiter() -> Limiter:
    global _limiter_instance
    if _limiter_instance is None:
        settings = get_settings()
        _limiter_instance = Limiter(
            key_func=get_remote_address,
            default_limits=[f"{settings.rate_limit_per_ip}/minute"],
            application_limits=[f"{settings.rate_limit_per_minute}/minute"],
        )
    return _limiter_instance


def setup_rate_limit(app: FastAPI) -> None:
    limiter = get_limiter()
    app.state.limiter = limiter
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)
    app.add_middleware(SlowAPIMiddleware)
