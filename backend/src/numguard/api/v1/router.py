from fastapi import APIRouter, Depends

from ..deps import verify_api_key
from .feedback import router as feedback_router
from .health import router as health_router
from .reports import router as reports_router
from .stats import router as stats_router
from .validation import router as validation_router

router = APIRouter()
router.include_router(health_router)
router.include_router(validation_router)
router.include_router(reports_router)
router.include_router(feedback_router)
router.include_router(stats_router)


@router.get("/protected-ping")
def protected_ping(_api_key: str = Depends(verify_api_key)):
    return {"status": "ok"}
