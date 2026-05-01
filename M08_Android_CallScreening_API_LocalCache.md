# M08 - Android CallScreeningService, cliente API y cache local Room

## Objetivo del modulo

Implementar el flujo critico del MVP en Android:

1. `CallScreeningService` recibe llamada entrante.
2. Normaliza o prepara el numero.
3. Consulta cache local Room.
4. Si no hay cache valido, consulta `POST /v1/validate`.
5. Si response es `BLOCK` o `INVALID_PREFIX`, bloquea/rechaza.
6. Si hay timeout/error, deja pasar la llamada (`fail-open`).

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("Android CallScreeningService")
context7.resolve-library-id("Retrofit")
context7.resolve-library-id("OkHttp")
context7.resolve-library-id("Kotlin Coroutines")
context7.resolve-library-id("Room Android")
context7.resolve-library-id("Hilt Android")
```

Rutas sugeridas si existen:

```text
/square/retrofit
/square/okhttp
/Kotlin/kotlinx.coroutines
/androidx/androidx
/google/dagger
/android/platform-samples
```

Topics sugeridos:

```text
CallScreeningService: onScreenCall, CallResponse, timeout behavior
Retrofit: suspend APIs, converter, error handling
OkHttp: interceptors, timeouts, headers
Coroutines: service scope, withTimeout, Dispatchers.IO
Room: entities, DAO, database, migrations
Hilt: injecting dependencies into Android service
```

## Dependencias a agregar

- Retrofit2.
- OkHttp.
- Kotlinx Serialization o Moshi/Gson. Elegir una y documentar. Recomendado: Kotlinx Serialization si el setup es simple.
- Room runtime/compiler/ksp.
- Coroutines Android.
- Hilt si ya quedo estable en M07.

No agregar WorkManager todavia salvo que sea necesario para cache cleanup. WorkManager queda para M09.

## Configuracion de red

Crear configuracion por build type:

```text
BASE_API_URL_DEBUG=http://10.0.2.2:8000/  // emulador Android
BASE_API_URL_RELEASE=https://api.numguard.com.ar/
NUMGUARD_API_KEY_DEBUG=dev key local desde local.properties o BuildConfig no versionado
```

No commitear keys reales. Para debug puede usarse una key dummy compatible con backend local.

## Modelos API

```kotlin
data class ValidateRequest(
    val number: String,
    val deviceId: String,
    val callDirection: String = "INCOMING",
    val timestamp: String
)

data class ValidateResponse(
    val verdict: String,
    val spamScore: Int?,
    val reason: String?,
    val reportCount: Int?,
    val prefixValid: Boolean?,
    val prefixZone: String?,
    val operator: String?,
    val cached: Boolean?,
    val latencyMs: Int?
)
```

Ajustar nombres segun serializador elegido (`@SerialName` o equivalente) para mapear snake_case.

## Room cache local

Entidad sugerida:

```text
CachedValidation
- numberHash: String PK
- numberE164Masked: String nullable
- verdict: String
- spamScore: Int
- reason: String nullable
- reportCount: Int
- prefixZone: String nullable
- cachedAtMillis: Long
- expiresAtMillis: Long
```

No usar numero completo como PK. Usar SHA-256 de E.164. Si se necesita mostrar historial, guardar numero enmascarado.

TTL local:

```text
BLOCK: 24 h
ALLOW: 7 dias
SUSPECT: 30 min
UNKNOWN: 5 min
INVALID_PREFIX: 24 h
```

## Timeouts y fail-open

OkHttp:

```text
connectTimeout: 150 ms a 300 ms en debug puede ser mayor
readTimeout: 150 ms a 500 ms segun pruebas reales
callTimeout: menor a 4.5 s siempre
```

En `CallScreeningService`, usar `withTimeout(4500)` como margen antes del limite del sistema.

Regla obligatoria:

- Si falla cache y falla red: `ALLOW`.
- Si API tarda demasiado: `ALLOW`.
- Si response invalida: `ALLOW` y notificar "No se pudo verificar" en modulo M09.

## Logica de decision Android

```text
BLOCK o INVALID_PREFIX:
  setDisallowCall(true)
  setRejectCall(true)
  setSkipCallLog(false)

SUSPECT:
  setDisallowCall(false)

ALLOW o UNKNOWN:
  setDisallowCall(false)

Error/timeout:
  setDisallowCall(false)
```

Verificar metodos exactos disponibles en la API Android actual antes de compilar.

## Estructura a crear/modificar

```text
android/app/src/main/java/ar/com/numguard/data/api/
  NumGuardApi.kt
  ApiModels.kt
  ApiClientModule.kt
android/app/src/main/java/ar/com/numguard/data/local/
  NumGuardDatabase.kt
  CachedValidationEntity.kt
  CachedValidationDao.kt
android/app/src/main/java/ar/com/numguard/domain/
  ValidateIncomingCallUseCase.kt
  DeviceIdProvider.kt
  PhoneHashing.kt
android/app/src/main/java/ar/com/numguard/screening/
  NumGuardScreeningService.kt
android/app/src/test/java/ar/com/numguard/
  ValidateIncomingCallUseCaseTest.kt
  CacheTtlTest.kt
```

## Device ID anonimo

Crear `DeviceIdProvider`:

- Genera UUID aleatorio en primera ejecucion.
- Lo guarda en EncryptedSharedPreferences si esta disponible o SharedPreferences simple para MVP.
- Envia hash SHA-256 del UUID, no Android ID crudo.

## Tests requeridos

- TTL por verdict correcto.
- Cache hit evita llamada API.
- Cache vencido llama API.
- API `BLOCK` produce decision de bloqueo.
- API timeout produce decision allow.
- Error de red produce decision allow.
- Hash no es igual al numero crudo.

## Criterios de aceptacion

- Android compila.
- Servicio llama a use case.
- Use case consulta cache y API.
- Fail-open garantizado.
- Room cache funciona.
- No se versionan keys reales.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M08. Usa Context7 para CallScreeningService, Retrofit, OkHttp, Coroutines, Room y Hilt. Implementa cliente API, cache local Room y logica del servicio con fail-open. No implementes UI de historial ni reportes todavia. Verifica firmas exactas de Android antes de compilar. Ejecuta assembleDebug y tests unitarios.
```
