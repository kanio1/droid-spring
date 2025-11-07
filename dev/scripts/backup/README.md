# PostgreSQL Backup & Restore for BSS

This directory contains automated backup and restore scripts for PostgreSQL database.

## Scripts

- `postgres-backup.sh` - Automated backup script
- `postgres-restore.sh` - Database restore script

## Quick Start

### Create a Backup

```bash
# Basic backup (creates .dump.gz file)
./postgres-backup.sh backup

# Backup with custom retention (14 days)
./postgres-backup.sh backup --retention 14

# Backup with S3 upload
./postgres-backup.sh backup --s3-bucket my-bss-backups

# Backup with encryption
./postgres-backup.sh backup --encrypt "my-secret-key"
```

### List Available Backups

```bash
./postgres-backup.sh list
```

### Restore a Backup

```bash
# Restore from local backup
./postgres-restore.sh /var/lib/postgresql/backups/bss_20241106_120000.sql.gz

# Drop existing database and restore
./postgres-restore.sh --drop-existing /path/to/backup.dump

# Restore with custom database name
./postgres-restore.sh --target-db bss_test /path/to/backup.dump

# Decrypt and restore encrypted backup
./postgres-restore.sh --encrypt-key "my-secret-key" /path/to/backup.sql.gz.gpg
```

### Verify Backup Integrity

```bash
./postgres-backup.sh verify /path/to/backup.sql.gz
```

## Automated Backups

### Using Cron

Add to crontab for daily backups at 2 AM:

```bash
# Edit crontab
crontab -e

# Add line
0 2 * * * /home/labadmin/projects/droid-spring/dev/scripts/backup/postgres-backup.sh backup --retention 7 >> /var/log/postgres-backup.log 2>&1
```

### Docker Compose Integration

Add to `dev/compose.yml` for regular backups:

```yaml
postgres-backup:
  image: postgres:18-alpine
  container_name: bss-postgres-backup
  depends_on:
    - postgres
  environment:
    POSTGRES_DB: bss
    POSTGRES_USER: bss_app
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    BACKUP_RETENTION_DAYS: 7
  volumes:
    - ./backups:/var/lib/postgresql/backups
    - ./scripts/backup:/scripts
  entrypoint: |
    sh -c '
      while true; do
        /scripts/postgres-backup.sh backup
        sleep 86400
      done
    '
  networks:
    - bss-net
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | bss |
| `POSTGRES_USER` | Database user | bss_app |
| `POSTGRES_PASSWORD` | Database password | (required) |
| `BACKUP_RETENTION_DAYS` | Backup retention in days | 7 |
| `BACKUP_S3_BUCKET` | S3 bucket for uploads | (optional) |
| `BACKUP_ENCRYPTION_KEY` | GPG encryption key | (optional) |

## Features

### Backup Features
- ✅ Custom format dumps (compressed, portable)
- ✅ GZIP compression
- ✅ GPG encryption
- ✅ AWS S3 upload
- ✅ Automated cleanup of old backups
- ✅ Backup integrity verification
- ✅ Detailed logging

### Restore Features
- ✅ Support for compressed and encrypted backups
- ✅ S3 download support
- ✅ Custom database name
- ✅ Drop existing database option
- ✅ Dry-run mode
- ✅ List tables in backup
- ✅ No-owner restore option

## Backup Format

Backups are created in PostgreSQL's custom format with:
- Compression level 6
- Schema and data
- Ownership information (can be restored with --no-owner)

File naming: `bss_YYYYMMDD_HHMMSS.dump[.gz][.gpg]`

## Storage Locations

- **Local**: `/var/lib/postgresql/backups/`
- **S3**: `s3://BACKUP_S3_BUCKET/backups/`

## Best Practices

1. **Regular Backups**: Schedule daily backups
2. **Retention Policy**: Keep 7-30 days of backups
3. **Offsite Storage**: Upload to S3 for disaster recovery
4. **Encryption**: Always encrypt sensitive data
5. **Testing**: Regularly test restore procedure
6. **Monitoring**: Set up alerts for backup failures
7. **Documentation**: Document restore procedures

## Troubleshooting

### Backup Fails

```bash
# Check PostgreSQL is running
docker exec bss-postgres pg_isready

# Check credentials
docker exec bss-postgres psql -U bss_app -c "SELECT 1"
```

### Restore Fails

```bash
# Verify backup integrity
./postgres-backup.sh verify /path/to/backup

# Check available space
df -h /var/lib/postgresql/backups/

# Check PostgreSQL logs
docker logs bss-postgres
```

### S3 Upload Fails

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check bucket permissions
aws s3 ls s3://your-bucket-name/
```

## Example Backup Schedule

| Time | Action | Retention |
|------|--------|-----------|
| 2:00 AM | Daily full backup | 7 days |
| 2:00 AM (Sunday) | Weekly full backup | 30 days |
| 2:00 AM (1st of month) | Monthly archive | 1 year |

## Security Notes

- Never commit backup scripts with hardcoded passwords
- Use environment variables for secrets
- Encrypt backups containing sensitive data
- Restrict S3 bucket access with IAM policies
- Use dedicated service account for backups
- Enable S3 versioning and lifecycle policies

## Recovery Time Objectives (RTO)

- **Daily backup**: < 1 hour to restore
- **Weekly backup**: < 2 hours to restore
- Data loss: Maximum 24 hours (with daily backups)

## Testing Restore Procedure

Regularly test your restore procedure:

```bash
# 1. Create a backup
./postgres-backup.sh backup

# 2. List backups
./postgres-backup.sh list

# 3. Restore to test database
./postgres-restore.sh --target-db bss_test /path/to/latest/backup.dump

# 4. Verify data
docker exec bss-postgres psql -U bss_app -d bss_test -c "SELECT count(*) FROM customers;"

# 5. Cleanup test database
docker exec bss-postgres psql -U bss_app -c "DROP DATABASE bss_test;"
```

## Additional Resources

- [PostgreSQL Backup Documentation](https://www.postgresql.org/docs/current/backup.html)
- [pg_dump Reference](https://www.postgresql.org/docs/current/app-pgdump.html)
- [pg_restore Reference](https://www.postgresql.org/docs/current/app-pgrestore.html)
- [AWS S3 Backup Best Practices](https://aws.amazon.com/blogs/database/managing-postgresql-backups-with-aws-backup/)
