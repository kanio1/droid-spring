package com.droid.bss.api.monitoring;

import com.droid.bss.application.query.monitoring.GenerateCostForecastUseCase;
import com.droid.bss.application.query.monitoring.GetCostForecastsUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.monitoring.CostForecast;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring/cost-forecasts")
@Tag(name = "Cost Forecasts", description = "Cost forecasting API")
public class CostForecastsController {

    private final GenerateCostForecastUseCase generateCostForecastUseCase;
    private final GetCostForecastsUseCase getCostForecastsUseCase;

    public CostForecastsController(GenerateCostForecastUseCase generateCostForecastUseCase,
                                   GetCostForecastsUseCase getCostForecastsUseCase) {
        this.generateCostForecastUseCase = generateCostForecastUseCase;
        this.getCostForecastsUseCase = getCostForecastsUseCase;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate cost forecast for a customer and resource type")
    @Audited(action = AuditAction.MONITORING_CREATE, entityType = "CostForecast", description = "Generating cost forecast")
    public ResponseEntity<List<CostForecast>> generateForecast(
            @RequestParam Long customerId,
            @RequestParam String resourceType,
            @RequestParam String billingPeriod,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant forecastStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant forecastEndDate,
            @RequestParam(defaultValue = "3") int historicalMonths,
            @RequestParam(defaultValue = "LINEAR_REGRESSION") String forecastModel) {

        List<CostForecast> forecasts = generateCostForecastUseCase.generateForecast(
                customerId, resourceType, billingPeriod, forecastStartDate, forecastEndDate, historicalMonths, forecastModel);

        return ResponseEntity.ok(forecasts);
    }

    @GetMapping("/customer/{customerId}/resource/{resourceType}")
    @Operation(summary = "Get cost forecasts by customer ID and resource type")
    public ResponseEntity<List<CostForecast>> getByCustomerIdAndResourceType(
            @PathVariable Long customerId,
            @PathVariable String resourceType) {

        List<CostForecast> forecasts = getCostForecastsUseCase.getByCustomerIdAndResourceType(
                customerId, resourceType);

        return ResponseEntity.ok(forecasts);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get cost forecasts by customer ID and period")
    public ResponseEntity<List<CostForecast>> getByCustomerIdAndPeriod(
            @PathVariable Long customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        List<CostForecast> forecasts = getCostForecastsUseCase.getByCustomerIdAndPeriod(
                customerId, startDate, endDate);

        return ResponseEntity.ok(forecasts);
    }

    @GetMapping("/period/{forecastPeriodStart}")
    @Operation(summary = "Get cost forecasts by forecast period start")
    public ResponseEntity<List<CostForecast>> getByForecastPeriodStart(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant forecastPeriodStart) {

        List<CostForecast> forecasts = getCostForecastsUseCase.getByForecastPeriodStart(forecastPeriodStart);
        return ResponseEntity.ok(forecasts);
    }
}
