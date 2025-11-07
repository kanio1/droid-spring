-- Create outbox_event table
-- Implements Outbox Pattern for reliable event publishing
-- Ensures events are not lost during database transactions

CREATE TABLE IF NOT EXISTS outbox_event (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Outbox-specific fields
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    event_name VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255),
    aggregate_type VARCHAR(100),
    event_data JSONB NOT NULL,
    metadata JSONB,
    version_field VARCHAR(50),
    source VARCHAR(255),
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    user_id VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    trace_id VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_event (status);
CREATE INDEX IF NOT EXISTS idx_outbox_type ON outbox_event (event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_event (aggregate_id);
CREATE INDEX IF NOT EXISTS idx_outbox_created ON outbox_event (created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_status_type ON outbox_event (status, event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_retry ON outbox_event (status, next_retry_at, retry_count);
CREATE INDEX IF NOT EXISTS idx_outbox_trace ON outbox_event (trace_id);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_outbox_pending_retry ON outbox_event (status, next_retry_at) WHERE status IN ('PENDING', 'RETRY');
CREATE INDEX IF NOT EXISTS idx_outbox_published_range ON outbox_event (published_at) WHERE status = 'PUBLISHED';

-- Create unique constraint on event_id
ALTER TABLE outbox_event ADD CONSTRAINT IF NOT EXISTS uk_outbox_event_id UNIQUE (event_id);

-- Create function to automatically set event_id if not provided
CREATE OR REPLACE FUNCTION set_outbox_event_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.event_id IS NULL THEN
        NEW.event_id := gen_random_uuid()::text;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-generate event_id
CREATE TRIGGER outbox_event_set_id
    BEFORE INSERT ON outbox_event
    FOR EACH ROW
    EXECUTE FUNCTION set_outbox_event_id();

-- Create function to validate outbox event
CREATE OR REPLACE FUNCTION validate_outbox_event()
RETURNS TRIGGER AS $$
BEGIN
    -- Ensure status is valid
    IF NEW.status NOT IN ('PENDING', 'PUBLISHED', 'RETRY', 'DEAD_LETTER') THEN
        RAISE EXCEPTION 'Invalid status: %', NEW.status;
    END IF;

    -- Ensure next_retry_at is set for RETRY status
    IF NEW.status = 'RETRY' AND NEW.next_retry_at IS NULL THEN
        RAISE EXCEPTION 'next_retry_at must be set for RETRY status';
    END IF;

    -- Ensure published_at is set for PUBLISHED status
    IF NEW.status = 'PUBLISHED' AND NEW.published_at IS NULL THEN
        NEW.published_at := NOW();
    END IF;

    -- Ensure retry_count doesn't exceed max_retries
    IF NEW.retry_count > NEW.max_retries THEN
        RAISE EXCEPTION 'retry_count (%) exceeds max_retries (%)', NEW.retry_count, NEW.max_retries;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to validate outbox events
CREATE TRIGGER outbox_event_validate
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW
    EXECUTE FUNCTION validate_outbox_event();

-- Enable RLS (Row Level Security)
ALTER TABLE outbox_event ENABLE ROW LEVEL SECURITY;

-- Create policy to allow service role to manage outbox events
CREATE POLICY outbox_event_service_policy ON outbox_event
    FOR ALL
    TO bss_app
    USING (true)
    WITH CHECK (true);

-- Create view for outbox statistics
CREATE OR REPLACE VIEW outbox_statistics AS
SELECT
    status,
    COUNT(*) as count,
    COUNT(*) * 100.0 / (SELECT COUNT(*) FROM outbox_event) as percentage
FROM outbox_event
GROUP BY status;

-- Create view for failed events
CREATE OR REPLACE VIEW outbox_dead_letters AS
SELECT
    e.*
FROM outbox_event e
WHERE e.status = 'DEAD_LETTER'
ORDER BY e.created_at DESC;

-- Add comments
COMMENT ON TABLE outbox_event IS 'Outbox Pattern - Reliable event publishing';
COMMENT ON COLUMN outbox_event.event_id IS 'Unique event identifier (CloudEvents ce_id)';
COMMENT ON COLUMN outbox_event.event_type IS 'Type of event (enum)';
COMMENT ON COLUMN outbox_event.event_name IS 'Event name for humans';
COMMENT ON COLUMN outbox_event.aggregate_id IS 'ID of the aggregate root';
COMMENT ON COLUMN outbox_event.event_data IS 'Event payload (JSONB)';
COMMENT ON COLUMN outbox_event.correlation_id IS 'ID for correlating related events';
COMMENT ON COLUMN outbox_event.causation_id IS 'ID of event that caused this event';
COMMENT ON COLUMN outbox_event.status IS 'Current status: PENDING, PUBLISHED, RETRY, DEAD_LETTER';
COMMENT ON COLUMN outbox_event.retry_count IS 'Number of retry attempts';
COMMENT ON COLUMN outbox_event.next_retry_at IS 'Next retry timestamp';

-- Create function to clean up old published events
CREATE OR REPLACE FUNCTION cleanup_old_outbox_events()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Delete published events older than 30 days
    DELETE FROM outbox_event
    WHERE status = 'PUBLISHED'
      AND published_at < NOW() - INTERVAL '30 days';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION cleanup_old_outbox_events() TO bss_app;
