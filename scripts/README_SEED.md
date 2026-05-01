# Seed Data Pipeline

## Datos de muestra

Los archivos en `scripts/data_samples/` contienen datos ficticios para pruebas:

- `prefixes_sample.csv`: 3 prefijos de area argentinos de ejemplo (CABA, La Plata, Cordoba).
- `numbers_clean_sample.csv`: 2 numeros limpios seed.
- `numbers_spam_sample.csv`: 2 numeros spam seed con score alto.

## Como cargar los samples

Desde la raiz del repo, con PostgreSQL corriendo via Docker Compose:

```bash
# Asegurar que la base de datos este corriendo
docker compose -f infra/docker-compose.yml up -d

# Aplicar migraciones (si no se hizo antes)
cd backend && alembic upgrade head && cd ..

# Importar prefijos
python scripts/seed_import.py import-prefixes --file scripts/data_samples/prefixes_sample.csv

# Importar numeros limpios
python scripts/seed_import.py import-numbers --file scripts/data_samples/numbers_clean_sample.csv --default-status CLEAN

# Importar numeros spam
python scripts/seed_import.py import-numbers --file scripts/data_samples/numbers_spam_sample.csv --default-status SPAM

# Validar archivo
python scripts/seed_import.py validate-file --file scripts/data_samples/numbers_clean_sample.csv
```

## Como usar datasets reales

1. Obtener el dataset real de ENACOM u otras fuentes en formato CSV.
2. Ubicarlo en `scripts/data_samples/` (carpeta ignorada por Git para archivos raw).
3. Asegurar que el CSV tenga las columnas esperadas segun el contrato de cada comando.
4. Ejecutar el comando correspondiente apuntando al archivo real.

### Formato esperado para prefijos

```csv
prefix,city,province,operator,is_mobile,is_valid
011,Buenos Aires,CABA,Movistar,false,true
```

### Formato esperado para numeros clean

```csv
number,source,metadata_name,metadata_type
+541112345678,ENACOM,Nombre Entidad,company
```

### Formato esperado para numeros spam

```csv
number,source,report_count,spam_score
+541199999999,CROWDSOURCE,80,90
```

## Idempotencia

El pipeline usa upsert (INSERT ... ON CONFLICT DO UPDATE). Esto significa que:

- Ejecutar el mismo archivo dos veces no duplica registros.
- Si un registro ya existe, se actualizan sus campos (metadatos, score, status).
- Los conteos del CLI muestran cuantos registros se insertaron y cuantos se actualizaron.

## Privacidad

- Los datasets reales NO deben versionarse en Git.
- El directorio `scripts/data_samples/raw/` esta en `.gitignore`.
- Los logs muestran hashes truncados de numeros, nunca numeros completos.
