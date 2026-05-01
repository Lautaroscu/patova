# M04 - API de validacion, cache Redis y performance

## Objetivo del modulo

Implementar el endpoint principal del MVP:

```text
POST /v1/validate
```

Debe recibir un numero telefonico, normalizarlo, verificar prefijo, consultar Redis y PostgreSQL, calcular veredicto y responder con latencia objetivo menor a 200 ms en p95 bajo carga inicial.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("FastAPI")
context7.resolve-library-id("SQLAlchemy")
context7.resolve-library-id("Redis Python")
context7.resolve-library-id("Pydantic")
context7.resolve-library-id("SlowAPI")
context7.resolve-library-id("Locust")
```

Rutas sugeridas si existen:

```text
/tiangolo/fastapi
/sqlalchemy/sqlalchemy
/redis/redis-py
/pydantic/pydantic
/laurentS/slowapi
/locustio/locust
```

Topics sugeridos:

```text
FastAPI: dependency injection, request state, exception handlers
SQLAlchemy: async select, prepared queries, connection pooling
Redis: asyncio get/setex, JSON serialization
Pydantic: request/response models, enums
Locust: HTTP user, p95 metrics
```

## Endpoint requerido

### Request

```http
POST /v1/validate
X-NumGuard-Key: <api_key>
Content-Type: application/json
```

```json
{
  "number": "+5491112345678",
  "device_id": "sha256_hash_anonimo",
  "call_direction": "INCOMING",
  "timestamp": "2026-04-15T14:30:00Z"
}
```

### Response

```json
{
  "verdict": "BLOCK",
  "spam_score": 87,
  "reason": "HIGH_REPORT_VOLUME",
  "report_count": 143,
  "prefix_valid": true,
  "prefix_zone": "Buenos Aires - CABA",
  "operator": "Personal",
  "cached": false,
  "latency_ms": 18
}
```

## Valores de `verdict`

```text
ALLOW
SUSPECT
BLOCK
UNKNOWN
INVALID_PREFIX
```

## Reglas de decision MVP

Implementar una logica simple, transparente y testeable:

1. Si el numero no se puede normalizar a E.164: `INVALID_PREFIX` o `UNKNOWN` segun corresponda.
2. Si el prefijo no existe o esta marcado como invalido: `INVALID_PREFIX`.
3. Si existe `phone_numbers.status = SPAM` o `spam_score >= BLOCK_SCORE_MIN`: `BLOCK`.
4. Si `spam_score` entre `SUSPECT_SCORE_MIN` y `BLOCK_SCORE_MIN - 1`: `SUSPECT`.
5. Si `status = CLEAN` y score bajo: `ALLOW`.
6. Si no existe en base pero prefijo valido: `UNKNOWN`.

Variables de entorno:

```env
SUSPECT_SCORE_MIN=21
BLOCK_SCORE_MIN=61
VALIDATE_CACHE_ENABLED=true
RATE_LIMIT_PER_MINUTE=1000
RATE_LIMIT_PER_IP=60
```

Para beta temprana, permitir subir el umbral de bloqueo a 80 por configuracion sin cambiar codigo.

## TTL de cache

Usar Redis con key hasheada:

```text
numguard:validate:{sha256(number_e164)}
```

No guardar numero completo en la key.

TTL por veredicto:

```text
BLOCK: 3600 segundos
ALLOW: 86400 segundos
SUSPECT: 900 segundos
UNKNOWN: 300 segundos
INVALID_PREFIX: 86400 segundos
```

El response cacheado debe incluir `cached: true` y recalcular o preservar `latency_ms` de forma clara. Recomendado: al leer cache, setear `cached=true` y `latency_ms` del request actual.

## Endpoints adicionales en este modulo

### `GET /v1/number/{e164}`

Lookup publico sin API key. Respuesta reducida:

```json
{
  "number_e164": "+541112345678",
  "status": "CLEAN",
  "spam_score": 0,
  "report_count": 0,
  "prefix_zone": "Buenos Aires - CABA"
}
```

No exponer metadata sensible.

### `GET /v1/prefixes`

Lista de prefijos validos para cache local de Android:

```json
{
  "items": [
    {"prefix":"011","city":"Buenos Aires","province":"CABA","is_mobile":false}
  ],
  "count": 1
}
```

## Estructura a crear/modificar

```text
backend/src/numguard/api/v1/validation.py
backend/src/numguard/schemas/validation.py
backend/src/numguard/services/validation_service.py
backend/src/numguard/services/cache_service.py
backend/src/numguard/services/rate_limit.py
backend/tests/test_validate_endpoint.py
backend/tests/test_validation_service.py
backend/tests/test_cache_service.py
backend/tests/load/locustfile_validate.py
```

## Reglas de performance

- Medir `latency_ms` dentro del backend con `time.perf_counter()`.
- Usar una unica query principal por numero si es posible.
- Evitar N+1 en prefijo.
- Redis primero; DB solo en cache miss.
- No hacer llamadas externas en `/v1/validate`.
- No recalcular scores pesados en el request; eso va en worker.

## Tests requeridos

- API key obligatoria para `/v1/validate`.
- Numero clean devuelve `ALLOW`.
- Numero spam devuelve `BLOCK`.
- Score medio devuelve `SUSPECT`.
- Numero no existente con prefijo valido devuelve `UNKNOWN`.
- Prefijo invalido devuelve `INVALID_PREFIX`.
- Segunda consulta del mismo numero devuelve `cached=true`.
- Cache key no contiene numero crudo.
- `GET /v1/prefixes` devuelve prefijos validos.

## Prueba de carga inicial

Crear `backend/tests/load/locustfile_validate.py`.

Comando esperado:

```bash
locust -f tests/load/locustfile_validate.py --headless -u 50 -r 5 -t 1m --host http://localhost:8000
```

Objetivo local inicial:

- Sin red externa.
- p95 menor a 200 ms con dataset sample.
- Cero errores 5xx.

## Criterios de aceptacion

- `/v1/validate` implementado con contrato estable.
- Redis cache funciona y no expone numeros en keys.
- DB lookup funciona con seed sample.
- Tests pasan.
- Locust corre y reporta p95.
- No hay log de API key ni numeros completos salvo en modo debug explicitamente desactivado por defecto.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M04. Usa Context7 para FastAPI, SQLAlchemy async, redis-py, Pydantic y Locust. Implementa POST /v1/validate con cache Redis, decision service, schemas, tests y prueba de carga. No implementes reportes ni Android. Prioriza contrato estable, privacidad de cache key y latencia. Ejecuta pytest, ruff y locust headless si el entorno lo permite.
```
