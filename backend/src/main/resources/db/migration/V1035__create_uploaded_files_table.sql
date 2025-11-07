-- Uploaded Files Tables
-- Stores file uploads with metadata, security scanning, and tenant isolation

-- Main uploaded_files table
CREATE TABLE uploaded_files (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255),
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scan_result VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP,
    max_downloads INTEGER,
    download_count INTEGER DEFAULT 0,
    is_encrypted BOOLEAN DEFAULT false,
    encryption_method VARCHAR(100),
    access_level VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    description TEXT,
    uploaded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_uploaded_files_tenant_id ON uploaded_files(tenant_id);
CREATE INDEX idx_uploaded_files_customer_id ON uploaded_files(customer_id);
CREATE INDEX idx_uploaded_files_file_type ON uploaded_files(file_type);
CREATE INDEX idx_uploaded_files_status ON uploaded_files(status);
CREATE INDEX idx_uploaded_files_scan_result ON uploaded_files(scan_result);
CREATE INDEX idx_uploaded_files_uploaded_by ON uploaded_files(uploaded_by);
CREATE INDEX idx_uploaded_files_created_at ON uploaded_files(created_at);
CREATE INDEX idx_uploaded_files_expires_at ON uploaded_files(expires_at);
CREATE INDEX idx_uploaded_files_checksum ON uploaded_files(checksum);

-- Composite indexes for common queries
CREATE INDEX idx_uploaded_files_tenant_type ON uploaded_files(tenant_id, file_type);
CREATE INDEX idx_uploaded_files_tenant_status ON uploaded_files(tenant_id, status);
CREATE INDEX idx_uploaded_files_customer_created ON uploaded_files(customer_id, created_at);

-- File metadata (key-value pairs)
CREATE TABLE file_metadata (
    file_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    metadata_key VARCHAR(255) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (file_id, metadata_key, tenant_id),
    FOREIGN KEY (file_id) REFERENCES uploaded_files(id) ON DELETE CASCADE
);

CREATE INDEX idx_file_metadata_file_id ON file_metadata(file_id);
CREATE INDEX idx_file_metadata_tenant_id ON file_metadata(tenant_id);

-- File tags
CREATE TABLE file_tags (
    file_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (file_id, tag, tenant_id),
    FOREIGN KEY (file_id) REFERENCES uploaded_files(id) ON DELETE CASCADE
);

CREATE INDEX idx_file_tags_file_id ON file_tags(file_id);
CREATE INDEX idx_file_tags_tenant_id ON file_tags(tenant_id);
CREATE INDEX idx_file_tags_tag ON file_tags(tag);

-- Enable Row Level Security (RLS)
ALTER TABLE uploaded_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_metadata ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_tags ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for uploaded_files
CREATE POLICY tenant_isolation_uploaded_files ON uploaded_files
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Create RLS policies for file_metadata
CREATE POLICY tenant_isolation_file_metadata ON file_metadata
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Create RLS policies for file_tags
CREATE POLICY tenant_isolation_file_tags ON file_tags
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Comments for documentation
COMMENT ON TABLE uploaded_files IS 'Uploaded files with metadata, security scanning, and access control';
COMMENT ON COLUMN uploaded_files.id IS 'Unique file identifier';
COMMENT ON COLUMN uploaded_files.tenant_id IS 'Tenant for multi-tenant isolation';
COMMENT ON COLUMN uploaded_files.customer_id IS 'Associated customer (optional)';
COMMENT ON COLUMN uploaded_files.original_filename IS 'Original filename as uploaded';
COMMENT ON COLUMN uploaded_files.stored_filename IS 'Unique stored filename';
COMMENT ON COLUMN uploaded_files.checksum IS 'MD5 checksum for duplicate detection';
COMMENT ON COLUMN uploaded_files.status IS 'File status (PENDING, PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN uploaded_files.scan_result IS 'Virus scan result (PENDING, CLEAN, INFECTED)';
COMMENT ON COLUMN uploaded_files.access_level IS 'Access control level (PUBLIC, PRIVATE, RESTRICTED)';

COMMENT ON TABLE file_metadata IS 'Key-value metadata associated with files';
COMMENT ON TABLE file_tags IS 'Tags for categorizing and searching files';
