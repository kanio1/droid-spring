package com.droid.bss.application.dto.order;

import com.droid.bss.domain.order.OrderItem;
import com.droid.bss.domain.order.OrderItemStatus;
import com.droid.bss.domain.order.OrderItemType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * OrderItem DTO for application layer
 */
public record OrderItemDto(
    String productId,
    String itemType,
    String itemCode,
    String itemName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal discountAmount,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal netAmount,
    BigDecimal finalAmount,
    String status,
    LocalDate activationDate,
    LocalDate expiryDate,
    Map<String, Object> configuration
) {

    /**
     * Creates OrderItemDto from OrderItem domain object
     */
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(
            item.getProductId().toString(),
            item.getItemType().name(),
            item.getItemCode(),
            item.getItemName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getDiscountAmount(),
            item.getTaxRate(),
            item.getTaxAmount(),
            item.getNetAmount(),
            item.getFinalAmount(),
            item.getStatus().name(),
            item.getActivationDate(),
            item.getExpiryDate(),
            null // Configuration not implemented yet
        );
    }
}
