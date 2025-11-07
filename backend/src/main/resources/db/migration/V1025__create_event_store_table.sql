-- Create event store table
CREATE TABLE event_store (
    id VARCHAR(36) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data LONGTEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    user_id VARCHAR(255),
    correlation_id VARCHAR(255),
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_aggregate_id (aggregate_id),
    INDEX idx_aggregate_type (aggregate_type),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_correlation_id (correlation_id),
    INDEX idx_aggregate_version (aggregate_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create event store snapshot table
CREATE TABLE snapshot_store (
    id VARCHAR(36) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL UNIQUE,
    aggregate_type VARCHAR(255) NOT NULL,
    state LONGTEXT NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_aggregate_id_snapshot (aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
