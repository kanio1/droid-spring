-- SIM cards table for mobile/cellular asset tracking
CREATE TABLE sim_cards (
    id VARCHAR(36) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    iccid VARCHAR(20) NOT NULL UNIQUE,
    msisdn VARCHAR(15),
    imsi VARCHAR(15) UNIQUE,
    pin VARCHAR(8),
    puk VARCHAR(8),
    ki VARCHAR(32),
    status VARCHAR(50) NOT NULL,
    network_operator VARCHAR(255),
    apn VARCHAR(255),
    activation_date DATE,
    deactivation_date DATE,
    expiry_date DATE,
    assigned_to_type VARCHAR(100),
    assigned_to_id VARCHAR(36),
    assigned_to_name VARCHAR(255),
    assigned_date DATE,
    data_limit_mb BIGINT,
    data_used_mb BIGINT DEFAULT 0,
    voice_limit_minutes BIGINT,
    voice_used_minutes BIGINT DEFAULT 0,
    sms_limit BIGINT,
    sms_used BIGINT DEFAULT 0,
    last_usage_date DATE,
    notes TEXT
);

-- Create indexes
CREATE INDEX idx_sim_cards_iccid ON sim_cards(iccid);
CREATE INDEX idx_sim_cards_msisdn ON sim_cards(msisdn);
CREATE INDEX idx_sim_cards_imsi ON sim_cards(imsi);
CREATE INDEX idx_sim_cards_status ON sim_cards(status);
CREATE INDEX idx_sim_cards_assigned_to ON sim_cards(assigned_to_id, assigned_to_type);
CREATE INDEX idx_sim_cards_expiry ON sim_cards(expiry_date);
CREATE INDEX idx_sim_cards_deleted_at ON sim_cards(deleted_at);

-- Comments
COMMENT ON TABLE sim_cards IS 'SIM cards for mobile/cellular asset tracking';
COMMENT ON COLUMN sim_cards.iccid IS 'Integrated Circuit Card Identifier';
COMMENT ON COLUMN sim_cards.msisdn IS 'Mobile Station International Subscriber Directory Number';
COMMENT ON COLUMN sim_cards.imsi IS 'International Mobile Subscriber Identity';
COMMENT ON COLUMN sim_cards.status IS 'SIM card status (AVAILABLE, ASSIGNED, SUSPENDED, etc.)';
COMMENT ON COLUMN sim_cards.assigned_to_type IS 'Type of assignment (CUSTOMER, DEVICE)';
COMMENT ON COLUMN sim_cards.assigned_to_id IS 'ID of assigned entity';
COMMENT ON COLUMN sim_cards.assigned_to_name IS 'Name of assigned entity';
COMMENT ON COLUMN sim_cards.data_limit_mb IS 'Data usage limit in megabytes';
COMMENT ON COLUMN sim_cards.data_used_mb IS 'Data used in megabytes';
COMMENT ON COLUMN sim_cards.voice_limit_minutes IS 'Voice usage limit in minutes';
COMMENT ON COLUMN sim_cards.voice_used_minutes IS 'Voice used in minutes';
COMMENT ON COLUMN sim_cards.sms_limit IS 'SMS usage limit';
COMMENT ON COLUMN sim_cards.sms_used IS 'SMS used';
