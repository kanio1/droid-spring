package com.droid.bss.application.dto.order;

import com.droid.bss.domain.customer.CustomerId;

import java.util.UUID;

/**
 * Query to get orders by customer
 */
public record GetOrdersByCustomerQuery(
    UUID customerId
) {
    public CustomerId getCustomerId() {
        return new CustomerId(customerId);
    }
}
