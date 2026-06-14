from functools import lru_cache

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_env: str = "local"
    app_name: str = "Patova API"
    api_version: str = "v1"
    patova_api_key: str = "change-me-local-dev-key"
    patova_admin_key: str = "change-me-local-admin-key"
    log_level: str = "INFO"
    database_url: str = "postgresql+asyncpg://patova:patova@localhost:5432/patova"
    redis_url: str = "redis://localhost:6379/0"
    cors_origins: str = "*"

    @field_validator("database_url", mode="before")
    @classmethod
    def normalize_database_url(cls, v: str) -> str:
        if not v:
            return v
        # Normalizar el protocolo para asegurar el uso del driver asíncrono asyncpg
        if v.startswith("postgres://"):
            v = v.replace("postgres://", "postgresql+asyncpg://", 1)
        elif v.startswith("postgresql://") and not v.startswith("postgresql+asyncpg://"):
            v = v.replace("postgresql://", "postgresql+asyncpg://", 1)
        
        # asyncpg requiere 'ssl=require' en vez de 'sslmode=require'
        if "sslmode=" in v:
            v = v.replace("sslmode=req", "ssl=require").replace("sslmode=require", "ssl=require")
        return v

    @field_validator("redis_url", mode="before")
    @classmethod
    def normalize_redis_url(cls, v: str) -> str:
        if not v:
            return v
        if v.startswith("rediss://"):
            # Para redis-py (utilizado por el cliente async de FastAPI), el parámetro
            # 'ssl_cert_reqs' debe ser en minúsculas: 'none', 'optional', o 'required'.
            # Si no está configurado, le agregamos 'ssl_cert_reqs=none' por defecto.
            # Si ya está en mayúsculas (por ejemplo, 'CERT_NONE'), lo normalizamos a minúsculas.
            if "ssl_cert_reqs" not in v:
                separator = "&" if "?" in v else "?"
                v = f"{v}{separator}ssl_cert_reqs=none"
            else:
                v = v.replace("ssl_cert_reqs=CERT_NONE", "ssl_cert_reqs=none")
                v = v.replace("ssl_cert_reqs=CERT_REQUIRED", "ssl_cert_reqs=required")
                v = v.replace("ssl_cert_reqs=CERT_OPTIONAL", "ssl_cert_reqs=optional")
        return v


    suspect_score_min: int = 21
    block_score_min: int = 61
    validate_cache_enabled: bool = True
    rate_limit_per_minute: int = 1000
    rate_limit_per_ip: int = 60

    auto_block_threshold: int = 50
    max_reports_per_device_per_day: int = 3

    spam_reputation_cache_ttl: int = 3600
    cache_invalidation_report_threshold: int = 5
    rate_limit_redis_window_sec: int = 60

    sentry_dsn: str = ""
    sentry_traces_sample_rate: float = 1.0

    mp_access_token: str = "TEST-0000000000000000-000000-00000000000000000000000000000000-000000000"
    mp_sandbox: bool = True
    mp_webhook_base_url: str = ""


@lru_cache
def get_settings() -> Settings:
    return Settings()
