package com.droid.bss.infrastructure.performance;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Connection pool configuration for optimal performance
 */
@Configuration
public class ConnectionPoolConfig {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Database connection settings
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);

        // Connection pool settings
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // Performance settings
        config.setConnectionInitSql("SELECT 1"); // Validate connections
        config.setLeakDetectionThreshold(60000); // 60 seconds leak detection
        config.setValidationTimeout(5000); // 5 seconds validation timeout

        // Auto-commit settings
        config.setAutoCommit(true);

        // Connection testing
        config.setConnectionTestQuery("SELECT 1");

        // Health check
        config.setHealthCheckProperties(createHealthCheckProperties());

        // Pool name for monitoring
        config.setPoolName("BSS-HikariPool");

        // Custom properties for PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        HikariDataSource dataSource = new HikariDataSource(config);

        // Start monitoring
        startPoolMonitoring(dataSource);

        return dataSource;
    }

    private java.util.Properties createHealthCheckProperties() {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("connectivityCheckEnabled", "true");
        props.setProperty("expected99PercentileMs", "1000");
        return props;
    }

    private void startPoolMonitoring(HikariDataSource dataSource) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pool-monitor");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(() -> {
            try {
                int active = dataSource.getHikariPoolMXBean().getActiveConnections();
                int total = dataSource.getHikariPoolMXBean().getTotalConnections();
                int idle = dataSource.getHikariPoolMXBean().getIdleConnections();
                int waiting = dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

                System.out.printf("Pool Status: Active=%d, Total=%d, Idle=%d, Waiting=%d%n",
                        active, total, idle, waiting);

                // Log if pool is heavily used
                if (active > maximumPoolSize * 0.8) {
                    System.out.println("WARNING: Connection pool is heavily loaded!");
                }

            } catch (Exception e) {
                System.err.println("Error monitoring pool: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @Bean
    public ScheduledExecutorService connectionPoolExecutor() {
        return Executors.newScheduledThreadPool(5, r -> {
            Thread t = new Thread(r, "connection-pool-monitor");
            t.setDaemon(true);
            return t;
        });
    }
}
