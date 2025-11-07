package com.droid.bss.application.dto.order;

import com.droid.bss.domain.order.Order;
import com.droid.bss.domain.order.OrderItem;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.OrderType;
import com.droid.bss.domain.order.OrderPriority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order DTO for application layer
 */
public record OrderDto(
    String orderNumber,
    String customerId,
    String orderType,
    String status,
    String priority,
    BigDecimal totalAmount,
    String currency,
    LocalDate requestedDate,
    LocalDate promisedDate,
    String orderChannel,
    String salesRepId,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<OrderItemDto> items
) {

    /**
     * Creates OrderDto from Order domain object
     */
    public static OrderDto from(Order order) {
        return new OrderDto(
            order.getOrderNumber(),
            order.getCustomerId().toString(),
            order.getOrderType().name(),
            order.getStatus().name(),
            order.getPriority().name(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getRequestedDate(),
            order.getPromisedDate(),
            order.getOrderChannel(),
            order.getSalesRepId(),
            order.getNotes(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getItems().stream()
                .map(OrderItemDto::from)
                .collect(Collectors.toList())
        );
    }
}
