from pathlib import Path

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.ext.asyncio import AsyncSession

from patova.api.deps import verify_admin_basic
from patova.core.config import get_settings
from patova.db.session import get_session
from patova.services.stats_service import get_stats

router = APIRouter()

_TEMPLATES_DIR = str(Path(__file__).resolve().parent / "templates")
templates = Jinja2Templates(directory=_TEMPLATES_DIR)


@router.get("/admin", response_class=HTMLResponse)
async def admin_dashboard(
    request: Request,
    session: AsyncSession = Depends(get_session),
    _admin: str = Depends(verify_admin_basic),
):

    data = await get_stats(session)
    data["api_key"] = get_settings().patova_admin_key
    return templates.TemplateResponse(
        request=request,
        name="dashboard.html",
        context=data,
    )
