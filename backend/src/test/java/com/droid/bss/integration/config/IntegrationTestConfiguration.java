package com.droid.bss.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests
 * Provides PostgreSQL, Kafka, and Redis containers
 */
@TestConfiguration
@Testcontainers
public class IntegrationTestConfiguration {

    /**
     * PostgreSQL container for database testing
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("bss_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Kafka container for event testing
     */
    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    )
            .withExposedPorts(9093);

    /**
     * Redis container for caching and rate limiting
     */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server --appendonly yes");

    /**
     * Dynamic properties to configure Spring Boot application
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Flyway configuration to use Testcontainers database
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        // JPA Configuration - Use update for tests to allow schema changes
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // OAuth2 test configuration - Use simple JWT for tests
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8081/realms/bss");

        // Logging - Reduce noise in tests
        registry.add("logging.level.root", () -> "WARN");
        registry.add("logging.level.com.droid.bss", () -> "INFO");
    }
}
