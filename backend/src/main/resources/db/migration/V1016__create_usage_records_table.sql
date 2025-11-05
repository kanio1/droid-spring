-- Usage records (CDRs - Call Detail Records)
CREATE TABLE usage_records (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subscription_id VARCHAR(36) NOT NULL,
    usage_type VARCHAR(50) NOT NULL,
    usage_unit VARCHAR(50) NOT NULL,
    usage_amount DECIMAL(10, 4) NOT NULL,
    usage_date DATE NOT NULL,
    usage_time TIME NOT NULL,
    destination_type VARCHAR(50),
    destination_number VARCHAR(50),
    destination_country VARCHAR(3),
    network_id VARCHAR(100),
    rate_period VARCHAR(50),
    source VARCHAR(50),
    source_file VARCHAR(255),
    unit_rate DECIMAL(10, 4),
    charge_amount DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'PLN',
    total_amount DECIMAL(10, 2),
    tax_amount DECIMAL(10, 2),
    total_with_tax DECIMAL(10, 2),
    is_rated BOOLEAN NOT NULL DEFAULT FALSE,
    rating_date DATE
);

-- Create indexes for query performance
CREATE INDEX idx_usage_records_subscription ON usage_records(subscription_id);
CREATE INDEX idx_usage_records_usage_type ON usage_records(usage_type);
CREATE INDEX idx_usage_records_usage_date ON usage_records(usage_date);
CREATE INDEX idx_usage_records_destination ON usage_records(destination_type, destination_number);
CREATE INDEX idx_usage_records_rated ON usage_records(is_rated);
CREATE INDEX idx_usage_records_network ON usage_records(network_id);

-- Foreign key
ALTER TABLE usage_records
    ADD CONSTRAINT fk_usage_records_subscription
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id);

-- Comments
COMMENT ON TABLE usage_records IS 'Usage records (CDRs) for billing';
COMMENT ON COLUMN usage_records.usage_type IS 'Type (VOICE, SMS, DATA, VIDEO)';
COMMENT ON COLUMN usage_records.usage_unit IS 'Unit (MINUTES, SECONDS, MB, GB, COUNT)';
COMMENT ON COLUMN usage_records.usage_amount IS 'Amount of usage';
COMMENT ON COLUMN usage_records.usage_date IS 'Date of usage';
COMMENT ON COLUMN usage_records.usage_time IS 'Time of usage';
COMMENT ON COLUMN usage_records.destination_type IS 'Destination type (NATIONAL, INTERNATIONAL, MOBILE, FIXED)';
COMMENT ON COLUMN usage_records.destination_number IS 'Destination phone number or address';
COMMENT ON COLUMN usage_records.destination_country IS 'ISO 3166-1 alpha-3 country code';
COMMENT ON COLUMN usage_records.network_id IS 'Network element ID where usage occurred';
COMMENT ON COLUMN usage_records.rate_period IS 'Rate period (PEAK, OFF_PEAK, WEEKEND)';
COMMENT ON COLUMN usage_records.source IS 'Source of CDR (SYSTEM, FILE, API)';
COMMENT ON COLUMN usage_records.source_file IS 'Source file name if imported';
COMMENT ON COLUMN usage_records.is_rated IS 'Whether usage has been rated';
