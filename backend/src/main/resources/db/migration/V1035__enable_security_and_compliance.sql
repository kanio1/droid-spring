-- =====================================================
-- Advanced Security & Compliance Setup
-- PostgreSQL 18 Security Features
-- =====================================================

-- Enable necessary extensions for security
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- For encryption functions
CREATE EXTENSION IF NOT EXISTS pg_stat_statements; -- For audit logging
CREATE EXTENSION IF NOT EXISTS btree_gin; -- For audit log indexing

-- =====================================================
-- 1. AUDIT LOGGING SYSTEM
-- =====================================================

-- Create comprehensive audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID,
    user_name VARCHAR(100),
    session_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL, -- INSERT, UPDATE, DELETE, LOGIN, LOGOUT, ACCESS_DENIED
    table_name VARCHAR(100),
    table_schema VARCHAR(50),
    record_id UUID,
    operation VARCHAR(20), -- BEFORE, AFTER
    old_data JSONB,
    new_data JSONB,
    query_text TEXT,
    ip_address INET,
    user_agent TEXT,
    application_name VARCHAR(100),
    client_port INTEGER,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    severity VARCHAR(20) DEFAULT 'INFO', -- INFO, WARNING, ERROR, CRITICAL
    tags JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create audit log indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_log_time ON audit_log(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_table ON audit_log(table_schema, table_name, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_type ON audit_log(event_type, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_success ON audit_log(success, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_severity ON audit_log(severity, event_time DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_gin_tags ON audit_log USING GIN (tags);
CREATE INDEX IF NOT EXISTS idx_audit_log_gin_data ON audit_log USING GIN (old_data, new_data);

-- Create audit log partition by month (for better performance)
-- This will automatically create partitions for the last 12 months and next 12 months
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    i INTEGER;
BEGIN
    start_date := DATE_TRUNC('month', CURRENT_DATE - INTERVAL '12 months');
    end_date := DATE_TRUNC('month', CURRENT_DATE + INTERVAL '12 months');

    FOR i IN 0..24 LOOP
        partition_name := 'audit_log_' || TO_CHAR(start_date + (i * INTERVAL '1 month'), 'YYYY_MM');
        EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_log FOR VALUES FROM (%L) TO (%L)',
                       partition_name,
                       start_date + (i * INTERVAL '1 month'),
                       start_date + ((i + 1) * INTERVAL '1 month'));
    END LOOP;
END $$;

-- Create audit log view for easy querying
CREATE OR REPLACE VIEW audit_log_summary AS
SELECT
    event_type,
    table_name,
    COUNT(*) as event_count,
    COUNT(CASE WHEN success = TRUE THEN 1 END) as success_count,
    COUNT(CASE WHEN success = FALSE THEN 1 END) as error_count,
    MIN(event_time) as first_event,
    MAX(event_time) as last_event
FROM audit_log
GROUP BY event_type, table_name;

-- =====================================================
-- 2. ROW LEVEL SECURITY (RLS) SETUP
-- =====================================================

-- Enable RLS on customer tables
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for customers table
DROP POLICY IF EXISTS tenant_isolation_policy ON customers;
CREATE POLICY tenant_isolation_policy ON customers
    USING (
        -- Allow access if user is a super admin
        auth.uid() IN (SELECT id FROM auth.users WHERE raw_user_meta_data->>'role' = 'admin')
        OR
        -- Allow access if user belongs to the same organization
        organization_id IN (
            SELECT uo.organization_id
            FROM user_organizations uo
            WHERE uo.user_id = auth.uid()
        )
        OR
        -- Allow access if user is viewing their own data
        id IN (
            SELECT c.id FROM customers c
            WHERE c.email = (
                SELECT au.email FROM auth.users au WHERE au.id = auth.uid()
            )
        )
    );

-- Create RLS policies for invoices table
DROP POLICY IF EXISTS invoice_tenant_policy ON invoices;
CREATE POLICY invoice_tenant_policy ON invoices
    USING (
        auth.uid() IN (SELECT id FROM auth.users WHERE raw_user_meta_data->>'role' = 'admin')
        OR
        customer_id IN (
            SELECT c.id FROM customers c
            JOIN user_organizations uo ON c.organization_id = uo.organization_id
            WHERE uo.user_id = auth.uid()
        )
    );

-- Create RLS policies for payments table
DROP POLICY IF EXISTS payment_tenant_policy ON payments;
CREATE POLICY payment_tenant_policy ON payments
    USING (
        auth.uid() IN (SELECT id FROM auth.users WHERE raw_user_meta_data->>'role' = 'admin')
        OR
        customer_id IN (
            SELECT c.id FROM customers c
            JOIN user_organizations uo ON c.organization_id = uo.organization_id
            WHERE uo.user_id = auth.uid()
        )
    );

-- =====================================================
-- 3. COLUMN-LEVEL ENCRYPTION
-- =====================================================

-- Create encryption key management table
CREATE TABLE IF NOT EXISTS encryption_keys (
    key_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_name VARCHAR(100) NOT NULL UNIQUE,
    key_version INTEGER NOT NULL DEFAULT 1,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'aes-256-cbc',
    key_material TEXT NOT NULL, -- In production, this should be encrypted or stored in HSM
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    rotated_at TIMESTAMPTZ,
    created_by UUID
);

-- Insert default encryption keys
INSERT INTO encryption_keys (key_name, key_material) VALUES
    ('customer_pii', 'customer_pii_key_material_32_chars_long'),
    ('payment_data', 'payment_data_key_material_32_char'),
    ('invoice_details', 'invoice_details_key_material_32_ch')
ON CONFLICT (key_name) DO NOTHING;

-- Create PII data classification table
CREATE TABLE IF NOT EXISTS data_classification (
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    classification_level VARCHAR(20) NOT NULL, -- PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
    pii_type VARCHAR(50), -- EMAIL, PHONE, SSN, CREDIT_CARD, etc.
    encrypted BOOLEAN DEFAULT FALSE,
    encrypted_with_key_id UUID REFERENCES encryption_keys(key_id),
    masked BOOLEAN DEFAULT FALSE,
    retention_period INTERVAL,
    compliance_tags TEXT[], -- GDPR, PCI-DSS, HIPAA, etc.
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (table_name, column_name)
);

-- Insert data classification metadata
INSERT INTO data_classification (table_name, column_name, classification_level, pii_type, encrypted, encrypted_with_key_id) VALUES
    ('customers', 'email', 'CONFIDENTIAL', 'EMAIL', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'customer_pii' LIMIT 1)),
    ('customers', 'phone', 'CONFIDENTIAL', 'PHONE', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'customer_pii' LIMIT 1)),
    ('customers', 'ssn', 'RESTRICTED', 'SSN', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'customer_pii' LIMIT 1)),
    ('customers', 'address', 'CONFIDENTIAL', 'ADDRESS', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'customer_pii' LIMIT 1)),
    ('payments', 'credit_card_number', 'RESTRICTED', 'CREDIT_CARD', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'payment_data' LIMIT 1)),
    ('payments', 'cvv', 'RESTRICTED', 'CREDIT_CARD', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'payment_data' LIMIT 1)),
    ('payments', 'bank_account', 'RESTRICTED', 'BANK_ACCOUNT', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'payment_data' LIMIT 1)),
    ('invoices', 'tax_id', 'CONFIDENTIAL', 'TAX_ID', TRUE, (SELECT key_id FROM encryption_keys WHERE key_name = 'invoice_details' LIMIT 1))
ON CONFLICT (table_name, column_name) DO NOTHING;

-- Create function to encrypt sensitive data
CREATE OR REPLACE FUNCTION encrypt_column_data(
    table_name TEXT,
    column_name TEXT,
    value TEXT
) RETURNS TEXT AS $$
DECLARE
    key_material TEXT;
    encrypted_value TEXT;
BEGIN
    -- Get encryption key
    SELECT ek.key_material INTO key_material
    FROM data_classification dc
    JOIN encryption_keys ek ON dc.encrypted_with_key_id = ek.key_id
    WHERE dc.table_name = encrypt_column_data.table_name
    AND dc.column_name = encrypt_column_data.column_name
    AND dc.encrypted = TRUE
    AND ek.is_active = TRUE
    LIMIT 1;

    -- If no encryption key found, return original value
    IF key_material IS NULL THEN
        RETURN value;
    END IF;

    -- Encrypt using pgcrypto
    encrypted_value := encode(
        encrypt(value::bytea, key_material, 'aes'),
        'base64'
    );

    RETURN encrypted_value;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create function to decrypt sensitive data
CREATE OR REPLACE FUNCTION decrypt_column_data(
    table_name TEXT,
    column_name TEXT,
    encrypted_value TEXT
) RETURNS TEXT AS $$
DECLARE
    key_material TEXT;
    decrypted_value TEXT;
BEGIN
    -- Get encryption key
    SELECT ek.key_material INTO key_material
    FROM data_classification dc
    JOIN encryption_keys ek ON dc.encrypted_with_key_id = ek.key_id
    WHERE dc.table_name = decrypt_column_data.table_name
    AND dc.column_name = decrypt_column_data.column_name
    AND dc.encrypted = TRUE
    AND ek.is_active = TRUE
    LIMIT 1;

    -- If no encryption key found, return original value
    IF key_material IS NULL THEN
        RETURN encrypted_value;
    END IF;

    -- Decrypt using pgcrypto
    BEGIN
        decrypted_value := convert_from(
            decrypt(decode(encrypted_value, 'base64'), key_material, 'aes'),
            'UTF-8'
        );
    EXCEPTION WHEN OTHERS THEN
        -- If decryption fails, return encrypted value
        RETURN encrypted_value;
    END;

    RETURN decrypted_value;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create function to mask sensitive data for display
CREATE OR REPLACE FUNCTION mask_sensitive_data(
    value TEXT,
    pii_type TEXT,
    classification_level TEXT
) RETURNS TEXT AS $$
DECLARE
    masked_value TEXT;
BEGIN
    -- Don't mask public or internal data
    IF classification_level IN ('PUBLIC', 'INTERNAL') THEN
        RETURN value;
    END IF;

    -- Don't mask if value is already masked
    IF value ~ '^\*+\d{4}$' THEN
        RETURN value;
    END IF;

    -- Mask based on PII type
    CASE pii_type
        WHEN 'EMAIL' THEN
            masked_value := regexp_replace(value, '(.*)@.*', '\1@***');
        WHEN 'PHONE' THEN
            masked_value := regexp_replace(value, '(\d{3})\d{3}(\d{4})', '\1-***-\2');
        WHEN 'SSN' THEN
            masked_value := regexp_replace(value, '(\d{3})\d{2}(\d{4})', '\1-**-\2');
        WHEN 'CREDIT_CARD' THEN
            masked_value := regexp_replace(value, '(\d{4})\d+(\d{4})', '\1 **** **** \2');
        WHEN 'BANK_ACCOUNT' THEN
            masked_value := regexp_replace(value, '(\d{4})\d+(\d{4})', '\1 **** \2');
        ELSE
            -- Generic masking - show first and last 2 characters
            IF length(value) <= 4 THEN
                masked_value := repeat('*', length(value));
            ELSE
                masked_value := substr(value, 1, 2) || repeat('*', length(value) - 4) || substr(value, -2);
            END IF;
    END CASE;

    RETURN masked_value;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 4. SECURITY AUDIT TRIGGERS
-- =====================================================

-- Function to log audit events
CREATE OR REPLACE FUNCTION log_audit_event(
    p_user_id UUID,
    p_event_type VARCHAR(50),
    p_table_name VARCHAR(100),
    p_table_schema VARCHAR(50) DEFAULT 'public',
    p_record_id UUID DEFAULT NULL,
    p_operation VARCHAR(20) DEFAULT 'AFTER',
    p_old_data JSONB DEFAULT NULL,
    p_new_data JSONB DEFAULT NULL,
    p_query_text TEXT DEFAULT NULL,
    p_ip_address INET DEFAULT NULL,
    p_user_agent TEXT DEFAULT NULL,
    p_success BOOLEAN DEFAULT TRUE,
    p_error_message TEXT DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    audit_id UUID;
BEGIN
    INSERT INTO audit_log (
        user_id,
        event_type,
        table_name,
        table_schema,
        record_id,
        operation,
        old_data,
        new_data,
        query_text,
        ip_address,
        user_agent,
        success,
        error_message
    ) VALUES (
        p_user_id,
        p_event_type,
        p_table_name,
        p_table_schema,
        p_record_id,
        p_operation,
        p_old_data,
        p_new_data,
        p_query_text,
        p_ip_address,
        p_user_agent,
        p_success,
        p_error_message
    ) RETURNING audit_id INTO audit_id;

    RETURN audit_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    user_id UUID;
    old_data JSONB;
    new_data JSONB;
BEGIN
    -- Get current user ID from session context
    -- This assumes you're using an authentication system that sets the user ID
    user_id := current_setting('app.current_user_id', TRUE)::UUID;

    -- Convert OLD and NEW to JSONB
    old_data := row_to_json(OLD);
    new_data := row_to_json(NEW);

    -- Log the event
    IF TG_OP = 'INSERT' THEN
        PERFORM log_audit_event(
            user_id,
            'INSERT',
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW.id,
            'AFTER',
            NULL,
            new_data,
            current_query()
        );
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        PERFORM log_audit_event(
            user_id,
            'UPDATE',
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            NEW.id,
            'AFTER',
            old_data,
            new_data,
            current_query()
        );
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM log_audit_event(
            user_id,
            'DELETE',
            TG_TABLE_NAME,
            TG_TABLE_SCHEMA,
            OLD.id,
            'AFTER',
            old_data,
            NULL,
            current_query()
        );
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create audit triggers on sensitive tables
DROP TRIGGER IF EXISTS audit_customers ON customers;
CREATE TRIGGER audit_customers
    AFTER INSERT OR UPDATE OR DELETE ON customers
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

DROP TRIGGER IF EXISTS audit_invoices ON invoices;
CREATE TRIGGER audit_invoices
    AFTER INSERT OR UPDATE OR DELETE ON invoices
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

DROP TRIGGER IF EXISTS audit_payments ON payments;
CREATE TRIGGER audit_payments
    AFTER INSERT OR UPDATE OR DELETE ON payments
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- =====================================================
-- 5. COMPLIANCE REPORTS
-- =====================================================

-- Create compliance report view
CREATE OR REPLACE VIEW compliance_summary AS
SELECT
    'RLS_POLICIES' as check_type,
    COUNT(*) as total_checks,
    COUNT(CASE WHEN enabled = TRUE THEN 1 END) as passed,
    COUNT(CASE WHEN enabled = FALSE THEN 1 END) as failed
FROM (
    SELECT schemaname, tablename, rowsecurity as enabled
    FROM pg_tables
    WHERE schemaname = 'public'
) rls_status

UNION ALL

SELECT
    'ENCRYPTED_COLUMNS' as check_type,
    COUNT(*) as total_checks,
    COUNT(CASE WHEN encrypted = TRUE THEN 1 END) as passed,
    COUNT(CASE WHEN encrypted = FALSE THEN 1 END) as failed
FROM data_classification

UNION ALL

SELECT
    'AUDIT_LOG_COVERAGE' as check_type,
    COUNT(DISTINCT table_name) as total_checks,
    COUNT(CASE WHEN last_audit IS NOT NULL THEN 1 END) as passed,
    COUNT(CASE WHEN last_audit IS NULL THEN 1 END) as failed
FROM (
    SELECT
        t.table_name,
        MAX(a.event_time) as last_audit
    FROM information_schema.tables t
    LEFT JOIN audit_log a ON a.table_name = t.table_name
    WHERE t.table_schema = 'public'
    AND t.table_type = 'BASE TABLE'
    GROUP BY t.table_name
) audit_coverage;

-- Create security metrics view
CREATE OR REPLACE VIEW security_metrics AS
SELECT
    -- Encryption coverage
    (SELECT COUNT(*) FROM data_classification WHERE encrypted = TRUE) as encrypted_columns,
    (SELECT COUNT(*) FROM data_classification) as total_classified_columns,
    ROUND(
        (SELECT COUNT(*)::float FROM data_classification WHERE encrypted = TRUE) /
        NULLIF((SELECT COUNT(*) FROM data_classification), 0) * 100,
        2
    ) as encryption_coverage_percent,

    -- Audit log volume
    (SELECT COUNT(*) FROM audit_log WHERE event_time >= NOW() - INTERVAL '24 hours') as events_last_24h,
    (SELECT COUNT(*) FROM audit_log WHERE event_time >= NOW() - INTERVAL '7 days') as events_last_7d,
    (SELECT COUNT(*) FROM audit_log WHERE success = FALSE AND event_time >= NOW() - INTERVAL '24 hours') as failed_events_24h,

    -- RLS status
    (SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = TRUE) as rls_enabled_tables,
    (SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public') as total_tables,
    ROUND(
        (SELECT COUNT(*)::float FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = TRUE) /
        NULLIF((SELECT COUNT(*) FROM pg_tables WHERE schemaname = 'public'), 0) * 100,
        2
    ) as rls_coverage_percent;

-- =====================================================
-- 6. DATA RETENTION POLICIES
-- =====================================================

-- Create data retention policy table
CREATE TABLE IF NOT EXISTS data_retention_policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100),
    retention_period INTERVAL NOT NULL,
    action VARCHAR(20) NOT NULL, -- DELETE, ARCHIVE, ANONYMIZE
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(table_name, column_name)
);

-- Insert default retention policies
INSERT INTO data_retention_policies (table_name, column_name, retention_period, action) VALUES
    ('audit_log', NULL, INTERVAL '7 years', 'ARCHIVE'),
    ('login_sessions', NULL, INTERVAL '1 year', 'DELETE'),
    ('password_resets', NULL, INTERVAL '30 days', 'DELETE'),
    ('customers', 'email', INTERVAL '7 years after deletion', 'ANONYMIZE')
ON CONFLICT (table_name, column_name) DO NOTHING;

-- Create function to clean up old data
CREATE OR REPLACE FUNCTION cleanup_expired_data()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    policy RECORD;
    deleted_count INTEGER;
BEGIN
    FOR policy IN
        SELECT * FROM data_retention_policies
    LOOP
        IF policy.table_name = 'audit_log' THEN
            EXECUTE format('DELETE FROM %I WHERE event_time < NOW() - $1',
                          policy.table_name)
            USING policy.retention_period;

            GET DIAGNOSTICS deleted_count = ROW_COUNT;
            RETURN QUERY SELECT 'Deleted ' || deleted_count::TEXT || ' old audit log entries';
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 7. PERMISSIONS & ACCESS CONTROL
-- =====================================================

-- Create role-based access control tables
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by UUID,
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_name VARCHAR(50) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50), -- TABLE, VIEW, FUNCTION
    resource_name VARCHAR(100),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_name, permission_name, resource_name)
);

-- Insert default roles and permissions
INSERT INTO role_permissions (role_name, permission_name, resource_type, resource_name) VALUES
    ('admin', 'ALL', 'ALL', '*'),
    (' auditor', 'READ', 'TABLE', 'audit_log'),
    ('auditor', 'READ', 'VIEW', 'compliance_summary'),
    ('user', 'SELECT', 'TABLE', 'customers'),
    ('user', 'UPDATE', 'TABLE', 'customers')
ON CONFLICT (role_name, permission_name, resource_name) DO NOTHING;

-- =====================================================
-- 8. INDEXES FOR PERFORMANCE
-- =====================================================

-- Create additional indexes for security tables
CREATE INDEX IF NOT EXISTS idx_encryption_keys_active ON encryption_keys(is_active, key_name);
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_name);
CREATE INDEX IF NOT EXISTS idx_data_retention_table ON data_retention_policies(table_name);

-- Grant necessary permissions
GRANT USAGE ON SCHEMA public TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL ON ALL VIEWS IN SCHEMA public TO postgres;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO postgres;

-- Comments for documentation
COMMENT ON TABLE audit_log IS 'Comprehensive audit trail for all database operations';
COMMENT ON TABLE encryption_keys IS 'Encryption key management for column-level encryption';
COMMENT ON TABLE data_classification IS 'PII data classification and compliance metadata';
COMMENT ON TABLE data_retention_policies IS 'Data retention and lifecycle management policies';
COMMENT ON TABLE user_roles IS 'Role-based access control';
COMMENT ON VIEW compliance_summary IS 'Compliance status summary';
COMMENT ON VIEW security_metrics IS 'Security posture metrics and KPIs';
