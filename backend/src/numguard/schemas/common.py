from pydantic import BaseModel


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


class PingResponse(BaseModel):
    status: str


class ErrorResponse(BaseModel):
    detail: str
