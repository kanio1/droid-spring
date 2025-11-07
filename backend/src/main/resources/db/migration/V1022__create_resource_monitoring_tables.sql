-- Resource Usage Monitoring & Cost Optimization
-- Supports real-time monitoring, threshold alerts, and cost optimization
-- Phase 1: Foundation

-- 1. Resource Catalogs
-- Define all trackable resources (bandwidth, storage, API calls, etc.)
CREATE TABLE resource_catalogs (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Resource identification
    tenant_id VARCHAR(36) NOT NULL DEFAULT 'default-tenant',
    resource_type VARCHAR(50) NOT NULL, -- bandwidth, storage, api_calls, cpu, memory
    resource_name VARCHAR(100) NOT NULL, -- "Internet 100Mbps", "Cloud Storage 1TB"
    unit VARCHAR(20) NOT NULL, -- GB, MB, hours, requests
    cost_per_unit DECIMAL(10,4), -- Cost per unit of measurement
    is_billable BOOLEAN NOT NULL DEFAULT TRUE, -- Whether this resource is billable

    -- Resource metadata
    description TEXT,
    category VARCHAR(50), -- network, compute, storage, service

    -- Constraints
    CONSTRAINT chk_resource_catalogs_type
        CHECK (resource_type IN ('bandwidth', 'storage', 'api_calls', 'cpu', 'memory', 'disk_io', 'network_io')),

    CONSTRAINT chk_resource_catalogs_cost_positive
        CHECK (cost_per_unit IS NULL OR cost_per_unit >= 0)
);

-- 2. Customer Resources
-- Track resources assigned to customers
CREATE TABLE customer_resources (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Resource association
    customer_id VARCHAR(36) NOT NULL,
    resource_catalog_id VARCHAR(36) NOT NULL,
    subscription_id VARCHAR(36), -- Optional link to subscription

    -- Usage tracking
    current_usage DECIMAL(15,4) NOT NULL DEFAULT 0,
    limit_value DECIMAL(15,4), -- NULL = unlimited

    -- Alert thresholds
    warning_threshold DECIMAL(5,2) NOT NULL DEFAULT 80, -- % of limit
    critical_threshold DECIMAL(5,2) NOT NULL DEFAULT 95, -- % of limit

    -- Resource status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, INACTIVE
    activated_at TIMESTAMP,
    suspended_at TIMESTAMP,
    deactivated_at TIMESTAMP,

    -- Metadata
    notes TEXT,

    -- Constraints
    CONSTRAINT fk_customer_resources_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_customer_resources_catalog
        FOREIGN KEY (resource_catalog_id) REFERENCES resource_catalogs(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_customer_resources_thresholds
        CHECK (warning_threshold <= critical_threshold),

    CONSTRAINT chk_customer_resources_limit_positive
        CHECK (limit_value IS NULL OR limit_value > 0),

    CONSTRAINT chk_customer_resources_usage_positive
        CHECK (current_usage >= 0)
);

-- 3. Usage Metrics
-- Raw usage data for real-time tracking
CREATE TABLE usage_metrics (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Reference to customer resource
    customer_resource_id VARCHAR(36) NOT NULL,

    -- Metric data
    metric_timestamp TIMESTAMP NOT NULL,
    metric_value DECIMAL(15,4) NOT NULL,
    metric_unit VARCHAR(20) NOT NULL,

    -- Source and context
    source VARCHAR(50) NOT NULL, -- "snmp", "api_gateway", "application", "db", "custom"
    metadata JSONB, -- Additional context (device_id, region, etc.)

    -- Constraints
    CONSTRAINT fk_usage_metrics_customer_resource
        FOREIGN KEY (customer_resource_id) REFERENCES customer_resources(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_usage_metrics_value_positive
        CHECK (metric_value >= 0)
);

-- 4. Usage Aggregates
-- Hourly/daily/monthly aggregated usage data
CREATE TABLE usage_aggregates (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Reference to customer resource
    customer_resource_id VARCHAR(36) NOT NULL,

    -- Aggregate period
    period_type VARCHAR(10) NOT NULL, -- hour, day, week, month
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,

    -- Aggregate values
    usage_count INTEGER NOT NULL DEFAULT 0, -- Number of data points
    avg_usage DECIMAL(15,4),
    min_usage DECIMAL(15,4),
    max_usage DECIMAL(15,4),
    total_usage DECIMAL(15,4) NOT NULL DEFAULT 0,
    cost DECIMAL(10,2), -- Calculated cost for this period

    -- Constraints
    CONSTRAINT fk_usage_aggregates_customer_resource
        FOREIGN KEY (customer_resource_id) REFERENCES customer_resources(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_usage_aggregates_period_type
        CHECK (period_type IN ('hour', 'day', 'week', 'month')),

    CONSTRAINT chk_usage_aggregates_values_positive
        CHECK (total_usage >= 0 AND usage_count >= 0),

    CONSTRAINT chk_usage_aggregates_period_range
        CHECK (period_start < period_end),

    -- Ensure no duplicate aggregates for same resource and period
    CONSTRAINT uniq_usage_aggregates_unique
        UNIQUE (customer_resource_id, period_type, period_start)
);

-- 5. Alerts
-- Alert system for threshold breaches and anomalies
CREATE TABLE alerts (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Alert reference
    customer_resource_id VARCHAR(36) NOT NULL,

    -- Alert details
    alert_type VARCHAR(50) NOT NULL, -- THRESHOLD_WARNING, THRESHOLD_CRITICAL, ANOMALY_DETECTED
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    message TEXT NOT NULL,

    -- Metric values
    current_value DECIMAL(15,4),
    threshold_value DECIMAL(15,4),
    percentage DECIMAL(5,2), -- % of threshold or limit

    -- Alert lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ACKNOWLEDGED, RESOLVED, SUPPRESSED
    acknowledged_by VARCHAR(36),
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,

    -- Additional data
    metadata JSONB,

    -- Constraints
    CONSTRAINT fk_alerts_customer_resource
        FOREIGN KEY (customer_resource_id) REFERENCES customer_resources(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_alerts_type
        CHECK (alert_type IN ('THRESHOLD_WARNING', 'THRESHOLD_CRITICAL', 'ANOMALY_DETECTED', 'USAGE_SPIKE', 'LIMIT_EXCEEDED')),

    CONSTRAINT chk_alerts_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

    CONSTRAINT chk_alerts_status
        CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED', 'SUPPRESSED')),

    CONSTRAINT chk_alerts_acknowledged_consistency
        CHECK (
            (acknowledged_by IS NULL AND acknowledged_at IS NULL) OR
            (acknowledged_by IS NOT NULL AND acknowledged_at IS NOT NULL)
        ),

    CONSTRAINT chk_alerts_resolved_consistency
        CHECK (
            (resolved_at IS NULL AND status IN ('ACTIVE', 'ACKNOWLEDGED')) OR
            (resolved_at IS NOT NULL AND status = 'RESOLVED')
        )
);

-- 6. Cost Models
-- Define billing models per tenant
CREATE TABLE cost_models (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Model identification
    tenant_id VARCHAR(36) NOT NULL DEFAULT 'default-tenant',
    model_name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Billing parameters
    billing_period VARCHAR(20) NOT NULL, -- monthly, yearly
    base_cost DECIMAL(10,2) NOT NULL DEFAULT 0, -- Flat fee
    overage_rate DECIMAL(10,4) NOT NULL DEFAULT 0, -- Rate for usage over limit
    included_usage DECIMAL(15,4) NOT NULL DEFAULT 0, -- Usage included in base cost

    -- Model settings
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    rounding_mode VARCHAR(20) NOT NULL DEFAULT 'NEAREST', -- NEAREST, CEIL, FLOOR
    rounding_increment DECIMAL(10,4) NOT NULL DEFAULT 0.01,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Constraints
    CONSTRAINT chk_cost_models_billing_period
        CHECK (billing_period IN ('monthly', 'yearly')),

    CONSTRAINT chk_cost_models_currency
        CHECK (char_length(currency) = 3),

    CONSTRAINT chk_cost_models_rounding_mode
        CHECK (rounding_mode IN ('NEAREST', 'CEIL', 'FLOOR')),

    CONSTRAINT chk_cost_models_values_positive
        CHECK (base_cost >= 0 AND overage_rate >= 0 AND included_usage >= 0 AND rounding_increment > 0),

    -- Ensure only one active model per tenant
    CONSTRAINT uniq_cost_models_active_per_tenant
        UNIQUE (tenant_id) DEFERRABLE INITIALLY DEFERRED
);

-- 7. Cost Forecasts
-- Predicted costs for planning
CREATE TABLE cost_forecasts (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Forecast reference
    customer_resource_id VARCHAR(36) NOT NULL,

    -- Forecast period
    forecast_period_start DATE NOT NULL,
    forecast_period_end DATE NOT NULL,
    forecast_type VARCHAR(20) NOT NULL DEFAULT 'LINEAR', -- LINEAR, SEASONAL, ML

    -- Predicted values
    predicted_usage DECIMAL(15,4),
    predicted_cost DECIMAL(10,2),
    confidence_level DECIMAL(5,2), -- 0-100, confidence in forecast

    -- Model metadata
    model_version VARCHAR(20),
    algorithm VARCHAR(50), -- 'linear_regression', 'seasonal_naive', 'arima'

    -- Actual vs predicted (for comparison after period ends)
    actual_usage DECIMAL(15,4),
    actual_cost DECIMAL(10,2),
    error_percentage DECIMAL(5,2), -- |actual - predicted| / predicted * 100

    -- Constraints
    CONSTRAINT fk_cost_forecasts_customer_resource
        FOREIGN KEY (customer_resource_id) REFERENCES customer_resources(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_cost_forecasts_period_range
        CHECK (forecast_period_start < forecast_period_end),

    CONSTRAINT chk_cost_forecasts_forecast_type
        CHECK (forecast_type IN ('LINEAR', 'SEASONAL', 'ML', 'EXPERT')),

    CONSTRAINT chk_cost_forecasts_confidence
        CHECK (confidence_level IS NULL OR (confidence_level >= 0 AND confidence_level <= 100)),

    CONSTRAINT chk_cost_forecasts_positive
        CHECK (predicted_usage >= 0 AND predicted_cost >= 0)
);

-- 8. Resource Thresholds
-- Configurable thresholds for different resource types
CREATE TABLE resource_thresholds (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Threshold configuration
    tenant_id VARCHAR(36) NOT NULL DEFAULT 'default-tenant',
    resource_type VARCHAR(50) NOT NULL,
    threshold_name VARCHAR(100) NOT NULL,

    -- Threshold values
    warning_percentage DECIMAL(5,2) NOT NULL DEFAULT 80,
    critical_percentage DECIMAL(5,2) NOT NULL DEFAULT 95,
    max_percentage DECIMAL(5,2) NOT NULL DEFAULT 100,

    -- Notification settings
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    slack_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Recurrence settings
    alert_cooldown_minutes INTEGER NOT NULL DEFAULT 60, -- Min time between alerts of same type
    max_alerts_per_day INTEGER, -- NULL = unlimited

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Constraints
    CONSTRAINT chk_resource_thresholds_type
        CHECK (resource_type IN ('bandwidth', 'storage', 'api_calls', 'cpu', 'memory')),

    CONSTRAINT chk_resource_thresholds_percentages
        CHECK (
            warning_percentage <= critical_percentage AND
            critical_percentage <= max_percentage AND
            warning_percentage >= 0 AND
            critical_percentage >= 0 AND
            max_percentage >= 0
        ),

    CONSTRAINT chk_resource_thresholds_cooldown
        CHECK (alert_cooldown_minutes > 0),

    CONSTRAINT chk_resource_thresholds_max_alerts
        CHECK (max_alerts_per_day IS NULL OR max_alerts_per_day > 0),

    -- Ensure unique threshold per resource type per tenant
    CONSTRAINT uniq_resource_thresholds
        UNIQUE (tenant_id, resource_type, threshold_name)
);

-- Indexes for Resource Catalogs
CREATE INDEX idx_resource_catalogs_tenant ON resource_catalogs(tenant_id);
CREATE INDEX idx_resource_catalogs_type ON resource_catalogs(resource_type);
CREATE INDEX idx_resource_catalogs_billable ON resource_catalogs(is_billable) WHERE is_billable = TRUE;
CREATE INDEX idx_resource_catalogs_category ON resource_catalogs(category);

-- Indexes for Customer Resources
CREATE INDEX idx_customer_resources_customer ON customer_resources(customer_id);
CREATE INDEX idx_customer_resources_catalog ON customer_resources(resource_catalog_id);
CREATE INDEX idx_customer_resources_status ON customer_resources(status);
CREATE INDEX idx_customer_resources_subscription ON customer_resources(subscription_id);
CREATE INDEX idx_customer_resources_deleted_at ON customer_resources(deleted_at) WHERE deleted_at IS NULL;

-- Composite index for resource lookup
CREATE INDEX idx_customer_resources_lookup ON customer_resources(customer_id, resource_catalog_id, status) WHERE deleted_at IS NULL;

-- Indexes for Usage Metrics (Time-series)
CREATE INDEX idx_usage_metrics_customer_resource ON usage_metrics(customer_resource_id);
CREATE INDEX idx_usage_metrics_timestamp ON usage_metrics(metric_timestamp);
CREATE INDEX idx_usage_metrics_customer_time ON usage_metrics(customer_resource_id, metric_timestamp);
CREATE INDEX idx_usage_metrics_source ON usage_metrics(source);
CREATE INDEX idx_usage_metrics_time_range ON usage_metrics(metric_timestamp) WHERE metric_timestamp > NOW() - INTERVAL '1 year';

-- Composite index for recent metrics
CREATE INDEX idx_usage_metrics_recent ON usage_metrics(customer_resource_id, metric_timestamp DESC);

-- Indexes for Usage Aggregates
CREATE INDEX idx_usage_aggregates_customer_resource ON usage_aggregates(customer_resource_id);
CREATE INDEX idx_usage_aggregates_period_type ON usage_aggregates(period_type);
CREATE INDEX idx_usage_aggregates_customer_period ON usage_aggregates(customer_resource_id, period_type, period_start);
CREATE INDEX idx_usage_aggregates_time_range ON usage_aggregates(period_start, period_end);

-- Indexes for Alerts
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_customer_resource ON alerts(customer_resource_id);
CREATE INDEX idx_alerts_severity ON alerts(severity);
CREATE INDEX idx_alerts_type ON alerts(alert_type);
CREATE INDEX idx_alerts_created_at ON alerts(created_at);
CREATE INDEX idx_alerts_active_unresolved ON alerts(status, created_at) WHERE status IN ('ACTIVE', 'ACKNOWLEDGED');

-- Composite index for alert queries
CREATE INDEX idx_alerts_active_customer ON alerts(customer_resource_id, status, created_at DESC) WHERE status IN ('ACTIVE', 'ACKNOWLEDGED');

-- Indexes for Cost Models
CREATE INDEX idx_cost_models_tenant ON cost_models(tenant_id);
CREATE INDEX idx_cost_models_active ON cost_models(is_active) WHERE is_active = TRUE;

-- Indexes for Cost Forecasts
CREATE INDEX idx_cost_forecasts_customer_resource ON cost_forecasts(customer_resource_id);
CREATE INDEX idx_cost_forecasts_period ON cost_forecasts(forecast_period_start, forecast_period_end);
CREATE INDEX idx_cost_forecasts_type ON cost_forecasts(forecast_type);

-- Composite index for forecast lookup
CREATE INDEX idx_cost_forecasts_customer_period ON cost_forecasts(customer_resource_id, forecast_period_start) WHERE actual_usage IS NULL;

-- Indexes for Resource Thresholds
CREATE INDEX idx_resource_thresholds_tenant ON resource_thresholds(tenant_id);
CREATE INDEX idx_resource_thresholds_type ON resource_thresholds(resource_type);
CREATE INDEX idx_resource_thresholds_active ON resource_thresholds(is_active) WHERE is_active = TRUE;

-- Composite index for threshold lookup
CREATE INDEX idx_resource_thresholds_tenant_type ON resource_thresholds(tenant_id, resource_type) WHERE is_active = TRUE;

-- Views for common queries

-- View: Active customer resources with catalog info
CREATE VIEW v_customer_resources_active AS
SELECT
    cr.id,
    cr.customer_id,
    cr.resource_catalog_id,
    cr.current_usage,
    cr.limit_value,
    cr.warning_threshold,
    cr.critical_threshold,
    cr.status,
    rc.resource_type,
    rc.resource_name,
    rc.unit,
    rc.cost_per_unit,
    rc.is_billable,
    -- Calculate usage percentage
    CASE
        WHEN cr.limit_value IS NULL THEN NULL
        ELSE ROUND((cr.current_usage / cr.limit_value) * 100, 2)
    END AS usage_percentage,
    -- Calculate days until limit
    CASE
        WHEN cr.limit_value IS NULL THEN NULL
        ELSE GREATEST(0, FLOOR(cr.limit_value - cr.current_usage))
    END AS remaining_usage
FROM customer_resources cr
JOIN resource_catalogs rc ON cr.resource_catalog_id = rc.id
WHERE cr.status = 'ACTIVE' AND cr.deleted_at IS NULL AND rc.deleted_at IS NULL;

-- View: Current usage summary per customer
CREATE VIEW v_customer_usage_summary AS
SELECT
    cr.customer_id,
    rc.resource_type,
    COUNT(*) AS resource_count,
    SUM(cr.current_usage) AS total_usage,
    AVG(cr.current_usage) AS avg_usage,
    MAX(cr.current_usage) AS max_usage,
    SUM(cr.limit_value) AS total_limit,
    CASE
        WHEN SUM(cr.limit_value) > 0 THEN
            ROUND((SUM(cr.current_usage) / SUM(cr.limit_value)) * 100, 2)
        ELSE NULL
    END AS overall_usage_percentage,
    SUM(CASE WHEN cr.current_usage >= cr.critical_threshold THEN 1 ELSE 0 END) AS critical_resources,
    SUM(CASE WHEN cr.current_usage >= cr.warning_threshold AND cr.current_usage < cr.critical_threshold THEN 1 ELSE 0 END) AS warning_resources
FROM customer_resources cr
JOIN resource_catalogs rc ON cr.resource_catalog_id = rc.id
WHERE cr.status = 'ACTIVE' AND cr.deleted_at IS NULL
GROUP BY cr.customer_id, rc.resource_type;

-- Triggers for updated_at

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to tables with updated_at
CREATE TRIGGER update_resource_catalogs_updated_at
    BEFORE UPDATE ON resource_catalogs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customer_resources_updated_at
    BEFORE UPDATE ON customer_resources
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_alerts_updated_at
    BEFORE UPDATE ON alerts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cost_models_updated_at
    BEFORE UPDATE ON cost_models
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_resource_thresholds_updated_at
    BEFORE UPDATE ON resource_thresholds
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation

COMMENT ON TABLE resource_catalogs IS 'Catalog of all trackable resources (bandwidth, storage, API calls, etc.)';
COMMENT ON TABLE customer_resources IS 'Resources assigned to customers with usage tracking and thresholds';
COMMENT ON TABLE usage_metrics IS 'Raw usage metrics for real-time tracking';
COMMENT ON TABLE usage_aggregates IS 'Aggregated usage data (hourly/daily/monthly)';
COMMENT ON TABLE alerts IS 'Alert system for threshold breaches and anomalies';
COMMENT ON TABLE cost_models IS 'Billing models for cost calculation';
COMMENT ON TABLE cost_forecasts IS 'Predicted costs for planning purposes';
COMMENT ON TABLE resource_thresholds IS 'Configurable thresholds by resource type';

-- Insert default resource catalog
INSERT INTO resource_catalogs (id, resource_type, resource_name, unit, cost_per_unit, is_billable, description, category)
VALUES
    -- Bandwidth resources
    (UUID(), 'bandwidth', 'Internet 100Mbps', 'GB', 0.10, TRUE, 'Standard internet access 100Mbps', 'network'),
    (UUID(), 'bandwidth', 'Internet 500Mbps', 'GB', 0.08, TRUE, 'High-speed internet access 500Mbps', 'network'),
    (UUID(), 'bandwidth', 'Internet 1Gbps', 'GB', 0.05, TRUE, 'Premium internet access 1Gbps', 'network'),

    -- Storage resources
    (UUID(), 'storage', 'Cloud Storage 100GB', 'GB', 0.05, TRUE, 'Cloud storage allocation 100GB', 'storage'),
    (UUID(), 'storage', 'Cloud Storage 1TB', 'GB', 0.04, TRUE, 'Cloud storage allocation 1TB', 'storage'),
    (UUID(), 'storage', 'Cloud Storage 10TB', 'GB', 0.03, TRUE, 'Cloud storage allocation 10TB', 'storage'),

    -- API resources
    (UUID(), 'api_calls', 'API Requests', 'request', 0.001, TRUE, 'API call usage tracking', 'service'),
    (UUID(), 'api_calls', 'Premium API Requests', 'request', 0.002, TRUE, 'Premium API with higher limits', 'service'),

    -- Compute resources
    (UUID(), 'cpu', 'CPU Hours', 'hour', 0.05, TRUE, 'CPU time consumption', 'compute'),
    (UUID(), 'memory', 'Memory Hours', 'hour', 0.03, TRUE, 'Memory consumption', 'compute'),

    -- I/O resources
    (UUID(), 'disk_io', 'Disk I/O Operations', 'operation', 0.0001, TRUE, 'Disk read/write operations', 'storage'),
    (UUID(), 'network_io', 'Network I/O', 'MB', 0.001, TRUE, 'Network data transfer', 'network');

-- Insert default cost model
INSERT INTO cost_models (id, model_name, description, billing_period, base_cost, overage_rate, included_usage)
VALUES (
    UUID(),
    'Default Monthly',
    'Standard monthly billing with overage charges',
    'monthly',
    0.00,
    0.10,
    0
);

-- Insert default resource thresholds
INSERT INTO resource_thresholds (id, resource_type, threshold_name, warning_percentage, critical_percentage, max_percentage)
VALUES
    (UUID(), 'bandwidth', 'Default Bandwidth Thresholds', 80, 95, 100),
    (UUID(), 'storage', 'Default Storage Thresholds', 80, 95, 100),
    (UUID(), 'api_calls', 'Default API Thresholds', 80, 95, 100),
    (UUID(), 'cpu', 'Default CPU Thresholds', 70, 90, 100),
    (UUID(), 'memory', 'Default Memory Thresholds', 70, 90, 100),
    (UUID(), 'disk_io', 'Default Disk I/O Thresholds', 80, 95, 100),
    (UUID(), 'network_io', 'Default Network I/O Thresholds', 80, 95, 100);
