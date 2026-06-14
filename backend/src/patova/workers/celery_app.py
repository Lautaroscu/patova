from celery import Celery

from patova.core.config import get_settings

settings = get_settings()

def get_celery_redis_url(url: str) -> str:
    # Celery exige que el valor de 'ssl_cert_reqs' esté en MAYÚSCULAS ('CERT_NONE', 'CERT_REQUIRED', 'CERT_OPTIONAL').
    # Hacemos la transformación inversa solo para la configuración de Celery.
    if url.startswith("rediss://"):
        url = url.replace("ssl_cert_reqs=none", "ssl_cert_reqs=CERT_NONE")
        url = url.replace("ssl_cert_reqs=required", "ssl_cert_reqs=CERT_REQUIRED")
        url = url.replace("ssl_cert_reqs=optional", "ssl_cert_reqs=CERT_OPTIONAL")
    return url


app = Celery("patova")
app.conf.update(
    broker_url=get_celery_redis_url(str(settings.redis_url)),
    result_backend=get_celery_redis_url(str(settings.redis_url)),
    task_serializer="json",
    result_serializer="json",
    accept_content=["json"],
    timezone="UTC",
    enable_utc=True,
    task_always_eager=False,
    broker_connection_retry_on_startup=True,
)
app.autodiscover_tasks(["patova.workers.tasks"])
