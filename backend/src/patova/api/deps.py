from fastapi import Depends, HTTPException, Security
from fastapi.security import HTTPBasic, HTTPBasicCredentials

from patova.core.config import get_settings
from patova.core.security import API_KEY_HEADER

security = HTTPBasic(auto_error=False)


def verify_api_key(api_key: str = Security(API_KEY_HEADER)):
    """Valida la API key de la app móvil (X-Patova-Key)."""
    settings = get_settings()
    if not api_key:
        raise HTTPException(status_code=401, detail="X-Patova-Key header required")
    if api_key != settings.patova_api_key:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return api_key


def verify_admin_key(api_key: str = Security(API_KEY_HEADER)):
    """Valida la API key de administración (X-Patova-Key). Solo para endpoints admin."""
    settings = get_settings()
    if not api_key:
        raise HTTPException(status_code=401, detail="X-Patova-Key header required")
    if api_key != settings.patova_admin_key:
        raise HTTPException(status_code=401, detail="Invalid admin key")
    return api_key


def verify_admin_basic(credentials: HTTPBasicCredentials = Depends(security)):
    """Protege el panel HTML /admin con Basic Auth usando la admin key."""
    settings = get_settings()
    if not credentials:
        raise HTTPException(
            status_code=401,
            detail="Authentication required",
            headers={"WWW-Authenticate": "Basic"},
        )
    if credentials.username != "admin" or credentials.password != settings.patova_admin_key:
        raise HTTPException(
            status_code=401,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Basic"},
        )
    return credentials.username

