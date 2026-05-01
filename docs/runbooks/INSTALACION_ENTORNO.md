# Instalacion del entorno de desarrollo

## Herramientas detectadas

| Herramienta | Estado | Version |
|---|---|---|
| git | Disponible | 2.53.0 |
| python | Disponible | 3.14.0 |
| pip | Disponible | 25.3 |
| docker | Disponible | 29.4.0 |
| docker compose | Disponible | v5.1.2 |
| java (OpenJDK) | Disponible | 17.0.18 LTS |
| adb | NO disponible | - |

## Context7 - IDs de librerias resueltos

| Libreria | ID Context7 | Snippets | Reputacion | Benchmark |
|---|---|---|---|---|
| FastAPI | `/fastapi/fastapi` | 1048 | High | 84.52 |
| SQLAlchemy | `/websites/sqlalchemy_en_20` | 18308 | High | 78.89 |
| Alembic | `/websites/alembic_sqlalchemy` | 3039 | High | 84.80 |
| Pydantic | `/pydantic/pydantic` | 755 | High | 84.31 |
| Docker Compose | `/docker/compose` | 138 | High | 69.13 |
| Gradle | `/gradle/gradle` | 4403 | High | 74.53 |
| Kotlin | `/websites/kotlinlang` | 11908 | High | 79.19 |

### Notas sobre rutas sugeridas en M00

| Ruta sugerida | Estado | ID real usado |
|---|---|---|
| `/tiangolo/fastapi` | No existe en Context7 | `/fastapi/fastapi` |
| `/sqlalchemy/sqlalchemy` | Existe pero reputacion Low | `/websites/sqlalchemy_en_20` (High) |
| `/sqlalchemy/alembic` | Existe pero reputacion Low | `/websites/alembic_sqlalchemy` (High) |
| `/pydantic/pydantic` | OK | `/pydantic/pydantic` |
| `/docker/docs` | No resuelto para Docker Compose | `/docker/compose` |
| `/docker/compose` | OK | `/docker/compose` |
| `/gradle/gradle` | OK | `/gradle/gradle` |
| `/JetBrains/kotlin` | Existe pero benchmark bajo (64.15) | `/websites/kotlinlang` (79.19) |

## Notas

- **Context7**: Verificado y funcionando (abril 2025). Todas las librerias requeridas tienen IDs validos en Context7.
- **adb**: No detectado. Necesario para despliegue en dispositivo Android. Instalar via Android SDK Platform Tools.

## Instalacion por sistema operativo

### Windows

```powershell
# Python
winget install Python.Python.3.14

# Docker
winget install Docker.DockerDesktop

# Java (OpenJDK 17)
winget install Microsoft.OpenJDK.17

# Android SDK Platform Tools (adb)
# Descargar desde: https://developer.android.com/studio/releases/platform-tools
# Extraer y agregar al PATH

# Git
winget install Git.Git
```

### macOS

```bash
# Python
brew install python@3.14

# Docker
brew install --cask docker

# Java
brew install openjdk@17

# Android Platform Tools
brew install android-platform-tools

# Git
brew install git
```

### Linux (Ubuntu/Debian)

```bash
# Python
sudo apt install python3.14 python3-pip

# Docker
# Seguir: https://docs.docker.com/engine/install/ubuntu/

# Java
sudo apt install openjdk-17-jdk

# Android Platform Tools
sudo apt install android-sdk-platform-tools

# Git
sudo apt install git
```
