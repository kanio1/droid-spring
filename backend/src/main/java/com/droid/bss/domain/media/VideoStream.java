/**
 * Video Stream Entity
 *
 * Represents a video stream with multiple quality options
 * Supports adaptive bitrate streaming (ABR)
 */

package com.droid.bss.domain.media;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "video_streams")
@EntityListeners(AuditingEntityListener.class)
public class VideoStream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "stream_url", nullable = false)
    private String streamUrl;

    @Column(name = "manifest_url", nullable = false)
    private String manifestUrl;

    @Column(name = "drm_enabled", nullable = false)
    private Boolean drmEnabled = false;

    @Column(name = "drm_type")
    private String drmType;

    @Column(name = "drm_license_url")
    private String drmLicenseUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StreamStatus status = StreamStatus.ACTIVE;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StreamQuality> qualities = new ArrayList<>();

    @Column(name = "default_quality")
    private String defaultQuality = "auto";

    @Column(name = "adaptive_bitrate", nullable = false)
    private Boolean adaptiveBitrate = true;

    @Column(name = "buffer_size", nullable = false)
    private Integer bufferSize = 30; // seconds

    @Column(name = "max_buffer_size")
    private Integer maxBufferSize = 60;

    @Column(name = "startup_buffer")
    private Integer startupBuffer = 3;

    @Column(name = "codec")
    private String codec = "H.264";

    @Column(name = "container")
    private String container = "MP4";

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "total_views")
    private Integer totalViews = 0;

    @Column(name = "active_viewers")
    private Integer activeViewers = 0;

    @Column(name = "bandwidth_usage")
    private Long bandwidthUsage = 0L;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public enum StreamStatus {
        ACTIVE,
        INACTIVE,
        PROCESSING,
        ERROR,
        ARCHIVED
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public String getManifestUrl() {
        return manifestUrl;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public Boolean getDrmEnabled() {
        return drmEnabled;
    }

    public void setDrmEnabled(Boolean drmEnabled) {
        this.drmEnabled = drmEnabled;
    }

    public String getDrmType() {
        return drmType;
    }

    public void setDrmType(String drmType) {
        this.drmType = drmType;
    }

    public String getDrmLicenseUrl() {
        return drmLicenseUrl;
    }

    public void setDrmLicenseUrl(String drmLicenseUrl) {
        this.drmLicenseUrl = drmLicenseUrl;
    }

    public StreamStatus getStatus() {
        return status;
    }

    public void setStatus(StreamStatus status) {
        this.status = status;
    }

    public List<StreamQuality> getQualities() {
        return qualities;
    }

    public void setQualities(List<StreamQuality> qualities) {
        this.qualities = qualities;
    }

    public void addQuality(StreamQuality quality) {
        qualities.add(quality);
        quality.setStream(this);
    }

    public void removeQuality(StreamQuality quality) {
        qualities.remove(quality);
        quality.setStream(null);
    }

    public String getDefaultQuality() {
        return defaultQuality;
    }

    public void setDefaultQuality(String defaultQuality) {
        this.defaultQuality = defaultQuality;
    }

    public Boolean getAdaptiveBitrate() {
        return adaptiveBitrate;
    }

    public void setAdaptiveBitrate(Boolean adaptiveBitrate) {
        this.adaptiveBitrate = adaptiveBitrate;
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Integer getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(Integer maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    public Integer getStartupBuffer() {
        return startupBuffer;
    }

    public void setStartupBuffer(Integer startupBuffer) {
        this.startupBuffer = startupBuffer;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Integer totalViews) {
        this.totalViews = totalViews;
    }

    public Integer getActiveViewers() {
        return activeViewers;
    }

    public void setActiveViewers(Integer activeViewers) {
        this.activeViewers = activeViewers;
    }

    public Long getBandwidthUsage() {
        return bandwidthUsage;
    }

    public void setBandwidthUsage(Long bandwidthUsage) {
        this.bandwidthUsage = bandwidthUsage;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    // Business logic methods

    public boolean isActive() {
        return status == StreamStatus.ACTIVE;
    }

    public boolean isDRMEnabled() {
        return drmEnabled != null && drmEnabled;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public void incrementViews() {
        this.totalViews++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void incrementErrorCount() {
        this.errorCount++;
        if (errorCount >= 10) {
            this.status = StreamStatus.ERROR;
        }
    }

    public void addBandwidthUsage(Long bytes) {
        this.bandwidthUsage += bytes;
    }

    public List<StreamQuality> getAvailableQualities() {
        return qualities.stream()
            .filter(q -> q.getStatus() == StreamQuality.QualityStatus.AVAILABLE)
            .toList();
    }

    public StreamQuality getQualityByName(String name) {
        return qualities.stream()
            .filter(q -> q.getQualityName().equals(name))
            .findFirst()
            .orElse(null);
    }

    @Entity
    @Table(name = "stream_qualities")
    @EntityListeners(AuditingEntityListener.class)
    public static class StreamQuality {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private String id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "stream_id", nullable = false)
        private VideoStream stream;

        @Column(name = "quality_name", nullable = false)
        private String qualityName; // 2160p, 1080p, 720p, 480p, 360p

        @Column(name = "resolution_width", nullable = false)
        private Integer resolutionWidth;

        @Column(name = "resolution_height", nullable = false)
        private Integer resolutionHeight;

        @Column(name = "bitrate", nullable = false)
        private Long bitrate; // bits per second

        @Column(name = "framerate", nullable = false)
        private Integer framerate = 30;

        @Column(name = "codec", nullable = false)
        private String codec = "H.264";

        @Column(name = "file_size")
        private Long fileSize;

        @Column(name = "segment_duration", nullable = false)
        private Integer segmentDuration = 6; // seconds

        @Column(name = "segment_count")
        private Integer segmentCount;

        @Column(name = "status")
        @Enumerated(EnumType.STRING)
        private QualityStatus status = QualityStatus.AVAILABLE;

        @Column(name = "view_count", nullable = false)
        private Long viewCount = 0L;

        @Column(name = "created_at", nullable = false, updatable = false)
        @CreatedDate
        private LocalDateTime createdAt;

        public enum QualityStatus {
            AVAILABLE,
            PROCESSING,
            FAILED,
            ARCHIVED
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public VideoStream getStream() {
            return stream;
        }

        public void setStream(VideoStream stream) {
            this.stream = stream;
        }

        public String getQualityName() {
            return qualityName;
        }

        public void setQualityName(String qualityName) {
            this.qualityName = qualityName;
        }

        public Integer getResolutionWidth() {
            return resolutionWidth;
        }

        public void setResolutionWidth(Integer resolutionWidth) {
            this.resolutionWidth = resolutionWidth;
        }

        public Integer getResolutionHeight() {
            return resolutionHeight;
        }

        public void setResolutionHeight(Integer resolutionHeight) {
            this.resolutionHeight = resolutionHeight;
        }

        public Long getBitrate() {
            return bitrate;
        }

        public void setBitrate(Long bitrate) {
            this.bitrate = bitrate;
        }

        public Integer getFramerate() {
            return framerate;
        }

        public void setFramerate(Integer framerate) {
            this.framerate = framerate;
        }

        public String getCodec() {
            return codec;
        }

        public void setCodec(String codec) {
            this.codec = codec;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public Integer getSegmentDuration() {
            return segmentDuration;
        }

        public void setSegmentDuration(Integer segmentDuration) {
            this.segmentDuration = segmentDuration;
        }

        public Integer getSegmentCount() {
            return segmentCount;
        }

        public void setSegmentCount(Integer segmentCount) {
            this.segmentCount = segmentCount;
        }

        public QualityStatus getStatus() {
            return status;
        }

        public void setStatus(QualityStatus status) {
            this.status = status;
        }

        public Long getViewCount() {
            return viewCount;
        }

        public void setViewCount(Long viewCount) {
            this.viewCount = viewCount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public boolean isAvailable() {
            return status == QualityStatus.AVAILABLE;
        }

        public String getResolution() {
            return resolutionWidth + "x" + resolutionHeight;
        }

        public String getFormattedBitrate() {
            double mbps = bitrate / 1000000.0;
            return String.format("%.2f Mbps", mbps);
        }
    }
}
