# M02 - Base de datos PostgreSQL, Redis y migraciones Alembic

## Objetivo del modulo

Agregar persistencia y cache al backend:

- PostgreSQL 16 local por Docker Compose.
- Redis 7 local por Docker Compose.
- SQLAlchemy 2.x async.
- Alembic para migraciones.
- Modelos iniciales: `phone_numbers`, `area_prefixes`, `reports`.
- Indices criticos para lookup de numeros y reportes.

No implementar todavia el endpoint real `/v1/validate`. Este modulo deja la infraestructura de datos lista.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("SQLAlchemy")
context7.resolve-library-id("Alembic")
context7.resolve-library-id("asyncpg")
context7.resolve-library-id("Redis Python")
context7.resolve-library-id("Docker Compose")
context7.resolve-library-id("Testcontainers Python")
```

Rutas sugeridas si existen:

```text
/sqlalchemy/sqlalchemy
/sqlalchemy/alembic
/MagicStack/asyncpg
/redis/redis-py
/docker/compose
/testcontainers/testcontainers-python
```

Topics sugeridos:

```text
SQLAlchemy: async ORM, declarative models, sessionmaker
Alembic: async migrations, env.py configuration
Redis Python: asyncio client
Docker Compose: service healthchecks, env vars
```

## Dependencias a agregar

En `backend/pyproject.toml`:

```toml
"SQLAlchemy[asyncio]>=2.0",
"asyncpg>=0.29",
"alembic>=1.13",
"redis>=5.0",
"psycopg[binary]>=3.2"
```

Dev:

```toml
"testcontainers[postgres]>=4.8",
"pytest-dotenv>=0.5"
```

## Docker Compose local

Crear `infra/docker-compose.yml` con servicios:

- `postgres`:
  - image `postgres:16`
  - db `numguard`
  - user `numguard`
  - password local de desarrollo
  - puerto `5432`
  - volumen persistente local
  - healthcheck con `pg_isready`

- `redis`:
  - image `redis:7-alpine`
  - puerto `6379`
  - comando con politica LRU basica para desarrollo si corresponde
  - healthcheck con `redis-cli ping`

Agregar `.env.example` del backend:

```env
DATABASE_URL=postgresql+asyncpg://numguard:numguard@localhost:5432/numguard
REDIS_URL=redis://localhost:6379/0
```

## Modelos a implementar

### `area_prefixes`

Campos:

```text
id UUID PK
prefix VARCHAR(6) UNIQUE NOT NULL
city VARCHAR(100) NOT NULL
province VARCHAR(100) NOT NULL
operator VARCHAR(50) NULLABLE
is_mobile BOOLEAN DEFAULT false
is_valid BOOLEAN DEFAULT true
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

### `phone_numbers`

Campos:

```text
id UUID PK
number_e164 VARCHAR(20) UNIQUE NOT NULL
number_local VARCHAR(15) NOT NULL
prefix_id UUID FK area_prefixes.id NULLABLE
status ENUM/CHECK: CLEAN, SUSPECT, SPAM, UNVERIFIED
spam_score SMALLINT DEFAULT 0 CHECK 0-100
report_count INTEGER DEFAULT 0
source ENUM/CHECK: ENACOM, CROWDSOURCE, AI_AGENT, MANUAL, SEED
metadata JSONB DEFAULT {}
first_seen_at TIMESTAMPTZ NOT NULL
last_reported_at TIMESTAMPTZ NULLABLE
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
```

### `reports`

Campos:

```text
id UUID PK
phone_number_id UUID FK phone_numbers.id NOT NULL
reporter_device_id VARCHAR(64) NOT NULL
report_type ENUM/CHECK: SPAM_CALL, ROBOCALL, SCAM, FRAUD, OTHER
description TEXT NULLABLE
call_duration_sec INTEGER NULLABLE
call_time TIMESTAMPTZ NULLABLE
ip_hash VARCHAR(64) NULLABLE
created_at TIMESTAMPTZ
```

## Indices obligatorios

Crear migracion Alembic con:

```sql
CREATE INDEX idx_phone_numbers_e164 ON phone_numbers(number_e164);
CREATE INDEX idx_phone_numbers_status_non_clean ON phone_numbers(status) WHERE status <> 'CLEAN';
CREATE INDEX idx_reports_phone_created ON reports(phone_number_id, created_at DESC);
CREATE INDEX idx_prefixes_prefix ON area_prefixes(prefix);
```

En migraciones normales evitar `CONCURRENTLY` porque Alembic dentro de transaccion puede fallar. Documentar en comentario que en produccion se puede recrear concurrentemente si la tabla ya es grande.

## Estructura a crear/modificar

```text
backend/src/numguard/db/
  __init__.py
  base.py
  session.py
  redis.py
backend/src/numguard/models/
  __init__.py
  base.py
  enums.py
  area_prefix.py
  phone_number.py
  report.py
backend/alembic.ini
backend/alembic/env.py
backend/alembic/versions/<timestamp>_initial_schema.py
backend/tests/test_db_models.py
backend/tests/test_redis_client.py
infra/docker-compose.yml
```

## Reglas de implementacion

- Usar UUID como PK.
- Usar timestamps timezone-aware.
- Centralizar `Base` de SQLAlchemy.
- La sesion async debe ser dependencia inyectable en FastAPI luego.
- Redis debe tener wrapper simple `get_redis_client()` para reutilizacion futura.
- No guardar numeros en Redis todavia.
- No crear datos seed reales todavia.

## Comandos esperados

Desde raiz del repo:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Desde `backend/`:

```bash
python -m pip install -e ".[dev]"
alembic upgrade head
pytest
ruff check .
```

## Tests requeridos

- Crear una fila de `area_prefixes` y persistirla.
- Crear una fila de `phone_numbers` asociada a prefijo.
- Crear un `report` asociado.
- Verificar constraint de `spam_score` 0-100.
- Verificar conexion Redis con `PING`.

## Criterios de aceptacion

- Docker Compose levanta PostgreSQL y Redis.
- Alembic crea las tablas y los indices.
- Tests pasan contra DB de prueba o contenedor.
- La estructura queda lista para seed y endpoint de validacion.
- No hay datos reales en el repositorio.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M02. Usa Context7 para SQLAlchemy async, Alembic, asyncpg, redis-py y Docker Compose antes de programar. Agrega PostgreSQL y Redis locales, modelos, migracion inicial e indices. No implementes /v1/validate ni pipeline ENACOM. Ejecuta docker compose, alembic upgrade head, pytest y ruff. Entrega resumen y cualquier ajuste necesario al README.
```
