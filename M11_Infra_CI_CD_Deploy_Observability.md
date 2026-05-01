# M11 - Infraestructura, CI/CD, deploy y observabilidad

## Objetivo del modulo

Preparar el MVP para deploy controlado:

- Dockerfile backend.
- Docker Compose local completo.
- GitHub Actions para tests y build.
- Configuracion base para EC2 + Nginx.
- Variables de entorno documentadas.
- Observabilidad minima: logs, Sentry opcional, metricas Prometheus.

No crear recursos cloud reales desde el agente salvo que el usuario lo pida explicitamente. Este modulo debe dejar archivos e instrucciones reproducibles.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("Docker")
context7.resolve-library-id("Docker Compose")
context7.resolve-library-id("GitHub Actions")
context7.resolve-library-id("Nginx")
context7.resolve-library-id("Uvicorn")
context7.resolve-library-id("Gunicorn")
context7.resolve-library-id("Sentry Python")
```

Rutas sugeridas si existen:

```text
/docker/docs
/docker/compose
/github/docs
/nginx/nginx
/encode/uvicorn
/benoitc/gunicorn
/getsentry/sentry-python
```

Topics sugeridos:

```text
Docker: Python multi-stage image, non-root user
Compose: env files, healthchecks
GitHub Actions: Python setup, Gradle setup, caching, artifacts
Nginx: reverse proxy, headers, timeouts
Gunicorn/Uvicorn: ASGI worker class
Sentry: FastAPI integration
```

## Archivos a crear/modificar

```text
backend/Dockerfile
backend/.dockerignore
infra/docker-compose.yml
infra/docker-compose.prod.example.yml
infra/nginx/numguard-api.conf
infra/github-actions/ci.yml
.github/workflows/ci.yml
docs/runbooks/DEPLOY_EC2.md
docs/runbooks/ENV_VARS.md
docs/runbooks/OBSERVABILIDAD.md
```

## Dockerfile backend

Requisitos:

- Imagen Python slim actual.
- Instalar dependencias sin cache.
- Copiar solo lo necesario.
- Usuario no root.
- Healthcheck si es viable.
- Comando productivo con Gunicorn + Uvicorn worker o Uvicorn directo documentado.

Ejemplo conceptual:

```bash
gunicorn numguard.main:create_app --factory -k uvicorn.workers.UvicornWorker -w 2 -b 0.0.0.0:8000
```

Verificar sintaxis actual con Context7.

## GitHub Actions

Workflow minimo:

1. Backend:
   - setup Python.
   - install deps.
   - ruff.
   - pytest.

2. Android:
   - setup JDK.
   - Gradle cache.
   - `./gradlew testDebugUnitTest`.
   - `./gradlew assembleDebug`.

3. Docker:
   - build backend image.

No configurar deploy automatico a produccion en este modulo salvo que existan secrets definidos.

## Variables de entorno

Documentar en `docs/runbooks/ENV_VARS.md`:

```env
APP_ENV=
DATABASE_URL=
REDIS_URL=
NUMGUARD_API_KEY=
SUSPECT_SCORE_MIN=
BLOCK_SCORE_MIN=
AUTO_BLOCK_THRESHOLD=
RATE_LIMIT_PER_MINUTE=
RATE_LIMIT_PER_IP=
SENTRY_DSN=
```

Indicar cuales son obligatorias y cuales opcionales.

## Nginx

Configurar reverse proxy para:

- `/` hacia API puerto 8000.
- Timeouts razonables.
- Headers `X-Forwarded-For`, `X-Forwarded-Proto`.
- Limite de body bajo para endpoints JSON.

No incluir certificados reales.

## Deploy EC2 runbook

`docs/runbooks/DEPLOY_EC2.md` debe cubrir:

1. Crear instancia.
2. Instalar Docker.
3. Configurar `.env` en servidor.
4. Levantar compose prod.
5. Configurar Nginx.
6. Configurar dominio y SSL.
7. Verificar `/health`.
8. Correr migraciones.
9. Rollback manual.

No ejecutar acciones cloud reales.

## Observabilidad

- Mantener logs estructurados.
- Exponer `/metrics` si M06 lo implemento.
- Agregar integracion Sentry opcional si `SENTRY_DSN` existe.
- Documentar dashboards sugeridos:
  - latencia p95/p99 `/v1/validate`
  - errores 4xx/5xx
  - cache hits/misses
  - reportes por hora
  - bloqueos por hora

## Criterios de aceptacion

- Backend image build funciona.
- CI existe y cubre backend + Android.
- Runbooks creados.
- Nginx config example creado.
- No hay secrets reales.
- No se hizo deploy real sin autorizacion.

## Comandos esperados

```bash
docker build -f backend/Dockerfile -t numguard-api:local backend

docker compose -f infra/docker-compose.yml up --build
```

Si GitHub Actions no se puede ejecutar localmente, validar YAML sintacticamente si hay herramienta disponible o revisar manualmente.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M11. Usa Context7 para Docker, Compose, GitHub Actions, Nginx, Uvicorn/Gunicorn y Sentry. Crea Dockerfile, CI, configs example y runbooks. No crees recursos cloud reales ni agregues secrets. Ejecuta docker build y docker compose si el entorno lo permite.
```
