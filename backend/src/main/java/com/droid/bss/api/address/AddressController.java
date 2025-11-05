package com.droid.bss.api.address;

import com.droid.bss.application.command.address.*;
import com.droid.bss.application.dto.address.*;
import com.droid.bss.application.query.address.GetAddressUseCase;
import com.droid.bss.application.query.address.GetCustomerAddressesUseCase;
import com.droid.bss.application.query.address.ListAddressesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for address management
 * Provides CRUD operations for customer addresses
 */
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Address Management", description = "Operations for managing customer addresses")
public class AddressController {

    // Command use cases
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final ChangeAddressStatusUseCase changeAddressStatusUseCase;
    private final SetPrimaryAddressUseCase setPrimaryAddressUseCase;

    // Query use cases
    private final GetAddressUseCase getAddressUseCase;
    private final ListAddressesUseCase listAddressesUseCase;
    private final GetCustomerAddressesUseCase getCustomerAddressesUseCase;

    public AddressController(
            CreateAddressUseCase createAddressUseCase,
            UpdateAddressUseCase updateAddressUseCase,
            DeleteAddressUseCase deleteAddressUseCase,
            ChangeAddressStatusUseCase changeAddressStatusUseCase,
            SetPrimaryAddressUseCase setPrimaryAddressUseCase,
            GetAddressUseCase getAddressUseCase,
            ListAddressesUseCase listAddressesUseCase,
            GetCustomerAddressesUseCase getCustomerAddressesUseCase
    ) {
        this.createAddressUseCase = createAddressUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
        this.changeAddressStatusUseCase = changeAddressStatusUseCase;
        this.setPrimaryAddressUseCase = setPrimaryAddressUseCase;
        this.getAddressUseCase = getAddressUseCase;
        this.listAddressesUseCase = listAddressesUseCase;
        this.getCustomerAddressesUseCase = getCustomerAddressesUseCase;
    }

    // ========== CREATE OPERATIONS ==========

    @PostMapping
    @Operation(
        summary = "Create new address",
        description = "Creates a new address for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Address created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody CreateAddressCommand command) {
        AddressResponse response = createAddressUseCase.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== READ OPERATIONS ==========

    @GetMapping
    @Operation(
        summary = "List addresses",
        description = "Retrieves a paginated list of addresses with optional filtering"
    )
    @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<AddressListResponse> listAddresses(
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,

            @Parameter(description = "Filter by address type (BILLING, SHIPPING, SERVICE, CORRESPONDENCE)")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filter by status (ACTIVE, INACTIVE, PENDING)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by country code")
            @RequestParam(required = false) String country,

            @Parameter(description = "Search term (street, city, or region)")
            @RequestParam(required = false) String searchTerm,

            @Parameter(description = "Page number (default: 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field and direction (e.g., 'createdAt,desc')")
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        AddressListResponse response = listAddressesUseCase.handle(
                customerId, type, status, country, searchTerm, page, size, sort
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
        summary = "Get customer addresses",
        description = "Retrieves all non-deleted addresses for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Customer addresses retrieved successfully")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<AddressResponse>> getCustomerAddresses(
            @Parameter(description = "Customer ID")
            @PathVariable String customerId
    ) {
        List<AddressResponse> response = getCustomerAddressesUseCase.handle(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{addressId}")
    @Operation(
        summary = "Get address by ID",
        description = "Retrieves a specific address by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Address retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<AddressResponse> getAddress(
            @Parameter(description = "Address ID")
            @PathVariable String addressId
    ) {
        AddressResponse response = getAddressUseCase.handle(addressId);
        return ResponseEntity.ok(response);
    }

    // ========== UPDATE OPERATIONS ==========

    @PutMapping("/{addressId}")
    @Operation(
        summary = "Update address",
        description = "Updates an existing address with optimistic locking"
    )
    @ApiResponse(responseCode = "200", description = "Address updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or version conflict")
    @ApiResponse(responseCode = "404", description = "Address or customer not found")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "Address ID")
            @PathVariable String addressId,

            @Valid @RequestBody UpdateAddressCommand command
    ) {
        // Ensure the address ID in the path matches the one in the command
        if (!addressId.equals(command.id())) {
            return ResponseEntity.badRequest().build();
        }

        AddressResponse response = updateAddressUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{addressId}/status")
    @Operation(
        summary = "Change address status",
        description = "Changes the status of an address (ACTIVE, INACTIVE, PENDING)"
    )
    @ApiResponse(responseCode = "200", description = "Address status changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status or constraint violation")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddressResponse> changeAddressStatus(
            @Parameter(description = "Address ID")
            @PathVariable String addressId,

            @Valid @RequestBody ChangeAddressStatusCommand command
    ) {
        // Ensure the address ID in the path matches the one in the command
        if (!addressId.equals(command.id())) {
            return ResponseEntity.badRequest().build();
        }

        AddressResponse response = changeAddressStatusUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{addressId}/primary")
    @Operation(
        summary = "Set address as primary",
        description = "Sets an address as the primary address for its type"
    )
    @ApiResponse(responseCode = "200", description = "Address set as primary successfully")
    @ApiResponse(responseCode = "400", description = "Cannot set inactive address as primary")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<AddressResponse> setPrimaryAddress(
            @Parameter(description = "Address ID")
            @PathVariable String addressId,

            @Valid @RequestBody SetPrimaryAddressCommand command
    ) {
        // Ensure the address ID in the path matches the one in the command
        if (!addressId.equals(command.addressId())) {
            return ResponseEntity.badRequest().build();
        }

        AddressResponse response = setPrimaryAddressUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    // ========== DELETE OPERATIONS ==========

    @DeleteMapping("/{addressId}")
    @Operation(
        summary = "Delete address",
        description = "Soft-deletes an address (marks as deleted without removing from database)"
    )
    @ApiResponse(responseCode = "204", description = "Address deleted successfully")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "Address ID")
            @PathVariable String addressId
    ) {
        deleteAddressUseCase.handle(addressId);
        return ResponseEntity.noContent().build();
    }
}
