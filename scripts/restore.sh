#!/bin/bash
set -euo pipefail

BACKUP_PATH="${1:?'Kullanim: restore.sh <backup_dizini>'}"

mongorestore \
  --uri="${MONGO_URI:-mongodb://admin:changeme@localhost:27017/optinms_db?authSource=admin}" \
  --dir="$BACKUP_PATH" \
  --gzip \
  --drop

echo "Restore tamamlandi: $BACKUP_PATH"
