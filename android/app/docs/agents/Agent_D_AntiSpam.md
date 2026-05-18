# NumGuard / Patova — Ficha de Tarea: AGENTE D (Anti-SPAM Intelligence)

* **Rol:** Especialista en Inteligencia Anti-SPAM y Scoring Híbrido
* **Rama Git Asignada:** `feature/spam-intel`
* **Directorio de Trabajo:**
  * Backend: `[backend/src/numguard/services/scoring/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/services/scoring)`
  * Android: `[android/app/src/main/java/ar/com/numguard/domain/heuristics/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/android/app/src/main/java/ar/com/numguard/domain/heuristics)`
* **Tecnologías:** Python, Numpy (opcional para cálculos), Kotlin, Room, Algoritmia de Detección de Patrones.

---

## 🎯 1. Objetivo General
Construir el motor híbrido de reputación: programar el algoritmo de scoring de confianza en el backend con deduplicación de reportes y protección anti-fraude, y desarrollar las reglas heurísticas rápidas locales en Android para tomar decisiones inteligentes al instante y de forma offline (< 50ms).

---

## 📜 2. Contratos de API Obligatorios
Debes alimentar y respetar el siguiente contrato de respuesta de reputación y scoring.

### Contrato: Detalle de Scoring y Reputación (`GET /api/v1/spam/reputation/{phone_hash}`)
* **Response Payload:**
```json
{
  "phone_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "reputation_score": 0.82,
  "reputation_state": "SUSPICIOUS",
  "total_reports": 24,
  "unique_reporters": 18,
  "confidence": 0.91,
  "explainability": {
    "heuristic_flags": ["HIGH_CALL_FREQUENCY_BURST", "TEMPORARY_NUMBER_PATTERN"],
    "community_severity": "HIGH",
    "description": "Reportado frecuentemente como telemarketing en las últimas 2 horas con llamadas de alta frecuencia."
  },
  "last_seen": "2026-05-18T01:30:00Z"
}
```

---

## 🛠️ 3. Plan de Tareas Step-by-Step

### Paso D.1: Central Reputation Scoring Engine (Backend)
1. Desarrollar el servicio central en `backend/src/numguard/services/scoring/engine.py`.
2. Implementar la fórmula matemática de score de reputación (valor decimal de 0.0 a 1.0) basada en:
   * **Volumen:** Cantidad total de reportes comunitarios recibidos.
   * **Diversidad:** Cantidad de reportes de usuarios *únicos* (penalizar reportes del mismo dispositivo).
   * **Gravedad:** Peso configurado según el motivo de reporte (ej: `FRAUD` = peso 1.0, `TELEMARKETING` = peso 0.5).
3. Clasificar el número en uno de los estados de reputación: `SAFE`, `LIKELY_SAFE`, `UNKNOWN`, `SUSPICIOUS`, `LIKELY_SPAM`, `BLOCKED`.
4. Incluir protección contra ataques Sybil (reportes masivos maliciosos) omitiendo reportes de IPs sospechosas o de usuarios con historial de reportes no contrastados.

### Paso D.2: Generar Explicabilidad del Bloqueo (Explainability Payload)
1. Generar metadatos claros de explicabilidad (`explainability` object) explicando con precisión qué reglas heurísticas y qué reportes generaron la clasificación del número, para que el usuario pueda entender por qué se bloqueó una llamada.

### Paso D.3: Motor de Heurísticas Locales (Android)
1. Implementar la clase `LocalHeuristicsEngine` en `ar.com.numguard.domain.heuristics.LocalHeuristicsEngine`.
2. Programar comprobaciones heurísticas instantáneas en el dispositivo:
   * **Secuencias Numéricas:** Identificar si el número entrante tiene patrones de numeración VoIP masivos o temporales (ej: llamadas consecutivas de números idénticos variando solo el último dígito).
   * **Mismatches Geográficos/Ráfagas:** Detectar ráfagas de llamadas entrantes muy rápidas de números desconocidos en cortos períodos de tiempo.
   * **Prefix Match:** Verificar si el prefijo pertenece a rangos telemarketing conocidos provistos por bases de datos oficiales (ej: ENACOM).

### Paso D.4: Pruebas unitarias de detección y falsos positivos
1. Crear una batería de tests unitarios que simulen llamadas entrantes seguras, de telemarketing y fraudulentas para verificar la correcta clasificación en ambos lados.

---

## ✅ 4. Definición de DONE
Para que tu rama `feature/spam-intel` pueda ser integrada a `main`:
1. **Algoritmo validado con Mocks:** Probar el sistema de scoring con datasets simulados de llamadas normales e intrusivas para certificar un porcentaje de acierto superior al 95%.
2. **Offline Latency Test (< 50ms):** El método `evaluate()` de `LocalHeuristicsEngine` en Android debe completar su ejecución en menos de **50ms** para evitar demoras en la respuesta nativa del servicio de Call Screening.
3. **Anti-Sybil Proof:** Demostrar que reportes repetidos creados maliciosamente por un único usuario no alteran drásticamente el score global del número telefónico.
4. **Documentation:** Explicar claramente en el archivo de logs/código el peso matemático asignado a cada heurística y a cada motivo de reporte.
