/**
 * File Preview Service
 *
 * Generates and manages file previews/thumbnails
 * Supports images, videos, PDFs, and documents
 */

package com.droid.bss.application.service.media;

import com.droid.bss.domain.media.FilePreview;
import com.droid.bss.domain.media.FilePreviewRepository;
import com.droid.bss.domain.media.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class FilePreviewService {

    private final FilePreviewRepository previewRepository;
    private final String previewPath = "/tmp/previews";

    @Autowired
    public FilePreviewService(FilePreviewRepository previewRepository) {
        this.previewRepository = previewRepository;
        initializePreviewStorage();
    }

    /**
     * Initialize preview storage directories
     */
    private void initializePreviewStorage() {
        try {
            Path previewDir = Paths.get(previewPath);
            if (!Files.exists(previewDir)) {
                Files.createDirectories(previewDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create preview directory", e);
        }
    }

    /**
     * Generate preview for uploaded file
     */
    public PreviewResult generatePreview(
            String fileId,
            String tenantId,
            UploadedFile.FileType fileType,
            String originalFilePath
    ) {
        try {
            List<FilePreview> previews = new ArrayList<>();

            switch (fileType) {
                case IMAGE:
                    previews.addAll(generateImagePreviews(fileId, tenantId, originalFilePath));
                    break;
                case VIDEO:
                    previews.addAll(generateVideoPreviews(fileId, tenantId, originalFilePath));
                    break;
                case PDF:
                    previews.addAll(generatePdfPreviews(fileId, tenantId, originalFilePath));
                    break;
                case DOCUMENT:
                case SPREADSHEET:
                case PRESENTATION:
                    previews.addAll(generateDocumentPreview(fileId, tenantId, originalFilePath));
                    break;
                case AUDIO:
                    previews.addAll(generateAudioPreview(fileId, tenantId, originalFilePath));
                    break;
                default:
                    previews.add(generateGenericPreview(fileId, tenantId));
            }

            return new PreviewResult(previews);
        } catch (Exception e) {
            return new PreviewResult(null, e.getMessage());
        }
    }

    /**
     * Generate image previews
     */
    private List<FilePreview> generateImagePreviews(String fileId, String tenantId, String filePath) {
        List<FilePreview> previews = new ArrayList<>();

        try {
            // Get image dimensions (mock implementation)
            int[] dimensions = getImageDimensions(filePath);
            int width = dimensions[0];
            int height = dimensions[1];

            // Generate multiple thumbnail sizes
            previews.add(createImagePreview(fileId, tenantId, filePath, width, height, FilePreview.ThumbnailSize.SMALL));
            previews.add(createImagePreview(fileId, tenantId, filePath, width, height, FilePreview.ThumbnailSize.MEDIUM));
            previews.add(createImagePreview(fileId, tenantId, filePath, width, height, FilePreview.ThumbnailSize.LARGE));

        } catch (Exception e) {
            // Create failed preview
            FilePreview failed = createFailedPreview(fileId, tenantId, "Failed to generate image preview: " + e.getMessage());
            previews.add(failed);
        }

        return previews;
    }

    /**
     * Generate video previews
     */
    private List<FilePreview> generateVideoPreviews(String fileId, String tenantId, String filePath) {
        List<FilePreview> previews = new ArrayList<>();

        try {
            // Get video duration (mock implementation)
            int duration = getVideoDuration(filePath);
            int[] dimensions = getVideoDimensions(filePath);
            int width = dimensions[0];
            int height = dimensions[1];

            // Generate video thumbnail at 1 second
            FilePreview thumbnail = createVideoPreview(
                fileId,
                tenantId,
                filePath,
                width,
                height,
                duration,
                1,
                FilePreview.ThumbnailSize.MEDIUM
            );
            previews.add(thumbnail);

            // Generate waveform preview (audio visualization)
            FilePreview waveform = createAudioWaveformPreview(fileId, tenantId, filePath, duration);
            previews.add(waveform);

        } catch (Exception e) {
            FilePreview failed = createFailedPreview(fileId, tenantId, "Failed to generate video preview: " + e.getMessage());
            previews.add(failed);
        }

        return previews;
    }

    /**
     * Generate PDF previews
     */
    private List<FilePreview> generatePdfPreviews(String fileId, String tenantId, String filePath) {
        List<FilePreview> previews = new ArrayList<>();

        try {
            int pageCount = getPdfPageCount(filePath);
            int maxPages = Math.min(pageCount, 3); // Generate up to 3 pages

            for (int i = 1; i <= maxPages; i++) {
                FilePreview page = createPdfPagePreview(
                    fileId,
                    tenantId,
                    filePath,
                    i,
                    pageCount,
                    FilePreview.ThumbnailSize.MEDIUM
                );
                previews.add(page);
            }

        } catch (Exception e) {
            FilePreview failed = createFailedPreview(fileId, tenantId, "Failed to generate PDF preview: " + e.getMessage());
            previews.add(failed);
        }

        return previews;
    }

    /**
     * Generate document preview
     */
    private List<FilePreview> generateDocumentPreview(String fileId, String tenantId, String filePath) {
        List<FilePreview> previews = new ArrayList<>();

        try {
            // Generate generic document preview
            FilePreview preview = new FilePreview();
            preview.setFileId(fileId);
            preview.setTenantId(tenantId);
            preview.setPreviewType(FilePreview.PreviewType.THUMBNAIL);
            preview.setPreviewPath(generatePreviewPath(fileId, "doc-preview", "png"));
            preview.setWidth(600);
            preview.setHeight(800);
            preview.setThumbnailSize(FilePreview.ThumbnailSize.MEDIUM);
            preview.setQuality(85);
            preview.setFormat("png");
            preview.setFileSize(45000L);
            preview.setStatus(FilePreview.PreviewStatus.COMPLETED);
            preview.setProcessedAt(LocalDateTime.now());

            // Create mock file
            Path previewFile = Paths.get(preview.getPreviewPath());
            Files.createDirectories(previewFile.getParent());
            Files.createFile(previewFile);

            previews.add(previewRepository.save(preview));

        } catch (Exception e) {
            FilePreview failed = createFailedPreview(fileId, tenantId, "Failed to generate document preview: " + e.getMessage());
            previews.add(failed);
        }

        return previews;
    }

    /**
     * Generate audio preview
     */
    private List<FilePreview> generateAudioPreview(String fileId, String tenantId, String filePath) {
        List<FilePreview> previews = new ArrayList<>();

        try {
            int duration = getAudioDuration(filePath);
            FilePreview waveform = createAudioWaveformPreview(fileId, tenantId, filePath, duration);
            previews.add(waveform);

        } catch (Exception e) {
            FilePreview failed = createFailedPreview(fileId, tenantId, "Failed to generate audio preview: " + e.getMessage());
            previews.add(failed);
        }

        return previews;
    }

    /**
     * Generate generic preview
     */
    private FilePreview generateGenericPreview(String fileId, String tenantId) {
        FilePreview preview = new FilePreview();
        preview.setFileId(fileId);
        preview.setTenantId(tenantId);
        preview.setPreviewType(FilePreview.PreviewType.THUMBNAIL);
        preview.setPreviewPath(generatePreviewPath(fileId, "generic", "png"));
        preview.setWidth(300);
        preview.setHeight(300);
        preview.setThumbnailSize(FilePreview.ThumbnailSize.MEDIUM);
        preview.setQuality(80);
        preview.setFormat("png");
        preview.setFileSize(25000L);
        preview.setStatus(FilePreview.PreviewStatus.COMPLETED);
        preview.setProcessedAt(LocalDateTime.now());

        try {
            Path previewFile = Paths.get(preview.getPreviewPath());
            Files.createDirectories(previewFile.getParent());
            Files.createFile(previewFile);
        } catch (IOException e) {
            // Log error
        }

        return previewRepository.save(preview);
    }

    /**
     * Create image preview
     */
    private FilePreview createImagePreview(
            String fileId,
            String tenantId,
            String filePath,
            int originalWidth,
            int originalHeight,
            FilePreview.ThumbnailSize size
    ) {
        // Calculate target dimensions
        int[] targetDims = getTargetDimensions(originalWidth, originalHeight, size);
        int targetWidth = targetDims[0];
        int targetHeight = targetDims[1];

        String format = "webp"; // Modern format
        String filename = String.format("%s-%s.%s", fileId, size.name().toLowerCase(), format);

        FilePreview preview = new FilePreview();
        preview.setFileId(fileId);
        preview.setTenantId(tenantId);
        preview.setPreviewType(FilePreview.PreviewType.THUMBNAIL);
        preview.setPreviewPath(generatePreviewPath(fileId, filename, null));
        preview.setWidth(targetWidth);
        preview.setHeight(targetHeight);
        preview.setThumbnailSize(size);
        preview.setQuality(size == FilePreview.ThumbnailSize.LARGE ? 90 : 80);
        preview.setFormat(format);
        preview.setFileSize(calculatePreviewSize(targetWidth, targetHeight, format));
        preview.setStatus(FilePreview.PreviewStatus.COMPLETED);
        preview.setProcessedAt(LocalDateTime.now());

        try {
            Path previewFile = Paths.get(preview.getPreviewPath());
            Files.createDirectories(previewFile.getParent());
            Files.createFile(previewFile);
        } catch (IOException e) {
            // Log error
        }

        return previewRepository.save(preview);
    }

    /**
     * Create video preview
     */
    private FilePreview createVideoPreview(
            String fileId,
            String tenantId,
            String filePath,
            int width,
            int height,
            int duration,
            int frameAt,
            FilePreview.ThumbnailSize size
    ) {
        int[] targetDims = getTargetDimensions(width, height, size);

        FilePreview preview = new FilePreview();
        preview.setFileId(fileId);
        preview.setTenantId(tenantId);
        preview.setPreviewType(FilePreview.PreviewType.VIDEO_FRAME);
        preview.setPreviewPath(generatePreviewPath(fileId, "video-frame", "jpg"));
        preview.setWidth(targetDims[0]);
        preview.setHeight(targetDims[1]);
        preview.setDuration(duration);
        preview.setThumbnailSize(size);
        preview.setQuality(85);
        preview.setFormat("jpg");
        preview.setFileSize(75000L);
        preview.setStatus(FilePreview.PreviewStatus.COMPLETED);
        preview.setProcessedAt(LocalDateTime.now());

        try {
            Path previewFile = Paths.get(preview.getPreviewPath());
            Files.createDirectories(previewFile.getParent());
            Files.createFile(previewFile);
        } catch (IOException e) {
            // Log error
        }

        return previewRepository.save(preview);
    }

    /**
     * Create PDF page preview
     */
    private FilePreview createPdfPagePreview(
            String fileId,
            String tenantId,
            String filePath,
            int pageNumber,
            int totalPages,
            FilePreview.ThumbnailSize size
    ) {
        FilePreview preview = new FilePreview();
        preview.setFileId(fileId);
        preview.setTenantId(tenantId);
        preview.setPreviewType(FilePreview.PreviewType.DOCUMENT_PAGE);
        preview.setPreviewPath(generatePreviewPath(fileId, "page-" + pageNumber, "png"));
        preview.setWidth(600);
        preview.setHeight(800);
        preview.setPageCount(totalPages);
        preview.setThumbnailSize(size);
        preview.setQuality(80);
        preview.setFormat("png");
        preview.setFileSize(95000L);
        preview.setStatus(FilePreview.PreviewStatus.COMPLETED);
        preview.setProcessedAt(LocalDateTime.now());

        try {
            Path previewFile = Paths.get(preview.getPreviewPath());
            Files.createDirectories(previewFile.getParent());
            Files.createFile(previewFile);
        } catch (IOException e) {
            // Log error
        }

        return previewRepository.save(preview);
    }

    /**
     * Create audio waveform preview
     */
    private FilePreview createAudioWaveformPreview(
            String fileId,
            String tenantId,
            String filePath,
            int duration
    ) {
        FilePreview preview = new FilePreview();
        preview.setFileId(fileId);
        preview.setTenantId(tenantId);
        preview.setPreviewType(FilePreview.PreviewType.AUDIO_WAVEFORM);
        preview.setPreviewPath(generatePreviewPath(fileId, "waveform", "png"));
        preview.setWidth(800);
        preview.setHeight(200);
        preview.setDuration(duration);
        preview.setThumbnailSize(FilePreview.ThumbnailSize.LARGE);
        preview.setQuality(75);
        preview.setFormat("png");
        preview.setFileSize(45000L);
        preview.setStatus(FilePreview.PreviewStatus.COMPLETED);
        preview.setProcessedAt(LocalDateTime.now());

        try {
            Path previewFile = Paths.get(preview.getPreviewPath());
            Files.createDirectories(previewFile.getParent());
            Files.createFile(previewFile);
        } catch (IOException e) {
            // Log error
        }

        return previewRepository.save(preview);
    }

    /**
     * Create failed preview
     */
    private FilePreview createFailedPreview(String fileId, String tenantId, String errorMessage) {
        FilePreview preview = new FilePreview();
        preview.setFileId(fileId);
        preview.setTenantId(tenantId);
        preview.setPreviewType(FilePreview.PreviewType.THUMBNAIL);
        preview.setPreviewPath(generatePreviewPath(fileId, "failed", "png"));
        preview.setWidth(300);
        preview.setHeight(300);
        preview.setThumbnailSize(FilePreview.ThumbnailSize.MEDIUM);
        preview.setFormat("png");
        preview.setStatus(FilePreview.PreviewStatus.FAILED);
        preview.setErrorMessage(errorMessage);
        preview.setProcessedAt(LocalDateTime.now());

        return previewRepository.save(preview);
    }

    /**
     * Get previews for file
     */
    public List<FilePreview> getFilePreviews(String fileId, String tenantId) {
        return previewRepository.findCompletedByFileId(fileId, tenantId);
    }

    /**
     * Get specific preview
     */
    public Optional<FilePreview> getPreview(
            String fileId,
            String tenantId,
            FilePreview.PreviewType previewType
    ) {
        return previewRepository.findByFileIdAndTenantIdAndPreviewType(fileId, tenantId, previewType);
    }

    /**
     * Delete previews for file
     */
    public void deleteFilePreviews(String fileId, String tenantId) {
        List<FilePreview> previews = previewRepository.findByFileIdAndTenantId(fileId, tenantId);
        for (FilePreview preview : previews) {
            try {
                Files.deleteIfExists(Paths.get(preview.getPreviewPath()));
            } catch (IOException e) {
                // Log error but continue
            }
        }
        previewRepository.deleteByFileIdAndTenantId(fileId, tenantId);
    }

    /**
     * Get preview statistics
     */
    public Map<String, Long> getPreviewStatistics(String tenantId) {
        List<Object[]> stats = previewRepository.getPreviewStatistics(tenantId);
        Map<String, Long> result = new HashMap<>();
        for (Object[] stat : stats) {
            result.put(((FilePreview.PreviewType) stat[0]).name(), (Long) stat[1]);
        }
        return result;
    }

    // Mock utility methods (in real implementation, would use image/video processing libraries)

    private int[] getImageDimensions(String filePath) {
        return new int[]{1920, 1080}; // Mock dimensions
    }

    private int getVideoDuration(String filePath) {
        return 120; // 2 minutes mock
    }

    private int[] getVideoDimensions(String filePath) {
        return new int[]{1920, 1080}; // Mock dimensions
    }

    private int getPdfPageCount(String filePath) {
        return 15; // Mock page count
    }

    private int getAudioDuration(String filePath) {
        return 180; // 3 minutes mock
    }

    private int[] getTargetDimensions(int width, int height, FilePreview.ThumbnailSize size) {
        switch (size) {
            case SMALL:
                return new int[]{150, 150};
            case MEDIUM:
                return new int[]{300, 300};
            case LARGE:
                return new int[]{600, 600};
            default:
                return new int[]{width, height};
        }
    }

    private long calculatePreviewSize(int width, int height, String format) {
        long baseSize = (long) width * height;
        if ("webp".equals(format)) {
            return baseSize / 10; // WebP compression
        } else if ("jpg".equals(format)) {
            return baseSize / 8; // JPEG compression
        } else {
            return baseSize / 5; // PNG (less compression)
        }
    }

    private String generatePreviewPath(String fileId, String filename, String format) {
        String suffix = format != null ? "." + format : "";
        return String.format("%s/%s/%s%s", previewPath, fileId.substring(0, 2), filename, suffix);
    }

    // Result class

    public static class PreviewResult {
        private List<FilePreview> previews;
        private String error;

        public PreviewResult(List<FilePreview> previews) {
            this.previews = previews;
        }

        public PreviewResult(List<FilePreview> previews, String error) {
            this.previews = previews;
            this.error = error;
        }

        public List<FilePreview> getPreviews() { return previews; }
        public String getError() { return error; }
        public boolean isSuccess() { return previews != null && !previews.isEmpty() && error == null; }
    }
}
