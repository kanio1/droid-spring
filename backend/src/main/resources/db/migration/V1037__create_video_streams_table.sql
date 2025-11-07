-- Video Streams Table
-- Stores video streaming configuration with adaptive bitrate and quality levels

-- Main video_streams table
CREATE TABLE video_streams (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    stream_url TEXT NOT NULL,
    manifest_url TEXT NOT NULL,
    drm_enabled BOOLEAN NOT NULL DEFAULT false,
    drm_type VARCHAR(50),
    drm_license_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    default_quality VARCHAR(20) NOT NULL DEFAULT 'auto',
    adaptive_bitrate BOOLEAN NOT NULL DEFAULT true,
    buffer_size INTEGER NOT NULL DEFAULT 30,
    max_buffer_size INTEGER,
    startup_buffer INTEGER,
    codec VARCHAR(20) NOT NULL DEFAULT 'H.264',
    container VARCHAR(10) NOT NULL DEFAULT 'MP4',
    duration INTEGER NOT NULL,
    total_views INTEGER NOT NULL DEFAULT 0,
    active_viewers INTEGER NOT NULL DEFAULT 0,
    bandwidth_usage BIGINT NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES uploaded_files(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_video_streams_tenant_id ON video_streams(tenant_id);
CREATE INDEX idx_video_streams_file_id ON video_streams(file_id);
CREATE INDEX idx_video_streams_status ON video_streams(status);
CREATE INDEX idx_video_streams_created_at ON video_streams(created_at);
CREATE INDEX idx_video_streams_active_viewers ON video_streams(active_viewers);

-- Composite indexes for common queries
CREATE INDEX idx_video_streams_tenant_status ON video_streams(tenant_id, status);

-- Stream qualities table (one-to-many relationship)
CREATE TABLE stream_qualities (
    id VARCHAR(36) PRIMARY KEY,
    stream_id VARCHAR(36) NOT NULL,
    quality_name VARCHAR(20) NOT NULL,
    resolution_width INTEGER NOT NULL,
    resolution_height INTEGER NOT NULL,
    bitrate BIGINT NOT NULL,
    framerate INTEGER NOT NULL DEFAULT 30,
    codec VARCHAR(20) NOT NULL DEFAULT 'H.264',
    file_size BIGINT,
    segment_duration INTEGER NOT NULL DEFAULT 6,
    segment_count INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (stream_id) REFERENCES video_streams(id) ON DELETE CASCADE
);

-- Create indexes for stream_qualities
CREATE INDEX idx_stream_qualities_stream_id ON stream_qualities(stream_id);
CREATE INDEX idx_stream_qualities_quality_name ON stream_qualities(quality_name);
CREATE INDEX idx_stream_qualities_status ON stream_qualities(status);
CREATE INDEX idx_stream_qualities_view_count ON stream_qualities(view_count);

-- Composite indexes
CREATE INDEX idx_stream_qualities_stream_status ON stream_qualities(stream_id, status);
CREATE INDEX idx_stream_qualities_stream_quality ON stream_qualities(stream_id, quality_name);

-- Enable Row Level Security (RLS)
ALTER TABLE video_streams ENABLE ROW LEVEL SECURITY;
ALTER TABLE stream_qualities ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for video_streams
CREATE POLICY tenant_isolation_video_streams ON video_streams
    USING (tenant_id = current_setting('app.current_tenant_id', true))
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true));

-- Create RLS policies for stream_qualities
CREATE POLICY tenant_isolation_stream_qualities ON stream_qualities
    USING (stream_id IN (SELECT id FROM video_streams WHERE tenant_id = current_setting('app.current_tenant_id', true)))
    WITH CHECK (stream_id IN (SELECT id FROM video_streams WHERE tenant_id = current_setting('app.current_tenant_id', true)));

-- Comments for documentation
COMMENT ON TABLE video_streams IS 'Video streaming configuration with ABR support';
COMMENT ON COLUMN video_streams.id IS 'Unique stream identifier';
COMMENT ON COLUMN video_streams.tenant_id IS 'Tenant for multi-tenant isolation';
COMMENT ON COLUMN video_streams.file_id IS 'Reference to uploaded file';
COMMENT ON COLUMN video_streams.stream_url IS 'Direct stream URL';
COMMENT ON COLUMN video_streams.manifest_url IS 'HLS/DASH manifest URL';
COMMENT ON COLUMN video_streams.drm_enabled IS 'Whether DRM is enabled';
COMMENT ON COLUMN video_streams.adaptive_bitrate IS 'Whether ABR is enabled';
COMMENT ON COLUMN video_streams.buffer_size IS 'Target buffer size in seconds';
COMMENT ON COLUMN video_streams.default_quality IS 'Default quality (auto, 1080p, etc.)';

COMMENT ON TABLE stream_qualities IS 'Available quality levels for a video stream';
COMMENT ON COLUMN stream_qualities.quality_name IS 'Quality label (2160p, 1080p, 720p, etc.)';
COMMENT ON COLUMN stream_qualities.resolution_width IS 'Video width in pixels';
COMMENT ON COLUMN stream_qualities.resolution_height IS 'Video height in pixels';
COMMENT ON COLUMN stream_qualities.bitrate IS 'Bitrate in bits per second';
COMMENT ON COLUMN stream_qualities.segment_duration IS 'Segment duration in seconds';
COMMENT ON COLUMN stream_qualities.view_count IS 'Number of times this quality was selected';
