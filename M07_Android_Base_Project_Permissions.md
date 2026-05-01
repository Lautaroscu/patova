# M07 - Proyecto Android base, permisos y configuracion como app de screening

## Objetivo del modulo

Crear el proyecto Android nativo en Kotlin y preparar la base para que NumGuard pueda ser designada como app de screening de llamadas.

Este modulo no debe implementar todavia toda la logica de bloqueo. Debe dejar el proyecto compilando, con estructura, permisos, pantalla inicial y guias para habilitar el rol correspondiente en Android.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("Android Kotlin")
context7.resolve-library-id("Android Gradle Plugin")
context7.resolve-library-id("Jetpack Compose")
context7.resolve-library-id("Hilt Android")
context7.resolve-library-id("Android CallScreeningService")
```

Rutas sugeridas si existen:

```text
/JetBrains/kotlin
/android/gradle-recipes
/androidx/androidx
/google/dagger
/android/platform-samples
```

Si Context7 no devuelve documentacion suficiente para `CallScreeningService`, usar la documentacion oficial de Android desde el entorno disponible o dejar marcado en comentarios la API exacta verificada. No programar por memoria sin verificacion.

Topics sugeridos:

```text
Android: minimum SDK, foreground/background restrictions, role manager
CallScreeningService: manifest declaration, permissions, onScreenCall
Hilt: Android setup, application class, injection
Compose: simple activity, state, navigation basics
Gradle: Kotlin DSL, version catalogs if available
```

## Requisitos tecnicos Android

- Kotlin nativo.
- Min SDK: 29 o superior, porque el MVP apunta a Android 10+.
- Usar Gradle Wrapper, no Gradle global.
- Usar Android Studio o SDK local si esta disponible.
- Usar package name consistente, por ejemplo: `ar.com.numguard.app`.

## Dependencias previstas

Agregar solo las necesarias para la base:

- AndroidX Core.
- Lifecycle.
- Activity Compose o XML simple. Recomendado: Compose si el entorno ya lo soporta.
- Hilt para DI, si no complica el setup inicial.

Retrofit, Room y WorkManager se agregan en M08/M09.

## Estructura sugerida

```text
android/
  settings.gradle.kts
  build.gradle.kts
  gradle/
  gradlew
  gradlew.bat
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/ar/com/numguard/
        NumGuardApplication.kt
        MainActivity.kt
        ui/
          HomeScreen.kt
        screening/
          NumGuardScreeningService.kt
```

## Manifest minimo

Declarar el servicio de screening con permisos adecuados segun docs actuales:

```xml
<service
    android:name=".screening.NumGuardScreeningService"
    android:permission="android.permission.BIND_SCREENING_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.telecom.CallScreeningService" />
    </intent-filter>
</service>
```

Verificar con Context7/documentacion oficial si los atributos o permisos cambiaron. No asumir.

## Pantalla inicial

La pantalla inicial debe mostrar:

- Estado: NumGuard no configurado / configurado como app de screening.
- Boton: Abrir configuracion para activar NumGuard como app de screening.
- Texto corto explicando que sin ese permiso el bloqueo no funciona.
- Ambiente API configurable en build config o settings locales para desarrollo.

## Configuracion del rol

Implementar una funcion que intente abrir la configuracion correspondiente. Segun version de Android, puede usarse `RoleManager` para solicitar rol si aplica o abrir settings de apps predeterminadas.

Regla importante: si no se puede abrir el intent exacto, mostrar instrucciones manuales.

Texto sugerido:

```text
Para activar NumGuard: Ajustes > Aplicaciones > Aplicaciones predeterminadas > Identificador y spam / Screening de llamadas. Selecciona NumGuard.
```

Ajustar texto segun dispositivo real durante pruebas.

## Servicio stub

Crear `NumGuardScreeningService` con `onScreenCall` minimo que siempre permita la llamada.

No llamar aun a la API.

Pseudoflujo:

```kotlin
override fun onScreenCall(callDetails: Call.Details) {
    val response = CallResponse.Builder()
        .setDisallowCall(false)
        .build()
    respondToCall(callDetails, response) // verificar firma exacta en docs actuales
}
```

Importante: verificar firma real de `respondToCall` en la API Android usada. No copiar pseudocodigo sin compilar.

## Criterios de aceptacion

- Proyecto Android compila.
- App abre pantalla inicial.
- Manifest declara servicio de screening.
- Servicio existe y no bloquea llamadas.
- Hay instrucciones visibles para activar NumGuard como app de screening.
- No hay llamadas de red todavia.

## Comandos esperados

Desde `android/`:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M07. Usa Context7 para Android Kotlin, Android Gradle Plugin, Hilt/Compose si los usas y CallScreeningService. Crea proyecto Android base que compile, pantalla inicial y servicio stub que siempre permita llamadas. No agregues Retrofit, Room ni logica de API todavia. Verifica firmas contra documentacion actual. Ejecuta Gradle assembleDebug y tests si el entorno lo permite.
```
