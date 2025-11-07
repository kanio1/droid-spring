package com.droid.bss.infrastructure.performance;

import com.droid.bss.domain.performance.QueryPerformanceMetrics;
import com.droid.bss.domain.performance.DatabaseStats;
import com.droid.bss.domain.performance.IndexStats;
import com.droid.bss.domain.performance.TableStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Performance Repository
 * Accesses PostgreSQL performance views and pg_stat_statements
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class QueryPerformanceRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get top slow queries from pg_stat_statements
     */
    public List<QueryPerformanceMetrics> getTopSlowQueries(int limit) {
        log.debug("Fetching top {} slow queries", limit);

        String sql = """
            SELECT
                queryid,
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                min_exec_time,
                max_exec_time,
                stddev_exec_time,
                rows,
                CASE
                    WHEN shared_blks_hit + shared_blks_read = 0 THEN 100
                    ELSE 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)
                END as hit_percent
            FROM pg_stat_statements
            WHERE query NOT LIKE '%pg_stat_statements%'
            ORDER BY mean_exec_time DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal totalExecTime = rs.getBigDecimal("total_exec_time");
            BigDecimal meanExecTime = rs.getBigDecimal("mean_exec_time");
            BigDecimal minExecTime = rs.getBigDecimal("min_exec_time");
            BigDecimal maxExecTime = rs.getBigDecimal("max_exec_time");
            BigDecimal stddevExecTime = rs.getBigDecimal("stddev_exec_time");
            BigDecimal hitPercent = rs.getBigDecimal("hit_percent");

            return QueryPerformanceMetrics.builder()
                .queryId(rs.getLong("queryid"))
                .query(rs.getString("query"))
                .calls(rs.getLong("calls"))
                .totalExecTime(totalExecTime != null ? totalExecTime : BigDecimal.ZERO)
                .meanExecTime(meanExecTime != null ? meanExecTime : BigDecimal.ZERO)
                .minExecTime(minExecTime != null ? minExecTime : BigDecimal.ZERO)
                .maxExecTime(maxExecTime != null ? maxExecTime : BigDecimal.ZERO)
                .stddevExecTime(stddevExecTime != null ? stddevExecTime : BigDecimal.ZERO)
                .rows(rs.getLong("rows"))
                .hitPercent(hitPercent != null ? hitPercent : BigDecimal.ZERO)
                .build();
        }, limit);
    }

    /**
     * Get most frequently called queries
     */
    public List<QueryPerformanceMetrics> getMostFrequentQueries(int limit) {
        log.debug("Fetching top {} most frequent queries", limit);

        String sql = """
            SELECT
                queryid,
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                min_exec_time,
                max_exec_time,
                stddev_exec_time,
                rows,
                CASE
                    WHEN shared_blks_hit + shared_blks_read = 0 THEN 100
                    ELSE 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)
                END as hit_percent
            FROM pg_stat_statements
            WHERE query NOT LIKE '%pg_stat_statements%'
            ORDER BY calls DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal totalExecTime = rs.getBigDecimal("total_exec_time");
            BigDecimal meanExecTime = rs.getBigDecimal("mean_exec_time");
            BigDecimal minExecTime = rs.getBigDecimal("min_exec_time");
            BigDecimal maxExecTime = rs.getBigDecimal("max_exec_time");
            BigDecimal stddevExecTime = rs.getBigDecimal("stddev_exec_time");
            BigDecimal hitPercent = rs.getBigDecimal("hit_percent");

            return QueryPerformanceMetrics.builder()
                .queryId(rs.getLong("queryid"))
                .query(rs.getString("query"))
                .calls(rs.getLong("calls"))
                .totalExecTime(totalExecTime != null ? totalExecTime : BigDecimal.ZERO)
                .meanExecTime(meanExecTime != null ? meanExecTime : BigDecimal.ZERO)
                .minExecTime(minExecTime != null ? minExecTime : BigDecimal.ZERO)
                .maxExecTime(maxExecTime != null ? maxExecTime : BigDecimal.ZERO)
                .stddevExecTime(stddevExecTime != null ? stddevExecTime : BigDecimal.ZERO)
                .rows(rs.getLong("rows"))
                .hitPercent(hitPercent != null ? hitPercent : BigDecimal.ZERO)
                .build();
        }, limit);
    }

    /**
     * Get queries with worst cache hit ratio
     */
    public List<QueryPerformanceMetrics> getWorstCacheQueries(int limit) {
        log.debug("Fetching top {} queries with worst cache hit ratio", limit);

        String sql = """
            SELECT
                queryid,
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                min_exec_time,
                max_exec_time,
                stddev_exec_time,
                rows,
                CASE
                    WHEN shared_blks_hit + shared_blks_read = 0 THEN 100
                    ELSE 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)
                END as hit_percent
            FROM pg_stat_statements
            WHERE query NOT LIKE '%pg_stat_statements%'
            AND calls > 10
            ORDER BY hit_percent ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal totalExecTime = rs.getBigDecimal("total_exec_time");
            BigDecimal meanExecTime = rs.getBigDecimal("mean_exec_time");
            BigDecimal minExecTime = rs.getBigDecimal("min_exec_time");
            BigDecimal maxExecTime = rs.getBigDecimal("max_exec_time");
            BigDecimal stddevExecTime = rs.getBigDecimal("stddev_exec_time");
            BigDecimal hitPercent = rs.getBigDecimal("hit_percent");

            return QueryPerformanceMetrics.builder()
                .queryId(rs.getLong("queryid"))
                .query(rs.getString("query"))
                .calls(rs.getLong("calls"))
                .totalExecTime(totalExecTime != null ? totalExecTime : BigDecimal.ZERO)
                .meanExecTime(meanExecTime != null ? meanExecTime : BigDecimal.ZERO)
                .minExecTime(minExecTime != null ? minExecTime : BigDecimal.ZERO)
                .maxExecTime(maxExecTime != null ? maxExecTime : BigDecimal.ZERO)
                .stddevExecTime(stddevExecTime != null ? stddevExecTime : BigDecimal.ZERO)
                .rows(rs.getLong("rows"))
                .hitPercent(hitPercent != null ? hitPercent : BigDecimal.ZERO)
                .build();
        }, limit);
    }

    /**
     * Get database performance statistics
     */
    public DatabaseStats getDatabaseStats() {
        log.debug("Fetching database performance statistics");

        String sql = """
            SELECT
                datname,
                blks_read,
                blks_hit,
                xact_commit,
                xact_rollback,
                blks_fetch,
                tup_returned,
                tup_fetched,
                tup_inserted,
                tup_updated,
                tup_deleted,
                conflicts,
                temp_files,
                temp_bytes,
                deadlocks,
                blk_read_time,
                blk_write_time
            FROM pg_stat_database
            WHERE datname = current_database()
            """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                Long blksRead = rs.getLong("blks_read");
                Long blksHit = rs.getLong("blks_hit");
                Long xactCommit = rs.getLong("xact_commit");
                Long xactRollback = rs.getLong("xact_rollback");

                BigDecimal cacheHitRatio = BigDecimal.ZERO;
                if (blksRead != null && blksHit != null && (blksRead + blksHit) > 0) {
                    cacheHitRatio = new BigDecimal(blksHit)
                        .divide(new BigDecimal(blksRead + blksHit), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100));
                }

                // Get database size
                String sizeQuery = "SELECT pg_database_size(current_database())";
                Long sizeBytes = jdbcTemplate.queryForObject(sizeQuery, Long.class);

                String sizeHuman = formatSize(sizeBytes);

                return DatabaseStats.builder()
                    .databaseName(rs.getString("datname"))
                    .sizeBytes(sizeBytes)
                    .sizeHuman(sizeHuman)
                    .cacheHitRatio(cacheHitRatio)
                    .blksRead(blksRead)
                    .blksHit(blksHit)
                    .xactCommit(xactCommit)
                    .xactRollback(xactRollback)
                    .blksFetch(rs.getLong("blks_fetch"))
                    .tupReturned(rs.getLong("tup_returned"))
                    .tupFetched(rs.getLong("tup_fetched"))
                    .tupInserted(rs.getLong("tup_inserted"))
                    .tupUpdated(rs.getLong("tup_updated"))
                    .tupDeleted(rs.getLong("tup_deleted"))
                    .conflicts(rs.getLong("conflicts"))
                    .tempFiles(rs.getLong("temp_files"))
                    .tempBytes(rs.getLong("temp_bytes"))
                    .deadlocks(rs.getLong("deadlocks"))
                    .blkReadTime(rs.getDouble("blk_read_time"))
                    .blkWriteTime(rs.getDouble("blk_write_time"))
                    .build();
            }
            return null;
        });
    }

    /**
     * Get index usage statistics
     */
    public List<IndexStats> getIndexStats() {
        log.debug("Fetching index usage statistics");

        String sql = """
            SELECT
                schemaname,
                tablename,
                indexname,
                idx_scan,
                idx_tup_read,
                idx_tup_fetch
            FROM pg_stat_user_indexes
            ORDER BY idx_scan DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long idxScan = rs.getLong("idx_scan");
            Long idxTupRead = rs.getLong("idx_tup_read");
            Long idxTupFetch = rs.getLong("idx_tup_fetch");

            Double avgTuplesFetched = 0.0;
            if (idxScan != null && idxScan > 0 && idxTupFetch != null) {
                avgTuplesFetched = (double) idxTupFetch / idxScan;
            }

            return IndexStats.builder()
                .schemaName(rs.getString("schemaname"))
                .tableName(rs.getString("tablename"))
                .indexName(rs.getString("indexname"))
                .indexScans(idxScan)
                .tuplesRead(idxTupRead)
                .tuplesFetched(idxTupFetch)
                .avgTuplesFetched(avgTuplesFetched)
                .build();
        });
    }

    /**
     * Get table statistics including bloat
     */
    public List<TableStats> getTableStats() {
        log.debug("Fetching table statistics");

        String sql = """
            SELECT
                schemaname,
                tablename,
                n_tup_ins,
                n_tup_upd,
                n_tup_del,
                n_live_tup,
                n_dead_tup,
                last_vacuum,
                last_autovacuum,
                last_analyze,
                last_autoanalyze,
                vacuum_count,
                autovacuum_count,
                analyze_count,
                autoanalyze_count
            FROM pg_stat_user_tables
            WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
            ORDER BY n_dead_tup DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long nLiveTup = rs.getLong("n_live_tup");
            Long nDeadTup = rs.getLong("n_dead_tup");

            BigDecimal deadRowPercent = BigDecimal.ZERO;
            if (nLiveTup != null && nLiveTup > 0 && nDeadTup != null) {
                deadRowPercent = new BigDecimal(nDeadTup)
                    .divide(new BigDecimal(nLiveTup), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            }

            return TableStats.builder()
                .schemaName(rs.getString("schemaname"))
                .tableName(rs.getString("tablename"))
                .rowsInserted(rs.getLong("n_tup_ins"))
                .rowsUpdated(rs.getLong("n_tup_upd"))
                .rowsDeleted(rs.getLong("n_tup_del"))
                .liveRows(nLiveTup)
                .deadRows(nDeadTup)
                .deadRowPercent(deadRowPercent)
                .lastVacuum(rs.getTimestamp("last_vacuum"))
                .lastAutoVacuum(rs.getTimestamp("last_autovacuum"))
                .lastAnalyze(rs.getTimestamp("last_analyze"))
                .lastAutoAnalyze(rs.getTimestamp("last_autoanalyze"))
                .vacuumCount(rs.getLong("vacuum_count"))
                .autoVacuumCount(rs.getLong("autovacuum_count"))
                .analyzeCount(rs.getLong("analyze_count"))
                .autoAnalyzeCount(rs.getLong("autoanalyze_count"))
                .build();
        });
    }

    /**
     * Get all query statistics
     */
    public List<QueryPerformanceMetrics> getAllQueryStats() {
        log.debug("Fetching all query statistics");

        String sql = """
            SELECT
                queryid,
                query,
                calls,
                total_exec_time,
                mean_exec_time,
                min_exec_time,
                max_exec_time,
                stddev_exec_time,
                rows,
                CASE
                    WHEN shared_blks_hit + shared_blks_read = 0 THEN 100
                    ELSE 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)
                END as hit_percent
            FROM pg_stat_statements
            WHERE query NOT LIKE '%pg_stat_statements%'
            ORDER BY total_exec_time DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal totalExecTime = rs.getBigDecimal("total_exec_time");
            BigDecimal meanExecTime = rs.getBigDecimal("mean_exec_time");
            BigDecimal minExecTime = rs.getBigDecimal("min_exec_time");
            BigDecimal maxExecTime = rs.getBigDecimal("max_exec_time");
            BigDecimal stddevExecTime = rs.getBigDecimal("stddev_exec_time");
            BigDecimal hitPercent = rs.getBigDecimal("hit_percent");

            return QueryPerformanceMetrics.builder()
                .queryId(rs.getLong("queryid"))
                .query(rs.getString("query"))
                .calls(rs.getLong("calls"))
                .totalExecTime(totalExecTime != null ? totalExecTime : BigDecimal.ZERO)
                .meanExecTime(meanExecTime != null ? meanExecTime : BigDecimal.ZERO)
                .minExecTime(minExecTime != null ? minExecTime : BigDecimal.ZERO)
                .maxExecTime(maxExecTime != null ? maxExecTime : BigDecimal.ZERO)
                .stddevExecTime(stddevExecTime != null ? stddevExecTime : BigDecimal.ZERO)
                .rows(rs.getLong("rows"))
                .hitPercent(hitPercent != null ? hitPercent : BigDecimal.ZERO)
                .build();
        });
    }

    /**
     * Reset query statistics (requires superuser)
     */
    public void resetQueryStats() {
        log.info("Resetting query performance statistics");
        String sql = "SELECT pg_stat_statements_reset()";
        jdbcTemplate.query(sql, rs -> null);
    }

    /**
     * Format bytes to human readable format
     */
    private String formatSize(Long bytes) {
        if (bytes == null) return "0 B";

        double size = bytes.doubleValue();
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
