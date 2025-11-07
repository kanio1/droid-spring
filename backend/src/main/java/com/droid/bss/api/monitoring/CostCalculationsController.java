package com.droid.bss.api.monitoring;

import com.droid.bss.application.command.monitoring.CalculateCostUseCase;
import com.droid.bss.application.query.monitoring.GetCostCalculationsUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.monitoring.CostCalculation;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring/cost-calculations")
@Tag(name = "Cost Calculations", description = "Cost calculation API")
public class CostCalculationsController {

    private final CalculateCostUseCase calculateCostUseCase;
    private final GetCostCalculationsUseCase getCostCalculationsUseCase;

    public CostCalculationsController(CalculateCostUseCase calculateCostUseCase,
                                      GetCostCalculationsUseCase getCostCalculationsUseCase) {
        this.calculateCostUseCase = calculateCostUseCase;
        this.getCostCalculationsUseCase = getCostCalculationsUseCase;
    }

    @PostMapping
    @Operation(summary = "Calculate cost for a resource usage period")
    @Audited(action = AuditAction.MONITORING_CREATE, entityType = "CostCalculation", description = "Calculating cost for resource")
    public ResponseEntity<CostCalculation> calculate(
            @RequestParam Long customerId,
            @RequestParam String resourceType,
            @RequestParam String billingPeriod,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd,
            @RequestParam BigDecimal totalUsage,
            @RequestParam Long costModelId,
            @RequestParam String currency) {

        CostCalculation calculation = calculateCostUseCase.calculate(
                customerId, resourceType, billingPeriod, periodStart, periodEnd, totalUsage, costModelId, currency);

        return ResponseEntity.status(HttpStatus.CREATED).body(calculation);
    }

    @PostMapping("/{id}/recalculate")
    @Operation(summary = "Recalculate an existing cost calculation")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "CostCalculation", description = "Recalculating cost calculation {id}")
    public ResponseEntity<CostCalculation> recalculate(@PathVariable Long id) {
        CostCalculation calculation = calculateCostUseCase.recalculate(id);
        return ResponseEntity.ok(calculation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cost calculation by ID")
    public ResponseEntity<List<CostCalculation>> getById(@PathVariable Long id) {
        List<CostCalculation> calculations = getCostCalculationsUseCase.getById(id);
        return ResponseEntity.ok(calculations);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get cost calculations by customer ID and period")
    public ResponseEntity<List<CostCalculation>> getByCustomerIdAndPeriod(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        List<CostCalculation> calculations = getCostCalculationsUseCase.getByCustomerIdAndPeriod(
                customerId, startDate, endDate);
        return ResponseEntity.ok(calculations);
    }

    @GetMapping("/customer/{customerId}/resource/{resourceType}")
    @Operation(summary = "Get cost calculations by customer ID and resource type")
    public ResponseEntity<List<CostCalculation>> getByCustomerIdAndResourceType(
            @PathVariable Long customerId,
            @PathVariable String resourceType) {

        List<CostCalculation> calculations = getCostCalculationsUseCase.getByCustomerIdAndResourceType(
                customerId, resourceType);
        return ResponseEntity.ok(calculations);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get cost calculations by status")
    public ResponseEntity<List<CostCalculation>> getByStatus(@PathVariable String status) {
        List<CostCalculation> calculations = getCostCalculationsUseCase.getByStatus(status);
        return ResponseEntity.ok(calculations);
    }
}
