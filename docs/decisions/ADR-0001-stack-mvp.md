# ADR-0001: Stack tecnologico del MVP

## Estado

Aceptado

## Contexto

Se requiere construir un MVP de NumGuard que demuestre el flujo completo de screening de llamadas, desde Android hasta una API de validacion con cache, reportes de usuarios y panel admin.

## Decision

- **Arquitectura**: Monorepo modular.
- **Backend**: Python + FastAPI.
- **Base de datos**: PostgreSQL.
- **Cache y cola**: Redis.
- **Android**: Kotlin nativo.
- **Infraestructura local**: Docker Compose.

## Razon

MVP rapido, simple, con posibilidad de evolucionar a ML en Python despues. El monorepo permite mantener todo el codigo versionado en conjunto sin complejidad de microservicios.

## Consecuencias

- No se usaran microservicios en el MVP. El backend es un monolito modular.
- Se usara un solo `docker-compose.yml` local con PostgreSQL, Redis y el backend.
- Android se compila con Gradle nativo, sin Expo ni React Native.
- La cache y scoring de spam usaran Redis, sin necesidad de infraestructura adicional.
