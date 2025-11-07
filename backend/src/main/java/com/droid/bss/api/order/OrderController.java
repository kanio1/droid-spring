package com.droid.bss.api.order;

import com.droid.bss.application.command.order.CreateOrderUseCase;
import com.droid.bss.application.command.order.UpdateOrderStatusUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.order.CreateOrderCommand;
import com.droid.bss.application.dto.order.OrderResponse;
import com.droid.bss.application.dto.order.UpdateOrderStatusCommand;
import com.droid.bss.application.query.order.OrderQueryService;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST Controller for Order CRUD operations
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "Order management API")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderQueryService orderQueryService;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            UpdateOrderStatusUseCase updateOrderStatusUseCase,
            OrderQueryService orderQueryService) {
        this.createOrderUseCase = createOrderUseCase;
        this.updateOrderStatusUseCase = updateOrderStatusUseCase;
        this.orderQueryService = orderQueryService;
    }

    // Create

    /**
     * Create a new order
     */
    @PostMapping
    @Operation(
        summary = "Create a new order",
        description = "Creates a new order with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Order number already exists")
    @Audited(action = AuditAction.ORDER_CREATE, entityType = "Order", description = "Creating new order")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var orderId = createOrderUseCase.handle(command);
        var order = orderQueryService.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found after creation"));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();

        return ResponseEntity.created(location).body(order);
    }

    // Read

    /**
     * Get order by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get order by ID",
        description = "Retrieves an order by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id
    ) {
        return orderQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all orders with pagination, search, and filters
     */
    @GetMapping
    @Operation(
        summary = "Get all orders",
        description = "Retrieves all orders with optional search and filters"
    )
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')")
            @RequestParam(defaultValue = "createdAt,desc") String sort,

            @Parameter(description = "Search term (order number or notes)")
            @RequestParam(required = false) String searchTerm,

            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Filter by order type")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId
    ) {
        PageResponse<OrderResponse> orders;

        if (searchTerm != null && !searchTerm.isBlank()) {
            orders = orderQueryService.searchOrders(searchTerm, page, size);
        } else if (status != null) {
            orders = orderQueryService.findByStatus(status, page, size);
        } else if (customerId != null) {
            orders = orderQueryService.findByCustomerId(customerId, page, size);
        } else {
            orders = orderQueryService.findAll(page, size);
        }

        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by status
     */
    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "Get orders by status",
        description = "Retrieves orders filtered by status"
    )
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByStatus(
            @Parameter(description = "Order status", required = true)
            @PathVariable OrderStatus status,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var orders = orderQueryService.findByStatus(status, page, size);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by customer ID
     */
    @GetMapping("/by-customer/{customerId}")
    @Operation(
        summary = "Get orders by customer",
        description = "Retrieves orders for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<OrderResponse>> getOrdersByCustomer(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable UUID customerId,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var orders = orderQueryService.findByCustomerId(customerId, page, size);
        return ResponseEntity.ok(orders);
    }

    /**
     * Search orders
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search orders",
        description = "Searches orders by order number or notes"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<OrderResponse>> searchOrders(
            @Parameter(description = "Search term", required = true)
            @RequestParam String searchTerm,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var orders = orderQueryService.searchOrders(searchTerm, page, size);
        return ResponseEntity.ok(orders);
    }

    // Update

    /**
     * Update order status
     */
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Update order status",
        description = "Updates the status of an order"
    )
    @ApiResponse(responseCode = "200", description = "Order status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status transition or input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @Audited(action = AuditAction.ORDER_UPDATE, entityType = "Order", description = "Updating status for order {id}")
    public ResponseEntity<Void> updateOrderStatus(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id,

            @Valid @RequestBody UpdateOrderStatusCommand command,

            @AuthenticationPrincipal Jwt principal
    ) {
        // Verify the ID in the path matches the ID in the command
        if (!id.toString().equals(command.id().toString())) {
            return ResponseEntity.badRequest().build();
        }

        updateOrderStatusUseCase.handle(command);
        return ResponseEntity.ok().build();
    }

    // Delete

    /**
     * Delete an order (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete an order",
        description = "Deletes an order by marking it as deleted (soft delete)"
    )
    @ApiResponse(responseCode = "204", description = "Order deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @Audited(action = AuditAction.ORDER_DELETE, entityType = "Order", description = "Deleting order {id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable UUID id,

            @AuthenticationPrincipal Jwt principal
    ) {
        // Delete order use case not yet implemented
        return ResponseEntity.status(501).build();
    }
}
