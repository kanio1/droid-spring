/**
 * File Preview Entity
 *
 * Represents a file preview/thumbnail for uploaded files
 * Supports images, videos, and documents
 */

package com.droid.bss.domain.media;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_previews")
@EntityListeners(AuditingEntityListener.class)
public class FilePreview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "file_id", nullable = false)
    private String fileId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "preview_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PreviewType previewType;

    @Column(name = "preview_path", nullable = false)
    private String previewPath;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration")
    private Integer duration; // For videos in seconds

    @Column(name = "page_count")
    private Integer pageCount; // For documents

    @Column(name = "thumbnail_size", nullable = false)
    @Enumerated(EnumType.STRING)
    private ThumbnailSize thumbnailSize;

    @Column(name = "quality")
    private Integer quality; // 1-100

    @Column(name = "format", nullable = false)
    private String format; // jpg, png, webp, etc.

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PreviewStatus status = PreviewStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum PreviewType {
        THUMBNAIL,
        PREVIEW_IMAGE,
        DOCUMENT_PAGE,
        VIDEO_FRAME,
        AUDIO_WAVEFORM
    }

    public enum ThumbnailSize {
        SMALL,   // 150x150
        MEDIUM,  // 300x300
        LARGE,   // 600x600
        ORIGINAL
    }

    public enum PreviewStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CACHED
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public PreviewType getPreviewType() {
        return previewType;
    }

    public void setPreviewType(PreviewType previewType) {
        this.previewType = previewType;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public ThumbnailSize getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(ThumbnailSize thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public PreviewStatus getStatus() {
        return status;
    }

    public void setStatus(PreviewStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    // Business logic methods

    public boolean isCompleted() {
        return status == PreviewStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PreviewStatus.FAILED;
    }

    public boolean isPending() {
        return status == PreviewStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == PreviewStatus.PROCESSING;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public String getDimensions() {
        if (width != null && height != null) {
            return width + "x" + height;
        }
        return null;
    }

    public void markAsCompleted() {
        this.status = PreviewStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = PreviewStatus.FAILED;
        this.errorMessage = error;
        this.processedAt = LocalDateTime.now();
    }
}
