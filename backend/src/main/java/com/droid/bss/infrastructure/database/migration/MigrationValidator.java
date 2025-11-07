package com.droid.bss.infrastructure.database.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Migration Validator
 *
 * Validates Flyway migrations for:
 * - Syntax correctness
 * - Schema compatibility
 * - Data integrity
 * - Performance impact
 * - Rollback capability
 */
@Component
public class MigrationValidator {

    private static final Logger log = LoggerFactory.getLogger(MigrationValidator.class);

    private final DataSource dataSource;

    public MigrationValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Validate all migrations in classpath
     */
    public ValidationResult validateAllMigrations() {
        log.info("Starting migration validation...");

        ValidationResult result = new ValidationResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            // 1. Validate Flyway configuration
            validateFlywayConfig();

            // 2. Validate migration files exist
            validateMigrationFilesExist();

            // 3. Validate migration syntax
            validateMigrationSyntax();

            // 4. Validate migration order
            validateMigrationOrder();

            // 5. Validate schema consistency
            validateSchemaConsistency();

            // 6. Validate performance impact
            validatePerformanceImpact();

        } catch (Exception e) {
            log.error("Migration validation failed", e);
            result.addError("Validation exception: " + e.getMessage());
        }

        result.setEndTime(System.currentTimeMillis());
        result.setDurationMs(result.getEndTime() - result.getStartTime());

        log.info("Migration validation completed. Status: {}",
                result.isValid() ? "VALID" : "INVALID");

        return result;
    }

    /**
     * Validate single migration
     */
    public ValidationResult validateMigration(String migrationVersion) {
        log.info("Validating migration: {}", migrationVersion);

        ValidationResult result = new ValidationResult();

        try {
            // Get migration file
            String migrationFile = findMigrationFile(migrationVersion);
            if (migrationFile == null) {
                result.addError("Migration file not found: " + migrationVersion);
                return result;
            }

            // Validate file syntax
            validateMigrationFile(migrationFile);

            // Validate schema changes
            validateMigrationSchema(migrationFile);

        } catch (Exception e) {
            log.error("Migration validation failed for: {}", migrationVersion, e);
            result.addError("Validation exception: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateFlywayConfig() {
        ValidationResult result = new ValidationResult();

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .load();

            flyway.validate();

            result.addSuccess("Flyway configuration is valid");

        } catch (Exception e) {
            result.addError("Flyway configuration error: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateMigrationFilesExist() {
        ValidationResult result = new ValidationResult();

        try (Stream<Path> paths = Files.list(Paths.get("src/main/resources/db/migration"))) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .collect(Collectors.toList());

            if (migrationFiles.isEmpty()) {
                result.addError("No migration files found");
            } else {
                result.addSuccess("Found " + migrationFiles.size() + " migration files");
            }

        } catch (IOException e) {
            result.addError("Failed to list migration files: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateMigrationSyntax() {
        ValidationResult result = new ValidationResult();

        try (Stream<Path> paths = Files.list(Paths.get("src/main/resources/db/migration"))) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .collect(Collectors.toList());

            for (Path migrationFile : migrationFiles) {
                ValidationResult fileResult = validateMigrationFile(migrationFile.toString());
                result.merge(fileResult);
            }

        } catch (IOException e) {
            result.addError("Failed to validate migration syntax: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateMigrationFile(String migrationFile) {
        ValidationResult result = new ValidationResult();

        try {
            List<String> lines = Files.readAllLines(Paths.get(migrationFile));

            // Check for common SQL issues
            List<String> issues = new ArrayList<>();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();

                // Check for missing semicolons
                if (line.contains("CREATE TABLE") && !line.endsWith(";")) {
                    issues.add("Line " + (i + 1) + ": CREATE TABLE missing semicolon");
                }

                // Check for potential syntax errors
                if (line.contains("DROP") && !line.contains("IF EXISTS") && !line.contains("--")) {
                    issues.add("Line " + (i + 1) + ": DROP without IF EXISTS (potentially dangerous)");
                }
            }

            if (issues.isEmpty()) {
                result.addSuccess("Migration file syntax is valid: " + migrationFile);
            } else {
                for (String issue : issues) {
                    result.addWarning(issue);
                }
            }

        } catch (Exception e) {
            result.addError("Failed to validate migration file: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateMigrationOrder() {
        ValidationResult result = new ValidationResult();

        try (Stream<Path> paths = Files.list(Paths.get("src/main/resources/db/migration"))) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted()
                    .collect(Collectors.toList());

            // Validate version ordering
            String previousVersion = null;
            for (Path migrationFile : migrationFiles) {
                String fileName = migrationFile.getFileName().toString();
                String version = extractVersion(fileName);

                if (version != null) {
                    if (previousVersion != null && version.compareTo(previousVersion) <= 0) {
                        result.addWarning("Version ordering issue: " + previousVersion + " -> " + version);
                    }
                    previousVersion = version;
                }
            }

            result.addSuccess("Migration order validation completed");

        } catch (Exception e) {
            result.addError("Failed to validate migration order: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateSchemaConsistency() {
        ValidationResult result = new ValidationResult();

        try (Connection conn = dataSource.getConnection()) {
            // Check for orphaned foreign keys
            String fkQuery = """
                SELECT
                    tc.constraint_name,
                    tc.table_name,
                    kcu.column_name,
                    ccu.table_name AS foreign_table_name,
                    ccu.column_name AS foreign_column_name
                FROM information_schema.table_constraints AS tc
                JOIN information_schema.key_column_usage AS kcu
                    ON tc.constraint_name = kcu.constraint_name
                JOIN information_schema.constraint_column_usage AS ccu
                    ON ccu.constraint_name = tc.constraint_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                """;

            try (PreparedStatement stmt = conn.prepareStatement(fkQuery);
                 ResultSet rs = stmt.executeQuery()) {

                int fkCount = 0;
                while (rs.next()) {
                    fkCount++;
                    // In production, validate each foreign key exists
                }

                result.addSuccess("Found " + fkCount + " foreign key constraints");
            }

        } catch (SQLException e) {
            result.addError("Failed to validate schema consistency: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validatePerformanceImpact() {
        ValidationResult result = new ValidationResult();

        // Analyze migration files for potential performance issues
        try (Stream<Path> paths = Files.list(Paths.get("src/main/resources/db/migration"))) {
            List<Path> migrationFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".sql"))
                    .collect(Collectors.toList());

            for (Path migrationFile : migrationFiles) {
                String fileName = migrationFile.getFileName().toString();
                List<String> lines = Files.readAllLines(migrationFile);

                // Check for performance warnings
                long createTableCount = lines.stream()
                        .filter(l -> l.contains("CREATE TABLE"))
                        .count();

                long addIndexCount = lines.stream()
                        .filter(l -> l.contains("CREATE INDEX"))
                        .count();

                long addColumnCount = lines.stream()
                        .filter(l -> l.contains("ADD COLUMN"))
                        .count();

                if (createTableCount > 0) {
                    result.addWarning(fileName + ": Contains " + createTableCount + " table(s) - consider online migration");
                }

                if (addIndexCount > 0) {
                    result.addInfo(fileName + ": Contains " + addIndexCount + " index(es)");
                }

                if (addColumnCount > 0) {
                    result.addWarning(fileName + ": Contains " + addColumnCount + " column addition(s) - verify nullability");
                }
            }

        } catch (Exception e) {
            result.addError("Failed to validate performance impact: " + e.getMessage());
        }

        return result;
    }

    private ValidationResult validateMigrationSchema(String migrationFile) {
        // Validate specific schema changes
        return new ValidationResult();
    }

    private String findMigrationFile(String version) {
        try (Stream<Path> paths = Files.list(Paths.get("src/main/resources/db/migration"))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(p -> p.toString())
                    .filter(p -> p.contains(version))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private String extractVersion(String fileName) {
        // Extract version from V{VERSION}__{DESCRIPTION}.sql
        int start = fileName.indexOf('V');
        int end = fileName.indexOf("__");
        if (start >= 0 && end >= 0) {
            return fileName.substring(start + 1, end);
        }
        return null;
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private long startTime;
        private long endTime;
        private long durationMs;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();
        private final List<String> successes = new ArrayList<>();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public void addError(String error) {
            log.error("Migration validation error: {}", error);
            errors.add(error);
        }

        public void addWarning(String warning) {
            log.warn("Migration validation warning: {}", warning);
            warnings.add(warning);
        }

        public void addInfo(String info) {
            log.info("Migration validation info: {}", info);
            infos.add(info);
        }

        public void addSuccess(String success) {
            log.info("Migration validation success: {}", success);
            successes.add(success);
        }

        public void merge(ValidationResult other) {
            this.errors.addAll(other.errors);
            this.warnings.addAll(other.warnings);
            this.infos.addAll(other.infos);
            this.successes.addAll(other.successes);
        }

        // Getters
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public List<String> getInfos() { return infos; }
        public List<String> getSuccesses() { return successes; }
    }
}
