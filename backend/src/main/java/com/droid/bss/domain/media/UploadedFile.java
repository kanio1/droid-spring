/**
 * Uploaded File Entity
 *
 * Represents a file uploaded to the system
 * Supports multiple file types, sizes, and metadata
 */

package com.droid.bss.domain.media;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
@EntityListeners(AuditingEntityListener.class)
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @ElementCollection
    @CollectionTable(
        name = "file_metadata",
        joinColumns = {
            @JoinColumn(name = "file_id"),
            @JoinColumn(name = "tenant_id")
        }
    )
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 500)
    private java.util.Map<String, String> metadata;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private FileStatus status = FileStatus.PENDING;

    @Column(name = "scan_result")
    @Enumerated(EnumType.STRING)
    private ScanResult scanResult = ScanResult.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "max_downloads")
    private Integer maxDownloads = 100;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(name = "encryption_method")
    private String encryptionMethod;

    @Column(name = "access_level")
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel = AccessLevel.PUBLIC;

    @ElementCollection
    @CollectionTable(
        name = "file_tags",
        joinColumns = {
            @JoinColumn(name = "file_id"),
            @JoinColumn(name = "tenant_id")
        }
    )
    private List<String> tags;

    @Column(name = "description")
    private String description;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public enum FileType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        PDF,
        SPREADSHEET,
        PRESENTATION,
        ARCHIVE,
        CODE,
        DATA,
        OTHER
    }

    public enum FileStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        DELETED
    }

    public enum ScanResult {
        PENDING,
        CLEAN,
        INFECTED,
        QUARANTINED,
        ERROR
    }

    public enum AccessLevel {
        PUBLIC,
        PRIVATE,
        RESTRICTED,
        CONFIDENTIAL
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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public java.util.Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Boolean getIsEncrypted() {
        return isEncrypted;
    }

    public void setIsEncrypted(Boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public String getEncryptionMethod() {
        return encryptionMethod;
    }

    public void setEncryptionMethod(String encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
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

    public String getExtension() {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        return lastDotIndex > 0 ? originalFilename.substring(lastDotIndex + 1) : "";
    }

    public String getFilenameWithoutExtension() {
        int lastDotIndex = originalFilename.lastIndexOf('.');
        return lastDotIndex > 0 ? originalFilename.substring(0, lastDotIndex) : originalFilename;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public boolean isImage() {
        return fileType == FileType.IMAGE;
    }

    public boolean isVideo() {
        return fileType == FileType.VIDEO;
    }

    public boolean isAudio() {
        return fileType == FileType.AUDIO;
    }

    public boolean isDocument() {
        return fileType == FileType.DOCUMENT || fileType == FileType.PDF;
    }

    public boolean isCompleted() {
        return status == FileStatus.COMPLETED;
    }

    public boolean isPending() {
        return status == FileStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == FileStatus.PROCESSING;
    }

    public boolean isFailed() {
        return status == FileStatus.FAILED;
    }

    public boolean isClean() {
        return scanResult == ScanResult.CLEAN;
    }

    public boolean isInfected() {
        return scanResult == ScanResult.INFECTED;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canDownload() {
        return isCompleted() && isClean() && !isExpired() &&
               (maxDownloads == null || downloadCount < maxDownloads);
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void addMetadata(String key, String value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }

    public void addTag(String tag) {
        if (tags == null) {
            tags = new java.util.ArrayList<>();
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void markAsAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public static FileType determineFileType(String mimeType, String filename) {
        String lowerMimeType = mimeType.toLowerCase();
        String lowerFilename = filename.toLowerCase();

        if (lowerMimeType.startsWith("image/") ||
            lowerFilename.matches(".*\\.(jpg|jpeg|png|gif|bmp|svg|webp)$")) {
            return FileType.IMAGE;
        } else if (lowerMimeType.startsWith("video/") ||
                   lowerFilename.matches(".*\\.(mp4|avi|mkv|mov|wmv|flv|webm)$")) {
            return FileType.VIDEO;
        } else if (lowerMimeType.startsWith("audio/") ||
                   lowerFilename.matches(".*\\.(mp3|wav|flac|aac|ogg)$")) {
            return FileType.AUDIO;
        } else if (lowerMimeType.equals("application/pdf") ||
                   lowerFilename.endsWith(".pdf")) {
            return FileType.PDF;
        } else if (lowerMimeType.contains("spreadsheet") ||
                   lowerFilename.matches(".*\\.(xls|xlsx|csv)$")) {
            return FileType.SPREADSHEET;
        } else if (lowerMimeType.contains("presentation") ||
                   lowerFilename.matches(".*\\.(ppt|pptx)$")) {
            return FileType.PRESENTATION;
        } else if (lowerMimeType.contains("word") ||
                   lowerFilename.matches(".*\\.(doc|docx)$")) {
            return FileType.DOCUMENT;
        } else if (lowerMimeType.contains("zip") ||
                   lowerMimeType.contains("rar") ||
                   lowerFilename.matches(".*\\.(zip|rar|7z|tar|gz)$")) {
            return FileType.ARCHIVE;
        } else if (lowerFilename.matches(".*\\.(java|js|ts|py|rb|cpp|c|html|css)$")) {
            return FileType.CODE;
        } else {
            return FileType.OTHER;
        }
    }
}
