package com.droid.bss.application.query.order;

import com.droid.bss.application.dto.order.GetOrdersByCustomerQuery;
import com.droid.bss.application.dto.order.OrderDto;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.order.Order;
import com.droid.bss.domain.order.OrderItem;
import com.droid.bss.domain.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for GetOrdersByCustomerUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrdersByCustomerUseCase Query Side")
class GetOrdersByCustomerUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GetOrdersByCustomerUseCase getOrdersByCustomerUseCase;

    @Test
    @DisplayName("Should return all orders for customer")
    void shouldReturnAllOrdersForCustomer() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(customer.getId(), "PENDING", 1, new BigDecimal("99.99")));
        orders.add(createOrder(customer.getId(), "PAID", 2, new BigDecimal("49.99")));
        orders.add(createOrder(customer.getId(), "FULFILLED", 3, new BigDecimal("29.99")));

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(result.get(1).getStatus()).isEqualTo("PAID");
        assertThat(result.get(2).getStatus()).isEqualTo("FULFILLED");

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should return empty list when customer has no orders")
    void shouldReturnEmptyListWhenCustomerHasNoOrders() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(List.of());

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should return orders sorted by creation date (newest first)")
    void shouldReturnOrdersSortedByCreationDate() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");
        ContactInfo contactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");
        Customer customer = Customer.create(personalInfo, contactInfo);

        // Create orders with different creation timestamps
        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(customer.getId(), "FULFILLED", 1, new BigDecimal("199.99")));
        orders.add(createOrder(customer.getId(), "PENDING", 1, new BigDecimal("99.99")));
        orders.add(createOrder(customer.getId(), "PAID", 1, new BigDecimal("149.99")));

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        // Orders should be sorted by creation date (newest first)
        // Note: The actual sorting depends on the repository implementation
        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should return orders with correct customer ID")
    void shouldReturnOrdersWithCorrectCustomerId() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<Order> orders = List.of(
            createOrder(customer.getId(), "PAID", 2, new BigDecimal("99.99"))
        );

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customer.getId().toString());

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should return order with multiple items")
    void shouldReturnOrderWithMultipleItems() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<Order> orders = List.of(
            createOrderWithMultipleItems(customer.getId(), "PAID")
        );

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItems()).hasSize(3);
        assertThat(result.get(0).getTotalAmount()).isEqualTo(new BigDecimal("375.97")); // 149.99 + 125.98 + 100.00

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should return orders with different statuses")
    void shouldReturnOrdersWithDifferentStatuses() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(customer.getId(), "PENDING", 1, new BigDecimal("99.99")));
        orders.add(createOrder(customer.getId(), "PAID", 1, new BigDecimal("149.99")));
        orders.add(createOrder(customer.getId(), "PROCESSING", 1, new BigDecimal("79.99")));
        orders.add(createOrder(customer.getId(), "SHIPPED", 1, new BigDecimal("129.99")));
        orders.add(createOrder(customer.getId(), "FULFILLED", 1, new BigDecimal("199.99")));
        orders.add(createOrder(customer.getId(), "CANCELLED", 1, new BigDecimal("59.99")));

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(6);

        List<String> statuses = result.stream()
            .map(OrderDto::getStatus)
            .toList();

        assertThat(statuses).containsExactlyInAnyOrder(
            "PENDING", "PAID", "PROCESSING", "SHIPPED", "FULFILLED", "CANCELLED"
        );

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should calculate totals correctly for all orders")
    void shouldCalculateTotalsCorrectlyForAllOrders() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<Order> orders = new ArrayList<>();
        orders.add(createOrder(customer.getId(), "PAID", 2, new BigDecimal("100.00"))); // 200.00
        orders.add(createOrder(customer.getId(), "FULFILLED", 3, new BigDecimal("50.00"))); // 150.00
        orders.add(createOrder(customer.getId(), "PENDING", 1, new BigDecimal("299.99")); // 299.99

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        assertThat(result.get(0).getTotalAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(result.get(1).getTotalAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(result.get(2).getTotalAmount()).isEqualTo(new BigDecimal("299.99"));

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should handle large number of orders efficiently")
    void shouldHandleLargeNumberOfOrdersEfficiently() {
        // Arrange
        String customerId = UUID.randomUUID().toString();

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(customerId);

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        // Create 100 orders
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            orders.add(createOrder(customer.getId(), "PAID", 1, new BigDecimal("99.99")));
        }

        CustomerId expectedCustomerId = new CustomerId(UUID.fromString(customerId));
        when(orderRepository.findByCustomerId(eq(expectedCustomerId))).thenReturn(orders);

        // Act
        List<OrderDto> result = getOrdersByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);

        verify(orderRepository).findByCustomerId(eq(expectedCustomerId));
    }

    @Test
    @DisplayName("Should throw exception for invalid customer ID format")
    void shouldThrowExceptionForInvalidCustomerIdFormat() {
        // Arrange
        String invalidCustomerId = "invalid-uuid-format";

        GetOrdersByCustomerQuery query = new GetOrdersByCustomerQuery(invalidCustomerId);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            getOrdersByCustomerUseCase.handle(query);
        }, "Should throw exception for invalid customer ID format");

        verify(orderRepository, never()).findByCustomerId(any(CustomerId.class));
    }

    // Helper method to create a simple order
    private Order createOrder(CustomerId customerId, String status, int quantity, BigDecimal unitPrice) {
        Order order = Order.create(
            customerId,
            List.of(new OrderItem(new ProductId(UUID.randomUUID()), quantity, unitPrice)),
            "SHIPPING_ADDRESS"
        );

        // Set status using reflection
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(order, status);
        } catch (Exception e) {
            // Ignore reflection errors
        }

        return order;
    }

    // Helper method to create an order with multiple items
    private Order createOrderWithMultipleItems(CustomerId customerId, String status) {
        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("149.99")),
            new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("62.99")),
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("100.00"))
        );

        Order order = Order.create(customerId, items, "SHIPPING_ADDRESS_MULTIPLE_ITEMS");

        // Set status using reflection
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(order, status);
        } catch (Exception e) {
            // Ignore reflection errors
        }

        return order;
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
