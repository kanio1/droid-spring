/**
 * File Preview Repository
 *
 * Data access layer for file previews
 */

package com.droid.bss.domain.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilePreviewRepository extends JpaRepository<FilePreview, String> {

    /**
     * Find previews by file ID
     */
    List<FilePreview> findByFileIdAndTenantId(String fileId, String tenantId);

    /**
     * Find preview by file ID and type
     */
    Optional<FilePreview> findByFileIdAndTenantIdAndPreviewType(
        String fileId,
        String tenantId,
        FilePreview.PreviewType previewType
    );

    /**
     * Find thumbnail by file ID and size
     */
    Optional<FilePreview> findByFileIdAndTenantIdAndThumbnailSize(
        String fileId,
        String tenantId,
        FilePreview.ThumbnailSize thumbnailSize
    );

    /**
     * Find completed previews by file ID
     */
    @Query("SELECT fp FROM FilePreview fp WHERE fp.fileId = :fileId AND fp.tenantId = :tenantId AND fp.status = 'COMPLETED' ORDER BY fp.createdAt DESC")
    List<FilePreview> findCompletedByFileId(@Param("fileId") String fileId, @Param("tenantId") String tenantId);

    /**
     * Find pending previews for processing
     */
    @Query("SELECT fp FROM FilePreview fp WHERE fp.status = 'PENDING' ORDER BY fp.createdAt ASC")
    List<FilePreview> findPendingPreviews();

    /**
     * Find previews by type
     */
    List<FilePreview> findByPreviewTypeAndTenantId(FilePreview.PreviewType previewType, String tenantId);

    /**
     * Find previews by thumbnail size
     */
    List<FilePreview> findByThumbnailSizeAndTenantId(FilePreview.ThumbnailSize thumbnailSize, String tenantId);

    /**
     * Count previews by file
     */
    @Query("SELECT COUNT(fp) FROM FilePreview fp WHERE fp.fileId = :fileId AND fp.tenantId = :tenantId")
    long countByFileId(@Param("fileId") String fileId, @Param("tenantId") String tenantId);

    /**
     * Check if preview exists
     */
    @Query("SELECT COUNT(fp) > 0 FROM FilePreview fp WHERE fp.fileId = :fileId AND fp.tenantId = :tenantId AND fp.previewType = :previewType")
    boolean existsByFileIdAndPreviewType(
        @Param("fileId") String fileId,
        @Param("tenantId") String tenantId,
        @Param("previewType") FilePreview.PreviewType previewType
    );

    /**
     * Delete previews by file ID
     */
    void deleteByFileIdAndTenantId(String fileId, String tenantId);

    /**
     * Get preview statistics
     */
    @Query("SELECT fp.previewType, COUNT(fp) FROM FilePreview fp WHERE fp.tenantId = :tenantId GROUP BY fp.previewType")
    List<Object[]> getPreviewStatistics(@Param("tenantId") String tenantId);

    /**
     * Find previews by status
     */
    List<FilePreview> findByStatusAndTenantId(FilePreview.PreviewStatus status, String tenantId);
}
