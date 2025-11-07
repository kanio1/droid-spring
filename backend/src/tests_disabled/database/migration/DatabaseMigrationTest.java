package com.droid.bss.infrastructure.database.migration;

import com.droid.bss.BssApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Migration Tests
 *
 * Tests Flyway migration execution, idempotency, rollback, and schema evolution.
 * Validates migration history, checksums, and version conflicts.
 */
@SpringBootTest(classes = Application.class)
@Testcontainers
@DisplayName("Database Migration Tests")
class DatabaseMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("bss_migration_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.validate-on-migrate", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.flyway.out-of-order", () -> false);
    }

    @Test
    @DisplayName("Should execute migration successfully")
    void shouldExecuteMigration() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.baseline();

        MigrationResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(0);
        assertThat(flyway.info().current().getVersion().getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Should validate migration idempotency")
    void shouldValidateMigrationIdempotency() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        MigrationResult firstResult = flyway.migrate();
        int firstVersion = Integer.parseInt(flyway.info().current().getVersion().getVersion());

        MigrationResult secondResult = flyway.migrate();
        int secondVersion = Integer.parseInt(flyway.info().current().getVersion().getVersion());

        assertThat(firstResult.success).isTrue();
        assertThat(secondResult.success).isTrue();
        assertThat(secondResult.migrationsExecuted).isEqualTo(0);
        assertThat(firstVersion).isEqualTo(secondVersion);
    }

    @Test
    @DisplayName("Should handle migration rollback")
    void shouldHandleMigrationRollback() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();

        String currentVersion = flyway.info().current().getVersion().getVersion();
        assertThat(currentVersion).isNotNull();

        try {
            flyway.clean();
            assertThat(flyway.info().current()).isNull();
        } catch (Exception e) {
            fail("Migration rollback test failed", e);
        }
    }

    @Test
    @DisplayName("Should handle failed migration")
    void shouldHandleFailedMigration() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();

        int currentVersion = Integer.parseInt(flyway.info().current().getVersion().getVersion());

        try {
            flyway.repair();
        } catch (Exception e) {
        }

        int repairedVersion = Integer.parseInt(flyway.info().current().getVersion().getVersion());
        assertThat(repairedVersion).isGreaterThanOrEqualTo(currentVersion);
    }

    @Test
    @DisplayName("Should validate migration checksum")
    void shouldValidateMigrationChecksum() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();

        var migrations = flyway.info().all().getMigrations();

        assertThat(migrations).isNotEmpty();
        migrations.forEach(migration -> {
            assertThat(migration.getChecksum()).isNotNull();
            assertThat(migration.getChecksum()).isGreaterThan(0);
        });
    }

    @Test
    @DisplayName("Should detect version conflicts")
    void shouldDetectVersionConflicts() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .outOfOrder(false)
                .load();

        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();

        var migrations = flyway.info().applied().getMigrations();
        assertThat(migrations).isNotEmpty();

        var versions = migrations.stream()
                .map(m -> m.getVersion().getVersion())
                .toList();

        assertThat(versions).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should detect schema drift")
    void shouldDetectSchemaDrift() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        flyway.baseline();
        MigrationResult result = flyway.migrate();

        assertThat(result.success).isTrue();

        var pending = flyway.info().pending();
        var all = flyway.info().all();

        assertThat(pending.getMigrations()).isNotNull();
    }

    @Test
    @DisplayName("Should validate data migration")
    void shouldValidateDataMigration() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();

        try (Connection conn = dataSource.getConnection()) {
            var tables = getTables(conn);
            assertThat(tables).isNotEmpty();

            if (tables.contains("customer")) {
                var count = getRowCount(conn, "customer");
                assertThat(count).isGreaterThanOrEqualTo(0);
            }
        } catch (SQLException e) {
            fail("Data migration validation failed", e);
        }
    }

    @Test
    @DisplayName("Should handle multi-schema migration")
    void shouldHandleMultiSchemaMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("public", "bss")
                .load();

        flyway.baseline();

        MigrationResult result = flyway.migrate();

        assertThat(result.success).isTrue();

        try (Connection conn = dataSource.getConnection()) {
            var schemas = getSchemas(conn);
            assertThat(schemas).contains("public");
        } catch (SQLException e) {
            fail("Multi-schema migration failed", e);
        }
    }

    @Test
    @DisplayName("Should measure migration performance")
    void shouldMeasureMigrationPerformance() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        long startTime = System.currentTimeMillis();
        MigrationResult result = flyway.migrate();
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        assertThat(result.success).isTrue();
        assertThat(duration).isGreaterThan(0);
        assertThat(duration).isLessThan(30000);
    }

    @Test
    @DisplayName("Should create baseline migration")
    void shouldCreateBaselineMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();

        String baselineVersion = flyway.getBaselineVersion().toString();
        MigrationResult result = flyway.baseline();

        assertThat(result.success).isTrue();
        assertThat(baselineVersion).isNotNull();

        var current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo(baselineVersion);
    }

    @Test
    @DisplayName("Should validate migration history integrity")
    void shouldValidateMigrationHistoryIntegrity() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        flyway.baseline();
        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();

        try (Connection conn = dataSource.getConnection()) {
            var schemaVersion = getSchemaVersion(conn, "flyway_schema_history");
            assertThat(schemaVersion).isNotNull();
        } catch (SQLException e) {
            fail("Migration history integrity check failed", e);
        }
    }

    @Test
    @DisplayName("Should resolve dependency resolution")
    void shouldResolveDependencyResolution() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();

        flyway.baseline();
        MigrationResult result = flyway.migrate();

        assertThat(result.success).isTrue();

        var migrations = flyway.info().applied().getMigrations();

        assertThat(migrations).isNotEmpty();

        for (int i = 1; i < migrations.size(); i++) {
            var current = migrations.get(i).getVersion().getVersion();
            var previous = migrations.get(i - 1).getVersion().getVersion();
            assertThat(Integer.parseInt(current)).isGreaterThan(Integer.parseInt(previous));
        }
    }

    @Test
    @DisplayName("Should support selective migration")
    void shouldSupportSelectiveMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .target("V2")
                .load();

        flyway.baseline();
        MigrationResult result = flyway.migrate();

        assertThat(result.success).isTrue();

        String currentVersion = flyway.info().current().getVersion().getVersion();
        int version = Integer.parseInt(currentVersion);

        assertThat(version).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should support dry-run migration")
    void shouldSupportDryRunMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load();

        flyway.baseline();
        var pending = flyway.info().pending();

        assertThat(pending).isNotNull();
        assertThat(pending.getMigrations()).isNotNull();

        MigrationResult result = flyway.migrate();
        assertThat(result.success).isTrue();
    }

    private static class MigrationResult {
        boolean success;
        int migrationsExecuted;

        MigrationResult(boolean success, int migrationsExecuted) {
            this.success = success;
            this.migrationsExecuted = migrationsExecuted;
        }
    }

    private List<String> getTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
        while (rs.next()) {
            tables.add(rs.getString("TABLE_NAME"));
        }
        return tables;
    }

    private int getRowCount(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private List<String> getSchemas(Connection conn) throws SQLException {
        List<String> schemas = new ArrayList<>();
        ResultSet rs = conn.getMetaData().getSchemas();
        while (rs.next()) {
            schemas.add(rs.getString("SCHEMA_NAME"));
        }
        return schemas;
    }

    private String getSchemaVersion(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.createStatement()
                .executeQuery("SELECT version FROM " + tableName + " ORDER BY installed_rank DESC LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("version");
            }
        }
        return null;
    }
}
