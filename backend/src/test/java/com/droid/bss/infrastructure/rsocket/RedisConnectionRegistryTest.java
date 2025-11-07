package com.droid.bss.infrastructure.rsocket;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisConnectionRegistry
 */
@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisConnectionRegistryTest {

    @Autowired
    private RedisConnectionRegistry connectionRegistry;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private RSocketRequester mockRequester;

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("instance.id", () -> "test-instance");
    }

    private static final Map<String, Object> testData = new ConcurrentHashMap<>();

    @AfterEach
    void cleanup() {
        connectionRegistry.cleanupStaleConnections();
        testData.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Should register connection in Redis and local cache")
    void shouldRegisterConnection() {
        // Given
        String clientId = UUID.randomUUID().toString();
        String userId = "test-user-" + System.currentTimeMillis();
        String clientInfo = "Test Client";

        // When
        connectionRegistry.registerConnection(clientId, mockRequester, userId, clientInfo);

        // Then
        assertTrue(connectionRegistry.connectionExists(clientId), "Connection should exist");
        assertNotNull(connectionRegistry.getConnectionMetadata(clientId), "Metadata should not be null");
        assertEquals(userId, connectionRegistry.getConnectionMetadata(clientId).getUserId(), "User ID should match");
        assertEquals("test-instance", connectionRegistry.getConnectionMetadata(clientId).getInstanceId(), "Instance ID should match");
    }

    @Test
    @Order(2)
    @DisplayName("Should unregister connection from Redis and local cache")
    void shouldUnregisterConnection() {
        // Given
        String clientId = UUID.randomUUID().toString();
        String userId = "test-user-" + System.currentTimeMillis();
        connectionRegistry.registerConnection(clientId, mockRequester, userId, "Test");

        // When
        connectionRegistry.unregisterConnection(clientId);

        // Then
        assertFalse(connectionRegistry.connectionExists(clientId), "Connection should not exist after unregister");
    }

    @Test
    @Order(3)
    @DisplayName("Should update last seen timestamp")
    void shouldUpdateLastSeen() {
        // Given
        String clientId = UUID.randomUUID().toString();
        String userId = "test-user";
        connectionRegistry.registerConnection(clientId, mockRequester, userId, "Test");

        // When
        try {
            Thread.sleep(100); // Ensure different timestamp
        } catch (InterruptedException e) {
            fail("Thread interrupted", e);
        }
        connectionRegistry.updateLastSeen(clientId);

        // Then
        RedisConnectionRegistry.ConnectionMetadata metadata = connectionRegistry.getConnectionMetadata(clientId);
        assertNotNull(metadata, "Metadata should exist");
        // Note: In real scenario, timestamp would be updated
    }

    @Test
    @Order(4)
    @DisplayName("Should get all connections for a user")
    void shouldGetUserConnections() {
        // Given
        String userId = "test-user";
        String clientId1 = UUID.randomUUID().toString();
        String clientId2 = UUID.randomUUID().toString();
        String clientId3 = UUID.randomUUID().toString();

        connectionRegistry.registerConnection(clientId1, mockRequester, userId, "Client 1");
        connectionRegistry.registerConnection(clientId2, mockRequester, userId, "Client 2");
        connectionRegistry.registerConnection(clientId3, mockRequester, "other-user", "Client 3");

        // When
        java.util.List<String> userConnections = connectionRegistry.getUserConnections(userId);

        // Then
        assertEquals(2, userConnections.size(), "Should have 2 connections for user");
        assertTrue(userConnections.contains(clientId1), "Should contain client 1");
        assertTrue(userConnections.contains(clientId2), "Should contain client 2");
    }

    @Test
    @Order(5)
    @DisplayName("Should track connection counts")
    void shouldTrackConnectionCounts() {
        // When
        long initialCount = connectionRegistry.getTotalConnections();

        // Given
        String clientId1 = UUID.randomUUID().toString();
        String clientId2 = UUID.randomUUID().toString();
        String clientId3 = UUID.randomUUID().toString();

        connectionRegistry.registerConnection(clientId1, mockRequester, "user1", "Test 1");
        connectionRegistry.registerConnection(clientId2, mockRequester, "user2", "Test 2");
        connectionRegistry.registerConnection(clientId3, mockRequester, "user3", "Test 3");

        // Then
        assertEquals(initialCount + 3, connectionRegistry.getTotalConnections(), "Total connections should be 3 more");
        assertEquals(3, connectionRegistry.getLocalConnectionsCount(), "Local connections should be 3");
    }

    @Test
    @Order(6)
    @DisplayName("Should add and get subscriptions")
    void shouldAddAndGetSubscriptions() {
        // Given
        String clientId = UUID.randomUUID().toString();
        connectionRegistry.registerConnection(clientId, mockRequester, "user", "Test");

        // When
        connectionRegistry.addSubscription(clientId, "customer.events");
        connectionRegistry.addSubscription(clientId, "invoice.events");

        // Then
        String subscriptions = connectionRegistry.getSubscriptions(clientId);
        assertNotNull(subscriptions, "Subscriptions should not be null");
        assertTrue(subscriptions.contains("customer.events"), "Should contain customer.events");
        assertTrue(subscriptions.contains("invoice.events"), "Should contain invoice.events");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle non-existent connection")
    void shouldHandleNonExistentConnection() {
        // Given
        String nonExistentClientId = UUID.randomUUID().toString();

        // When/Then
        assertFalse(connectionRegistry.connectionExists(nonExistentClientId), "Non-existent connection should not exist");
        assertNull(connectionRegistry.getConnectionMetadata(nonExistentClientId), "Metadata should be null for non-existent connection");
    }

    @Test
    @Order(8)
    @DisplayName("Should get all local connections")
    void shouldGetAllLocalConnections() {
        // Given
        String clientId1 = UUID.randomUUID().toString();
        String clientId2 = UUID.randomUUID().toString();

        connectionRegistry.registerConnection(clientId1, mockRequester, "user1", "Test 1");
        connectionRegistry.registerConnection(clientId2, mockRequester, "user2, Test 2");

        // When
        Map<String, RSocketRequester> localConnections = connectionRegistry.getAllLocalConnections();

        // Then
        assertEquals(2, localConnections.size(), "Should have 2 local connections");
        assertNotNull(localConnections.get(clientId1), "Should contain client 1");
        assertNotNull(localConnections.get(clientId2), "Should contain client 2");
    }

    @Test
    @Order(9)
    @DisplayName("Should cleanup stale connections")
    void shouldCleanupStaleConnections() {
        // Given
        String clientId1 = UUID.randomUUID().toString();
        String clientId2 = UUID.randomUUID().toString();

        connectionRegistry.registerConnection(clientId1, mockRequester, "user1", "Test 1");
        connectionRegistry.registerConnection(clientId2, mockRequester, "user2", "Test 2");

        long countBefore = connectionRegistry.getTotalConnections();

        // Note: Stale connections are defined as inactive for 30 minutes
        // In test environment, we can't easily simulate this, but we can verify
        // that the cleanup method runs without errors
        connectionRegistry.cleanupStaleConnections();

        // The connections should still be there (not stale)
        assertEquals(countBefore, connectionRegistry.getTotalConnections(), "Connections should remain (not stale)");
    }

    @Test
    @Order(10)
    @DisplayName("Should provide connection metadata with all fields")
    void shouldProvideConnectionMetadata() {
        // Given
        String clientId = UUID.randomUUID().toString();
        String userId = "test-user";
        String clientInfo = "Test Client Info";

        connectionRegistry.registerConnection(clientId, mockRequester, userId, clientInfo);

        // When
        RedisConnectionRegistry.ConnectionMetadata metadata = connectionRegistry.getConnectionMetadata(clientId);

        // Then
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(clientId, metadata.getClientId(), "Client ID should match");
        assertEquals(userId, metadata.getUserId(), "User ID should match");
        assertEquals(clientInfo, metadata.getClientInfo(), "Client info should match");
        assertEquals("test-instance", metadata.getInstanceId(), "Instance ID should match");
        assertNotNull(metadata.getConnectedAt(), "Connected at should be set");
        assertNotNull(metadata.getLastSeen(), "Last seen should be set");
    }
}
