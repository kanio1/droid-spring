-- Orders table
-- Customer order management (new orders, changes, cancellations)

CREATE TYPE order_type AS ENUM ('NEW', 'CHANGE', 'CANCEL', 'SUSPEND', 'RESUME');
CREATE TYPE order_status AS ENUM ('DRAFT', 'PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED');
CREATE TYPE order_priority AS ENUM ('LOW', 'NORMAL', 'HIGH', 'URGENT');

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    order_type order_type NOT NULL,
    status order_status NOT NULL,
    priority order_priority DEFAULT 'NORMAL',
    total_amount DECIMAL(12,2),
    currency VARCHAR(3) DEFAULT 'PLN',
    requested_date DATE,
    promised_date DATE,
    completed_date DATE,
    order_channel VARCHAR(50),
    sales_rep_id VARCHAR(100),
    notes TEXT,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_type ON orders(order_type);
CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_priority ON orders(priority);

-- Update trigger for updated_at column
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
