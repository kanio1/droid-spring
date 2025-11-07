package com.droid.bss.infrastructure.temporal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Temporal Table Service
 * Manages PostgreSQL temporal tables for historical data tracking
 */
@Service
public class TemporalTableService {

    private static final Logger log = LoggerFactory.getLogger(TemporalTableService.class);

    private final JdbcTemplate jdbcTemplate;

    public TemporalTableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Create temporal version of a table
     */
    public void createTemporalTable(String tableName, String primaryKeyColumn) {
        log.info("Creating temporal table for: {}", tableName);

        String temporalTableName = tableName + "_history";
        String systemTimeColumn = "sys_period";
        String versionColumn = "version";
        String primaryKeyVersionColumn = primaryKeyColumn + "_version";

        try {
            // Create history table with system time period
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS %s (
                    LIKE %s INCLUDING ALL,
                    %s INTEGER DEFAULT 1,
                    PRIMARY KEY (%s, %s)
                );
                """.formatted(
                temporalTableName, tableName,
                versionColumn, primaryKeyColumn, versionColumn
            );

            jdbcTemplate.execute(createTableSQL);

            // Create trigger function to automatically capture changes
            String createFunctionSQL = """
                CREATE OR REPLACE FUNCTION %s_history_trigger()
                RETURNS TRIGGER AS $$
                BEGIN
                    -- Update the end time of the previous version
                    UPDATE %s
                    SET %s = tstzrange(lower(%s), now(), '[)')
                    WHERE %s && tstzrange(lower(%s), now(), '[)')
                    AND %s = (SELECT MAX(%s) FROM %s WHERE %s = NEW.%s);

                    -- Insert new version
                    NEW.%s = COALESCE((
                        SELECT MAX(%s) FROM %s WHERE %s = NEW.%s
                    ), 0) + 1;

                    NEW.%s = tstzrange(now(), NULL, '[)');

                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;
                """.formatted(
                tableName, temporalTableName,
                systemTimeColumn, systemTimeColumn, primaryKeyColumn,
                systemTimeColumn, versionColumn, versionColumn,
                temporalTableName, primaryKeyColumn, primaryKeyColumn,
                versionColumn, versionColumn, temporalTableName, primaryKeyColumn,
                systemTimeColumn
            );

            jdbcTemplate.execute(createFunctionSQL);

            // Create trigger
            String createTriggerSQL = """
                DROP TRIGGER IF EXISTS %s_history_trigger ON %s;
                CREATE TRIGGER %s_history_trigger
                    BEFORE INSERT OR UPDATE OR DELETE ON %s
                    FOR EACH ROW
                    EXECUTE FUNCTION %s_history_trigger();
                """.formatted(
                tableName, tableName,
                tableName, tableName, tableName
            );

            jdbcTemplate.execute(createTriggerSQL);

            log.info("Successfully created temporal table: {}", temporalTableName);

        } catch (Exception e) {
            log.error("Failed to create temporal table for: {}", tableName, e);
            throw new RuntimeException("Failed to create temporal table", e);
        }
    }

    /**
     * Create all temporal tables
     */
    public void createAllTemporalTables() {
        log.info("Creating all temporal tables");

        createTemporalTable("customer", "id");
        createTemporalTable("orders", "id");
        createTemporalTable("payment", "id");
        createTemporalTable("invoice", "id");
        createTemporalTable("subscription", "id");
        createTemporalTable("product", "id");
        createTemporalTable("address", "id");

        log.info("All temporal tables created successfully");
    }

    /**
     * Get historical versions of an entity
     */
    public List<Map<String, Object>> getHistory(String tableName, String id) {
        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT *,
                   lower(sys_period) AS valid_from,
                   upper(sys_period) AS valid_to,
                   version
            FROM %s
            WHERE %s = ?
            ORDER BY version DESC
            """.formatted(historyTable, tableName.split("_")[0]);

        return jdbcTemplate.queryForList(selectSQL, id);
    }

    /**
     * Get version at specific time
     */
    public Map<String, Object> getVersionAtTime(String tableName, String id, Instant atTime) {
        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT *,
                   lower(sys_period) AS valid_from,
                   upper(sys_period) AS valid_to,
                   version
            FROM %s
            WHERE %s = ?
            AND sys_period @> ?
            ORDER BY version DESC
            LIMIT 1
            """.formatted(historyTable, tableName.split("_")[0]);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(selectSQL, id, atTime.toString());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get all versions between time range
     */
    public List<Map<String, Object>> getHistoryBetween(
            String tableName, String id, Instant fromTime, Instant toTime) {

        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT *,
                   lower(sys_period) AS valid_from,
                   upper(sys_period) AS valid_to,
                   version
            FROM %s
            WHERE %s = ?
            AND sys_period && tstzrange(?, ?)
            ORDER BY version ASC
            """.formatted(historyTable, tableName.split("_")[0]);

        return jdbcTemplate.queryForList(
            selectSQL,
            id,
            fromTime.toString(),
            toTime.toString()
        );
    }

    /**
     * Get all changes within time range
     */
    public List<Map<String, Object>> getChangesInRange(
            String tableName, Instant fromTime, Instant toTime) {

        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT *,
                   lower(sys_period) AS valid_from,
                   upper(sys_period) AS valid_to,
                   version
            FROM %s
            WHERE lower(sys_period) >= ?
            AND lower(sys_period) <= ?
            ORDER BY lower(sys_period) ASC
            """.formatted(historyTable);

        return jdbcTemplate.queryForList(
            selectSQL,
            fromTime.toString(),
            toTime.toString()
        );
    }

    /**
     * Get latest version
     */
    public Map<String, Object> getLatestVersion(String tableName, String id) {
        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT *,
                   lower(sys_period) AS valid_from,
                   upper(sys_period) AS valid_to,
                   version
            FROM %s
            WHERE %s = ?
            ORDER BY version DESC
            LIMIT 1
            """.formatted(historyTable, tableName.split("_")[0]);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(selectSQL, id);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get change history summary
     */
    public List<Map<String, Object>> getChangeSummary(String tableName, String id) {
        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT
                version,
                lower(sys_period) AS changed_at,
                upper(sys_period) AS valid_until,
                CASE
                    WHEN upper(sys_period) = 'infinity' THEN 'Current'
                    ELSE 'Historical'
                END AS status,
                EXTRACT(EPOCH FROM (COALESCE(upper(sys_period), NOW()) - lower(sys_period))) AS duration_seconds
            FROM %s
            WHERE %s = ?
            ORDER BY version DESC
            """.formatted(historyTable, tableName.split("_")[0]);

        return jdbcTemplate.queryForList(selectSQL, id);
    }

    /**
     * Find entities changed since a time
     */
    public List<Map<String, Object>> findChangedSince(
            String tableName, Instant sinceTime) {

        String historyTable = tableName + "_history";
        String selectSQL = """
            SELECT DISTINCT ON (%s) %s, *
            FROM %s
            WHERE lower(sys_period) >= ?
            ORDER BY %s, version DESC
            """.formatted(
            tableName.split("_")[0],
            tableName.split("_")[0],
            historyTable,
            tableName.split("_")[0]
        );

        return jdbcTemplate.queryForList(selectSQL, sinceTime.toString());
    }

    /**
     * Reconstruct state at specific time
     */
    public Map<String, Object> reconstructStateAtTime(
            String tableName, String id, Instant targetTime) {

        String historyTable = tableName + "_history";
        String reconstructSQL = """
            WITH state AS (
                SELECT *
                FROM %s
                WHERE %s = ?
                AND sys_period @> ?
                ORDER BY version DESC
                LIMIT 1
            )
            SELECT state.*,
                   lower(sys_period) AS valid_from,
                   upper(sys_period) AS valid_to,
                   version
            FROM state
            """.formatted(historyTable, tableName.split("_")[0]);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            reconstructSQL, id, targetTime.toString()
        );

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get version count
     */
    public int getVersionCount(String tableName, String id) {
        String historyTable = tableName + "_history";
        String countSQL = """
            SELECT COUNT(*)
            FROM %s
            WHERE %s = ?
            """.formatted(historyTable, tableName.split("_")[0]);

        Integer count = jdbcTemplate.queryForObject(countSQL, Integer.class, id);
        return count != null ? count : 0;
    }

    /**
     * Delete history older than specified time
     */
    public int cleanupOldHistory(String tableName, Instant cutoffTime) {
        String historyTable = tableName + "_history";
        String deleteSQL = """
            DELETE FROM %s
            WHERE upper(sys_period) < ?
            AND upper(sys_period) != 'infinity'
            """.formatted(historyTable);

        int deleted = jdbcTemplate.update(deleteSQL, cutoffTime.toString());
        log.info("Cleaned up {} old history records from {}", deleted, historyTable);
        return deleted;
    }

    /**
     * Get temporal tables list
     */
    public List<String> getTemporalTables() {
        String selectSQL = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_name LIKE '%_history'
            AND table_schema = 'public'
            ORDER BY table_name
            """;
        return jdbcTemplate.queryForList(selectSQL, String.class);
    }

    /**
     * Verify temporal table structure
     */
    public boolean verifyTemporalTable(String tableName) {
        String historyTable = tableName + "_history";
        String verifySQL = """
            SELECT COUNT(*) > 0
            FROM information_schema.columns
            WHERE table_name = ?
            AND column_name = 'sys_period'
            """;

        Boolean result = jdbcTemplate.queryForObject(verifySQL, Boolean.class, historyTable);
        return result != null && result;
    }
}
