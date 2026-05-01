# NumGuard

Sistema de screening y validacion de llamadas telefonicas entrantes.

## Alcance del MVP

El MVP demuestra el flujo completo:

1. Llamada entrante en Android.
2. `CallScreeningService` consulta API de validacion.
3. API responde `ALLOW`, `SUSPECT`, `BLOCK`, `UNKNOWN` o `INVALID_PREFIX`.
4. Android permite o bloquea con estrategia `fail-open` ante error/timeout.
5. Reportes de usuarios retroalimentan el score de spam.
6. Panel admin con metricas basicas.

Fuera del alcance inicial: iOS, modelos ML, agentes IA de enriquecimiento, integracion con operadoras, API publica B2B.

## Estructura del monorepo

```
numguard/
  backend/          # Python + FastAPI
  android/          # Kotlin nativo
  scripts/          # Seed de datos, samples
  infra/            # Docker Compose, nginx, CI
  docs/             # ADRs, runbooks
```

## Comandos de desarrollo

> Los comandos se habilitaran progresivamente segun avancen los modulos.

```bash
# Backend (pendiente - M01)
# cd backend && python -m venv .venv && source .venv/bin/activate && pip install -e ".[dev]"

# Infra local (pendiente - M11)
# docker compose up -d

# Android (pendiente - M07)
# cd android && ./gradlew assembleDebug

# Tests (pendiente - M10)
# cd backend && pytest
```

## Reglas

- No subir `.env` ni datasets crudos sensibles.
- Todo secreto va a `.env.example` solo con placeholders.

## Regla para agentes Open Code / DeepSeek

Trabajar modulo por modulo. Antes de usar una libreria, consultar Context7. No asumir APIs por memoria. No modificar archivos fuera del alcance del modulo salvo que sea estrictamente necesario. Ejecutar pruebas despues de cada cambio funcional. Entregar resumen final con archivos modificados y comandos ejecutados.
