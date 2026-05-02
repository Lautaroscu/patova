# Incidentes de falsos positivos

## Definicion

Un falso positivo (FP) ocurre cuando NumGuard clasifica una llamada legftima como `suspect` o `block` y el usuario la reporta como erronea.

## Niveles de severidad

| Nivel | Descripcion                                                          | Ejemplos                                                                     |
| ----- | -------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| S0    | Bloqueo de llamada critica, emergencia o cliente importante.         | Emergencias medicas, bancos verificando fraude, familiar en situacion de riesgo, llamado de trabajo urgente que resulta en perdida economica. |
| S1    | Bloqueo de llamada legitima recurrente.                              | Delivery, medico de cabecera, colegio de los hijos, proveedor frecuente. El usuario reporta que siempre le bloquean el mismo numero. |
| S2    | Bloqueo legitimo dudoso o falta de contexto.                         | Numero desconocido que el usuario cree que es legitimo pero no puede confirmar, o llamada esporadica que no deberia haberse bloqueado pero no genera impacto real. |

## Procedimiento de resolucion

### Paso 1: identificar

- Obtener el numero original del reporte (del email del usuario o del campo `phone` en el payload de `/v1/feedback`).
- Identificar el hash si el numero fue enmascarado antes de guardar (ver `PhoneHashing` en codigo Android / normalizacion en backend).
- Buscar el numero en la base de datos (`phone_numbers` + `area_prefixes`).
- Revisar el score actual y los reportes asociados desde el dashboard admin.

### Paso 2: corregir score

Si se confirma que es falso positivo:

**Opción A — Bajar score manualmente:**

```sql
UPDATE phone_numbers
SET suspect_score = 0, block_score = 0, reputation = 'CLEAN'
WHERE e164 = '+54XXXXXXXXXX';
```

**Opción B — Marcar como CLEAN via API (si el endpoint admin lo permite):**

```
POST /admin/numbers/+54XXXXXXXXXX/override
{"reputation": "CLEAN", "suspect_score": 0, "block_score": 0}
```

Preferir opcion A durante la beta si el endpoint admin de override no esta implementado.

### Paso 3: invalidar cache Redis

```bash
# Desde el servidor o contenedor
docker compose -f infra/docker-compose.yml exec redis redis-cli

# Buscar y eliminar keys de cache del numero
KEYS numguard:validate:*
# O directamente si se conoce la key exacta:
DEL numguard:validate:v1:+54XXXXXXXXXX
```

Si Redis no es accesible directamente, reiniciar el contenedor `api` fuerza cold start de cache.

### Paso 4: publicar fix

- Si el ajuste fue puntual (un solo numero), responder al usuario por email (ver plantilla en [SOPORTE_USUARIOS.md](./SOPORTE_USUARIOS.md)).
- Si el patron se repite en varios numeros (ej: todos los numeros de un prefijo o carrier), evaluar ajuste de thresholds:
  - Subir `BLOCK_SCORE_MIN` temporalmente (ej: de 61 a 80).
  - Bajar `AUTO_BLOCK_THRESHOLD` para que menos reportes automaticamente bloqueen.
  - Ver [METRICAS_MVP.md](./METRICAS_MVP.md) para thresholds sugeridos.

### Paso 5: registrar aprendizaje

Cada incidente S0 o S1 debe dejar registro escrito de:

| Campo                   | Ejemplo                                     |
| ----------------------- | ------------------------------------------- |
| Fecha                   | 2026-05-15 14:30                            |
| Numero                  | +54 11 5555-XXXX (hash: `a1b2c3...`)       |
| Severidad               | S1                                          |
| Causa raíz              | Prefijo rotado de Claro asignado a nuevo dueno legitimo |
| Acción                  | Score bajado a 0, marca CLEAN, cache invalidada |
| Mejora propuesta        | Agregar decay de score para prefijos reasignados |
| Tiempo total resolucion | 3 horas                                     |

Formato sugerido: issue en GitHub con label `incident-fp`, planilla compartida, o canal de Slack/Telegram.

## Umbrales de accion

| Condicion                                             | Accion                                                         |
| ----------------------------------------------------- | -------------------------------------------------------------- |
| 1 FP S0 en cualquier momento                          | Reunion inmediata del equipo, considerar pausar beta.         |
| 3+ FP S1 en 24 horas                                  | Aumentar `BLOCK_SCORE_MIN`, revisar scoring service.          |
| 10+ FP S2 en una semana                               | Revisar modelo de scoring en profundidad.                     |
| FP recurrente del mismo prefijo o carrier             | Investigar si hubo reasignacion de numeros (ENACOM).          |

## Comandos utiles

```bash
# Ver logs del backend con filtro por numero
docker compose -f infra/docker-compose.yml logs api | grep "+54XXXXXXXXXX"

# Ver score actual desde el admin (si esta implementado)
curl -H "X-NumGuard-Key: $API_KEY" http://localhost:8000/v1/number/+54XXXXXXXXXX

# Invalidar key especifica en Redis
docker compose exec redis redis-cli DEL "numguard:validate:v1:+54XXXXXXXXXX"

# Invalidar todas las keys de validacion (drástico, uso excepcional)
docker compose exec redis redis-cli KEYS "numguard:validate:*" | xargs docker compose exec redis redis-cli DEL

# Aplicar migracion si se cambia el schema
docker compose exec api python -m alembic upgrade head
```
