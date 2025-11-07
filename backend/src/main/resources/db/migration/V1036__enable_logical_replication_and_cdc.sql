-- =====================================================
-- Logical Replication & CDC (Change Data Capture)
-- PostgreSQL 18 Real-Time Data Streaming
-- =====================================================

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS pgoutput; -- Built-in logical replication output plugin
CREATE EXTENSION IF NOT EXISTS decoderbufs; -- For streaming WAL to Kafka (if available)
CREATE EXTENSION IF NOT EXISTS wal2json; -- Alternative: JSON output for CDC

-- =====================================================
-- 1. LOGICAL REPLICATION SETUP
-- =====================================================

-- Create publication for logical replication
-- This will be used to stream changes to external systems
DROP PUBLICATION IF EXISTS bss_publication CASCADE;
CREATE PUBLICATION bss_publication
    FOR ALL TABLES
    WITH (publish = 'insert, update, delete');

-- Note: In production, you would typically create separate publications
-- for different data types or consumers
-- Example:
-- CREATE PUBLICATION customer_publication FOR TABLE customers, customer_addresses;
-- CREATE PUBLICATION order_publication FOR TABLE orders, order_items;

-- =====================================================
-- 2. CDC EVENT TRACKING
-- =====================================================

-- Create CDC event log table
CREATE TABLE IF NOT EXISTS cdc_event_log (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    event_type VARCHAR(20) NOT NULL, -- INSERT, UPDATE, DELETE
    table_name VARCHAR(100) NOT NULL,
    table_schema VARCHAR(50) NOT NULL DEFAULT 'public',
    operation_id BIGSERIAL, -- Monotonically increasing ID for ordering
    pk_value UUID, -- Primary key of the affected row
    old_data JSONB, -- Row state before change
    new_data JSONB, -- Row state after change
    changed_columns TEXT[], -- Array of column names that changed
    transaction_id BIGINT, -- Transaction ID
    lsn_pos VARCHAR(50), -- WAL location (Log Sequence Number)
    publisher_name VARCHAR(100), -- Name of the publisher
    consumer_group VARCHAR(100), -- Kafka consumer group or similar
    event_topic VARCHAR(200), -- Target topic/stream name
    retry_count INTEGER DEFAULT 0,
    processed BOOLEAN DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

-- Create indexes for CDC event log
CREATE INDEX IF NOT EXISTS idx_cdc_event_log_time ON cdc_event_log(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_cdc_event_log_table ON cdc_event_log(table_name, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_cdc_event_log_operation ON cdc_event_log(operation_id);
CREATE INDEX IF NOT EXISTS idx_cdc_event_log_processed ON cdc_event_log(processed, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_cdc_event_log_pk ON cdc_event_log(pk_value);
CREATE INDEX IF NOT EXISTS idx_cdc_event_log_consumer ON cdc_event_log(consumer_group, event_time DESC);

-- Partition CDC event log by day (for performance and retention)
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    i INTEGER;
BEGIN
    start_date := DATE_TRUNC('day', CURRENT_DATE);
    end_date := DATE_TRUNC('day', CURRENT_DATE + INTERVAL '90 days');

    FOR i IN 0..90 LOOP
        partition_name := 'cdc_event_log_' || TO_CHAR(start_date + (i * INTERVAL '1 day'), 'YYYY_MM_DD');
        EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF cdc_event_log FOR VALUES FROM (%L) TO (%L)',
                       partition_name,
                       start_date + (i * INTERVAL '1 day'),
                       start_date + ((i + 1) * INTERVAL '1 day'));
    END LOOP;
END $$;

-- Create function to record CDC events
CREATE OR REPLACE FUNCTION record_cdc_event(
    p_table_name TEXT,
    p_event_type TEXT,
    p_pk_value UUID,
    p_old_data JSONB DEFAULT NULL,
    p_new_data JSONB DEFAULT NULL,
    p_changed_columns TEXT[] DEFAULT NULL,
    p_consumer_group TEXT DEFAULT 'default',
    p_event_topic TEXT DEFAULT NULL
) RETURNS BIGINT AS $$
DECLARE
    op_id BIGINT;
BEGIN
    -- Insert CDC event record
    INSERT INTO cdc_event_log (
        event_type,
        table_name,
        pk_value,
        old_data,
        new_data,
        changed_columns,
        consumer_group,
        event_topic
    ) VALUES (
        p_event_type,
        p_table_name,
        p_pk_value,
        p_old_data,
        p_new_data,
        p_changed_columns,
        p_consumer_group,
        COALESCE(p_event_topic, 'cdc.' || p_table_name)
    ) RETURNING operation_id INTO op_id;

    RETURN op_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 3. CDC TRIGGERS
-- =====================================================

-- Function to capture changes and record CDC events
CREATE OR REPLACE FUNCTION cdc_capture_function()
RETURNS TRIGGER AS $$
DECLARE
    old_json JSONB;
    new_json JSONB;
    changed_cols TEXT[];
    op_id BIGINT;
BEGIN
    -- Convert OLD and NEW to JSONB
    old_json := row_to_json(OLD);
    new_json := row_to_json(NEW);

    -- Determine changed columns
    changed_cols := ARRAY[]::TEXT[];

    -- Record CDC event
    IF TG_OP = 'INSERT' THEN
        op_id := record_cdc_event(
            TG_TABLE_NAME,
            'INSERT',
            NEW.id,
            NULL,
            new_json,
            changed_cols,
            'analytics-service',
            'events.' || TG_TABLE_NAME
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        -- Track which columns changed
        -- In a real implementation, you would compare specific columns
        changed_cols := ARRAY['all_columns'];
        op_id := record_cdc_event(
            TG_TABLE_NAME,
            'UPDATE',
            NEW.id,
            old_json,
            new_json,
            changed_cols,
            'analytics-service',
            'events.' || TG_TABLE_NAME
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        op_id := record_cdc_event(
            TG_TABLE_NAME,
            'DELETE',
            OLD.id,
            old_json,
            NULL,
            changed_cols,
            'analytics-service',
            'events.' || TG_TABLE_NAME
        );
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create CDC triggers on key tables
DROP TRIGGER IF EXISTS cdc_customers ON customers;
CREATE TRIGGER cdc_customers
    AFTER INSERT OR UPDATE OR DELETE ON customers
    FOR EACH ROW EXECUTE FUNCTION cdc_capture_function();

DROP TRIGGER IF EXISTS cdc_invoices ON invoices;
CREATE TRIGGER cdc_invoices
    AFTER INSERT OR UPDATE OR DELETE ON invoices
    FOR EACH ROW EXECUTE FUNCTION cdc_capture_function();

DROP TRIGGER IF EXISTS cdc_payments ON payments;
CREATE TRIGGER cdc_payments
    AFTER INSERT OR UPDATE OR DELETE ON payments
    FOR EACH ROW EXECUTE FUNCTION cdc_capture_function();

DROP TRIGGER IF EXISTS cdc_orders ON orders;
CREATE TRIGGER cdc_orders
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW EXECUTE FUNCTION cdc_capture_function();

DROP TRIGGER IF EXISTS cdc_subscriptions ON subscriptions;
CREATE TRIGGER cdc_subscriptions
    AFTER INSERT OR UPDATE OR DELETE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION cdc_capture_function();

-- =====================================================
-- 4. KAFKA INTEGRATION
-- =====================================================

-- Create Kafka producer configuration table
CREATE TABLE IF NOT EXISTS kafka_producer_config (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producer_name VARCHAR(100) NOT NULL UNIQUE,
    kafka_bootstrap_servers TEXT NOT NULL,
    topic_prefix VARCHAR(100) NOT NULL DEFAULT 'postgres-cdc',
    batch_size INTEGER DEFAULT 100,
    linger_ms INTEGER DEFAULT 10,
    buffer_memory INTEGER DEFAULT 33554432, -- 32MB
    compression_type VARCHAR(20) DEFAULT 'gzip',
    retries INTEGER DEFAULT 3,
    acks VARCHAR(10) DEFAULT 'all',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Insert default Kafka producer config
INSERT INTO kafka_producer_config (
    producer_name,
    kafka_bootstrap_servers,
    topic_prefix
) VALUES (
    'default-producer',
    'kafka:9092',
    'bss-cdc'
) ON CONFLICT (producer_name) DO NOTHING;

-- Function to get CDC events for Kafka publishing
CREATE OR REPLACE FUNCTION get_cdc_events_for_kafka(
    p_consumer_group TEXT,
    p_batch_size INTEGER DEFAULT 100
) RETURNS TABLE (
    event_id UUID,
    event_time TIMESTAMPTZ,
    event_type VARCHAR(20),
    table_name VARCHAR(100),
    operation_id BIGINT,
    pk_value UUID,
    old_data JSONB,
    new_data JSONB,
    event_topic VARCHAR(200)
) AS $$
BEGIN
    -- Return unprocessed events for the consumer group
    RETURN QUERY
    SELECT
        e.event_id,
        e.event_time,
        e.event_type,
        e.table_name,
        e.operation_id,
        e.pk_value,
        e.old_data,
        e.new_data,
        e.event_topic
    FROM cdc_event_log e
    WHERE e.consumer_group = p_consumer_group
    AND e.processed = FALSE
    ORDER BY e.operation_id ASC
    LIMIT p_batch_size;
END;
$$ LANGUAGE plpgsql;

-- Function to mark events as processed
CREATE OR REPLACE FUNCTION mark_cdc_events_processed(
    p_event_ids UUID[]
) RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE cdc_event_log
    SET
        processed = TRUE,
        processed_at = NOW()
    WHERE event_id = ANY(p_event_ids);

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. REAL-TIME MATERIALIZED VIEWS
-- =====================================================

-- Create real-time materialized view for customer analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_customer_analytics AS
SELECT
    c.id,
    c.email,
    c.created_at,
    COUNT(DISTINCT o.id) as total_orders,
    COUNT(DISTINCT i.id) as total_invoices,
    COUNT(DISTINCT s.id) as active_subscriptions,
    COALESCE(SUM(i.total_amount), 0) as total_revenue,
    c.updated_at
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id
LEFT JOIN invoices i ON c.id = i.customer_id
LEFT JOIN subscriptions s ON c.id = s.customer_id AND s.status = 'ACTIVE'
GROUP BY c.id, c.email, c.created_at, c.updated_at;

-- Create unique index for materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_customer_analytics_id ON mv_customer_analytics(id);

-- Function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_customer_analytics_mv()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_analytics;
    RAISE NOTICE 'Customer analytics materialized view refreshed';
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically refresh materialized view
CREATE OR REPLACE FUNCTION refresh_mv_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    -- Refresh the materialized view after changes
    PERFORM refresh_customer_analytics_mv();
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_refresh_customer_analytics ON customers;
CREATE TRIGGER trigger_refresh_customer_analytics
    AFTER INSERT OR UPDATE OR DELETE ON customers
    FOR EACH STATEMENT EXECUTE FUNCTION refresh_mv_trigger_function();

-- Create real-time materialized view for revenue analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_revenue_analytics AS
SELECT
    DATE_TRUNC('day', i.created_at) as revenue_date,
    COUNT(DISTINCT i.id) as invoice_count,
    COUNT(DISTINCT i.customer_id) as unique_customers,
    SUM(i.total_amount) as daily_revenue,
    AVG(i.total_amount) as avg_invoice_amount,
    MAX(i.total_amount) as max_invoice_amount
FROM invoices i
WHERE i.status = 'PAID'
GROUP BY DATE_TRUNC('day', i.created_at)
ORDER BY revenue_date DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_revenue_analytics_date ON mv_revenue_analytics(revenue_date);

-- =====================================================
-- 6. STREAMING REPLICATION SLOT
-- =====================================================

-- Create replication slot for CDC streaming
-- This ensures WAL records are retained for CDC consumers
DROP_REPLICATION_SLOT IF EXISTS bss_cdc_slot;
SELECT * FROM pg_create_logical_replication_slot('bss_cdc_slot', 'pgoutput');

-- Note: In production, you would configure your CDC tool (Debezium, etc.)
-- to consume from this slot

-- =====================================================
-- 7. CDC MONITORING VIEWS
-- =====================================================

-- View for CDC event statistics
CREATE OR REPLACE VIEW cdc_event_stats AS
SELECT
    table_name,
    event_type,
    COUNT(*) as event_count,
    COUNT(CASE WHEN processed = TRUE THEN 1 END) as processed_count,
    COUNT(CASE WHEN processed = FALSE THEN 1 END) as pending_count,
    MIN(event_time) as first_event,
    MAX(event_time) as last_event
FROM cdc_event_log
GROUP BY table_name, event_type
ORDER BY table_name, event_type;

-- View for CDC event lag
CREATE OR REPLACE VIEW cdc_event_lag AS
SELECT
    consumer_group,
    table_name,
    COUNT(*) as total_events,
    COUNT(CASE WHEN processed = FALSE THEN 1 END) as pending_events,
    MAX(operation_id) as max_operation_id,
    COALESCE(MAX(operation_id) - MAX(
        CASE WHEN processed = TRUE THEN operation_id ELSE NULL END
    ), 0) as lag_count,
    NOW() - MAX(event_time) as max_lag_duration
FROM cdc_event_log
WHERE event_time >= NOW() - INTERVAL '24 hours'
GROUP BY consumer_group, table_name
ORDER BY lag_count DESC;

-- View for replication health
CREATE OR REPLACE VIEW replication_health AS
SELECT
    'PUBLICATION' as component,
    'bss_publication' as name,
    CASE
        WHEN EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'bss_publication')
        THEN 'HEALTHY'
        ELSE 'ERROR'
    END as status,
    NOW() as check_time;

-- =====================================================
-- 8. EVENT SOURCING SUPPORT
-- =====================================================

-- Create event store table for event sourcing pattern
CREATE TABLE IF NOT EXISTS event_store (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    aggregate_id UUID NOT NULL, -- ID of the aggregate (e.g., customer ID, order ID)
    aggregate_type VARCHAR(50) NOT NULL, -- e.g., Customer, Order, Invoice
    event_data JSONB NOT NULL, -- Event payload
    metadata JSONB, -- Additional metadata (user ID, timestamp, etc.)
    event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    causation_id UUID, -- ID of the event that caused this event
    correlation_id UUID, -- ID to correlate related events
    processed_sequence INTEGER, -- For ordering events within aggregate
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for event store
CREATE INDEX IF NOT EXISTS idx_event_store_aggregate ON event_store(aggregate_type, aggregate_id, processed_sequence);
CREATE INDEX IF NOT EXISTS idx_event_store_type ON event_store(event_type, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_event_store_time ON event_store(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_event_store_correlation ON event_store(correlation_id);

-- Function to append event to event store
CREATE OR REPLACE FUNCTION append_event(
    p_aggregate_id UUID,
    p_aggregate_type TEXT,
    p_event_type TEXT,
    p_event_data JSONB,
    p_metadata JSONB DEFAULT NULL,
    p_causation_id UUID DEFAULT NULL,
    p_correlation_id UUID DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    next_sequence INTEGER;
    event_uuid UUID;
BEGIN
    -- Get next sequence number for the aggregate
    SELECT COALESCE(MAX(processed_sequence), 0) + 1
    INTO next_sequence
    FROM event_store
    WHERE aggregate_id = p_aggregate_id
    AND aggregate_type = p_aggregate_type;

    -- Insert event
    INSERT INTO event_store (
        aggregate_id,
        aggregate_type,
        event_type,
        event_data,
        metadata,
        causation_id,
        correlation_id,
        processed_sequence
    ) VALUES (
        p_aggregate_id,
        p_aggregate_type,
        p_event_type,
        p_event_data,
        p_metadata,
        p_causation_id,
        p_correlation_id,
        next_sequence
    ) RETURNING event_id INTO event_uuid;

    RETURN event_uuid;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 9. PERMISSIONS
-- =====================================================

-- Grant necessary permissions
GRANT USAGE ON SCHEMA public TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL ON ALL MATERIALIZED VIEWS IN SCHEMA public TO postgres;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO postgres;

-- =====================================================
-- 10. DOCUMENTATION
-- =====================================================

-- Add comments
COMMENT ON PUBLICATION bss_publication IS 'Logical replication publication for CDC streaming';
COMMENT ON TABLE cdc_event_log IS 'Change Data Capture event log for real-time data streaming';
COMMENT ON TABLE kafka_producer_config IS 'Kafka producer configuration for CDC streaming';
COMMENT ON TABLE event_store IS 'Event store for event sourcing pattern implementation';
COMMENT ON VIEW cdc_event_stats IS 'CDC event statistics by table and event type';
COMMENT ON VIEW cdc_event_lag IS 'CDC event lag monitoring by consumer group';
COMMENT ON VIEW replication_health IS 'Replication health monitoring';
COMMENT ON FUNCTION record_cdc_event(TEXT, TEXT, UUID, JSONB, JSONB, TEXT[], TEXT, TEXT) IS 'Record a CDC event for streaming';
COMMENT ON FUNCTION get_cdc_events_for_kafka(TEXT, INTEGER) IS 'Get unprocessed CDC events for Kafka publishing';
COMMENT ON FUNCTION append_event(UUID, TEXT, TEXT, JSONB, JSONB, UUID, UUID) IS 'Append event to event store for event sourcing';

-- =====================================================
-- 11. SAMPLE DATA FOR TESTING
-- =====================================================

-- Insert sample CDC event for testing
INSERT INTO cdc_event_log (
    event_type,
    table_name,
    pk_value,
    old_data,
    new_data,
    consumer_group
) VALUES (
    'INSERT',
    'customers',
    gen_random_uuid(),
    NULL,
    '{"email": "test@example.com", "name": "Test Customer"}'::JSONB,
    'analytics-service'
);

RAISE NOTICE 'Logical replication and CDC setup completed successfully';
RAISE NOTICE 'Publication created: bss_publication';
RAISE NOTICE 'Replication slot created: bss_cdc_slot';
RAISE NOTICE 'CDC triggers created on: customers, invoices, payments, orders, subscriptions';
RAISE NOTICE 'Real-time materialized views created: mv_customer_analytics, mv_revenue_analytics';
