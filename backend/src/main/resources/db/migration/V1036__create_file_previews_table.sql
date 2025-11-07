-- File Previews Table
-- Stores file preview/thumbnail metadata for images, videos, PDFs, and documents

CREATE TABLE file_previews (
    id VARCHAR(36) PRIMARY KEY,
    file_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    preview_type VARCHAR(50) NOT NULL,
    preview_path TEXT NOT NULL,
    width INTEGER,
    height INTEGER,
    duration INTEGER,
    page_count INTEGER,
    thumbnail_size VARCHAR(20) NOT NULL,
    quality INTEGER,
    format VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES uploaded_files(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_file_previews_file_id ON file_previews(file_id);
CREATE INDEX idx_file_previews_tenant_id ON file_previews(tenant_id);
CREATE INDEX idx_file_previews_preview_type ON file_previews(preview_type);
CREATE INDEX idx_file_previews_thumbnail_size ON file_previews(thumbnail_size);
CREATE INDEX idx_file_previews_status ON file_previews(status);
CREATE INDEX idx_file_previews_created_at ON file_previews(created_at);

-- Composite indexes for common queries
CREATE INDEX idx_file_previews_file_type ON file_previews(file_id, preview_type);
CREATE INDEX idx_file_previews_tenant_status ON file_previews(tenant_id, status);

-- Enable Row Level Security (RLS)
ALTER TABLE file_previews ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for file_previews
CREATE POLICY tenant_isolation_file_previews ON file_previews
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Comments for documentation
COMMENT ON TABLE file_previews IS 'File previews and thumbnails for uploaded files';
COMMENT ON COLUMN file_previews.file_id IS 'Reference to uploaded file';
COMMENT ON COLUMN file_previews.tenant_id IS 'Tenant for multi-tenant isolation';
COMMENT ON COLUMN file_previews.preview_type IS 'Type of preview (THUMBNAIL, VIDEO_FRAME, DOCUMENT_PAGE, etc.)';
COMMENT ON COLUMN file_previews.width IS 'Preview width in pixels';
COMMENT ON COLUMN file_previews.height IS 'Preview height in pixels';
COMMENT ON COLUMN file_previews.duration IS 'Duration in seconds (for videos/audio)';
COMMENT ON COLUMN file_previews.page_count IS 'Page count (for documents/PDFs)';
COMMENT ON COLUMN file_previews.thumbnail_size IS 'Thumbnail size category';
COMMENT ON COLUMN file_previews.quality IS 'Quality setting (1-100)';
COMMENT ON COLUMN file_previews.format IS 'Image format (jpg, png, webp)';
COMMENT ON COLUMN file_previews.status IS 'Preview generation status';
