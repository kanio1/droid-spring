-- Create service_dependencies table
CREATE TABLE service_dependencies (
    service_id VARCHAR(36) NOT NULL,
    depends_on_service_code VARCHAR(100) NOT NULL,
    PRIMARY KEY (service_id, depends_on_service_code),
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

-- Create index
CREATE INDEX idx_service_dependencies_service_id ON service_dependencies(service_id);
