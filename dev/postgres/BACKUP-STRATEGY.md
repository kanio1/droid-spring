# PostgreSQL Backup & Recovery Strategy

## Overview
Comprehensive backup and recovery strategy for PostgreSQL 18 in the BSS system.

---

## Backup Methods

### 1. WAL Archiving (Continuous Backup)

**Purpose:** Point-in-Time Recovery (PITR)  
**Retention:** 7 days  
**Recovery Point:** Any point in time within retention period

**Configuration File:** `postgresql-wal.conf`

**How it works:**
- WAL (Write-Ahead Log) files are continuously archived
- Allows recovery to any point in time
- Minimal data loss (seconds to minutes)

**Enable WAL Archiving:**
```bash
# Add to postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://bss-postgres-backups/wal/%f'
archive_timeout = 60
```

---

### 2. Base Backup (Full Backup)

**Purpose:** Starting point for WAL replay  
**Frequency:** Daily  
**Retention:** 30 days

**Create Base Backup:**
```bash
# Using pg_basebackup
pg_basebackup -h localhost -U postgres -D /backup/base/$(date +\%Y\%m\%d) -Ft -z -P -W

# Using pgBackRest (recommended)
pgbackrest --stanza=main backup
```

---

### 3. Logical Backup (SQL Dump)

**Purpose:** Cross-version migration, selective data  
**Frequency:** Weekly  
**Retention:** 12 weeks

**Create SQL Dump:**
```bash
# Full database dump
pg_dumpall -U postgres --clean --if-exists --verbose --file=bss_backup_$(date +\%Y\%m\%d).sql

# Single database
pg_dump -U postgres -Fc bss > bss_backup_$(date +\%Y\%m\%d).dump
```

---

## Backup Schedule

| Type | Frequency | Command | Retention |
|------|-----------|---------|-----------|
| WAL Archive | Continuous | Archive command | 7 days |
| Base Backup | Daily 2 AM | pg_basebackup | 30 days |
| SQL Dump | Weekly Sunday 3 AM | pg_dumpall | 12 weeks |
| Verification | Weekly | pg_basebackup --verify | N/A |

**Cron Example:**
```bash
# Base backup daily at 2 AM
0 2 * * * /usr/local/bin/postgres-base-backup.sh

# SQL dump weekly at 3 AM
0 3 * * 0 /usr/local/bin/postgres-sql-dump.sh

# Cleanup old backups
0 4 * * * /usr/local/bin/postgres-cleanup-backups.sh
```

---

## Recovery Procedures

### Point-in-Time Recovery (PITR)

**Scenario:** Database crashed, need to restore to specific time

**Steps:**

1. **Stop PostgreSQL:**
   ```bash
   docker compose stop postgres
   ```

2. **Clean data directory:**
   ```bash
   rm -rf /var/lib/postgresql/data/*
   ```

3. **Restore base backup:**
   ```bash
   # Extract base backup
   tar -xzvf /backup/base/20251107/base.tar.gz -C /var/lib/postgresql/data/
   
   # Set proper ownership
   chown -R postgres:postgres /var/lib/postgresql/data
   ```

4. **Configure recovery:**
   ```bash
   # Create recovery.signal
   touch /var/lib/postgresql/data/recovery.signal
   
   # Add to postgresql.auto.conf
   echo "restore_command = 'aws s3 cp s3://bss-postgres-backups/wal/%f %p'" >> /var/lib/postgresql/data/postgresql.auto.conf
   echo "recovery_target_time = '2025-11-07 14:30:00'" >> /var/lib/postgresql/data/postgresql.auto.conf
   echo "recovery_target_action = promote" >> /var/lib/postgresql/data/postgresql.auto.conf
   ```

5. **Start PostgreSQL:**
   ```bash
   docker compose start postgres
   ```

6. **Verify recovery:**
   ```bash
   psql -U postgres -c "SELECT pg_is_in_recovery();"
   ```

---

### Full Database Recovery

**Scenario:** Complete data loss, start from scratch

**Steps:**

1. **Stop PostgreSQL:**
   ```bash
   docker compose stop postgres
   ```

2. **Clean data directory:**
   ```bash
   rm -rf /var/lib/postgresql/data/*
   ```

3. **Restore latest base backup:**
   ```bash
   tar -xzvf /backup/base/20251107/base.tar.gz -C /var/lib/postgresql/data/
   chown -R postgres:postgres /var/lib/postgresql/data
   ```

4. **Start PostgreSQL:**
   ```bash
   docker compose start postgres
   ```

5. **Verify database:**
   ```bash
   psql -U postgres -l
   ```

---

## Backup Storage Options

### 1. AWS S3 (Recommended)
```bash
archive_command = 'aws s3 cp %p s3://bss-postgres-backups/wal/%f'
```

**Pros:**
- Highly durable (99.999999999%)
- Cost-effective
- Cross-region replication
- Lifecycle policies

**Cons:**
- Network dependency
- AWS costs

---

### 2. Local Filesystem
```bash
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'
```

**Pros:**
- Fast
- No network needed
- No cloud costs

**Cons:**
- Single point of failure
- Limited scalability
- No off-site redundancy

---

### 3. Object Storage (MinIO, Ceph)
```bash
archive_command = 'curl -X PUT -T %p http://minio:9000/postgres-wal/%f'
```

**Pros:**
- Self-hosted
- S3-compatible
- Private cloud

**Cons:**
- Additional infrastructure
- Maintenance overhead

---

## Testing Recovery

### Monthly DR Drill

**Week 1 of every month:**
1. Set up test environment
2. Restore from backup
3. Verify data integrity
4. Test application connectivity
5. Document timing and issues
6. Update runbook if needed

**Success Criteria:**
- Recovery time < 1 hour
- Data loss < 5 minutes
- 100% data integrity

---

## Monitoring & Alerting

### Key Metrics

1. **WAL Archive Lag:**
   ```sql
   SELECT EXTRACT(EPOCH FROM NOW() - pg_last_xact_replay_timestamp()) AS lag;
   ```

2. **Backup Success:**
   ```bash
   # Monitor via Prometheus
   postgres_backup_last_success_timestamp
   ```

3. **Disk Usage:**
   ```bash
   # Alert if backups use > 80% of disk
   df -h /backup
   ```

### Alerts

- WAL archive failures
- Backup failures
- Backup size anomalies
- Disk space low
- Replication lag

---

## Compliance

### Data Retention

- **Regulatory (PCI DSS, SOX):** 7 years minimum
- **Business:** 90 days
- **Development/Testing:** 30 days

### Encryption

- Encrypt backups at rest (AES-256)
- Encrypt in transit (TLS 1.2+)
- Use customer-managed keys (KMS)

### Access Control

- Limit backup access to DBA team
- Use IAM roles for cloud backups
- Audit all access

---

## Automation Scripts

See:
- `/home/labadmin/projects/droid-spring/dev/scripts/backup/postgres-backup.sh`
- `/home/labadmin/projects/droid-spring/dev/scripts/backup/postgres-restore.sh`
- `/home/labadmin/projects/droid-spring/dev/scripts/backup/postgres-cleanup.sh`

---

## Troubleshooting

### Common Issues

1. **WAL archive failing:**
   - Check S3 permissions
   - Verify network connectivity
   - Review archive_command syntax

2. **Disk full:**
   - Increase archive retention
   - Add more storage
   - Compress WAL files

3. **Slow backup:**
   - Use parallel backup
   - Increase network bandwidth
   - Optimize compression

---

## References

- [PostgreSQL Backup Documentation](https://www.postgresql.org/docs/18/backup.html)
- [pgBackRest Documentation](https://pgbackrest.org/)
- [AWS RDS Backup Best Practices](https://aws.amazon.com/blogs/database/tag/backups/)
- [PostgreSQL Continuous Archiving](https://www.postgresql.org/docs/18/continuous-archiving.html)

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-07  
**Next Review:** 2025-12-07
