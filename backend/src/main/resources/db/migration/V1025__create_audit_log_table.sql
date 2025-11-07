-- Create audit log table
-- This table implements WORM (Write Once Read Many) pattern for compliance
-- All sensitive operations are logged here

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Audit-specific fields
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id VARCHAR(255),
    username VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(255),
    entity_id VARCHAR(255),
    description TEXT,
    old_values JSONB,
    new_values JSONB,
    metadata JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    session_id VARCHAR(255),
    request_id VARCHAR(255),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    execution_time_ms BIGINT,
    correlation_id VARCHAR(255),
    source VARCHAR(255) NOT NULL,
    version_field VARCHAR(50)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log (timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_ip ON audit_log (ip_address);
CREATE INDEX IF NOT EXISTS idx_audit_success ON audit_log (success);
CREATE INDEX IF NOT EXISTS idx_audit_correlation ON audit_log (correlation_id);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_audit_user_timestamp ON audit_log (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_timestamp ON audit_log (action, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity_timestamp ON audit_log (entity_type, entity_id, timestamp DESC);

-- Enable RLS (Row Level Security) for audit logs
-- Audit logs are immutable - only inserts are allowed
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

-- Create policy to allow only inserts
CREATE POLICY audit_log_insert_policy ON audit_log
    FOR INSERT
    TO bss_app
    WITH CHECK (true);

-- Create policy to allow selects for authenticated users
CREATE POLICY audit_log_select_policy ON audit_log
    FOR SELECT
    TO bss_app
    USING (auth.uid() IS NOT NULL);

-- Create policy to prevent updates and deletes
CREATE POLICY audit_log_no_update_policy ON audit_log
    FOR UPDATE
    TO bss_app
    USING (false);

CREATE POLICY audit_log_no_delete_policy ON audit_log
    FOR DELETE
    TO bss_app
    USING (false);

-- Create trigger to prevent updates and deletes
CREATE OR REPLACE FUNCTION audit_log_prevent_modifications()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable - modifications are not allowed';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_prevent_updates
    BEFORE UPDATE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION audit_log_prevent_modifications();

CREATE TRIGGER audit_log_prevent_deletes
    BEFORE DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION audit_log_prevent_modifications();

-- Add comment
COMMENT ON TABLE audit_log IS 'Immutable audit log for compliance - WORM pattern';
COMMENT ON COLUMN audit_log.id IS 'Unique audit log ID';
COMMENT ON COLUMN audit_log.timestamp IS 'When the operation occurred';
COMMENT ON COLUMN audit_log.user_id IS 'ID of user performing the action';
COMMENT ON COLUMN audit_log.action IS 'Type of action performed';
COMMENT ON COLUMN audit_log.old_values IS 'State before the change (JSONB)';
COMMENT ON COLUMN audit_log.new_values IS 'State after the change (JSONB)';
COMMENT ON COLUMN audit_log.success IS 'Whether the operation succeeded';
COMMENT ON COLUMN audit_log.correlation_id IS 'Correlation ID for tracing related operations';
COMMENT ON COLUMN audit_log.source IS 'Source system (BSS-System)';
