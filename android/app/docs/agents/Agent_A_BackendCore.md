# NumGuard / Patova — Ficha de Tarea: AGENTE A (Backend Core)

* **Rol:** Especialista de Backend Core
* **Rama Git Asignada:** `feature/backend-core`
* **Directorio de Trabajo Exclusivo:** `[backend/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend)` (No debes modificar ningún archivo en `android/` ni `infra/`).
* **Tecnologías:** Python, FastAPI, SQLAlchemy 2, Alembic, PostgreSQL, Redis, Pydantic v2.

---

## 🎯 1. Objetivo General
Establecer los cimientos del backend de NumGuard / Patova construyendo las tablas de la base de datos, optimizando el sistema de caché y rate-limiting en Redis, creando los endpoints robustos de sincronización base y asegurando un control estricto de excepciones y timeouts en FastAPI.

---

## 📜 2. Contratos de API Obligatorios
Debes respetar estrictamente las firmas de los siguientes contratos acordados. Cualquier modificación de estos esquemas requiere aprobación del Arquitecto.

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
* **Response Payload (Estado Canónico Combinado):**
```json
{
  "sync_timestamp": "2026-05-18T01:40:00Z",
  "sync_status": "SUCCESS",
  "canonical_preferences": {
    "strict_mode": true,
    "block_unknown": false,
    "spam_threshold": 0.75,
    "sync_enabled": true,
    "updated_at": "2026-05-18T01:20:00Z"
  },
  "whitelist_delta": [],
  "blacklist_delta": [
    { "phone_hash": "z9y8x7w6...", "reason": "Global Block", "added_at": "2026-05-17T23:50:00Z" }
  ]
}
```

---

## 🛠️ 3. Plan de Tareas Step-by-Step

### Paso A.1: Migraciones y Modelos Base (PostgreSQL)
1. Escribir los modelos de SQLAlchemy 2.0 en `backend/src/numguard/models/` para:
   * `user_preferences` (id, user_id, strict_mode, block_unknown, spam_threshold, sync_enabled, created_at, updated_at).
   * `whitelist_entries` (id, user_id, phone_hash, label, added_at).
   * `blacklist_entries` (id, user_id, phone_hash, reason, added_at).
2. Generar y probar las migraciones utilizando Alembic en `backend/alembic/versions/` ejecutando `alembic upgrade head`.

### Paso A.2: Estrategia de Caché Avanzada (Redis)
1. Diseñar el gestor de caché en [core/cache.py](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/core/cache.py).
2. Guardar las reputaciones de spam con TTL (Time-To-Live) configurable y un método de invalidación por eventos (ej: cuando un número es reportado repetidas veces).
3. Asegurar resiliencia ante caídas de Redis (fallback gracefully a consulta directa de base de datos Postgres sin romper la llamada).

### Paso A.3: Rate Limiting y Seguridad Base
1. Implementar rate-limiting por dirección IP y por API Key/Token en los endpoints públicos de scoring y feedback en [core/security.py](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/core/security.py).
2. Usar Redis para llevar el conteo de accesos de manera distribuida.

### Paso A.4: Endpoints Base y Robustez en FastAPI
1. Crear el router de sincronización incremental `POST /api/v1/behavior/sync` en [api/sync.py](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/api/sync.py).
2. Programar la lógica de resolución de conflictos en `services/sync.py`:
   * **Preferencias:** Comparar timestamps `updated_at`, gana la más reciente (*Last-Write-Wins*).
   * **Listas:** Hacer una unión sin duplicados de los teléfonos agregados localmente y los presentes en la nube.
3. Asegurar control de excepciones global mediante un Middleware personalizado en [main.py](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/src/numguard/main.py) que capture cualquier error y devuelva JSON estructurado con logs detallados.

---

## ✅ 4. Definición de DONE
Para que tu rama `feature/backend-core` pueda ser integrada a `main`:
1. **Migrations Test:** Todas las migraciones deben correr y poder revertirse (`alembic downgrade -1`) sin perder consistencia de base de datos.
2. **Cobertura de Tests:** Pruebas unitarias completadas para la sincronización incremental y el gestor de caché Redis con cobertura >85%.
3. **Resilience Test:** Demostrar que si Redis está desconectado, los endpoints siguen respondiendo de forma correcta consultando la base de datos directamente (aunque sea más lento).
4. **Validación de Lints:** El código debe pasar los linters (flake8/black/isort/mypy) configurados en el proyecto.
