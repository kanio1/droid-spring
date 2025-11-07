package com.droid.bss.infrastructure.messaging.metrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Event Metrics REST Controller
 *
 * Provides endpoints for monitoring event processing metrics
 */
@RestController
@RequestMapping("/api/v1/metrics/events")
@Tag(name = "Event Metrics", description = "Event processing metrics and monitoring")
public class EventMetricsController {

    private final EventMetricsService metricsService;

    public EventMetricsController(EventMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/overall")
    @Operation(summary = "Get overall event processing statistics", description = "Returns aggregated metrics across all event consumers")
    public ResponseEntity<EventProcessingStatistics> getOverallStatistics() {
        EventProcessingStatistics stats = metricsService.getOverallStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/consumers")
    @Operation(summary = "Get metrics for all event consumers", description = "Returns metrics for each registered event consumer")
    public ResponseEntity<Map<String, EventConsumerMetrics>> getConsumerMetrics() {
        Map<String, EventConsumerMetrics> metrics = metricsService.getConsumerMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/consumers/{consumerName}")
    @Operation(summary = "Get metrics for a specific consumer", description = "Returns metrics for a specific event consumer")
    public ResponseEntity<EventConsumerMetrics> getConsumerMetrics(@PathVariable String consumerName) {
        EventConsumerMetrics metrics = metricsService.getConsumerMetrics(consumerName);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    @Operation(summary = "Get event processing health status", description = "Returns the overall health status of event processing")
    public ResponseEntity<EventHealthStatus> getHealthStatus() {
        EventHealthStatus health = metricsService.getHealthStatus();
        return ResponseEntity.ok(health);
    }
}
