-- Customer Insights Tables
-- Stores AI-generated customer insights and recommendations

-- Main customer_insights table
CREATE TABLE customer_insights (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    insight_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    confidence_score DECIMAL(5, 4),
    priority INTEGER DEFAULT 0,
    category VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP,
    model_version VARCHAR(50),
    model_name VARCHAR(100),
    model_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    viewed_at TIMESTAMP,
    dismissed_at TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_customer_insights_customer_id ON customer_insights(customer_id);
CREATE INDEX idx_customer_insights_tenant_id ON customer_insights(tenant_id);
CREATE INDEX idx_customer_insights_type ON customer_insights(insight_type);
CREATE INDEX idx_customer_insights_status ON customer_insights(status);
CREATE INDEX idx_customer_insights_priority ON customer_insights(priority);
CREATE INDEX idx_customer_insights_confidence ON customer_insights(confidence_score);
CREATE INDEX idx_customer_insights_created_at ON customer_insights(created_at);
CREATE INDEX idx_customer_insights_expires_at ON customer_insights(expires_at);

-- Composite indexes for common queries
CREATE INDEX idx_customer_insights_tenant_type ON customer_insights(tenant_id, insight_type);
CREATE INDEX idx_customer_insights_customer_active ON customer_insights(customer_id, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_customer_insights_tenant_created ON customer_insights(tenant_id, created_at);

-- Insight data (key-value pairs) for additional data
CREATE TABLE insight_data (
    insight_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    data_key VARCHAR(255) NOT NULL,
    data_value VARCHAR(500),
    PRIMARY KEY (insight_id, data_key, tenant_id),
    FOREIGN KEY (insight_id) REFERENCES customer_insights(id) ON DELETE CASCADE
);

CREATE INDEX idx_insight_data_insight_id ON insight_data(insight_id);
CREATE INDEX idx_insight_data_tenant_id ON insight_data(tenant_id);

-- Insight metrics (structured metrics)
CREATE TABLE insight_metrics (
    insight_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_value VARCHAR(255) NOT NULL,
    metric_unit VARCHAR(50),
    metric_change VARCHAR(50),
    PRIMARY KEY (insight_id, metric_name, tenant_id),
    FOREIGN KEY (insight_id) REFERENCES customer_insights(id) ON DELETE CASCADE
);

CREATE INDEX idx_insight_metrics_insight_id ON insight_metrics(insight_id);
CREATE INDEX idx_insight_metrics_tenant_id ON insight_metrics(tenant_id);

-- Enable Row Level Security (RLS)
ALTER TABLE customer_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE insight_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE insight_metrics ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for customer_insights
CREATE POLICY tenant_isolation_customer_insights ON customer_insights
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Create RLS policies for insight_data
CREATE POLICY tenant_isolation_insight_data ON insight_data
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Create RLS policies for insight_metrics
CREATE POLICY tenant_isolation_insight_metrics ON insight_metrics
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_customer_insights_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    -- Note: created_at is not updated after insert
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON TABLE customer_insights IS 'AI-generated customer insights with confidence scores and recommendations';
COMMENT ON COLUMN customer_insights.id IS 'Unique insight identifier';
COMMENT ON COLUMN customer_insights.customer_id IS 'Reference to customer';
COMMENT ON COLUMN customer_insights.tenant_id IS 'Tenant for multi-tenant isolation';
COMMENT ON COLUMN customer_insights.insight_type IS 'Type of insight (CHURN_RISK, LIFETIME_VALUE, etc.)';
COMMENT ON COLUMN customer_insights.confidence_score IS 'AI model confidence (0.0 to 1.0)';
COMMENT ON COLUMN customer_insights.priority IS 'Business priority (0-10, higher is more important)';
COMMENT ON COLUMN customer_insights.status IS 'Insight status (ACTIVE, VIEWED, DISMISSED, EXPIRED)';
COMMENT ON COLUMN customer_insights.expires_at IS 'When insight becomes stale';
COMMENT ON COLUMN customer_insights.model_name IS 'Name of AI model that generated this insight';
COMMENT ON COLUMN customer_insights.model_version IS 'Version of AI model';

COMMENT ON TABLE insight_data IS 'Key-value data associated with insights';
COMMENT ON TABLE insight_metrics IS 'Structured metrics for insights';
