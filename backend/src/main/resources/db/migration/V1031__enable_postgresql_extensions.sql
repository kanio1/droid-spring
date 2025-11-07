-- Enable PostgreSQL 18 Cloud-Native Extensions
-- This migration enables essential extensions for performance monitoring and optimization

-- 1. pg_stat_statements: Track execution statistics of all SQL statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements
    SCHEMA public;

-- 2. pg_hint_plan: Allow manual control of planner behavior with hints
-- Note: Requires superuser privileges
CREATE EXTENSION IF NOT EXISTS pg_hint_plan
    SCHEMA public;

-- 3. pg_cron: Schedule background jobs directly in PostgreSQL
CREATE EXTENSION IF NOT EXISTS pg_cron
    SCHEMA public;

-- Grant necessary permissions
GRANT EXECUTE ON FUNCTION pg_stat_statementsReset() TO bss_app;
GRANT EXECUTE ON FUNCTION pg_stat_statements(show text) TO bss_app;
GRANT EXECUTE ON FUNCTION pg_stat_statements(queryid bigint) TO bss_app;

-- Create view for easier access to query statistics
CREATE OR REPLACE VIEW performance_query_stats AS
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
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY mean_exec_time DESC;

-- Grant view access
GRANT SELECT ON performance_query_stats TO bss_app;

-- Create performance cache hit ratio view
CREATE OR REPLACE VIEW performance_cache_stats AS
SELECT
    datname,
    blks_read,
    blks_hit,
    CASE
        WHEN blks_read = 0 THEN 100.0
        ELSE 100.0 * blks_hit / (blks_read + blks_hit)
    END AS cache_hit_ratio,
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
WHERE datname = current_database();

GRANT SELECT ON performance_cache_stats TO bss_app;

-- Create index usage statistics view
CREATE OR REPLACE VIEW performance_index_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched,
    CASE
        WHEN idx_scan = 0 THEN 0
        ELSE round((idx_tup_fetch::numeric / idx_scan), 2)
    END as avg_tuples_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

GRANT SELECT ON performance_index_stats TO bss_app;

-- Create table bloat statistics view
CREATE OR REPLACE VIEW performance_table_stats AS
SELECT
    schemaname,
    tablename,
    n_tup_ins as rows_inserted,
    n_tup_upd as rows_updated,
    n_tup_del as rows_deleted,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows,
    CASE
        WHEN n_live_tup = 0 THEN 0
        ELSE round((n_dead_tup::numeric / n_live_tup) * 100, 2)
    END as dead_row_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze,
    vacuum_count,
    autovacuum_count,
    analyze_count,
    autoanalyze_count
FROM pg_stat_user_tables
WHERE schemaname NOT LIKE 'pg_%'
ORDER BY n_dead_tup DESC;

GRANT SELECT ON performance_table_stats TO bss_app;

-- Create function to get database size
CREATE OR REPLACE FUNCTION get_database_size()
RETURNS TABLE (
    database_name text,
    size_bytes bigint,
    size_human text
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        current_database() as database_name,
        pg_database_size(current_database()) as size_bytes,
        CASE
            WHEN pg_database_size(current_database()) > 1024^3 THEN
                round(pg_database_size(current_database())::numeric / 1024^3, 2) || ' GB'
            WHEN pg_database_size(current_database()) > 1024^2 THEN
                round(pg_database_size(current_database())::numeric / 1024^2, 2) || ' MB'
            ELSE
                round(pg_database_size(current_database())::numeric / 1024, 2) || ' KB'
        END as size_human;
END;
$$ LANGUAGE plpgsql;

GRANT EXECUTE ON FUNCTION get_database_size() TO bss_app;

-- Create function to get table sizes
CREATE OR REPLACE FUNCTION get_table_sizes(
    min_size_mb numeric DEFAULT 0
)
RETURNS TABLE (
    table_schema text,
    table_name text,
    size_mb numeric,
    total_size_mb numeric,
    index_size_mb numeric
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        schemaname as table_schema,
        tablename as table_name,
        pg_relation_size(tablename::regclass)::numeric / 1024^2 as size_mb,
        pg_total_relation_size(tablename::regclass)::numeric / 1024^2 as total_size_mb,
        (pg_total_relation_size(tablename::regclass) - pg_relation_size(tablename::regclass))::numeric / 1024^2 as index_size_mb
    FROM pg_tables
    WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
    AND pg_total_relation_size(tablename::regclass)::numeric / 1024^2 >= min_size_mb
    ORDER BY total_size_mb DESC;
END;
$$ LANGUAGE plpgsql;

GRANT EXECUTE ON FUNCTION get_table_sizes(numeric) TO bss_app;

-- Insert comment
COMMENT ON EXTENSION pg_stat_statements IS 'Track execution statistics of all SQL statements executed by the server';
COMMENT ON EXTENSION pg_hint_plan IS 'Allow manual control of query planner behavior with hint comments';
COMMENT ON EXTENSION pg_cron IS 'Schedule background jobs directly in PostgreSQL';
