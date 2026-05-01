# M12 - Beta cerrada, operacion y criterios de salida del MVP

## Objetivo del modulo

Preparar el MVP para una beta cerrada controlada en Argentina, inicialmente CABA/GBA o una zona acotada.

Este modulo no agrega grandes funcionalidades. Organiza operacion, soporte, metricas y criterios de decision para saber si el MVP funciona o debe iterarse.

## Context7 obligatorio

Este modulo tiene menos codigo, pero si se modifican herramientas de analytics, crash reporting o distribucion Android, usar Context7 antes:

```text
context7.resolve-library-id("Firebase App Distribution")
context7.resolve-library-id("Firebase Crashlytics")
context7.resolve-library-id("Sentry Android")
context7.resolve-library-id("Google Play Android")
```

Rutas sugeridas si existen:

```text
/firebase/docs
/getsentry/sentry-java
```

Usar solo lo necesario. No agregar Firebase si el MVP ya usa Sentry y distribucion manual alcanza.

## Documentos a crear

```text
docs/runbooks/BETA_CERRADA.md
docs/runbooks/SOPORTE_USUARIOS.md
docs/runbooks/INCIDENTES_FALSOS_POSITIVOS.md
docs/runbooks/METRICAS_MVP.md
docs/runbooks/PRIVACIDAD_DATOS.md
docs/runbooks/RELEASE_CHECKLIST.md
```

## Beta cerrada

`BETA_CERRADA.md` debe definir:

- Objetivo: validar bloqueo real de spam con bajo falso positivo.
- Zona inicial: CABA/GBA o zona elegida.
- Participantes: 20 usuarios internos primero, luego 200.
- Criterios de inclusion: Android 10+, aceptan configurar app de screening, aceptan reportar feedback.
- Duracion: 2 a 4 semanas.
- Canal de soporte: email o formulario.
- Procedimiento de instalacion APK.
- Procedimiento de desinstalacion y desactivacion del screening.

## Metricas MVP

`METRICAS_MVP.md` debe incluir:

### Tecnicas

```text
API p95 < 200 ms
API p99 < 500 ms
Uptime > 99% durante beta
Cache hit rate > 80%
Errores 5xx < 1%
Crash-free Android sessions > 99%
```

### Producto

```text
Usuarios activos beta
Llamadas evaluadas
Llamadas bloqueadas
Falsos positivos reportados
Reportes de spam enviados
Usuarios que mantienen la app activa luego de 7 dias
NPS o encuesta simple
```

## Falsos positivos

`INCIDENTES_FALSOS_POSITIVOS.md` debe definir severidad:

- S0: bloqueo de llamada critica/emergencia o cliente importante.
- S1: bloqueo de llamada legitima recurrente.
- S2: bloqueo legitimo dudoso o falta de contexto.

Procedimiento:

1. Identificar numero enmascarado y hash si corresponde.
2. Bajar score o marcar CLEAN manualmente si se confirma.
3. Invalidar cache Redis.
4. Publicar fix o ajuste de umbral.
5. Registrar aprendizaje.

## Privacidad y datos

`PRIVACIDAD_DATOS.md` debe dejar claro:

- Que datos se recolectan.
- Para que se usan.
- Como se hashean device_id, IP y cache keys.
- Que numeros se guardan en base porque son el nucleo del producto.
- Como pedir baja/eliminacion en beta.
- Quien tiene acceso admin.

No redactar como documento legal definitivo. Marcar que requiere revision legal antes de beta publica.

## Release checklist

`RELEASE_CHECKLIST.md`:

Backend:

- Migraciones aplicadas.
- `.env` completo.
- `/health` OK.
- `/v1/validate` OK con numero clean/spam/unknown.
- Redis OK.
- Logs sin secretos.
- Backup DB configurado si hay cloud.

Android:

- APK debug/release firmado segun canal.
- Base URL correcta.
- API key de beta no hardcodeada de forma insegura en repo.
- Permisos revisados.
- Fail-open probado.
- Desactivacion probada.

Operacion:

- Canal soporte activo.
- Formulario feedback listo.
- Dashboard admin accesible.
- Runbook falso positivo listo.

## Ajuste de thresholds

Para beta cerrada, recomendar thresholds conservadores:

```env
SUSPECT_SCORE_MIN=21
BLOCK_SCORE_MIN=80
AUTO_BLOCK_THRESHOLD=50
```

Luego bajar `BLOCK_SCORE_MIN` solo si los datos muestran bajo falso positivo.

## Criterios de salida

La beta cerrada pasa a beta publica si:

- No hay S0 abiertos.
- Falsos positivos confirmados < 2% de bloqueos.
- p95 API < 200 ms en trafico real.
- Al menos 20 usuarios usan la app durante 7 dias.
- Se bloquean llamadas spam reales verificadas.
- El soporte puede resolver incidentes sin tocar codigo manualmente cada vez.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M12. No agregues features grandes. Crea runbooks de beta cerrada, soporte, falsos positivos, metricas, privacidad y release checklist. Si agregas integraciones de distribucion o crash reporting, usa Context7 primero. No hagas deploy ni publiques APK sin autorizacion. Entrega documentos claros y accionables.
```
