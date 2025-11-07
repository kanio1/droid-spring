package com.droid.bss.api.streams;

import com.droid.bss.application.service.KafkaStreamsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API for monitoring Kafka Streams
 */
@RestController
@RequestMapping("/api/v1/streams")
@Tag(name = "Kafka Streams", description = "Kafka Streams monitoring and management")
public class StreamsController {

    private final KafkaStreamsService streamsService;

    public StreamsController(KafkaStreamsService streamsService) {
        this.streamsService = streamsService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get streams status", description = "Check if Kafka Streams is running")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
            "applicationId", streamsService.getApplicationId(),
            "running", streamsService.isRunning(),
            "state", streamsService.isRunning() ? "RUNNING" : "STOPPED"
        );
        return ResponseEntity.ok(status);
    }

    @GetMapping("/metadata")
    @Operation(summary = "Get streams metadata", description = "Get metadata about Kafka Streams instances")
    public CompletableFuture<ResponseEntity<Collection<StreamsMetadata>>> getMetadata() {
        return streamsService.getAllMetadata()
            .thenApply(metadata -> ResponseEntity.ok(metadata));
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup local state", description = "Clean up local Kafka Streams state")
    public ResponseEntity<Void> cleanup() {
        streamsService.cleanUp();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get streams metrics", description = "Get Kafka Streams metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        // In production, this would expose actual Kafka Streams metrics
        Map<String, Object> metrics = Map.of(
            "applicationId", streamsService.getApplicationId(),
            "status", "IMPLEMENTATION_PENDING"
        );
        return ResponseEntity.ok(metrics);
    }
}
