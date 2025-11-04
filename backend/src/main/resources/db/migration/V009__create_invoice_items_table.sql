-- Invoice Items table
-- Line items within invoices

CREATE TYPE invoice_item_type AS ENUM ('SUBSCRIPTION', 'USAGE', 'DISCOUNT', 'ADJUSTMENT', 'TAX');

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id),
    usage_record_id VARCHAR(255),
    item_type invoice_item_type NOT NULL,
    description TEXT NOT NULL,
    quantity DECIMAL(10,3) NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    unit VARCHAR(20),
    discount_rate DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_rate DECIMAL(5,2) DEFAULT 23.00,
    tax_amount DECIMAL(10,2),
    net_amount DECIMAL(12,2),
    total_amount DECIMAL(12,2) NOT NULL,
    period_start DATE,
    period_end DATE,
    configuration JSONB,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_subscription ON invoice_items(subscription_id);
CREATE INDEX idx_invoice_items_type ON invoice_items(item_type);

-- Update trigger for updated_at column
CREATE TRIGGER update_invoice_items_updated_at
    BEFORE UPDATE ON invoice_items
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
