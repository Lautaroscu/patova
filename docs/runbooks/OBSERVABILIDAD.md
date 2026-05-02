# Observabilidad

## Logs estructurados

El backend usa `structlog` con renderizado a consola. Los logs incluyen:

- Timestamp ISO 8601
- Nivel de log
- Nombre del logger
- Stack trace en errores
- Contexto adicional por request

En produccion se recomienda cambiar el renderer a JSON para ingesta en sistemas de logs:

```python
# backend/src/numguard/core/logging.py
structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.dict_tracebacks,
        structlog.processors.JSONRenderer(),
    ],
    ...
)
```

## Metrics (Prometheus)

El endpoint `/metrics` expone metricas en formato Prometheus. Metricas disponibles:

| Metrica                               | Tipo      | Descripcion                                  |
| ------------------------------------- | --------- | -------------------------------------------- |
| `numguard_validate_requests_total`    | Counter   | Total de requests de validacion por verdict  |
| `numguard_validate_latency_seconds`   | Histogram | Latencia de requests de validacion           |
| `numguard_validate_cache_hits_total`  | Counter   | Total de cache hits en validacion            |
| `numguard_reports_total`              | Counter   | Total de reportes enviados                   |

### Scrape config para Prometheus

```yaml
scrape_configs:
  - job_name: numguard-api
    scrape_interval: 15s
    static_configs:
      - targets: ["api.numguard.com.ar:8000"]
    metrics_path: /metrics
```

## Sentry (opcional)

Si la variable `SENTRY_DSN` esta definida en `.env`, la app inicializa el SDK de Sentry al arrancar.

Variables de configuracion relacionadas (se pueden agregar al Settings de pydantic):

```env
SENTRY_DSN=https://examplePublicKey@o0.ingest.sentry.io/0
SENTRY_TRACES_SAMPLE_RATE=1.0
```

La integracion usa `sentry_sdk.init()` directamente. Si se requiere mas control (profiling, releases, environments), ajustar en `main.py`.

## Dashboards sugeridos

Recomendacion de dashboards para Grafana (o similar):

### Dashboard: API Performance

| Panel                    | Query / Metrica                                        |
| ------------------------ | ------------------------------------------------------ |
| Request rate             | `rate(numguard_validate_requests_total[1m])`           |
| Latencia p95             | `histogram_quantile(0.95, rate(numguard_validate_latency_seconds_bucket[5m]))` |
| Latencia p99             | `histogram_quantile(0.99, rate(numguard_validate_latency_seconds_bucket[5m]))` |
| Cache hit rate           | `rate(numguard_validate_cache_hits_total[1m]) / rate(numguard_validate_requests_total[1m])` |
| Errores 4xx              | `sum(rate(numguard_validate_requests_total{verdict="allow"}[1m]))` de app logs |
| Errores 5xx              | Contador de status codes >= 500 en logs o Sentry       |

### Dashboard: Operacional

| Panel                    | Query / Metrica                                        |
| ------------------------ | ------------------------------------------------------ |
| Reportes por hora        | `rate(numguard_reports_total[1h]) * 3600`              |
| Bloqueos por hora        | `rate(numguard_validate_requests_total{verdict="block"}[1h]) * 3600` |
| Sospechosos por hora     | `rate(numguard_validate_requests_total{verdict="suspect"}[1h]) * 3600` |

### Alertas sugeridas

| Alerta                                   | Condicion                           |
| ---------------------------------------- | ----------------------------------- |
| API down                                 | `up{job="numguard-api"} == 0` for 2m |
| Latencia p99 > 1s                        | `histogram_quantile(0.99, ...) > 1` for 5m |
| Tasa de errores 5xx > 5%                 | `rate(...{status=~"5.."}[5m]) / rate(...[5m]) > 0.05` |
| Sin cache hits por 10 min                | `rate(numguard_validate_cache_hits_total[10m]) == 0` |
