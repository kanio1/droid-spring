package com.droid.bss.application.dto.order;

import com.droid.bss.domain.order.OrderId;

import java.util.UUID;

/**
 * Query to get order by ID
 */
public record GetOrderByIdQuery(
    UUID orderId
) {
    public OrderId getOrderId() {
        return new OrderId(orderId);
    }
}
