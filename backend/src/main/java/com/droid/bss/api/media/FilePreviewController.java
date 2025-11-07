/**
 * File Preview REST API Controller
 *
 * Exposes file preview/thumbnail generation and retrieval
 */

package com.droid.bss.api.media;

import com.droid.bss.application.service.media.FilePreviewService;
import com.droid.bss.domain.media.FilePreview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/previews")
@Tag(name = "File Preview", description = "File preview/thumbnail API")
public class FilePreviewController {

    private final FilePreviewService previewService;

    @Autowired
    public FilePreviewController(FilePreviewService previewService) {
        this.previewService = previewService;
    }

    @GetMapping("/file/{fileId}")
    @Operation(
        summary = "Get all previews for a file",
        description = "Retrieve all generated previews for a specific file"
    )
    @ApiResponse(responseCode = "200", description = "Previews retrieved successfully")
    public ResponseEntity<List<FilePreview>> getFilePreviews(
            @Parameter(description = "File ID", required = true)
            @PathVariable String fileId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        List<FilePreview> previews = previewService.getFilePreviews(fileId, tenantId);
        return ResponseEntity.ok(previews);
    }

    @GetMapping("/file/{fileId}/type/{previewType}")
    @Operation(
        summary = "Get specific preview type",
        description = "Retrieve a specific type of preview for a file"
    )
    @ApiResponse(responseCode = "200", description = "Preview retrieved successfully")
    public ResponseEntity<FilePreview> getPreviewByType(
            @Parameter(description = "File ID", required = true)
            @PathVariable String fileId,
            @Parameter(description = "Preview type", required = true)
            @PathVariable FilePreview.PreviewType previewType,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        Optional<FilePreview> preview = previewService.getPreview(fileId, tenantId, previewType);
        return preview.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get preview statistics",
        description = "Retrieve statistics about generated previews"
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<Map<String, Long>> getPreviewStatistics(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        Map<String, Long> stats = previewService.getPreviewStatistics(tenantId);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/file/{fileId}")
    @Operation(
        summary = "Delete all previews for a file",
        description = "Delete all previews associated with a file"
    )
    @ApiResponse(responseCode = "200", description = "Previews deleted successfully")
    public ResponseEntity<Map<String, Boolean>> deleteFilePreviews(
            @Parameter(description = "File ID", required = true)
            @PathVariable String fileId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        previewService.deleteFilePreviews(fileId, tenantId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the file preview service is operational"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "File Preview Service",
            "version", "1.0.0"
        ));
    }
}
