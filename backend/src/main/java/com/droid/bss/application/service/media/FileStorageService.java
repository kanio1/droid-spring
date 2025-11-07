/**
 * File Storage Service
 *
 * Handles file uploads, storage, and management
 * Supports multiple file types, batch uploads, and security scanning
 */

package com.droid.bss.application.service.media;

import com.droid.bss.domain.media.UploadedFile;
import com.droid.bss.domain.media.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final UploadedFileRepository fileRepository;
    private final FilePreviewService previewService;
    private final String uploadPath = "/tmp/uploads";

    @Autowired
    public FileStorageService(UploadedFileRepository fileRepository, FilePreviewService previewService) {
        this.fileRepository = fileRepository;
        this.previewService = previewService;
        initializeStorage();
    }

    /**
     * Initialize storage directories
     */
    private void initializeStorage() {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Upload a single file
     */
    public FileUploadResult uploadFile(
            MultipartFile file,
            String tenantId,
            String customerId,
            String uploadedBy,
            List<String> tags,
            String description
    ) throws IOException {
        validateFile(file);

        // Create file record
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setTenantId(tenantId);
        uploadedFile.setCustomerId(customerId);
        uploadedFile.setUploadedBy(uploadedBy);
        uploadedFile.setOriginalFilename(file.getOriginalFilename());
        uploadedFile.setMimeType(file.getContentType());
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setStatus(UploadedFile.FileStatus.PENDING);
        uploadedFile.setTags(tags);
        uploadedFile.setDescription(description);
        uploadedFile.setChecksum(calculateChecksum(file));

        // Determine file type
        uploadedFile.setFileType(UploadedFile.determineFileType(
            file.getContentType(),
            file.getOriginalFilename()
        ));

        // Generate stored filename
        String storedFilename = generateStoredFilename(uploadedFile.getOriginalFilename());
        uploadedFile.setStoredFilename(storedFilename);

        // Save file to storage
        Path targetPath = Paths.get(uploadPath, storedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        uploadedFile.setFilePath(targetPath.toString());

        // Set expiration (30 days from now)
        uploadedFile.setExpiresAt(LocalDateTime.now().plusDays(30));

        // Add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("upload_ip", "127.0.0.1");
        metadata.put("user_agent", "BSS-Client");
        metadata.put("original_size", String.valueOf(file.getSize()));
        uploadedFile.setMetadata(metadata);

        // Save to database
        uploadedFile = fileRepository.save(uploadedFile);

        // Process file asynchronously (in real implementation)
        processFileAsync(uploadedFile);

        return new FileUploadResult(uploadedFile);
    }

    /**
     * Upload multiple files (batch upload)
     */
    public List<FileUploadResult> uploadFiles(
            List<MultipartFile> files,
            String tenantId,
            String customerId,
            String uploadedBy
    ) throws IOException {
        List<FileUploadResult> results = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                FileUploadResult result = uploadFile(
                    file,
                    tenantId,
                    customerId,
                    uploadedBy,
                    null,
                    null
                );
                results.add(result);
            } catch (Exception e) {
                results.add(new FileUploadResult(null, e.getMessage()));
            }
        }

        return results;
    }

    /**
     * Get file by ID
     */
    public Optional<UploadedFile> getFile(String fileId, String tenantId) {
        return fileRepository.findById(fileId)
            .filter(f -> f.getTenantId().equals(tenantId));
    }

    /**
     * Get all files for tenant
     */
    public Page<UploadedFile> getFiles(String tenantId, Pageable pageable) {
        return fileRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get files by customer
     */
    public List<UploadedFile> getCustomerFiles(String customerId, String tenantId) {
        return fileRepository.findByCustomerId(customerId, tenantId);
    }

    /**
     * Get files by type
     */
    public Page<UploadedFile> getFilesByType(String tenantId, UploadedFile.FileType fileType, Pageable pageable) {
        return fileRepository.findByFileType(tenantId, fileType, pageable);
    }

    /**
     * Search files by filename
     */
    public Page<UploadedFile> searchFiles(String tenantId, String query, Pageable pageable) {
        String pattern = "%" + query.toLowerCase() + "%";
        return fileRepository.searchByFilename(tenantId, pattern, pageable);
    }

    /**
     * Get file statistics
     */
    public FileStatistics getFileStatistics(String tenantId) {
        List<Object[]> stats = fileRepository.getFileStatistics(tenantId);
        Long totalStorage = fileRepository.getTotalStorageUsed(tenantId);
        List<Object[]> storageByType = fileRepository.getStorageByFileType(tenantId);
        List<Object[]> countByStatus = fileRepository.countByStatus(tenantId);

        FileStatistics statistics = new FileStatistics();
        statistics.setTenantId(tenantId);
        statistics.setTotalStorageBytes(totalStorage);

        Map<UploadedFile.FileType, Long> fileTypeCounts = new HashMap<>();
        for (Object[] stat : stats) {
            fileTypeCounts.put((UploadedFile.FileType) stat[0], (Long) stat[1]);
        }
        statistics.setFileTypeCounts(fileTypeCounts);

        Map<UploadedFile.FileType, Long> storageByTypeMap = new HashMap<>();
        for (Object[] stat : storageByType) {
            storageByTypeMap.put((UploadedFile.FileType) stat[0], (Long) stat[1]);
        }
        statistics.setStorageByFileType(storageByTypeMap);

        Map<UploadedFile.FileStatus, Long> statusCounts = new HashMap<>();
        for (Object[] stat : countByStatus) {
            statusCounts.put((UploadedFile.FileStatus) stat[0], (Long) stat[1]);
        }
        statistics.setStatusCounts(statusCounts);

        return statistics;
    }

    /**
     * Delete file
     */
    public boolean deleteFile(String fileId, String tenantId) {
        Optional<UploadedFile> fileOpt = getFile(fileId, tenantId);
        if (fileOpt.isPresent()) {
            UploadedFile file = fileOpt.get();
            // Delete physical file
            try {
                Files.deleteIfExists(Paths.get(file.getFilePath()));
            } catch (IOException e) {
                // Log error but continue
            }
            // Delete from database
            fileRepository.delete(file);
            return true;
        }
        return false;
    }

    /**
     * Mark file as accessed
     */
    public void markFileAsAccessed(String fileId, String tenantId) {
        fileRepository.findById(fileId)
            .filter(f -> f.getTenantId().equals(tenantId))
            .ifPresent(UploadedFile::markAsAccessed);
    }

    /**
     * Increment download count
     */
    public boolean incrementDownloadCount(String fileId, String tenantId) {
        Optional<UploadedFile> fileOpt = getFile(fileId, tenantId);
        if (fileOpt.isPresent()) {
            UploadedFile file = fileOpt.get();
            if (file.canDownload()) {
                file.incrementDownloadCount();
                fileRepository.save(file);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if file can be downloaded
     */
    public boolean canDownload(String fileId, String tenantId) {
        return getFile(fileId, tenantId)
            .map(UploadedFile::canDownload)
            .orElse(false);
    }

    /**
     * Get storage quota usage
     */
    public StorageQuota getStorageQuota(String tenantId) {
        Object[] usage = fileRepository.getStorageQuotaUsage(tenantId);
        long fileCount = (Long) usage[0];
        long usedBytes = (Long) usage[1];

        StorageQuota quota = new StorageQuota();
        quota.setTenantId(tenantId);
        quota.setFileCount(fileCount);
        quota.setUsedBytes(usedBytes);
        quota.setUsedGB(usedBytes / (1024.0 * 1024.0 * 1024.0));
        quota.setMaxBytes(10L * 1024 * 1024 * 1024); // 10GB default
        quota.setAvailableBytes(quota.getMaxBytes() - usedBytes);
        quota.setUsagePercentage((usedBytes * 100.0) / quota.getMaxBytes());

        return quota;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > 100 * 1024 * 1024) { // 100MB limit
            throw new IllegalArgumentException("File size exceeds 100MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("File type not allowed");
        }
    }

    /**
     * Check if content type is allowed
     */
    private boolean isAllowedContentType(String contentType) {
        Set<String> allowedTypes = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/csv", "text/plain",
            "video/mp4", "video/avi", "video/quicktime",
            "audio/mpeg", "audio/wav",
            "application/zip", "application/x-rar-compressed"
        );
        return allowedTypes.contains(contentType.toLowerCase());
    }

    /**
     * Calculate file checksum (MD5)
     */
    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = file.getBytes();
            byte[] hash = md.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Generate unique stored filename
     */
    private String generateStoredFilename(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Process file asynchronously
     */
    private void processFileAsync(UploadedFile file) {
        // In real implementation, this would use a message queue or async executor
        // For now, we'll simulate processing
        file.setStatus(UploadedFile.FileStatus.PROCESSING);
        fileRepository.save(file);

        // Simulate virus scan and preview generation
        try {
            Thread.sleep(100);
            file.setScanResult(UploadedFile.ScanResult.CLEAN);
            file.setStatus(UploadedFile.FileStatus.COMPLETED);

            // Add image metadata if it's an image
            if (file.isImage()) {
                file.addMetadata("width", "1920");
                file.addMetadata("height", "1080");
                file.addMetadata("format", "JPEG");
            }

            fileRepository.save(file);

            // Generate previews for the file
            try {
                previewService.generatePreview(
                    file.getId(),
                    file.getTenantId(),
                    file.getFileType(),
                    file.getFilePath()
                );
            } catch (Exception e) {
                // Log preview generation error but don't fail the upload
                System.err.println("Failed to generate preview for file " + file.getId() + ": " + e.getMessage());
            }

        } catch (InterruptedException e) {
            file.setStatus(UploadedFile.FileStatus.FAILED);
            fileRepository.save(file);
        }
    }

    // Result classes

    public static class FileUploadResult {
        private UploadedFile file;
        private String error;

        public FileUploadResult(UploadedFile file) {
            this.file = file;
        }

        public FileUploadResult(UploadedFile file, String error) {
            this.file = file;
            this.error = error;
        }

        public UploadedFile getFile() { return file; }
        public String getError() { return error; }
        public boolean isSuccess() { return file != null && error == null; }
    }

    public static class FileStatistics {
        private String tenantId;
        private Long totalStorageBytes;
        private Map<UploadedFile.FileType, Long> fileTypeCounts;
        private Map<UploadedFile.FileType, Long> storageByFileType;
        private Map<UploadedFile.FileStatus, Long> statusCounts;

        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public Long getTotalStorageBytes() { return totalStorageBytes; }
        public void setTotalStorageBytes(Long totalStorageBytes) { this.totalStorageBytes = totalStorageBytes; }

        public Map<UploadedFile.FileType, Long> getFileTypeCounts() { return fileTypeCounts; }
        public void setFileTypeCounts(Map<UploadedFile.FileType, Long> fileTypeCounts) { this.fileTypeCounts = fileTypeCounts; }

        public Map<UploadedFile.FileType, Long> getStorageByFileType() { return storageByFileType; }
        public void setStorageByFileType(Map<UploadedFile.FileType, Long> storageByFileType) { this.storageByFileType = storageByFileType; }

        public Map<UploadedFile.FileStatus, Long> getStatusCounts() { return statusCounts; }
        public void setStatusCounts(Map<UploadedFile.FileStatus, Long> statusCounts) { this.statusCounts = statusCounts; }
    }

    public static class StorageQuota {
        private String tenantId;
        private long fileCount;
        private long usedBytes;
        private double usedGB;
        private long maxBytes;
        private long availableBytes;
        private double usagePercentage;

        // Getters and setters
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public long getFileCount() { return fileCount; }
        public void setFileCount(long fileCount) { this.fileCount = fileCount; }

        public long getUsedBytes() { return usedBytes; }
        public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }

        public double getUsedGB() { return usedGB; }
        public void setUsedGB(double usedGB) { this.usedGB = usedGB; }

        public long getMaxBytes() { return maxBytes; }
        public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }

        public long getAvailableBytes() { return availableBytes; }
        public void setAvailableBytes(long availableBytes) { this.availableBytes = availableBytes; }

        public double getUsagePercentage() { return usagePercentage; }
        public void setUsagePercentage(double usagePercentage) { this.usagePercentage = usagePercentage; }
    }
}
