/**
 * Prediction Validation REST API Controller
 *
 * Exposes ML model prediction validation functionality via REST endpoints
 */

package com.droid.bss.api.ai;

import com.droid.bss.application.service.ai.PredictionValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/validation")
@Tag(name = "Prediction Validation", description = "ML model prediction validation API")
public class PredictionValidationController {

    private final PredictionValidationService validationService;

    @Autowired
    public PredictionValidationController(PredictionValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Validate a single prediction",
        description = "Validate a single ML model prediction against actual outcome"
    )
    @ApiResponse(responseCode = "200", description = "Successfully validated prediction")
    public ResponseEntity<PredictionValidationService.PredictionValidationResult> validatePrediction(
            @Parameter(description = "Validation request", required = true)
            @RequestBody ValidationRequest request
    ) {
        PredictionValidationService.PredictionValidationResult result = validationService.validatePrediction(
            request.getModelName(),
            request.getModelVersion(),
            request.getPrediction(),
            request.getActual(),
            request.getTolerance() != null ? request.getTolerance() : 0.1,
            request.getCustomerId(),
            request.getTenantId()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-batch")
    @Operation(
        summary = "Batch validate predictions",
        description = "Validate multiple ML model predictions in batch"
    )
    @ApiResponse(responseCode = "200", description = "Successfully validated batch predictions")
    public ResponseEntity<List<PredictionValidationService.PredictionValidationResult>> validateBatch(
            @Parameter(description = "Batch validation request", required = true)
            @RequestBody BatchValidationRequest request
    ) {
        List<PredictionValidationService.PredictionValidationResult> results = validationService.validateBatch(
            request.getTestCases(),
            request.getModelName(),
            request.getModelVersion()
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping("/performance")
    @Operation(
        summary = "Calculate model performance metrics",
        description = "Calculate overall performance metrics (accuracy, precision, recall, F1) for a model"
    )
    @ApiResponse(responseCode = "200", description = "Successfully calculated performance metrics")
    public ResponseEntity<PredictionValidationService.ModelPerformanceMetrics> calculatePerformance(
            @Parameter(description = "Performance calculation request", required = true)
            @RequestBody PerformanceRequest request
    ) {
        PredictionValidationService.ModelPerformanceMetrics metrics = validationService.calculateModelPerformance(
            request.getValidationResults(),
            request.getModelName(),
            request.getTenantId()
        );
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/compare-models")
    @Operation(
        summary = "Compare model versions",
        description = "Compare performance between two model versions"
    )
    @ApiResponse(responseCode = "200", description = "Successfully compared models")
    public ResponseEntity<PredictionValidationService.ModelComparisonResult> compareModels(
            @Parameter(description = "Model comparison request", required = true)
            @RequestBody ModelComparisonRequest request
    ) {
        PredictionValidationService.ModelComparisonResult result = validationService.compareModelVersions(
            request.getCurrentVersionResults(),
            request.getPreviousVersionResults(),
            request.getModelName(),
            request.getCurrentVersion(),
            request.getPreviousVersion()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ab-test")
    @Operation(
        summary = "Run A/B test on model variants",
        description = "Run A/B test to compare two model variants"
    )
    @ApiResponse(responseCode = "200", description = "Successfully ran A/B test")
    public ResponseEntity<PredictionValidationService.ABTestResult> runABTest(
            @Parameter(description = "A/B test request", required = true)
            @RequestBody ABTestRequest request
    ) {
        PredictionValidationService.ABTestResult result = validationService.runABTest(
            request.getModelName(),
            request.getVariantAName(),
            request.getVariantBName(),
            request.getTestCases(),
            request.getTrafficSplit() != null ? request.getTrafficSplit() : 0.5
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the prediction validation service is operational"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Prediction Validation Service",
            "version", "1.0.0"
        ));
    }

    // Request classes

    public static class ValidationRequest {
        private String modelName;
        private String modelVersion;
        private Object prediction;
        private Object actual;
        private Double tolerance;
        private String customerId;
        private String tenantId;

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

        public Object getPrediction() { return prediction; }
        public void setPrediction(Object prediction) { this.prediction = prediction; }

        public Object getActual() { return actual; }
        public void setActual(Object actual) { this.actual = actual; }

        public Double getTolerance() { return tolerance; }
        public void setTolerance(Double tolerance) { this.tolerance = tolerance; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class BatchValidationRequest {
        private List<PredictionValidationService.PredictionTestCase> testCases;
        private String modelName;
        private String modelVersion;

        public List<PredictionValidationService.PredictionTestCase> getTestCases() { return testCases; }
        public void setTestCases(List<PredictionValidationService.PredictionTestCase> testCases) { this.testCases = testCases; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    }

    public static class PerformanceRequest {
        private List<PredictionValidationService.PredictionValidationResult> validationResults;
        private String modelName;
        private String tenantId;

        public List<PredictionValidationService.PredictionValidationResult> getValidationResults() { return validationResults; }
        public void setValidationResults(List<PredictionValidationService.PredictionValidationResult> validationResults) { this.validationResults = validationResults; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class ModelComparisonRequest {
        private List<PredictionValidationService.PredictionValidationResult> currentVersionResults;
        private List<PredictionValidationService.PredictionValidationResult> previousVersionResults;
        private String modelName;
        private String currentVersion;
        private String previousVersion;

        public List<PredictionValidationService.PredictionValidationResult> getCurrentVersionResults() { return currentVersionResults; }
        public void setCurrentVersionResults(List<PredictionValidationService.PredictionValidationResult> currentVersionResults) { this.currentVersionResults = currentVersionResults; }

        public List<PredictionValidationService.PredictionValidationResult> getPreviousVersionResults() { return previousVersionResults; }
        public void setPreviousVersionResults(List<PredictionValidationService.PredictionValidationResult> previousVersionResults) { this.previousVersionResults = previousVersionResults; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }

        public String getPreviousVersion() { return previousVersion; }
        public void setPreviousVersion(String previousVersion) { this.previousVersion = previousVersion; }
    }

    public static class ABTestRequest {
        private String modelName;
        private String variantAName;
        private String variantBName;
        private List<PredictionValidationService.PredictionTestCase> testCases;
        private Double trafficSplit;

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getVariantAName() { return variantAName; }
        public void setVariantAName(String variantAName) { this.variantAName = variantAName; }

        public String getVariantBName() { return variantBName; }
        public void setVariantBName(String variantBName) { this.variantBName = variantBName; }

        public List<PredictionValidationService.PredictionTestCase> getTestCases() { return testCases; }
        public void setTestCases(List<PredictionValidationService.PredictionTestCase> testCases) { this.testCases = testCases; }

        public Double getTrafficSplit() { return trafficSplit; }
        public void setTrafficSplit(Double trafficSplit) { this.trafficSplit = trafficSplit; }
    }
}
