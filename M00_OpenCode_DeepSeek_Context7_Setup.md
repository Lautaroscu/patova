# M00 - Setup de Open Code, DeepSeek V4 Pro y Context7

## Objetivo del modulo

Preparar el entorno de trabajo y las reglas operativas para que el desarrollo del MVP sea consistente, reproducible y optimizado para DeepSeek V4 Pro.

Este modulo no debe implementar funcionalidades de producto. Su objetivo es dejar el repositorio listo para desarrollar con agentes.

## Context7 obligatorio

Antes de modificar archivos, el agente debe usar Context7 para confirmar la documentacion actual de las herramientas base.

Ejecutar, como minimo:

```text
context7.resolve-library-id("FastAPI")
context7.resolve-library-id("SQLAlchemy")
context7.resolve-library-id("Alembic")
context7.resolve-library-id("Pydantic")
context7.resolve-library-id("Docker Compose")
context7.resolve-library-id("Gradle")
context7.resolve-library-id("Android Kotlin")
```

Luego usar `context7.get-library-docs` sobre los IDs resueltos. Rutas sugeridas si existen en Context7:

```text
/tiangolo/fastapi
/sqlalchemy/sqlalchemy
/sqlalchemy/alembic
/pydantic/pydantic
/docker/docs
/docker/compose
/gradle/gradle
/JetBrains/kotlin
```

Si una ruta sugerida no existe, usar siempre el resultado oficial o mas confiable devuelto por `resolve-library-id`. No inventar APIs por memoria.

## Prerrequisitos a verificar

El agente debe inspeccionar y documentar si estan disponibles:

```bash
git --version
python --version
python -m pip --version
docker --version
docker compose version
java -version
```

Para Android, si el entorno lo permite:

```bash
adb version
```

Si falta una herramienta, no improvisar instalacion global destructiva. Crear una seccion `docs/runbooks/INSTALACION_ENTORNO.md` con instrucciones por sistema operativo.

## Estructura inicial a crear

Crear carpetas base:

```text
backend/
android/
scripts/
infra/
docs/
docs/decisions/
docs/runbooks/
```

Crear archivos iniciales:

```text
.gitignore
README.md
docs/decisions/ADR-0001-stack-mvp.md
docs/runbooks/INSTALACION_ENTORNO.md
```

## Contenido minimo de `.gitignore`

Debe excluir:

```gitignore
# Python
.venv/
__pycache__/
*.pyc
.pytest_cache/
.ruff_cache/
.mypy_cache/

# Env
.env
.env.*
!.env.example

# Android / Gradle
.gradle/
build/
local.properties
*.apk
*.aab

# IDE
.idea/
.vscode/

# OS
.DS_Store
Thumbs.db

# Data local
scripts/data_samples/raw/
scripts/data_samples/processed/
```

## README inicial

El `README.md` debe explicar:

- Que es NumGuard.
- Alcance del MVP.
- Estructura del monorepo.
- Comandos de desarrollo, aunque algunos queden como pendientes hasta modulos posteriores.
- Regla de no subir `.env` ni datasets crudos sensibles.

## ADR inicial

Crear `docs/decisions/ADR-0001-stack-mvp.md` con:

- Decision: monorepo modular.
- Backend: Python + FastAPI.
- DB: PostgreSQL.
- Cache/queue: Redis.
- Android: Kotlin nativo.
- Infra local: Docker Compose.
- Razon: MVP rapido, simple, con posibilidad de evolucionar a ML en Python despues.
- Consecuencia: no usar microservicios en el MVP.

## Instrucciones optimizadas para DeepSeek V4 Pro

Agregar al README esta regla para agentes:

```text
Regla para agentes Open Code / DeepSeek:
Trabajar modulo por modulo. Antes de usar una libreria, consultar Context7. No asumir APIs por memoria. No modificar archivos fuera del alcance del modulo salvo que sea estrictamente necesario. Ejecutar pruebas despues de cada cambio funcional. Entregar resumen final con archivos modificados y comandos ejecutados.
```

## Criterios de aceptacion

- Existe estructura base del monorepo.
- Existe `.gitignore` adecuado.
- Existe README inicial.
- Existe ADR-0001.
- Existe runbook de instalacion con estado de herramientas detectadas.
- No se instalaron dependencias del producto todavia.
- No se agregaron secretos ni datasets reales al repositorio.

## Comandos de verificacion

```bash
find . -maxdepth 3 -type f | sort
```

Revisar manualmente que no existan archivos `.env`, binarios pesados ni datasets crudos versionados.
