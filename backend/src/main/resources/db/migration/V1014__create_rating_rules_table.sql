-- Rating rules for usage rating
CREATE TABLE rating_rules (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usage_type VARCHAR(50) NOT NULL,
    destination_type VARCHAR(50),
    rate_period VARCHAR(50) NOT NULL,
    unit_rate DECIMAL(10, 4) NOT NULL,
    minimum_units BIGINT DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'PLN',
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create indexes for better query performance
CREATE INDEX idx_rating_rules_usage_type ON rating_rules(usage_type);
CREATE INDEX idx_rating_rules_destination_type ON rating_rules(destination_type);
CREATE INDEX idx_rating_rules_rate_period ON rating_rules(rate_period);
CREATE INDEX idx_rating_rules_effective_period ON rating_rules(effective_from, effective_to);
CREATE INDEX idx_rating_rules_active ON rating_rules(is_active);

-- Comments
COMMENT ON TABLE rating_rules IS 'Rating rules for usage-based charges';
COMMENT ON COLUMN rating_rules.usage_type IS 'Type of usage (VOICE, SMS, DATA, VIDEO)';
COMMENT ON COLUMN rating_rules.destination_type IS 'Destination type (NATIONAL, INTERNATIONAL, MOBILE, FIXED)';
COMMENT ON COLUMN rating_rules.rate_period IS 'Rate period (PEAK, OFF_PEAK, WEEKEND)';
COMMENT ON COLUMN rating_rules.unit_rate IS 'Rate per unit of usage';
COMMENT ON COLUMN rating_rules.minimum_units IS 'Minimum billable units';
COMMENT ON COLUMN rating_rules.currency IS 'ISO 4217 currency code';
COMMENT ON COLUMN rating_rules.effective_from IS 'Rule validity start date';
COMMENT ON COLUMN rating_rules.effective_to IS 'Rule validity end date';
