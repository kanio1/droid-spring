-- =====================================================
-- TimescaleDB Extension Installation and Hypertables
-- =====================================================

-- Install TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create time-series tables for performance metrics
-- This table will store database performance metrics over time
CREATE TABLE IF NOT EXISTS performance_metrics (
    time TIMESTAMPTZ NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    tags JSONB,
    PRIMARY KEY (time, metric_name)
);

-- Create hypertable for performance_metrics
-- This automatically partitions data by time
SELECT create_hypertable('performance_metrics', 'time', if_not_exists => TRUE);

-- Enable compression on the hypertable
-- Compress data older than 7 days
ALTER TABLE performance_metrics SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'time DESC',
    timescaledb.compress_segmentby = 'metric_name'
);

-- Compress data older than 7 days
SELECT add_compression_policy('performance_metrics', INTERVAL '7 days');

-- Create time-series table for business metrics
-- This table will store business KPIs and metrics
CREATE TABLE IF NOT EXISTS business_metrics (
    time TIMESTAMPTZ NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    customer_id UUID,
    product_id UUID,
    metadata JSONB,
    PRIMARY KEY (time, metric_name, customer_id, product_id)
);

-- Create hypertable for business_metrics
SELECT create_hypertable('business_metrics', 'time', if_not_exists => TRUE);

-- Enable compression on business_metrics
ALTER TABLE business_metrics SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'time DESC',
    timescaledb.compress_segmentby = 'metric_name'
);

-- Compress data older than 30 days
SELECT add_compression_policy('business_metrics', INTERVAL '30 days');

-- Create time-series table for system resource metrics
-- This table will store CPU, memory, disk usage over time
CREATE TABLE IF NOT EXISTS resource_metrics (
    time TIMESTAMPTZ NOT NULL,
    host VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL, -- cpu, memory, disk, network
    resource_name VARCHAR(100) NOT NULL, -- cpu0, memory, /dev/sda1, eth0
    usage_percent DOUBLE PRECISION,
    usage_value DOUBLE PRECISION,
    usage_unit VARCHAR(20),
    PRIMARY KEY (time, host, resource_type, resource_name)
);

-- Create hypertable for resource_metrics
SELECT create_hypertable('resource_metrics', 'time', if_not_exists => TRUE);

-- Enable compression
ALTER TABLE resource_metrics SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'time DESC',
    timescaledb.compress_segmentby = 'resource_type'
);

-- Compress data older than 7 days
SELECT add_compression_policy('resource_metrics', INTERVAL '7 days');

-- Create time-series table for application metrics
-- This table will store application-specific metrics (active users, request rate, etc.)
CREATE TABLE IF NOT EXISTS application_metrics (
    time TIMESTAMPTZ NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_unit VARCHAR(20),
    service_name VARCHAR(100),
    endpoint VARCHAR(200),
    status_code INTEGER,
    PRIMARY KEY (time, metric_name, service_name)
);

-- Create hypertable for application_metrics
SELECT create_hypertable('application_metrics', 'time', if_not_exists => TRUE);

-- Enable compression
ALTER TABLE application_metrics SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'time DESC',
    timescaledb.compress_segmentby = 'metric_name'
);

-- Compress data older than 14 days
SELECT add_compression_policy('application_metrics', INTERVAL '14 days');

-- Create aggregation views for different time periods
-- This will help with dashboard queries and performance

-- 1-minute aggregations (keep for 1 week)
CREATE MATERIALIZED VIEW IF NOT EXISTS performance_metrics_1m
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) as bucket,
    metric_name,
    AVG(metric_value) as avg_value,
    MIN(metric_value) as min_value,
    MAX(metric_value) as max_value,
    COUNT(*) as sample_count
FROM performance_metrics
GROUP BY bucket, metric_name;

-- Add continuous aggregate policy
SELECT add_continuous_aggregate_policy('performance_metrics_1m',
    start_offset => INTERVAL '1 week',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- 5-minute aggregations (keep for 1 month)
CREATE MATERIALIZED VIEW IF NOT EXISTS performance_metrics_5m
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('5 minutes', time) as bucket,
    metric_name,
    AVG(metric_value) as avg_value,
    MIN(metric_value) as min_value,
    MAX(metric_value) as max_value,
    COUNT(*) as sample_count
FROM performance_metrics
GROUP BY bucket, metric_name;

SELECT add_continuous_aggregate_policy('performance_metrics_5m',
    start_offset => INTERVAL '1 month',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Hourly aggregations (keep for 1 year)
CREATE MATERIALIZED VIEW IF NOT EXISTS performance_metrics_1h
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) as bucket,
    metric_name,
    AVG(metric_value) as avg_value,
    MIN(metric_value) as min_value,
    MAX(metric_value) as max_value,
    COUNT(*) as sample_count
FROM performance_metrics
GROUP BY bucket, metric_name;

SELECT add_continuous_aggregate_policy('performance_metrics_1h',
    start_offset => INTERVAL '1 year',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Business metrics aggregations
CREATE MATERIALIZED VIEW IF NOT EXISTS business_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) as bucket,
    metric_name,
    customer_id,
    product_id,
    AVG(metric_value) as avg_value,
    MIN(metric_value) as min_value,
    MAX(metric_value) as max_value,
    SUM(metric_value) as sum_value,
    COUNT(*) as sample_count
FROM business_metrics
GROUP BY bucket, metric_name, customer_id, product_id;

SELECT add_continuous_aggregate_policy('business_metrics_hourly',
    start_offset => INTERVAL '6 months',
    end_offset => INTERVAL '2 hours',
    schedule_interval => INTERVAL '2 hours');

-- Create indexes for better query performance
-- Performance metrics indexes
CREATE INDEX IF NOT EXISTS idx_performance_metrics_name ON performance_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_tags ON performance_metrics USING GIN (tags);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_1m ON performance_metrics_1m(bucket, metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_5m ON performance_metrics_5m(bucket, metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_metrics_1h ON performance_metrics_1h(bucket, metric_name);

-- Business metrics indexes
CREATE INDEX IF NOT EXISTS idx_business_metrics_name ON business_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_business_metrics_customer ON business_metrics(customer_id);
CREATE INDEX IF NOT EXISTS idx_business_metrics_product ON business_metrics(product_id);
CREATE INDEX IF NOT EXISTS idx_business_metrics_hourly ON business_metrics_hourly(bucket, metric_name, customer_id, product_id);

-- Resource metrics indexes
CREATE INDEX IF NOT EXISTS idx_resource_metrics_host ON resource_metrics(host);
CREATE INDEX IF NOT EXISTS idx_resource_metrics_type ON resource_metrics(resource_type);
CREATE INDEX IF NOT EXISTS idx_resource_metrics_time ON resource_metrics(time DESC);

-- Application metrics indexes
CREATE INDEX IF NOT EXISTS idx_application_metrics_name ON application_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_application_metrics_service ON application_metrics(service_name);
CREATE INDEX IF NOT EXISTS idx_application_metrics_time ON application_metrics(time DESC);

-- Create utility functions for inserting metrics
-- Function to insert a performance metric
CREATE OR REPLACE FUNCTION insert_performance_metric(
    p_metric_name VARCHAR,
    p_metric_value DOUBLE PRECISION,
    p_metric_unit VARCHAR DEFAULT NULL,
    p_tags JSONB DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO performance_metrics (time, metric_name, metric_value, metric_unit, tags)
    VALUES (NOW(), p_metric_name, p_metric_value, p_metric_unit, p_tags);
END;
$$ LANGUAGE plpgsql;

-- Function to insert a business metric
CREATE OR REPLACE FUNCTION insert_business_metric(
    p_metric_name VARCHAR,
    p_metric_value DOUBLE PRECISION,
    p_metric_unit VARCHAR DEFAULT NULL,
    p_customer_id UUID DEFAULT NULL,
    p_product_id UUID DEFAULT NULL,
    p_metadata JSONB DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO business_metrics (time, metric_name, metric_value, metric_unit, customer_id, product_id, metadata)
    VALUES (NOW(), p_metric_name, p_metric_value, p_metric_unit, p_customer_id, p_product_id, p_metadata);
END;
$$ LANGUAGE plpgsql;

-- Function to insert a resource metric
CREATE OR REPLACE FUNCTION insert_resource_metric(
    p_host VARCHAR,
    p_resource_type VARCHAR,
    p_resource_name VARCHAR,
    p_usage_percent DOUBLE PRECISION,
    p_usage_value DOUBLE PRECISION DEFAULT NULL,
    p_usage_unit VARCHAR DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO resource_metrics (time, host, resource_type, resource_name, usage_percent, usage_value, usage_unit)
    VALUES (NOW(), p_host, p_resource_type, p_resource_name, p_usage_percent, p_usage_value, p_usage_unit);
END;
$$ LANGUAGE plpgsql;

-- Function to insert an application metric
CREATE OR REPLACE FUNCTION insert_application_metric(
    p_metric_name VARCHAR,
    p_metric_value DOUBLE PRECISION,
    p_metric_unit VARCHAR DEFAULT NULL,
    p_service_name VARCHAR DEFAULT NULL,
    p_endpoint VARCHAR DEFAULT NULL,
    p_status_code INTEGER DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO application_metrics (time, metric_name, metric_value, metric_unit, service_name, endpoint, status_code)
    VALUES (NOW(), p_metric_name, p_metric_value, p_metric_unit, p_service_name, p_endpoint, p_status_code);
END;
$$ LANGUAGE plpgsql;

-- Create retention policies
-- Keep raw performance data for 90 days
SELECT add_retention_policy('performance_metrics', INTERVAL '90 days');

-- Keep raw business data for 1 year
SELECT add_retention_policy('business_metrics', INTERVAL '1 year');

-- Keep raw resource data for 30 days
SELECT add_retention_policy('resource_metrics', INTERVAL '30 days');

-- Keep raw application data for 60 days
SELECT add_retention_policy('application_metrics', INTERVAL '60 days');

-- Create sample data insertion function for testing
CREATE OR REPLACE FUNCTION insert_sample_metrics()
RETURNS VOID AS $$
BEGIN
    -- Insert sample performance metrics
    INSERT INTO performance_metrics (time, metric_name, metric_value, metric_unit, tags)
    VALUES
        (NOW(), 'db_query_time', 125.5, 'ms', '{"query_type": "select", "table": "customers"}'),
        (NOW(), 'db_connections_active', 45, 'connections', '{"pool": "primary"}'),
        (NOW(), 'db_cache_hit_ratio', 98.5, 'percent', NULL);

    -- Insert sample business metrics
    INSERT INTO business_metrics (time, metric_name, metric_value, metric_unit, customer_id, product_id, metadata)
    VALUES
        (NOW() - INTERVAL '1 hour', 'revenue', 1500.00, 'USD', gen_random_uuid(), gen_random_uuid(), '{"source": "subscription"}'),
        (NOW() - INTERVAL '1 hour', 'active_users', 1250, 'users', NULL, NULL, '{"type": "daily"}');

    -- Insert sample resource metrics
    INSERT INTO resource_metrics (time, host, resource_type, resource_name, usage_percent, usage_value, usage_unit)
    VALUES
        (NOW(), 'server-01', 'cpu', 'cpu0', 45.5, 0.455, 'percent'),
        (NOW(), 'server-01', 'memory', 'mem', 62.3, 15728.0, 'MB'),
        (NOW(), 'server-01', 'disk', '/dev/sda1', 78.9, 236.7, 'GB');

    -- Insert sample application metrics
    INSERT INTO application_metrics (time, metric_name, metric_value, metric_unit, service_name, endpoint, status_code)
    VALUES
        (NOW() - INTERVAL '5 minutes', 'http_requests', 150, 'requests', 'api-gateway', '/api/customers', 200),
        (NOW() - INTERVAL '5 minutes', 'http_requests', 5, 'requests', 'api-gateway', '/api/payments', 500);

    RAISE NOTICE 'Sample metrics inserted successfully';
END;
$$ LANGUAGE plpgsql;

-- Grant necessary permissions
GRANT USAGE ON SCHEMA timescaledb TO postgres;
GRANT USAGE ON SCHEMA timescaledb TO bss_app;
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA public TO bss_app;
GRANT ALL ON ALL MATERIALIZED VIEWS IN SCHEMA public TO postgres;
GRANT ALL ON ALL MATERIALIZED VIEWS IN SCHEMA public TO bss_app;

-- Add comments
COMMENT ON TABLE performance_metrics IS 'Time-series performance metrics from database and system';
COMMENT ON TABLE business_metrics IS 'Time-series business KPIs and metrics';
COMMENT ON TABLE resource_metrics IS 'Time-series system resource usage metrics';
COMMENT ON TABLE application_metrics IS 'Time-series application performance metrics';
COMMENT ON FUNCTION insert_performance_metric(VARCHAR, DOUBLE PRECISION, VARCHAR, JSONB) IS 'Insert a performance metric with automatic timestamp';
COMMENT ON FUNCTION insert_business_metric(VARCHAR, DOUBLE PRECISION, VARCHAR, UUID, UUID, JSONB) IS 'Insert a business metric with automatic timestamp';
COMMENT ON FUNCTION insert_resource_metric(VARCHAR, VARCHAR, VARCHAR, DOUBLE PRECISION, DOUBLE PRECISION, VARCHAR) IS 'Insert a resource metric with automatic timestamp';
COMMENT ON FUNCTION insert_application_metric(VARCHAR, DOUBLE PRECISION, VARCHAR, VARCHAR, VARCHAR, INTEGER) IS 'Insert an application metric with automatic timestamp';
