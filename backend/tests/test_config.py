import pytest
from patova.core.config import Settings
from patova.workers.celery_app import get_celery_redis_url

pytestmark = pytest.mark.unit


def test_redis_url_normalization_standard():
    settings = Settings(redis_url="redis://localhost:6379/0")
    assert settings.redis_url == "redis://localhost:6379/0"
    assert get_celery_redis_url(settings.redis_url) == "redis://localhost:6379/0"


def test_redis_url_normalization_ssl_missing_params():
    settings = Settings(redis_url="rediss://localhost:6379/0")
    assert settings.redis_url == "rediss://localhost:6379/0?ssl_cert_reqs=none"
    assert get_celery_redis_url(settings.redis_url) == "rediss://localhost:6379/0?ssl_cert_reqs=CERT_NONE"


def test_redis_url_normalization_ssl_existing_params():
    # If the user passes uppercase, we normalize it to lowercase for redis-py
    settings = Settings(redis_url="rediss://localhost:6379/0?ssl_cert_reqs=CERT_REQUIRED")
    assert settings.redis_url == "rediss://localhost:6379/0?ssl_cert_reqs=required"
    assert get_celery_redis_url(settings.redis_url) == "rediss://localhost:6379/0?ssl_cert_reqs=CERT_REQUIRED"


def test_redis_url_normalization_ssl_other_params():
    settings = Settings(redis_url="rediss://localhost:6379/0?foo=bar")
    assert settings.redis_url == "rediss://localhost:6379/0?foo=bar&ssl_cert_reqs=none"
    assert get_celery_redis_url(settings.redis_url) == "rediss://localhost:6379/0?foo=bar&ssl_cert_reqs=CERT_NONE"
