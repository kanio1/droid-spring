package com.droid.bss.api.monitoring;

import com.droid.bss.application.command.monitoring.GenerateOptimizationRecommendationsUseCase;
import com.droid.bss.application.command.monitoring.ManageOptimizationRecommendationUseCase;
import com.droid.bss.application.query.monitoring.GetOptimizationRecommendationsUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.monitoring.OptimizationRecommendation;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring/optimization-recommendations")
@Tag(name = "Optimization Recommendations", description = "Cost optimization recommendations API")
public class OptimizationRecommendationsController {

    private final GenerateOptimizationRecommendationsUseCase generateOptimizationRecommendationsUseCase;
    private final GetOptimizationRecommendationsUseCase getOptimizationRecommendationsUseCase;
    private final ManageOptimizationRecommendationUseCase manageOptimizationRecommendationUseCase;

    public OptimizationRecommendationsController(
            GenerateOptimizationRecommendationsUseCase generateOptimizationRecommendationsUseCase,
            GetOptimizationRecommendationsUseCase getOptimizationRecommendationsUseCase,
            ManageOptimizationRecommendationUseCase manageOptimizationRecommendationUseCase) {
        this.generateOptimizationRecommendationsUseCase = generateOptimizationRecommendationsUseCase;
        this.getOptimizationRecommendationsUseCase = getOptimizationRecommendationsUseCase;
        this.manageOptimizationRecommendationUseCase = manageOptimizationRecommendationUseCase;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze costs and generate optimization recommendations")
    @Audited(action = AuditAction.MONITORING_CREATE, entityType = "OptimizationRecommendation", description = "Analyzing costs and generating optimization recommendations")
    public ResponseEntity<List<OptimizationRecommendation>> analyzeAndRecommend(
            @RequestParam Long customerId,
            @RequestParam(required = false) String resourceType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        List<OptimizationRecommendation> recommendations = generateOptimizationRecommendationsUseCase
                .analyzeAndRecommend(customerId, resourceType, startDate, endDate);

        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all recommendations for a customer")
    public ResponseEntity<List<OptimizationRecommendation>> getByCustomerId(@PathVariable Long customerId) {
        List<OptimizationRecommendation> recommendations = getOptimizationRecommendationsUseCase
                .getByCustomerId(customerId);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    @Operation(summary = "Get recommendations by customer and status")
    public ResponseEntity<List<OptimizationRecommendation>> getByCustomerIdAndStatus(
            @PathVariable Long customerId,
            @PathVariable String status) {

        List<OptimizationRecommendation> recommendations = getOptimizationRecommendationsUseCase
                .getByCustomerIdAndStatus(customerId, status);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/customer/{customerId}/resource-type/{resourceType}")
    @Operation(summary = "Get recommendations by customer and resource type")
    public ResponseEntity<List<OptimizationRecommendation>> getByCustomerIdAndResourceType(
            @PathVariable Long customerId,
            @PathVariable String resourceType) {

        List<OptimizationRecommendation> recommendations = getOptimizationRecommendationsUseCase
                .getByCustomerIdAndResourceType(customerId, resourceType);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/customer/{customerId}/severity/{severity}")
    @Operation(summary = "Get recommendations by customer and severity")
    public ResponseEntity<List<OptimizationRecommendation>> getByCustomerIdAndSeverity(
            @PathVariable Long customerId,
            @PathVariable String severity) {

        List<OptimizationRecommendation> recommendations = getOptimizationRecommendationsUseCase
                .getByCustomerIdAndSeverity(customerId, severity);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge a recommendation")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "OptimizationRecommendation", description = "Acknowledging optimization recommendation {id}")
    public ResponseEntity<OptimizationRecommendation> acknowledge(@PathVariable Long id) {
        return manageOptimizationRecommendationUseCase.acknowledgeRecommendation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/implement")
    @Operation(summary = "Mark recommendation as implemented")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "OptimizationRecommendation", description = "Implementing optimization recommendation {id}")
    public ResponseEntity<OptimizationRecommendation> implement(@PathVariable Long id) {
        return manageOptimizationRecommendationUseCase.implementRecommendation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss a recommendation")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "OptimizationRecommendation", description = "Dismissing optimization recommendation {id}")
    public ResponseEntity<OptimizationRecommendation> dismiss(@PathVariable Long id) {
        return manageOptimizationRecommendationUseCase.dismissRecommendation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recommendation")
    @Audited(action = AuditAction.MONITORING_DELETE, entityType = "OptimizationRecommendation", description = "Deleting optimization recommendation {id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        manageOptimizationRecommendationUseCase.deleteRecommendation(id);
        return ResponseEntity.noContent().build();
    }
}
