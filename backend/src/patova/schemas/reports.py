from datetime import datetime

from pydantic import BaseModel, Field, field_validator

from patova.models.enums import ReportType


class ReportRequest(BaseModel):
    number: str = Field(..., min_length=3, max_length=30)
    device_id: str = Field(..., min_length=1, max_length=256)
    report_type: ReportType = ReportType.SPAM_CALL
    description: str | None = Field(None, max_length=500)
    call_duration_sec: int | None = None
    call_time: datetime | None = None


class ReportResponse(BaseModel):
    status: str
    number_e164: str
    new_spam_score: int
    report_count: int


class AndroidReportBatch(BaseModel):
    device_id: str = Field(..., description="ID único del dispositivo que reporta")
    numbers: list[int | str] = Field(..., description="Lista de números detectados (int o string E.164)")

    @field_validator("numbers", mode="before")
    @classmethod
    def coerce_to_int(cls, v):
        result = []
        for item in v:
            if isinstance(item, int):
                result.append(item)
            elif isinstance(item, str):
                # Normalizar: sacar +, espacios, guiones
                cleaned = item.strip().lstrip("+").replace("-", "").replace(" ", "")
                if not cleaned.isdigit():
                    raise ValueError(f"Número inválido: {item!r}")
                result.append(int(cleaned))
            else:
                raise ValueError(f"Tipo de número no soportado: {type(item)}")
        return result


class ManualSeedRequest(BaseModel):
    seed_number: str = Field(..., description="Número telefónico semilla para expandir el rango de 1000")


class CallKitDeltaResponse(BaseModel):
    added: list[int]
    removed: list[int]
