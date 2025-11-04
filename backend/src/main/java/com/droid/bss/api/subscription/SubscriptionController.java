package com.droid.bss.api.subscription;

import com.droid.bss.application.command.subscription.ChangeSubscriptionStatusUseCase;
import com.droid.bss.application.command.subscription.CreateSubscriptionUseCase;
import com.droid.bss.application.command.subscription.DeleteSubscriptionUseCase;
import com.droid.bss.application.command.subscription.UpdateSubscriptionUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.subscription.ChangeSubscriptionStatusCommand;
import com.droid.bss.application.dto.subscription.CreateSubscriptionCommand;
import com.droid.bss.application.dto.subscription.SubscriptionResponse;
import com.droid.bss.application.dto.subscription.UpdateSubscriptionCommand;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscription", description = "Subscription management API")
@SecurityRequirement(name = "bearer-key")
public class SubscriptionController {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;
    private final UpdateSubscriptionUseCase updateSubscriptionUseCase;
    private final ChangeSubscriptionStatusUseCase changeSubscriptionStatusUseCase;
    private final DeleteSubscriptionUseCase deleteSubscriptionUseCase;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(
            CreateSubscriptionUseCase createSubscriptionUseCase,
            UpdateSubscriptionUseCase updateSubscriptionUseCase,
            ChangeSubscriptionStatusUseCase changeSubscriptionStatusUseCase,
            DeleteSubscriptionUseCase deleteSubscriptionUseCase,
            SubscriptionRepository subscriptionRepository) {
        this.createSubscriptionUseCase = createSubscriptionUseCase;
        this.updateSubscriptionUseCase = updateSubscriptionUseCase;
        this.changeSubscriptionStatusUseCase = changeSubscriptionStatusUseCase;
        this.deleteSubscriptionUseCase = deleteSubscriptionUseCase;
        this.subscriptionRepository = subscriptionRepository;
    }

    // Create
    @PostMapping
    @Operation(
        summary = "Create a new subscription",
        description = "Creates a new subscription with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "Subscription created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var subscriptionId = createSubscriptionUseCase.handle(command);
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found after creation"));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(subscriptionId.toString())
                .toUri();

        return ResponseEntity.created(location).body(SubscriptionResponse.from(subscription));
    }

    // Read single
    @GetMapping("/{id}")
    @Operation(
        summary = "Get subscription by ID",
        description = "Retrieves a single subscription by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Subscription found")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurity.checkSubscriptionAccess(#id, authentication)")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @Parameter(description = "Subscription ID", required = true) @PathVariable String id
    ) {
        return subscriptionRepository.findById(UUID.fromString(id))
                .map(SubscriptionResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Read all with pagination and sorting
    @GetMapping
    @Operation(
        summary = "Get all subscriptions",
        description = "Retrieves a paginated list of all subscriptions"
    )
    @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getAllSubscriptions(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<SubscriptionEntity> subscriptionPage = subscriptionRepository.findAll(pageable);

        PageResponse<SubscriptionResponse> response = PageResponse.of(
                subscriptionPage.getContent().stream()
                        .map(SubscriptionResponse::from)
                        .toList(),
                subscriptionPage.getNumber(),
                subscriptionPage.getSize(),
                subscriptionPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Read by customer
    @GetMapping("/by-customer/{customerId}")
    @Operation(
        summary = "Get subscriptions by customer ID",
        description = "Retrieves subscriptions for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.principal.getClaimAsString('customer_id')")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getSubscriptionsByCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String customerId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<SubscriptionEntity> subscriptionPage = subscriptionRepository.findByCustomerId(
                UUID.fromString(customerId), pageable);

        PageResponse<SubscriptionResponse> response = PageResponse.of(
                subscriptionPage.getContent().stream()
                        .map(SubscriptionResponse::from)
                        .toList(),
                subscriptionPage.getNumber(),
                subscriptionPage.getSize(),
                subscriptionPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Read by status
    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "Get subscriptions by status",
        description = "Retrieves subscriptions filtered by their status"
    )
    @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<SubscriptionResponse>> getSubscriptionsByStatus(
            @Parameter(description = "Subscription status (ACTIVE, SUSPENDED, CANCELLED, EXPIRED)", required = true) @PathVariable String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // This would need a custom query method - simplified here
        Page<SubscriptionEntity> subscriptionPage = subscriptionRepository.findAll(pageable)
                .map(subscription -> {
                    if (subscription.getStatus().name().equals(status)) {
                        return subscription;
                    }
                    return null;
                });

        PageResponse<SubscriptionResponse> response = PageResponse.of(
                subscriptionPage.getContent().stream()
                        .filter(subscription -> subscription != null)
                        .map(SubscriptionResponse::from)
                        .toList(),
                subscriptionPage.getNumber(),
                subscriptionPage.getSize(),
                subscriptionPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Search
    @GetMapping("/search")
    @Operation(
        summary = "Search subscriptions",
        description = "Searches subscriptions by subscription number"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<SubscriptionResponse>> searchSubscriptions(
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // This would need a custom query method - simplified here
        Page<SubscriptionEntity> subscriptionPage = subscriptionRepository.findAll(pageable)
                .map(subscription -> {
                    if (subscription.getSubscriptionNumber().toLowerCase().contains(searchTerm.toLowerCase())) {
                        return subscription;
                    }
                    return null;
                });

        PageResponse<SubscriptionResponse> response = PageResponse.of(
                subscriptionPage.getContent().stream()
                        .filter(subscription -> subscription != null)
                        .map(SubscriptionResponse::from)
                        .toList(),
                subscriptionPage.getNumber(),
                subscriptionPage.getSize(),
                subscriptionPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Update
    @PutMapping("/{id}")
    @Operation(
        summary = "Update subscription",
        description = "Updates an existing subscription with new details"
    )
    @ApiResponse(responseCode = "200", description = "Subscription updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @Parameter(description = "Subscription ID", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateSubscriptionCommand command
    ) {
        var updatedCommand = new UpdateSubscriptionCommand(
                id,
                command.endDate(),
                command.nextBillingDate(),
                command.price(),
                command.currency(),
                command.billingPeriod(),
                command.autoRenew()
        );

        SubscriptionEntity updatedSubscription = updateSubscriptionUseCase.handle(updatedCommand);
        SubscriptionResponse response = SubscriptionResponse.from(updatedSubscription);

        return ResponseEntity.ok(response);
    }

    // Change status
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Change subscription status",
        description = "Changes the status of an existing subscription"
    )
    @ApiResponse(responseCode = "200", description = "Subscription status changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> changeSubscriptionStatus(
            @Parameter(description = "Subscription ID", required = true) @PathVariable String id,
            @Valid @RequestBody ChangeSubscriptionStatusCommand command
    ) {
        var statusCommand = new ChangeSubscriptionStatusCommand(id, command.status(), command.reason());
        SubscriptionEntity updatedSubscription = changeSubscriptionStatusUseCase.handle(statusCommand);
        SubscriptionResponse response = SubscriptionResponse.from(updatedSubscription);

        return ResponseEntity.ok(response);
    }

    // Renew subscription
    @PutMapping("/{id}/renew")
    @Operation(
        summary = "Renew subscription",
        description = "Renews an existing subscription"
    )
    @ApiResponse(responseCode = "200", description = "Subscription renewed successfully")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> renewSubscription(
            @Parameter(description = "Subscription ID", required = true) @PathVariable String id
    ) {
        // Implementation would handle renewal logic
        // This is a placeholder
        throw new UnsupportedOperationException("Renew subscription not yet implemented");
    }

    // Delete
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete subscription",
        description = "Soft deletes a subscription by setting the deleted_at date"
    )
    @ApiResponse(responseCode = "204", description = "Subscription deleted successfully")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubscription(
            @Parameter(description = "Subscription ID", required = true) @PathVariable String id
    ) {
        boolean deleted = deleteSubscriptionUseCase.handle(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1].trim()), parts[0].trim());
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
