-- Migration to fix all version columns from INTEGER to BIGINT
-- This aligns with BaseEntity.version field which is of type Long
--
-- Issue: BaseEntity defines version as Long (64-bit) but database schemas used INTEGER (32-bit)
-- Fix: Alter all version columns to BIGINT to match the Java Long type
--
-- Affected tables: products, product_features, orders, order_items, subscriptions,
--                   invoices, invoice_items, payments, network_elements
-- Note: customers table was already fixed in V999

-- Products table
ALTER TABLE products ALTER COLUMN version TYPE BIGINT;
ALTER TABLE products ALTER COLUMN version SET DEFAULT 0;

-- Product features table
ALTER TABLE product_features ALTER COLUMN version TYPE BIGINT;
ALTER TABLE product_features ALTER COLUMN version SET DEFAULT 0;

-- Orders table
ALTER TABLE orders ALTER COLUMN version TYPE BIGINT;
ALTER TABLE orders ALTER COLUMN version SET DEFAULT 0;

-- Order items table
ALTER TABLE order_items ALTER COLUMN version TYPE BIGINT;
ALTER TABLE order_items ALTER COLUMN version SET DEFAULT 0;

-- Subscriptions table
ALTER TABLE subscriptions ALTER COLUMN version TYPE BIGINT;
ALTER TABLE subscriptions ALTER COLUMN version SET DEFAULT 0;

-- Invoices table
ALTER TABLE invoices ALTER COLUMN version TYPE BIGINT;
ALTER TABLE invoices ALTER COLUMN version SET DEFAULT 0;

-- Invoice items table
ALTER TABLE invoice_items ALTER COLUMN version TYPE BIGINT;
ALTER TABLE invoice_items ALTER COLUMN version SET DEFAULT 0;

-- Payments table
ALTER TABLE payments ALTER COLUMN version TYPE BIGINT;
ALTER TABLE payments ALTER COLUMN version SET DEFAULT 0;

-- Network elements table
ALTER TABLE network_elements ALTER COLUMN version TYPE BIGINT;
ALTER TABLE network_elements ALTER COLUMN version SET DEFAULT 0;

-- Add comments for documentation
COMMENT ON COLUMN products.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN product_features.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN orders.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN order_items.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN subscriptions.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN invoices.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN invoice_items.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN payments.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
COMMENT ON COLUMN network_elements.version IS 'Optimistic locking version (from BaseEntity) - BIGINT to match Long type';
