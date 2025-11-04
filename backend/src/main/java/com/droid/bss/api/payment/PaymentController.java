package com.droid.bss.api.payment;

import com.droid.bss.application.command.payment.ChangePaymentStatusUseCase;
import com.droid.bss.application.command.payment.CreatePaymentUseCase;
import com.droid.bss.application.command.payment.DeletePaymentUseCase;
import com.droid.bss.application.command.payment.UpdatePaymentUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.payment.ChangePaymentStatusCommand;
import com.droid.bss.application.dto.payment.CreatePaymentCommand;
import com.droid.bss.application.dto.payment.PaymentResponse;
import com.droid.bss.application.dto.payment.UpdatePaymentCommand;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.repository.PaymentRepository;
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
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Payment management API")
@SecurityRequirement(name = "bearer-key")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final UpdatePaymentUseCase updatePaymentUseCase;
    private final ChangePaymentStatusUseCase changePaymentStatusUseCase;
    private final DeletePaymentUseCase deletePaymentUseCase;
    private final PaymentRepository paymentRepository;

    public PaymentController(
            CreatePaymentUseCase createPaymentUseCase,
            UpdatePaymentUseCase updatePaymentUseCase,
            ChangePaymentStatusUseCase changePaymentStatusUseCase,
            DeletePaymentUseCase deletePaymentUseCase,
            PaymentRepository paymentRepository) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.updatePaymentUseCase = updatePaymentUseCase;
        this.changePaymentStatusUseCase = changePaymentStatusUseCase;
        this.deletePaymentUseCase = deletePaymentUseCase;
        this.paymentRepository = paymentRepository;
    }

    // Create
    @PostMapping
    @Operation(
        summary = "Create a new payment",
        description = "Creates a new payment with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "Payment created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var paymentId = createPaymentUseCase.handle(command);
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found after creation"));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(paymentId.toString())
                .toUri();

        return ResponseEntity.created(location).body(PaymentResponse.from(payment));
    }

    // Read single
    @GetMapping("/{id}")
    @Operation(
        summary = "Get payment by ID",
        description = "Retrieves a single payment by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Payment found")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.checkPaymentAccess(#id, authentication)")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable String id
    ) {
        return paymentRepository.findById(UUID.fromString(id))
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Read all with pagination and sorting
    @GetMapping
    @Operation(
        summary = "Get all payments",
        description = "Retrieves a paginated list of all payments"
    )
    @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> getAllPayments(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<PaymentEntity> paymentPage = paymentRepository.findAll(pageable);

        PageResponse<PaymentResponse> response = PageResponse.of(
                paymentPage.getContent().stream()
                        .map(PaymentResponse::from)
                        .toList(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Read by customer
    @GetMapping("/by-customer/{customerId}")
    @Operation(
        summary = "Get payments by customer ID",
        description = "Retrieves payments for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.principal.getClaimAsString('customer_id')")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentsByCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable String customerId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<PaymentEntity> paymentPage = paymentRepository.findByCustomerId(
                UUID.fromString(customerId), pageable);

        PageResponse<PaymentResponse> response = PageResponse.of(
                paymentPage.getContent().stream()
                        .map(PaymentResponse::from)
                        .toList(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Read by status
    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "Get payments by status",
        description = "Retrieves payments filtered by their status"
    )
    @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentsByStatus(
            @Parameter(description = "Payment status (PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED)", required = true) @PathVariable String status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // This would need a custom query method - simplified here
        Page<PaymentEntity> paymentPage = paymentRepository.findAll(pageable)
                .map(payment -> {
                    if (payment.getPaymentStatus().name().equals(status)) {
                        return payment;
                    }
                    return null;
                });

        PageResponse<PaymentResponse> response = PageResponse.of(
                paymentPage.getContent().stream()
                        .filter(payment -> payment != null)
                        .map(PaymentResponse::from)
                        .toList(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Search
    @GetMapping("/search")
    @Operation(
        summary = "Search payments",
        description = "Searches payments by payment number or reference"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> searchPayments(
            @Parameter(description = "Search term", required = true) @RequestParam String searchTerm,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // This would need a custom query method - simplified here
        Page<PaymentEntity> paymentPage = paymentRepository.findAll(pageable)
                .map(payment -> {
                    if (payment.getPaymentNumber().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        (payment.getReferenceNumber() != null &&
                         payment.getReferenceNumber().toLowerCase().contains(searchTerm.toLowerCase()))) {
                        return payment;
                    }
                    return null;
                });

        PageResponse<PaymentResponse> response = PageResponse.of(
                paymentPage.getContent().stream()
                        .filter(payment -> payment != null)
                        .map(PaymentResponse::from)
                        .toList(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    // Update
    @PutMapping("/{id}")
    @Operation(
        summary = "Update payment",
        description = "Updates an existing payment with new details"
    )
    @ApiResponse(responseCode = "200", description = "Payment updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> updatePayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable String id,
            @Valid @RequestBody UpdatePaymentCommand command
    ) {
        var updatedCommand = new UpdatePaymentCommand(
                id,
                command.amount(),
                command.currency(),
                command.paymentMethod(),
                command.paymentDate(),
                command.receivedDate(),
                command.referenceNumber(),
                command.notes()
        );

        PaymentEntity updatedPayment = updatePaymentUseCase.handle(updatedCommand);
        PaymentResponse response = PaymentResponse.from(updatedPayment);

        return ResponseEntity.ok(response);
    }

    // Change status
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Change payment status",
        description = "Changes the status of an existing payment"
    )
    @ApiResponse(responseCode = "200", description = "Payment status changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or ID mismatch")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> changePaymentStatus(
            @Parameter(description = "Payment ID", required = true) @PathVariable String id,
            @Valid @RequestBody ChangePaymentStatusCommand command
    ) {
        var statusCommand = new ChangePaymentStatusCommand(id, command.status(), command.reason());
        PaymentEntity updatedPayment = changePaymentStatusUseCase.handle(statusCommand);
        PaymentResponse response = PaymentResponse.from(updatedPayment);

        return ResponseEntity.ok(response);
    }

    // Delete
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete payment",
        description = "Soft deletes a payment by setting the deleted_at date"
    )
    @ApiResponse(responseCode = "204", description = "Payment deleted successfully")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(
            @Parameter(description = "Payment ID", required = true) @PathVariable String id
    ) {
        boolean deleted = deletePaymentUseCase.handle(id);
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
