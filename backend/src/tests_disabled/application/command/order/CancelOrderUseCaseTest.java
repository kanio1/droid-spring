package com.droid.bss.application.command.order;

import com.droid.bss.application.dto.order.CancelOrderCommand;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.order.Order;
import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.order.OrderItem;
import com.droid.bss.domain.order.OrderRepository;
import com.droid.bss.domain.payment.Payment;
import com.droid.bss.domain.payment.PaymentRepository;
import com.droid.bss.domain.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for CancelOrderUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CancelOrderUseCase Application Layer")
class CancelOrderUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CancelOrderUseCase cancelOrderUseCase;

    @Test
    @DisplayName("Should cancel pending order successfully")
    void shouldCancelPendingOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer requested cancellation"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("99.99"))
        );

        Order existingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        Order cancelledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(cancelledOrder, "CANCELLED");
        } catch (Exception e) {
            // Ignore reflection errors
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // Act
        Order result = cancelOrderUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isEqualTo("Customer requested cancellation");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository, never()).findByOrderId(any(OrderId.class));
    }

    @Test
    @DisplayName("Should cancel order and process refund for paid order")
    void shouldCancelOrderAndProcessRefundForPaidOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        String paymentId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer changed mind"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order pendingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Set order status to PAID
        Order paidOrder = pendingOrder;
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(paidOrder, "PAID");
        } catch (Exception e) {
            // Ignore
        }

        Payment payment = Payment.create(
            expectedOrderId,
            new BigDecimal("99.99"),
            "CREDIT_CARD",
            PaymentStatus.COMPLETED
        );

        Order cancelledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(cancelledOrder, "CANCELLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(paidOrder));
        when(paymentRepository.findByOrderId(eq(expectedOrderId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // Act
        Order result = cancelOrderUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isEqualTo("Customer changed mind");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(paymentRepository).findByOrderId(eq(expectedOrderId));
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer requested cancellation"
        );

        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cancelOrderUseCase.handle(command);
        }, "Should throw exception when order not found");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).findByOrderId(any(OrderId.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel already fulfilled order")
    void shouldThrowExceptionWhenTryingToCancelFulfilledOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer requested cancellation"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order fulfilledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Set order status to FULFILLED
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(fulfilledOrder, "FULFILLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(fulfilledOrder));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cancelOrderUseCase.handle(command);
        }, "Should throw exception when trying to cancel fulfilled order");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).findByOrderId(any(OrderId.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel already cancelled order")
    void shouldThrowExceptionWhenTryingToCancelCancelledOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer requested cancellation"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order cancelledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Set order status to CANCELLED
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(cancelledOrder, "CANCELLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(cancelledOrder));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cancelOrderUseCase.handle(command);
        }, "Should throw exception when trying to cancel already cancelled order");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).findByOrderId(any(OrderId.class));
    }

    @Test
    @DisplayName("Should cancel order without reason")
    void shouldCancelOrderWithoutReason() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            null // No reason provided
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order existingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        Order cancelledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(cancelledOrder, "CANCELLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // Act
        Order result = cancelOrderUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isNull();

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository, never()).findByOrderId(any(OrderId.class));
    }

    @Test
    @DisplayName("Should handle cancellation of order with multiple items")
    void shouldHandleCancellationOfOrderWithMultipleItems() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer requested cancellation due to high price"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("100.00")),
            new OrderItem(new ProductId(UUID.randomUUID()), 3, new BigDecimal("50.00"))
        );

        Order existingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        Order cancelledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(cancelledOrder, "CANCELLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // Act
        Order result = cancelOrderUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isEqualTo("Customer requested cancellation due to high price");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("350.00"));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
        verify(paymentRepository, never()).findByOrderId(any(OrderId.class));
    }

    @Test
    @DisplayName("Should not process refund when no payment found for paid order")
    void shouldNotProcessRefundWhenNoPaymentFoundForPaidOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        CancelOrderCommand command = new CancelOrderCommand(
            orderId,
            "Customer requested cancellation"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order paidOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Set order status to PAID
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(paidOrder, "PAID");
        } catch (Exception e) {
            // Ignore
        }

        Order cancelledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(cancelledOrder, "CANCELLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(paidOrder));
        when(paymentRepository.findByOrderId(eq(expectedOrderId))).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // Act
        Order result = cancelOrderUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isEqualTo("Customer requested cancellation");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(paymentRepository).findByOrderId(eq(expectedOrderId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    // Helper class for ProductId
    private static class ProductId {
        private final UUID value;

        public ProductId(UUID value) {
            this.value = value;
        }

        public UUID getValue() {
            return value;
        }
    }
}
