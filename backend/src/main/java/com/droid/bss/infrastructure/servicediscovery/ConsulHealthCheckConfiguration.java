package com.droid.bss.infrastructure.servicediscovery;

import com.ecwid.consul.v1.ConsulClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Consul Health Check Configuration
 * Integrates Spring Boot Actuator health checks with Consul service discovery
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "bss.servicediscovery.enabled", havingValue = "true")
public class ConsulHealthCheckConfiguration {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "consul-health-check");
        t.setDaemon(true);
        return t;
    });

    @Bean
    public HealthIndicator consulHealthIndicator(
            ConsulServiceProperties properties,
            ConsulClient consulClient) {

        return () -> {
            try {
                // Check Consul server availability
                log.trace("Checking Consul server health");
                var status = consulClient.getStatusResponse();

                Health health = Health.up()
                    .withDetail("consul.available", true)
                    .withDetail("consul.responseTime", status.getConsulResponseTime().toString())
                    .build();

                // If health check URL is configured, verify it
                if (properties.isEnableHealthCheck() && properties.getHealthCheckPath() != null) {
                    checkHealthCheckUrl(properties, health);
                }

                return health;

            } catch (Exception e) {
                log.warn("Consul health check failed", e);
                return Health.down()
                    .withDetail("consul.available", false)
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    private void checkHealthCheckUrl(ConsulServiceProperties properties, Health health) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String healthCheckUrl = "http://localhost:" + properties.getServicePort() + properties.getHealthCheckPath();

            long startTime = System.currentTimeMillis();
            var response = restTemplate.getForEntity(healthCheckUrl, String.class);
            long duration = System.currentTimeMillis() - startTime;

            if (response.getStatusCode() == HttpStatus.OK) {
                health.withDetail("healthCheck.available", true)
                    .withDetail("healthCheck.url", healthCheckUrl)
                    .withDetail("healthCheck.duration", duration + "ms");
            } else {
                health.withDetail("healthCheck.available", false)
                    .withDetail("healthCheck.status", response.getStatusCode().toString());
            }

        } catch (RestClientException e) {
            log.trace("Health check URL not available: {}", e.getMessage());
            health.withDetail("healthCheck.available", false)
                .withDetail("healthCheck.error", e.getMessage());
        }
    }

    @Bean
    public ConsulHealthCheckScheduler healthCheckScheduler(
            ConsulServiceProperties properties,
            ConsulClient consulClient) {

        return new ConsulHealthCheckScheduler(properties, consulClient);
    }

    /**
     * Scheduler for periodic health checks and service updates
     */
    public static class ConsulHealthCheckScheduler {

        private final ConsulServiceProperties properties;
        private final ConsulClient consulClient;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        public ConsulHealthCheckScheduler(ConsulServiceProperties properties, ConsulClient consulClient) {
            this.properties = properties;
            this.consulClient = consulClient;

            if (properties.isEnableHeartbeat()) {
                scheduleHeartbeat();
            }
        }

        private void scheduleHeartbeat() {
            long intervalSeconds = properties.getHeartbeatInterval().getSeconds();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // Ping Consul to keep the service registration alive
                    log.trace("Sending heartbeat to Consul");
                    var response = consulClient.getStatusResponse();
                    log.trace("Heartbeat successful, response time: {}",
                        response.getConsulResponseTime().toString());

                } catch (Exception e) {
                    log.warn("Heartbeat failed", e);
                }
            }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }

        public void shutdown() {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
