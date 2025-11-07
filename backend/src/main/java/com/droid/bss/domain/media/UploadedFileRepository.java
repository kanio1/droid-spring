/**
 * Uploaded File Repository
 *
 * Data access layer for uploaded files
 */

package com.droid.bss.domain.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, String> {

    /**
     * Find files by tenant
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId ORDER BY f.createdAt DESC")
    Page<UploadedFile> findByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find files by customer
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.customerId = :customerId AND f.tenantId = :tenantId ORDER BY f.createdAt DESC")
    List<UploadedFile> findByCustomerId(@Param("customerId") String customerId, @Param("tenantId") String tenantId);

    /**
     * Find files by type
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.fileType = :fileType ORDER BY f.createdAt DESC")
    Page<UploadedFile> findByFileType(@Param("tenantId") String tenantId, @Param("fileType") UploadedFile.FileType fileType, Pageable pageable);

    /**
     * Find files by MIME type
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.mimeType LIKE :mimeTypePattern ORDER BY f.createdAt DESC")
    Page<UploadedFile> findByMimeType(@Param("tenantId") String tenantId, @Param("mimeTypePattern") String mimeTypePattern, Pageable pageable);

    /**
     * Find files by status
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.status = :status ORDER BY f.createdAt DESC")
    Page<UploadedFile> findByStatus(@Param("tenantId") String tenantId, @Param("status") UploadedFile.FileStatus status, Pageable pageable);

    /**
     * Find files by tags
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND :tag MEMBER OF f.tags ORDER BY f.createdAt DESC")
    List<UploadedFile> findByTag(@Param("tenantId") String tenantId, @Param("tag") String tag);

    /**
     * Find pending files for processing
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.status = 'PENDING' AND f.scanResult = 'PENDING' ORDER BY f.createdAt ASC")
    List<UploadedFile> findPendingFiles();

    /**
     * Find files exceeding download limit
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.maxDownloads IS NOT NULL AND f.downloadCount >= f.maxDownloads AND f.tenantId = :tenantId")
    List<UploadedFile> findExceededDownloads(@Param("tenantId") String tenantId);

    /**
     * Find expired files
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.expiresAt IS NOT NULL AND f.expiresAt < :now AND f.tenantId = :tenantId")
    List<UploadedFile> findExpiredFiles(@Param("now") LocalDateTime now, @Param("tenantId") String tenantId);

    /**
     * Find files by uploaded by user
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.uploadedBy = :userId AND f.tenantId = :tenantId ORDER BY f.createdAt DESC")
    Page<UploadedFile> findByUploadedBy(@Param("userId") String userId, @Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Search files by filename
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND (f.originalFilename LIKE :filenamePattern OR f.storedFilename LIKE :filenamePattern) ORDER BY f.createdAt DESC")
    Page<UploadedFile> searchByFilename(@Param("tenantId") String tenantId, @Param("filenamePattern") String filenamePattern, Pageable pageable);

    /**
     * Get file statistics
     */
    @Query("SELECT f.fileType, COUNT(f), SUM(f.fileSize) FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.status = 'COMPLETED' GROUP BY f.fileType")
    List<Object[]> getFileStatistics(@Param("tenantId") String tenantId);

    /**
     * Get total storage used
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.status = 'COMPLETED'")
    Long getTotalStorageUsed(@Param("tenantId") String tenantId);

    /**
     * Get storage by file type
     */
    @Query("SELECT f.fileType, SUM(f.fileSize) FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.status = 'COMPLETED' GROUP BY f.fileType")
    List<Object[]> getStorageByFileType(@Param("tenantId") String tenantId);

    /**
     * Count files by status
     */
    @Query("SELECT f.status, COUNT(f) FROM UploadedFile f WHERE f.tenantId = :tenantId GROUP BY f.status")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    /**
     * Find recently accessed files
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.lastAccessedAt IS NOT NULL ORDER BY f.lastAccessedAt DESC")
    List<UploadedFile> findRecentlyAccessed(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find large files
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.fileSize > :minSize ORDER BY f.fileSize DESC")
    List<UploadedFile> findLargeFiles(@Param("tenantId") String tenantId, @Param("minSize") Long minSize);

    /**
     * Delete expired files
     */
    @Query("DELETE FROM UploadedFile f WHERE f.expiresAt IS NOT NULL AND f.expiresAt < :cutoff")
    void deleteExpired(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find files by checksum (for duplicate detection)
     */
    Optional<UploadedFile> findByChecksumAndTenantId(@Param("checksum") String checksum, @Param("tenantId") String tenantId);

    /**
     * Get storage quota usage
     */
    @Query("SELECT COUNT(f), COALESCE(SUM(f.fileSize), 0) FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.status = 'COMPLETED'")
    Object[] getStorageQuotaUsage(@Param("tenantId") String tenantId);
}
