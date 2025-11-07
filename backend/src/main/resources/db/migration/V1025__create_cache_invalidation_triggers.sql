-- Cache Invalidation Triggers
-- This migration creates triggers for automatic cache invalidation

-- Create notification tracking table
CREATE TABLE IF NOT EXISTS cache_notifications (
    id BIGSERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,
    record_id TEXT NOT NULL,
    notification_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed BOOLEAN DEFAULT FALSE
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_cache_notifications_unprocessed
    ON cache_notifications(processed, created_at)
    WHERE processed = FALSE;

-- Create function to send cache invalidation notification
CREATE OR REPLACE FUNCTION notify_cache_invalidation()
RETURNS TRIGGER AS $$
DECLARE
    notification_payload JSON;
BEGIN
    -- Build notification payload
    notification_payload = json_build_object(
        'table', TG_TABLE_NAME,
        'operation', TG_OP,
        'id', COALESCE(NEW.id, OLD.id),
        'timestamp', EXTRACT(EPOCH FROM NOW()),
        'schema', TG_TABLE_SCHEMA
    );

    -- Store notification in tracking table
    INSERT INTO cache_notifications (table_name, operation, record_id, notification_data)
    VALUES (
        TG_TABLE_NAME,
        TG_OP,
        COALESCE(NEW.id, OLD.id)::TEXT,
        notification_payload
    );

    -- Send NOTIFY (commented out for initial implementation)
    -- PERFORM pg_notify('cache_invalidation', notification_payload::text);

    -- Log the operation
    IF TG_OP = 'DELETE' THEN
        RAISE NOTICE 'Cache invalidation: Deleted % record (id: %)', TG_TABLE_NAME, OLD.id;
    ELSE
        RAISE NOTICE 'Cache invalidation: % % record (id: %)', TG_OP, TG_TABLE_NAME, NEW.id;
    END IF;

    -- Return appropriate record
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- CUSTOMER TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS customer_cache_invalidation_insert ON customer;
CREATE TRIGGER customer_cache_invalidation_insert
    AFTER INSERT ON customer
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS customer_cache_invalidation_update ON customer;
CREATE TRIGGER customer_cache_invalidation_update
    AFTER UPDATE ON customer
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS customer_cache_invalidation_delete ON customer;
CREATE TRIGGER customer_cache_invalidation_delete
    AFTER DELETE ON customer
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- ========================================
-- ADDRESS TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS address_cache_invalidation_insert ON address;
CREATE TRIGGER address_cache_invalidation_insert
    AFTER INSERT ON address
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS address_cache_invalidation_update ON address;
CREATE TRIGGER address_cache_invalidation_update
    AFTER UPDATE ON address
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS address_cache_invalidation_delete ON address;
CREATE TRIGGER address_cache_invalidation_delete
    AFTER DELETE ON address
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- ========================================
-- ORDER TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS orders_cache_invalidation_insert ON orders;
CREATE TRIGGER orders_cache_invalidation_insert
    AFTER INSERT ON orders
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS orders_cache_invalidation_update ON orders;
CREATE TRIGGER orders_cache_invalidation_update
    AFTER UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS orders_cache_invalidation_delete ON orders;
CREATE TRIGGER orders_cache_invalidation_delete
    AFTER DELETE ON orders
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- ========================================
-- PAYMENT TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS payment_cache_invalidation_insert ON payment;
CREATE TRIGGER payment_cache_invalidation_insert
    AFTER INSERT ON payment
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS payment_cache_invalidation_update ON payment;
CREATE TRIGGER payment_cache_invalidation_update
    AFTER UPDATE ON payment
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS payment_cache_invalidation_delete ON payment;
CREATE TRIGGER payment_cache_invalidation_delete
    AFTER DELETE ON payment
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- ========================================
-- INVOICE TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS invoice_cache_invalidation_insert ON invoice;
CREATE TRIGGER invoice_cache_invalidation_insert
    AFTER INSERT ON invoice
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS invoice_cache_invalidation_update ON invoice;
CREATE TRIGGER invoice_cache_invalidation_update
    AFTER UPDATE ON invoice
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS invoice_cache_invalidation_delete ON invoice;
CREATE TRIGGER invoice_cache_invalidation_delete
    AFTER DELETE ON invoice
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- ========================================
-- SUBSCRIPTION TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS subscription_cache_invalidation_insert ON subscription;
CREATE TRIGGER subscription_cache_invalidation_insert
    AFTER INSERT ON subscription
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS subscription_cache_invalidation_update ON subscription;
CREATE TRIGGER subscription_cache_invalidation_update
    AFTER UPDATE ON subscription
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS subscription_cache_invalidation_delete ON subscription;
CREATE TRIGGER subscription_cache_invalidation_delete
    AFTER DELETE ON subscription
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- ========================================
-- PRODUCT TABLE TRIGGERS
-- ========================================

DROP TRIGGER IF EXISTS product_cache_invalidation_insert ON product;
CREATE TRIGGER product_cache_invalidation_insert
    AFTER INSERT ON product
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS product_cache_invalidation_update ON product;
CREATE TRIGGER product_cache_invalidation_update
    AFTER UPDATE ON product
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

DROP TRIGGER IF EXISTS product_cache_invalidation_delete ON product;
CREATE TRIGGER product_cache_invalidation_delete
    AFTER DELETE ON product
    FOR EACH ROW EXECUTE FUNCTION notify_cache_invalidation();

-- Grant permissions (adjust as needed)
GRANT SELECT, INSERT, UPDATE, DELETE ON cache_notifications TO bss_app;
GRANT USAGE ON SCHEMA public TO bss_app;
