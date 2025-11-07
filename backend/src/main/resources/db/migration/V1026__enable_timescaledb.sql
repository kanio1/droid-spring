-- ============================================
-- TimescaleDB Extension and Hypertables
-- ============================================
-- Purpose: Enable time-series data storage and analytics
-- Migration: V1026__enable_timescaledb.sql
-- Created: 2025-11-07
-- ============================================

-- Install TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Verify installation
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        RAISE NOTICE 'TimescaleDB extension is installed';
    ELSE
        RAISE EXCEPTION 'TimescaleDB extension failed to install';
    END IF;
END
$$;

-- ============================================
-- Create time-series tables (hypertables)
-- ============================================

-- Customer activity metrics
CREATE TABLE IF NOT EXISTS customer_metrics (
    time TIMESTAMPTZ NOT NULL,
    customer_id UUID NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    labels JSONB,
    PRIMARY KEY (customer_id, metric_name, time)
);

-- Orders time-series data
CREATE TABLE IF NOT EXISTS order_metrics (
    time TIMESTAMPTZ NOT NULL,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    items_count INTEGER NOT NULL,
    region VARCHAR(100),
    tags JSONB,
    PRIMARY KEY (order_id, time)
);

-- Payment time-series data
CREATE TABLE IF NOT EXISTS payment_metrics (
    time TIMESTAMPTZ NOT NULL,
    payment_id UUID NOT NULL,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50),
    fraud_score DOUBLE PRECISION,
    PRIMARY KEY (payment_id, time)
);

-- Revenue metrics
CREATE TABLE IF NOT EXISTS revenue_metrics (
    time TIMESTAMPTZ NOT NULL,
    revenue DECIMAL(15, 2) NOT NULL,
    costs DECIMAL(15, 2) NOT NULL,
    profit DECIMAL(15, 2) NOT NULL,
    orders_count INTEGER NOT NULL,
    avg_order_value DECIMAL(12, 2) NOT NULL,
    region VARCHAR(100),
    product_category VARCHAR(100),
    PRIMARY KEY (time, region, product_category)
);

-- System performance metrics
CREATE TABLE IF NOT EXISTS system_metrics (
    time TIMESTAMPTZ NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    cpu_usage DOUBLE PRECISION,
    memory_usage DOUBLE PRECISION,
    request_rate DOUBLE PRECISION,
    error_rate DOUBLE PRECISION,
    latency_p99 DOUBLE PRECISION,
    PRIMARY KEY (time, service_name)
);

-- ============================================
-- Convert to hypertables
-- ============================================

-- Convert customer_metrics to hypertable
SELECT create_hypertable('customer_metrics', 'time', if_not_exists => TRUE);
SELECT add_retention_policy('customer_metrics', INTERVAL '2 years', if_not_exists => TRUE);
SELECT add_compression_policy('customer_metrics', INTERVAL '7 days', if_not_exists => TRUE);

-- Convert order_metrics to hypertable
SELECT create_hypertable('order_metrics', 'time', if_not_exists => TRUE);
SELECT add_retention_policy('order_metrics', INTERVAL '2 years', if_not_exists => TRUE);
SELECT add_compression_policy('order_metrics', INTERVAL '7 days', if_not_exists => TRUE);

-- Convert payment_metrics to hypertable
SELECT create_hypertable('payment_metrics', 'time', if_not_exists => TRUE);
SELECT add_retention_policy('payment_metrics', INTERVAL '2 years', if_not_exists => TRUE);
SELECT add_compression_policy('payment_metrics', INTERVAL '7 days', if_not_exists => TRUE);

-- Convert revenue_metrics to hypertable
SELECT create_hypertable('revenue_metrics', 'time', if_not_exists => TRUE);
SELECT add_retention_policy('revenue_metrics', INTERVAL '2 years', if_not_exists => TRUE);
SELECT add_compression_policy('revenue_metrics', INTERVAL '7 days', if_not_exists => TRUE);

-- Convert system_metrics to hypertable
SELECT create_hypertable('system_metrics', 'time', if_not_exists => TRUE);
SELECT add_retention_policy('system_metrics', INTERVAL '2 years', if_not_exists => TRUE);
SELECT add_compression_policy('system_metrics', INTERVAL '7 days', if_not_exists => TRUE);

-- ============================================
-- Create indexes for better performance
-- ============================================

-- Customer metrics indexes
CREATE INDEX IF NOT EXISTS idx_customer_metrics_name_time
    ON customer_metrics (metric_name, time DESC);
CREATE INDEX IF NOT EXISTS idx_customer_metrics_customer_time
    ON customer_metrics (customer_id, time DESC);

-- Order metrics indexes
CREATE INDEX IF NOT EXISTS idx_order_metrics_customer_time
    ON order_metrics (customer_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_order_metrics_status_time
    ON order_metrics (status, time DESC);

-- Payment metrics indexes
CREATE INDEX IF NOT EXISTS idx_payment_metrics_customer_time
    ON payment_metrics (customer_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_payment_metrics_status_time
    ON payment_metrics (status, time DESC);

-- Revenue metrics indexes
CREATE INDEX IF NOT EXISTS idx_revenue_metrics_time
    ON revenue_metrics (time DESC);
CREATE INDEX IF NOT EXISTS idx_revenue_metrics_region
    ON revenue_metrics (region, time DESC);

-- System metrics indexes
CREATE INDEX IF NOT EXISTS idx_system_metrics_service_time
    ON system_metrics (service_name, time DESC);

-- ============================================
-- Create continuous aggregates for common queries
-- ============================================

-- Revenue daily aggregate
CREATE MATERIALIZED VIEW IF NOT EXISTS revenue_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS day,
    SUM(revenue) AS total_revenue,
    SUM(costs) AS total_costs,
    SUM(profit) AS total_profit,
    SUM(orders_count) AS total_orders,
    AVG(avg_order_value) AS avg_order_value
FROM revenue_metrics
GROUP BY day
WITH NO DATA;

-- Customer activity hourly aggregate
CREATE MATERIALIZED VIEW IF NOT EXISTS customer_activity_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS hour,
    customer_id,
    COUNT(*) AS activity_count,
    AVG(metric_value) AS avg_metric_value
FROM customer_metrics
GROUP BY hour, customer_id
WITH NO DATA;

-- Payment status daily aggregate
CREATE MATERIALIZED VIEW IF NOT EXISTS payment_status_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS day,
    status,
    COUNT(*) AS payment_count,
    SUM(amount) AS total_amount,
    AVG(fraud_score) AS avg_fraud_score
FROM payment_metrics
GROUP BY day, status
WITH NO DATA;

-- ============================================
-- Create refresh policies for continuous aggregates
-- ============================================

-- Refresh policy for revenue_daily
SELECT add_continuous_aggregate_policy('revenue_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- Refresh policy for customer_activity_hourly
SELECT add_continuous_aggregate_policy('customer_activity_hourly',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '15 minutes',
    schedule_interval => INTERVAL '15 minutes',
    if_not_exists => TRUE);

-- Refresh policy for payment_status_daily
SELECT add_continuous_aggregate_policy('payment_status_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

-- ============================================
-- Grant permissions
-- ============================================

-- Grant permissions to application role
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO application_role;
GRANT SELECT ON ALL MATERIALIZED VIEWS IN SCHEMA public TO application_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO application_role;

-- Grant permissions to admin role
GRANT ALL ON ALL TABLES IN SCHEMA public TO admin_role;
GRANT ALL ON ALL MATERIALIZED VIEWS IN SCHEMA public TO admin_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO admin_role;

-- ============================================
-- Create helper functions
-- ============================================

-- Function to record customer metric
CREATE OR REPLACE FUNCTION record_customer_metric(
    p_customer_id UUID,
    p_metric_name VARCHAR(100),
    p_metric_value DOUBLE PRECISION,
    p_labels JSONB DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO customer_metrics (time, customer_id, metric_name, metric_value, labels)
    VALUES (NOW(), p_customer_id, p_metric_name, p_metric_value, p_labels);
END;
$$;

-- Function to record order metric
CREATE OR REPLACE FUNCTION record_order_metric(
    p_order_id UUID,
    p_customer_id UUID,
    p_status VARCHAR(50),
    p_total_amount DECIMAL(12, 2),
    p_items_count INTEGER,
    p_region VARCHAR(100) DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO order_metrics (time, order_id, customer_id, status, total_amount, items_count, region)
    VALUES (NOW(), p_order_id, p_customer_id, p_status, p_total_amount, p_items_count, p_region);
END;
$$;

-- Function to record payment metric
CREATE OR REPLACE FUNCTION record_payment_metric(
    p_payment_id UUID,
    p_order_id UUID,
    p_customer_id UUID,
    p_amount DECIMAL(12, 2),
    p_status VARCHAR(50),
    p_payment_method VARCHAR(50),
    p_fraud_score DOUBLE PRECISION DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO payment_metrics (time, payment_id, order_id, customer_id, amount, status, payment_method, fraud_score)
    VALUES (NOW(), p_payment_id, p_order_id, p_customer_id, p_amount, p_status, p_payment_method, p_fraud_score);
END;
`;

-- Function to record revenue metric
CREATE OR REPLACE FUNCTION record_revenue_metric(
    p_revenue DECIMAL(15, 2),
    p_costs DECIMAL(15, 2),
    p_orders_count INTEGER,
    p_region VARCHAR(100) DEFAULT NULL,
    p_product_category VARCHAR(100) DEFAULT NULL
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_profit DECIMAL(15, 2);
    v_avg_order_value DECIMAL(12, 2);
BEGIN
    v_profit := p_revenue - p_costs;
    v_avg_order_value := CASE WHEN p_orders_count > 0
                         THEN p_revenue / p_orders_count
                         ELSE 0 END;

    INSERT INTO revenue_metrics (time, revenue, costs, profit, orders_count, avg_order_value, region, product_category)
    VALUES (NOW(), p_revenue, p_costs, v_profit, p_orders_count, v_avg_order_value, p_region, p_product_category);
END;
$$;

-- Function to record system metric
CREATE OR REPLACE FUNCTION record_system_metric(
    p_service_name VARCHAR(100),
    p_cpu_usage DOUBLE PRECISION,
    p_memory_usage DOUBLE PRECISION,
    p_request_rate DOUBLE PRECISION,
    p_error_rate DOUBLE PRECISION,
    p_latency_p99 DOUBLE PRECISION
) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO system_metrics (time, service_name, cpu_usage, memory_usage, request_rate, error_rate, latency_p99)
    VALUES (NOW(), p_service_name, p_cpu_usage, p_memory_usage, p_request_rate, p_error_rate, p_latency_p99);
END;
$$;

-- Grant execute permissions
GRANT EXECUTE ON FUNCTION record_customer_metric(UUID, VARCHAR(100), DOUBLE PRECISION, JSONB) TO application_role;
GRANT EXECUTE ON FUNCTION record_order_metric(UUID, UUID, VARCHAR(50), DECIMAL(12, 2), INTEGER, VARCHAR(100)) TO application_role;
GRANT EXECUTE ON FUNCTION record_payment_metric(UUID, UUID, UUID, DECIMAL(12, 2), VARCHAR(50), VARCHAR(50), DOUBLE PRECISION) TO application_role;
GRANT EXECUTE ON FUNCTION record_revenue_metric(DECIMAL(15, 2), DECIMAL(15, 2), INTEGER, VARCHAR(100), VARCHAR(100)) TO application_role;
GRANT EXECUTE ON FUNCTION record_system_metric(VARCHAR(100), DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION) TO application_role;

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO admin_role;

-- ============================================
-- Verification queries (for logging)
-- ============================================

DO $$
DECLARE
    ht_count INTEGER;
    ca_count INTEGER;
BEGIN
    -- Count hypertables
    SELECT COUNT(*) INTO ht_count
    FROM timescaledb_information.hypertables
    WHERE hypertable_schema = 'public';

    RAISE NOTICE 'Created % hypertables', ht_count;

    -- Count continuous aggregates
    SELECT COUNT(*) INTO ca_count
    FROM timescaledb_information.continuous_aggregates
    WHERE view_schema = 'public';

    RAISE NOTICE 'Created % continuous aggregates', ca_count;

    -- Verify tables exist
    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'customer_metrics') THEN
        RAISE NOTICE 'customer_metrics table created';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'order_metrics') THEN
        RAISE NOTICE 'order_metrics table created';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'payment_metrics') THEN
        RAISE NOTICE 'payment_metrics table created';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'revenue_metrics') THEN
        RAISE NOTICE 'revenue_metrics table created';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'system_metrics') THEN
        RAISE NOTICE 'system_metrics table created';
    END IF;

    RAISE NOTICE 'TimescaleDB migration completed successfully';
END
$$;
