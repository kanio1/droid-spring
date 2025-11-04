-- Order Items table
-- Individual items within an order

CREATE TYPE order_item_type AS ENUM ('PRODUCT', 'SERVICE', 'DEVICE', 'DISCOUNT', 'CHARGE');
CREATE TYPE order_item_status AS ENUM ('PENDING', 'ACTIVE', 'FAILED', 'SKIPPED');

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    item_type order_item_type NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2),
    total_price DECIMAL(12,2),
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_rate DECIMAL(5,2) DEFAULT 23.00,
    tax_amount DECIMAL(10,2),
    net_amount DECIMAL(12,2),
    status order_item_status DEFAULT 'PENDING',
    activation_date DATE,
    expiry_date DATE,
    configuration JSONB,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_status ON order_items(status);
CREATE INDEX idx_order_items_type ON order_items(item_type);

-- Update trigger for updated_at column
CREATE TRIGGER update_order_items_updated_at
    BEFORE UPDATE ON order_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
