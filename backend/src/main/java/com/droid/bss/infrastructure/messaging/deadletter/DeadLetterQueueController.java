package com.droid.bss.infrastructure.messaging.deadletter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Dead Letter Queue REST Controller
 *
 * Provides endpoints for monitoring and managing failed events
 */
@RestController
@RequestMapping("/api/v1/dlq")
@Tag(name = "Dead Letter Queue", description = "Dead Letter Queue monitoring and management")
public class DeadLetterQueueController {

    private final DeadLetterQueue deadLetterQueue;

    public DeadLetterQueueController(DeadLetterQueue deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get DLQ statistics", description = "Returns statistics about failed events")
    public ResponseEntity<DLQStatistics> getStatistics() {
        DLQStatistics stats = deadLetterQueue.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/failed-events")
    @Operation(summary = "Get all failed events", description = "Returns all events currently in the DLQ")
    public ResponseEntity<Map<String, DLQEntry>> getFailedEvents() {
        Map<String, DLQEntry> failedEvents = deadLetterQueue.getFailedEvents();
        return ResponseEntity.ok(failedEvents);
    }

    @GetMapping("/failed-events/pending")
    @Operation(summary = "Get pending failed events", description = "Returns all pending (unprocessed) failed events")
    public ResponseEntity<Map<String, DLQEntry>> getPendingEvents() {
        Map<String, DLQEntry> pendingEvents = deadLetterQueue.getFailedEventsByStatus(DLQStatus.PENDING);
        return ResponseEntity.ok(pendingEvents);
    }

    @GetMapping("/failed-events/resolved")
    @Operation(summary = "Get resolved failed events", description = "Returns all resolved failed events")
    public ResponseEntity<Map<String, DLQEntry>> getResolvedEvents() {
        Map<String, DLQEntry> resolvedEvents = deadLetterQueue.getFailedEventsByStatus(DLQStatus.RESOLVED);
        return ResponseEntity.ok(resolvedEvents);
    }

    @PostMapping("/reprocess/{eventId}")
    @Operation(summary = "Reprocess a failed event", description = "Marks an event for reprocessing")
    public ResponseEntity<String> reprocessEvent(@PathVariable String eventId) {
        boolean success = deadLetterQueue.reprocessEvent(eventId);
        if (success) {
            return ResponseEntity.ok("Event " + eventId + " marked for reprocessing");
        } else {
            return ResponseEntity.badRequest().body("Failed to reprocess event " + eventId + " (not found or already processed)");
        }
    }

    @PostMapping("/resolve/{eventId}")
    @Operation(summary = "Mark event as resolved", description = "Marks a failed event as manually resolved")
    public ResponseEntity<String> resolveEvent(
            @PathVariable String eventId,
            @RequestParam String resolution
    ) {
        boolean success = deadLetterQueue.markAsResolved(eventId, resolution);
        if (success) {
            return ResponseEntity.ok("Event " + eventId + " marked as resolved");
        } else {
            return ResponseEntity.badRequest().body("Failed to resolve event " + eventId + " (not found)");
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Clean up old DLQ entries", description = "Removes old resolved entries (older than 7 days)")
    public ResponseEntity<String> cleanupOldEntries() {
        deadLetterQueue.cleanupOldEntries();
        return ResponseEntity.ok("Old DLQ entries cleaned up successfully");
    }
}
