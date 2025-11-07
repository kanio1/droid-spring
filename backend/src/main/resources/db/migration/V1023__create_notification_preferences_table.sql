-- Notification Preferences for Multi-Channel Alerting
-- Phase 2: Core Features - Notification Service
-- Supports email, SMS, and Slack notifications with per-customer preferences

CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Customer association
    customer_id BIGINT NOT NULL UNIQUE,

    -- Notification channels
    email VARCHAR(255),
    phone_number VARCHAR(50),
    slack_channel VARCHAR(255),

    -- Channel enablement flags
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    slack_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Alert filtering
    critical_alerts_only BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes for performance
CREATE INDEX idx_notification_preferences_customer_id ON notification_preferences(customer_id);
CREATE INDEX idx_notification_preferences_email_enabled ON notification_preferences(email_enabled) WHERE email_enabled = TRUE;
CREATE INDEX idx_notification_preferences_sms_enabled ON notification_preferences(sms_enabled) WHERE sms_enabled = TRUE;
CREATE INDEX idx_notification_preferences_slack_enabled ON notification_preferences(slack_enabled) WHERE slack_enabled = TRUE;

-- Trigger for updated_at
CREATE TRIGGER update_notification_preferences_updated_at
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comment for documentation
COMMENT ON TABLE notification_preferences IS 'Customer notification preferences for email, SMS, and Slack channels';
COMMENT ON COLUMN notification_preferences.email IS 'Email address for email notifications';
COMMENT ON COLUMN notification_preferences.phone_number IS 'Phone number for SMS notifications';
COMMENT ON COLUMN notification_preferences.slack_channel IS 'Slack channel or user for notifications';
COMMENT ON COLUMN notification_preferences.email_enabled IS 'Enable email notifications';
COMMENT ON COLUMN notification_preferences.sms_enabled IS 'Enable SMS notifications';
COMMENT ON COLUMN notification_preferences.slack_enabled IS 'Enable Slack notifications';
COMMENT ON COLUMN notification_preferences.critical_alerts_only IS 'Only send notifications for CRITICAL severity alerts';
