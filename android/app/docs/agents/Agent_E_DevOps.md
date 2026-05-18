# NumGuard / Patova — Ficha de Tarea: AGENTE E (Infra / DevOps)

* **Rol:** Ingeniero de Infraestructura y DevOps
* **Rama Git Asignada:** `feature/infra-devops`
* **Directorio de Trabajo Exclusivo:** `[infra/](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/infra)` y `.github/workflows/` (Puedes modificar los `Dockerfile` en `backend/` y configurar herramientas de monitoreo).
* **Tecnologías:** Docker, Docker Compose, GitHub Actions, Prometheus, Grafana, Nginx, PostgreSQL, Bash.

---

## 🎯 1. Objetivo General
Securizar y automatizar los ciclos de compilación, testeo y despliegue del sistema (CI/CD), optimizar y asegurar los contenedores Docker para producción, y montar la infraestructura de observabilidad (Prometheus/Grafana) para monitorizar el estado operativo en tiempo real.

---

## 🛠️ 2. Plan de Tareas Step-by-Step

### Paso E.1: Docker Hardening (Backend)
1. Modificar el [Dockerfile](file:///c:/Users/Lauta/OneDrive/Escritorio/patova/backend/Dockerfile) del backend para utilizar compilaciones multi-stage y reducir el tamaño de la imagen final usando `python:3.11-slim`.
2. **Seguridad del Contenedor:**
   * Crear y forzar la ejecución del contenedor bajo un usuario del sistema no privilegiado (`USER appuser`) en lugar de `root`.
   * Configurar límites de recursos de CPU y Memoria RAM en el archivo compose.
   * Agregar instrucciones `HEALTHCHECK` nativas para verificar la disponibilidad del endpoint de salud `/health` de FastAPI.

### Paso E.2: Pipelines de Integración Continua (CI/CD)
1. Desarrollar las GitHub Actions en `.github/workflows/`:
   * **Backend Pipeline (`backend-ci.yml`):**
     * Desencadenarse en PRs y pushes a `main`.
     * Instalar dependencias mediante Poetry o pip.
     * Ejecutar formateadores y linters (black, flake8, isort).
     * Analizar código con escáneres de seguridad (Bandit / Safety) en busca de secretos expuestos y librerías vulnerables.
     * Ejecutar tests unitarios y de integración con reporte de cobertura (pytest).
   * **Android Pipeline (`android-ci.yml`):**
     * Configurar JDK 17.
     * Ejecutar lint de Kotlin (ktlint/detekt).
     * Ejecutar tests unitarios locales y tests instrumentados simulados.
     * Compilar y generar el Android App Bundle (`.aab`) para testing en staging.

### Paso E.3: Observabilidad y Monitoreo (Prometheus & Grafana)
1. Configurar la exportación de métricas de Prometheus en la aplicación FastAPI utilizando middlewares de monitoreo oficiales.
2. Levantar Prometheus y Grafana dentro del flujo de `docker-compose.yml`.
3. Diseñar un dashboard en Grafana (`infra/grafana/dashboards/`) con paneles que expongan visualmente:
   * Latencia de las consultas de reputación de llamadas (percentiles p50, p90, p99).
   * Tasa de errores HTTP 4xx y 5xx.
   * Hits de Rate-limiting (peticiones bloqueadas por abuso).
   * Volumen de reportes de Spam entrantes.
   * Uso de recursos del servidor (CPU, RAM de Postgres y Redis).

### Paso E.4: Backups y Rotación de Logs
1. Escribir un script bash automatizado (`infra/scripts/backup-db.sh`) para realizar copias de seguridad calientes de PostgreSQL, comprimirlas con gzip y enviarlas a un almacenamiento seguro.
2. Configurar la rotación de logs de Nginx y del Backend para evitar desbordar el almacenamiento local.

---

## ✅ 4. Definición de DONE
Para que tu rama `feature/infra-devops` pueda ser integrada a `main`:
1. **GitHub Actions Green:** El pipeline completo de GitHub Actions debe ejecutarse y pasar con éxito ante cualquier commits.
2. **Container Security Pass:** Validar con herramientas de análisis de contenedores (Trivy o similar) que las imágenes generadas no presenten vulnerabilidades críticas.
3. **Observability working:** Demostrar que al simular llamadas y peticiones al backend, Prometheus recolecta las métricas correctamente y Grafana las renderiza en tiempo real en los paneles.
4. **Non-Root Verification:** Demostrar que los procesos de FastAPI corren bajo un PID sin privilegios dentro de Docker.
