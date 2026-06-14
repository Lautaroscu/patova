import datetime
from sqlalchemy import BigInteger, DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class BlockedCallLog(Base):
    __tablename__ = "blocked_calls_log"

    # Clave secuencial para evitar fragmentación por inserciones masivas
    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)

    # Indexamos phone_number para que los JOINs de métricas vuelen
    phone_number: Mapped[int] = mapped_column(BigInteger, index=True, nullable=False)
    device_id: Mapped[str] = mapped_column(String(255), nullable=False)

    # server_default maneja la zona horaria directo en el motor de Postgres
    intercepted_at: Mapped[datetime.datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
