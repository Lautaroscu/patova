#!/usr/bin/env bash
set -euo pipefail

# Apply database migrations (Alembic)
# The DATABASE_URL (or SQLALCHEMY_DATABASE_URL) is provided via environment variable / secret
if command -v alembic >/dev/null 2>&1; then
  echo "▶️ Ejecutando migraciones Alembic..."
  alembic upgrade head
else
  echo "⚠️ Alembic no está disponible en PATH. Saltando migraciones."
fi

# Start FastAPI server
exec uvicorn patova.main:create_app --factory --host 0.0.0.0 --port ${PORT:-8080} --log-level info
