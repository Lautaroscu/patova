from pathlib import Path

from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.api.deps import verify_api_key
from numguard.db.session import get_session
from numguard.services.stats_service import get_stats

router = APIRouter()

_TEMPLATES_DIR = str(Path(__file__).resolve().parent / "templates")
templates = Jinja2Templates(directory=_TEMPLATES_DIR)


@router.get("/admin", response_class=HTMLResponse)
async def admin_dashboard(
    request: Request,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    data = await get_stats(session)
    return templates.TemplateResponse(
        request=request,
        name="dashboard.html",
        context=data,
    )
