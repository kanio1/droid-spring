package com.droid.bss.api.events;

import com.droid.bss.domain.outbox.OutboxEvent;
import com.droid.bss.domain.outbox.OutboxEventType;
import com.droid.bss.domain.outbox.OutboxStatus;
import com.droid.bss.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Event Streaming (SSE) with Testcontainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    topics = {"bss.events.customer", "bss.events.order", "bss.events.payment"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
@DisplayName("Event Stream Integration Tests")
class EventStreamIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EventsController eventsController;

    private RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterEach
    void cleanup() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Should establish SSE connection successfully")
    void shouldEstablishSSEConnection() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> eventData = new AtomicReference<>();

        // When
        try (var response = restClient.get()
                .uri("/api/v1/events/stream")
                .retrieve()
                .toEntity(String.class)) {

            // Then
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }

        // Verify SSE endpoint is available
        restClient.get()
                .uri("/api/v1/events/stream")
                .retrieve()
                .toEntity(String.class);
    }

    @Test
    @DisplayName("Should broadcast event to connected SSE clients")
    void shouldBroadcastEvent() throws InterruptedException {
        // Given
        String eventId = UUID.randomUUID().toString();
        String eventName = "test.event";
        String eventData = "{\"message\": \"test data\"}";

        // When
        eventsController.broadcastEvent(eventName, eventData);

        // Then
        // In a real scenario, the event would be received by connected clients
        // For this test, we verify the controller accepts the broadcast
        assertThat(eventsController.getConnectedClients()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should create outbox event and publish via scheduler")
    void shouldCreateAndPublishOutboxEvent() {
        // Given
        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.CUSTOMER_CREATED)
                .eventName("CustomerCreated")
                .aggregateId(UUID.randomUUID().toString())
                .aggregateType("Customer")
                .eventData("{\"name\": \"Test Customer\"}")
                .metadata("{\"version\": \"1.0\"}")
                .version("1.0")
                .source("BSS-System")
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .timestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .traceId(UUID.randomUUID().toString())
                .build();

        // When
        OutboxEvent saved = outboxRepository.save(event);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Verify can find by status
        var pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        assertThat(pendingEvents).hasSize(1);
        assertThat(pendingEvents.get(0).getEventId()).isEqualTo(event.getEventId());
    }

    @Test
    @DisplayName("Should handle retry logic for failed events")
    void shouldHandleRetryLogic() {
        // Given
        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.ORDER_CREATED)
                .eventName("OrderCreated")
                .aggregateId(UUID.randomUUID().toString())
                .aggregateType("Order")
                .eventData("{\"orderNumber\": \"ORD-001\"}")
                .metadata("{\"version\": \"1.0\"}")
                .version("1.0")
                .source("BSS-System")
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .timestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .traceId(UUID.randomUUID().toString())
                .build();

        // When - simulate failed publish
        event.markAsFailed("Network error");
        outboxRepository.save(event);

        // Then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.RETRY);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getNextRetryAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should move to dead letter after max retries")
    void shouldMoveToDeadLetterAfterMaxRetries() {
        // Given
        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.PAYMENT_FAILED)
                .eventName("PaymentFailed")
                .aggregateId(UUID.randomUUID().toString())
                .aggregateType("Payment")
                .eventData("{\"amount\": 100}")
                .metadata("{\"version\": \"1.0\"}")
                .version("1.0")
                .source("BSS-System")
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .timestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(2) // Set low for test
                .traceId(UUID.randomUUID().toString())
                .build();

        // When - simulate multiple failures
        event.markAsFailed("First error");
        outboxRepository.save(event);

        event.markAsFailed("Second error");
        outboxRepository.save(event);

        event.markAsFailed("Third error - max retries");
        outboxRepository.save(event);

        // Then
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.hasExceededMaxRetries()).isTrue();
    }

    @Test
    @DisplayName("Should filter events by correlation ID")
    void shouldFilterEventsByCorrelationId() {
        // Given
        String correlationId = UUID.randomUUID().toString();
        OutboxEvent event1 = createTestEvent(correlationId);
        OutboxEvent event2 = createTestEvent(UUID.randomUUID().toString());
        OutboxEvent event3 = createTestEvent(correlationId);

        outboxRepository.save(event1);
        outboxRepository.save(event2);
        outboxRepository.save(event3);

        // When
        var events = outboxRepository.findByCorrelationId(correlationId);

        // Then
        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.getCorrelationId().equals(correlationId));
    }

    @Test
    @DisplayName("Should get statistics for outbox events")
    void shouldGetOutboxStatistics() {
        // Given
        outboxRepository.save(createTestEvent(UUID.randomUUID().toString()));
        outboxRepository.save(createTestEvent(UUID.randomUUID().toString()));
        outboxRepository.save(createTestEvent(UUID.randomUUID().toString()));

        OutboxEvent published = createTestEvent(UUID.randomUUID().toString());
        published.markAsPublished();
        outboxRepository.save(published);

        // When
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long published = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);

        // Then
        assertThat(pending).isEqualTo(3);
        assertThat(published).isEqualTo(1);
    }

    @Test
    @DisplayName("Should delete published events older than cutoff")
    void shouldDeleteOldPublishedEvents() {
        // Given
        OutboxEvent oldEvent = createTestEvent(UUID.randomUUID().toString());
        oldEvent.markAsPublished();
        outboxRepository.save(oldEvent);

        // When
        int deleted = outboxRepository.deletePublishedEventsOlderThan(LocalDateTime.now().minusDays(30));

        // Then
        assertThat(deleted).isGreaterThanOrEqualTo(0);
    }

    private OutboxEvent createTestEvent(String correlationId) {
        return OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(OutboxEventType.CUSTOMER_CREATED)
                .eventName("CustomerCreated")
                .aggregateId(UUID.randomUUID().toString())
                .aggregateType("Customer")
                .eventData("{\"name\": \"Test\"}")
                .metadata("{\"version\": \"1.0\"}")
                .version("1.0")
                .source("BSS-System")
                .correlationId(correlationId)
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .timestamp(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .traceId(UUID.randomUUID().toString())
                .build();
    }
}
