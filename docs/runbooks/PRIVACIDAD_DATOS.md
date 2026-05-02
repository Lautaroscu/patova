# Privacidad y datos — Beta cerrada

> **Aviso:** Este documento describe las practicas de manejo de datos durante la beta cerrada. No constituye un documento legal definitivo. Requiere revision por un profesional legal antes de la beta publica.

## Que datos se recolectan

### Datos que el usuario provee activamente

| Dato                     | Proposito                                              |
| ------------------------ | ------------------------------------------------------ |
| Reporte de spam          | Identificar numeros maliciosos para la comunidad       |
| Feedback (falso positivo) | Corregir errores del modelo de scoring                |
| Respuesta a encuesta     | Medir satisfaccion y NPS                               |

### Datos que se recolectan automaticamente

| Dato                     | Proposito                                              |
| ------------------------ | ------------------------------------------------------ |
| Numero llamante          | Evaluar contra la base de spam (nucleo del producto)    |
| Timestamp de la llamada  | Metricas de uso, orden en historial                     |
| Veredicto (allow/suspect/block) | Mostrar al usuario, entrenar el modelo            |
| Device ID hasheado       | Rate limiting, limite de reportes por dispositivo       |
| IP del dispositivo       | Rate limiting por IP                                    |
| User-Agent / app version | Debugging, compatibilidad                               |

### Datos que NO se recolectan

- Contactos del telefono.
- Ubicacion GPS precisa.
- Historial de llamadas completo (solo la llamada entrante en el momento del screening).
- Grabaciones de llamadas.
- Identidad real del usuario (nombre, email, telefono del usuario).
- Datos de otras apps del dispositivo.

## Como se protegen los datos

### Hashing del device_id

El device_id (generado con `DeviceIdProviderImpl`) se hashea con SHA-256 antes de enviarse al backend. No se almacena el valor crudo en la base de datos. Ver `PhoneHashing.kt` en el codigo Android.

### Hashing en cache keys

Las keys de Redis para cache de validacion se generan con `hashlib.sha256` en el backend, combinando el E164 normalizado con un salt interno. No se guarda el numero en texto plano en Redis.

### IP

La IP se usa exclusivamente para rate limiting (slowapi). No se persiste en base de datos. Los logs en produccion deben excluir IPs si se usa JSON renderer.

### Numeros en base de datos

Los numeros de telefono se guardan en la tabla `phone_numbers` en formato E164. Son el nucleo del producto (base antispam colaborativa). Sin esta base el servicio no funciona. Los numeros guardados son siempre numeros llamantes, nunca el numero del usuario.

### Datos del usuario

El backend no tiene tabla de usuarios. No se pide registro, email, ni nombre. El unico identificador es el device_id hasheado, que se usa para:
- Limitar reportes por dispositivo (`max_reports_per_device_per_day`).
- Evitar que un mismo dispositivo sobre-escriba su propio feedback.

## Para que se usan los datos

| Uso                                     | Base                                                       |
| --------------------------------------- | ---------------------------------------------------------- |
| Clasificar llamadas como spam/sospechoso | Funcionamiento esencial del producto                       |
| Mostrar historial al usuario             | Funcionalidad de la app                                    |
| Mejorar el modelo de scoring             | Agregacion anonima de reportes para recalcular scores      |
| Metrica de uso agregada                  | Evaluar exito del MVP (sin datos individuales)             |
| Debugging de incidentes                  | Resolucion de falsos positivos (acceso restringido a devs) |

## Retencion de datos

- **Reportes**: se retienen mientras el numero este en la base. Si un numero se elimina, sus reportes se anonimizan.
- **Feedback**: se retiene 90 dias para analisis de calidad del modelo. Luego se anonimiza.
- **Cache Redis**: TTL configurable (default: `VALIDATE_CACHE_TTL`). Se invalida automaticamente.
- **Logs**: rotacion cada 7 dias en entorno de beta. No se envian logs a servicios externos salvo Sentry si esta configurado.

## Como pedir baja o eliminacion en beta

1. El usuario envia email a `soporte@numguard.com.ar` con asunto "Baja de datos".
2. El usuario debe incluir su device_id (se muestra en Ajustes de la app).
3. El equipo ejecuta la eliminacion en un plazo maximo de 48 horas habiles:

```sql
-- Eliminar reportes del dispositivo
DELETE FROM reports WHERE device_id_hash = '<hash_provisto>';

-- Eliminar feedback del dispositivo
DELETE FROM feedback_events WHERE device_id_hash = '<hash_provisto>';
```

```bash
# Invalidar cache
docker compose exec redis redis-cli KEYS "numguard:validate:*" | xargs docker compose exec redis redis-cli DEL
```

4. Se confirma la eliminacion al usuario por email.

## Quien tiene acceso

| Rol             | Nivel de acceso                                              |
| --------------- | ------------------------------------------------------------ |
| Desarrolladores | Acceso completo a DB, logs, Redis (necesario para operar)    |
| Soporte N1      | Acceso a dashboard admin (reportes y feedback sin hashes expuestos) |
| Terceros        | Ninguno durante la beta cerrada                              |
| Usuario         | Solo sus propios datos via la app (historial, reportes propios) |

## Transferencia a terceros

Durante la beta cerrada **no se transfieren datos a terceros**. No hay integraciones con servicios de analytics, publicidad, ni venta de datos.

Si en el futuro se integra Sentry para crash reporting:
- Sentry recibe stack traces y datos de crash (no numeros de telefono ni device_id).
- Ver [OBSERVABILIDAD.md](./OBSERVABILIDAD.md) y la politica de privacidad de Sentry.

## Seguridad

- API key requerida para todos los endpoints (header `X-NumGuard-Key`).
- Conexiones HTTPS en produccion.
- Base de datos sin exponer a Internet (solo dentro de la red Docker o VPC).
- Secrets en variables de entorno, nunca hardcodeados.
- No se loguean numeros completos si `LOG_LEVEL=WARNING` o superior.

## Cumplimiento con legislacion Argentina

Durante la beta cerrada aplica la Ley 25.326 de Proteccion de Datos Personales. Puntos clave:

- Los datos se recolectan con consentimiento informado (el usuario acepta al instalar).
- El usuario puede solicitar acceso, rectificacion o supresion de sus datos.
- Los datos se almacenan en servidores bajo control del equipo (no se transfieren internacionalmente durante la beta).

**Requiere revision legal antes de beta publica.**
