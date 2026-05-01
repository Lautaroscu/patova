# M09 - UI Android: notificaciones, historial, reportes y feedback

## Objetivo del modulo

Completar la experiencia Android del MVP:

- Notificacion cuando una llamada fue bloqueada o no pudo verificarse.
- Pantalla de historial de llamadas bloqueadas/sospechosas.
- Flujo de reporte de spam.
- Flujo de feedback: falso positivo / era spam.
- Sincronizacion basica de reportes pendientes cuando hay red.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("Jetpack Compose")
context7.resolve-library-id("Android Notifications")
context7.resolve-library-id("Room Android")
context7.resolve-library-id("WorkManager")
context7.resolve-library-id("Retrofit")
context7.resolve-library-id("Kotlin Coroutines")
```

Rutas sugeridas si existen:

```text
/androidx/androidx
/square/retrofit
/Kotlin/kotlinx.coroutines
```

Topics sugeridos:

```text
Compose: navigation, lists, forms, state hoisting
Notifications: channels, Android 13 notification permission
WorkManager: one-time work, constraints, retry
Room: relations, flows, paging small lists
Retrofit: POST requests, error handling
```

## Funcionalidades requeridas

### 1. Notificacion de bloqueo

Cuando `verdict = BLOCK` o `INVALID_PREFIX`:

Titulo:

```text
Llamada bloqueada por NumGuard
```

Contenido:

```text
Numero sospechoso: +5491****5678 · Score 87
```

Acciones opcionales para MVP:

- `Falso positivo`
- `Ver detalle`

Si es complejo, dejar acciones para iteracion posterior pero notificacion simple debe existir.

### 2. Notificacion de verificacion fallida

Cuando API timeout/error:

```text
No se pudo verificar la llamada. NumGuard la dejo pasar por seguridad.
```

Esta notificacion puede ser silenciosa o solo visible en historial para no molestar.

### 3. Historial local

Crear tabla Room:

```text
CallEvent
- id UUID/string
- numberHash
- numberMasked
- verdict
- spamScore nullable
- reason nullable
- occurredAtMillis
- actionTaken: ALLOWED/BLOCKED/FAILED_OPEN
- syncedFeedback: Boolean
```

Guardar eventos de llamadas evaluadas, al menos bloqueadas y fallidas. Para MVP, evitar guardar todo si hay dudas de privacidad.

### 4. Pantalla historial

Lista con:

- Fecha/hora.
- Numero enmascarado.
- Veredicto.
- Accion tomada.
- Botones: reportar spam / falso positivo segun caso.

### 5. Reporte de spam

Formulario:

- Tipo: SPAM_CALL, ROBOCALL, SCAM, FRAUD, OTHER.
- Descripcion opcional.
- Boton enviar.

Enviar a:

```text
POST /v1/report
```

Si no hay red, guardar pendiente y sincronizar con WorkManager.

### 6. Feedback

Enviar a:

```text
POST /v1/feedback
```

Casos:

- Bloqueada pero era legitima -> `FALSE_POSITIVE`.
- Permitida pero era spam -> `WAS_SPAM`.

## Permisos y compatibilidad

- Android 13+ requiere permiso de notificaciones. Solicitarlo de forma clara.
- No bloquear el uso principal si el usuario no concede notificaciones.
- No pedir permisos innecesarios.

## Estructura a crear/modificar

```text
android/app/src/main/java/ar/com/numguard/data/local/
  CallEventEntity.kt
  CallEventDao.kt
  PendingReportEntity.kt
  PendingReportDao.kt
android/app/src/main/java/ar/com/numguard/data/api/
  ReportApiModels.kt
android/app/src/main/java/ar/com/numguard/domain/
  SaveCallEventUseCase.kt
  SubmitReportUseCase.kt
  SubmitFeedbackUseCase.kt
android/app/src/main/java/ar/com/numguard/notifications/
  NumGuardNotificationManager.kt
android/app/src/main/java/ar/com/numguard/sync/
  PendingReportsWorker.kt
android/app/src/main/java/ar/com/numguard/ui/history/
  HistoryScreen.kt
  ReportDialog.kt
  FeedbackDialog.kt
android/app/src/test/java/ar/com/numguard/
  ReportSyncTest.kt
  HistoryViewModelTest.kt
```

## Reglas UX

- Texto profesional y claro.
- Evitar alarmismo excesivo.
- No mostrar datos completos si no es necesario.
- Si un bloqueo fue por `INVALID_PREFIX`, explicar: "El prefijo no corresponde a numeracion valida registrada localmente".
- Si la llamada se dejo pasar por error de red, explicar que fue por seguridad.

## Tests requeridos

- Se guarda evento local tras decision de llamada.
- Notificacion se construye con numero enmascarado.
- Reporte con red llama API.
- Reporte sin red queda pendiente.
- Worker reintenta reporte pendiente.
- Feedback falso positivo llama endpoint correcto.

## Criterios de aceptacion

- App compila.
- Historial visible.
- Notificaciones funcionando en debug.
- Reporte y feedback integrados con backend.
- Reportes offline quedan pendientes.
- No se muestra numero completo salvo decision explicita en configuracion futura.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M09. Usa Context7 para Compose, Notifications, Room, WorkManager, Retrofit y Coroutines. Agrega historial local, notificaciones, reporte de spam, feedback y sync de pendientes. No cambies la logica core de validacion salvo para guardar eventos. Ejecuta assembleDebug y tests. Mantene privacidad: numeros enmascarados en UI.
```
