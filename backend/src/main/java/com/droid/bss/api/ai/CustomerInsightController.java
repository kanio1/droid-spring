/**
 * Customer Insight REST API Controller
 *
 * Exposes AI-generated customer insights via REST endpoints
 * Provides CRUD operations and analytics for customer insights
 */

package com.droid.bss.api.ai;

import com.droid.bss.application.service.ai.CustomerInsightService;
import com.droid.bss.domain.ai.CustomerInsight;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/insights")
@Tag(name = "Customer Insights", description = "AI-powered customer insights API")
public class CustomerInsightController {

    private final CustomerInsightService insightService;

    @Autowired
    public CustomerInsightController(CustomerInsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
        summary = "Get customer insights",
        description = "Retrieve all insights for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved customer insights")
    public ResponseEntity<PagedModel<EntityModel<CustomerInsight>>> getCustomerInsights(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            PagedResourcesAssembler<CustomerInsight> assembler
    ) {
        Page<CustomerInsight> insights = insightService.getCustomerInsights(customerId, tenantId, pageable);
        return ResponseEntity.ok(assembler.toModel(insights));
    }

    @GetMapping("/customer/{customerId}/active")
    @Operation(
        summary = "Get active customer insights",
        description = "Retrieve only active (unexpired) insights for a customer"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active insights")
    public ResponseEntity<List<CustomerInsight>> getActiveCustomerInsights(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        List<CustomerInsight> insights = insightService.getActiveCustomerInsights(customerId, tenantId);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get insights by type",
        description = "Retrieve insights filtered by type (e.g., CHURN_RISK, LIFETIME_VALUE)"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved insights by type")
    public ResponseEntity<PagedModel<EntityModel<CustomerInsight>>> getInsightsByType(
            @Parameter(description = "Insight type", required = true)
            @PathVariable CustomerInsight.InsightType type,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Pagination parameters")
            Pageable pageable,
            PagedResourcesAssembler<CustomerInsight> assembler
    ) {
        Page<CustomerInsight> insights = insightService.getInsightsByType(tenantId, type, pageable);
        return ResponseEntity.ok(assembler.toModel(insights));
    }

    @GetMapping("/high-confidence")
    @Operation(
        summary = "Get high-confidence insights",
        description = "Retrieve insights with confidence score above specified threshold"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved high-confidence insights")
    public ResponseEntity<List<CustomerInsight>> getHighConfidenceInsights(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Minimum confidence score (0.0 to 1.0)", example = "0.8")
            @RequestParam(defaultValue = "0.8") Double minConfidence
    ) {
        List<CustomerInsight> insights = insightService.getHighConfidenceInsights(tenantId, minConfidence);
        return ResponseEntity.ok(insights);
    }

    @PostMapping("/customer/{customerId}/generate-churn-risk")
    @Operation(
        summary = "Generate churn risk insight",
        description = "Generate a new AI-powered churn risk assessment for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Successfully generated churn risk insight")
    public ResponseEntity<CustomerInsight> generateChurnRiskInsight(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        CustomerInsight insight = insightService.generateChurnRiskInsight(customerId, tenantId);
        return ResponseEntity.status(201).body(insight);
    }

    @PostMapping("/customer/{customerId}/generate-cross-sell")
    @Operation(
        summary = "Generate cross-sell insight",
        description = "Generate a new AI-powered cross-sell opportunity for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Successfully generated cross-sell insight")
    public ResponseEntity<CustomerInsight> generateCrossSellInsight(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Product category to analyze")
            @RequestParam(defaultValue = "premium") String productCategory
    ) {
        CustomerInsight insight = insightService.generateCrossSellInsight(customerId, tenantId, productCategory);
        return ResponseEntity.status(201).body(insight);
    }

    @PostMapping("/customer/{customerId}/generate-ltv")
    @Operation(
        summary = "Generate LTV insight",
        description = "Generate a new AI-powered lifetime value prediction for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Successfully generated LTV insight")
    public ResponseEntity<CustomerInsight> generateLTVInsight(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        CustomerInsight insight = insightService.generateLTVInsight(customerId, tenantId);
        return ResponseEntity.status(201).body(insight);
    }

    @PostMapping("/customer/{customerId}/generate-behavioral")
    @Operation(
        summary = "Generate behavioral insight",
        description = "Generate a new AI-powered behavioral pattern analysis for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Successfully generated behavioral insight")
    public ResponseEntity<CustomerInsight> generateBehavioralInsight(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        CustomerInsight insight = insightService.generateBehavioralInsight(customerId, tenantId);
        return ResponseEntity.status(201).body(insight);
    }

    @PostMapping("/customer/{customerId}/generate-purchase-prediction")
    @Operation(
        summary = "Generate purchase prediction",
        description = "Generate a new AI-powered purchase prediction for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Successfully generated purchase prediction")
    public ResponseEntity<CustomerInsight> generatePurchasePrediction(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        CustomerInsight insight = insightService.generatePurchasePredictionInsight(customerId, tenantId);
        return ResponseEntity.status(201).body(insight);
    }

    @PostMapping("/batch-generate")
    @Operation(
        summary = "Batch generate insights",
        description = "Generate insights for multiple customers at once"
    )
    @ApiResponse(responseCode = "201", description = "Successfully generated batch insights")
    public ResponseEntity<List<CustomerInsight>> batchGenerateInsights(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "List of customer IDs", required = true)
            @RequestBody List<String> customerIds
    ) {
        List<CustomerInsight> insights = insightService.batchGenerateInsights(tenantId, customerIds);
        return ResponseEntity.status(201).body(insights);
    }

    @PostMapping("/{insightId}/view")
    @Operation(
        summary = "Mark insight as viewed",
        description = "Mark an insight as viewed by the user"
    )
    @ApiResponse(responseCode = "200", description = "Successfully marked insight as viewed")
    public ResponseEntity<Void> markInsightAsViewed(
            @Parameter(description = "Insight ID", required = true)
            @PathVariable String insightId
    ) {
        insightService.markInsightAsViewed(insightId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{insightId}/dismiss")
    @Operation(
        summary = "Dismiss insight",
        description = "Dismiss an insight (remove from active list)"
    )
    @ApiResponse(responseCode = "200", description = "Successfully dismissed insight")
    public ResponseEntity<Void> dismissInsight(
            @Parameter(description = "Insight ID", required = true)
            @PathVariable String insightId
    ) {
        insightService.dismissInsight(insightId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get insight statistics",
        description = "Retrieve analytics and statistics about customer insights"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved insight statistics")
    public ResponseEntity<Map<String, Object>> getInsightStatistics(
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        Map<String, Object> statistics = insightService.getInsightStatistics(tenantId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the AI insights service is operational"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Customer Insight Service",
            "version", "1.0.0"
        ));
    }
}
