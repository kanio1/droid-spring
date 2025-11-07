/**
 * File Upload REST API Controller
 *
 * Exposes file upload and management functionality via REST endpoints
 */

package com.droid.bss.api.media;

import com.droid.bss.application.service.media.FileStorageService;
import com.droid.bss.domain.media.UploadedFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Upload", description = "File upload and management API")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Autowired
    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @Operation(
        summary = "Upload a single file",
        description = "Upload a file to the system"
    )
    @ApiResponse(responseCode = "201", description = "File uploaded successfully")
    public ResponseEntity<FileStorageService.FileUploadResult> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Customer ID")
            @RequestParam(required = false) String customerId,
            @Parameter(description = "Uploaded by user ID", required = true)
            @RequestParam String uploadedBy,
            @Parameter(description = "File tags (comma-separated)")
            @RequestParam(required = false) String tags,
            @Parameter(description = "File description")
            @RequestParam(required = false) String description
    ) {
        try {
            List<String> tagList = tags != null ? List.of(tags.split(",")) : null;
            FileStorageService.FileUploadResult result = fileStorageService.uploadFile(
                file,
                tenantId,
                customerId,
                uploadedBy,
                tagList,
                description
            );

            if (result.isSuccess()) {
                return ResponseEntity.status(201).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new FileStorageService.FileUploadResult(null, e.getMessage())
            );
        }
    }

    @PostMapping("/upload-batch")
    @Operation(
        summary = "Upload multiple files",
        description = "Upload multiple files in a single request"
    )
    @ApiResponse(responseCode = "201", description = "Files uploaded successfully")
    public ResponseEntity<List<FileStorageService.FileUploadResult>> uploadFiles(
            @Parameter(description = "Files to upload", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Customer ID")
            @RequestParam(required = false) String customerId,
            @Parameter(description = "Uploaded by user ID", required = true)
            @RequestParam String uploadedBy
    ) {
        try {
            List<FileStorageService.FileUploadResult> results = fileStorageService.uploadFiles(
                files,
                tenantId,
                customerId,
                uploadedBy
            );
            return ResponseEntity.status(201).body(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{fileId}")
    @Operation(
        summary = "Get file by ID",
        description = "Retrieve file metadata by ID"
    )
    @ApiResponse(responseCode = "200", description = "File found")
    public ResponseEntity<UploadedFile> getFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable String fileId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        Optional<UploadedFile> file = fileStorageService.getFile(fileId, tenantId);
        return file.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
        summary = "Get all files",
        description = "Retrieve all files for the tenant with pagination"
    )
    @ApiResponse(responseCode = "200", description = "Files retrieved successfully")
    public ResponseEntity<PagedModel<EntityModel<UploadedFile>>> getFiles(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            PagedResourcesAssembler<UploadedFile> assembler
    ) {
        Page<UploadedFile> files = fileStorageService.getFiles(tenantId, pageable);
        return ResponseEntity.ok(assembler.toModel(files));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
        summary = "Get customer files",
        description = "Retrieve all files for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Customer files retrieved")
    public ResponseEntity<List<UploadedFile>> getCustomerFiles(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        List<UploadedFile> files = fileStorageService.getCustomerFiles(customerId, tenantId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/type/{fileType}")
    @Operation(
        summary = "Get files by type",
        description = "Retrieve files filtered by type (IMAGE, VIDEO, DOCUMENT, etc.)"
    )
    @ApiResponse(responseCode = "200", description = "Files retrieved successfully")
    public ResponseEntity<PagedModel<EntityModel<UploadedFile>>> getFilesByType(
            @Parameter(description = "File type", required = true)
            @PathVariable UploadedFile.FileType fileType,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            PagedResourcesAssembler<UploadedFile> assembler
    ) {
        Page<UploadedFile> files = fileStorageService.getFilesByType(tenantId, fileType, pageable);
        return ResponseEntity.ok(assembler.toModel(files));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search files",
        description = "Search files by filename"
    )
    @ApiResponse(responseCode = "200", description = "Search completed")
    public ResponseEntity<PagedModel<EntityModel<UploadedFile>>> searchFiles(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            PagedResourcesAssembler<UploadedFile> assembler
    ) {
        Page<UploadedFile> files = fileStorageService.searchFiles(tenantId, query, pageable);
        return ResponseEntity.ok(assembler.toModel(files));
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get file statistics",
        description = "Retrieve file storage statistics"
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    public ResponseEntity<FileStorageService.FileStatistics> getStatistics(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        FileStorageService.FileStatistics stats = fileStorageService.getFileStatistics(tenantId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/quota")
    @Operation(
        summary = "Get storage quota",
        description = "Retrieve storage quota usage information"
    )
    @ApiResponse(responseCode = "200", description = "Quota information retrieved")
    public ResponseEntity<FileStorageService.StorageQuota> getQuota(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        FileStorageService.StorageQuota quota = fileStorageService.getStorageQuota(tenantId);
        return ResponseEntity.ok(quota);
    }

    @DeleteMapping("/{fileId}")
    @Operation(
        summary = "Delete file",
        description = "Delete a file from the system"
    )
    @ApiResponse(responseCode = "200", description = "File deleted successfully")
    public ResponseEntity<Map<String, Boolean>> deleteFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable String fileId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        boolean deleted = fileStorageService.deleteFile(fileId, tenantId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @PostMapping("/{fileId}/access")
    @Operation(
        summary = "Mark file as accessed",
        description = "Update last accessed timestamp"
    )
    @ApiResponse(responseCode = "200", description = "File access marked")
    public ResponseEntity<Void> markAsAccessed(
            @Parameter(description = "File ID", required = true)
            @PathVariable String fileId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        fileStorageService.markFileAsAccessed(fileId, tenantId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the file storage service is operational"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "File Storage Service",
            "version", "1.0.0"
        ));
    }
}
