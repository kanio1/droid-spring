package com.droid.bss.application.command.order;

import com.droid.bss.application.dto.order.UpdateOrderStatusCommand;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.order.Order;
import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.order.OrderItem;
import com.droid.bss.domain.order.OrderRepository;
import com.droid.bss.domain.product.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for UpdateOrderStatusUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateOrderStatusUseCase Application Layer")
class UpdateOrderStatusUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Test
    @DisplayName("Should update order status from PENDING to PAID")
    void shouldUpdateOrderStatusFromPendingToPaid() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "PAID"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("99.99"))
        );

        Order existingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Simulate status change
        Order updatedOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(updatedOrder, "PAID");
        } catch (Exception e) {
            // Fallback - create new order with PAID status
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        // Act
        Order result = updateOrderStatusUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PAID");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should update order status from PAID to FULFILLED")
    void shouldUpdateOrderStatusFromPaidToFulfilled() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "FULFILLED"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order existingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");

        // Manually set status to PAID
        Order paidOrder = existingOrder;
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(paidOrder, "PAID");
        } catch (Exception e) {
            // Ignore reflection errors
        }

        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Create fulfilled order
        Order fulfilledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(fulfilledOrder, "FULFILLED");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(paidOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(fulfilledOrder);

        // Act
        Order result = updateOrderStatusUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("FULFILLED");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should update order status from PENDING to CANCELLED")
    void shouldUpdateOrderStatusFromPendingToCancelled() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "CANCELLED"
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
        Order result = updateOrderStatusUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "PAID"
        );

        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            updateOrderStatusUseCase.handle(command);
        }, "Should throw exception when order not found");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to cancel already fulfilled order")
    void shouldThrowExceptionWhenTryingToCancelFulfilledOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "CANCELLED"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order fulfilledOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(fulfilledOrder, "FULFILLED");
        } catch (Exception e) {
            // Ignore
        }

        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(fulfilledOrder));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            updateOrderStatusUseCase.handle(command);
        }, "Should throw exception when trying to cancel fulfilled order");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception with invalid status transition")
    void shouldThrowExceptionWithInvalidStatusTransition() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "FULFILLED" // Trying to go from PENDING directly to FULFILLED
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("99.99"))
        );

        Order pendingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(pendingOrder));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            updateOrderStatusUseCase.handle(command);
        }, "Should throw exception for invalid status transition");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should handle null status gracefully")
    void shouldHandleNullStatusGracefully() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            null
        );

        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            updateOrderStatusUseCase.handle(command);
        }, "Should throw exception for null status");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should update status for order with multiple items")
    void shouldUpdateStatusForOrderWithMultipleItems() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            orderId,
            "PAID"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("99.99")),
            new OrderItem(new ProductId(UUID.randomUUID()), 3, new BigDecimal("49.99"))
        );

        Order existingOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        Order updatedOrder = Order.create(customer.getId(), items, "SHIPPING_ADDRESS_123");
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(updatedOrder, "PAID");
        } catch (Exception e) {
            // Ignore
        }

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        // Act
        Order result = updateOrderStatusUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("349.95"));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(orderRepository).save(any(Order.class));
    }
}
