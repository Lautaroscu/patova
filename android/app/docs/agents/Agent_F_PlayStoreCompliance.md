# NumGuard / Patova — Ficha de Tarea: AGENTE F (Play Store Compliance)

* **Rol:** Especialista de Cumplimiento y Políticas de Google Play Store
* **Rama Git Asignada:** `feature/play-compliance`
* **Directorio de Trabajo Exclusivo:** `[android/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android)` (Enfocado en manifests, flujos de consentimiento legal y vistas de onboarding. No modificas backend).
* **Tecnologías:** Kotlin, Android Manifest, Jetpack Compose UI, Play Console Data Safety Requirements.

---

## 🎯 1. Objetivo General
Asegurar que la aplicación cumpla de forma irrefutable con las estrictas políticas de Google Play Store para aplicaciones que acceden a llamadas y contactos (Call Screening & Call Log), previniendo rechazos de publicación o bloqueos de cuenta.

---

## ⚠️ 2. Contexto Crítico de Google Play Store
Google considera los permisos telefónicos como datos altamente sensibles. Para que una aplicación pueda usar el rol `ROLE_CALL_SCREENING` y acceder a los estados del teléfono, debe:
1. Tener una justificación inequívoca.
2. Presentar un flujo de consentimiento prominente (*Prominent Disclosure*) **ANTES** de mostrar los diálogos nativos de permisos de Android.
3. Declarar encriptación de extremo a extremo y detallar que los hashes de teléfonos nunca se venden ni comparten.

---

## 🛠️ 3. Plan de Tareas Step-by-Step

### Paso F.1: Auditoría Integral del Manifest y Permisos
1. Analizar minuciosamente el archivo [AndroidManifest.xml](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/AndroidManifest.xml).
2. Asegurar que solo se soliciten los permisos estrictamente necesarios:
   * `READ_CALL_LOG` (para mostrar historial de llamadas).
   * `READ_CONTACTS` (para whitelist local automática de tus contactos).
   * `ANSWER_PHONE_CALLS` y `READ_PHONE_STATE` (para gestionar estados).
   * `ROLE_CALL_SCREENING` (rol nativo de bloqueo).
3. Eliminar del manifest cualquier permiso sobrante (ej: localización, escritura de logs externos) que eleve las alertas de Google.
4. Validar que todos los Foreground Services requeridos especifiquen su tipo correspondiente (ej: `android:foregroundServiceType="specialUse"` o `"phoneCall"`) para cumplir con Android 13 y 14.

### Paso F.2: Implementación de la Pantalla de Consentimiento Prominente (Prominent Disclosure)
1. Desarrollar en Jetpack Compose la pantalla de explicación de datos en `ar.com.numguard.ui.disclosure.DisclosureScreen`.
2. **Requisitos de la interfaz (UX Obligatoria para Google):**
   * Debe aparecer inmediatamente en el onboarding de la aplicación, antes de que el usuario vea cualquier solicitud de permiso nativo del sistema operativo.
   * Explicar claramente que el permiso de llamadas se utiliza para interceptar números fraudulentos, y que los números se transforman en **hashes criptográficos unidireccionales (SHA-256)** para ser verificados en la nube.
   * Detallar explícitamente que **ningún dato personal identificable ni el número telefónico en crudo es guardado o vendido**.
   * Debe tener un botón claro de "Aceptar y Continuar" y otro de "Denegar" (con salida elegante explicando que la app no funcionará sin estos permisos).

### Paso F.3: Flujo de Solicitud de Permisos en Runtime
1. Programar el manejador de permisos nativos utilizando `rememberLauncherForActivityResult` de Jetpack Compose en [ar.com.numguard.MainActivity.kt](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/java/ar/com/numguard/MainActivity.kt).
2. Solo se deben invocar los diálogos nativos de permisos una vez que el usuario presionó voluntariamente el botón "Aceptar y Continuar" de la pantalla de *Prominent Disclosure*.

### Paso F.4: Data Safety & Declaración de Políticas
1. Escribir el borrador del documento oficial de Data Safety y Privacidad de la aplicación en `android/app/docs/DataSafetyDraft.md`.
2. El borrador debe responder de forma clara a las preguntas del cuestionario de la consola de Google Play Console sobre: recolección de datos, encriptación en tránsito, eliminación a solicitud del usuario y propósitos de seguridad de la app.

---

## ✅ 4. Definición de DONE
Para que tu rama `feature/play-compliance` pueda ser integrada a `main`:
1. **Consent Flow Completo:** Al abrir la app por primera vez, el usuario ve la pantalla de consentimiento explicando los permisos, hace clic en aceptar, se despliegan los diálogos nativos de Android y, al aceptarlos, ingresa a la aplicación sin crashes.
2. **Denial Flow Elegant:** Si el usuario presiona "Denegar" en la pantalla de consentimiento, la aplicación se detiene pacíficamente mostrando una pantalla informativa con un botón para abrir los Ajustes del sistema y otorgarlos manualmente.
3. **No Unused Perms:** Verificar que no existan permisos fantasmas heredados de librerías de terceros (analizar el merged manifest en Android Studio).
4. **DataSafetyDraft Listo:** El archivo de políticas `DataSafetyDraft.md` debe estar completamente redactado y listo para copiar en la consola de publicación.
