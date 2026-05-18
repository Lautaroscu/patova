# NumGuard / Patova — Master Plan Final para Producción + Play Store

## Estado actual detectado

### Arquitectura actual

El proyecto ya tiene una base bastante sólida y bien organizada:

* Monorepo estructurado
* Backend en Python + FastAPI
* Android nativo Kotlin
* PostgreSQL
* Redis
* Alembic
* Docker
* Tests automatizados
* Infraestructura separada
* Sistema inicial de scoring/reportes
* Panel admin
* Sistema de validación de números
* Call Screening API

El proyecto YA está orientado a un MVP serio.

### Lo que ya existe parcialmente

Backend:

* validación de números
* scoring service
* feedback events
* métricas
* cache
* seguridad básica
* seeds
* reportes
* admin dashboard
* testing
* Dockerización

Android:

* estructura base
* permisos
* integración inicial Call Screening
* cache local
* UI inicial

Infra:

* CI/CD inicial
* observabilidad básica
* docker compose

---

# Objetivo real del siguiente milestone

Transformar el MVP actual en:

* aplicación lista para Play Store
* sistema monetizable
* sistema escalable
* arquitectura offline-first
* reputación colaborativa de spam
* sincronización multi-device
* backend productivo
* experiencia premium estable

---

# MASTER ROADMAP

# FASE 1 — AUDITORÍA TÉCNICA COMPLETA

## Objetivo

Determinar exactamente:

* qué está terminado
* qué está incompleto
* qué es prototipo
* qué necesita refactor
* qué rompe Play Store policy

## Tasks

### Backend Audit

* revisar models
* revisar migrations
* revisar endpoints
* revisar seguridad
* revisar autenticación
* revisar manejo de errores
* revisar logs
* revisar workers
* revisar jobs
* revisar rate limiting
* revisar timeouts
* revisar retries

### Android Audit

* revisar permisos sensibles
* revisar Battery Optimization
* revisar foreground services
* revisar compatibilidad Android 12+
* revisar Play Store policies
* revisar UX flows
* revisar almacenamiento local
* revisar lifecycle
* revisar ANRs/crashes

### Infra Audit

* revisar Dockerfiles
* revisar secrets
* revisar nginx
* revisar backups
* revisar monitoring
* revisar observabilidad
* revisar CI/CD

## Deliverables

* documento de deuda técnica
* lista de blockers críticos
* riesgos Play Store
* mapa de arquitectura actual
* lista de TODOs reales

---

# FASE 2 — SISTEMA AVANZADO DE DETECCIÓN SPAM

# Objetivo

Construir un motor híbrido:

* heurístico
* colaborativo
* extensible a ML futuro

---

# Arquitectura propuesta

## Scoring híbrido

### Componentes

1. Local Behavior Engine
2. Cloud Reputation Engine
3. Community Reports
4. Rule Engine
5. Trust Score Aggregator
6. Future ML Adapter

---

# Modelo de scoring propuesto

## Factores

### Historial local del usuario

* frecuencia
* duración promedio
* cantidad de rechazos
* cantidad de bloqueos
* horario
* persistencia
* repetición agresiva
* desconocido/no contacto

### Señales globales

* reportes comunitarios
* usuarios únicos que bloquearon
* score global
* actividad sospechosa
* reputación histórica

### Señales heurísticas

* llamadas masivas
* ráfagas
* patrones automáticos
* números temporales
* secuencias
* spoofing sospechoso

---

# Estados del score

```text
SAFE
LIKELY_SAFE
UNKNOWN
SUSPICIOUS
LIKELY_SPAM
BLOCKED
```

---

# Arquitectura offline-first

## Local first

El dispositivo SIEMPRE debe poder decidir incluso sin internet.

### Cache local

* reputación reciente
* reglas locales
* scores recientes
* whitelist/blacklist
* patrones

### Estrategia

```text
Incoming Call
 -> Local Rules
 -> Local Cache
 -> Local Heuristics
 -> Cloud Validation
 -> Final Decision
```

---

# Nuevas tablas necesarias

## spam_reports

Campos:

* id
* phone_hash
* reporter_user_id
* reason
* severity
* created_at

## phone_reputation

Campos:

* id
* phone_hash
* reputation_score
* total_reports
* unique_reporters
* confidence
* last_seen
* updated_at

## local_behavior_snapshots

Campos:

* id
* user_id
* phone_hash
* call_frequency
* avg_duration
* block_count
* ignored_count
* updated_at

## heuristic_events

Campos:

* id
* phone_hash
* heuristic_type
* weight
* metadata_json
* created_at

---

# Endpoints nuevos

## POST /spam/report

Reportar número.

## GET /spam/reputation/{phone}

Obtener score.

## POST /spam/feedback

Feedback de clasificación.

## GET /spam/rules

Descargar reglas globales.

## POST /behavior/sync

Sincronizar señales locales.

---

# Tasks concretas para agente

## Backend

### Agent Task — Reputation Engine

DONE:

* score agregado
* weights configurables
* Redis cache
* invalidación de cache
* tests unitarios
* score explainable

### Agent Task — Heuristic Engine

DONE:

* reglas dinámicas
* thresholds configurables
* sistema extensible
* métricas
* tests

### Agent Task — Community Feedback

DONE:

* reportes
* antifraude básico
* rate limiting
* reputación ponderada
* deduplicación

---

# FASE 3 — SISTEMA DE PREFERENCIAS AVANZADAS

# Objetivo

Dar control total al usuario.

---

# Arquitectura propuesta

## Settings Layers

1. Local Settings
2. Synced Cloud Settings
3. Runtime Effective Rules
4. Emergency Fallback Rules

---

# Preferencias necesarias

## Whitelist

* números permitidos
* prefijos permitidos
* contactos favoritos

## Blacklist

* números bloqueados
* prefijos bloqueados
* ocultos/desconocidos
* patrones

## Modos

### STRICT_MODE

Solo contactos + whitelist.

### CONTACTS_ONLY

### BLOCK_UNKNOWN

### BLOCK_SPAM

### SILENT_SPAM

### BUSINESS_HOURS_ONLY

---

# Persistencia

## Local

Android Room Database.

## Cloud

PostgreSQL.

## Sincronización

* incremental
* timestamps
* merge strategy
* conflict resolution

---

# Estrategia de sincronización

## Modelo

```text
Device Local State
      ↓
Sync Queue
      ↓
Backend API
      ↓
Conflict Resolver
      ↓
Canonical Cloud State
```

---

# Tablas necesarias

## user_preferences

Campos:

* id
* user_id
* strict_mode
* block_unknown
* spam_threshold
* sync_enabled
* created_at
* updated_at

## whitelist_entries

## blacklist_entries

## sync_events

## preference_versions

---

# Android — Arquitectura propuesta

## Componentes

### Settings Screen

Jetpack Compose.

### Local DB

Room.

### Sync Worker

WorkManager.

### Runtime Rule Engine

Evaluación inmediata.

---

# Edge cases importantes

* usuario sin internet
* sync parcial
* conflicto multi-device
* corrupción local
* reinstalación
* logout/login
* rollback
* spam cache vencido

---

# Tasks concretas

## Android Agent — Preferences UI

DONE:

* Compose screens
* navegación
* persistencia local
* tests
* estados vacíos
* loading states
* dark mode

## Backend Agent — Sync API

DONE:

* sync incremental
* merge rules
* versionado
* tests
* retries

---

# FASE 4 — SISTEMA DE PAGOS + PREMIUM

# Objetivo

Monetización estable y segura.

---

# Arquitectura recomendada

NO confiar solamente en el cliente Android.

Toda validación premium debe ocurrir en backend.

---

# Modelo de negocio

## Plan inicial

* mensual
* ARS 1000

---

# Estados de suscripción

```text
ACTIVE
PENDING
PAST_DUE
EXPIRED
CANCELED
REFUNDED
FRAUD_REVIEW
```

---

# Tablas necesarias

## subscriptions

Campos:

* id
* user_id
* plan_id
* status
* started_at
* expires_at
* provider
* external_payment_id
* renewal_enabled

## payment_events

## payment_webhooks

## invoices

## subscription_plans

---

# Mercado Pago

## Requisitos

### Webhooks

* idempotencia
* retries
* firma/verificación
* logs
* replay protection

### Seguridad

* nunca confiar en payload bruto
* revalidar contra MP API
* rate limit

---

# Endpoints nuevos

## POST /payments/create-preference

## POST /payments/webhook/mp

## GET /subscriptions/me

## POST /subscriptions/cancel

---

# Android Premium UX

## Pantallas

* paywall
* premium active
* payment pending
* expired
* restore purchase

---

# Features premium sugeridas

* bloqueo automático avanzado
* historial ilimitado
* sync multi-device
* backup cloud
* estadísticas
* reglas avanzadas

---

# Tasks concretas

## Backend Agent — MP Integration

DONE:

* preference creation
* webhook verification
* event persistence
* retries
* subscription activation
* tests

## Android Agent — Premium

DONE:

* paywall
* premium state cache
* restore flow
* loading/error states

---

# FASE 5 — PLAY STORE READINESS

# CRÍTICO

Google es extremadamente estricta con apps de Call Screening.

---

# Checklist Play Store

## Permisos

Revisar:

* READ_CALL_LOG
* READ_CONTACTS
* ANSWER_PHONE_CALLS
* READ_PHONE_STATE
* CALL_SCREENING

---

# Requisitos obligatorios

## Política anti abuso

Explicar:

* por qué se usan permisos
* cómo se procesan llamadas
* qué se almacena
* privacidad

## Data Safety

* qué datos se guardan
* cifrado
* third parties
* retención

## Foreground services

Validar Android 13+

---

# Seguridad Android

## Requerido

* Proguard/R8
* certificate pinning
* encrypted local storage
* root detection básica
* anti tampering básico
* obfuscation

---

# Observabilidad

## Android

* Crashlytics
* ANRs
* analytics
* funnel premium
* eventos spam

## Backend

* Prometheus
* Grafana
* structured logging
* tracing
* alerting

---

# FASE 6 — ESCALABILIDAD Y HARDENING

# Backend

## Necesario

* rate limiting
* abuse protection
* Redis tuning
* async workers
* queue system
* retries
* dead letter queue
* backups
* secrets management

---

# Docker hardening

## Tasks

* non-root containers
* multi-stage builds
* image slimming
* healthchecks
* resource limits

---

# CI/CD

## Pipeline

### Backend

* lint
* tests
* coverage
* security scan
* docker build
* deploy

### Android

* lint
* unit tests
* instrumentation tests
* bundle generation
* signing

---

# Testing Strategy

## Backend

* unit
* integration
* load
* chaos
* contract tests

## Android

* UI tests
* lifecycle tests
* offline tests
* battery tests
* compatibility tests

---

# Riesgos críticos detectados

## 1. Play Store rejection

Muy probable si:

* permisos mal justificados
* UX engañosa
* bloqueo agresivo
* privacidad incompleta

## 2. Falsos positivos

El sistema DEBE:

* permitir override
* explicar decisiones
* permitir recovery

## 3. Escalabilidad

Si el crowdsourcing crece:

* cache será crítico
* colas serán necesarias
* agregación async será obligatoria

---

# Recomendaciones técnicas IMPORTANTES

## Android

Migrar todo lo posible a:

* Jetpack Compose
* WorkManager
* Room
* DataStore
* Hilt

---

## Backend

Usar:

* SQLAlchemy 2
* Pydantic v2
* async everywhere
* repository pattern
* service layer clara

---

# Arquitectura futura recomendada

## Corto plazo

Monolito modular.

## Mediano plazo

Separar:

* scoring service
* payments service
* analytics service

---

# DEFINICIÓN FINAL DE DONE

El proyecto estará DONE cuando:

## Android

* pase Play Store review
* no tenga crashes críticos
* soporte offline
* tenga paywall funcional
* tenga sync estable
* tenga tests críticos

## Backend

* tenga observabilidad
* backups
* webhooks seguros
* rate limiting
* tests
* deploy reproducible

## Producto

* onboarding completo
* UX consistente
* políticas legales
* privacidad
* monetización funcional
* métricas operativas

---

# PRIORIDADES REALES

## PRIORIDAD MÁXIMA

1. Policies Play Store
2. Seguridad
3. Persistencia/sync
4. Pagos
5. Falsos positivos

## PRIORIDAD MEDIA

1. UX premium
2. Analytics
3. Optimización

## PRIORIDAD FUTURA

1. ML real
2. reputación avanzada
3. clustering
4. anti spoofing avanzado

---

# Recomendación final

NO intentar agregar ML complejo todavía.

Primero:

* heurísticas sólidas
* crowdsourcing
* explainability
* estabilidad
* UX
* Play Store compliance

Con suficientes datos reales, luego migrar a:

* modelos supervisados
* detección de patrones
* embeddings
* graph reputation
* federated learning

El proyecto ya tiene una muy buena base técnica. La prioridad ahora no es “hacer más features”, sino transformar el MVP en un producto robusto, monetizable y compatible con Play Store.
