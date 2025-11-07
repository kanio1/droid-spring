package com.droid.bss.api.service;

import com.droid.bss.application.command.service.CreateServiceActivationUseCase;
import com.droid.bss.application.command.service.DeactivateServiceUseCase;
import com.droid.bss.application.command.service.ServiceActivationService;
import com.droid.bss.application.dto.service.*;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.service.ActivationEligibility;
import com.droid.bss.domain.service.ServiceType;
import com.droid.bss.infrastructure.audit.Audited;
import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for Service management
 */
@RestController
@RequestMapping("/api/services")
@Tag(name = "Service", description = "Service management API")
public class ServiceController {

    private final ServiceActivationService service;
    private final CreateServiceActivationUseCase createActivationUseCase;
    private final DeactivateServiceUseCase deactivateServiceUseCase;
    private final BusinessMetrics businessMetrics;

    public ServiceController(
            ServiceActivationService service,
            CreateServiceActivationUseCase createActivationUseCase,
            DeactivateServiceUseCase deactivateServiceUseCase,
            BusinessMetrics businessMetrics) {
        this.service = service;
        this.createActivationUseCase = createActivationUseCase;
        this.deactivateServiceUseCase = deactivateServiceUseCase;
        this.businessMetrics = businessMetrics;
    }

    // Service Catalog
    @GetMapping
    @Operation(
        summary = "Get all available services",
        description = "Retrieves all active services in the catalog"
    )
    @ApiResponse(responseCode = "200", description = "Services retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<com.droid.bss.domain.service.ServiceEntity>> getAllServices() {
        var services = service.getAllServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/by-category/{category}")
    @Operation(
        summary = "Get services by category",
        description = "Retrieves services filtered by category"
    )
    @ApiResponse(responseCode = "200", description = "Services retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<com.droid.bss.domain.service.ServiceEntity>> getServicesByCategory(
            @Parameter(description = "Service category", required = true) @PathVariable String category
    ) {
        var services = service.getServicesByCategory(category);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/by-type/{type}")
    @Operation(
        summary = "Get services by type",
        description = "Retrieves services filtered by type"
    )
    @ApiResponse(responseCode = "200", description = "Services retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<List<com.droid.bss.domain.service.ServiceEntity>> getServicesByType(
            @Parameter(description = "Service type", required = true) @PathVariable ServiceType type
    ) {
        var services = service.getServicesByType(type);
        return ResponseEntity.ok(services);
    }

    // Service Activations
    @GetMapping("/activations")
    @Operation(
        summary = "Get customer service activations",
        description = "Retrieves active service activations for a customer"
    )
    @ApiResponse(responseCode = "200", description = "Activations retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or #customerId == authentication.principal.getClaimAsString('customer_id')")
    public ResponseEntity<List<ServiceActivationResponse>> getCustomerActivations(
            @Parameter(description = "Customer ID", required = true) @RequestParam String customerId
    ) {
        var activations = service.getCustomerActiveActivations(customerId);
        var responses = activations.stream()
                .map(ServiceActivationResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/activations/{activationId}")
    @Operation(
        summary = "Get service activation by ID",
        description = "Retrieves a specific service activation"
    )
    @ApiResponse(responseCode = "200", description = "Activation found")
    @ApiResponse(responseCode = "404", description = "Activation not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ServiceActivationResponse> getServiceActivation(
            @Parameter(description = "Activation ID", required = true) @PathVariable String activationId
    ) {
        return service.getServiceActivation(activationId)
                .map(a -> ResponseEntity.ok(ServiceActivationResponse.from(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/activations")
    @Operation(
        summary = "Create service activation",
        description = "Creates a new service activation for a customer"
    )
    @ApiResponse(responseCode = "201", description = "Activation created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.services.create_activation.time", description = "Time to create service activation")
    @Audited(action = AuditAction.SERVICE_CREATE, entityType = "Service", description = "Creating service activation for customer")
    public ResponseEntity<ServiceActivationResponse> createServiceActivation(
            @Valid @RequestBody CreateServiceActivationCommand command
    ) {
        var timer = businessMetrics.startServiceActivation();
        try {
            var response = createActivationUseCase.handle(command);
            return ResponseEntity.ok(response);
        } finally {
            businessMetrics.recordServiceActivation(timer);
        }
    }

    @PostMapping("/activations/{activationId}/deactivate")
    @Operation(
        summary = "Deactivate service",
        description = "Deactivates an active service"
    )
    @ApiResponse(responseCode = "200", description = "Service deactivated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "404", description = "Activation not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Timed(value = "bss.services.deactivate_service.time", description = "Time to deactivate service")
    @Audited(action = AuditAction.SERVICE_UPDATE, entityType = "Service", description = "Deactivating service activation {activationId}")
    public ResponseEntity<ServiceActivationResponse> deactivateService(
            @Parameter(description = "Activation ID", required = true) @PathVariable String activationId,
            @Valid @RequestBody DeactivateServiceCommand command
    ) {
        var timer = businessMetrics.startServiceDeactivation();
        try {
            var response = deactivateServiceUseCase.handle(command);
            return ResponseEntity.ok(response);
        } finally {
            businessMetrics.recordServiceDeactivation(timer);
        }
    }

    @PostMapping("/check-eligibility")
    @Operation(
        summary = "Check service activation eligibility",
        description = "Checks if a service can be activated for a customer"
    )
    @ApiResponse(responseCode = "200", description = "Eligibility check completed")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ActivationEligibility> checkActivationEligibility(
            @Parameter(description = "Customer ID", required = true) @RequestParam String customerId,
            @Parameter(description = "Service code", required = true) @RequestParam String serviceCode
    ) {
        var eligibility = service.checkActivationEligibility(customerId, serviceCode);
        return ResponseEntity.ok(eligibility);
    }
}
