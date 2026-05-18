# NumGuard / Patova — Ficha de Tarea: AGENTE B (Android Core)

* **Rol:** Especialista de Android Core
* **Rama Git Asignada:** `feature/android-core`
* **Directorio de Trabajo Exclusivo:** `[android/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android)` (No debes modificar ningún archivo en `backend/` ni `infra/`).
* **Tecnologías:** Kotlin, Jetpack Compose, Room Database, WorkManager, Coroutines, Jetpack DataStore / EncryptedSharedPreferences.

---

## 🎯 1. Objetivo General
Establecer la persistencia local de datos y la interfaz de usuario de configuración utilizando una arquitectura offline-first sólida, vinculando el servicio nativo de Call Screening a la base de datos local y garantizando un rendimiento de decisión inmediato (< 150ms).

---

## 📜 2. Contratos de Datos y Mocks
Debes utilizar Room Database localmente para persistir las listas y configuraciones. Cuando necesites interactuar con el backend, utiliza el siguiente contrato estructurado. Si el endpoint de backend aún no está listo, debes mockear las respuestas siguiendo exactamente este esquema.

### Contrato: Sincronización Incremental de Preferencias y Listas (`POST /api/v1/behavior/sync`)
* **Request Payload (de Android a FastAPI):**
```json
{
  "client_last_sync_timestamp": "2026-05-18T00:00:00Z",
  "local_changes": {
    "preferences": {
      "strict_mode": true,
      "block_unknown": false,
      "spam_threshold": 0.75,
      "sync_enabled": true,
      "updated_at": "2026-05-18T01:20:00Z"
    },
    "new_whitelist_entries": [
      { "phone_hash": "a1b2c3d4...", "label": "Familia", "added_at": "2026-05-18T01:10:00Z" }
    ],
    "new_blacklist_entries": [
      { "phone_hash": "f9e8d7c6...", "reason": "Molesto", "added_at": "2026-05-18T01:15:00Z" }
    ]
  }
}
```

---

## 🛠️ 3. Plan de Tareas Step-by-Step

### Paso B.1: Base de Datos Room y persistencia local
1. Crear las entidades de Room en el paquete `ar.com.numguard.data.local.entities`:
   * `LocalPreferencesEntity` (strictMode: Boolean, blockUnknown: Boolean, spamThreshold: Float, syncEnabled: Boolean, updatedAt: Long).
   * `WhitelistEntity` (phoneHash: String, label: String, addedAt: Long).
   * `BlacklistEntity` (phoneHash: String, reason: String, addedAt: Long).
2. Crear los DAOs en `ar.com.numguard.data.local.daos` con consultas optimizadas y observables (utilizando `Flow` o `LiveData`).
3. Inicializar la base de datos de Room en `ar.com.numguard.data.local.NumGuardDatabase` utilizando migraciones automáticas o destructivas controladas para desarrollo.

### Paso B.2: Pantalla de Configuración Avanzada (Settings Screen)
1. Desarrollar en Jetpack Compose la interfaz completa de configuración en [ar.com.numguard.ui.settings.SettingsScreen](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/java/ar/com/numguard/ui/settings/SettingsScreen.kt).
2. La interfaz debe permitir activar/desactivar:
   * **Strict Mode** (Solo permitir contactos y whitelist).
   * **Block Unknown** (Bloquear números desconocidos/ocultos).
   * **Spam Threshold** (Slider del nivel de sensibilidad al spam).
   * **Listas Negras y Blancas** (Vistas interactivas para agregar, buscar y eliminar números).
3. Asegurar transiciones suaves, soporte completo de Dark Mode y estados de loading/error premium.

### Paso B.3: Conectar el Servicio de Call Screening
1. Vincular el servicio nativo de interceptación en [NumGuardCallScreeningService.kt](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/java/ar/com/numguard/screening/NumGuardCallScreeningService.kt).
2. Al recibir una llamada entrante, consultar Room de forma síncrona en un hilo de background optimizado:
   * Si el número está en la whitelist -> Permitir llamada.
   * Si el número está en la blacklist -> Rechazar llamada.
   * Si "Block Unknown" está activo y el número no está en contactos -> Rechazar llamada.
3. Asegurar que la consulta e intercepción tome **menos de 150ms** para no retrasar la visualización de la llamada.

### Paso B.4: Encrypted Storage para Datos Sensibles
1. Migrar el almacenamiento de tokens de autenticación de usuario y configuraciones sensibles de SharedPreferences comunes a `EncryptedSharedPreferences` (Jetpack Security) o a Jetpack DataStore con cifrado AES.

---

## ✅ 4. Definición de DONE
Para que tu rama `feature/android-core` pueda ser integrada a `main`:
1. **Zero ANR (App Not Responding):** Todas las lecturas e intercepciones de base de datos en el `CallScreeningService` deben hacerse en hilos secundarios rápidos. Ninguna operación de base de datos puede bloquear el hilo principal (UI thread).
2. **Settings Instant Persist:** Al cambiar un toggle en la pantalla de settings, el estado debe persistirse en Room de forma instantánea y reactiva.
3. **UI Tests:** Cobertura de tests unitarios instrumentados sobre Room DAOs e interfaces de configuración de mínimo 80%.
4. **Dark Mode & Styling:** La UI debe respetar el diseño del sistema, utilizar fuentes personalizadas (Outfit/Inter) y funcionar perfectamente en Light y Dark Mode.
