# NumGuard MVP - Plan general de desarrollo por modulos

## Objetivo del MVP

Construir un prototipo funcional de NumGuard que demuestre el flujo completo:

1. Una llamada entrante llega a Android.
2. `CallScreeningService` consulta una API de validacion.
3. La API responde `ALLOW`, `SUSPECT`, `BLOCK`, `UNKNOWN` o `INVALID_PREFIX`.
4. Android permite o bloquea la llamada usando una estrategia `fail-open` ante error o timeout.
5. Los reportes de usuarios retroalimentan el score de spam.
6. Un panel admin permite monitorear metricas basicas.

El MVP no debe incluir iOS, modelos ML, agentes IA de enriquecimiento, integracion con operadoras ni API publica B2B. Eso queda fuera del alcance inicial.

## Arquitectura base del repositorio

Crear un monorepo con esta estructura:

```text
numguard/
  backend/
    pyproject.toml
    .env.example
    alembic.ini
    alembic/
    src/numguard/
      main.py
      core/
      db/
      models/
      schemas/
      api/
      services/
      workers/
      admin/
    tests/
  android/
    settings.gradle.kts
    build.gradle.kts
    app/
  scripts/
    seed_import.py
    data_samples/
  infra/
    docker-compose.yml
    nginx/
    github-actions/
  docs/
    decisions/
    runbooks/
```

## Secuencia recomendada de modulos

1. `M00_OpenCode_DeepSeek_Context7_Setup.md`
2. `M01_Backend_Base_FastAPI.md`
3. `M02_Database_PostgreSQL_Redis_Alembic.md`
4. `M03_DataPipeline_Seed_ENACOM.md`
5. `M04_Validation_API_Cache_Performance.md`
6. `M05_Crowdsourcing_Reports_Worker_Scoring.md`
7. `M06_Admin_Panel_Metrics.md`
8. `M07_Android_Base_Project_Permissions.md`
9. `M08_Android_CallScreening_API_LocalCache.md`
10. `M09_Android_UI_History_Report_Feedback.md`
11. `M10_Testing_Load_E2E_QualityGates.md`
12. `M11_Infra_CI_CD_Deploy_Observability.md`
13. `M12_Beta_Cerrada_Runbook_Operacion.md`

## Reglas globales para DeepSeek V4 Pro en Open Code

Usar cada modulo como prompt independiente. No pasarle todo el proyecto completo si no es necesario. DeepSeek suele rendir mejor cuando se le da un contexto cerrado, una lista concreta de archivos y criterios de aceptacion verificables.

### Formato de trabajo obligatorio para cada modulo

El agente debe ejecutar este ciclo:

1. Leer este modulo completo.
2. Inspeccionar el arbol del repositorio actual.
3. Usar Context7 antes de escribir codigo para toda libreria o framework involucrado.
4. Proponer un plan corto con archivos a crear/modificar.
5. Implementar en pasos pequenos.
6. Ejecutar pruebas/lint/comandos de verificacion.
7. Entregar resumen de cambios, comandos ejecutados y pendientes.

### Reglas de seguridad y calidad

- No instalar dependencias globalmente salvo herramientas base del sistema. Usar `.venv`, Gradle Wrapper y Docker Compose.
- No hardcodear secretos. Todo secreto debe ir a `.env` y `.env.example` solo con placeholders.
- No guardar numeros telefonicos en logs de forma innecesaria. Preferir hashes cuando se trate de cache o eventos internos.
- No bloquear llamadas por error de red o timeout. Android debe usar `fail-open`.
- No implementar ML ni agentes IA en este MVP.
- No introducir microservicios. Mantener monolito modular.
- No optimizar prematuramente con arquitectura compleja. Primero que funcione de punta a punta.

## Definition of Done global del MVP

El MVP se considera listo si:

- Backend local levanta con un comando usando Docker Compose.
- PostgreSQL tiene migraciones versionadas y seed minimo de prefijos.
- `POST /v1/validate` responde en formato estable y usa Redis cache.
- Android compila, solicita configuracion como app de screening y puede consultar la API.
- Ante `BLOCK` o `INVALID_PREFIX`, Android bloquea/rechaza la llamada.
- Ante timeout/error, Android deja pasar la llamada.
- Existe reporte in-app y endpoint `/v1/report`.
- Existe panel admin minimo con metricas basicas.
- Existen tests unitarios, tests de integracion y prueba de carga inicial.
- Existe runbook para beta cerrada.
