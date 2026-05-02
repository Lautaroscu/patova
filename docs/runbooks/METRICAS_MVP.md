# Metricas MVP — Beta cerrada

## Objetivo

Este documento define las metricas tecnicas y de producto que deben monitorearse durante la beta cerrada para tomar decisiones informadas sobre la calidad del MVP.

## Metricas tecnicas

| Metrica                         | Target beta cerrada | Critico (accion)                              | Como se mide                                      |
| ------------------------------- | ------------------- | ---------------------------------------------- | ------------------------------------------------- |
| API p95 latencia `/v1/validate` | < 200 ms            | > 500 ms requiere optimizacion                 | `numguard_validate_latency_seconds` histogram      |
| API p99 latencia `/v1/validate` | < 500 ms            | > 1000 ms requiere investigacion inmediata     | `numguard_validate_latency_seconds` histogram      |
| API uptime                      | > 99%               | < 95% en ventana de 1 hora activa alerta       | Health check cada 30s desde monitor externo        |
| Cache hit rate                  | > 80%               | < 60% en 1 hora sugiere problemas de cache     | `numguard_validate_cache_hits_total / numguard_validate_requests_total` |
| Errores 5xx                     | < 1% de requests    | > 5% activa rollback                           | Logs + Sentry (si esta activo)                     |
| Errores 4xx                     | < 5% de requests    | > 10% sugiere problemas de API key o rate limit | Logs + rate limit counters                         |
| DB connection pool              | Sin saturacion      | Timeouts en logs indican falta de conexiones    | Logs de SQLAlchemy / asyncpg                      |
| Redis conexion                  | Sin errores         | Errores de conexion degradan cache              | Logs de redis-py                                   |

### Prometheus queries de referencia

```promql
# Latencia p95 en ventana de 5 minutos
histogram_quantile(0.95, rate(numguard_validate_latency_seconds_bucket[5m]))

# Latencia p99
histogram_quantile(0.99, rate(numguard_validate_latency_seconds_bucket[5m]))

# Cache hit rate
rate(numguard_validate_cache_hits_total[5m])
/
rate(numguard_validate_requests_total[5m])

# Request rate por verdict
sum(rate(numguard_validate_requests_total[1m])) by (verdict)

# Reportes por minuto
rate(numguard_reports_total[1m])
```

## Metricas de producto

| Metrica                                 | Target beta cerrada   | Critico (accion)                                 |
| --------------------------------------- | --------------------- | ------------------------------------------------ |
| Usuarios activos beta                   | 20-200                | < 10 activos en semana 2: revisar onboarding      |
| Llamadas evaluadas (total)              | > 500 por semana      | < 100 sugiere baja adopcion o falla de screening  |
| Llamadas bloqueadas                     | > 10% de evaluadas    | < 5% sugiere thresholds muy altos o poco spam    |
| Falsos positivos confirmados            | < 2% de bloqueos      | > 2% requiere ajuste de thresholds               |
| Reportes de spam enviados               | > 5 por dia           | < 1 por dia: usuarios no encuentran valor en reportar |
| Usuarios que mantienen app activa 7 dias | > 30%                 | < 10% sugiere mala experiencia o falta de valor  |
| Tasa de desinstalacion                  | < 10% en 4 semanas    | > 20% requiere encuesta de salida                |
| NPS o encuesta simple                   | > 0 (positivo)        | < -20 requiere revision del producto             |

### Encuesta sugerida (semana 2 y 4)

Enviar por email o formulario:

1. Del 1 al 10, ?que tan probable es que recomiendes NumGuard a un amigo?
2. ?Bloqueo NumGuard alguna llamada que no debia?
3. ?Dejo de recibir llamadas spam desde que usas NumGuard?
4. ?Que mejorarias?

## Dashboard operativo

Ver [OBSERVABILIDAD.md](./OBSERVABILIDAD.md) para la configuracion de dashboards en Grafana.

## Recoleccion manual si no hay Grafana

Si Prometheus + Grafana no estan desplegados, recolectar metricas manualmente cada 48 horas:

```bash
# Health
curl -s http://localhost:8000/health

# Metrics raw
curl -s http://localhost:8000/metrics | grep -E "numguard_(validate_requests_total|validate_cache_hits_total|reports_total|validate_latency_seconds)"

# Cantidad de numeros en base
docker compose exec postgres psql -U numguard -d numguard -c "SELECT count(*) FROM phone_numbers;"

# Cantidad de reportes
docker compose exec postgres psql -U numguard -d numguard -c "SELECT count(*) FROM reports;"

# Reportes por dia (ultimos 7 dias)
docker compose exec postgres psql -U numguard -d numguard -c "SELECT date(created_at), count(*) FROM reports WHERE created_at > now() - interval '7 days' GROUP BY 1 ORDER BY 1;"
```

## Umbrales recomendados para beta

Valores conservadores para minimizar falsos positivos:

```env
SUSPECT_SCORE_MIN=21
BLOCK_SCORE_MIN=80
AUTO_BLOCK_THRESHOLD=50
```

Si tras 2 semanas los falsos positivos son < 1%, bajar `BLOCK_SCORE_MIN` progresivamente (80 -> 70 -> 61).

## Cadencia de revision

| Frecuencia  | Que se revisa                                          |
| ----------- | ------------------------------------------------------ |
| Diario      | Uptime, errores 5xx, falsos positivos nuevos           |
| Cada 48 hs  | Latencia p95/p99, cache hit rate, reportes recibidos   |
| Semanal     | Metricas de producto, NPS, criterios de salida         |
| Cierre beta | Evaluacion completa de criterios de salida (ver BETA_CERRADA.md) |

## Registro semanal

Mantener una planilla con:

| Semana | Usuarios activos | Llamadas evaluadas | Bloqueos | FP reportados | % FP/bloqueos | Cache hit % | p95 ms |
| ------ | ---------------- | ------------------ | -------- | ------------- | ------------- | ----------- | ------ |
| 1      |                  |                    |          |               |               |             |        |
| 2      |                  |                    |          |               |               |             |        |
| 3      |                  |                    |          |               |               |             |        |
| 4      |                  |                    |          |               |               |             |        |
