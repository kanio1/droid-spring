-- Network elements table for infrastructure tracking
CREATE TABLE network_elements (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    element_id VARCHAR(100) NOT NULL UNIQUE,
    element_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    ip_address INET,
    mac_address VARCHAR(17),
    firmware_version VARCHAR(100),
    software_version VARCHAR(100),
    location VARCHAR(100),
    rack_position VARCHAR(100),
    port_count INTEGER,
    capacity VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    last_heartbeat TIMESTAMP,
    operational_since TIMESTAMP,
    maintenance_mode BOOLEAN DEFAULT FALSE,
    maintenance_window_start TIMESTAMP,
    maintenance_window_end TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_network_elements_element_id ON network_elements(element_id);
CREATE INDEX idx_network_elements_element_type ON network_elements(element_type);
CREATE INDEX idx_network_elements_status ON network_elements(status);
CREATE INDEX idx_network_elements_ip_address ON network_elements(ip_address);
CREATE INDEX idx_network_elements_location ON network_elements(location);
CREATE INDEX idx_network_elements_rack ON network_elements(rack_position);
CREATE INDEX idx_network_elements_heartbeat ON network_elements(last_heartbeat);
CREATE INDEX idx_network_elements_deleted_at ON network_elements(deleted_at);

-- Comments
COMMENT ON TABLE network_elements IS 'Network elements for infrastructure tracking';
COMMENT ON COLUMN network_elements.element_id IS 'Unique network element identifier';
COMMENT ON COLUMN network_elements.element_type IS 'Type of network element (CORE_ROUTER, SWITCH, etc.)';
COMMENT ON COLUMN network_elements.status IS 'Element status (AVAILABLE, IN_USE, MAINTENANCE, etc.)';
COMMENT ON COLUMN network_elements.last_heartbeat IS 'Last heartbeat timestamp from element';
COMMENT ON COLUMN network_elements.operational_since IS 'When element became operational';
COMMENT ON COLUMN network_elements.maintenance_mode IS 'Whether element is in maintenance mode';
