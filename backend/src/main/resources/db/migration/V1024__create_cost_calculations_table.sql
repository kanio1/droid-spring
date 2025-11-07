-- Create cost_calculations table
CREATE TABLE cost_calculations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    billing_period VARCHAR(50) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    total_usage DECIMAL(19,4) NOT NULL,
    base_cost DECIMAL(19,4) NOT NULL,
    overage_cost DECIMAL(19,4) NOT NULL,
    total_cost DECIMAL(19,4) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    cost_model_id BIGINT,
    status VARCHAR(50) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT chk_cost_calculations_billing_period
        CHECK (billing_period IN ('hourly', 'daily', 'monthly', 'yearly')),

    CONSTRAINT chk_cost_calculations_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_cost_calculations_status
        CHECK (status IN ('DRAFT', 'FINAL', 'INVOICED')),

    CONSTRAINT chk_cost_calculations_values_positive
        CHECK (total_usage >= 0 AND base_cost >= 0 AND overage_cost >= 0 AND total_cost >= 0),

    CONSTRAINT fk_cost_calculations_cost_model
        FOREIGN KEY (cost_model_id) REFERENCES cost_models(id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX idx_cost_calculations_customer ON cost_calculations(customer_id);
CREATE INDEX idx_cost_calculations_resource_type ON cost_calculations(resource_type);
CREATE INDEX idx_cost_calculations_period ON cost_calculations(period_start, period_end);
CREATE INDEX idx_cost_calculations_status ON cost_calculations(status);
CREATE INDEX idx_cost_calculations_cost_model ON cost_calculations(cost_model_id);

-- Create trigger to update updated_at
CREATE TRIGGER update_cost_calculations_updated_at
    BEFORE UPDATE ON cost_calculations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE cost_calculations IS 'Calculated costs for resource usage over specific periods';
COMMENT ON COLUMN cost_calculations.customer_id IS 'Customer identifier';
COMMENT ON COLUMN cost_calculations.resource_type IS 'Type of resource (CPU, memory, storage, etc.)';
COMMENT ON COLUMN cost_calculations.billing_period IS 'Billing period type';
COMMENT ON COLUMN cost_calculations.period_start IS 'Start of the billing period';
COMMENT ON COLUMN cost_calculations.period_end IS 'End of the billing period';
COMMENT ON COLUMN cost_calculations.total_usage IS 'Total resource usage during the period';
COMMENT ON COLUMN cost_calculations.base_cost IS 'Base cost for included usage';
COMMENT ON COLUMN cost_calculations.overage_cost IS 'Cost for usage exceeding included amount';
COMMENT ON COLUMN cost_calculations.total_cost IS 'Total cost (base + overage)';
COMMENT ON COLUMN cost_calculations.currency IS 'Currency code (e.g., USD, EUR)';
COMMENT ON COLUMN cost_calculations.cost_model_id IS 'Reference to cost model used for calculation';
COMMENT ON COLUMN cost_calculations.status IS 'Calculation status';
COMMENT ON COLUMN cost_calculations.calculated_at IS 'When the calculation was performed';

-- Sample data
INSERT INTO cost_calculations (
    customer_id, resource_type, billing_period, period_start, period_end,
    total_usage, base_cost, overage_cost, total_cost, currency, status, calculated_at
) VALUES
(1, 'CPU', 'monthly', '2024-01-01 00:00:00+00', '2024-01-31 23:59:59+00',
 100.0000, 50.0000, 25.0000, 75.0000, 'USD', 'FINAL', NOW()),
(1, 'storage', 'monthly', '2024-01-01 00:00:00+00', '2024-01-31 23:59:59+00',
 500.0000, 30.0000, 0.0000, 30.0000, 'USD', 'FINAL', NOW());
