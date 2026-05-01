# M03 - Pipeline de datos y seed ENACOM

## Objetivo del modulo

Implementar scripts de importacion, normalizacion y carga inicial de datos para prefijos y numeros seed.

El objetivo tecnico de este modulo es que el sistema pueda cargar:

- Prefijos de area de Argentina.
- Numeros limpios/validados seed.
- Numeros spam seed de fuentes publicas o muestras controladas.

No se debe depender de que los datasets definitivos ya esten disponibles. El modulo debe incluir datos de muestra pequenos para pruebas y permitir reemplazarlos por CSV/XLS reales.

## Context7 obligatorio

Antes de escribir codigo, usar Context7:

```text
context7.resolve-library-id("Pandas")
context7.resolve-library-id("phonenumbers Python")
context7.resolve-library-id("Typer Python")
context7.resolve-library-id("SQLAlchemy")
```

Rutas sugeridas si existen:

```text
/pandas-dev/pandas
/daviddrysdale/python-phonenumbers
/fastapi/typer
/sqlalchemy/sqlalchemy
```

Topics sugeridos:

```text
Pandas: read_csv, chunksize, dtype handling
phonenumbers: parse, is_valid_number, format E164, Argentina region
Typer: CLI commands and options
SQLAlchemy: bulk insert async or core insert
```

## Dependencias a agregar

En `backend/pyproject.toml` o en un `scripts/requirements` si se separa:

```toml
"pandas>=2.2",
"phonenumbers>=8.13",
"typer>=0.12",
"openpyxl>=3.1"
```

Agregar a dev:

```toml
"pytest-cov>=5.0"
```

## Estructura a crear

```text
scripts/
  seed_import.py
  README_SEED.md
  data_samples/
    prefixes_sample.csv
    numbers_clean_sample.csv
    numbers_spam_sample.csv
backend/src/numguard/services/
  phone_normalization.py
  prefix_matching.py
backend/tests/test_phone_normalization.py
backend/tests/test_seed_import.py
```

## Contratos de entrada CSV

### `prefixes_sample.csv`

```csv
prefix,city,province,operator,is_mobile,is_valid
011,Buenos Aires,CABA,,false,true
0221,La Plata,Buenos Aires,,false,true
0351,Cordoba,Cordoba,,false,true
```

### `numbers_clean_sample.csv`

```csv
number,source,metadata_name,metadata_type
+541112345678,SEED,Empresa Demo,company
+542214567890,SEED,Comercio Demo,company
```

### `numbers_spam_sample.csv`

```csv
number,source,report_count,spam_score
+541199999999,SEED,80,90
+543511111111,SEED,65,85
```

## Funciones obligatorias

### `normalize_to_e164(raw_number: str, region: str = "AR") -> str | None`

- Debe aceptar formatos con espacios, guiones, parentesis y prefijos locales.
- Debe devolver E.164 si el numero es valido o plausible segun `phonenumbers`.
- Debe devolver `None` si no se puede normalizar.

### `extract_argentina_prefix(number_e164: str) -> str | None`

- Debe mapear el numero E.164 a un prefijo local candidato.
- Debe contemplar moviles con `+54 9`.
- Debe ser testeable con ejemplos de CABA, La Plata y Cordoba.
- Si hay ambiguedad, documentar limitacion y no inventar prefijo.

### CLI `seed_import.py`

Comandos:

```bash
python scripts/seed_import.py import-prefixes --file scripts/data_samples/prefixes_sample.csv
python scripts/seed_import.py import-numbers --file scripts/data_samples/numbers_clean_sample.csv --default-status CLEAN
python scripts/seed_import.py import-numbers --file scripts/data_samples/numbers_spam_sample.csv --default-status SPAM
python scripts/seed_import.py validate-file --file <csv>
```

## Reglas de privacidad y datos

- No versionar datasets crudos grandes.
- No subir dumps de DB.
- Los datos de muestra deben ser ficticios o claramente de prueba.
- El pipeline debe soportar fuentes reales, pero no debe incluirlas en Git.
- Documentar en `scripts/README_SEED.md` como descargar y ubicar datasets reales manualmente.

## Reglas de implementacion

- Procesar CSV por chunks para archivos grandes.
- Hacer upsert por `prefix` para prefijos.
- Hacer upsert por `number_e164` para numeros.
- Registrar conteos: filas leidas, normalizadas, invalidas, insertadas, actualizadas.
- Evitar logs con exceso de numeros completos. Para debug, truncar o hashear.
- No requerir ENACOM real para que los tests pasen.

## Tests requeridos

- Normalizacion de numero E.164.
- Normalizacion de formatos locales argentinos.
- Rechazo de datos invalidos.
- Import de prefijos sample.
- Import de numeros clean sample.
- Import de spam sample con score/status correctos.
- Idempotencia: correr dos veces no duplica registros.

## Comandos esperados

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend
alembic upgrade head
pytest tests/test_phone_normalization.py tests/test_seed_import.py
cd ..
python scripts/seed_import.py import-prefixes --file scripts/data_samples/prefixes_sample.csv
python scripts/seed_import.py import-numbers --file scripts/data_samples/numbers_clean_sample.csv --default-status CLEAN
python scripts/seed_import.py import-numbers --file scripts/data_samples/numbers_spam_sample.csv --default-status SPAM
```

## Criterios de aceptacion

- Existe CLI funcional para seed.
- Los datos sample cargan en PostgreSQL.
- El pipeline es idempotente.
- Los tests pasan.
- La documentacion indica como reemplazar samples por datasets reales.
- No hay datasets reales ni secretos versionados.

## Prompt recomendado para Open Code

```text
Implementa solo el Modulo M03. Usa Context7 para pandas, phonenumbers, Typer y SQLAlchemy antes de escribir codigo. Crea normalizacion telefonica Argentina, matching de prefijos, CLI de seed y CSV samples ficticios. No implementes todavia /v1/validate. Asegura idempotencia y tests. Ejecuta pytest y una carga sample local. Entrega resumen de comandos y limitaciones detectadas en prefijos argentinos.
```
