-- Migration to convert ENUM types directly to SMALLINT for @Enumerated(EnumType.ORDINAL)
-- This conversion skips the VARCHAR intermediate step

-- Convert ENUM columns to SMALLINT
-- Order Status
ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE orders ALTER COLUMN status TYPE SMALLINT USING status::smallint;
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 0;

-- Order Type and Priority
ALTER TABLE orders ALTER COLUMN order_type DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN order_type TYPE VARCHAR(50) USING order_type::text;
ALTER TABLE orders ALTER COLUMN order_type TYPE SMALLINT USING order_type::smallint;
ALTER TABLE orders ALTER COLUMN order_type SET DEFAULT 0;
ALTER TABLE orders ALTER COLUMN priority DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN priority TYPE VARCHAR(50) USING priority::text;
ALTER TABLE orders ALTER COLUMN priority TYPE SMALLINT USING priority::smallint;
ALTER TABLE orders ALTER COLUMN priority SET DEFAULT 0;

-- Payment Status (payment_status column) and Payment Method
ALTER TABLE payments ALTER COLUMN payment_status DROP DEFAULT;
ALTER TABLE payments ALTER COLUMN payment_status TYPE VARCHAR(50) USING payment_status::text;
ALTER TABLE payments ALTER COLUMN payment_status TYPE SMALLINT USING payment_status::smallint;
ALTER TABLE payments ALTER COLUMN payment_status SET DEFAULT 0;
ALTER TABLE payments ALTER COLUMN payment_method DROP DEFAULT;
ALTER TABLE payments ALTER COLUMN payment_method TYPE VARCHAR(50) USING payment_method::text;
ALTER TABLE payments ALTER COLUMN payment_method TYPE SMALLINT USING payment_method::smallint;
ALTER TABLE payments ALTER COLUMN payment_method SET DEFAULT 0;

-- Invoice Status and Type
ALTER TABLE invoices ALTER COLUMN status DROP DEFAULT;
ALTER TABLE invoices ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE invoices ALTER COLUMN status TYPE SMALLINT USING status::smallint;
ALTER TABLE invoices ALTER COLUMN status SET DEFAULT 0;
ALTER TABLE invoices ALTER COLUMN invoice_type DROP DEFAULT;
ALTER TABLE invoices ALTER COLUMN invoice_type TYPE VARCHAR(50) USING invoice_type::text;
ALTER TABLE invoices ALTER COLUMN invoice_type TYPE SMALLINT USING invoice_type::smallint;
ALTER TABLE invoices ALTER COLUMN invoice_type SET DEFAULT 0;

-- Product Status, Category, and Type
ALTER TABLE products ALTER COLUMN status DROP DEFAULT;
ALTER TABLE products ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE products ALTER COLUMN status TYPE SMALLINT USING status::smallint;
ALTER TABLE products ALTER COLUMN status SET DEFAULT 0;
ALTER TABLE products ALTER COLUMN category DROP DEFAULT;
ALTER TABLE products ALTER COLUMN category TYPE VARCHAR(50) USING category::text;
ALTER TABLE products ALTER COLUMN category TYPE SMALLINT USING category::smallint;
ALTER TABLE products ALTER COLUMN category SET DEFAULT 0;
ALTER TABLE products ALTER COLUMN product_type DROP DEFAULT;
ALTER TABLE products ALTER COLUMN product_type TYPE VARCHAR(50) USING product_type::text;
ALTER TABLE products ALTER COLUMN product_type TYPE SMALLINT USING product_type::smallint;
ALTER TABLE products ALTER COLUMN product_type SET DEFAULT 0;

-- Subscription Status
ALTER TABLE subscriptions ALTER COLUMN status DROP DEFAULT;
ALTER TABLE subscriptions ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE subscriptions ALTER COLUMN status TYPE SMALLINT USING status::smallint;
ALTER TABLE subscriptions ALTER COLUMN status SET DEFAULT 0;

-- Order Item Status and Type
ALTER TABLE order_items ALTER COLUMN status DROP DEFAULT;
ALTER TABLE order_items ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE order_items ALTER COLUMN status TYPE SMALLINT USING status::smallint;
ALTER TABLE order_items ALTER COLUMN status SET DEFAULT 0;
ALTER TABLE order_items ALTER COLUMN item_type DROP DEFAULT;
ALTER TABLE order_items ALTER COLUMN item_type TYPE VARCHAR(50) USING item_type::text;
ALTER TABLE order_items ALTER COLUMN item_type TYPE SMALLINT USING item_type::smallint;
ALTER TABLE order_items ALTER COLUMN item_type SET DEFAULT 0;

-- Invoice Item Type
ALTER TABLE invoice_items ALTER COLUMN item_type DROP DEFAULT;
ALTER TABLE invoice_items ALTER COLUMN item_type TYPE VARCHAR(50) USING item_type::text;
ALTER TABLE invoice_items ALTER COLUMN item_type TYPE SMALLINT USING item_type::smallint;
ALTER TABLE invoice_items ALTER COLUMN item_type SET DEFAULT 0;

-- Product Feature Data Type (data_type column)
ALTER TABLE product_features ALTER COLUMN data_type DROP DEFAULT;
ALTER TABLE product_features ALTER COLUMN data_type TYPE VARCHAR(50) USING data_type::text;
ALTER TABLE product_features ALTER COLUMN data_type TYPE SMALLINT USING data_type::smallint;
ALTER TABLE product_features ALTER COLUMN data_type SET DEFAULT 0;

-- Customer Status
ALTER TABLE customers ALTER COLUMN status DROP DEFAULT;
ALTER TABLE customers ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE customers ALTER COLUMN status TYPE SMALLINT USING status::smallint;
ALTER TABLE customers ALTER COLUMN status SET DEFAULT 0;

-- Drop ENUM types (safe to do after converting columns)
DROP TYPE IF EXISTS order_status CASCADE;
DROP TYPE IF EXISTS order_type CASCADE;
DROP TYPE IF EXISTS order_priority CASCADE;
DROP TYPE IF EXISTS payment_status CASCADE;
DROP TYPE IF EXISTS payment_method CASCADE;
DROP TYPE IF EXISTS invoice_status CASCADE;
DROP TYPE IF EXISTS invoice_type CASCADE;
DROP TYPE IF EXISTS product_status CASCADE;
DROP TYPE IF EXISTS product_category CASCADE;
DROP TYPE IF EXISTS product_type CASCADE;
DROP TYPE IF EXISTS subscription_status CASCADE;
DROP TYPE IF EXISTS order_item_status CASCADE;
DROP TYPE IF EXISTS order_item_type CASCADE;
DROP TYPE IF EXISTS invoice_item_type CASCADE;
DROP TYPE IF EXISTS feature_data_type CASCADE;
DROP TYPE IF EXISTS customer_status CASCADE;

-- Add comments for documentation
COMMENT ON COLUMN orders.status IS 'Order status ordinal: 0=PENDING, 1=CONFIRMED, 2=PROCESSING, 3=SHIPPED, 4=DELIVERED, 5=CANCELLED';
COMMENT ON COLUMN orders.order_type IS 'Order type ordinal: 0=PURCHASE, 1=UPGRADE, 2=CANCELLATION, 3=MODIFICATION';
COMMENT ON COLUMN orders.priority IS 'Order priority ordinal: 0=NORMAL, 1=HIGH, 2=URGENT';
COMMENT ON COLUMN payments.payment_status IS 'Payment status ordinal: 0=PENDING, 1=PROCESSING, 2=COMPLETED, 3=FAILED, 4=REFUNDED';
COMMENT ON COLUMN payments.payment_method IS 'Payment method ordinal: 0=CARD, 1=BANK_TRANSFER, 2=CASH, 3=DIRECT_DEBIT, 4=MOBILE_PAY';
COMMENT ON COLUMN invoices.status IS 'Invoice status ordinal: 0=DRAFT, 1=ISSUED, 2=SENT, 3=PAID, 4=OVERDUE, 5=CANCELLED';
COMMENT ON COLUMN invoices.invoice_type IS 'Invoice type ordinal: 0=RECURRING, 1=USAGE, 2=ONE_TIME, 3=ADJUSTMENT';
COMMENT ON COLUMN products.status IS 'Product status ordinal: 0=ACTIVE, 1=INACTIVE, 2=DISCONTINUED';
COMMENT ON COLUMN products.category IS 'Product category ordinal: 0=STANDARD, 1=PREMIUM, 2=ADDON';
COMMENT ON COLUMN products.product_type IS 'Product type ordinal: 0=SERVICE, 1=TARIFF, 2=BUNDLE, 3=ADDON';
COMMENT ON COLUMN subscriptions.status IS 'Subscription status ordinal: 0=ACTIVE, 1=SUSPENDED, 2=CANCELLED, 3=EXPIRED';
COMMENT ON COLUMN order_items.status IS 'Order item status ordinal: 0=PENDING, 1=CONFIRMED, 2=SHIPPED, 3=DELIVERED, 4=CANCELLED';
COMMENT ON COLUMN order_items.item_type IS 'Order item type ordinal: 0=PRODUCT, 1=SERVICE, 2=BUNDLE';
COMMENT ON COLUMN invoice_items.item_type IS 'Invoice item type ordinal: 0=SERVICE, 1=PRODUCT, 2=DISCOUNT, 3=TAX, 4=ADJUSTMENT, 5=USAGE';
COMMENT ON COLUMN product_features.data_type IS 'Feature data type ordinal: 0=STRING, 1=NUMBER, 2=BOOLEAN, 3=JSON';
COMMENT ON COLUMN customers.status IS 'Customer status ordinal: 0=ACTIVE, 1=INACTIVE, 2=SUSPENDED, 3=PENDING_VERIFICATION';
