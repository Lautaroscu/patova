from pydantic import BaseModel, Field

class DeviceConfigSchema(BaseModel):
    block_non_contacts: bool = False
    allowed_prefixes: list[str] = Field(default_factory=list)
    blocked_prefixes: list[str] = Field(default_factory=list)

class DeviceConfigRequest(DeviceConfigSchema):
    pass

class DeviceConfigResponse(DeviceConfigSchema):
    device_id: str
