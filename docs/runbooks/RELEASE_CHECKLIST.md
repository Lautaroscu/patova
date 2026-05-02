# Release checklist — Beta cerrada

Checklist para validar antes de cada release de la beta cerrada. Completar todos los items. Si un item no aplica, marcar N/A con justificacion.

## Backend

### Migraciones y base de datos

- [ ] Migraciones de Alembic aplicadas en el entorno de beta: `alembic upgrade head`
- [ ] `alembic history` muestra las migraciones esperadas sin gaps
- [ ] No hay migraciones pendientes sin aplicar
- [ ] Backup de base de datos configurado si hay cloud (pg_dump cron o snapshot RDS)

### Configuracion

- [ ] `.env` completo con todas las variables obligatorias segun [ENV_VARS.md](./ENV_VARS.md)
- [ ] `APP_ENV=production` para beta
- [ ] `NUMGUARD_API_KEY` es un valor fuerte (>= 32 bytes aleatorios), no `change-me-local-dev-key`
- [ ] `DATABASE_URL` apunta a la instancia correcta (no a localhost)
- [ ] `BLOCK_SCORE_MIN=80` para fase inicial conservadora (ver [METRICAS_MVP.md](./METRICAS_MVP.md))
- [ ] `SENTRY_DSN` configurado (opcional pero recomendado)
- [ ] Secrets no estan en el repositorio (verificar `.gitignore` y `docker-compose` no exponen `.env` real)

### Health y endpoints

- [ ] `GET /health` responde 200 con `{"status":"ok"}`
- [ ] `GET /v1/validate` con numero clean de prueba devuelve `verdict: allow`
- [ ] `GET /v1/validate` con numero spam de prueba devuelve `verdict: suspect` o `block`
- [ ] `GET /v1/validate` con numero desconocido devuelve `verdict: allow`
- [ ] `GET /v1/number/+54XXXXXXXXXX` funciona para un numero existente
- [ ] `POST /v1/feedback` acepta payload valido
- [ ] `POST /v1/reports` acepta payload valido
- [ ] `GET /metrics` expone metricas Prometheus
- [ ] `GET /admin` carga el dashboard (si admin esta habilitado)

### Redis

- [ ] Redis responde: `docker compose exec redis redis-cli PING` -> `PONG`
- [ ] Cache de validacion funciona: primera request lenta, segunda rapida (cache hit)
- [ ] `docker compose exec redis redis-cli KEYS "numguard:*"` muestra keys esperadas

### Performance

- [ ] `p95 < 200 ms` en `/v1/validate` con carga de prueba o datos reales
- [ ] No hay memory leaks observables (monitorear uso de RAM del contenedor por 1 hora)
- [ ] Pool de conexiones DB no se satura bajo carga simulada

### Logs

- [ ] Logs visibles con `docker compose logs api`
- [ ] Logs no contienen secrets (API key, passwords, DSN completo)
- [ ] Logs en formato correcto (JSON si produccion, console si beta debug)
- [ ] Sentry recibe errores si SENTRY_DSN esta configurado

### Contenedores

- [ ] `docker compose -f infra/docker-compose.yml -f infra/docker-compose.prod.example.yml ps` muestra todos los servicios `Up` y `healthy`
- [ ] Reinicio de contenedor no pierde datos (verificar volumenes)
- [ ] `docker compose restart api` no causa downtime > 5 segundos

## Android

### Build y firma

- [ ] APK debug generado con `./gradlew assembleDebug` sin errores
- [ ] APK release firmado con keystore de beta (si se usa release)
- [ ] `minSdk = 29` confirmado en `build.gradle.kts`
- [ ] `targetSdk = 34` confirmado
- [ ] ProGuard/R8 sin errores si esta habilitado para release

### Configuracion de endpoints

- [ ] `API_BASE_URL` en release apunta a la URL real de beta (NO `http://10.0.2.2:8000/`)
- [ ] `API_KEY` en build config es la clave de beta, NO `dev-dummy-key`
- [ ] `API_KEY` no se hardcodea de forma insegura (si esta en `buildConfigField`, verificar que no se commitea el valor real al repo)

### Permisos

- [ ] `INTERNET` presente en AndroidManifest.xml
- [ ] `POST_NOTIFICATIONS` presente
- [ ] Permiso de CallScreeningService declarado correctamente
- [ ] No hay permisos innecesarios que activen rechazo del usuario

### Funcionalidad core

- [ ] App instala correctamente via `adb install` o APK directo
- [ ] NumGuard aparece como opcion en "App de identificacion de llamadas y spam"
- [ ] Llamada de numero spam conocido se bloquea o marca como sospechoso
- [ ] Llamada de numero limpio conocido se permite (fail-open)
- [ ] Notificacion aparece en pantalla de llamada entrante
- [ ] Historial muestra la llamada evaluada
- [ ] Boton "Reportar spam" funciona y envia al backend
- [ ] Boton "Es falso positivo" funciona y envia feedback

### Fail-open

- [ ] Con WiFi apagado y datos moviles apagados, la llamada entra normalmente
- [ ] Con API caida (backend detenido), la llamada entra normalmente
- [ ] Con timeout de API (> 5 segundos), la llamada entra normalmente
- [ ] No se pierden llamadas por fallas del screening service

### Desactivacion

- [ ] Al desmarcar NumGuard como app de screening, las llamadas entran sin interferencia
- [ ] Al desinstalar la app, el screening deja de funcionar completamente
- [ ] Reinstalar la app restaura la funcionalidad sin datos residuales corruptos

## Operacion

### Soporte

- [ ] Email `soporte@numguard.com.ar` creado y funcional
- [ ] Plantillas de respuesta preparadas (ver [SOPORTE_USUARIOS.md](./SOPORTE_USUARIOS.md))
- [ ] Persona designada como N1 de guardia durante la primera semana
- [ ] Canal interno de coordinacion activo (Telegram/WhatsApp/Slack)

### Feedback y reportes

- [ ] Formulario de feedback desde la app funcional (POST `/v1/feedback`)
- [ ] Reportes de spam llegando a la base de datos
- [ ] Dashboard admin (`/admin`) accesible y mostrando datos reales
- [ ] Runbook de falso positivo probado (simular un FP y resolverlo en < 4 horas)

### Monitoreo

- [ ] Health check externo configurado (cron, UptimeRobot, o similar)
- [ ] Prometheus scrapeando `/metrics` si esta desplegado
- [ ] Sentry recibiendo errores del backend si SENTRY_DSN esta configurado
- [ ] Al menos una persona del equipo recibe alertas de caida

### Datos y privacidad

- [ ] Procedimiento de baja de datos probado (simular solicitud y ejecutar eliminacion)
- [ ] Verificar que los logs no contienen datos personales identificables
- [ ] `PRIVACIDAD_DATOS.md` revisado y accesible para usuarios que lo soliciten

### Comunicacion

- [ ] Email de invitacion a beta redactado y revisado
- [ ] Instrucciones de instalacion claras (link a APK + pasos)
- [ ] Canal para preguntas/preguntas frecuentes listo
- [ ] Usuarios saben como reportar problemas y como desinstalar

## Sign-off

| Rol              | Nombre | Fecha | Firma |
| ---------------- | ------ | ----- | ----- |
| Tech lead        |        |       |       |
| Desarrollador    |        |       |       |
| Soporte (N1)     |        |       |       |

---

Version: 0.1.0-beta | Ultima actualizacion: fecha del release
