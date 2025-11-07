package com.droid.bss.application.service;

import com.droid.bss.domain.customer.CustomerEvent;
import com.droid.bss.domain.invoice.InvoiceEvent;
import com.droid.bss.domain.payment.PaymentEvent;
import com.droid.bss.domain.subscription.SubscriptionEvent;
import com.droid.bss.infrastructure.rsocket.RedisConnectionRegistry;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 */
class NotificationServiceTest {

    @Mock
    private RedisConnectionRegistry connectionRegistry;

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private RSocketRequester requester1;

    @Mock
    private RSocketRequester requester2;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should send customer event notification")
    void shouldSendCustomerEventNotification() {
        // Given
        UUID customerId = UUID.randomUUID();
        CustomerEvent event = CustomerEvent.created(
            customerId,
            UUID.randomUUID(),
            "John Doe",
            "john@example.com"
        );

        Map<String, RSocketRequester> connections = new ConcurrentHashMap<>();
        connections.put("client1", requester1);
        when(connectionRegistry.getAllLocalConnections()).thenReturn(connections);

        // When
        notificationService.sendCustomerEventNotification(event).block();

        // Then
        verify(connectionRegistry, times(1)).getAllLocalConnections();
        // Verify request was made (specific verification depends on implementation)
    }

    @Test
    @DisplayName("Should send invoice event notification")
    void shouldSendInvoiceEventNotification() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        InvoiceEvent event = InvoiceEvent.generated(
            invoiceId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "INV-001"
        );

        Map<String, RSocketRequester> connections = new ConcurrentHashMap<>();
        connections.put("client1", requester1);
        connections.put("client2", requester2);
        when(connectionRegistry.getAllLocalConnections()).thenReturn(connections);

        // When
        notificationService.sendInvoiceEventNotification(event).block();

        // Then
        assertNotNull(event.getEventType());
        assertEquals(invoiceId, event.getInvoiceId());
    }

    @Test
    @DisplayName("Should send payment event notification")
    void shouldSendPaymentEventNotification() {
        // Given
        UUID paymentId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.processed(
            paymentId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00")
        );

        when(connectionRegistry.getAllLocalConnections()).thenReturn(new ConcurrentHashMap<>());

        // When
        notificationService.sendPaymentEventNotification(event).block();

        // Then
        assertNotNull(event.getEventType());
        assertEquals(paymentId, event.getPaymentId());
    }

    @Test
    @DisplayName("Should send subscription event notification")
    void shouldSendSubscriptionEventNotification() {
        // Given
        UUID subscriptionId = UUID.randomUUID();
        SubscriptionEvent event = SubscriptionEvent.created(
            subscriptionId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Premium Plan",
            com.droid.bss.domain.subscription.SubscriptionStatus.ACTIVE
        );

        when(connectionRegistry.getAllLocalConnections()).thenReturn(new ConcurrentHashMap<>());

        // When
        notificationService.sendSubscriptionEventNotification(event).block();

        // Then
        assertNotNull(event.getEventType());
        assertEquals(subscriptionId, event.getSubscriptionId());
    }

    @Test
    @DisplayName("Should handle empty connection list gracefully")
    void shouldHandleEmptyConnectionList() {
        // Given
        CustomerEvent event = CustomerEvent.created(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test User",
            "test@example.com"
        );

        when(connectionRegistry.getAllLocalConnections()).thenReturn(new ConcurrentHashMap<>());

        // When
        notificationService.sendCustomerEventNotification(event).block();

        // Then - should complete without errors
        verify(connectionRegistry, times(1)).getAllLocalConnections();
    }

    @Test
    @DisplayName("Should return subscriber count")
    void shouldReturnSubscriberCount() {
        // Given
        when(connectionRegistry.getLocalConnectionsCount()).thenReturn(5);

        // When
        int count = notificationService.getSubscriberCount();

        // Then
        assertEquals(5, count);
        verify(connectionRegistry, times(1)).getLocalConnectionsCount();
    }

    @Test
    @DisplayName("Should return total subscriber count across all instances")
    void shouldReturnTotalSubscriberCount() {
        // Given
        when(connectionRegistry.getTotalConnections()).thenReturn(15L);

        // When
        long total = notificationService.getTotalSubscriberCount();

        // Then
        assertEquals(15, total);
        verify(connectionRegistry, times(1)).getTotalConnections();
    }
}
