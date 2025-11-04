-- Create services table
CREATE TABLE services (
    id VARCHAR(36) PRIMARY KEY,
    service_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    service_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    category VARCHAR(100),
    provisioning_time_minutes INTEGER,
    deprovisioning_time_minutes INTEGER,
    requires_equipment BOOLEAN DEFAULT FALSE,
    auto_provision BOOLEAN DEFAULT TRUE,
    provisioning_script VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

-- Create index on service_code
CREATE INDEX idx_services_service_code ON services(service_code);

-- Create index on service_type
CREATE INDEX idx_services_service_type ON services(service_type);

-- Create index on status
CREATE INDEX idx_services_status ON services(status);

-- Create index on category
CREATE INDEX idx_services_category ON services(category);
