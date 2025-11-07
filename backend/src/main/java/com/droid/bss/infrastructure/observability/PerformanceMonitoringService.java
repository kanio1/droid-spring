package com.droid.bss.infrastructure.observability;

import com.droid.bss.domain.monitoring.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Real-Time Performance Monitoring Service
 * Tracks queries, locks, deadlocks, and system performance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMonitoringService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get active queries
     */
    public List<ActiveQuery> getActiveQueries() {
        log.debug("Fetching active queries");

        String query = """
            SELECT
                pid,
                usename,
                application_name,
                client_addr,
                state,
                query_start,
                state_change,
                query,
                backend_start
            FROM pg_stat_activity
            WHERE state = 'active'
            AND pid != pg_backend_pid()
            ORDER BY query_start DESC
            LIMIT 50
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(this::mapToActiveQuery)
                .collect(Collectors.toList());
    }

    /**
     * Get long-running queries
     */
    public List<LongRunningQuery> getLongRunningQueries(int thresholdSeconds) {
        log.debug("Fetching long-running queries (threshold: {}s)", thresholdSeconds);

        String query = """
            SELECT
                pid,
                usename,
                application_name,
                client_addr,
                state,
                query_start,
                state_change,
                EXTRACT(EPOCH FROM (NOW() - query_start))::INTEGER as duration_seconds,
                query
            FROM pg_stat_activity
            WHERE state = 'active'
            AND query_start < NOW() - INTERVAL '? seconds'
            AND pid != pg_backend_pid()
            ORDER BY query_start ASC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, thresholdSeconds);

        return rows.stream()
                .map(this::mapToLongRunningQuery)
                .collect(Collectors.toList());
    }

    /**
     * Get lock information
     */
    public List<LockInfo> getLockInformation() {
        log.debug("Fetching lock information");

        String query = """
            SELECT
                l.locktype,
                l.mode,
                l.lockstatus,
                l.granted,
                a.pid as blocked_pid,
                a.usename as blocked_user,
                a.query as blocked_query,
                a.query_start as blocked_query_start,
                b.pid as blocking_pid,
                b.usename as blocking_user,
                b.query as blocking_query,
                b.query_start as blocking_query_start
            FROM pg_locks l
            JOIN pg_stat_activity a ON l.pid = a.pid
            JOIN pg_stat_activity b ON l.pid = b.pid
            WHERE NOT l.granted
            OR a.state = 'active'
            ORDER BY l.mode, l.locktype
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(this::mapToLockInfo)
                .collect(Collectors.toList());
    }

    /**
     * Get deadlock information
     */
    public List<DeadlockInfo> getDeadlockInformation() {
        log.debug("Fetching deadlock information");

        String query = """
            SELECT
                pid,
                database,
                usename,
                application_name,
                client_addr,
                query_start,
                state,
                query
            FROM pg_stat_activity
            WHERE query LIKE '%deadlock%'
            OR query LIKE '%cancelled%'
            AND query_start >= NOW() - INTERVAL '24 hours'
            ORDER BY query_start DESC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(this::mapToDeadlockInfo)
                .collect(Collectors.toList());
    }

    /**
     * Get database statistics
     */
    public DatabaseStatistics getDatabaseStatistics() {
        log.debug("Fetching database statistics");

        String query = """
            SELECT
                (SELECT COUNT(*) FROM pg_stat_activity) as total_connections,
                (SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active') as active_connections,
                (SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'idle') as idle_connections,
                (SELECT xact_commit FROM pg_stat_database ORDER BY xact_commit DESC LIMIT 1) as total_commits,
                (SELECT xact_rollback FROM pg_stat_database ORDER BY xact_rollback DESC LIMIT 1) as total_rollbacks,
                (SELECT blks_read FROM pg_stat_database ORDER BY blks_read DESC LIMIT 1) as blks_read,
                (SELECT blks_hit FROM pg_stat_database ORDER BY blks_hit DESC LIMIT 1) as blks_hit,
                (SELECT tup_returned FROM pg_stat_database ORDER BY tup_returned DESC LIMIT 1) as tup_returned,
                (SELECT tup_fetched FROM pg_stat_database ORDER BY tup_fetched DESC LIMIT 1) as tup_fetched,
                (SELECT tup_inserted FROM pg_stat_database ORDER BY tup_inserted DESC LIMIT 1) as tup_inserted,
                (SELECT tup_updated FROM pg_stat_database ORDER BY tup_updated DESC LIMIT 1) as tup_updated,
                (SELECT tup_deleted FROM pg_stat_database ORDER BY tup_deleted DESC LIMIT 1) as tup_deleted
 """;

        Map<String, Object> row = jdbcTemplate.queryForMap(query);

        return DatabaseStatistics.builder()
                .totalConnections(getLong(row, "total_connections"))
                .activeConnections(getLong(row, "active_connections"))
                .idleConnections(getLong(row, "idle_connections"))
                .totalCommits(getLong(row, "total_commits"))
                .totalRollbacks(getLong(row, "total_rollbacks"))
                .blksRead(getLong(row, "blks_read"))
                .blksHit(getLong(row, "blks_hit"))
                .tupReturned(getLong(row, "tup_returned"))
                .tupFetched(getLong(row, "tup_fetched"))
                .tupInserted(getLong(row, "tup_inserted"))
                .tupUpdated(getLong(row, "tup_updated"))
                .tupDeleted(getLong(row, "tup_deleted"))
                .cacheHitRatio(calculateCacheHitRatio(
                        getLong(row, "blks_hit"),
                        getLong(row, "blks_read")))
                .build();
    }

    /**
     * Get table statistics
     */
    public List<TableStatistics> getTableStatistics() {
        log.debug("Fetching table statistics");

        String query = """
            SELECT
                schemaname,
                tablename,
                seq_scan,
                seq_tup_read,
                idx_scan,
                idx_tup_fetch,
                n_tup_ins,
                n_tup_upd,
                n_tup_del,
                n_live_tup,
                n_dead_tup,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze
            FROM pg_stat_user_tables
            ORDER BY seq_tup_read DESC
            LIMIT 50
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(this::mapToTableStatistics)
                .collect(Collectors.toList());
    }

    /**
     * Get index statistics
     */
    public List<IndexStatistics> getIndexStatistics() {
        log.debug("Fetching index statistics");

        String query = """
            SELECT
                schemaname,
                tablename,
                indexrelname,
                idx_scan,
                idx_tup_read,
                idx_tup_fetch
            FROM pg_stat_user_indexes
            ORDER BY idx_scan DESC
            LIMIT 50
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

        return rows.stream()
                .map(this::mapToIndexStatistics)
                .collect(Collectors.toList());
    }

    /**
     * Get query statistics
     */
    public List<QueryStatistics> getQueryStatistics(int limit) {
        log.debug("Fetching query statistics (limit: {})", limit);

        String query = """
            SELECT
                query,
                calls,
                total_time,
                mean_time,
                min_time,
                max_time,
                rows,
                100.0 * shared_blks_hit / NULLIF(shared_blks_hit + shared_blks_read, 0) AS hit_percent
            FROM pg_stat_statements
            ORDER BY mean_time DESC
            LIMIT ?
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, limit);

        return rows.stream()
                .map(this::mapToQueryStatistics)
                .collect(Collectors.toList());
    }

    /**
     * Get system metrics
     */
    public SystemMetrics getSystemMetrics() {
        log.debug("Fetching system metrics");

        String query = """
            SELECT
                (SELECT COUNT(*) FROM pg_stat_activity) as connections,
                (SELECT MAX(now() - query_start) FROM pg_stat_activity WHERE state = 'active') as max_query_duration,
                (SELECT AVG(now() - query_start) FROM pg_stat_activity WHERE state = 'active') as avg_query_duration,
                (SELECT COUNT(*) FROM pg_locks WHERE NOT granted) as waiting_locks,
                (SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'idle in transaction') as idle_in_transaction
 """;

        Map<String, Object> row = jdbcTemplate.queryForMap(query);

        return SystemMetrics.builder()
                .connections(getLong(row, "connections"))
                .maxQueryDuration(getDouble(row, "max_query_duration"))
                .avgQueryDuration(getDouble(row, "avg_query_duration"))
                .waitingLocks(getLong(row, "waiting_locks"))
                .idleInTransaction(getLong(row, "idle_in_transaction"))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Kill a query
     */
    public void killQuery(Long pid) {
        log.warn("Killing query with PID: {}", pid);
        String query = "SELECT pg_terminate_backend(?)";
        jdbcTemplate.queryForObject(query, Boolean.class, pid);
    }

    /**
     * Get performance alerts
     */
    public List<PerformanceAlert> getPerformanceAlerts() {
        log.debug("Fetching performance alerts");

        List<PerformanceAlert> alerts = new ArrayList<>();

        // Check for long-running queries
        List<LongRunningQuery> longRunning = getLongRunningQueries(30);
        if (!longRunning.isEmpty()) {
            alerts.add(PerformanceAlert.builder()
                    .severity("WARNING")
                    .type("LONG_RUNNING_QUERIES")
                    .message(String.format("%d queries running for more than 30 seconds", longRunning.size()))
                    .count(longRunning.size())
                    .timestamp(Instant.now())
                    .build());
        }

        // Check for waiting locks
        List<LockInfo> locks = getLockInformation();
        long waitingLocks = locks.stream()
                .filter(l -> !l.getGranted())
                .count();

        if (waitingLocks > 0) {
            alerts.add(PerformanceAlert.builder()
                    .severity("CRITICAL")
                    .type("LOCKS_WAITING")
                    .message(String.format("%d queries waiting for locks", waitingLocks))
                    .count(waitingLocks)
                    .timestamp(Instant.now())
                    .build());
        }

        // Check for deadlocks
        List<DeadlockInfo> deadlocks = getDeadlockInformation();
        if (!deadlocks.isEmpty()) {
            alerts.add(PerformanceAlert.builder()
                    .severity("CRITICAL")
                    .type("DEADLOCKS")
                    .message(String.format("Detected %d deadlock events", deadlocks.size()))
                    .count(deadlocks.size())
                    .timestamp(Instant.now())
                    .build());
        }

        // Check for high connection count
        DatabaseStatistics dbStats = getDatabaseStatistics();
        if (dbStats.getActiveConnections() > 80) {
            alerts.add(PerformanceAlert.builder()
                    .severity("WARNING")
                    .type("HIGH_CONNECTIONS")
                    .message(String.format("%d active connections", dbStats.getActiveConnections()))
                    .count(dbStats.getActiveConnections())
                    .timestamp(Instant.now())
                    .build());
        }

        return alerts;
    }

    /**
     * Helper methods
     */
    private ActiveQuery mapToActiveQuery(Map<String, Object> row) {
        return ActiveQuery.builder()
                .pid(getLong(row, "pid"))
                .username(getString(row, "usename"))
                .applicationName(getString(row, "application_name"))
                .clientAddress(getString(row, "client_addr"))
                .state(getString(row, "state"))
                .queryStart(getInstant(row, "query_start"))
                .stateChange(getInstant(row, "state_change"))
                .query(getString(row, "query"))
                .backendStart(getInstant(row, "backend_start"))
                .build();
    }

    private LongRunningQuery mapToLongRunningQuery(Map<String, Object> row) {
        return LongRunningQuery.builder()
                .pid(getLong(row, "pid"))
                .username(getString(row, "usename"))
                .applicationName(getString(row, "application_name"))
                .clientAddress(getString(row, "client_addr"))
                .state(getString(row, "state"))
                .queryStart(getInstant(row, "query_start"))
                .durationSeconds(getLong(row, "duration_seconds"))
                .query(getString(row, "query"))
                .build();
    }

    private LockInfo mapToLockInfo(Map<String, Object> row) {
        return LockInfo.builder()
                .lockType(getString(row, "locktype"))
                .mode(getString(row, "mode"))
                .lockStatus(getString(row, "lockstatus"))
                .granted(getBoolean(row, "granted"))
                .blockedPid(getLong(row, "blocked_pid"))
                .blockedUser(getString(row, "blocked_user"))
                .blockedQuery(getString(row, "blocked_query"))
                .blockedQueryStart(getInstant(row, "blocked_query_start"))
                .blockingPid(getLong(row, "blocking_pid"))
                .blockingUser(getString(row, "blocking_user"))
                .blockingQuery(getString(row, "blocking_query"))
                .blockingQueryStart(getInstant(row, "blocking_query_start"))
                .build();
    }

    private DeadlockInfo mapToDeadlockInfo(Map<String, Object> row) {
        return DeadlockInfo.builder()
                .pid(getLong(row, "pid"))
                .database(getString(row, "database"))
                .username(getString(row, "usename"))
                .applicationName(getString(row, "application_name"))
                .clientAddress(getString(row, "client_addr"))
                .queryStart(getInstant(row, "query_start"))
                .state(getString(row, "state"))
                .query(getString(row, "query"))
                .build();
    }

    private TableStatistics mapToTableStatistics(Map<String, Object> row) {
        return TableStatistics.builder()
                .schemaName(getString(row, "schemaname"))
                .tableName(getString(row, "tablename"))
                .seqScan(getLong(row, "seq_scan"))
                .seqTupRead(getLong(row, "seq_tup_read"))
                .idxScan(getLong(row, "idx_scan"))
                .idxTupFetch(getLong(row, "idx_tup_fetch"))
                .nTupIns(getLong(row, "n_tup_ins"))
                .nTupUpd(getLong(row, "n_tup_upd"))
                .nTupDel(getLong(row, "n_tup_del"))
                .nLiveTup(getLong(row, "n_live_tup"))
                .nDeadTup(getLong(row, "n_dead_tup"))
                .lastVacuum(getInstant(row, "last_vacuum"))
                .lastAutovacuum(getInstant(row, "last_autovacuum"))
                .lastAnalyze(getInstant(row, "last_analyze"))
                .lastAutoanalyze(getInstant(row, "last_autoanalyze"))
                .build();
    }

    private IndexStatistics mapToIndexStatistics(Map<String, Object> row) {
        return IndexStatistics.builder()
                .schemaName(getString(row, "schemaname"))
                .tableName(getString(row, "tablename"))
                .indexName(getString(row, "indexrelname"))
                .idxScan(getLong(row, "idx_scan"))
                .idxTupRead(getLong(row, "idx_tup_read"))
                .idxTupFetch(getLong(row, "idx_tup_fetch"))
                .build();
    }

    private QueryStatistics mapToQueryStatistics(Map<String, Object> row) {
        return QueryStatistics.builder()
                .query(getString(row, "query"))
                .calls(getLong(row, "calls"))
                .totalTime(getDouble(row, "total_time"))
                .meanTime(getDouble(row, "mean_time"))
                .minTime(getDouble(row, "min_time"))
                .maxTime(getDouble(row, "max_time"))
                .rows(getLong(row, "rows"))
                .hitPercent(getDouble(row, "hit_percent"))
                .build();
    }

    private double calculateCacheHitRatio(Long hits, Long reads) {
        if (hits == null || reads == null) return 0.0;
        long total = hits + reads;
        if (total == 0) return 100.0;
        return (double) hits / total * 100.0;
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).longValue() : 0L;
    }

    private Double getDouble(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).doubleValue() : 0.0;
    }

    private Boolean getBoolean(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? (Boolean) value : false;
    }

    private Instant getInstant(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toInstant();
        } else if (value instanceof String) {
            return Instant.parse((String) value);
        }
        return null;
    }
}
