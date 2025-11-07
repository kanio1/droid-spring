#!/bin/bash
################################################################################
# PostgreSQL Restore Script for BSS System
# Supports: compressed, encrypted, and S3 backups
################################################################################

set -e

# Configuration
CONTAINER_NAME="bss-postgres"
DB_NAME="${POSTGRES_DB:-bss}"
DB_USER="${POSTGRES_USER:-bss_app}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Show help
show_help() {
    cat << EOF
PostgreSQL Restore Script for BSS

USAGE:
    $0 [OPTIONS] BACKUP_FILE

DESCRIPTION:
    Restores PostgreSQL database from a backup file created by postgres-backup.sh

OPTIONS:
    --target-db NAME       Target database name (default: bss)
    --drop-existing        Drop existing database before restore
    --no-owner             Restore without ownership
    --encrypt-key KEY      Decryption key for encrypted backups
    --list-tables          List tables in backup without restoring
    --dry-run              Show what would be restored without executing
    --help                 Show this help

EXAMPLES:
    # Restore from local backup
    $0 /var/lib/postgresql/backups/bss_20241106_120000.sql.gz

    # Restore with custom database name
    $0 --target-db bss_test /path/to/backup.sql.gz

    # Drop existing database before restore
    $0 --drop-existing /path/to/backup.sql.gz

    # Decrypt and restore encrypted backup
    $0 --encrypt-key "my-secret-key" /path/to/backup.sql.gz.gpg

    # List tables in backup
    $0 --list-tables /path/to/backup.dump

    # Dry run (show what would be restored)
    $0 --dry-run /path/to/backup.dump

ENVIRONMENT VARIABLES:
    POSTGRES_DB              Database name (default: bss)
    POSTGRES_USER            Database user (default: bss_app)
    POSTGRES_PASSWORD        Database password (required)

EOF
}

# Parse arguments
TARGET_DB="$DB_NAME"
DROP_EXISTING=false
NO_OWNER=false
ENCRYPTION_KEY=""
LIST_TABLES=false
DRY_RUN=false
BACKUP_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --target-db)
            TARGET_DB="$2"
            shift 2
            ;;
        --drop-existing)
            DROP_EXISTING=true
            shift
            ;;
        --no-owner)
            NO_OWNER=true
            shift
            ;;
        --encrypt-key)
            ENCRYPTION_KEY="$2"
            shift 2
            ;;
        --list-tables)
            LIST_TABLES=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help|-h)
            show_help
            exit 0
            ;;
        -*)
            error "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            BACKUP_FILE="$1"
            shift
            ;;
    esac
done

# Validate input
if [ -z "$BACKUP_FILE" ]; then
    error "Backup file path is required"
    show_help
    exit 1
fi

if [ -z "$POSTGRES_PASSWORD" ]; then
    error "POSTGRES_PASSWORD environment variable is required!"
    exit 1
fi

export PGPASSWORD="$POSTGRES_PASSWORD"

# Check if backup file exists
if [[ ! "$BACKUP_FILE" =~ ^s3:// ]] && [ ! -f "$BACKUP_FILE" ]; then
    error "Backup file not found: $BACKUP_FILE"
    exit 1
fi

# Download from S3 if needed
TEMP_BACKUP_FILE=""
if [[ "$BACKUP_FILE" =~ ^s3:// ]]; then
    log "Downloading backup from S3..."
    TEMP_BACKUP_FILE="/tmp/$(basename "$BACKUP_FILE")"
    aws s3 cp "$BACKUP_FILE" "$TEMP_BACKUP_FILE"
    BACKUP_FILE="$TEMP_BACKUP_FILE"
    log "Download completed: $BACKUP_FILE"
fi

# Prepare file for restore
PREPARED_FILE="$BACKUP_FILE"
TEMP_FILES=()

cleanup() {
    log "Cleaning up temporary files..."
    rm -f "${TEMP_FILES[@]}"
}
trap cleanup EXIT

# Decrypt if encrypted
if [[ "$BACKUP_FILE" == *.gpg ]]; then
    if [ -z "$ENCRYPTION_KEY" ]; then
        error "Encrypted backup detected. Please provide --encrypt-key"
        exit 1
    fi

    log "Decrypting backup..."
    PREPARED_FILE="${BACKUP_FILE%.gpg}"
    gpg --decrypt --batch --yes --passphrase "$ENCRYPTION_KEY" "$BACKUP_FILE" > "$PREPARED_FILE" 2>/dev/null
    TEMP_FILES+=("$PREPARED_FILE")
    log "Decryption completed"
fi

# Decompress if compressed
if [[ "$PREPARED_FILE" == *.gz ]]; then
    log "Decompressing backup..."
    DECOMPRESSED_FILE="${PREPARED_FILE%.gz}"
    gunzip -c "$PREPARED_FILE" > "$DECOMPRESSED_FILE"
    PREPARED_FILE="$DECOMPRESSED_FILE"
    TEMP_FILES+=("$PREPARED_FILE")
    log "Decompression completed"
fi

# Get file info
log "Backup file info:"
log "  Path: $BACKUP_FILE"
log "  Size: $(du -h "$BACKUP_FILE" | cut -f1)"
log "  Type: $(file -b "$BACKUP_FILE")"

# List tables if requested
if [ "$LIST_TABLES" = true ]; then
    log "Tables in backup:"
    if [[ "$PREPARED_FILE" == *.dump ]]; then
        docker exec "$CONTAINER_NAME" pg_restore --list "$PREPARED_FILE" 2>/dev/null | grep "TABLE DATA" || \
            pg_restore --list "$PREPARED_FILE" 2>/dev/null | grep "TABLE DATA"
    else
        # For SQL dumps, use grep
        grep "COPY " "$PREPARED_FILE" | awk '{print $2}' | cut -d'(' -f1 | sort -u
    fi
    exit 0
fi

# Confirm restore
warn "This will restore database: $TARGET_DB"
warn "From backup: $(basename "$BACKUP_FILE")"

if [ "$DRY_RUN" = true ]; then
    log "DRY RUN MODE - No changes will be made"
    log "Backup can be restored with: $0 $@"
    exit 0
fi

read -p "Continue? (yes/no): " -r
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    log "Restore cancelled"
    exit 0
fi

# Drop database if requested
if [ "$DROP_EXISTING" = true ]; then
    log "Dropping existing database: $TARGET_DB"
    docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -c "DROP DATABASE IF EXISTS $TARGET_DB" 2>/dev/null || true
    docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -c "CREATE DATABASE $TARGET_DB" 2>/dev/null || true
fi

# Perform restore
log "Starting restore to database: $TARGET_DB"

RESTORE_CMD=""
if [[ "$PREPARED_FILE" == *.dump ]]; then
    # Custom format dump
    RESTORE_CMD="pg_restore"
    if [ "$NO_OWNER" = true ]; then
        RESTORE_CMD="$RESTORE_CMD --no-owner"
    fi
    if [ "$TARGET_DB" != "$DB_NAME" ]; then
        RESTORE_CMD="$RESTORE_CMD --dbname=$TARGET_DB"
    else
        RESTORE_CMD="$RESTORE_CMD --dbname=$TARGET_DB"
    fi
else
    # SQL dump
    RESTORE_CMD="psql"
    if [ "$TARGET_DB" != "$DB_NAME" ]; then
        RESTORE_CMD="$RESTORE_CMD -d $TARGET_DB"
    fi
fi

log "Executing: $RESTORE_CMD"

if [[ "$PREPARED_FILE" == *.dump ]]; then
    if [ "$DRY_RUN" = false ]; then
        # For custom format, we need to copy to container first
        docker cp "$PREPARED_FILE" "$CONTAINER_NAME:/tmp/restore_$$.dump"
        docker exec "$CONTAINER_NAME" \
            pg_restore \
            --username="$DB_USER" \
            --no-password \
            --verbose \
            --clean \
            --if-exists \
            --dbname="$TARGET_DB" \
            /tmp/restore_$$.dump
    fi
else
    # For SQL dumps
    if [ "$DRY_RUN" = false ]; then
        cat "$PREPARED_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$TARGET_DB"
    fi
fi

if [ $? -eq 0 ]; then
    log "✓ Restore completed successfully!"
    log "Database restored: $TARGET_DB"
    log "Tables count: $(docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$TARGET_DB" -t -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'" 2>/dev/null | xargs)"
else
    error "Restore failed!"
    exit 1
fi
