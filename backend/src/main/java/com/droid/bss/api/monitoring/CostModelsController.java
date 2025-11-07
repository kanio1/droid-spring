package com.droid.bss.api.monitoring;

import com.droid.bss.application.command.monitoring.ManageCostModelUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.monitoring.CostModel;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring/cost-models")
@Tag(name = "Cost Models", description = "Cost model management API")
public class CostModelsController {

    private final ManageCostModelUseCase manageCostModelUseCase;

    public CostModelsController(ManageCostModelUseCase manageCostModelUseCase) {
        this.manageCostModelUseCase = manageCostModelUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new cost model")
    @Audited(action = AuditAction.MONITORING_CREATE, entityType = "CostModel", description = "Creating new cost model")
    public ResponseEntity<CostModel> create(
            @RequestParam String modelName,
            @RequestParam(required = false) String description,
            @RequestParam String billingPeriod,
            @RequestParam BigDecimal baseCost,
            @RequestParam BigDecimal overageRate,
            @RequestParam BigDecimal includedUsage,
            @RequestParam String currency,
            @RequestParam(defaultValue = "true") boolean active) {

        CostModel model = manageCostModelUseCase.create(
                modelName, description, billingPeriod, baseCost, overageRate, includedUsage, currency, active);

        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing cost model")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "CostModel", description = "Updating cost model {id}")
    public ResponseEntity<CostModel> update(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal baseCost,
            @RequestParam BigDecimal overageRate,
            @RequestParam BigDecimal includedUsage,
            @RequestParam String currency,
            @RequestParam(defaultValue = "true") boolean active) {

        CostModel model = manageCostModelUseCase.update(id, description, baseCost, overageRate, includedUsage, currency, active);
        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a cost model")
    @Audited(action = AuditAction.MONITORING_DELETE, entityType = "CostModel", description = "Deleting cost model {id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        manageCostModelUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all active cost models")
    public ResponseEntity<List<CostModel>> getActive() {
        List<CostModel> models = manageCostModelUseCase.getActiveModels();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cost model by ID")
    public ResponseEntity<CostModel> getById(@PathVariable Long id) {
        return manageCostModelUseCase.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{modelName}")
    @Operation(summary = "Get cost model by name")
    public ResponseEntity<CostModel> getByModelName(@PathVariable String modelName) {
        return manageCostModelUseCase.getByModelName(modelName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
