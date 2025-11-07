package com.droid.bss.infrastructure.event.sourcing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for projection management
 */
@RestController
@RequestMapping("/api/v1/projections")
@Tag(name = "Projections", description = "Projection management operations")
public class ProjectionController {

    private final ProjectionManager projectionManager;

    public ProjectionController(ProjectionManager projectionManager) {
        this.projectionManager = projectionManager;
    }

    /**
     * Get all projection statuses
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get projection statuses",
        description = "Retrieves the status of all registered projections"
    )
    public ResponseEntity<List<ProjectionManager.ProjectionStatus>> getProjectionStatuses() {
        var statuses = projectionManager.getProjectionStatuses();
        return ResponseEntity.ok(statuses);
    }

    /**
     * Get status of a specific projection
     */
    @GetMapping("/status/{name}")
    @Operation(
        summary = "Get projection status",
        description = "Retrieves the status of a specific projection"
    )
    public ResponseEntity<ProjectionManager.ProjectionStatus> getProjectionStatus(
            @Parameter(description = "Projection name", required = true)
            @PathVariable String name) {
        var status = projectionManager.getProjectionStatus(name);
        return status.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rebuild a projection
     */
    @PostMapping("/rebuild/{name}")
    @Operation(
        summary = "Rebuild projection",
        description = "Rebuilds a specific projection from events"
    )
    public ResponseEntity<Void> rebuildProjection(
            @Parameter(description = "Projection name", required = true)
            @PathVariable String name) {
        try {
            projectionManager.rebuildProjection(name);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Rebuild all projections
     */
    @PostMapping("/rebuild/all")
    @Operation(
        summary = "Rebuild all projections",
        description = "Rebuilds all registered projections from events"
    )
    public ResponseEntity<Void> rebuildAllProjections() {
        projectionManager.rebuildAllProjections();
        return ResponseEntity.ok().build();
    }

    /**
     * Check if all projections are up to date
     */
    @GetMapping("/health")
    @Operation(
        summary = "Check projection health",
        description = "Checks if all projections are up to date"
    )
    public ResponseEntity<Map<String, Object>> checkProjectionsHealth() {
        boolean allUpToDate = projectionManager.areAllProjectionsUpToDate();
        var result = Map.of(
                "upToDate", allUpToDate,
                "totalProjections", projectionManager.getProjectionStatuses().size()
        );
        return ResponseEntity.ok(result);
    }
}
