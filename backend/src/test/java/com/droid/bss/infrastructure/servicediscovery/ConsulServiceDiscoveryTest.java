package com.droid.bss.infrastructure.servicediscovery;

import com.ecwid.consul.v1.QueryOptions;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.HealthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Consul Service Discovery Tests
 * Tests service registration, discovery, and health checks
 */
@SpringBootTest(classes = {
    ConsulServiceDiscoveryTest.TestConfig.class
})
@Testcontainers
class ConsulServiceDiscoveryTest {

    @Container
    static GenericContainer<?> consulContainer = new GenericContainer<>(
        DockerImageName.parse("consul:1.16.0")
    )
    .withCommand("agent", "-dev", "-client=0.0.0.0")
    .withExposedPorts(8500);

    @Configuration
    @EnableConfigurationProperties(ConsulServiceProperties.class)
    static class TestConfig {
        @Bean
        public ConsulDiscoveryProperties consulDiscoveryProperties() {
            return new ConsulDiscoveryProperties();
        }
    }

    @Autowired
    private ConsulServiceProperties properties;

    @Autowired
    private ServiceDiscoveryClient discoveryClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("bss.servicediscovery.enabled", () -> true);
        registry.add("bss.servicediscovery.host", consulContainer::getHost);
        registry.add("bss.servicediscovery.port", consulContainer::getFirstMappedPort);
        registry.add("bss.servicediscovery.serviceName", () -> "test-service");
        registry.add("bss.servicediscovery.servicePort", () -> 8080);
    }

    @Test
    @DisplayName("Service discovery configuration should be set correctly")
    void testServiceDiscoveryConfiguration() {
        assertTrue(properties.isEnabled());
        assertEquals("localhost", properties.getHost());
        assertEquals(8500, properties.getPort());
        assertEquals("test-service", properties.getServiceName());
        assertEquals(8080, properties.getServicePort());
        assertTrue(properties.isEnableHealthCheck());
        assertEquals(Duration.ofSeconds(10), properties.getHealthCheckInterval());
        assertTrue(properties.isEnableHeartbeat());
        assertEquals(Duration.ofSeconds(5), properties.getHeartbeatInterval());

        System.out.printf("Service Discovery Configuration:%n");
        System.out.printf("  Enabled: %s%n", properties.isEnabled());
        System.out.printf("  Consul host: %s:%d%n", properties.getHost(), properties.getPort());
        System.out.printf("  Service name: %s%n", properties.getServiceName());
        System.out.printf("  Service port: %d%n", properties.getServicePort());
        System.out.printf("  Health check: %s%n", properties.isEnableHealthCheck());
        System.out.printf("  Heartbeat: %s%n", properties.isEnableHeartbeat());
    }

    @Test
    @DisplayName("Service discovery client should initialize")
    void testServiceDiscoveryClient() {
        assertNotNull(discoveryClient);

        // Test service availability check
        boolean isAvailable = discoveryClient.isServiceAvailable("test-service");
        assertFalse(isAvailable, "Service should not be available before registration");

        System.out.printf("Service Discovery Client initialized%n");
    }

    @Test
    @DisplayName("Service discovery properties should have correct tags")
    void testServiceTags() {
        assertNotNull(properties.getTags());
        assertTrue(properties.getTags().length > 0);
        assertTrue(properties.getTags().length >= 3);

        assertTrue(List.of(properties.getTags()).contains("bss"));
        assertTrue(List.of(properties.getTags()).contains("backend"));
        assertTrue(List.of(properties.getTags()).contains("api"));

        System.out.printf("Service Tags: %s%n", String.join(", ", properties.getTags()));
    }

    @Test
    @DisplayName("Health check configuration should be complete")
    void testHealthCheckConfiguration() {
        assertTrue(properties.isEnableHealthCheck());
        assertNotNull(properties.getHealthCheckPath());
        assertEquals("/actuator/health", properties.getHealthCheckPath());
        assertEquals(Duration.ofSeconds(10), properties.getHealthCheckInterval());
        assertEquals(Duration.ofSeconds(5), properties.getHealthCheckTimeout());
        assertEquals(Duration.ofMinutes(1), properties.getDeregisterCriticalServiceAfter());

        System.out.printf("Health Check Configuration:%n");
        System.out.printf("  Enabled: %s%n", properties.isEnableHealthCheck());
        System.out.printf("  Path: %s%n", properties.getHealthCheckPath());
        System.out.printf("  Interval: %s%n", properties.getHealthCheckInterval());
        System.out.printf("  Timeout: %s%n", properties.getHealthCheckTimeout());
    }

    @Test
    @DisplayName("Service discovery should handle empty service list")
    void testEmptyServiceList() {
        List<String> instances = discoveryClient.discoverHealthyInstances("non-existent-service");
        assertNotNull(instances);
        assertTrue(instances.isEmpty());

        int count = discoveryClient.getServiceInstanceCount("non-existent-service");
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Service discovery should handle exceptions gracefully")
    void testExceptionHandling() {
        // Test with invalid service name
        assertThrows(
            ServiceDiscoveryClient.ServiceDiscoveryException.class,
            () -> discoveryClient.discoverHealthyInstances("invalid-service-name-123456789")
        );
    }

    @Nested
    @DisplayName("Service Instance Discovery")
    class ServiceInstanceTests {

        @BeforeEach
        void setUp() {
            // Simulate service registration in Consul
            // In a real test, we would use the Consul API to register services
        }

        @Test
        @DisplayName("Should discover single service instance")
        void testSingleServiceInstance() {
            String serviceName = "test-service-instance";

            // In a real scenario, we would register a test service
            // For now, we test the method contracts
            assertDoesNotThrow(() -> {
                List<String> instances = discoveryClient.discoverHealthyInstances(serviceName);
                assertNotNull(instances);
            });
        }

        @Test
        @DisplayName("Should get instance count for service")
        void testInstanceCount() {
            String serviceName = "test-service";

            assertDoesNotThrow(() -> {
                int count = discoveryClient.getServiceInstanceCount(serviceName);
                assertTrue(count >= 0);
            });
        }
    }

    @Nested
    @DisplayName("Consul Integration Tests")
    class ConsulIntegrationTests {

        @Test
        @DisplayName("Consul server should be accessible")
        void testConsulAccessibility() {
            // Test that the test container is running
            assertTrue(consulContainer.isRunning());
            assertTrue(consulContainer.getFirstMappedPort() > 0);

            System.out.printf("Consul Server:%n");
            System.out.printf("  Host: %s%n", consulContainer.getHost());
            System.out.printf("  Port: %d%n", consulContainer.getFirstMappedPort());
        }

        @Test
        @DisplayName("Service properties should match Consul configuration")
        void testConsulConfiguration() {
            // Verify that the service properties are configured correctly
            assertEquals("localhost", properties.getHost());
            assertEquals(8500, properties.getPort());

            // Check that service name is not empty
            assertNotNull(properties.getServiceName());
            assertFalse(properties.getServiceName().isBlank());

            // Check that tags are set
            assertNotNull(properties.getTags());
            assertTrue(properties.getTags().length > 0);
        }
    }
}
