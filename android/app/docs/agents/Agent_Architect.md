# NumGuard / Patova — Ficha de Tarea: ARQUITECTO PRINCIPAL (Orquestador & Auditor)

* **Rol:** Arquitecto Principal / Orquestador Multi-Agente
* **Rama Git Asignada:** `main` / `master` (Solo lectura de ramas secundarias, revisión e integración).
* **Directorio de Trabajo:** Todo el repositorio.
* **Tecnologías:** Validación de Contratos, Git Workflow, Aseguramiento de Calidad de Código (QA), Control de Deuda Técnica, Políticas de Play Store.

---

## 🎯 1. Objetivo General
Garantizar la consistencia conceptual y técnica de NumGuard / Patova. El Arquitecto **no codifica**, sino que valida las decisiones tomadas por los otros 6 agentes de desarrollo, congela y hace cumplir las interfaces de APIs, previene la acumulación de deuda técnica y supervisa el cumplimiento estricto de las directivas de Google Play Store.

---

## 📊 2. Lista de Control para la Integración de Pull Requests (PR Checklist)
Cuando un agente termine sus tareas en su rama de trabajo (`feature/...`) y solicite un Merge/Pull Request a `main`, debes auditar los siguientes puntos antes de aprobar la integración:

* [ ] **¿Cumple el Contrato de API?:** Verificar que no se hayan modificado los esquemas JSON establecidos en el plan de orquestación central sin justificación y consenso previo de todas las partes.
* [ ] **¿Respeta su Sandbox?:** Asegurar que el agente no haya modificado archivos fuera de su directorio de trabajo exclusivo (ej: que el Agente A de Backend no haya tocado el código Android del Agente B).
* [ ] **¿Pasa la Cobertura de Tests?:** El código integrado debe mantener o aumentar el nivel de cobertura general (mínimo 80-85%).
* [ ] **¿No Introduce Secretos?:** Verificar minuciosamente que no se hayan filtrado credenciales, claves de API, tokens de Sandbox o contraseñas en archivos `.env` o en el código fuente.
* [ ] **¿Cumple Play Store Policy?:** Si el PR modifica la app Android, auditar que no se soliciten nuevos permisos en el manifest que no estén plenamente justificados y explicados en la pantalla de consentimiento del usuario.

---

## 🚦 3. Planificación y Transición de Fases (Roadmap Gating)
Como Orquestador, debes controlar que el desarrollo siga un orden seguro. No permitas que comience el desarrollo de fases avanzadas si las fases críticas previas no están completas:

```text
               ┌───────────────────────────────────────┐
               │    FASE 1: Auditoría & Compliance     │
               │   (Completada y validada por Agent F) │
               └───────────────────┬───────────────────┘
                                   │
                                   ▼
               ┌───────────────────────────────────────┐
               │        FASE 2: Core Engineering       │
               │   (Conexión base Backend y Android)   │
               └───────────────────┬───────────────────┘
                                   │
                                   ▼
               ┌───────────────────────────────────────┐
               │   FASE 3: Inteligencia & Sync Engine  │
               │   (Scoring central y Sync de Room DB) │
               └───────────────────┬───────────────────┘
                                   │
                                   ▼
               ┌───────────────────────────────────────┐
               │       FASE 4: Pagos & Premium UX      │
               │   (Mercado Pago Sandbox operativo)    │
               └───────────────────┬───────────────────┘
                                   │
                                   ▼
               ┌───────────────────────────────────────┐
               │       FASE 5: DevOps & Hardening      │
               │   (CI/CD verificado e Infra lista)    │
               └───────────────────────────────────────┘
```

---

## ✅ 4. Definición de DONE del Proyecto
El proyecto estará completamente **DONE** a los ojos del Arquitecto cuando:

1. **Compilación Limpia General:** Tanto el cliente Android nativo como el backend en FastAPI compilen e inicien sin warnings ni errores en Staging.
2. **Offline-First Operativo:** El teléfono bloquee llamadas y procese heurísticas con latencia instantánea (< 50ms) en modo avión sin caídas ni crashes.
3. **Monetización Verificada:** Se procesen transacciones sandbox reales de Mercado Pago y el estado premium de los usuarios se actualice dinámicamente y se cachee localmente de forma segura.
4. **Play Store Readiness:** Manifest auditado, consentimientos premium presentados y el borrador de políticas `DataSafetyDraft.md` esté finalizado.
