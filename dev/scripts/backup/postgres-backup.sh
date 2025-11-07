#!/bin/bash
################################################################################
# PostgreSQL Backup Script for BSS System
# Supports: pg_dump, compression, retention, and cloud storage
################################################################################

set -e

# Configuration
CONTAINER_NAME="bss-postgres"
BACKUP_DIR="/var/lib/postgresql/backups"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="${POSTGRES_DB:-bss}"
DB_USER="${POSTGRES_USER:-bss_app}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
S3_BUCKET="${BACKUP_S3_BUCKET:-}"
COMPRESSION="${BACKUP_COMPRESSION:-gzip}"
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Get PostgreSQL password from environment
export PGPASSWORD="$POSTGRES_PASSWORD"

# Main backup function
backup_postgres() {
    local backup_file="$BACKUP_DIR/${DB_NAME}_${DATE}.sql"

    log "Starting PostgreSQL backup..."

    # Check if PostgreSQL is accessible
    if ! docker exec "$CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
        error "PostgreSQL is not accessible!"
        exit 1
    fi

    # Perform backup
    log "Creating database dump: $DB_NAME"
    docker exec "$CONTAINER_NAME" pg_dump \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --verbose \
        --no-password \
        --format=custom \
        --compress=6 \
        --file=/tmp/backup_"$DATE".dump

    if [ $? -ne 0 ]; then
        error "Backup failed!"
        exit 1
    fi

    # Copy backup from container to host
    log "Copying backup from container..."
    docker cp "$CONTAINER_NAME:/tmp/backup_$DATE.dump" "$backup_file.dump"

    if [ "$COMPRESSION" = "gzip" ]; then
        log "Compressing backup..."
        gzip "$backup_file.dump"
        backup_file="$backup_file.dump.gz"
    fi

    # Encrypt backup if encryption key provided
    if [ -n "$ENCRYPTION_KEY" ]; then
        log "Encrypting backup..."
        gpg --cipher-algo AES256 --compress-algo 1 --symmetric --output "$backup_file.gpg" --batch --yes --passphrase "$ENCRYPTION_KEY" "$backup_file"
        rm "$backup_file"
        backup_file="$backup_file.gpg"
    fi

    # Calculate backup size
    local backup_size=$(du -h "$backup_file" | cut -f1)
    log "Backup completed successfully!"
    log "Backup file: $backup_file"
    log "Backup size: $backup_size"

    # Upload to S3 if configured
    if [ -n "$S3_BUCKET" ]; then
        upload_to_s3 "$backup_file"
    fi

    # Cleanup old backups
    cleanup_old_backups

    # Return backup file path for notification
    echo "$backup_file"
}

# Upload backup to S3
upload_to_s3() {
    local backup_file="$1"
    local backup_name=$(basename "$backup_file")

    log "Uploading to S3: s3://$S3_BUCKET/backups/$backup_name"

    if command -v aws &> /dev/null; then
        aws s3 cp "$backup_file" "s3://$S3_BUCKET/backups/$backup_name"
        log "Upload to S3 completed!"
    else
        warn "AWS CLI not found. Skipping S3 upload."
    fi
}

# Cleanup old backups based on retention policy
cleanup_old_backups() {
    log "Cleaning up backups older than $RETENTION_DAYS days..."

    find "$BACKUP_DIR" -type f -name "${DB_NAME}_*.sql*" -mtime +$RETENTION_DAYS -delete 2>/dev/null || true

    if [ -n "$S3_BUCKET" ] && command -v aws &> /dev/null; then
        log "Cleaning up old S3 backups..."
        aws s3 ls "s3://$S3_BUCKET/backups/" | awk '{print $4}' | grep "${DB_NAME}_" | while read -r backup; do
            aws s3api list-objects-v2 \
                --bucket "$S3_BUCKET" \
                --prefix "backups/$backup" \
                --query 'Contents[?LastModified<=`'$(date -d "$RETENTION_DAYS days ago" --iso-8601)`'`].[Key]' \
                --output text | xargs -I {} aws s3 rm "s3://$S3_BUCKET/{}" 2>/dev/null || true
        done
    fi

    log "Cleanup completed!"
}

# Verify backup integrity
verify_backup() {
    local backup_file="$1"

    log "Verifying backup integrity..."

    if [[ "$backup_file" == *.gpg ]]; then
        # Decrypt first
        log "Decrypting backup for verification..."
        gpg --decrypt --batch --yes --passphrase "$ENCRYPTION_KEY" "$backup_file" > "${backup_file%.gpg}" 2>/dev/null
        backup_file="${backup_file%.gpg}"
    fi

    if [[ "$backup_file" == *.gz ]]; then
        # Test gzip
        if ! gzip -t "$backup_file"; then
            error "Backup file is corrupted!"
            return 1
        fi
        log "✓ Backup compression is valid"
    fi

    if [[ "$backup_file" == *.dump ]]; then
        # Test pg_restore
        log "Testing pg_restore..."
        if ! pg_restore --list "$backup_file" > /dev/null 2>&1; then
            error "Backup file is corrupted!"
            return 1
        fi
        log "✓ Backup restore test passed"
    fi

    log "✓ Backup integrity verification passed"
}

# List all backups
list_backups() {
    log "Available backups:"

    echo ""
    find "$BACKUP_DIR" -type f -name "${DB_NAME}_*.sql*" -exec ls -lh {} \; | \
        awk '{printf "%-50s %10s\n", $9, $5}'
    echo ""

    if [ -n "$S3_BUCKET" ] && command -v aws &> /dev/null; then
        log "S3 backups:"
        aws s3 ls "s3://$S3_BUCKET/backups/" --human-readable --summarize | \
            grep "${DB_NAME}_" || echo "  No S3 backups found"
    fi
}

# Show help
show_help() {
    cat << EOF
PostgreSQL Backup Script for BSS

USAGE:
    $0 [COMMAND] [OPTIONS]

COMMANDS:
    backup         Create a new backup (default)
    list           List all available backups
    verify FILE    Verify backup integrity
    help           Show this help message

OPTIONS:
    --retention DAYS    Set retention period in days (default: 7)
    --s3-bucket BUCKET  Upload backups to S3
    --encrypt KEY       Encrypt backups with GPG

EXAMPLES:
    # Basic backup
    $0 backup

    # Backup with 14-day retention
    $0 backup --retention 14

    # Backup with S3 upload
    $0 backup --s3-bucket my-bss-backups

    # Backup with encryption
    $0 backup --encrypt "my-secret-key"

    # List all backups
    $0 list

    # Verify a backup
    $0 verify /var/lib/postgresql/backups/bss_20241106_120000.sql.gz

ENVIRONMENT VARIABLES:
    POSTGRES_DB              Database name (default: bss)
    POSTGRES_USER            Database user (default: bss_app)
    POSTGRES_PASSWORD        Database password
    BACKUP_RETENTION_DAYS    Retention in days (default: 7)
    BACKUP_S3_BUCKET         S3 bucket for uploads
    BACKUP_ENCRYPTION_KEY    GPG encryption key

EOF
}

# Parse arguments
COMMAND="backup"
BACKUP_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        backup)
            COMMAND="backup"
            shift
            ;;
        list)
            COMMAND="list"
            shift
            ;;
        verify)
            COMMAND="verify"
            BACKUP_FILE="$2"
            shift 2
            ;;
        --retention)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        --s3-bucket)
            S3_BUCKET="$2"
            shift 2
            ;;
        --encrypt)
            ENCRYPTION_KEY="$2"
            shift 2
            ;;
        help|-h|--help)
            show_help
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main execution
case $COMMAND in
    backup)
        if [ -z "$POSTGRES_PASSWORD" ]; then
            error "POSTGRES_PASSWORD environment variable is required!"
            exit 1
        fi
        backup_file=$(backup_postgres)
        verify_backup "$backup_file"
        log "✓ Backup process completed successfully"
        ;;
    list)
        list_backups
        ;;
    verify)
        if [ -z "$BACKUP_FILE" ]; then
            error "Backup file path is required for verify command"
            exit 1
        fi
        verify_backup "$BACKUP_FILE"
        ;;
    *)
        error "Unknown command: $COMMAND"
        show_help
        exit 1
        ;;
esac
