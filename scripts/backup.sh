#!/bin/bash
set -euo pipefail

BACKUP_DIR="${BACKUP_ROOT:-/backup}/optinms_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

mongodump \
  --uri="${MONGO_URI:-mongodb://admin:changeme@localhost:27017/optinms_db?authSource=admin}" \
  --out="$BACKUP_DIR" \
  --gzip

echo "Backup tamamlandi: $BACKUP_DIR"

# 30 gundan eski backup'lari sil
find "${BACKUP_ROOT:-/backup}" -maxdepth 1 -name "optinms_*" -mtime +30 -exec rm -rf {} +
