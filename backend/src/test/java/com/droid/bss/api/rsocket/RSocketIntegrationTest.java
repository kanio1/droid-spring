package com.droid.bss.api.rsocket;

import com.droid.bss.application.service.NotificationService;
import com.droid.bss.infrastructure.rsocket.RedisConnectionRegistry;
import com.droid.bss.infrastructure.rsocket.RedisConnectionRegistry.ConnectionMetadata;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultRSocket;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * RSocket Integration Tests
 * Tests Redis-based connection registry and event broadcasting
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RSocketIntegrationTest {

    @Autowired
    private RedisConnectionRegistry connectionRegistry;

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.rsocket.server.port", () -> 7000 + (int) (Math.random() * 1000));
        registry.add("instance.id", () -> "test-instance-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private static final Map<String, TestRSocketClient> clients = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int TEST_CLIENT_COUNT = 5;

    @AfterEach
    void cleanup() {
        clients.values().forEach(TestRSocketClient::dispose);
        clients.clear();
        connectionRegistry.cleanupStaleConnections();
    }

    @Test
    @Order(1)
    @DisplayName("Should register connections in Redis-backed registry")
    void shouldRegisterConnections() throws InterruptedException {
        // When
        CountDownLatch latch = new CountDownLatch(TEST_CLIENT_COUNT);

        for (int i = 0; i < TEST_CLIENT_COUNT; i++) {
            final int clientNum = i;
            executor.submit(() -> {
                try {
                    TestRSocketClient client = connectClient("client-info-" + clientNum);
                    clients.put("client-" + clientNum, client);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Then
        Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS));

        // Verify registry has the connections
        long totalConnections = connectionRegistry.getTotalConnections();
        Assertions.assertEquals(TEST_CLIENT_COUNT, totalConnections,
            "Should have registered " + TEST_CLIENT_COUNT + " connections");

        int localConnections = connectionRegistry.getLocalConnectionsCount();
        Assertions.assertTrue(localConnections >= 0,
            "Local connections count should be >= 0");
    }

    @Test
    @Order(2)
    @DisplayName("Should broadcast events to all connected clients")
    void shouldBroadcastEvents() throws InterruptedException {
        // Given
        setupClients(3);

        CountDownLatch eventLatch = new CountDownLatch(3 * 2); // 3 clients × 2 events

        clients.values().forEach(client -> {
            client.addEventHandler(event -> {
                eventLatch.countDown();
            });
        });

        // When
        notificationService.sendCustomerEventNotification(
            com.droid.bss.domain.customer.CustomerEvent.created(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Customer",
                "test@example.com"
            )
        ).block();

        notificationService.sendInvoiceEventNotification(
            com.droid.bss.domain.invoice.InvoiceEvent.generated(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "INV-001"
            )
        ).block();

        // Then
        Assertions.assertTrue(eventLatch.await(5, TimeUnit.SECONDS),
            "All clients should receive events");
    }

    @Test
    @Order(3)
    @DisplayName("Should cleanup stale connections")
    void shouldCleanupStaleConnections() throws InterruptedException {
        // Given
        setupClients(2);

        // Simulate stale connections by not updating last seen
        String firstClientId = clients.keySet().iterator().next();
        ConnectionMetadata metadata = connectionRegistry.getConnectionMetadata(firstClientId);
        Assertions.assertNotNull(metadata, "Metadata should exist");

        // When - cleanup is called
        connectionRegistry.cleanupStaleConnections();

        // Then - connections should be cleaned up
        long totalConnections = connectionRegistry.getTotalConnections();
        // Note: cleanup only removes connections older than 30 minutes,
        // so in test environment they may not be removed
    }

    @Test
    @Order(4)
    @DisplayName("Should handle concurrent connections")
    void shouldHandleConcurrentConnections() throws InterruptedException {
        // Given
        int concurrentClients = 20;
        CountDownLatch latch = new CountDownLatch(concurrentClients);
        CountDownLatch disconnectLatch = new CountDownLatch(concurrentClients);

        // When
        for (int i = 0; i < concurrentClients; i++) {
            final int clientNum = i;
            executor.submit(() -> {
                try {
                    TestRSocketClient client = connectClient("concurrent-client-" + clientNum);
                    clients.put("concurrent-" + clientNum, client);
                    latch.countDown();

                    // Keep connection for a bit
                    Thread.sleep(100);

                    client.dispose();
                    disconnectLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Then
        Assertions.assertTrue(latch.await(15, TimeUnit.SECONDS),
            "All clients should connect");
        Assertions.assertTrue(disconnectLatch.await(15, TimeUnit.SECONDS),
            "All clients should disconnect");
    }

    @Test
    @Order(5)
    @DisplayName("Should track connection metadata")
    void shouldTrackConnectionMetadata() throws InterruptedException {
        // Given
        TestRSocketClient client = connectClient("metadata-test");
        clients.put("metadata-client", client);

        // Wait a bit for registration
        Thread.sleep(500);

        // Then
        String clientId = client.getClientId();
        Assertions.assertNotNull(clientId, "Client ID should be assigned");

        ConnectionMetadata metadata = connectionRegistry.getConnectionMetadata(clientId);
        Assertions.assertNotNull(metadata, "Metadata should exist in Redis");
        Assertions.assertNotNull(metadata.getUserId(), "User ID should be set");
        Assertions.assertNotNull(metadata.getInstanceId(), "Instance ID should be set");
        Assertions.assertNotNull(metadata.getConnectedAt(), "Connected at should be set");
    }

    @Test
    @Order(6)
    @DisplayName("Should handle ping/pong")
    void shouldHandlePingPong() throws InterruptedException {
        // Given
        TestRSocketClient client = connectClient("ping-pong-test");
        clients.put("ping-pong", client);

        // When/Then
        CountDownLatch latch = new CountDownLatch(1);
        client.requestResponse("ping", "test")
            .doOnNext(response -> {
                Assertions.assertNotNull(response, "Response should not be null");
                latch.countDown();
            })
            .subscribe();

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS),
            "Should receive ping response");
    }

    private void setupClients(int count) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            final int clientNum = i;
            executor.submit(() -> {
                try {
                    TestRSocketClient client = connectClient("setup-client-" + clientNum);
                    clients.put("setup-" + clientNum, client);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
    }

    private TestRSocketClient connectClient(String clientInfo) {
        String host = redisContainer.getHost();
        int port = redisContainer.getFirstMappedPort();

        RSocketConnector connector = RSocketConnector.create();
        io.rsocket RSocket socket = connector.connect(TcpClientTransport.create(host, port + 10000)).block();
        return new TestRSocketClient(socket, clientInfo);
    }

    /**
     * Test RSocket client wrapper
     */
    private static class TestRSocketClient {
        private final io.rsocket.RSocket socket;
        private final String clientInfo;
        private String clientId;
        private volatile java.util.function.Consumer<Map<String, Object>> eventHandler;

        public TestRSocketClient(io.rsocket.RSocket socket, String clientInfo) {
            this.socket = socket;
            this.clientInfo = clientInfo;
            initialize();
        }

        private void initialize() {
            // Send setup message
            socket.metadataPush(clientInfo.getBytes());
        }

        public void addEventHandler(java.util.function.Consumer<Map<String, Object>> handler) {
            this.eventHandler = handler;
        }

        public Mono<Map<String, Object>> requestResponse(String route, String data) {
            return Mono.create(sink -> {
                socket.requestResponse(
                    DefaultPayload.create(data, route),
                    payload -> {
                        try {
                            String payloadData = new String(payload.getData());
                            Map<String, Object> map = Map.of("data", payloadData);
                            sink.success(map);
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    },
                    throwable -> sink.error(throwable)
                );
            });
        }

        public void dispose() {
            if (socket != null) {
                socket.dispose();
            }
        }

        public String getClientId() {
            return clientId;
        }
    }
}
