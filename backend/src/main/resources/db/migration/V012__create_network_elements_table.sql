-- Network Elements table
-- Network infrastructure inventory

CREATE TYPE element_type AS ENUM ('CELL_TOWER', 'BASE_STATION', 'SWITCH', 'ROUTER', 'SERVER', 'CABLE');
CREATE TYPE element_status AS ENUM ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'FAULT', 'DECOMMISSIONED');
CREATE TYPE capacity_unit AS ENUM ('USERS', 'MBPS', 'GB', 'PORTS');

CREATE TABLE network_elements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    element_code VARCHAR(50) NOT NULL UNIQUE,
    element_type element_type NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    location_id UUID,
    status element_status DEFAULT 'ACTIVE',
    vendor VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    firmware_version VARCHAR(50),
    ip_address INET,
    mac_address VARCHAR(17),
    capacity_value DECIMAL(15,3),
    capacity_unit capacity_unit,
    longitude DECIMAL(10, 6),
    latitude DECIMAL(10, 6),
    installation_date DATE,
    warranty_expiry DATE,
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    attributes JSONB,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_network_elements_type ON network_elements(element_type);
CREATE INDEX idx_network_elements_status ON network_elements(status);
CREATE INDEX idx_network_elements_location ON network_elements(location_id);
CREATE INDEX idx_network_elements_coordinates ON network_elements(longitude, latitude);
CREATE INDEX idx_network_elements_code ON network_elements(element_code);

-- Update trigger for updated_at column
CREATE TRIGGER update_network_elements_updated_at
    BEFORE UPDATE ON network_elements
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
