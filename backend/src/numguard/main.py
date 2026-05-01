from pathlib import Path

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from .admin.routes import router as admin_router
from .api.router import router as api_router
from .core.config import get_settings
from .core.logging import setup_logging
from .core.metrics import metrics_app
from .services.rate_limit import setup_rate_limit

_ADMIN_STATIC = str(Path(__file__).resolve().parent / "admin" / "static")


def create_app() -> FastAPI:
    settings = get_settings()
    setup_logging(settings.log_level)

    app = FastAPI(title=settings.app_name, version=settings.api_version)
    app.include_router(api_router)
    setup_rate_limit(app)

    app.include_router(admin_router)
    app.mount(
        "/admin/static",
        StaticFiles(directory=_ADMIN_STATIC),
        name="admin_static",
    )
    app.mount("/metrics", metrics_app)

    return app
