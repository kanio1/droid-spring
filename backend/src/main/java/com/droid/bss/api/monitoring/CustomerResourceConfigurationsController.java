package com.droid.bss.api.monitoring;

import com.droid.bss.application.command.monitoring.ManageCustomerResourceConfigurationUseCase;
import com.droid.bss.application.query.monitoring.GetCustomerResourceConfigurationsUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.monitoring.CustomerResourceConfiguration;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring/customer-resource-configurations")
@Tag(name = "Customer Resource Configurations", description = "Customer resource configuration API")
public class CustomerResourceConfigurationsController {

    private final ManageCustomerResourceConfigurationUseCase manageConfigurationUseCase;
    private final GetCustomerResourceConfigurationsUseCase getConfigurationUseCase;

    public CustomerResourceConfigurationsController(
            ManageCustomerResourceConfigurationUseCase manageConfigurationUseCase,
            GetCustomerResourceConfigurationsUseCase getConfigurationUseCase) {
        this.manageConfigurationUseCase = manageConfigurationUseCase;
        this.getConfigurationUseCase = getConfigurationUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new customer resource configuration")
    @Audited(action = AuditAction.MONITORING_CREATE, entityType = "CustomerResourceConfiguration", description = "Creating customer resource configuration")
    public ResponseEntity<CustomerResourceConfiguration> create(
            @RequestParam Long customerId,
            @RequestParam String resourceType,
            @RequestParam String resourceId,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) BigDecimal maxLimit,
            @RequestParam(defaultValue = "80") BigDecimal warningThreshold,
            @RequestParam(defaultValue = "95") BigDecimal criticalThreshold,
            @RequestParam(required = false) BigDecimal budgetLimit,
            @RequestParam(required = false) String budgetCurrency,
            @RequestParam(required = false) String alertEmail,
            @RequestParam(required = false) String alertPhone,
            @RequestParam(required = false) String alertSlackWebhook,
            @RequestParam(defaultValue = "false") boolean autoScalingEnabled,
            @RequestParam(required = false) BigDecimal scaleUpThreshold,
            @RequestParam(required = false) BigDecimal scaleDownThreshold,
            @RequestParam(defaultValue = "ACTIVE") String status) {

        CustomerResourceConfiguration config = manageConfigurationUseCase.create(
                customerId, resourceType, resourceId, resourceName, region,
                maxLimit, warningThreshold, criticalThreshold, budgetLimit, budgetCurrency,
                alertEmail, alertPhone, alertSlackWebhook, autoScalingEnabled,
                scaleUpThreshold, scaleDownThreshold, status);

        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer resource configuration")
    @Audited(action = AuditAction.MONITORING_UPDATE, entityType = "CustomerResourceConfiguration", description = "Updating customer resource configuration {id}")
    public ResponseEntity<CustomerResourceConfiguration> update(
            @PathVariable Long id,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) BigDecimal maxLimit,
            @RequestParam(required = false) BigDecimal warningThreshold,
            @RequestParam(required = false) BigDecimal criticalThreshold,
            @RequestParam(required = false) BigDecimal budgetLimit,
            @RequestParam(required = false) String budgetCurrency,
            @RequestParam(required = false) String alertEmail,
            @RequestParam(required = false) String alertPhone,
            @RequestParam(required = false) String alertSlackWebhook,
            @RequestParam(required = false) Boolean autoScalingEnabled,
            @RequestParam(required = false) BigDecimal scaleUpThreshold,
            @RequestParam(required = false) BigDecimal scaleDownThreshold,
            @RequestParam(required = false) String status) {

        return manageConfigurationUseCase.update(
                id, resourceName, region, maxLimit, warningThreshold, criticalThreshold,
                budgetLimit, budgetCurrency, alertEmail, alertPhone, alertSlackWebhook,
                autoScalingEnabled != null ? autoScalingEnabled : false,
                scaleUpThreshold, scaleDownThreshold, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer resource configuration")
    @Audited(action = AuditAction.MONITORING_DELETE, entityType = "CustomerResourceConfiguration", description = "Deleting customer resource configuration {id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = manageConfigurationUseCase.delete(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all configurations for a customer")
    public ResponseEntity<List<CustomerResourceConfiguration>> getByCustomerId(@PathVariable Long customerId) {
        List<CustomerResourceConfiguration> configs = getConfigurationUseCase.getByCustomerId(customerId);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/customer/{customerId}/resource-type/{resourceType}")
    @Operation(summary = "Get configurations by customer and resource type")
    public ResponseEntity<List<CustomerResourceConfiguration>> getByCustomerIdAndResourceType(
            @PathVariable Long customerId,
            @PathVariable String resourceType) {

        List<CustomerResourceConfiguration> configs = getConfigurationUseCase
                .getByCustomerIdAndResourceType(customerId, resourceType);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/resource/{resourceId}")
    @Operation(summary = "Get configurations by resource ID")
    public ResponseEntity<List<CustomerResourceConfiguration>> getByResourceId(@PathVariable String resourceId) {
        List<CustomerResourceConfiguration> configs = getConfigurationUseCase.getByResourceId(resourceId);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get configurations by status")
    public ResponseEntity<List<CustomerResourceConfiguration>> getByStatus(@PathVariable String status) {
        List<CustomerResourceConfiguration> configs = getConfigurationUseCase.getByStatus(status);
        return ResponseEntity.ok(configs);
    }
}
