from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def v1_health_check():
    return {
        "status": "ok",
        "service": "patova-api",
        "version": "v1",
    }
