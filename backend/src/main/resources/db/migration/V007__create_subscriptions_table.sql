-- Subscriptions table
-- Active customer subscriptions/services

CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED');

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    product_id UUID NOT NULL REFERENCES products(id),
    order_id UUID REFERENCES orders(id),
    status subscription_status NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    billing_start DATE NOT NULL,
    next_billing_date DATE,
    billing_period VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    discount_amount DECIMAL(10,2) DEFAULT 0,
    net_amount DECIMAL(10,2),
    configuration JSONB,
    auto_renew BOOLEAN DEFAULT true,
    renewal_notice_sent BOOLEAN DEFAULT false,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_subscriptions_customer ON subscriptions(customer_id);
CREATE INDEX idx_subscriptions_product ON subscriptions(product_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_billing ON subscriptions(next_billing_date);
CREATE INDEX idx_subscriptions_number ON subscriptions(subscription_number);

-- Update trigger for updated_at column
CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
