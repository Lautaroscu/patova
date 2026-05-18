from pathlib import Path
from time import perf_counter

from fastapi import FastAPI, Request, Response
from fastapi.staticfiles import StaticFiles
from .admin.routes import router as admin_router
from .api.router import router as api_router
from .core.config import get_settings
from .core.logging import setup_logging
from .core.metrics import (
    app_info,
    http_request_duration_seconds,
    http_requests_in_flight,
    http_requests_total,
    metrics_app,
)

_ADMIN_STATIC = str(Path(__file__).resolve().parent / "admin" / "static")

_PROM_ROUTES = frozenset({"/metrics", "/admin/metrics"})


def _setup_sentry(settings) -> None:
    if not settings.sentry_dsn:
        return
    import sentry_sdk

    sentry_sdk.init(
        dsn=settings.sentry_dsn,
        traces_sample_rate=settings.sentry_traces_sample_rate,
        environment=settings.app_env,
    )


async def _prometheus_middleware(request: Request, call_next):
    if request.url.path in _PROM_ROUTES:
        return await call_next(request)

    method = request.method
    http_requests_in_flight.labels(method=method).inc()

    t_start = perf_counter()
    response: Response = await call_next(request)
    duration = perf_counter() - t_start

    http_requests_in_flight.labels(method=method).dec()
    http_request_duration_seconds.labels(method=method, endpoint=request.url.path).observe(duration)
    http_requests_total.labels(
        method=method,
        endpoint=request.url.path,
        status_code=str(response.status_code),
    ).inc()

    return response


def create_app() -> FastAPI:
    settings = get_settings()
    setup_logging(settings.log_level)
    _setup_sentry(settings)

    app = FastAPI(title=settings.app_name, version=settings.api_version)

    app.middleware("http")(_prometheus_middleware)

    app.include_router(api_router)

    from .services.rate_limit import setup_rate_limit

    setup_rate_limit(app)

    app.include_router(admin_router)
    app.mount(
        "/admin/static",
        StaticFiles(directory=_ADMIN_STATIC),
        name="admin_static",
    )
    app.mount("/metrics", metrics_app)

    app_info.info({"version": settings.api_version, "env": settings.app_env})

    return app
