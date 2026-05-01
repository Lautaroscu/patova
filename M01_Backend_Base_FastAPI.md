# M01 - Backend base con FastAPI

## Objetivo del modulo

Crear la base del backend FastAPI con estructura profesional, configuracion por entorno, health checks, versionado `/v1`, logging basico, autenticacion por API Key y pruebas iniciales.

No implementar aun la logica completa de validacion contra base de datos. Eso corresponde a modulos posteriores.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("FastAPI")
context7.resolve-library-id("Pydantic Settings")
context7.resolve-library-id("Uvicorn")
context7.resolve-library-id("Pytest")
context7.resolve-library-id("HTTPX")
```

Rutas sugeridas si estan disponibles:

```text
/tiangolo/fastapi
/pydantic/pydantic
/encode/uvicorn
/pytest-dev/pytest
/encode/httpx
```

Topics sugeridos para `get-library-docs`:

```text
FastAPI: routing, dependencies, security, testing
Pydantic: settings management, BaseSettings
HTTPX: AsyncClient tests with ASGI app
Pytest: fixtures, async tests
```

## Dependencias del backend

Usar un entorno virtual local. No instalar globalmente.

Crear `backend/pyproject.toml` con dependencias iniciales:

```toml
[project]
name = "numguard-backend"
version = "0.1.0"
description = "NumGuard MVP Validation API"
requires-python = ">=3.12"
dependencies = [
  "fastapi>=0.115",
  "uvicorn[standard]>=0.30",
  "pydantic>=2.8",
  "pydantic-settings>=2.4",
  "python-dotenv>=1.0",
  "structlog>=24.1",
]

[project.optional-dependencies]
dev = [
  "pytest>=8.0",
  "pytest-asyncio>=0.23",
  "httpx>=0.27",
  "ruff>=0.6",
  "mypy>=1.11",
]
```

Si Context7 indica versiones o practicas mas actuales, ajustar sin romper compatibilidad.

## Estructura a crear

```text
backend/
  pyproject.toml
  .env.example
  src/numguard/
    __init__.py
    main.py
    core/
      __init__.py
      config.py
      logging.py
      security.py
    api/
      __init__.py
      router.py
      deps.py
      v1/
        __init__.py
        router.py
        health.py
    schemas/
      __init__.py
      common.py
  tests/
    test_health.py
    test_security.py
```

## Variables `.env.example`

```env
APP_ENV=local
APP_NAME=NumGuard API
API_VERSION=v1
NUMGUARD_API_KEY=change-me-local-dev-key
LOG_LEVEL=INFO
```

No crear `.env` real con secretos.

## Endpoints a implementar

### `GET /health`

Respuesta:

```json
{
  "status": "ok",
  "service": "numguard-api",
  "version": "v1"
}
```

### `GET /v1/health`

Misma respuesta. Sirve para probar versionado.

### Dependencia de API Key

Implementar una dependencia reusable que valide header:

```http
X-NumGuard-Key: <api_key>
```

Para este modulo, probarla con un endpoint interno temporal:

```text
GET /v1/protected-ping
```

Respuesta si la key es valida:

```json
{"status":"ok"}
```

Si falta o no coincide, devolver `401`.

## Reglas de implementacion

- Usar Pydantic v2.
- Usar `pydantic-settings` para configuracion.
- El `main.py` debe tener una funcion `create_app()` para facilitar tests.
- No crear conexion a DB todavia.
- No implementar `/v1/validate` todavia salvo stub si es necesario para router.
- No loguear API keys.

## Tests requeridos

Crear tests para:

- `GET /health` devuelve 200.
- `GET /v1/health` devuelve 200.
- Endpoint protegido sin key devuelve 401.
- Endpoint protegido con key correcta devuelve 200.
- Endpoint protegido con key incorrecta devuelve 401.

## Comandos esperados

Desde `backend/`:

```bash
python -m venv .venv
. .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -e ".[dev]"
pytest
ruff check .
```

En Windows PowerShell, adaptar activacion del venv:

```powershell
.venv\Scripts\Activate.ps1
```

## Criterios de aceptacion

- El backend levanta con `uvicorn numguard.main:create_app --factory --reload` desde `backend/src` o configuracion equivalente documentada.
- Los tests pasan.
- La estructura queda preparada para agregar DB, cache y endpoints.
- No hay secretos reales.
- No hay dependencias innecesarias.

## Prompt recomendado para Open Code

```text
Actua como backend senior. Implementa solo el Modulo M01. Antes de escribir codigo, usa Context7 para FastAPI, Pydantic Settings, Uvicorn, Pytest y HTTPX. Crea estructura backend profesional, health checks, API key dependency y tests. No implementes DB ni Redis. No modifiques Android ni infra salvo README si hace falta documentar comandos. Ejecuta pytest y ruff. Entrega resumen de archivos modificados y comandos ejecutados.
```
