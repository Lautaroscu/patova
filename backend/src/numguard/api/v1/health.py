from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def v1_health_check():
    return {
        "status": "ok",
        "service": "numguard-api",
        "version": "v1",
    }
