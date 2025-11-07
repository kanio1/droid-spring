package com.droid.bss.domain.order;

import com.droid.bss.domain.product.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItem - Domain Entity Tests")
class OrderItemTest {

    @Test
    @DisplayName("should create order item with valid data")
    void shouldCreateOrderItemWithValidData() {
        // Given
        ProductId productId = ProductId.generate();
        String itemName = "Test Product";
        Integer quantity = 2;
        BigDecimal unitPrice = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("23.00");

        // When
        OrderItem item = OrderItem.create(
            productId,
            OrderItemType.PRODUCT,
            "TEST-001",
            itemName,
            quantity,
            unitPrice,
            BigDecimal.ZERO,
            taxRate
        );

        // Then
        assertThat(item).isNotNull();
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getItemName()).isEqualTo(itemName);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getUnitPrice()).isEqualByComparingTo(unitPrice);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PENDING);
        assertThat(item.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("should create order item with default values")
    void shouldCreateOrderItemWithDefaultValues() {
        // Given
        ProductId productId = ProductId.generate();
        String itemName = "Default Product";
        Integer quantity = 1;
        BigDecimal unitPrice = new BigDecimal("50.00");

        // When
        OrderItem item = OrderItem.create(
            productId,
            itemName,
            quantity,
            unitPrice
        );

        // Then
        assertThat(item.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getTaxRate()).isEqualByComparingTo(new BigDecimal("23.00"));
        assertThat(item.getItemType()).isEqualTo(OrderItemType.PRODUCT);
    }

    @Test
    @DisplayName("should calculate total price correctly")
    void shouldCalculateTotalPriceCorrectly() {
        // Given
        ProductId productId = ProductId.generate();
        Integer quantity = 3;
        BigDecimal unitPrice = new BigDecimal("100.00");

        OrderItem item = OrderItem.create(
            productId,
            "Product",
            quantity,
            unitPrice
        );

        // When
        BigDecimal totalPrice = item.getTotalPrice();

        // Then
        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("should calculate net amount after discount")
    void shouldCalculateNetAmountAfterDiscount() {
        // Given
        ProductId productId = ProductId.generate();
        BigDecimal unitPrice = new BigDecimal("100.00");
        BigDecimal discount = new BigDecimal("20.00");

        OrderItem item = OrderItem.create(
            productId,
            OrderItemType.PRODUCT,
            "TEST-001",
            "Product",
            1,
            unitPrice,
            discount,
            new BigDecimal("23.00")
        );

        // When
        BigDecimal netAmount = item.getNetAmount();

        // Then
        assertThat(netAmount).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("should calculate tax amount correctly")
    void shouldCalculateTaxAmountCorrectly() {
        // Given
        ProductId productId = ProductId.generate();
        BigDecimal unitPrice = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("23.00");

        OrderItem item = OrderItem.create(
            productId,
            OrderItemType.PRODUCT,
            "TEST-001",
            "Product",
            1,
            unitPrice,
            BigDecimal.ZERO,
            taxRate
        );

        // When
        BigDecimal taxAmount = item.getTaxAmount();
        BigDecimal finalAmount = item.getFinalAmount();

        // Then
        assertThat(taxAmount).isEqualByComparingTo(new BigDecimal("23.00"));
        assertThat(finalAmount).isEqualByComparingTo(new BigDecimal("123.00"));
    }

    @Test
    @DisplayName("should update quantity successfully")
    void shouldUpdateQuantitySuccessfully() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem item = OrderItem.create(
            productId,
            "Product",
            1,
            new BigDecimal("100.00")
        );

        // When
        OrderItem updated = item.updateQuantity(5);

        // Then
        assertThat(updated.getQuantity()).isEqualTo(5);
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(item.getVersion()).isEqualTo(1); // Original unchanged
    }

    @Test
    @DisplayName("should update unit price successfully")
    void shouldUpdateUnitPriceSuccessfully() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem item = OrderItem.create(
            productId,
            "Product",
            1,
            new BigDecimal("100.00")
        );

        // When
        OrderItem updated = item.updateUnitPrice(new BigDecimal("150.00"));

        // Then
        assertThat(updated.getUnitPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should change status successfully")
    void shouldChangeStatusSuccessfully() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem item = OrderItem.create(
            productId,
            "Product",
            1,
            new BigDecimal("100.00")
        );

        // When
        OrderItem activated = item.changeStatus(OrderItemStatus.ACTIVE);

        // Then
        assertThat(activated.getStatus()).isEqualTo(OrderItemStatus.ACTIVE);
        assertThat(activated.getActivationDate()).isNotNull();
        assertThat(activated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if item is active")
    void shouldCheckIfItemIsActive() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem pendingItem = OrderItem.create(productId, "Product", 1, new BigDecimal("100.00"));
        OrderItem activeItem = pendingItem.changeStatus(OrderItemStatus.ACTIVE);

        // Then
        assertThat(pendingItem.isActive()).isFalse();
        assertThat(activeItem.isActive()).isTrue();
    }

    @Test
    @DisplayName("should check if item can be activated")
    void shouldCheckIfItemCanBeActivated() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem item = OrderItem.create(productId, "Product", 1, new BigDecimal("100.00"));

        // Then
        assertThat(item.canBeActivated()).isTrue();
    }

    @Test
    @DisplayName("should throw exception for null product ID")
    void shouldThrowExceptionForNullProductId() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create(
            null,
            "Product",
            1,
            new BigDecimal("100.00")
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("ProductId cannot be null");
    }

    @Test
    @DisplayName("should throw exception for negative quantity")
    void shouldThrowExceptionForNegativeQuantity() {
        // Given
        ProductId productId = ProductId.generate();

        // When & Then
        assertThatThrownBy(() -> OrderItem.create(
            productId,
            OrderItemType.PRODUCT,
            "TEST-001",
            "Product",
            -1,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("23.00")
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive");
    }

    @Test
    @DisplayName("should throw exception for zero quantity")
    void shouldThrowExceptionForZeroQuantity() {
        // Given
        ProductId productId = ProductId.generate();

        // When & Then
        assertThatThrownBy(() -> OrderItem.create(
            productId,
            "Product",
            0,
            new BigDecimal("100.00")
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Quantity must be positive");
    }

    @Test
    @DisplayName("should throw exception for negative unit price")
    void shouldThrowExceptionForNegativeUnitPrice() {
        // Given
        ProductId productId = ProductId.generate();

        // When & Then
        assertThatThrownBy(() -> OrderItem.create(
            productId,
            OrderItemType.PRODUCT,
            "TEST-001",
            "Product",
            1,
            new BigDecimal("-100.00"),
            BigDecimal.ZERO,
            new BigDecimal("23.00")
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Unit price cannot be negative");
    }

    @Test
    @DisplayName("should throw exception when updating quantity of non-pending item")
    void shouldThrowExceptionWhenUpdatingQuantityOfNonPendingItem() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem item = OrderItem.create(productId, "Product", 1, new BigDecimal("100.00"))
            .changeStatus(OrderItemStatus.ACTIVE);

        // When & Then
        assertThatThrownBy(() -> item.updateQuantity(5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot update quantity of non-pending item");
    }

    @Test
    @DisplayName("should throw exception when updating price of non-pending item")
    void shouldThrowExceptionWhenUpdatingPriceOfNonPendingItem() {
        // Given
        ProductId productId = ProductId.generate();
        OrderItem item = OrderItem.create(productId, "Product", 1, new BigDecimal("100.00"))
            .changeStatus(OrderItemStatus.ACTIVE);

        // When & Then
        assertThatThrownBy(() -> item.updateUnitPrice(new BigDecimal("150.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot update price of non-pending item");
    }
}
