-- Addresses table for customer address management
-- Supports multiple address types per customer (billing, shipping, service, correspondence)

CREATE TABLE addresses (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Customer association
    customer_id VARCHAR(36) NOT NULL,

    -- Address type and status
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- Address lines
    street VARCHAR(255) NOT NULL,
    house_number VARCHAR(20),
    apartment_number VARCHAR(20),

    -- Postal and location
    postal_code VARCHAR(10) NOT NULL,
    city VARCHAR(100) NOT NULL,
    region VARCHAR(100),
    country VARCHAR(2) NOT NULL,

    -- Geographic coordinates (optional)
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,

    -- Address metadata
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    -- Additional fields
    notes TEXT,

    -- Constraints
    CONSTRAINT fk_addresses_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_addresses_postal_code
        CHECK (postal_code ~ '^\d{2}-\d{3}$'),

    CONSTRAINT chk_addresses_country_length
        CHECK (char_length(country) = 2),

    CONSTRAINT chk_addresses_coordinates
        CHECK (
            (latitude IS NULL AND longitude IS NULL) OR
            (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
        )
);

-- Indexes for common queries
CREATE INDEX idx_addresses_customer_id ON addresses(customer_id);
CREATE INDEX idx_addresses_customer_id_type ON addresses(customer_id, type);
CREATE INDEX idx_addresses_status ON addresses(status);
CREATE INDEX idx_addresses_country ON addresses(country);
CREATE INDEX idx_addresses_postal_code ON addresses(postal_code);
CREATE INDEX idx_addresses_is_primary ON addresses(is_primary) WHERE is_primary = TRUE;
CREATE INDEX idx_addresses_deleted_at ON addresses(deleted_at) WHERE deleted_at IS NULL;

-- Composite index for address search
CREATE INDEX idx_addresses_search ON addresses(customer_id, type, status, country) WHERE deleted_at IS NULL;

-- Ensure only one primary address per customer per type
CREATE UNIQUE INDEX idx_addresses_primary_per_type
    ON addresses(customer_id, type)
    WHERE is_primary = TRUE AND deleted_at IS NULL;
