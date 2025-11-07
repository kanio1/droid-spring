#!/bin/bash
# PostgreSQL Backup Script
# Creates full base backup and manages WAL archives

set -euo pipefail

# Configuration
BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
S3_BUCKET="${S3_BACKUP_BUCKET:-bss-postgres-backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"

# Create backup directory
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting PostgreSQL backup..."

# 1. Create base backup using pg_basebackup
echo "[$(date)] Creating base backup..."
pg_basebackup -h localhost -U postgres -D "$BACKUP_DIR/base_$DATE" -Ft -z -P -W

# 2. Upload to S3
echo "[$(date)] Uploading to S3..."
aws s3 sync "$BACKUP_DIR/base_$DATE" "s3://$S3_BUCKET/base/$DATE" --storage-class STANDARD_IA

# 3. Archive current WAL
echo "[$(date)] Archiving WAL files..."
sudo -u postgres psql -c "SELECT pg_switch_wal();"

# 4. Upload WAL files
echo "[$(date)] Uploading WAL files..."
aws s3 sync /var/lib/postgresql/wal_archive "s3://$S3_BUCKET/wal" --storage-class STANDARD_IA

# 5. Cleanup local backup (keep only latest locally)
echo "[$(date)] Cleaning up local backups..."
find "$BACKUP_DIR" -type d -name "base_*" -mtime +1 -exec rm -rf {} +

# 6. Cleanup old S3 backups
echo "[$(date)] Cleaning up old S3 backups (older than $RETENTION_DAYS days)..."
aws s3api list-objects-v2 \
  --bucket "$S3_BUCKET" \
  --prefix "base/" \
  --query "Contents[?LastModified<='$(date -d "$RETENTION_DAYS days ago" --iso-8601)'].Key" \
  --output text | xargs -I {} aws s3 rm "s3://$S3_BUCKET/{}"

echo "[$(date)] Backup completed successfully!"

# 7. Verification
echo "[$(date)] Verifying backup..."
if aws s3 ls "s3://$S3_BUCKET/base/$DATE/"; then
    echo "[$(date)] ✓ Backup verified successfully"
else
    echo "[$(date)] ✗ Backup verification failed!"
    exit 1
fi
