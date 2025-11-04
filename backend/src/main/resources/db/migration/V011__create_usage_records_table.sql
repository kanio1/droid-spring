-- Usage Records table
-- Service usage tracking (voice, data, SMS, etc.)

CREATE TYPE usage_type AS ENUM ('VOICE', 'SMS', 'DATA', 'VIDEO');
CREATE TYPE usage_unit AS ENUM ('SECONDS', 'COUNT', 'MB', 'GB');
CREATE TYPE destination_type AS ENUM ('NATIONAL', 'INTERNATIONAL', 'MOBILE', 'FIXED', 'SPECIAL');
CREATE TYPE rate_period AS ENUM ('PEAK', 'OFF_PEAK', 'WEEKEND');
CREATE TYPE usage_source AS ENUM ('CDR', 'MANUAL', 'BULK_UPLOAD');

CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    usage_type usage_type NOT NULL,
    usage_unit usage_unit NOT NULL,
    usage_amount DECIMAL(15,3) NOT NULL,
    usage_date DATE NOT NULL,
    usage_time TIME NOT NULL,
    destination_type destination_type,
    destination_number VARCHAR(50),
    destination_country VARCHAR(2),
    network_id VARCHAR(50),
    rate_period rate_period,
    unit_rate DECIMAL(8,4),
    charge_amount DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'PLN',
    tax_rate DECIMAL(5,2) DEFAULT 23.00,
    tax_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    rated BOOLEAN DEFAULT false,
    rating_date TIMESTAMPTZ,
    source usage_source,
    source_file VARCHAR(200),
    processed BOOLEAN DEFAULT false,
    invoice_id UUID REFERENCES invoices(id),
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_usage_records_subscription ON usage_records(subscription_id);
CREATE INDEX idx_usage_records_date ON usage_records(usage_date);
CREATE INDEX idx_usage_records_type ON usage_records(usage_type);
CREATE INDEX idx_usage_records_rated ON usage_records(rated);
CREATE INDEX idx_usage_records_invoice ON usage_records(invoice_id);

-- Update trigger for updated_at column
CREATE TRIGGER update_usage_records_updated_at
    BEFORE UPDATE ON usage_records
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
