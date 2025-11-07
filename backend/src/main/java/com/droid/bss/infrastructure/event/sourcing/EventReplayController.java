package com.droid.bss.infrastructure.event.sourcing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for event replay operations
 */
@RestController
@RequestMapping("/api/v1/event-sourcing")
@Tag(name = "Event Sourcing", description = "Event sourcing and replay operations")
public class EventReplayController {

    private final EventReplayService replayService;

    public EventReplayController(EventReplayService replayService) {
        this.replayService = replayService;
    }

    /**
     * Replay all events for a specific aggregate
     */
    @PostMapping("/replay/aggregate/{aggregateId}")
    @Operation(
        summary = "Replay aggregate events",
        description = "Replays all events for a specific aggregate"
    )
    public ResponseEntity<EventReplayService.ReplayResult> replayAggregate(
            @Parameter(description = "Aggregate ID", required = true)
            @PathVariable String aggregateId) {
        var result = replayService.replayAggregateEvents(aggregateId);
        return ResponseEntity.ok(result);
    }

    /**
     * Replay events by type
     */
    @PostMapping("/replay/type/{eventType}")
    @Operation(
        summary = "Replay events by type",
        description = "Replays all events of a specific type"
    )
    public ResponseEntity<EventReplayService.ReplayResult> replayByType(
            @Parameter(description = "Event type", required = true)
            @PathVariable String eventType) {
        var result = replayService.replayEventsByType(eventType);
        return ResponseEntity.ok(result);
    }

    /**
     * Replay events by correlation ID
     */
    @PostMapping("/replay/correlation/{correlationId}")
    @Operation(
        summary = "Replay events by correlation ID",
        description = "Replays all events with a specific correlation ID"
    )
    public ResponseEntity<EventReplayService.ReplayResult> replayByCorrelationId(
            @Parameter(description = "Correlation ID", required = true)
            @PathVariable String correlationId) {
        var result = replayService.replayEventsByCorrelationId(correlationId);
        return ResponseEntity.ok(result);
    }

    /**
     * Replay events in a time range
     */
    @PostMapping("/replay/time-range")
    @Operation(
        summary = "Replay events in time range",
        description = "Replays all events between two timestamps"
    )
    public ResponseEntity<EventReplayService.ReplayResult> replayInTimeRange(
            @Parameter(description = "Start time", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        var result = replayService.replayEventsInTimeRange(start, end);
        return ResponseEntity.ok(result);
    }

    /**
     * Replay events since a specific event
     */
    @PostMapping("/replay/since/{eventId}")
    @Operation(
        summary = "Replay events since ID",
        description = "Replays all events since a specific event ID"
    )
    public ResponseEntity<EventReplayService.ReplayResult> replaySince(
            @Parameter(description = "Event ID", required = true)
            @PathVariable UUID eventId) {
        var result = replayService.replayEventsSince(eventId);
        return ResponseEntity.ok(result);
    }

    /**
     * Async replay of events
     */
    @PostMapping("/replay/async/aggregate/{aggregateId}")
    @Operation(
        summary = "Async replay aggregate events",
        description = "Asynchronously replays all events for a specific aggregate"
    )
    public ResponseEntity<CompletableFuture<EventReplayService.ReplayResult>> replayAggregateAsync(
            @Parameter(description = "Aggregate ID", required = true)
            @PathVariable String aggregateId) {
        var future = replayService.replayEventsAsync(aggregateId);
        return ResponseEntity.ok(future);
    }

    /**
     * Check event stream integrity
     */
    @GetMapping("/integrity/{aggregateId}")
    @Operation(
        summary = "Check event stream integrity",
        description = "Checks if the event stream for an aggregate is valid and consistent"
    )
    public ResponseEntity<EventReplayService.IntegrityCheckResult> checkIntegrity(
            @Parameter(description = "Aggregate ID", required = true)
            @PathVariable String aggregateId) {
        var result = replayService.checkEventStreamIntegrity(aggregateId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get event statistics
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get event statistics",
        description = "Retrieves statistics about stored events"
    )
    public ResponseEntity<EventReplayService.EventStatistics> getStatistics() {
        var result = replayService.getEventStatistics();
        return ResponseEntity.ok(result);
    }

    /**
     * Batch replay for multiple aggregates
     */
    @PostMapping("/replay/batch")
    @Operation(
        summary = "Batch replay multiple aggregates",
        description = "Replays events for multiple aggregates in parallel"
    )
    public ResponseEntity<Map<String, EventReplayService.ReplayResult>> replayBatch(
            @Parameter(description = "List of aggregate IDs", required = true)
            @RequestBody List<String> aggregateIds) {
        var results = replayService.replayMultipleAggregates(aggregateIds);
        return ResponseEntity.ok(results);
    }
}
