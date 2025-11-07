package com.droid.bss.domain.order;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.product.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order - Aggregate Root Tests")
class OrderTest {

    @Test
    @DisplayName("should create order with single item")
    void shouldCreateOrderWithSingleItem() {
        // Given
        CustomerId customerId = CustomerId.generate();
        OrderItem item = createTestItem("Product A", 2, new BigDecimal("100.00"));
        String orderNumber = "ORDER-001";

        // When
        Order order = Order.create(customerId, orderNumber, item, OrderType.SERVICE);

        // Then
        assertThat(order).isNotNull();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("246.00")); // 2 * 123
    }

    @Test
    @DisplayName("should create order with multiple items")
    void shouldCreateOrderWithMultipleItems() {
        // Given
        CustomerId customerId = CustomerId.generate();
        OrderItem item1 = createTestItem("Product A", 1, new BigDecimal("100.00"));
        OrderItem item2 = createTestItem("Product B", 2, new BigDecimal("50.00"));
        List<OrderItem> items = List.of(item1, item2);
        String orderNumber = "ORDER-002";

        // When
        Order order = Order.create(customerId, orderNumber, items, OrderType.SERVICE);

        // Then
        assertThat(order).isNotNull();
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalAmount())
            .isEqualByComparingTo(new BigDecimal("123.00").add(new BigDecimal("123.00"))); // 246
    }

    @Test
    @DisplayName("should create order with requested date and channel")
    void shouldCreateOrderWithRequestedDateAndChannel() {
        // Given
        CustomerId customerId = CustomerId.generate();
        OrderItem item = createTestItem("Product A", 1, new BigDecimal("100.00"));
        String orderNumber = "ORDER-003";
        LocalDate requestedDate = LocalDate.now().plusDays(7);
        String channel = "ONLINE";

        // When
        Order order = Order.create(
            customerId,
            orderNumber,
            List.of(item),
            OrderType.SERVICE,
            requestedDate,
            channel
        );

        // Then
        assertThat(order.getRequestedDate()).isEqualTo(requestedDate);
        assertThat(order.getOrderChannel()).isEqualTo(channel);
    }

    @Test
    @DisplayName("should update order status from PENDING to APPROVED")
    void shouldUpdateOrderStatusFromPendingToApproved() {
        // Given
        Order order = createTestOrder();

        // When
        Order approved = order.changeStatus(OrderStatus.APPROVED);

        // Then
        assertThat(approved.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(approved.getVersion()).isEqualTo(2);
        assertThat(order.getVersion()).isEqualTo(1); // Original unchanged
    }

    @Test
    @DisplayName("should complete order successfully")
    void shouldCompleteOrderSuccessfully() {
        // Given
        Order order = createTestOrder();

        // When
        Order completed = order
            .changeStatus(OrderStatus.APPROVED)
            .changeStatus(OrderStatus.IN_PROGRESS)
            .changeStatus(OrderStatus.PROCESSING)
            .changeStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(completed.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.getVersion()).isEqualTo(5);
    }

    @Test
    @DisplayName("should cancel order successfully")
    void shouldCancelOrderSuccessfully() {
        // Given
        Order order = createTestOrder();
        String reason = "Customer request";

        // When
        Order cancelled = order.cancel(reason);

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getNotes()).isEqualTo(reason);
        assertThat(cancelled.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should add item to pending order")
    void shouldAddItemToPendingOrder() {
        // Given
        Order order = createTestOrder();
        OrderItem newItem = createTestItem("New Product", 1, new BigDecimal("200.00"));

        // When
        Order updated = order.addItem(newItem);

        // Then
        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getTotalAmount())
            .isGreaterThan(order.getTotalAmount());
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update item in order")
    void shouldUpdateItemInOrder() {
        // Given
        Order order = createTestOrder();
        OrderItem originalItem = order.getItems().get(0);
        OrderItem updatedItem = originalItem.updateQuantity(5);

        // When
        Order updated = order.updateItem(originalItem.getId(), updatedItem);

        // Then
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should remove item from order")
    void shouldRemoveItemFromOrder() {
        // Given
        OrderItem item1 = createTestItem("Product A", 1, new BigDecimal("100.00"));
        OrderItem item2 = createTestItem("Product B", 1, new BigDecimal("200.00"));
        Order order = Order.create(
            CustomerId.generate(),
            "ORDER-MULTI",
            List.of(item1, item2),
            OrderType.SERVICE
        );

        // When
        Order updated = order.removeItem(item1.getId());

        // Then
        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().get(0).getItemName()).isEqualTo("Product B");
    }

    @Test
    @DisplayName("should check if order is pending")
    void shouldCheckIfOrderIsPending() {
        // Given
        Order pendingOrder = createTestOrder();

        // Then
        assertThat(pendingOrder.isPending()).isTrue();
        assertThat(pendingOrder.isCompleted()).isFalse();
        assertThat(pendingOrder.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("should check if order is completed")
    void shouldCheckIfOrderIsCompleted() {
        // Given
        Order completedOrder = createTestOrder()
            .changeStatus(OrderStatus.APPROVED)
            .changeStatus(OrderStatus.IN_PROGRESS)
            .changeStatus(OrderStatus.PROCESSING)
            .changeStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(completedOrder.isCompleted()).isTrue();
        assertThat(completedOrder.isPending()).isFalse();
    }

    @Test
    @DisplayName("should check if order can be cancelled")
    void shouldCheckIfOrderCanBeCancelled() {
        // Given
        Order pendingOrder = createTestOrder();
        Order approvedOrder = pendingOrder.changeStatus(OrderStatus.APPROVED);
        Order inProgressOrder = approvedOrder.changeStatus(OrderStatus.IN_PROGRESS);
        Order completedOrder = inProgressOrder
            .changeStatus(OrderStatus.PROCESSING)
            .changeStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(pendingOrder.canBeCancelled()).isTrue();
        assertThat(approvedOrder.canBeCancelled()).isTrue();
        assertThat(inProgressOrder.canBeCancelled()).isTrue();
        assertThat(completedOrder.canBeCancelled()).isFalse();
    }

    @Test
    @DisplayName("should check if order can be modified")
    void shouldCheckIfOrderCanBeModified() {
        // Given
        Order pendingOrder = createTestOrder();

        // Then
        assertThat(pendingOrder.canBeModified()).isTrue();
    }

    @Test
    @DisplayName("should throw exception for invalid status transition")
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Given
        Order order = createTestOrder();

        // When & Then
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.COMPLETED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid transition from PENDING to COMPLETED");
    }

    @Test
    @DisplayName("should throw exception when adding item to non-pending order")
    void shouldThrowExceptionWhenAddingItemToNonPendingOrder() {
        // Given
        Order order = createTestOrder()
            .changeStatus(OrderStatus.APPROVED)
            .changeStatus(OrderStatus.IN_PROGRESS)
            .changeStatus(OrderStatus.PROCESSING)
            .changeStatus(OrderStatus.COMPLETED);
        OrderItem newItem = createTestItem("New Product", 1, new BigDecimal("200.00"));

        // When & Then
        assertThatThrownBy(() -> order.addItem(newItem))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot add items to order with status: COMPLETED");
    }

    @Test
    @DisplayName("should throw exception for empty items list")
    void shouldThrowExceptionForEmptyItemsList() {
        // Given
        CustomerId customerId = CustomerId.generate();
        String orderNumber = "ORDER-EMPTY";

        // When & Then
        assertThatThrownBy(() -> Order.create(
            customerId,
            orderNumber,
            List.of(),
            OrderType.SERVICE
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Order must have at least one item");
    }

    @Test
    @DisplayName("should throw exception when removing last item")
    void shouldThrowExceptionWhenRemovingLastItem() {
        // Given
        Order order = createTestOrder();
        OrderItem item = order.getItems().get(0);

        // When & Then
        assertThatThrownBy(() -> order.removeItem(item.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order must have at least one item");
    }

    @Test
    @DisplayName("should throw exception when updating non-existent item")
    void shouldThrowExceptionWhenUpdatingNonExistentItem() {
        // Given
        Order order = createTestOrder();
        OrderItem fakeItem = createTestItem("Fake", 1, new BigDecimal("100.00"));

        // When & Then
        assertThatThrownBy(() -> order.updateItem(UUID.randomUUID(), fakeItem))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Item not found");
    }

    @Test
    @DisplayName("should throw exception when cancelling completed order")
    void shouldThrowExceptionWhenCancellingCompletedOrder() {
        // Given
        Order order = createTestOrder()
            .changeStatus(OrderStatus.APPROVED)
            .changeStatus(OrderStatus.IN_PROGRESS)
            .changeStatus(OrderStatus.PROCESSING)
            .changeStatus(OrderStatus.COMPLETED);

        // When & Then
        assertThatThrownBy(() -> order.cancel("Test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Order with status COMPLETED cannot be cancelled");
    }

    @Test
    @DisplayName("should not allow transition from CANCELLED")
    void shouldNotAllowTransitionFromCancelled() {
        // Given
        Order order = createTestOrder().cancel("Test");

        // When & Then
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.PENDING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot transition from CANCELLED");
    }

    // Helper methods
    private OrderItem createTestItem(String name, Integer quantity, BigDecimal price) {
        return OrderItem.create(
            ProductId.generate(),
            OrderItemType.PRODUCT,
            "CODE-" + name.replace(" ", ""),
            name,
            quantity,
            price,
            BigDecimal.ZERO,
            new BigDecimal("23.00")
        );
    }

    private Order createTestOrder() {
        return Order.create(
            CustomerId.generate(),
            "TEST-ORDER-001",
            List.of(createTestItem("Test Product", 2, new BigDecimal("100.00"))),
            OrderType.SERVICE
        );
    }
}
