from celery import Celery

from numguard.core.config import get_settings

settings = get_settings()

app = Celery("numguard")
app.conf.update(
    broker_url=str(settings.redis_url),
    result_backend=str(settings.redis_url),
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    timezone="UTC",
    enable_utc=True,
    task_always_eager=False,
    broker_connection_retry_on_startup=True,
)
app.autodiscover_tasks(["numguard.workers.tasks"])
