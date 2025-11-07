-- Create users table for user management
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    status INTEGER NOT NULL DEFAULT 0, -- 0=PENDING_VERIFICATION, 1=ACTIVE, 2=INACTIVE, 3=SUSPENDED, 4=TERMINATED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

-- Create index for faster lookups
CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Create user_roles collection table for role assignments
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, role_name)
);

-- Create index for role lookups
CREATE INDEX idx_user_roles_role_name ON user_roles(role_name);

-- Add comments
COMMENT ON TABLE users IS 'User management table - synchronized with Keycloak';
COMMENT ON TABLE user_roles IS 'User role assignments - synchronized with Keycloak';
COMMENT ON COLUMN users.keycloak_id IS 'Corresponding user ID in Keycloak';
COMMENT ON COLUMN users.status IS 'User status: 0=Pending, 1=Active, 2=Inactive, 3=Suspended, 4=Terminated';
