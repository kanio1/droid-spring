package com.droid.bss.camunda.listener;

import com.droid.bss.domain.customer.event.CustomerCreatedEvent;
import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.payment.event.PaymentFailedEvent;
import com.droid.bss.domain.payment.PaymentEntity;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CamundaEventListener
 */
@ExtendWith(MockitoExtension.class)
class CamundaEventListenerTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ProcessInstance processInstance;

    private CamundaEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CamundaEventListener(runtimeService);
    }

    @Test
    void testHandleCustomerCreatedEvent() {
        // Given
        CustomerEntity customer = createTestCustomer("customer-123", "John", "Doe", "john@example.com");
        CustomerCreatedEvent event = new CustomerCreatedEvent(customer);

        when(runtimeService.startProcessInstanceByKey(
                eq("customerOnboarding"),
                eq("customer-123"),
                anyMap()
        )).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("process-456");

        // When
        assertDoesNotThrow(() -> listener.handleCustomerCreatedEvent(event));

        // Then
        verify(runtimeService).startProcessInstanceByKey(
                eq("customerOnboarding"),
                eq("customer-123"),
                argThat(variables -> {
                    assertEquals("customer-123", variables.get("customerId"));
                    assertEquals("John Doe", variables.get("customerName"));
                    assertEquals("john@example.com", variables.get("customerEmail"));
                    assertEquals(CustomerStatus.ACTIVE.toString(), variables.get("customer_status"));
                    assertEquals("basic,portal,support", variables.get("services"));
                    return true;
                })
        );
    }

    @Test
    void testHandlePaymentFailedEvent() {
        // Given
        PaymentEntity payment = createTestPayment("payment-456", 99.99, "USD", "card");
        PaymentFailedEvent event = new PaymentFailedEvent(payment, "Insufficient funds");

        when(runtimeService.startProcessInstanceByKey(
                eq("paymentFailedRecovery"),
                eq("payment-456"),
                anyMap()
        )).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("process-789");

        // When
        assertDoesNotThrow(() -> listener.handlePaymentFailedEvent(event));

        // Then
        verify(runtimeService).startProcessInstanceByKey(
                eq("paymentFailedRecovery"),
                eq("payment-456"),
                argThat(variables -> {
                    assertEquals("payment-456", variables.get("paymentId"));
                    assertEquals("customer-123", variables.get("customerId"));
                    assertEquals(99.99, (Double) variables.get("amount"));
                    assertEquals("USD", variables.get("currency"));
                    assertEquals("Insufficient funds", variables.get("reason"));
                    return true;
                })
        );
    }

    @Test
    void testHandleCustomerCreatedEventHandlesException() {
        // Given
        CustomerEntity customer = createTestCustomer("customer-999", "Test", "User", "test@example.com");
        CustomerCreatedEvent event = new CustomerCreatedEvent(customer);

        when(runtimeService.startProcessInstanceByKey(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Workflow error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> listener.handleCustomerCreatedEvent(event));
        assertEquals("Workflow error", exception.getMessage());
    }

    @Test
    void testHandlePaymentFailedEventHandlesException() {
        // Given
        PaymentEntity payment = createTestPayment("payment-999", 50.00, "EUR", "bank_transfer");
        PaymentFailedEvent event = new PaymentFailedEvent(payment, "Payment declined");

        when(runtimeService.startProcessInstanceByKey(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("Process error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> listener.handlePaymentFailedEvent(event));
        assertEquals("Process error", exception.getMessage());
    }

    private CustomerEntity createTestCustomer(String id, String firstName, String lastName, String email) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.fromString(id));
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setStatus(CustomerStatus.ACTIVE);
        return customer;
    }

    private PaymentEntity createTestPayment(String id, double amount, String currency, String method) {
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.fromString(id));
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setCurrency(currency);
        payment.setPaymentMethod(method);
        payment.setCustomer(createTestCustomer("customer-123", "John", "Doe", "john@example.com"));
        return payment;
    }
}
