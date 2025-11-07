#!/bin/bash
# ============================================
# TimescaleDB Setup Script
# ============================================
# Purpose: Install and configure TimescaleDB extension
# Enables time-series data storage and analytics
# Created: 2025-11-07
# ============================================

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-bss}"
DB_USER="${DB_USER:-bss_app}"
LOG_FILE="/tmp/timescaledb-setup.log"

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

# Check PostgreSQL connection
check_postgres() {
    log "Checking PostgreSQL connection..."

    if ! PGPASSWORD=$POSTGRES_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
        error "Cannot connect to PostgreSQL. Please ensure PostgreSQL is running."
    fi

    log "PostgreSQL connection successful"
}

# Install TimescaleDB extension
install_timescaledb() {
    log "Installing TimescaleDB extension..."

    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
-- Install TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Verify installation
SELECT extname, extversion FROM pg_extension WHERE extname = 'timescaledb';

-- Enable automatic compression
ALTER DATABASE bss SET timescaledb.max_background_workers = 8;
ALTER DATABASE bss SET timescaledb.compression_policy = 'on';

-- Create hypertables for time-series data
EOF

    if [ $? -eq 0 ]; then
        log "TimescaleDB extension installed successfully"
    else
        error "Failed to install TimescaleDB extension"
    fi
}

# Create hypertables
create_hypertables() {
    log "Creating hypertable schemas..."

    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
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

-- Convert to hypertables
SELECT create_hypertable('customer_metrics', 'time', if_not_exists => TRUE);
SELECT create_hypertable('order_metrics', 'time', if_not_exists => TRUE);
SELECT create_hypertable('payment_metrics', 'time', if_not_exists => TRUE);
SELECT create_hypertable('revenue_metrics', 'time', if_not_exists => TRUE);
SELECT create_hypertable('system_metrics', 'time', if_not_exists => TRUE);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_customer_metrics_name_time ON customer_metrics (metric_name, time DESC);
CREATE INDEX IF NOT EXISTS idx_order_metrics_customer_time ON order_metrics (customer_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_payment_metrics_customer_time ON payment_metrics (customer_id, time DESC);
CREATE INDEX IF NOT EXISTS idx_revenue_metrics_time ON revenue_metrics (time DESC);
CREATE INDEX IF NOT EXISTS idx_system_metrics_service_time ON system_metrics (service_name, time DESC);

-- Enable compression (after 7 days, keep data for 2 years)
SELECT add_compression_policy('customer_metrics', INTERVAL '7 days');
SELECT add_compression_policy('order_metrics', INTERVAL '7 days');
SELECT add_compression_policy('payment_metrics', INTERVAL '7 days');
SELECT add_compression_policy('revenue_metrics', INTERVAL '7 days');
SELECT add_compression_policy('system_metrics', INTERVAL '7 days');

-- Create data retention policies (keep for 2 years)
SELECT add_retention_policy('customer_metrics', INTERVAL '2 years');
SELECT add_retention_policy('order_metrics', INTERVAL '2 years');
SELECT add_retention_policy('payment_metrics', INTERVAL '2 years');
SELECT add_retention_policy('revenue_metrics', INTERVAL '2 years');
SELECT add_retention_policy('system_metrics', INTERVAL '2 years');

-- Create continuous aggregates for common queries
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

-- Create continuous aggregate for customer activity
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

-- Create continuous aggregate for payment analysis
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

-- Refresh policies for continuous aggregates
SELECT add_continuous_aggregate_policy('revenue_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

SELECT add_continuous_aggregate_policy('customer_activity_hourly',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '15 minutes',
    schedule_interval => INTERVAL '15 minutes');

SELECT add_continuous_aggregate_policy('payment_status_daily',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO application_role;
GRANT SELECT ON ALL MATERIALIZED VIEWS IN SCHEMA public TO application_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO application_role;

-- Grant permissions to admin role
GRANT ALL ON ALL TABLES IN SCHEMA public TO admin_role;
GRANT ALL ON ALL MATERIALIZED VIEWS IN SCHEMA public TO admin_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO admin_role;

-- Display hypertable information
SELECT
    hypertable_name,
    dimensions,
    distribution_option,
    segmentby,
    time_column_name,
    chunk_time_interval
FROM timescaledb_information.hypertables
WHERE hypertable_schema = 'public';

-- Display continuous aggregates information
SELECT
    view_name,
    view_definition
FROM timescaledb_information.continuous_aggregates
WHERE view_schema = 'public';
EOF

    if [ $? -eq 0 ]; then
        log "Hypertables created successfully"
    else
        error "Failed to create hypertables"
    fi
}

# Create sample data for testing
create_sample_data() {
    log "Creating sample time-series data..."

    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
-- Insert sample data for the last 30 days
INSERT INTO customer_metrics (time, customer_id, metric_name, metric_value, labels)
SELECT
    NOW() - (interval '1 day' * (random() * 30)) as time,
    gen_random_uuid() as customer_id,
    CASE (random() * 3)::int
        WHEN 0 THEN 'login_count'
        WHEN 1 THEN 'page_views'
        WHEN 2 THEN 'session_duration'
        ELSE 'conversion_rate'
    END as metric_name,
    (random() * 100)::double precision as metric_value,
    '{"source": "web", "device": "mobile"}' as labels
FROM generate_series(1, 1000);

INSERT INTO order_metrics (time, order_id, customer_id, status, total_amount, items_count, region)
SELECT
    NOW() - (interval '1 day' * (random() * 30)) as time,
    gen_random_uuid() as order_id,
    gen_random_uuid() as customer_id,
    CASE (random() * 4)::int
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'PROCESSING'
        WHEN 2 THEN 'SHIPPED'
        ELSE 'DELIVERED'
    END as status,
    (random() * 1000 + 50)::decimal(12, 2) as total_amount,
    (random() * 10 + 1)::integer as items_count,
    CASE (random() * 4)::int
        WHEN 0 THEN 'US_EAST'
        WHEN 1 THEN 'US_WEST'
        WHEN 2 THEN 'EU'
        ELSE 'APAC'
    END as region
FROM generate_series(1, 500);

INSERT INTO payment_metrics (time, payment_id, order_id, customer_id, amount, status, payment_method, fraud_score)
SELECT
    NOW() - (interval '1 day' * (random() * 30)) as time,
    gen_random_uuid() as payment_id,
    gen_random_uuid() as order_id,
    gen_random_uuid() as customer_id,
    (random() * 1000 + 50)::decimal(12, 2) as amount,
    CASE (random() * 3)::int
        WHEN 0 THEN 'COMPLETED'
        WHEN 1 THEN 'PENDING'
        ELSE 'FAILED'
    END as status,
    CASE (random() * 3)::int
        WHEN 0 THEN 'CREDIT_CARD'
        WHEN 1 THEN 'PAYPAL'
        ELSE 'BANK_TRANSFER'
    END as payment_method,
    (random() * 100)::double precision as fraud_score
FROM generate_series(1, 500);

INSERT INTO revenue_metrics (time, revenue, costs, profit, orders_count, avg_order_value, region, product_category)
SELECT
    time_bucket('1 hour', NOW() - (interval '1 day' * (random() * 30))) as time,
    (random() * 10000 + 1000)::decimal(15, 2) as revenue,
    (random() * 8000 + 800)::decimal(15, 2) as costs,
    (random() * 2000 + 200)::decimal(15, 2) as profit,
    (random() * 100 + 10)::integer as orders_count,
    (random() * 200 + 50)::decimal(12, 2) as avg_order_value,
    CASE (random() * 4)::int
        WHEN 0 THEN 'US_EAST'
        WHEN 1 THEN 'US_WEST'
        WHEN 2 THEN 'EU'
        ELSE 'APAC'
    END as region,
    CASE (random() * 3)::int
        WHEN 0 THEN 'SOFTWARE'
        WHEN 1 THEN 'HARDWARE'
        ELSE 'SERVICES'
    END as product_category
FROM generate_series(1, 200);

INSERT INTO system_metrics (time, service_name, cpu_usage, memory_usage, request_rate, error_rate, latency_p99)
SELECT
    time_bucket('1 minute', NOW() - (interval '1 day' * (random() * 7))) as time,
    CASE (random() * 3)::int
        WHEN 0 THEN 'backend-api'
        WHEN 1 THEN 'frontend-app'
        ELSE 'kafka-consumer'
    END as service_name,
    (random() * 100)::double precision as cpu_usage,
    (random() * 100)::double precision as memory_usage,
    (random() * 1000)::double precision as request_rate,
    (random() * 5)::double precision as error_rate,
    (random() * 500)::double precision as latency_p99
FROM generate_series(1, 1000);

-- Refresh continuous aggregates
CALL refresh_continuous_aggregate('revenue_daily', NULL, NOW());
CALL refresh_continuous_aggregate('customer_activity_hourly', NULL, NOW());
CALL refresh_continuous_aggregate('payment_status_daily', NULL, NOW());

-- Display sample queries
\echo ''
\echo '========================================='
\echo 'Sample Queries'
\echo '========================================='
\echo ''

\echo '1. Daily revenue for the last 7 days:'
SELECT day, total_revenue, total_profit, total_orders
FROM revenue_daily
WHERE day > NOW() - INTERVAL '7 days'
ORDER BY day DESC
LIMIT 7;

\echo ''
\echo '2. Top 10 customers by activity:'
SELECT customer_id, COUNT(*) as activity_count
FROM customer_metrics
WHERE time > NOW() - INTERVAL '7 days'
GROUP BY customer_id
ORDER BY activity_count DESC
LIMIT 10;

\echo ''
\echo '3. Payment status breakdown (last 7 days):'
SELECT day, status, payment_count, total_amount
FROM payment_status_daily
WHERE day > NOW() - INTERVAL '7 days'
ORDER BY day DESC, status;

\echo ''
\echo '4. System performance (last 24 hours):'
SELECT service_name, AVG(cpu_usage) as avg_cpu, AVG(memory_usage) as avg_memory
FROM system_metrics
WHERE time > NOW() - INTERVAL '24 hours'
GROUP BY service_name
ORDER BY avg_cpu DESC;
EOF

    if [ $? -eq 0 ]; then
        log "Sample data created successfully"
    else
        warn "Failed to create sample data (this is optional)"
    fi
}

# Verify setup
verify_setup() {
    log "Verifying TimescaleDB setup..."

    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
-- Check extension
SELECT extname, extversion FROM pg_extension WHERE extname = 'timescaledb';

-- Check hypertables
SELECT COUNT(*) as hypertable_count
FROM timescaledb_information.hypertables
WHERE hypertable_schema = 'public';

-- Check continuous aggregates
SELECT COUNT(*) as continuous_aggregate_count
FROM timescaledb_information.continuous_aggregates
WHERE view_schema = 'public';

-- Check data
SELECT 'customer_metrics' as table_name, COUNT(*) as row_count FROM customer_metrics
UNION ALL
SELECT 'order_metrics' as table_name, COUNT(*) as row_count FROM order_metrics
UNION ALL
SELECT 'payment_metrics' as table_name, COUNT(*) as row_count FROM payment_metrics
UNION ALL
SELECT 'revenue_metrics' as table_name, COUNT(*) as row_count FROM revenue_metrics
UNION ALL
SELECT 'system_metrics' as table_name, COUNT(*) as row_count FROM system_metrics;
EOF

    if [ $? -eq 0 ]; then
        log "TimescaleDB setup verified successfully"
    else
        error "Setup verification failed"
    fi
}

# Show next steps
show_next_steps() {
    log ""
    log "==========================================="
    log "TimescaleDB Setup Complete!"
    log "==========================================="
    log ""
    log "Next steps:"
    log "  1. Use the time-series tables for your application data"
    log "  2. Access Grafana dashboard for visualization:"
    log "     https://grafana.bss.local/d/timescaledb/analytics"
    log "  3. Run custom analytics queries:"
    log "     - Daily revenue trends"
    log "     - Customer activity patterns"
    log "     - Payment fraud detection"
    log "  4. Monitor continuous aggregate refresh"
    log "  5. Adjust compression and retention policies as needed"
    log ""
    log "==========================================="
    log "Example Queries"
    log "==========================================="
    log ""
    log "-- Get revenue for last 30 days"
    log "SELECT day, total_revenue, total_profit"
    log "FROM revenue_daily"
    log "WHERE day > NOW() - INTERVAL '30 days'"
    log "ORDER BY day;"
    log ""
    log "-- Get customer activity heatmap"
    log "SELECT hour_bucket, customer_id, activity_count"
    log "FROM customer_activity_hourly"
    log "WHERE hour_bucket > NOW() - INTERVAL '7 days';"
    log ""
    log "-- Detect payment anomalies"
    log "SELECT * FROM payment_metrics"
    log "WHERE fraud_score > 90"
    log "AND time > NOW() - INTERVAL '24 hours';"
    log "==========================================="
}

# Main execution
main() {
    log "==========================================="
    log "TimescaleDB Setup"
    log "==========================================="
    log ""

    # Check PostgreSQL
    check_postgres

    # Install extension
    log ""
    install_timescaledb

    # Create hypertables
    log ""
    create_hypertables

    # Create sample data
    log ""
    read -p "Create sample data for testing? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        create_sample_data
    fi

    # Verify setup
    log ""
    verify_setup

    # Show next steps
    show_next_steps

    log ""
    log "TimescaleDB setup completed successfully!"
    log "Log file: $LOG_FILE"
}

# Run main function
main
