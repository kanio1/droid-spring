package com.droid.bss.infrastructure.temporal;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Temporal Table Controller
 * REST API for PostgreSQL temporal table operations
 */
@RestController
@RequestMapping("/api/v1/temporal")
@Tag(name = "Temporal Tables", description = "Historical data tracking APIs")
public class TemporalTableController {

    private final TemporalTableService temporalTableService;

    public TemporalTableController(TemporalTableService temporalTableService) {
        this.temporalTableService = temporalTableService;
    }

    /**
     * Get history of an entity
     */
    @GetMapping("/{table}/history/{id}")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @PathVariable String table,
            @PathVariable String id) {

        List<Map<String, Object>> history = temporalTableService.getHistory(table, id);
        return ResponseEntity.ok(history);
    }

    /**
     * Get specific version at time
     */
    @GetMapping("/{table}/version-at")
    public ResponseEntity<Map<String, Object>> getVersionAtTime(
            @PathVariable String table,
            @RequestParam String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant atTime) {

        Map<String, Object> version = temporalTableService.getVersionAtTime(table, id, atTime);
        if (version == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(version);
    }

    /**
     * Get history between time range
     */
    @GetMapping("/{table}/history-range")
    public ResponseEntity<List<Map<String, Object>>> getHistoryBetween(
            @PathVariable String table,
            @RequestParam String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime) {

        List<Map<String, Object>> history = temporalTableService.getHistoryBetween(
            table, id, fromTime, toTime
        );
        return ResponseEntity.ok(history);
    }

    /**
     * Get all changes in time range
     */
    @GetMapping("/{table}/changes")
    public ResponseEntity<List<Map<String, Object>>> getChangesInRange(
            @PathVariable String table,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime) {

        List<Map<String, Object>> changes = temporalTableService.getChangesInRange(
            table, fromTime, toTime
        );
        return ResponseEntity.ok(changes);
    }

    /**
     * Get latest version
     */
    @GetMapping("/{table}/latest/{id}")
    public ResponseEntity<Map<String, Object>> getLatestVersion(
            @PathVariable String table,
            @PathVariable String id) {

        Map<String, Object> latest = temporalTableService.getLatestVersion(table, id);
        if (latest == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * Get change summary
     */
    @GetMapping("/{table}/summary/{id}")
    public ResponseEntity<List<Map<String, Object>>> getChangeSummary(
            @PathVariable String table,
            @PathVariable String id) {

        List<Map<String, Object>> summary = temporalTableService.getChangeSummary(table, id);
        return ResponseEntity.ok(summary);
    }

    /**
     * Find entities changed since
     */
    @GetMapping("/{table}/changed-since")
    public ResponseEntity<List<Map<String, Object>>> findChangedSince(
            @PathVariable String table,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant sinceTime) {

        List<Map<String, Object>> changed = temporalTableService.findChangedSince(table, sinceTime);
        return ResponseEntity.ok(changed);
    }

    /**
     * Reconstruct state at specific time
     */
    @PostMapping("/{table}/reconstruct")
    public ResponseEntity<Map<String, Object>> reconstructState(
            @PathVariable String table,
            @RequestParam String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant targetTime) {

        Map<String, Object> state = temporalTableService.reconstructStateAtTime(table, id, targetTime);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * Get version count
     */
    @GetMapping("/{table}/version-count/{id}")
    public ResponseEntity<Integer> getVersionCount(
            @PathVariable String table,
            @PathVariable String id) {

        int count = temporalTableService.getVersionCount(table, id);
        return ResponseEntity.ok(count);
    }

    /**
     * Create temporal table
     */
    @PostMapping("/create/{table}")
    public ResponseEntity<String> createTemporalTable(
            @PathVariable String table,
            @RequestParam String primaryKeyColumn) {

        temporalTableService.createTemporalTable(table, primaryKeyColumn);
        return ResponseEntity.ok("Temporal table created successfully for: " + table);
    }

    /**
     * Create all temporal tables
     */
    @PostMapping("/create-all")
    public ResponseEntity<String> createAllTemporalTables() {
        temporalTableService.createAllTemporalTables();
        return ResponseEntity.ok("All temporal tables created successfully");
    }

    /**
     * Get temporal tables list
     */
    @GetMapping("/tables")
    public ResponseEntity<List<String>> getTemporalTables() {
        List<String> tables = temporalTableService.getTemporalTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * Verify temporal table
     */
    @GetMapping("/verify/{table}")
    public ResponseEntity<Boolean> verifyTemporalTable(@PathVariable String table) {
        boolean verified = temporalTableService.verifyTemporalTable(table);
        return ResponseEntity.ok(verified);
    }

    /**
     * Cleanup old history
     */
    @DeleteMapping("/{table}/cleanup")
    public ResponseEntity<Integer> cleanupOldHistory(
            @PathVariable String table,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cutoffTime) {

        int deleted = temporalTableService.cleanupOldHistory(table, cutoffTime);
        return ResponseEntity.ok(deleted);
    }
}
