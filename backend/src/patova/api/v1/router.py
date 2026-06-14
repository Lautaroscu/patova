from fastapi import APIRouter, Depends

from ..deps import verify_api_key
from .admin import router as admin_v1_router
from .config import router as config_router
from .downloads import router as downloads_router
from .feedback import router as feedback_router
from .health import router as health_router
from .payments import router as payments_router
from .reports import router as reports_router
from .spam import router as spam_router
from .stats import router as stats_router
from .sync import router as sync_router
from .validation import router as validation_router

router = APIRouter()
router.include_router(health_router)
router.include_router(validation_router)
router.include_router(reports_router)
router.include_router(feedback_router)
router.include_router(stats_router)
router.include_router(sync_router)
router.include_router(spam_router, prefix="/spam", tags=["spam-intel"])
router.include_router(config_router, prefix="/config", tags=["config"])
router.include_router(payments_router)
router.include_router(admin_v1_router, prefix="/admin", tags=["admin"])
router.include_router(downloads_router, prefix="/downloads", tags=["downloads"])


@router.get("/protected-ping")
def protected_ping(_api_key: str = Depends(verify_api_key)):
    return {"status": "ok"}
