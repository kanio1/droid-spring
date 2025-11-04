package com.droid.bss.application.query.order;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.order.OrderResponse;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query service for order operations
 */
@Component
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Get order by ID
     */
    public Optional<OrderResponse> findById(UUID id) {
        return orderRepository.findById(id)
            .map(this::toOrderResponse);
    }

    /**
     * Get all orders with pagination
     */
    public PageResponse<OrderResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orderPage = orderRepository.findAll(pageable);

        List<OrderResponse> orders = orderPage.getContent().stream()
            .map(this::toOrderResponse)
            .collect(Collectors.toList());

        return PageResponse.of(
            orders,
            page,
            size,
            orderPage.getTotalElements()
        );
    }

    /**
     * Find orders by status
     */
    public PageResponse<OrderResponse> findByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orderPage = orderRepository.findByStatus(status, pageable);

        List<OrderResponse> orders = orderPage.getContent().stream()
            .map(this::toOrderResponse)
            .collect(Collectors.toList());

        return PageResponse.of(
            orders,
            page,
            size,
            orderPage.getTotalElements()
        );
    }

    /**
     * Find orders by customer ID
     */
    public PageResponse<OrderResponse> findByCustomerId(UUID customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orderPage = orderRepository.findByCustomerId(customerId, pageable);

        List<OrderResponse> orders = orderPage.getContent().stream()
            .map(this::toOrderResponse)
            .collect(Collectors.toList());

        return PageResponse.of(
            orders,
            page,
            size,
            orderPage.getTotalElements()
        );
    }

    /**
     * Search orders by order number or notes
     */
    public PageResponse<OrderResponse> searchOrders(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orderPage = orderRepository.searchOrders(searchTerm, pageable);

        List<OrderResponse> orders = orderPage.getContent().stream()
            .map(this::toOrderResponse)
            .collect(Collectors.toList());

        return PageResponse.of(
            orders,
            page,
            size,
            orderPage.getTotalElements()
        );
    }

    /**
     * Count orders by status
     */
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    /**
     * Convert OrderEntity to OrderResponse
     */
    private OrderResponse toOrderResponse(OrderEntity order) {
        String customerName = null;
        if (order.getCustomer() != null) {
            customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
        }

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomer() != null ? order.getCustomer().getId().toString() : null,
            customerName,
            order.getOrderType() != null ? order.getOrderType().name() : null,
            order.getOrderType() != null ? order.getOrderType().name() : null, // Display name would need enum method
            order.getStatus() != null ? order.getStatus().name() : null,
            order.getStatus() != null ? order.getStatus().name() : null, // Display name would need enum method
            order.getPriority() != null ? order.getPriority().name() : null,
            order.getPriority() != null ? order.getPriority().name() : null, // Display name would need enum method
            order.getTotalAmount(),
            order.getCurrency(),
            order.getRequestedDate(),
            order.getPromisedDate(),
            order.getCompletedDate(),
            order.getOrderChannel(),
            order.getSalesRepId(),
            null, // Sales rep name would need separate lookup
            order.getNotes(),
            order.isPending(),
            order.isCompleted(),
            order.canBeCancelled(),
            order.getItems() != null ? order.getItems().size() : 0,
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getCreatedBy(),
            order.getUpdatedBy(),
            order.getVersion() != null ? order.getVersion().longValue() : null
        );
    }
}
