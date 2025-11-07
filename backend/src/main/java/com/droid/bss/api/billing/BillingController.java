package com.droid.bss.api.billing;

import com.droid.bss.application.command.billing.IngestUsageRecordUseCase;
import com.droid.bss.application.command.billing.ProcessBillingCycleUseCase;
import com.droid.bss.application.command.billing.StartBillingCycleUseCase;
import com.droid.bss.application.dto.billing.IngestUsageRecordCommand;
import com.droid.bss.application.dto.billing.StartBillingCycleCommand;
import com.droid.bss.application.dto.billing.UsageRecordResponse;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.billing.*;
import com.droid.bss.infrastructure.audit.Audited;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API for Billing
 */
@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Billing and usage API")
public class BillingController {

    private final IngestUsageRecordUseCase ingestUseCase;
    private final StartBillingCycleUseCase startCycleUseCase;
    private final ProcessBillingCycleUseCase processCycleUseCase;
    private final UsageRecordRepository usageRecordRepository;
    private final BillingCycleRepository billingCycleRepository;

    public BillingController(
            IngestUsageRecordUseCase ingestUseCase,
            StartBillingCycleUseCase startCycleUseCase,
            ProcessBillingCycleUseCase processCycleUseCase,
            UsageRecordRepository usageRecordRepository,
            BillingCycleRepository billingCycleRepository) {
        this.ingestUseCase = ingestUseCase;
        this.startCycleUseCase = startCycleUseCase;
        this.processCycleUseCase = processCycleUseCase;
        this.usageRecordRepository = usageRecordRepository;
        this.billingCycleRepository = billingCycleRepository;
    }

    // Usage Records
    @PostMapping("/usage-records")
    @Operation(
        summary = "Ingest usage record",
        description = "Ingest a new usage record (CDR)"
    )
    @ApiResponse(responseCode = "201", description = "Usage record ingested successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.billing.api.ingest_usage_record", description = "Time to ingest a usage record")
    @Audited(action = AuditAction.BILLING_CREATE, entityType = "UsageRecord", description = "Ingesting usage record")
    public ResponseEntity<UsageRecordResponse> ingestUsageRecord(
            @Valid @RequestBody IngestUsageRecordCommand command
    ) {
        var response = ingestUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usage-records")
    @Operation(
        summary = "Get unrated usage records",
        description = "Get all unrated usage records"
    )
    @ApiResponse(responseCode = "200", description = "Usage records retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.billing.api.get_unrated_usage_records", description = "Time to get unrated usage records")
    public ResponseEntity<List<UsageRecordResponse>> getUnratedUsageRecords() {
        var records = usageRecordRepository.findUnrated();
        var responses = records.stream()
                .map(UsageRecordResponse::from)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // Billing Cycles
    @PostMapping("/cycles")
    @Operation(
        summary = "Start billing cycle",
        description = "Start a new billing cycle"
    )
    @ApiResponse(responseCode = "201", description = "Billing cycle started successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "bss.billing.api.start_billing_cycle", description = "Time to start a billing cycle")
    @Audited(action = AuditAction.BILLING_CREATE, entityType = "BillingCycle", description = "Starting billing cycle")
    public ResponseEntity<BillingCycleEntity> startBillingCycle(
            @Valid @RequestBody StartBillingCycleCommand command
    ) {
        var cycle = startCycleUseCase.handle(command);
        return ResponseEntity.ok(cycle);
    }

    @PostMapping("/cycles/{cycleId}/process")
    @Operation(
        summary = "Process billing cycle",
        description = "Process a billing cycle and generate invoices"
    )
    @ApiResponse(responseCode = "200", description = "Billing cycle processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "404", description = "Billing cycle not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "bss.billing.api.process_billing_cycle", description = "Time to process a billing cycle")
    @Audited(action = AuditAction.BILLING_UPDATE, entityType = "BillingCycle", description = "Processing billing cycle {cycleId}")
    public ResponseEntity<BillingCycleEntity> processBillingCycle(
            @Parameter(description = "Billing cycle ID", required = true) @PathVariable String cycleId
    ) {
        var cycle = processCycleUseCase.handle(cycleId);
        return ResponseEntity.ok(cycle);
    }

    @GetMapping("/cycles")
    @Operation(
        summary = "Get billing cycles",
        description = "Get all billing cycles"
    )
    @ApiResponse(responseCode = "200", description = "Billing cycles retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "bss.billing.api.get_billing_cycles", description = "Time to get billing cycles")
    public ResponseEntity<List<BillingCycleEntity>> getBillingCycles() {
        var cycles = billingCycleRepository.findAll();
        return ResponseEntity.ok(cycles);
    }

    @GetMapping("/cycles/pending")
    @Operation(
        summary = "Get pending billing cycles",
        description = "Get pending billing cycles ready for processing"
    )
    @ApiResponse(responseCode = "200", description = "Pending cycles retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "bss.billing.api.get_pending_cycles", description = "Time to get pending billing cycles")
    public ResponseEntity<List<BillingCycleEntity>> getPendingBillingCycles() {
        var today = LocalDate.now();
        var cycles = billingCycleRepository.findPendingForProcessing(today);
        return ResponseEntity.ok(cycles);
    }
}
