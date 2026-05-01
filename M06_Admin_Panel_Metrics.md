# M06 - Panel admin minimo y metricas

## Objetivo del modulo

Crear un panel admin web minimo para monitorear el MVP:

- Bloqueos del dia.
- Total de numeros en base.
- Total de reportes.
- Top 10 numeros reportados.
- Latencia promedio/p95 expuesta como metrica tecnica si ya esta disponible.

El panel debe ser simple. No construir un frontend complejo. Para MVP alcanza con FastAPI + HTML templates.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("FastAPI")
context7.resolve-library-id("Jinja2")
context7.resolve-library-id("Prometheus Python client")
context7.resolve-library-id("SQLAlchemy")
```

Rutas sugeridas si existen:

```text
/tiangolo/fastapi
/pallets/jinja
/prometheus/client_python
/sqlalchemy/sqlalchemy
```

Topics sugeridos:

```text
FastAPI: HTMLResponse, templates, static files, dependencies
Jinja2: templates and autoescaping
Prometheus: counters, histograms, ASGI integration
SQLAlchemy: aggregate queries
```

## Endpoints requeridos

### `GET /v1/stats`

Requiere API key o una admin key. Para MVP puede reutilizar `X-NumGuard-Key`.

Respuesta:

```json
{
  "total_numbers": 500000,
  "total_reports": 1200,
  "blocked_today": 88,
  "top_reported": [
    {
      "number_e164_masked": "+5411****5678",
      "spam_score": 92,
      "report_count": 143,
      "status": "SPAM"
    }
  ]
}
```

### `GET /admin`

HTML simple con los datos anteriores.

### `GET /metrics`

Exponer metricas Prometheus si se agrega `prometheus-client`:

- `numguard_validate_requests_total`
- `numguard_validate_latency_seconds`
- `numguard_validate_cache_hits_total`
- `numguard_reports_total`

Si instrumentar Prometheus completo demora, crear endpoint `/metrics` minimo y dejar TODO documentado. Pero no romper la API.

## Reglas de privacidad

- En admin, enmascarar numeros por defecto.
- No mostrar `device_id`.
- No mostrar IP hash salvo en vistas internas futuras.
- No incluir datos personales en logs.

Funcion sugerida:

```text
mask_e164(+5491112345678) -> +5491****5678
```

## Estructura a crear/modificar

```text
backend/src/numguard/api/v1/stats.py
backend/src/numguard/admin/routes.py
backend/src/numguard/admin/templates/dashboard.html
backend/src/numguard/admin/static/admin.css
backend/src/numguard/services/stats_service.py
backend/src/numguard/core/metrics.py
backend/tests/test_stats_endpoint.py
backend/tests/test_admin_dashboard.py
backend/tests/test_masking.py
```

## Diseno visual minimo

Usar HTML/CSS sin framework externo:

- Header: NumGuard Admin.
- Cards: total numeros, reportes, bloqueos hoy, cache hit rate si disponible.
- Tabla: top reportados.
- Estado de servicios: DB OK / Redis OK si se implementa health extendido.

No usar React, Vue ni build frontend.

## Queries sugeridas

- `COUNT(*) FROM phone_numbers`
- `COUNT(*) FROM reports`
- `COUNT(*) FROM phone_numbers WHERE status = 'SPAM' AND updated_at >= start_of_day`
- Top reportados por `report_count DESC LIMIT 10`

Ajustar segun modelos reales.

## Tests requeridos

- `/v1/stats` sin key devuelve 401.
- `/v1/stats` con key devuelve estructura correcta.
- Los numeros del top vienen enmascarados.
- `/admin` devuelve HTML 200.
- `/metrics` devuelve 200 si se implementa.

## Criterios de aceptacion

- Existe dashboard HTML operativo.
- Existe endpoint JSON `/v1/stats`.
- Numeros enmascarados por defecto.
- Tests pasan.
- No se agrego frontend pesado.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M06. Usa Context7 para FastAPI templates, Jinja2, Prometheus client y SQLAlchemy. Crea /v1/stats, /admin HTML simple, masking de numeros y tests. No agregues React ni frameworks frontend. No modifiques Android. Ejecuta pytest y ruff.
```
