# M05 - Crowdsourcing, reportes y worker de scoring

## Objetivo del modulo

Implementar el sistema de reportes de spam del MVP:

- `POST /v1/report`
- `POST /v1/feedback`
- Worker asincronico para recalcular `spam_score`
- Reglas anti-abuso basicas
- Invalidacion de cache Redis cuando cambia el score de un numero

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("FastAPI")
context7.resolve-library-id("Celery")
context7.resolve-library-id("Redis Python")
context7.resolve-library-id("SQLAlchemy")
context7.resolve-library-id("Pydantic")
```

Rutas sugeridas si existen:

```text
/tiangolo/fastapi
/celery/celery
/redis/redis-py
/sqlalchemy/sqlalchemy
/pydantic/pydantic
```

Topics sugeridos:

```text
Celery: Redis broker, task autodiscovery, retry, app factory
FastAPI: background tasks vs Celery, request validation
SQLAlchemy: transactions, row locking if needed
Redis: cache invalidation, rate counters
```

## Endpoint `POST /v1/report`

### Auth

Usar `X-NumGuard-Key` para MVP.

### Request

```json
{
  "number": "+5491112345678",
  "device_id": "sha256_hash_anonimo",
  "report_type": "SPAM_CALL",
  "description": "Llamada automatica de prestamo",
  "call_duration_sec": 0,
  "call_time": "2026-04-15T14:30:00Z"
}
```

### Response

```json
{
  "status": "accepted",
  "number_e164": "+5491112345678",
  "new_spam_score": 42,
  "report_count": 8
}
```

## Endpoint `POST /v1/feedback`

### Request

```json
{
  "number": "+5491112345678",
  "device_id": "sha256_hash_anonimo",
  "feedback_type": "FALSE_POSITIVE",
  "related_verdict": "BLOCK",
  "timestamp": "2026-04-15T14:30:00Z"
}
```

Valores de `feedback_type`:

```text
FALSE_POSITIVE
WAS_SPAM
CORRECT_BLOCK
CORRECT_ALLOW
```

Para simplificar el MVP, se puede guardar feedback como metadata/evento en una tabla nueva `feedback_events` o como tabla simple. Si se crea tabla nueva, agregar migracion.

## Reglas anti-abuso MVP

Implementar minimo:

1. Maximo 3 reportes por `device_id` por dia.
2. Deduplicacion: mismo `device_id` no puede reportar el mismo numero mas de una vez por 24 h.
3. Guardar `ip_hash` si se puede obtener IP, pero no guardar IP cruda.
4. Si supera limite, devolver `429` o `202` con mensaje claro. Recomendado: `429`.

Redis puede usarse para contadores diarios:

```text
numguard:report-limit:{date}:{device_id_hash}
numguard:report-dedupe:{date}:{device_id_hash}:{number_hash}
```

## Formula de scoring MVP

Implementar formula simple:

```text
report_weight =
  10 para reportes 1 a 3
  5 para reportes 4 a 10
  2 para reportes 11+

recency_factor =
  1.5 si hay mas de 10 reportes unicos en ultimas 24 h
  1.0 en caso normal

spam_score = min(100, sum(report_weight) * recency_factor)
```

Regla de auto-bloqueo:

```text
Si hay >= AUTO_BLOCK_THRESHOLD reportes unicos en 24 h, status = SPAM.
```

Variables:

```env
AUTO_BLOCK_THRESHOLD=50
MAX_REPORTS_PER_DEVICE_PER_DAY=3
```

## Worker asincronico

Configurar Celery con Redis como broker.

Tareas:

```text
recalculate_spam_score(phone_number_id)
invalidate_validate_cache(number_e164)
```

En el MVP, el endpoint puede calcular score sincronico para devolver respuesta inmediata y ademas encolar recalculo. Si complica demasiado, priorizar consistencia simple:

1. Insertar reporte.
2. Recalcular score en la misma transaccion o justo despues.
3. Invalidar cache.
4. Encolar worker solo para recalculos futuros/batch.

## Estructura a crear/modificar

```text
backend/src/numguard/api/v1/reports.py
backend/src/numguard/api/v1/feedback.py
backend/src/numguard/schemas/reports.py
backend/src/numguard/schemas/feedback.py
backend/src/numguard/services/report_service.py
backend/src/numguard/services/scoring_service.py
backend/src/numguard/services/abuse_guard.py
backend/src/numguard/workers/celery_app.py
backend/src/numguard/workers/tasks.py
backend/tests/test_reports_endpoint.py
backend/tests/test_scoring_service.py
backend/tests/test_abuse_guard.py
```

## Comandos de ejecucion local

API:

```bash
uvicorn numguard.main:create_app --factory --reload
```

Worker:

```bash
celery -A numguard.workers.celery_app worker --loglevel=INFO
```

## Tests requeridos

- Reportar numero nuevo crea `phone_number` si no existe y prefijo es valido.
- Reportar numero existente incrementa `report_count`.
- Score se calcula segun pesos.
- 50 reportes unicos en 24 h marcan `SPAM`.
- Reporte duplicado mismo device/numero en 24 h es rechazado.
- Mas de 3 reportes por device/dia es rechazado.
- Reporte invalida cache de `/v1/validate`.
- Feedback `FALSE_POSITIVE` reduce score o registra evento para revision.

## Criterios de aceptacion

- Endpoints de reportes y feedback funcionan.
- Scoring es deterministico y testeado.
- Anti-abuso minimo activo.
- Cache se invalida ante cambios.
- Worker Celery arranca localmente.
- No se guardan IPs crudas.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M05. Usa Context7 para FastAPI, Celery, Redis y SQLAlchemy. Crea endpoints /v1/report y /v1/feedback, scoring MVP, anti-abuso y worker Celery. No modifiques Android ni admin panel. Asegura invalidacion de cache y tests. Ejecuta pytest, ruff y prueba de arranque del worker si el entorno lo permite.
```
