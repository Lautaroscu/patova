# M10 - Testing, carga, E2E y quality gates

## Objetivo del modulo

Consolidar calidad tecnica del MVP antes de beta:

- Tests unitarios backend y Android.
- Tests de integracion backend con PostgreSQL y Redis.
- Tests de contrato API.
- Prueba de carga con Locust.
- Quality gates de lint, tipos y seguridad basica.
- Checklist E2E manual en dispositivo Android real.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("Pytest")
context7.resolve-library-id("Testcontainers Python")
context7.resolve-library-id("Locust")
context7.resolve-library-id("Ruff")
context7.resolve-library-id("Mypy")
context7.resolve-library-id("JUnit Android")
context7.resolve-library-id("MockK")
```

Rutas sugeridas si existen:

```text
/pytest-dev/pytest
/testcontainers/testcontainers-python
/locustio/locust
/astral-sh/ruff
/python/mypy
/mockk/mockk
```

Topics sugeridos:

```text
Pytest: fixtures, markers, tmp env
Testcontainers: postgres, redis containers
Locust: headless load test, percentiles
Ruff: configuration pyproject
Mypy: strictness gradual
Android testing: local unit tests, fake repositories
MockK: coroutine mocks
```

## Backend quality gates

Agregar o completar configuracion en `backend/pyproject.toml`:

```toml
[tool.ruff]
line-length = 100

[tool.ruff.lint]
select = ["E", "F", "I", "B", "UP", "ASYNC"]

[tool.pytest.ini_options]
addopts = "-q"
testpaths = ["tests"]

[tool.mypy]
python_version = "3.12"
ignore_missing_imports = true
warn_unused_configs = true
```

No activar `mypy --strict` si genera demasiada friccion inicial. Usar gradual.

## Tests backend minimos

Asegurar cobertura para:

- Health/auth.
- Migraciones y modelos.
- Normalizacion telefonica.
- Seed idempotente.
- `/v1/validate` completo.
- Cache Redis.
- Reportes/scoring/anti-abuso.
- Stats/admin.

Crear markers:

```text
unit
integration
load
```

## Tests de contrato API

Crear archivo:

```text
backend/tests/contracts/test_validate_contract.py
```

Validar que la respuesta de `/v1/validate` siempre contiene:

```text
verdict
spam_score
reason
report_count
prefix_valid
prefix_zone
operator
cached
latency_ms
```

Aunque algunos sean `null`, el contrato debe ser estable para Android.

## Prueba de carga

Crear script o Make target:

```bash
make load-test
```

Debe ejecutar Locust headless contra backend local.

Escenarios:

1. 80% numeros repetidos para medir cache hit.
2. 20% numeros no cacheados.
3. 5% prefijos invalidos.

Targets:

```text
p95 < 200 ms local con dataset sample
p99 < 500 ms local con dataset sample
0 errores 5xx
cache hit > 80% en escenario repetido
```

## Android quality gates

Desde `android/`:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Tests minimos:

- Use case validacion con fake API.
- Fail-open ante timeout.
- TTL cache por verdict.
- Reporte pendiente offline.
- Enmascaramiento de numeros.

## E2E manual en dispositivo real

Crear runbook:

```text
docs/runbooks/E2E_ANDROID_REAL_DEVICE.md
```

Checklist:

1. Instalar APK debug.
2. Configurar API local accesible desde dispositivo o entorno staging.
3. Activar NumGuard como app de screening.
4. Seedear numero spam conocido.
5. Realizar llamada desde numero spam test.
6. Verificar bloqueo/rechazo.
7. Verificar notificacion.
8. Verificar historial.
9. Marcar falso positivo.
10. Verificar que backend recibe feedback.
11. Apagar red y repetir llamada.
12. Confirmar fail-open.

## CI local antes de push

Crear `Makefile` o scripts equivalentes:

```makefile
backend-test:
	cd backend && pytest

backend-lint:
	cd backend && ruff check .

android-test:
	cd android && ./gradlew testDebugUnitTest

android-build:
	cd android && ./gradlew assembleDebug

load-test:
	cd backend && locust -f tests/load/locustfile_validate.py --headless -u 50 -r 5 -t 1m --host http://localhost:8000
```

Adaptar en Windows si no se usa Make.

## Criterios de aceptacion

- Existe suite de tests organizada.
- Existe prueba de carga reproducible.
- Existe runbook E2E.
- Backend lint/test pasa.
- Android build/test pasa si el entorno Android esta disponible.
- Contrato API estable para Android.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M10. Usa Context7 para pytest, testcontainers, locust, ruff, mypy y testing Android. No agregues features nuevas. Consolidá tests, quality gates, load test y runbook E2E. Ejecuta los comandos posibles en el entorno y reporta los que no se pudieron ejecutar con causa concreta.
```
