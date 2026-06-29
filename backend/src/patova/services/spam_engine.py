from typing import AsyncGenerator, Dict, List, Set, Tuple

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.models.enums import NumberStatus
from patova.models.phone_number import PhoneNumber


class SpamEngineService:
    BLOCK_SIZE = 1000

    @staticmethod
    def get_block_bounds(phone_number: int) -> Tuple[int, int]:
        """
        Aplica aritmética entera para truncar al millar más cercano.
        Ej: 5491154386201 -> Start: 5491154386000, End: 5491154386999
        """
        start_range = (phone_number // SpamEngineService.BLOCK_SIZE) * SpamEngineService.BLOCK_SIZE
        end_range = start_range + (SpamEngineService.BLOCK_SIZE - 1)
        return start_range, end_range

    @classmethod
    def expand_number_to_block(cls, phone_number: int) -> Set[int]:
        """
        Toma un número entero, calcula el bloque VoIP y genera el conjunto
        secuencial completo (1000 números).
        """
        start_range, end_range = cls.get_block_bounds(phone_number)
        return set(range(start_range, end_range + 1))

    @classmethod
    def expand_batch(cls, raw_numbers: List[int]) -> List[Tuple[int, bool]]:
        """
        Toma una lista de semillas, calcula los bloques únicos en memoria
        y devuelve las tuplas listas para el bulk_insert.
        Asegura que is_predicted=False tenga prioridad sobre True para mantener
        el reporte real de usuario.
        """
        if not raw_numbers:
            return []

        # Encontramos todas las bases de bloques únicas en memoria
        unique_block_starts: Set[int] = set()
        for num in raw_numbers:
            start, _ = cls.get_block_bounds(num)
            unique_block_starts.add(start)

        # Generamos el universo de números expandidos deduplicados
        final_payload: Dict[int, bool] = {}

        # 1. Poblar las predicciones preventivas (True)
        for start_base in unique_block_starts:
            for generated_num in range(start_base, start_base + cls.BLOCK_SIZE):
                final_payload[generated_num] = True

        # 2. Forzar que las semillas reales (reportadas) queden como False (is_predicted=False)
        for seed in raw_numbers:
            final_payload[seed] = False

        return [(num, is_predicted) for num, is_predicted in final_payload.items()]

    @classmethod
    def process_batch_reporting(cls, numbers: List[int]) -> List[Tuple[int, bool]]:
        """
        Procesa los reportes masivos deduplicando y expandiendo en memoria.
        Llama internamente a expand_batch.
        """
        return cls.expand_batch(numbers)

    @staticmethod
    async def fetch_sorted_callkit_list(
        db: AsyncSession, batch_size: int = 50000
    ) -> AsyncGenerator[int, None]:
        """
        Generador asíncrono (async yield) que hace streaming de los números ordenados ascendentemente
        para evitar picos de memoria en el backend al armar el archivo para iOS.
        """
        # El ORDER BY ASC es ley para que CallKit indexe correctamente en iOS.
        # Filtramos solo los números con estado SPAM activo.
        stmt = (
            select(PhoneNumber.phone_number)
            .where(PhoneNumber.status == NumberStatus.SPAM)
            .order_by(PhoneNumber.phone_number.asc())
        )

        # Se ejecuta el stream con await para obtener el objeto ChunkedIteratorResult
        result = await db.stream(stmt)

        # Se itera de forma asíncrona fila por fila
        async for row in result:
            yield row[0]

    @classmethod
    async def prune_predicted_numbers(cls, db: AsyncSession) -> int:
        """
        Elimina registros predictivos antiguos (is_predicted = True) creados hace más de 6 meses
        que no tengan ningún log de intercepción en blocked_calls_log.
        """
        from datetime import datetime, timedelta, UTC
        from typing import cast
        from sqlalchemy import delete
        from sqlalchemy.engine import CursorResult
        from patova.models.blocked_call_log import BlockedCallLog

        limit_date = datetime.now(UTC) - timedelta(days=180)

        # Subconsulta para obtener los números que sí tienen intercepciones
        subq = select(BlockedCallLog.phone_number).distinct()

        stmt = delete(PhoneNumber).where(
            PhoneNumber.is_predicted == True,
            PhoneNumber.created_at < limit_date,
            ~PhoneNumber.phone_number.in_(subq)
        )
        res = cast(CursorResult, await db.execute(stmt))
        await db.commit()
        return res.rowcount

    @classmethod
    async def delete_block(cls, db: AsyncSession, phone_number: int) -> Tuple[int, int, int]:
        """
        Calcula el bloque VoIP para un número y elimina de la base de datos el número semilla
        y todos los registros predictivos asociados (is_predicted=True) en ese bloque de 1000.
        Retorna (filas_eliminadas, start_range, end_range).
        """
        from typing import cast
        from sqlalchemy import delete
        from sqlalchemy.engine import CursorResult

        start_range, end_range = cls.get_block_bounds(phone_number)

        conditions = (
            (PhoneNumber.phone_number == phone_number) |
            (
                (PhoneNumber.phone_number >= start_range) &
                (PhoneNumber.phone_number <= end_range) &
                (PhoneNumber.is_predicted == True)
            )
        )

        if str(phone_number).startswith("54"):
            try:
                legacy_num = int(str(phone_number)[2:])
                legacy_start, legacy_end = cls.get_block_bounds(legacy_num)
                conditions = conditions | (
                    (PhoneNumber.phone_number == legacy_num) |
                    (
                        (PhoneNumber.phone_number >= legacy_start) &
                        (PhoneNumber.phone_number <= legacy_end) &
                        (PhoneNumber.is_predicted == True)
                    )
                )
            except ValueError:
                pass

        stmt = delete(PhoneNumber).where(conditions)
        res = cast(CursorResult, await db.execute(stmt))
        await db.commit()
        return res.rowcount, start_range, end_range


