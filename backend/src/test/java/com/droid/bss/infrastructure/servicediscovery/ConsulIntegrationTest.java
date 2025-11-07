package com.droid.bss.infrastructure.servicediscovery;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.HealthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.consul.serviceregistry.AutoServiceRegistration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Consul Service Discovery Integration Test
 * Tests complete integration with Consul including service registration and discovery
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ConsulIntegrationTest {

    @Container
    static GenericContainer<?> consulContainer = new GenericContainer<>(
        DockerImageName.parse("consul:1.16.0")
    )
    .withCommand("agent", "-dev", "-client=0.0.0.0", "-log-level=INFO")
    .withExposedPorts(8500)
    .withExposedPorts(8600);

    @LocalServerPort
    private int serverPort;

    @Autowired(required = false)
    private AutoServiceRegistration autoServiceRegistration;

    @Autowired
    private ConsulServiceProperties serviceProperties;

    @Autowired
    private ServiceDiscoveryClient discoveryClient;

    private ConsulClient consulClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("bss.servicediscovery.enabled", () -> true);
        registry.add("spring.cloud.consul.enabled", () -> true);
        registry.add("spring.cloud.consul.host", consulContainer::getHost);
        registry.add("spring.cloud.consul.port", consulContainer::getFirstMappedPort);
        registry.add("spring.cloud.consul.discovery.service-name", () -> "bss-backend-integration-test");
        registry.add("spring.cloud.consul.discovery.port", () -> 8080);
        registry.add("spring.cloud.consul.discovery.health-check-interval", () -> "5s");
        registry.add("spring.cloud.consul.discovery.heartbeat.enabled", () -> true);
        registry.add("bss.servicediscovery.host", consulContainer::getHost);
        registry.add("bss.servicediscovery.port", consulContainer::getFirstMappedPort);
        registry.add("bss.servicediscovery.service-name", () -> "bss-backend-integration-test");
    }

    @BeforeEach
    void setUp() {
        String consulHost = consulContainer.getHost();
        int consulPort = consulContainer.getFirstMappedPort();
        consulClient = new ConsulClient(consulHost, consulPort);

        // Wait for Consul to be ready
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                try {
                    Response<Void> response = consulClient.getStatusLeader();
                    return response.getValue() != null;
                } catch (Exception e) {
                    return false;
                }
            });
    }

    @Test
    @DisplayName("Service should register with Consul")
    void testServiceRegistration() {
        String serviceName = serviceProperties.getServiceName();

        // Wait for service to be registered
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response<List<CatalogService>> response = consulClient.getCatalogServices(
                    null
                );
                List<CatalogService> services = response.getValue().get(serviceName);

                assertNotNull(services, "Service should be registered in Consul");
            });

        // Verify service details
        Response<List<CatalogService>> response = consulClient.getCatalogService(
            serviceName,
            null
        );
        List<CatalogService> services = response.getValue();

        assertFalse(services.isEmpty(), "At least one service instance should be registered");
        CatalogService service = services.get(0);

        assertEquals(serviceName, service.getServiceName());
        assertTrue(service.getServicePort() > 0);

        System.out.printf("Service registered:%n");
        System.out.printf("  Name: %s%n", service.getServiceName());
        System.out.printf("  ID: %s%n", service.getServiceId());
        System.out.printf("  Address: %s%n", service.getServiceAddress());
        System.out.printf("  Port: %d%n", service.getServicePort());
        System.out.printf("  Tags: %s%n", service.getServiceTags());
    }

    @Test
    @DisplayName("Service health should be reported to Consul")
    void testServiceHealthCheck() {
        String serviceName = serviceProperties.getServiceName();

        // Wait for health check to be registered
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Response<List<HealthService>> response = consulClient.getHealthServiceInstances(
                    serviceName,
                    true,
                    null
                );
                List<HealthService> healthServices = response.getValue();

                assertFalse(healthServices.isEmpty(), "Service should have health check");
            });

        // Verify health check status
        Response<List<HealthService>> response = consulClient.getHealthServiceInstances(
            serviceName,
            true,
            null
        );
        List<HealthService> healthServices = response.getValue();

        HealthService healthService = healthServices.get(0);
        HealthService.Instance service = healthService.getService();

        assertNotNull(service, "Service instance should have health status");
        assertNotNull(healthService.getChecks(), "Health checks should be present");

        // Verify health check details
        boolean hasPassingCheck = healthService.getChecks().stream()
            .anyMatch(check -> "PASSING".equals(check.getStatus()));

        assertTrue(hasPassingCheck, "At least one health check should be passing");

        System.out.printf("Service health check:%n");
        System.out.printf("  Passing: %b%n", hasPassingCheck);
        healthService.getChecks().forEach(check -> {
            System.out.printf("    %s: %s%n", check.getName(), check.getStatus());
        });
    }

    @Test
    @DisplayName("Service discovery client should find registered services")
    void testServiceDiscovery() {
        String serviceName = serviceProperties.getServiceName();

        // Wait for service to be discoverable
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertTrue(discoveryClient.isServiceAvailable(serviceName),
                    "Service should be discoverable");
            });

        // Test discovering healthy instances
        List<String> instances = discoveryClient.discoverHealthyInstances(serviceName);

        assertNotNull(instances, "Service instances list should not be null");
        assertFalse(instances.isEmpty(), "At least one service instance should be found");

        System.out.printf("Discovered instances:%n");
        instances.forEach(instance -> System.out.printf("  - %s%n", instance));
    }

    @Test
    @DisplayName("Service discovery client should get correct instance count")
    void testInstanceCount() {
        String serviceName = serviceProperties.getServiceName();

        // Wait for service to be registered and discoverable
        await().atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int count = discoveryClient.getServiceInstanceCount(serviceName);
                assertTrue(count > 0, "Service instance count should be greater than 0");
            });

        int count = discoveryClient.getServiceInstanceCount(serviceName);
        System.out.printf("Service instance count: %d%n", count);
    }

    @Test
    @DisplayName("Service discovery should handle non-existent service")
    void testNonExistentService() {
        String nonExistentService = "non-existent-service-" + System.currentTimeMillis();

        // Test with non-existent service
        List<String> instances = discoveryClient.discoverHealthyInstances(nonExistentService);
        assertNotNull(instances, "Should return empty list for non-existent service");
        assertTrue(instances.isEmpty(), "Non-existent service should have no instances");

        int count = discoveryClient.getServiceInstanceCount(nonExistentService);
        assertEquals(0, count, "Non-existent service should have 0 instances");

        boolean isAvailable = discoveryClient.isServiceAvailable(nonExistentService);
        assertFalse(isAvailable, "Non-existent service should not be available");
    }

    @Test
    @DisplayName("Consul client should be properly configured")
    void testConsulClientConfiguration() {
        assertNotNull(serviceProperties, "Service properties should be configured");
        assertEquals(consulContainer.getHost(), serviceProperties.getHost());
        assertEquals(consulContainer.getFirstMappedPort(), serviceProperties.getPort());
        assertEquals("bss-backend-integration-test", serviceProperties.getServiceName());
        assertTrue(serviceProperties.isEnabled(), "Service discovery should be enabled");

        System.out.printf("Consul configuration:%n");
        System.out.printf("  Host: %s:%d%n", serviceProperties.getHost(), serviceProperties.getPort());
        System.out.printf("  Service: %s%n", serviceProperties.getServiceName());
        System.out.printf("  Enabled: %s%n", serviceProperties.isEnabled());
    }

    @Nested
    @DisplayName("Service Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Service should have correct tags")
        void testServiceTags() {
            String serviceName = serviceProperties.getServiceName();

            await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response<List<CatalogService>> response = consulClient.getCatalogService(
                        serviceName,
                        null
                    );
                    List<CatalogService> services = response.getValue();

                    assertFalse(services.isEmpty());
                    CatalogService service = services.get(0);

                    assertNotNull(service.getServiceTags());
                    assertTrue(service.getServiceTags().contains("bss"));

                    System.out.printf("Service tags: %s%n",
                        String.join(", ", service.getServiceTags()));
                });
        }

        @Test
        @DisplayName("Service should have metadata")
        void testServiceMetadata() {
            String serviceName = serviceProperties.getServiceName();

            await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response<List<CatalogService>> response = consulClient.getCatalogService(
                        serviceName,
                        null
                    );
                    List<CatalogService> services = response.getValue();

                    assertFalse(services.isEmpty());
                    CatalogService service = services.get(0);

                    // Check that service has metadata
                    assertNotNull(service.getServiceMeta());

                    System.out.printf("Service metadata: %s%n", service.getServiceMeta());
                });
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Health check should be periodic")
        void testPeriodicHealthCheck() {
            String serviceName = serviceProperties.getServiceName();

            await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response<List<HealthService>> response = consulClient.getHealthServiceInstances(
                        serviceName,
                        true,
                        null
                    );
                    List<HealthService> healthServices = response.getValue();

                    assertFalse(healthServices.isEmpty());

                    // Should have multiple health check events
                    HealthService healthService = healthServices.get(0);
                    int checkCount = healthService.getChecks().size();

                    assertTrue(checkCount > 0, "Health check should be registered");

                    System.out.printf("Health check count: %d%n", checkCount);
                });
        }

        @Test
        @DisplayName("Health check should verify service endpoint")
        void testHealthCheckEndpoint() {
            String serviceName = serviceProperties.getServiceName();

            await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response<List<HealthService>> response = consulClient.getHealthServiceInstances(
                        serviceName,
                        true,
                        null
                    );
                    List<HealthService> healthServices = response.getValue();

                    assertFalse(healthServices.isEmpty());

                    HealthService healthService = healthServices.get(0);
                    HealthService.Instance service = healthService.getService();

                    // Verify that the service port is correct
                    assertEquals(8080, service.getPort());

                    System.out.printf("Health check service port: %d%n", service.getPort());
                });
        }
    }
}
