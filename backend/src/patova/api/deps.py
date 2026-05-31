from fastapi import HTTPException, Security

from patova.core.config import get_settings
from patova.core.security import API_KEY_HEADER


def verify_api_key(api_key: str = Security(API_KEY_HEADER)):
    settings = get_settings()
    if not api_key:
        raise HTTPException(status_code=401, detail="X-Patova-Key header required")
    if api_key != settings.patova_api_key:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return api_key
