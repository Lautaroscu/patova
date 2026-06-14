Plan de Implementación: Importación de Call Log y Corrección de Contrato API en Android
Este plan detalla los cambios en el proyecto de Android y el contrato de API para implementar la importación inicial del historial de llamadas (para sembrar números de spam) y corregir el desacople del contrato API para reportes y feedback individuales.

Decisiones de Diseño
Corrección de Contrato API:

Las APIs /v1/report y /v1/feedback en el backend esperan el número telefónico en crudo (number: str) para normalizarlo e insertarlo en la base de datos como un entero.
La app de Android actualmente envía un hash SHA-256 (number_hash) en el payload de ReportRequest y FeedbackRequest.
Solución: Modificar ReportRequest y FeedbackRequest en Android para usar number (el número en formato crudo/E.164) en lugar de numberHash, resolviendo la discrepancia que produce respuestas 422 Unprocessable Entity.
Almacenamiento Local en Room:

Para poder reintentar envíos offline y permitir reportar números desde la pantalla de Historial, agregaremos la columna number a las entidades CallEventEntity y PendingReportEntity en la base de datos local.
Esto se realizará mediante una migración de base de datos (MIGRATION_3_4) de Room incrementando la versión a 4, preservando la información del usuario y agregando el campo number TEXT NOT NULL DEFAULT ''.
Importación Inicial de Historial (Seeding):

Implementar un CallLogImportWorker (WorkManager) ejecutado una única vez al finalizar el onboarding (cuando se obtienen los permisos necesarios).
Heurísticas de Spam:
Llamadas marcadas explícitamente como bloqueadas (CallLog.Calls.BLOCKED_TYPE).
Llamadas entrantes/perdidas con duración inferior a 5 segundos (duration < 5 y tipo INCOMING_TYPE o MISSED_TYPE).
Privacidad y Lista Blanca Local:
Consultar la libreta de contactos completa al inicio del worker y mantenerla en un Set<Long> en memoria para descartar llamadas de números que pertenezcan a la agenda del usuario.
Envío en Lotes:
Los números filtrados se normalizan a enteros (ej. 5491112345678) y se envían en lotes de hasta 50 números al endpoint /v1/reports/batch usando AndroidReportBatch.
Cambios Propuestos
Componente: Base de Datos Local (Android)
[MODIFY] 
CallEventEntity.kt
Agregar el campo val number: String = "" con anotación @ColumnInfo(name = "number").
[MODIFY] 
PendingReportEntity.kt
Agregar el campo val number: String = "" con anotación @ColumnInfo(name = "number").
[MODIFY] 
PatovaDatabase.kt
Incrementar la versión de la base de datos a 4.
Implementar y registrar MIGRATION_3_4:
kotlin

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE call_events ADD COLUMN number TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE pending_reports ADD COLUMN number TEXT NOT NULL DEFAULT ''")
    }
}
[MODIFY] 
DatabaseModule.kt
Agregar PatovaDatabase.MIGRATION_3_4 al constructor del Room database builder.
Componente: API & Modelos (Android)
[MODIFY] 
ReportApiModels.kt
Reemplazar number_hash por number: String en ReportRequest y FeedbackRequest para alinearse con el backend.
Agregar las clases de serialización para reportar por lotes:
kotlin

@Serializable
data class AndroidReportBatch(
    @SerialName("device_id")
    val deviceId: String,
    val numbers: List<Long>
)
@Serializable
data class AndroidReportBatchResponse(
    val status: String,
    @SerialName("numeros_nuevos_guardados")
    val newSavedNumbers: Int
)
[MODIFY] 
PatovaApi.kt
Agregar el endpoint de reportes masivos:
kotlin

@POST("v1/reports/batch")
suspend fun reportBatch(@Body request: AndroidReportBatch): AndroidReportBatchResponse
Componente: Casos de Uso, Workers e Interfaz (Android)
[MODIFY] 
SubmitReportUseCase.kt
Aceptar number: String en el método invoke y mapear a number en ReportRequest y PendingReportEntity.
[MODIFY] 
SubmitFeedbackUseCase.kt
Aceptar number: String en el método invoke y mapear a number en FeedbackRequest y PendingReportEntity.
[MODIFY] 
PendingReportsWorker.kt
Adaptar el envío para pasar pending.number en vez de pending.numberHash a la API.
[MODIFY] 
PatovaScreeningService.kt
En las llamadas a saveCallEvent(), pasar el número telefónico sin encriptar para que se persista en la nueva columna de la base de datos local.
[MODIFY] 
HistoryViewModel.kt
Actualizar mergeWithSystemCallLog para propagar el número telefónico original number en CallEventEntity.
Pasar event.number en lugar de event.numberHash al invocar los Casos de Uso en submitReport y submitFeedback.
[NEW] 
CallLogImportWorker.kt
Crear el worker que:
Verifica el permiso READ_CALL_LOG.
Lee todos los números de contactos telefónicos para filtrarlos localmente (whitelist de privacidad).
Consulta las llamadas entrantes/perdidas de los últimos 30 días con duración < 5 segundos o marcadas como bloqueadas.
Deduplica y normaliza a tipo Long.
Envía en lotes de 50 a la API /v1/reports/batch en segundo plano.
[MODIFY] 
MainActivity.kt
Definir la constante KEY_CALL_LOG_IMPORTED = "call_log_imported" en las Shared Preferences.
Implementar triggerCallLogImportIfNeeded() para encolar una única vez el CallLogImportWorker usando WorkManager tras conceder los permisos e ingresar al estado OnboardingState.APP.
Componente: Tests (Android)
[MODIFY] 
ReportSyncTest.kt
 y 
HistoryViewModelTest.kt
Adaptar los mocks de PatovaApi para implementar el nuevo método reportBatch.
Corregir las llamadas a los Casos de Uso modificados y aserciones correspondientes en los tests.
Plan de Verificación
Pruebas Automatizadas
Ejecutar los tests unitarios e integrados del proyecto de Android para validar la serialización, la migración de la base de datos Room y los casos de uso:
powershell

./gradlew testDebugUnitTest
Verificación Manual
Levantar el backend de desarrollo en local (docker compose build && docker compose up).
Compilar la aplicación en un dispositivo Android de pruebas/emulador.
Asegurar que tras conceder permisos y pasar la pantalla de divulgación (Disclosure), se lance el CallLogImportWorker e impacte en el backend (se pueden verificar los registros del contenedor de FastAPI mostrando el log de /v1/reports/batch).
Realizar un reporte manual desde el Historial y verificar que se envíe exitosamente a /v1/report del backend sin lanzar errores 422.
