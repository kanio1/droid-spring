package com.droid.bss.application.command.order;

import com.droid.bss.application.dto.order.CancelOrderCommand;
import com.droid.bss.domain.order.repository.OrderRepository;
import com.droid.bss.domain.order.OrderStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Stub class for CancelOrderUseCase
 * Minimal implementation for testing purposes
 */
@Component
@Transactional
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;

    public CancelOrderUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void handle(CancelOrderCommand command) {
        // Stub implementation
        System.out.println("Cancelling order " + command.orderId() + " with reason: " + command.reason());
    }
}
