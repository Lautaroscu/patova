# Variables de entorno

Archivo de referencia: `backend/.env` (local), `.env` en servidor para produccion.

## Obligatorias

| Variable               | Descripcion                                      | Ejemplo                                        |
| ---------------------- | ------------------------------------------------ | ---------------------------------------------- |
| `APP_ENV`              | Entorno: local / production                      | `production`                                   |
| `DATABASE_URL`         | URL asyncpg de PostgreSQL                        | `postgresql+asyncpg://numguard:pass@host:5432/numguard` |
| `REDIS_URL`            | URL conexion Redis                               | `redis://redis:6379/0`                         |
| `NUMGUARD_API_KEY`     | API Key para autenticar clientes (header `X-NumGuard-Key`) | `sk-...`                             |

## Opcionales

| Variable                     | Default | Descripcion                                  |
| ---------------------------- | ------- | -------------------------------------------- |
| `APP_NAME`                   | `NumGuard API` | Titulo de la app en OpenAPI             |
| `API_VERSION`                | `v1`    | Prefijo de version de la API                |
| `LOG_LEVEL`                  | `INFO`  | Nivel de log: DEBUG / INFO / WARNING / ERROR |
| `SUSPECT_SCORE_MIN`          | `21`    | Umbral minimo para clasificar como sospechoso |
| `BLOCK_SCORE_MIN`            | `61`    | Umbral minimo para clasificar como bloqueo   |
| `AUTO_BLOCK_THRESHOLD`       | `50`    | Cantidad de reportes que dispara bloqueo automatico |
| `RATE_LIMIT_PER_MINUTE`      | `1000`  | Rate limit global por minuto                |
| `RATE_LIMIT_PER_IP`          | `60`    | Rate limit por IP por minuto                |
| `VALIDATE_CACHE_ENABLED`     | `true`  | Habilitar cache local de validaciones       |
| `MAX_REPORTS_PER_DEVICE_PER_DAY` | `3` | Limite de reportes por dispositivo por dia |
| `SENTRY_DSN`                 | (vacio) | DSN de Sentry para error tracking (opcional) |

## Notas

- Las variables se leen con `pydantic-settings` desde `.env`. El modelo acepta `extra="ignore"`.
- En produccion, `NUMGUARD_API_KEY` debe ser un secreto fuerte generado con al menos 32 bytes aleatorios.
- `SENTRY_DSN` solo es necesario si se quiere activar error tracking en Sentry. Si no se define, la app ignora Sentry.
- `POSTGRES_PASSWORD` se define en el docker-compose, no en `.env` de la app. La app usa `DATABASE_URL` exclusivamente.
