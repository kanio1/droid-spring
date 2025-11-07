package com.droid.bss.application.query.order;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.order.OrderResponse;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
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
@CacheConfig(cacheNames = "orders")
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Get order by ID
     */
    @CircuitBreaker(name = "orderQueryService", fallbackMethod = "findByIdFallback")
    @Retry(name = "orderQueryService")
    @TimeLimiter(name = "orderQueryService")
    @Cacheable(key = "#id", unless = "#result == null")
    public Optional<OrderResponse> findById(UUID id) {
        return orderRepository.findById(id)
            .map(this::toOrderResponse);
    }

    public Optional<OrderResponse> findByIdFallback(UUID id, Exception ex) {
        // Circuit breaker is open - return empty result
        return Optional.empty();
    }

    /**
     * Get all orders with pagination
     */
    @CircuitBreaker(name = "orderQueryService", fallbackMethod = "findAllFallback")
    @Retry(name = "orderQueryService")
    @TimeLimiter(name = "orderQueryService")
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

    public PageResponse<OrderResponse> findAllFallback(int page, int size, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }

    /**
     * Find orders by status
     */
    @CircuitBreaker(name = "orderQueryService", fallbackMethod = "findByStatusFallback")
    @Retry(name = "orderQueryService")
    @TimeLimiter(name = "orderQueryService")
    @Cacheable(key = "{#status, #page, #size}")
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

    public PageResponse<OrderResponse> findByStatusFallback(OrderStatus status, int page, int size, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }

    /**
     * Find orders by customer ID
     */
    @CircuitBreaker(name = "orderQueryService", fallbackMethod = "findByCustomerIdFallback")
    @Retry(name = "orderQueryService")
    @TimeLimiter(name = "orderQueryService")
    @Cacheable(key = "{#customerId, #page, #size}")
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

    public PageResponse<OrderResponse> findByCustomerIdFallback(UUID customerId, int page, int size, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }

    /**
     * Search orders by order number or notes
     */
    @CircuitBreaker(name = "orderQueryService", fallbackMethod = "searchOrdersFallback")
    @Retry(name = "orderQueryService")
    @TimeLimiter(name = "orderQueryService")
    @Cacheable(key = "{#searchTerm, #page, #size}")
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

    public PageResponse<OrderResponse> searchOrdersFallback(String searchTerm, int page, int size, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }

    /**
     * Count orders by status
     */
    @CircuitBreaker(name = "orderQueryService", fallbackMethod = "countByStatusFallback")
    @Retry(name = "orderQueryService")
    @TimeLimiter(name = "orderQueryService")
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public long countByStatusFallback(OrderStatus status, Exception ex) {
        // Circuit breaker is open - return 0
        return 0;
    }

    /**
     * Convert OrderEntity to OrderResponse
     */
    private OrderResponse toOrderResponse(OrderEntity order) {
        String customerName = null;
        java.util.UUID customerId = null;
        if (order.getCustomer() != null) {
            customerName = order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
            customerId = order.getCustomer().getId();
        }

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            customerId,
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
