import traceback
from pathlib import Path
from time import perf_counter

import structlog
from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.staticfiles import StaticFiles
from starlette.middleware.base import BaseHTTPMiddleware

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

logger = structlog.get_logger(__name__)
_base_path = Path(__file__).resolve().parent
_static_path = _base_path / "admin" / "static"
if not _static_path.exists():
    _docker_path = Path("/app/src/patova/admin/static")
    if _docker_path.exists():
        _static_path = _docker_path
    else:
        _local_path = Path.cwd() / "src" / "patova" / "admin" / "static"
        if _local_path.exists():
            _static_path = _local_path
_ADMIN_STATIC = str(_static_path)

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


class ExceptionHandlingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        try:
            return await call_next(request)
        except Exception:
            tb = traceback.format_exc()
            logger.exception(
                "unhandled_exception",
                method=request.method,
                path=request.url.path,
                client=request.client.host if request.client else None,
            )
            return JSONResponse(
                status_code=500,
                content={
                    "detail": "Internal server error",
                    "error_type": "UNHANDLED",
                    "path": request.url.path,
                    "method": request.method,
                    "traceback": tb.split("\n")[-4:-2] if tb else [],
                },
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
    
    app.add_middleware(ExceptionHandlingMiddleware)

    from fastapi.middleware.cors import CORSMiddleware
    origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.middleware("http")(_prometheus_middleware)


    @app.get("/", include_in_schema=False)
    def root_status():
        return {"status": "ok", "message": "Patova API"}

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
