-- =====================================================
-- Tenant Management Tables
-- Multi-tenancy support for BSS System
-- =====================================================

-- Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    domain VARCHAR(255) NOT NULL UNIQUE,
    contact_email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(50),
    postal_code VARCHAR(10),
    country VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    custom_branding TEXT,
    logo_url VARCHAR(100),
    timezone VARCHAR(50),
    locale VARCHAR(10),
    currency VARCHAR(10),
    industry VARCHAR(50),
    tenant_tier VARCHAR(10),
    max_users INTEGER,
    max_customers INTEGER,
    storage_quota_mb BIGINT,
    api_rate_limit INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Tenant Settings (embedded)
CREATE TABLE tenant_settings (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    allow_self_registration BOOLEAN DEFAULT true,
    require_email_verification BOOLEAN DEFAULT true,
    two_factor_required BOOLEAN DEFAULT false,
    session_timeout_minutes INTEGER DEFAULT 60 CHECK (session_timeout_minutes >= 15 AND session_timeout_minutes <= 1440),
    password_policy_min_length INTEGER DEFAULT 8 CHECK (password_policy_min_length >= 8 AND password_policy_min_length <= 128),
    password_policy_require_uppercase BOOLEAN DEFAULT true,
    password_policy_require_lowercase BOOLEAN DEFAULT true,
    password_policy_require_numbers BOOLEAN DEFAULT true,
    password_policy_require_symbols BOOLEAN DEFAULT true,
    allow_api_access BOOLEAN DEFAULT true,
    api_key_required BOOLEAN DEFAULT false,
    webhook_enabled BOOLEAN DEFAULT true,
    data_retention_days INTEGER DEFAULT 365 CHECK (data_retention_days >= 30 AND data_retention_days <= 3650),
    enable_audit_log BOOLEAN DEFAULT true,
    allow_file_uploads BOOLEAN DEFAULT true,
    max_file_size_mb INTEGER DEFAULT 10 CHECK (max_file_size_mb >= 1 AND max_file_size_mb <= 100),
    allowed_file_types TEXT,
    enable_realtime_notifications BOOLEAN DEFAULT true,
    enable_sso BOOLEAN DEFAULT false,
    sso_provider VARCHAR(50),
    sso_config TEXT,
    custom_css TEXT,
    primary_color VARCHAR(10),
    secondary_color VARCHAR(10),
    suspension_reason TEXT,
    billing_cycle VARCHAR(20),
    trial_days INTEGER DEFAULT 14 CHECK (trial_days >= 0 AND trial_days <= 90),
    grace_period_days INTEGER DEFAULT 7 CHECK (grace_period_days >= 0 AND grace_period_days <= 30),
    overage_rate_per_unit DECIMAL(10,2),
    custom_fields_config TEXT,
    feature_flags TEXT,
    additional_settings TEXT
);

-- User-Tenant junction table
CREATE TABLE user_tenants (
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    invited_by UUID,
    invited_at TIMESTAMP,
    accepted_at TIMESTAMP,
    last_accessed_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tenant_id)
);

-- User-Tenant permissions (element collection)
CREATE TABLE user_tenant_permissions (
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, tenant_id, permission),
    FOREIGN KEY (user_id, tenant_id) REFERENCES user_tenants(user_id, tenant_id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_tier ON tenants(tenant_tier);
CREATE INDEX idx_tenants_name_search ON tenants USING gin(lower(name) gin_trgm_ops);
CREATE INDEX idx_tenants_code_search ON tenants USING gin(lower(code) gin_trgm_ops);
CREATE INDEX idx_user_tenants_user_id ON user_tenants(user_id);
CREATE INDEX idx_user_tenants_tenant_id ON user_tenants(tenant_id);
CREATE INDEX idx_user_tenants_active ON user_tenants(is_active) WHERE is_active = true AND deleted_at IS NULL;
CREATE INDEX idx_user_tenants_default ON user_tenants(user_id, is_default) WHERE is_default = true;

-- Row Level Security (RLS) setup for tenant isolation
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_tenant_permissions ENABLE ROW LEVEL SECURITY;

-- RLS Policies will be implemented in application layer
-- This ensures data isolation between tenants

-- Create trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_tenants_updated_at BEFORE UPDATE ON user_tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE tenants IS 'Multi-tenant organization/company entity';
COMMENT ON TABLE tenant_settings IS 'Tenant-specific configuration settings';
COMMENT ON TABLE user_tenants IS 'Many-to-many relationship between users and tenants';
COMMENT ON TABLE user_tenant_permissions IS 'User permissions within specific tenant';
