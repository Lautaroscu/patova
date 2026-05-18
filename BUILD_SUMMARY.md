# NumGuard / Patova — Build Summary

**Fecha:** 2026-05-18
**Branch final:** `main`
**Commits mergeados:** 6 feature branches → 1 main

---

## Arquitectura General

```
patova/
├── backend/          # Python FastAPI — SQLAlchemy, Redis, Alembic
├── android/          # Kotlin Jetpack Compose — Room DB, Hilt, WorkManager
├── infra/            # Docker Compose, Prometheus, Grafana, Nginx, backups
├── .github/          # CI/CD pipelines (backend + android)
└── docs/             # ADRs, Runbooks, DataSafetyDraft
```

---

## Fase 1 — Play Store Compliance (Agent F)

| Rama | Commit |
|---|---|
| `feature/play-compliance` | `d279744` |

**Archivos (4):** `AndroidManifest.xml`, `MainActivity.kt`, `DisclosureScreen.kt`, `DataSafetyDraft.md`

- Consent flow prominente: `DisclosureScreen → RequestingPermissionsScreen → App`
- Denial flow elegante con botón "Abrir Ajustes"
- `foregroundServiceType="phoneCall"` para Android 14+
- `DataSafetyDraft.md` completo (143 líneas)

---

## Fase 2 — Core Engineering (Agents A + B)

| Rama | Commit | Tests |
|---|---|---|
| `feature/backend-core` | `6920d07` | 33 unit ✓ |
| `feature/android-core` | `c7ddc6a` | 21 unit ✓ |

**Backend Core (Agent A):**
- Modelos SQLAlchemy 2.0: `user_preferences`, `whitelist_entries`, `blacklist_entries`
- Migración Alembic con upgrade/downgrade (`a7d1e2f3b4c5`)
- `SpamReputationCache` con TTL + fallback a DB si Redis caído
- Rate limiting por IP y token (`check_ip_rate_limit`, `check_token_rate_limit`)
- Endpoint `POST /api/v1/behavior/sync` — contrato Last-Write-Wins
- `ExceptionHandlingMiddleware` global

**Android Core (Agent B):**
- Room DB v3 con `LocalPreferencesEntity`, `WhitelistEntity`, `BlacklistEntity`
- 3 DAOs con `Flow` reactivo
- `SettingsScreen` (Compose): toggle Strict Mode, Block Unknown, Spam Threshold slider, listas blanco/negro
- `EncryptedPreferencesManager` (AES-256 GCM)
- `CallScreeningService`: pipeline whitelist → blacklist → contacts → heurísticas -> cloud (<150ms en `Dispatchers.IO`)

---

## Fase 3 — AntiSpam Intelligence (Agent D)

| Rama | Commit | Tests |
|---|---|---|
| `feature/spam-intel` | `1b73b63` | 65 unit ✓ |

**Backend (`services/scoring/engine.py`):**
- Score híbrido (0.0–1.0): volumen + gravedad + diversidad + recencia
- Pesos: `FRAUD=1.0`, `SCAM=0.9`, `ROBOCALL=0.7`, `SPAM_CALL=0.5`, `OTHER=0.3`
- Clasificación 6 estados: `SAFE → BLOCKED`
- Anti-Sybil: IPs con >10 reportes/día ignoradas, deduplicación de dispositivos
- Endpoint `GET /api/v1/spam/reputation/{phone_hash}` con explainability payload

**Android (`domain/heuristics/LocalHeuristicsEngine.kt`):**
- 4 reglas offline: secuencias VoIP, ráfagas de llamadas, números temporales, prefijos telemarketing (27 ENACOM)
- Latencia <50ms

---

## Fase 4 — Payments (Agent C)

| Rama | Commit |
|---|---|
| `feature/payments` | `f4686b2` |

**Backend (`services/mp/client.py`):**
- Cliente async Mercado Pago REST
- `POST /api/v1/payments/create-preference` → `Subscription` estado `PENDING`
- `POST /api/v1/payments/webhook/mp` con re-validación server-to-server
- Idempotencia vía Redis (`mp_webhook:{id}`, TTL 30d)
- `GET /api/v1/subscriptions/me` devuelve estado premium

**Android (`ui/premium/PaywallScreen.kt`):**
- Glassmorphism + gradientes oro/violeta
- Tabla comparativa 7 features gratis vs premium
- CustomTabs para checkout Mercado Pago
- `PremiumCacheManager` con `EncryptedSharedPreferences` + grace period 7 días

---

## Fase 5 — DevOps (Agent E)

| Rama | Commit |
|---|---|
| `feature/infra-devops` | `4a02101` |

- `Dockerfile` multi-stage con `python:3.11-slim`, usuario no-root (`numguard:1001`)
- `docker-compose.yml` con Prometheus + Grafana + límites CPU/RAM
- Dashboard Grafana: latencia p50/p90/p99, HTTP 4xx/5xx, rate-limit hits, reports, recursos
- GitHub Actions CI: ruff + mypy + bandit + pip-audit + pytest-cov + Trivy scan + ktlint + AAB
- `backup-db.sh`: backup caliente PostgreSQL con gzip + rotación
- Log rotation: Nginx (14d) + backend (30d, max 50MB)
- Middleware Prometheus: métricas `http_requests_total`, `http_request_duration_seconds`

---

## Total

| Métrica | Valor |
|---|---|
| Agentes ejecutados | 6 (F, A, B, D, C, E) |
| Ramas mergeadas | 6 |
| Tests unitarios backend | 132 pasan |
| Tests unitarios android | 21 pasan |
| Archivos creados | ~50 |
| Conflictos resueltos | 7 (MainActivity, router, models/__init__, build.gradle.kts, libs.versions.toml, main.py) |

---

## Pendiente para producción

- Configurar credenciales reales de Mercado Pago
- PostgreSQL + Redis en entorno de staging
- Ejecutar `alembic upgrade head` contra DB real
- Publicar en Google Play Console con el `DataSafetyDraft.md`
