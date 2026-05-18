from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_env: str = "local"
    app_name: str = "NumGuard API"
    api_version: str = "v1"
    numguard_api_key: str = "change-me-local-dev-key"
    log_level: str = "INFO"
    database_url: str = "postgresql+asyncpg://numguard:numguard@localhost:5432/numguard"
    redis_url: str = "redis://localhost:6379/0"

    suspect_score_min: int = 21
    block_score_min: int = 61
    validate_cache_enabled: bool = True
    rate_limit_per_minute: int = 1000
    rate_limit_per_ip: int = 60

    auto_block_threshold: int = 50
    max_reports_per_device_per_day: int = 3

    sentry_dsn: str = ""
    sentry_traces_sample_rate: float = 1.0

    mp_access_token: str = "TEST-0000000000000000-000000-00000000000000000000000000000000-000000000"
    mp_sandbox: bool = True
    mp_webhook_base_url: str = ""


@lru_cache
def get_settings() -> Settings:
    return Settings()
