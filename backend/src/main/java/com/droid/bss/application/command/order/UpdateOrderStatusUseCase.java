package com.droid.bss.application.command.order;

import com.droid.bss.application.dto.order.UpdateOrderStatusCommand;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Transactional
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Update order status
     */
    public void handle(UpdateOrderStatusCommand command) {
        // Find order
        OrderEntity order = orderRepository.findById(command.id())
            .orElseThrow(() -> new IllegalArgumentException("Order with ID %s not found".formatted(command.id())));

        // Verify version for optimistic locking
        if (!order.getVersion().equals(command.version())) {
            throw new IllegalArgumentException(
                "Order version mismatch. Expected %d but found %d".formatted(command.version(), order.getVersion())
            );
        }

        // Validate status transition
        validateStatusTransition(order.getStatus(), command.status());

        // Update status
        order.setStatus(command.status());

        // Set completed date if status is COMPLETED
        if (command.status() == com.droid.bss.domain.order.OrderStatus.COMPLETED) {
            order.setCompletedDate(LocalDate.now());
        }

        // Save order
        orderRepository.save(order);
    }

    /**
     * Validate that the status transition is allowed
     */
    private void validateStatusTransition(
        com.droid.bss.domain.order.OrderStatus currentStatus,
        com.droid.bss.domain.order.OrderStatus newStatus
    ) {
        // Define allowed transitions
        boolean isValidTransition = switch (currentStatus) {
            case DRAFT -> newStatus == com.droid.bss.domain.order.OrderStatus.PENDING
                || newStatus == com.droid.bss.domain.order.OrderStatus.CANCELLED;
            case PENDING -> newStatus == com.droid.bss.domain.order.OrderStatus.APPROVED
                || newStatus == com.droid.bss.domain.order.OrderStatus.REJECTED
                || newStatus == com.droid.bss.domain.order.OrderStatus.CANCELLED;
            case APPROVED -> newStatus == com.droid.bss.domain.order.OrderStatus.IN_PROGRESS
                || newStatus == com.droid.bss.domain.order.OrderStatus.PROCESSING
                || newStatus == com.droid.bss.domain.order.OrderStatus.CANCELLED;
            case IN_PROGRESS, PROCESSING -> newStatus == com.droid.bss.domain.order.OrderStatus.COMPLETED
                || newStatus == com.droid.bss.domain.order.OrderStatus.CANCELLED;
            case REJECTED, CANCELLED, COMPLETED ->
                false; // Terminal states
        };

        if (!isValidTransition) {
            throw new IllegalArgumentException(
                "Invalid status transition from %s to %s".formatted(currentStatus, newStatus)
            );
        }
    }
}
