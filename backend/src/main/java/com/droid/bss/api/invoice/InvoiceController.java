package com.droid.bss.api.invoice;

import com.droid.bss.application.command.invoice.ChangeInvoiceStatusUseCase;
import com.droid.bss.application.command.invoice.CreateInvoiceUseCase;
import com.droid.bss.application.command.invoice.UpdateInvoiceUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.invoice.ChangeInvoiceStatusCommand;
import com.droid.bss.application.dto.invoice.CreateInvoiceCommand;
import com.droid.bss.application.dto.invoice.InvoiceResponse;
import com.droid.bss.application.dto.invoice.UpdateInvoiceCommand;
import com.droid.bss.application.query.invoice.InvoiceQueryService;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * REST Controller for Invoice CRUD operations
 */
@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoice", description = "Invoice management API")
public class InvoiceController {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final ChangeInvoiceStatusUseCase changeInvoiceStatusUseCase;
    private final InvoiceQueryService invoiceQueryService;

    public InvoiceController(
            CreateInvoiceUseCase createInvoiceUseCase,
            UpdateInvoiceUseCase updateInvoiceUseCase,
            ChangeInvoiceStatusUseCase changeInvoiceStatusUseCase,
            InvoiceQueryService invoiceQueryService) {
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.changeInvoiceStatusUseCase = changeInvoiceStatusUseCase;
        this.invoiceQueryService = invoiceQueryService;
    }

    // Create

    /**
     * Create a new invoice
     */
    @PostMapping
    @Operation(
        summary = "Create a new invoice",
        description = "Creates a new invoice with the provided details"
    )
    @ApiResponse(responseCode = "201", description = "Invoice created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Invoice number already exists")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = AuditAction.INVOICE_CREATE, entityType = "Invoice", description = "Creating new invoice")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var invoice = createInvoiceUseCase.execute(command);

        return ResponseEntity.ok(invoice);
    }

    // Read

    /**
     * Get invoice by ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get invoice by ID",
        description = "Retrieves an invoice by its unique identifier"
    )
    @ApiResponse(responseCode = "200", description = "Invoice found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.getClaimAsString('customer_id')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable UUID id
    ) {
        return invoiceQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all invoices with pagination, search, and filters
     */
    @GetMapping
    @Operation(
        summary = "Get all invoices",
        description = "Retrieves all invoices with optional search and filters"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<InvoiceResponse>> getAllInvoices(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')")
            @RequestParam(defaultValue = "createdAt,desc") String sort,

            @Parameter(description = "Search term (invoice number, customer name, or notes)")
            @RequestParam(required = false) String query,

            @Parameter(description = "Filter by invoice status")
            @RequestParam(required = false) InvoiceStatus status,

            @Parameter(description = "Filter by invoice type")
            @RequestParam(required = false) InvoiceType type,

            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,

            @Parameter(description = "Filter by start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Filter by end date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Filter to show only unpaid invoices")
            @RequestParam(required = false) Boolean unpaid,

            @Parameter(description = "Filter to show only overdue invoices")
            @RequestParam(required = false) Boolean overdue
    ) {
        PageResponse<InvoiceResponse> invoices;

        if (query != null && !query.isBlank()) {
            invoices = invoiceQueryService.search(query, page, size);
        } else if (status != null) {
            invoices = invoiceQueryService.findByStatus(status, page, size);
        } else if (type != null) {
            invoices = invoiceQueryService.findByInvoiceType(type, page, size);
        } else if (customerId != null) {
            invoices = invoiceQueryService.findByCustomerId(customerId, page, size);
        } else if (unpaid != null && unpaid) {
            invoices = invoiceQueryService.findUnpaid(page, size);
        } else if (overdue != null && overdue) {
            invoices = invoiceQueryService.findOverdue(page, size);
        } else if (startDate != null && endDate != null) {
            invoices = invoiceQueryService.findByIssueDateBetween(startDate, endDate, page, size);
        } else {
            invoices = invoiceQueryService.findAll(page, size);
        }

        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoices by status
     */
    @GetMapping("/by-status/{status}")
    @Operation(
        summary = "Get invoices by status",
        description = "Retrieves invoices filtered by status"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getInvoicesByStatus(
            @Parameter(description = "Invoice status", required = true)
            @PathVariable InvoiceStatus status,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findByStatus(status, page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoices by type
     */
    @GetMapping("/by-type/{type}")
    @Operation(
        summary = "Get invoices by type",
        description = "Retrieves invoices filtered by type"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getInvoicesByType(
            @Parameter(description = "Invoice type", required = true)
            @PathVariable InvoiceType type,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findByInvoiceType(type, page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoices by customer ID
     */
    @GetMapping("/by-customer/{customerId}")
    @Operation(
        summary = "Get invoices by customer",
        description = "Retrieves invoices for a specific customer"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getInvoicesByCustomer(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findByCustomerId(customerId, page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoice by invoice number
     */
    @GetMapping("/by-invoice-number/{invoiceNumber}")
    @Operation(
        summary = "Get invoice by invoice number",
        description = "Retrieves an invoice by its invoice number"
    )
    @ApiResponse(responseCode = "200", description = "Invoice found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    public ResponseEntity<InvoiceResponse> getInvoiceByInvoiceNumber(
            @Parameter(description = "Invoice number", required = true)
            @PathVariable String invoiceNumber
    ) {
        return invoiceQueryService.findByInvoiceNumber(invoiceNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get invoices issued between dates
     */
    @GetMapping("/issued-between")
    @Operation(
        summary = "Get invoices issued between dates",
        description = "Retrieves invoices issued within a date range"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getInvoicesByIssueDateBetween(
            @Parameter(description = "Start date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findByIssueDateBetween(startDate, endDate, page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoices due between dates
     */
    @GetMapping("/due-between")
    @Operation(
        summary = "Get invoices due between dates",
        description = "Retrieves invoices due within a date range"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getInvoicesByDueDateBetween(
            @Parameter(description = "Start date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findByDueDateBetween(startDate, endDate, page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get unpaid invoices
     */
    @GetMapping("/unpaid")
    @Operation(
        summary = "Get unpaid invoices",
        description = "Retrieves all unpaid invoices (issued, sent, overdue)"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getUnpaidInvoices(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findUnpaid(page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get overdue invoices
     */
    @GetMapping("/overdue")
    @Operation(
        summary = "Get overdue invoices",
        description = "Retrieves all overdue invoices"
    )
    @ApiResponse(responseCode = "200", description = "Invoices retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> getOverdueInvoices(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.findOverdue(page, size);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Search invoices
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search invoices",
        description = "Searches invoices by invoice number, customer name, or notes"
    )
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<PageResponse<InvoiceResponse>> searchInvoices(
            @Parameter(description = "Search term", required = true)
            @RequestParam String query,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page")
            @RequestParam(defaultValue = "20") int size
    ) {
        var invoices = invoiceQueryService.search(query, page, size);
        return ResponseEntity.ok(invoices);
    }

    // Update

    /**
     * Update invoice status
     */
    @PutMapping("/{id}/status")
    @Operation(
        summary = "Update invoice status",
        description = "Updates the status of an invoice"
    )
    @ApiResponse(responseCode = "200", description = "Invoice status updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status transition or input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @Audited(action = AuditAction.INVOICE_UPDATE, entityType = "Invoice", description = "Updating status for invoice {id}")
    public ResponseEntity<Void> updateInvoiceStatus(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable UUID id,

            @Valid @RequestBody ChangeInvoiceStatusCommand command,

            @AuthenticationPrincipal Jwt principal
    ) {
        // Verify the ID in the path matches the ID in the command
        if (!id.toString().equals(command.id().toString())) {
            return ResponseEntity.badRequest().build();
        }

        changeInvoiceStatusUseCase.execute(command);
        return ResponseEntity.ok().build();
    }

    // Update

    /**
     * Update an invoice
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update an invoice",
        description = "Updates an existing invoice with new details"
    )
    @ApiResponse(responseCode = "200", description = "Invoice updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or business rule violation")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Version conflict (optimistic locking)")
    @Audited(action = AuditAction.INVOICE_UPDATE, entityType = "Invoice", description = "Updating invoice {id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable UUID id,

            @Valid @RequestBody UpdateInvoiceCommand command,

            @AuthenticationPrincipal Jwt principal
    ) {
        // Verify the ID in the path matches the ID in the command
        if (!id.toString().equals(command.id().toString())) {
            return ResponseEntity.badRequest().build();
        }

        var invoice = updateInvoiceUseCase.execute(command);
        return ResponseEntity.ok(invoice);
    }
}
