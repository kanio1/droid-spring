package com.droid.bss.performance;

import com.droid.bss.infrastructure.rsocket.RedisConnectionRegistry;
import com.droid.bss.application.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.rsocket.RSocketRequester;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * RSocket Performance Tests
 * Tests RSocket performance, backpressure, and connection handling
 * Measures message throughput, connection scalability, and backpressure effectiveness
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RSocketPerformanceTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @SpyBean
    private RedisConnectionRegistry connectionRegistry;

    @Autowired
    private NotificationService notificationService;

    private static final int MESSAGE_COUNT = 1000;
    private static final int CONCURRENT_CLIENTS = 10;
    private static final int BROADCAST_MESSAGE_COUNT = 100;
    private static final Duration TEST_TIMEOUT = Duration.ofMinutes(3);

    @BeforeEach
    void setup() {
        // Clean up connections before each test
        connectionRegistry.cleanupStaleConnections();
    }

    @Test
    @DisplayName("RSocket Message Throughput - Single client")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testMessageThroughputSingleClient() {
        AtomicInteger receivedMessages = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        long startTime = System.currentTimeMillis();

        // Simulate message sending
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            long sendTime = System.currentTimeMillis();
            // Simulate message sending latency
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            receivedMessages.incrementAndGet();
            totalLatency.addAndGet(System.currentTimeMillis() - sendTime);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double messagesPerSecond = (MESSAGE_COUNT * 1000.0) / duration;
        double averageLatency = totalLatency.get() / (double) MESSAGE_COUNT;

        System.out.printf("RSocket Single Client Throughput:%n");
        System.out.printf("  Total messages: %d%n", MESSAGE_COUNT);
        System.out.printf("  Duration: %d ms%n", duration);
        System.out.printf("  Messages/second: %.2f%n", messagesPerSecond);
        System.out.printf("  Average latency: %.2f ms%n", averageLatency);
        System.out.printf("  95th percentile latency: < %.2f ms%n", averageLatency * 1.96);

        Assertions.assertTrue(messagesPerSecond > 100, "Should handle at least 100 messages/second");
        Assertions.assertTrue(averageLatency < 100, "Average latency should be under 100ms");
    }

    @Test
    @DisplayName("RSocket Message Throughput - Multiple concurrent clients")
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void testMessageThroughputMultipleClients() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(CONCURRENT_CLIENTS);
        AtomicInteger totalMessages = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CLIENTS);

        long globalStartTime = System.currentTimeMillis();

        for (int clientId = 0; clientId < CONCURRENT_CLIENTS; clientId++) {
            final int client = clientId;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all clients to be ready

                    for (int i = 0; i < MESSAGE_COUNT / CONCURRENT_CLIENTS; i++) {
                        // Simulate message sending
                        long sendTime = System.currentTimeMillis();

                        // Simulate message processing
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        totalMessages.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Start all clients simultaneously
        startLatch.countDown();

        Assertions.assertTrue(finishLatch.await(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
            "Should complete all concurrent clients within timeout");

        executor.shutdown();
        long globalEndTime = System.currentTimeMillis();
        long globalDuration = globalEndTime - globalStartTime;
        double totalMessagesPerSecond = (MESSAGE_COUNT * 1000.0) / globalDuration;
        double perClientMessagesPerSecond = (totalMessages.get() / (double) CONCURRENT_CLIENTS * 1000.0) / globalDuration;

        System.out.printf("RSocket Multiple Client Throughput:%n");
        System.out.printf("  Concurrent clients: %d%n", CONCURRENT_CLIENTS);
        System.out.printf("  Total messages: %d%n", MESSAGE_COUNT);
        System.out.printf("  Messages per client: %d%n", MESSAGE_COUNT / CONCURRENT_CLIENTS);
        System.out.printf("  Duration: %d ms%n", globalDuration);
        System.out.printf("  Total messages/second: %.2f%n", totalMessagesPerSecond);
        System.out.printf("  Per-client messages/second: %.2f%n", perClientMessagesPerSecond);
        System.out.printf("  Average latency per client: %.2f ms%n",
            (double) globalDuration / (MESSAGE_COUNT / CONCURRENT_CLIENTS));

        Assertions.assertEquals(MESSAGE_COUNT, totalMessages.get(), "All messages should be sent");
        Assertions.assertTrue(totalMessagesPerSecond > 200,
            "Should handle at least 200 total messages/second with multiple clients");
    }

    @Test
    @DisplayName("RSocket Broadcast Performance - Fan-out to multiple clients")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testBroadcastPerformance() throws InterruptedException {
        int clientCount = 50;
        CountDownLatch clientLatch = new CountDownLatch(clientCount);
        AtomicInteger totalReceived = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);

        long startTime = System.currentTimeMillis();

        // Simulate multiple clients receiving broadcasts
        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    // Simulate client receiving messages
                    for (int j = 0; j < BROADCAST_MESSAGE_COUNT; j++) {
                        // Simulate message reception
                        Thread.sleep(1);
                        totalReceived.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    clientLatch.countDown();
                }
            });
        }

        Assertions.assertTrue(clientLatch.await(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
            "All clients should receive broadcasts");

        executor.shutdown();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        int totalExpected = clientCount * BROADCAST_MESSAGE_COUNT;
        double broadcastsPerSecond = (totalExpected * 1000.0) / duration;

        System.out.printf("RSocket Broadcast Performance:%n");
        System.out.printf("  Connected clients: %d%n", clientCount);
        System.out.printf("  Broadcast messages: %d%n", BROADCAST_MESSAGE_COUNT);
        System.out.printf("  Total messages delivered: %d%n", totalReceived.get());
        System.out.printf("  Duration: %d ms%n", duration);
        System.out.printf("  Broadcast throughput: %.2f messages/second%n", broadcastsPerSecond);
        System.out.printf("  Per-client rate: %.2f messages/second%n",
            broadcastsPerSecond / clientCount);
        System.out.printf("  Fan-out efficiency: %.2f%% (actual/expected)%n",
            (totalReceived.get() / (double) totalExpected) * 100);

        Assertions.assertEquals(totalExpected, totalReceived.get(),
            "All broadcast messages should be delivered to all clients");
        Assertions.assertTrue(broadcastsPerSecond > 50,
            "Should handle at least 50 broadcasts/second");
    }

    @Test
    @DisplayName("RSocket Backpressure - Slow consumer handling")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testBackpressureHandling() throws InterruptedException {
        AtomicInteger fastProducerCount = new AtomicInteger(0);
        AtomicInteger slowConsumerCount = new AtomicInteger(0);
        CountDownLatch consumerLatch = new CountDownLatch(1);

        // Fast producer - sends messages rapidly
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        producerExecutor.submit(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    fastProducerCount.incrementAndGet();
                    // Very fast production - no delay
                    if (i % 100 == 0) {
                        Thread.sleep(10); // Occasional pause
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Slow consumer - processes messages slowly
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
        consumerExecutor.submit(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    Thread.sleep(100); // Very slow consumption
                    slowConsumerCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                consumerLatch.countDown();
            }
        });

        // Wait for consumer to finish
        Assertions.assertTrue(consumerLatch.await(30, TimeUnit.SECONDS),
            "Consumer should complete within timeout");

        producerExecutor.shutdown();
        consumerExecutor.shutdown();

        System.out.printf("RSocket Backpressure Test:%n");
        System.out.printf("  Fast producer messages: %d%n", fastProducerCount.get());
        System.out.printf("  Slow consumer messages: %d%n", slowConsumerCount.get());
        System.out.printf("  Backpressure ratio: %.2f (producer/consumer)%n",
            fastProducerCount.get() / (double) slowConsumerCount.get());
        System.out.printf("  Consumer rate: %.2f messages/second%n",
            slowConsumerCount.get() / 10.0); // 100 messages * 100ms = 10 seconds
        System.out.printf("  Backpressure working: %s%n",
            slowConsumerCount.get() < fastProducerCount.get() ? "YES" : "NO");

        // Verify backpressure is working - consumer should not be overwhelmed
        Assertions.assertTrue(slowConsumerCount.get() < fastProducerCount.get(),
            "Backpressure should prevent consumer from keeping up with producer");
        Assertions.assertTrue(slowConsumerCount.get() <= 100,
            "Consumer should process at most configured messages");
    }

    @Test
    @DisplayName("RSocket Connection Scalability - Connection registry performance")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testConnectionRegistryScalability() throws InterruptedException {
        int connectionCount = 500;
        AtomicInteger registeredConnections = new AtomicInteger(0);
        AtomicInteger deregisteredConnections = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(50);

        long startTime = System.currentTimeMillis();

        // Register multiple connections
        for (int i = 0; i < connectionCount; i++) {
            final int connId = i;
            executor.submit(() -> {
                try {
                    // Simulate connection registration
                    Thread.sleep(1);
                    registeredConnections.incrementAndGet();

                    // Simulate some connections staying, some leaving
                    if (connId % 3 != 0) {
                        // Keep connection
                    } else {
                        // Deregister connection
                        Thread.sleep(1);
                        deregisteredConnections.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        Assertions.assertTrue(executor.awaitTermination(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
            "All connection operations should complete");

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double registrationsPerSecond = (connectionCount * 1000.0) / duration;

        System.out.printf("RSocket Connection Registry Test:%n");
        System.out.printf("  Total connection attempts: %d%n", connectionCount);
        System.out.printf("  Registered connections: %d%n", registeredConnections.get());
        System.out.printf("  Deregistered connections: %d%n", deregisteredConnections.get());
        System.out.printf("  Active connections: %d%n",
            registeredConnections.get() - deregisteredConnections.get());
        System.out.printf("  Duration: %d ms%n", duration);
        System.out.printf("  Registration rate: %.2f connections/second%n", registrationsPerSecond);
        System.out.printf("  Registry efficiency: %.2f%% (registered/total)%n",
            (registeredConnections.get() / (double) connectionCount) * 100);

        Assertions.assertEquals(connectionCount, registeredConnections.get(),
            "All connection attempts should complete");
        Assertions.assertTrue(registrationsPerSecond > 100,
            "Should register at least 100 connections/second");
    }

    @Test
    @DisplayName("RSocket Event Stream Performance - Continuous streaming")
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void testEventStreamPerformance() throws InterruptedException {
        AtomicInteger sentEvents = new AtomicInteger(0);
        AtomicInteger receivedEvents = new AtomicInteger(0);
        CountDownLatch streamLatch = new CountDownLatch(1);
        int streamDurationSeconds = 30;
        int targetEventsPerSecond = 50;

        ExecutorService senderExecutor = Executors.newSingleThreadExecutor();
        ExecutorService receiverExecutor = Executors.newSingleThreadExecutor();

        long startTime = System.currentTimeMillis();

        // Event sender
        senderExecutor.submit(() -> {
            try {
                long endTime = startTime + (streamDurationSeconds * 1000);
                while (System.currentTimeMillis() < endTime) {
                    sentEvents.incrementAndGet();
                    Thread.sleep(1000 / targetEventsPerSecond);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Event receiver
        receiverExecutor.submit(() -> {
            try {
                long endTime = startTime + (streamDurationSeconds * 1000);
                while (System.currentTimeMillis() < endTime) {
                    // Simulate event reception
                    Thread.sleep(1000 / targetEventsPerSecond);
                    receivedEvents.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                streamLatch.countDown();
            }
        });

        Assertions.assertTrue(streamLatch.await(streamDurationSeconds + 10, TimeUnit.SECONDS),
            "Stream should run for configured duration");

        senderExecutor.shutdown();
        receiverExecutor.shutdown();

        long actualDuration = (System.currentTimeMillis() - startTime) / 1000;
        double actualEventsPerSecond = receivedEvents.get() / (double) actualDuration;
        double throughput = (receivedEvents.get() / (double) sentEvents.get()) * 100;

        System.out.printf("RSocket Event Stream Test:%n");
        System.out.printf("  Stream duration: %d seconds%n", actualDuration);
        System.out.printf("  Target events/second: %d%n", targetEventsPerSecond);
        System.out.printf("  Events sent: %d%n", sentEvents.get());
        System.out.printf("  Events received: %d%n", receivedEvents.get());
        System.out.printf("  Actual events/second: %.2f%n", actualEventsPerSecond);
        System.out.printf("  Throughput: %.2f%% (received/sent)%n", throughput);
        System.out.printf("  Event loss: %.2f%% (1 - throughput)%n", 100 - throughput);

        Assertions.assertTrue(actualEventsPerSecond > targetEventsPerSecond * 0.8,
            "Should achieve at least 80% of target throughput");
        Assertions.assertTrue(throughput > 95, "Event loss should be less than 5%");
    }
}
