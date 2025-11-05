-- Assets table for equipment tracking
CREATE TABLE assets (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    asset_tag VARCHAR(100) NOT NULL UNIQUE,
    asset_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    serial_number VARCHAR(255) UNIQUE,
    model_number VARCHAR(255),
    manufacturer VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    purchase_date DATE,
    warranty_expiry DATE,
    location VARCHAR(500),
    assigned_to_type VARCHAR(100),
    assigned_to_id VARCHAR(36),
    assigned_to_name VARCHAR(255),
    assigned_date DATE,
    cost_center VARCHAR(100),
    notes TEXT
);

-- Create indexes
CREATE INDEX idx_assets_asset_tag ON assets(asset_tag);
CREATE INDEX idx_assets_asset_type ON assets(asset_type);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_assigned_to ON assets(assigned_to_id, assigned_to_type);
CREATE INDEX idx_assets_warranty_expiry ON assets(warranty_expiry);
CREATE INDEX idx_assets_location ON assets(location);
CREATE INDEX idx_assets_deleted_at ON assets(deleted_at);

-- Comments
COMMENT ON TABLE assets IS 'Assets for equipment and hardware tracking';
COMMENT ON COLUMN assets.asset_tag IS 'Unique asset identifier';
COMMENT ON COLUMN assets.asset_type IS 'Type of asset (ROUTER, SWITCH, MODEM, etc.)';
COMMENT ON COLUMN assets.status IS 'Asset status (AVAILABLE, IN_USE, MAINTENANCE, etc.)';
COMMENT ON COLUMN assets.assigned_to_type IS 'Type of assignment (CUSTOMER, LOCATION, DEPARTMENT)';
COMMENT ON COLUMN assets.assigned_to_id IS 'ID of assigned entity';
COMMENT ON COLUMN assets.assigned_to_name IS 'Name of assigned entity';
COMMENT ON COLUMN assets.warranty_expiry IS 'Warranty expiry date';
