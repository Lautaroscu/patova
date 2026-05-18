from fastapi import APIRouter, Depends

from ..deps import verify_api_key
from .config import router as config_router
from .feedback import router as feedback_router
from .health import router as health_router
from .reports import router as reports_router
from .spam import router as spam_router
from .stats import router as stats_router
from .validation import router as validation_router

router = APIRouter()
router.include_router(health_router)
router.include_router(validation_router)
router.include_router(reports_router)
router.include_router(feedback_router)
router.include_router(stats_router)
router.include_router(spam_router, prefix="/spam", tags=["spam-intel"])
router.include_router(config_router, prefix="/config", tags=["config"])


@router.get("/protected-ping")
def protected_ping(_api_key: str = Depends(verify_api_key)):
    return {"status": "ok"}
