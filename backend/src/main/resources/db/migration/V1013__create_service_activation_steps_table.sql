-- Create service_activation_steps table
CREATE TABLE service_activation_steps (
    id VARCHAR(36) PRIMARY KEY,
    activation_id VARCHAR(36) NOT NULL,
    step_order INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_description TEXT,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    provisioning_system VARCHAR(200),
    provisioning_command VARCHAR(500),
    execution_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (activation_id) REFERENCES service_activations(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_service_activation_steps_activation_id ON service_activation_steps(activation_id);
CREATE INDEX idx_service_activation_steps_status ON service_activation_steps(status);
CREATE INDEX idx_service_activation_steps_step_order ON service_activation_steps(activation_id, step_order);
