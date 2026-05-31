from sqlalchemy import JSON, Boolean, Column, String

from patova.models.base import Base


class DeviceConfig(Base):
    __tablename__ = "device_configs"

    device_id = Column(String(256), primary_key=True)
    block_non_contacts = Column(Boolean, default=False, nullable=False)
    allowed_prefixes = Column(JSON, default=list, nullable=False)
    blocked_prefixes = Column(JSON, default=list, nullable=False)
