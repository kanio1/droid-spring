-- ============================================
-- PostgreSQL Row Level Security (RLS) Setup
-- ============================================
-- Purpose: Implement tenant isolation at database level
-- Migration: V1025__enable_row_level_security.sql
-- Created: 2025-11-07
-- ============================================

-- Enable RLS on customer-specific tables
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- ============================================
-- RLS Policies
-- ============================================

-- Customers table policies
CREATE POLICY customer_isolation_select ON customers
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY customer_isolation_insert ON customers
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY customer_isolation_update ON customers
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY customer_isolation_delete ON customers
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Addresses table policies
CREATE POLICY address_isolation_select ON addresses
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY address_isolation_insert ON addresses
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY address_isolation_update ON addresses
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY address_isolation_delete ON addresses
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Orders table policies
CREATE POLICY order_isolation_select ON orders
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY order_isolation_insert ON orders
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY order_isolation_update ON orders
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY order_isolation_delete ON orders
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Order Items table policies
CREATE POLICY order_item_isolation_select ON order_items
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY order_item_isolation_insert ON order_items
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY order_item_isolation_update ON order_items
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY order_item_isolation_delete ON order_items
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Invoices table policies
CREATE POLICY invoice_isolation_select ON invoices
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY invoice_isolation_insert ON invoices
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY invoice_isolation_update ON invoices
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY invoice_isolation_delete ON invoices
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Invoice Items table policies
CREATE POLICY invoice_item_isolation_select ON invoice_items
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY invoice_item_isolation_insert ON invoice_items
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY invoice_item_isolation_update ON invoice_items
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY invoice_item_isolation_delete ON invoice_items
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Payments table policies
CREATE POLICY payment_isolation_select ON payments
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY payment_isolation_insert ON payments
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY payment_isolation_update ON payments
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY payment_isolation_delete ON payments
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Subscriptions table policies
CREATE POLICY subscription_isolation_select ON subscriptions
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY subscription_isolation_insert ON subscriptions
    FOR INSERT
    TO application_role
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY subscription_isolation_update ON subscriptions
    FOR UPDATE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id')::uuid);

CREATE POLICY subscription_isolation_delete ON subscriptions
    FOR DELETE
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);

-- Products table policies (read-only for all tenants)
CREATE POLICY product_isolation_select ON products
    FOR SELECT
    TO application_role
    USING (true);

CREATE POLICY product_isolation_insert ON products
    FOR INSERT
    TO application_role
    WITH CHECK (false);

CREATE POLICY product_isolation_update ON products
    FOR UPDATE
    TO application_role
    USING (false);

CREATE POLICY product_isolation_delete ON products
    FOR DELETE
    TO application_role
    USING (false);

-- ============================================
-- Utility Functions
-- ============================================

-- Function to set tenant context
CREATE OR REPLACE FUNCTION set_tenant_context(tenant_uuid uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    PERFORM set_config('app.current_tenant_id', tenant_uuid::text, true);
END;
$$;

-- Function to clear tenant context
CREATE OR REPLACE FUNCTION clear_tenant_context()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    PERFORM set_config('app.current_tenant_id', '', true);
END;
$$;

-- Function to get current tenant ID
CREATE OR REPLACE FUNCTION get_current_tenant_id()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
    SELECT current_setting('app.current_tenant_id', true)::uuid;
$$;

-- ============================================
-- Application Role Setup
-- ============================================

-- Create application role (used by Spring Boot)
CREATE ROLE application_role;
GRANT application_role TO bss_app;

-- Grant necessary permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO application_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO application_role;

-- Grant execute on utility functions
GRANT EXECUTE ON FUNCTION set_tenant_context(uuid) TO application_role;
GRANT EXECUTE ON FUNCTION clear_tenant_context() TO application_role;
GRANT EXECUTE ON FUNCTION get_current_tenant_id() TO application_role;

-- ============================================
-- Admin Bypass (for administrative operations)
-- ============================================

-- Create admin role that can bypass RLS
CREATE ROLE admin_role;
GRANT admin_role TO bss_admin;

-- Grant full access to admin role (bypasses RLS)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin_role;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO admin_role;

-- ============================================
-- Indexes for Performance
-- ============================================

-- Create indexes on tenant_id columns
CREATE INDEX IF NOT EXISTS idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_addresses_tenant_id ON addresses(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_order_items_tenant_id ON order_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_id ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_tenant_id ON invoice_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant_id ON subscriptions(tenant_id);
