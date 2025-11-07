package com.droid.bss.api.monitoring;

import com.droid.bss.application.service.monitoring.CollectMetricUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.infrastructure.audit.Audited;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Stub implementation of MetricsController
 * Minimal implementation for testing purposes
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private final CollectMetricUseCase collectMetricUseCase;

    public MetricsController(CollectMetricUseCase collectMetricUseCase) {
        this.collectMetricUseCase = collectMetricUseCase;
    }

    @PostMapping
    @Audited(action = AuditAction.METRIC_CREATE, entityType = "Metric", description = "Ingesting metric data")
    public ResponseEntity<String> ingestMetric(@RequestBody Object request) {
        return ResponseEntity.ok("Metric ingestion stubbed");
    }

    @PostMapping("/batch")
    @Audited(action = AuditAction.METRIC_CREATE, entityType = "Metric", description = "Batch ingesting metrics data")
    public ResponseEntity<String> ingestMetrics(@RequestBody Object request) {
        return ResponseEntity.ok("Batch metric ingestion stubbed");
    }

    @GetMapping
    public ResponseEntity<String> getMetricsByCustomer() {
        return ResponseEntity.ok("Get metrics stubbed");
    }
}
