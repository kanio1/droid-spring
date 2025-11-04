-- Create service_activations table
CREATE TABLE service_activations (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    service_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    activation_date TIMESTAMP,
    deactivation_date TIMESTAMP,
    scheduled_date TIMESTAMP,
    activation_notes TEXT,
    deactivation_notes TEXT,
    correlation_id VARCHAR(100),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

-- Create indexes
CREATE INDEX idx_service_activations_customer_id ON service_activations(customer_id);
CREATE INDEX idx_service_activations_service_id ON service_activations(service_id);
CREATE INDEX idx_service_activations_status ON service_activations(status);
CREATE INDEX idx_service_activations_correlation_id ON service_activations(correlation_id);
CREATE INDEX idx_service_activations_scheduled_date ON service_activations(scheduled_date);
