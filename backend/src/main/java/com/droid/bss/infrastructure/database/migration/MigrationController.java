package com.droid.bss.infrastructure.database.migration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Migration Management REST Controller
 *
 * Provides endpoints for:
 * - Migration validation
 * - Migration status
 * - Migration history
 * - Rollback operations
 */
@RestController
@RequestMapping("/api/migration")
@Tag(name = "Migration Management", description = "Database migration management endpoints")
public class MigrationController {

    private static final Logger log = LoggerFactory.getLogger(MigrationController.class);

    private final Flyway flyway;
    private final MigrationValidator migrationValidator;

    public MigrationController(Flyway flyway, MigrationValidator migrationValidator) {
        this.flyway = flyway;
        this.migrationValidator = migrationValidator;
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate all migrations", description = "Validates all database migrations for syntax and integrity")
    public ResponseEntity<Map<String, Object>> validateMigrations() {
        log.info("Received migration validation request");

        MigrationValidator.ValidationResult result = migrationValidator.validateAllMigrations();

        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("duration_ms", result.getDurationMs());
        response.put("errors", result.getErrors());
        response.put("warnings", result.getWarnings());
        response.put("infos", result.getInfos());
        response.put("successes", result.getSuccesses());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/{version}")
    @Operation(summary = "Validate specific migration", description = "Validates a specific migration by version")
    public ResponseEntity<Map<String, Object>> validateMigration(@PathVariable String version) {
        log.info("Received migration validation request for version: {}", version);

        MigrationValidator.ValidationResult result = migrationValidator.validateMigration(version);

        Map<String, Object> response = new HashMap<>();
        response.put("version", version);
        response.put("valid", result.isValid());
        response.put("errors", result.getErrors());
        response.put("warnings", result.getWarnings());
        response.put("infos", result.getInfos());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Get migration status", description = "Returns current migration status and history")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        log.info("Received migration status request");

        MigrationInfo[] appliedMigrations = flyway.info().applied();
        MigrationInfo[] pendingMigrations = flyway.info().pending();

        Map<String, Object> response = new HashMap<>();
        response.put("current_version", flyway.info().current() != null ?
                flyway.info().current().getVersion().toString() : "N/A");

        // Get target version from all migrations
        MigrationInfo[] allMigrations = flyway.info().all();
        String targetVersion = "N/A";
        if (allMigrations.length > 0) {
            MigrationInfo latest = allMigrations[allMigrations.length - 1];
            targetVersion = latest.getVersion().toString();
        }
        response.put("target_version", targetVersion);

        response.put("applied_count", appliedMigrations.length);
        response.put("pending_count", pendingMigrations.length);
        response.put("failed_count", 0); // Not available in current API

        response.put("applied_migrations", Arrays.stream(appliedMigrations)
                .map(this::toMigrationInfoDto)
                .collect(Collectors.toList()));

        response.put("pending_migrations", Arrays.stream(pendingMigrations)
                .map(this::toMigrationInfoDto)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Get migration history", description = "Returns complete migration history")
    public ResponseEntity<List<Map<String, Object>>> getMigrationHistory() {
        log.info("Received migration history request");

        MigrationInfo[] allMigrations = flyway.info().all();

        List<Map<String, Object>> history = Arrays.stream(allMigrations)
                .map(this::toMigrationInfoDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    @PostMapping("/repair")
    @Operation(summary = "Repair migration table", description = "Repairs the Flyway schema history table")
    public ResponseEntity<Map<String, Object>> repairMigration() {
        log.info("Received migration repair request");

        try {
            flyway.repair();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Migration table repaired successfully");

            log.info("Migration repair completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Migration repair failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/undo")
    @Operation(summary = "Undo last migration", description = "Undoes the last applied migration (requires Flyway Teams edition)")
    public ResponseEntity<Map<String, Object>> undoLastMigration() {
        log.info("Received migration undo request");

        try {
            // Note: Undo requires Flyway Teams edition
            // For community edition, manual rollback SQL would be needed

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Undo requires Flyway Teams edition. Manual rollback SQL needed for community edition.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Migration undo failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/info")
    @Operation(summary = "Get Flyway info", description = "Returns Flyway configuration and metadata")
    public ResponseEntity<Map<String, Object>> getFlywayInfo() {
        log.info("Received Flyway info request");

        Map<String, Object> response = new HashMap<>();
        // Note: Flyway.getVersion() not available in current API
        response.put("flyway_version", "Unknown");
        response.put("locations", flyway.getConfiguration().getLocations());
        response.put("schemas", flyway.getConfiguration().getSchemas());
        response.put("baseline_version", flyway.getConfiguration().getBaselineVersion().toString());
        response.put("baseline_description", flyway.getConfiguration().getBaselineDescription());
        response.put("validate_on_migrate", flyway.getConfiguration().isValidateOnMigrate());
        response.put("out_of_order", flyway.getConfiguration().isOutOfOrder());
        response.put("clean_disabled", flyway.getConfiguration().isCleanDisabled());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/checksum/{version}")
    @Operation(summary = "Get migration checksum", description = "Returns checksum for a specific migration version")
    public ResponseEntity<Map<String, Object>> getMigrationChecksum(@PathVariable String version) {
        log.info("Received checksum request for version: {}", version);

        MigrationInfo migration = Arrays.stream(flyway.info().all())
                .filter(info -> info.getVersion().toString().equals(version))
                .findFirst()
                .orElse(null);

        if (migration == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("version", version);
        response.put("checksum", migration.getChecksum());
        response.put("description", migration.getDescription());
        response.put("type", migration.getType());
        response.put("state", migration.getState().toString());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toMigrationInfoDto(MigrationInfo info) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("version", info.getVersion().toString());
        dto.put("description", info.getDescription());
        dto.put("type", info.getType());
        dto.put("state", info.getState().toString());
        dto.put("installed_on", info.getInstalledOn().toString());
        dto.put("execution_time_ms", info.getExecutionTime());
        dto.put("checksum", info.getChecksum());
        return dto;
    }
}
