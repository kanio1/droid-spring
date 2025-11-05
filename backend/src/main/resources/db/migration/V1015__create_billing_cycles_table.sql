-- Billing cycles for customer billing periods
CREATE TABLE billing_cycles (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    customer_id VARCHAR(36) NOT NULL,
    cycle_start DATE NOT NULL,
    cycle_end DATE NOT NULL,
    billing_date DATE NOT NULL,
    billing_cycle_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12, 2) DEFAULT 0,
    tax_amount DECIMAL(12, 2) DEFAULT 0,
    total_with_tax DECIMAL(12, 2) DEFAULT 0,
    invoice_count INTEGER DEFAULT 0,
    generated_at TIMESTAMP,
    processed_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_billing_cycles_customer_id ON billing_cycles(customer_id);
CREATE INDEX idx_billing_cycles_status ON billing_cycles(status);
CREATE INDEX idx_billing_cycles_billing_date ON billing_cycles(billing_date);
CREATE INDEX idx_billing_cycles_cycle_period ON billing_cycles(cycle_start, cycle_end);
CREATE INDEX idx_billing_cycles_deleted_at ON billing_cycles(deleted_at);

-- Foreign key
ALTER TABLE billing_cycles
    ADD CONSTRAINT fk_billing_cycles_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);

-- Comments
COMMENT ON TABLE billing_cycles IS 'Billing cycles for customer billing periods';
COMMENT ON COLUMN billing_cycles.cycle_start IS 'Billing period start date';
COMMENT ON COLUMN billing_cycles.cycle_end IS 'Billing period end date';
COMMENT ON COLUMN billing_cycles.billing_date IS 'Invoice billing date';
COMMENT ON COLUMN billing_cycles.billing_cycle_type IS 'Cycle type (MONTHLY, QUARTERLY, YEARLY)';
COMMENT ON COLUMN billing_cycles.status IS 'Cycle status (PENDING, GENERATED, PROCESSED, CANCELLED)';
COMMENT ON COLUMN billing_cycles.total_amount IS 'Total amount before tax';
COMMENT ON COLUMN billing_cycles.tax_amount IS 'Tax amount (VAT)';
COMMENT ON COLUMN billing_cycles.total_with_tax IS 'Total amount including tax';
