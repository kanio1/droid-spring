package com.droid.bss.api.customer;

import com.droid.bss.application.command.customer.ChangeCustomerStatusUseCase;
import com.droid.bss.application.command.customer.CreateCustomerUseCase;
import com.droid.bss.application.command.customer.DeleteCustomerUseCase;
import com.droid.bss.application.command.customer.UpdateCustomerUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.customer.ChangeCustomerStatusCommand;
import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.application.dto.customer.UpdateCustomerCommand;
import com.droid.bss.application.query.customer.CustomerQueryService;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.infrastructure.audit.Audited;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import com.droid.bss.infrastructure.security.RateLimiting;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer", description = "Customer management API")
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final ChangeCustomerStatusUseCase changeCustomerStatusUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;
    private final CustomerQueryService customerQueryService;
    private final BusinessMetrics businessMetrics;

    public CustomerController(
            CreateCustomerUseCase createCustomerUseCase,
            UpdateCustomerUseCase updateCustomerUseCase,
            ChangeCustomerStatusUseCase changeCustomerStatusUseCase,
            DeleteCustomerUseCase deleteCustomerUseCase,
            CustomerQueryService customerQueryService,
            BusinessMetrics businessMetrics) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.changeCustomerStatusUseCase = changeCustomerStatusUseCase;
        this.deleteCustomerUseCase = deleteCustomerUseCase;
        this.customerQueryService = customerQueryService;
        this.businessMetrics = businessMetrics;
    }
    
    // Create
    @PostMapping
    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "bss.customers.create.time", description = "Time to create customer")
    @Audited(action = AuditAction.CUSTOMER_CREATE, entityType = "Customer", description = "Creating new customer")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var customerId = createCustomerUseCase.handle(command);
        var customer = customerQueryService.findById(customerId.toString())
                .orElseThrow(() -> new RuntimeException("Customer not found after creation"));

        // Record business metrics
        businessMetrics.incrementCustomerCreated();

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customerId.toString())
                .toUri();

        return ResponseEntity.created(location).body(customer);
    }
    
    // Read single
    @GetMapping("/{id}")
    @Operation(
        summary = "Get customer by ID",
        description = "Retrieves a single customer by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "429", description = "Too Many Requests - Rate limit exceeded")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.getClaimAsString('customer_id')")
    @RateLimiting(value = 100, timeWindow = 60, keyPrefix = "customer_get")
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String id
    ) {
        return customerQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Read all with pagination, sorting, search, and status filter
    @GetMapping
    @Operation(
        summary = "Get all customers",
        description = "Retrieves a paginated list of all customers with optional search and status filter"
    )
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "429", description = "Too Many Requests - Rate limit exceeded")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiting(value = 50, timeWindow = 60, keyPrefix = "customer_list")
    public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Search term (name, email, PESEL, or NIP)") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by customer status") @RequestParam(required = false) String status
    ) {
        PageResponse<CustomerResponse> pageResponse;

        if (search != null && !search.isBlank()) {
            pageResponse = customerQueryService.search(search, page, size, sort);
        } else if (status != null && !status.isBlank()) {
            pageResponse = customerQueryService.findByStatus(status, page, size, sort);
        } else {
            pageResponse = customerQueryService.findAll(page, size, sort);
        }

        return ResponseEntity.ok(pageResponse);
    }
    
    // Read by status
    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "Get customers by status",
        description = "Retrieves customers filtered by their status"
    )
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "bss.customers.query_by_status.time", description = "Time to query customers by status")
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomersByStatus(
            @Parameter(description = "Customer status (ACTIVE, INACTIVE, BLOCKED)", required = true) @PathVariable String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = customerQueryService.findByStatus(status, page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }
    
    // Search
    @GetMapping("/search")
    @Operation(
        summary = "Search customers",
        description = "Searches customers by name, email, PESEL, or NIP"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "429", description = "Too Many Requests - Rate limit exceeded")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiting(value = 30, timeWindow = 60, keyPrefix = "customer_search")
    public ResponseEntity<PageResponse<CustomerResponse>> searchCustomers(
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = customerQueryService.search(searchTerm, page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }
    
    // Update
    @PutMapping("/{id}")
    @Operation(
        summary = "Update customer",
        description = "Updates an existing customer with new details"
    )
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.getClaimAsString('customer_id')")
    @Audited(action = AuditAction.CUSTOMER_UPDATE, entityType = "Customer", description = "Updating customer {id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateCustomerCommand command
    ) {
        var updatedCommand = new UpdateCustomerCommand(
                id,
                command.firstName(),
                command.lastName(),
                command.pesel(),
                command.nip(),
                command.email(),
                command.phone()
        );

        var updatedCustomer = updateCustomerUseCase.handle(updatedCommand);
        var response = CustomerResponse.from(updatedCustomer);

        // Record business metrics
        businessMetrics.incrementCustomerUpdated();

        return ResponseEntity.ok(response);
    }
    
    // Change status
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Change customer status",
        description = "Changes the status of an existing customer"
    )
    @ApiResponse(responseCode = "200", description = "Customer status changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = AuditAction.CUSTOMER_UPDATE, entityType = "Customer", description = "Changing status for customer {id}")
    public ResponseEntity<CustomerResponse> changeCustomerStatus(
            @Parameter(description = "Customer ID", required = true) @PathVariable String id,
            @Valid @RequestBody ChangeCustomerStatusCommand command
    ) {
        var statusCommand = new ChangeCustomerStatusCommand(id, command.status());
        var updatedCustomer = changeCustomerStatusUseCase.handle(statusCommand);
        var response = CustomerResponse.from(updatedCustomer);

        // Record business metrics
        businessMetrics.incrementCustomerStatusChanged();

        return ResponseEntity.ok(response);
    }
    
    // Delete
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete customer",
        description = "Soft deletes a customer by setting the deleted_at date"
    )
    @ApiResponse(responseCode = "204", description = "Customer deleted successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = AuditAction.CUSTOMER_DELETE, entityType = "Customer", description = "Deleting customer {id}")
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String id
    ) {
        boolean deleted = deleteCustomerUseCase.handle(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
