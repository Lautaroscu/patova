#!/usr/bin/env bash
set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-patova-postgres-1}"
DB_NAME="${DB_NAME:-numguard}"
DB_USER="${DB_USER:-numguard}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/numguard}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
DATE_STAMP="$(date -u +%Y%m%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/numguard_${DATE_STAMP}.sql.gz"

if [ ! -d "${BACKUP_DIR}" ]; then
    mkdir -p "${BACKUP_DIR}"
    chmod 700 "${BACKUP_DIR}"
fi

echo "[$(date -Iseconds)] Starting NumGuard PostgreSQL hot backup..."

docker exec "${DB_CONTAINER}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" --no-owner --no-acl --clean --if-exists | gzip > "${BACKUP_FILE}"

BACKUP_SIZE=$(wc -c < "${BACKUP_FILE}")
echo "[$(date -Iseconds)] Backup created: ${BACKUP_FILE} (${BACKUP_SIZE} bytes)"

echo "[$(date -Iseconds)] Cleaning backups older than ${BACKUP_RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -name "numguard_*.sql.gz" -type f -mtime "+${BACKUP_RETENTION_DAYS}" -delete

BACKUP_COUNT=$(find "${BACKUP_DIR}" -name "numguard_*.sql.gz" -type f | wc -l)
echo "[$(date -Iseconds)] Done. ${BACKUP_COUNT} backup(s) retained in ${BACKUP_DIR}."
