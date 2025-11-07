/**
 * Tenant REST Controller
 *
 * REST API endpoints for tenant management
 * Implements CRUD operations for tenants
 */
package com.droid.bss.api.tenant;

import com.droid.bss.application.command.tenant.TenantCommandService;
import com.droid.bss.application.dto.tenant.CreateTenantCommand;
import com.droid.bss.application.dto.tenant.UpdateTenantCommand;
import com.droid.bss.application.dto.tenant.TenantDto;
import com.droid.bss.application.query.tenant.TenantQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for tenant operations
 * Provides REST API endpoints for tenant management
 */
@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenant Management", description = "Multi-tenant management operations")
public class TenantController {

    private final TenantQueryService tenantQueryService;
    private final TenantCommandService tenantCommandService;

    @Autowired
    public TenantController(TenantQueryService tenantQueryService, TenantCommandService tenantCommandService) {
        this.tenantQueryService = tenantQueryService;
        this.tenantCommandService = tenantCommandService;
    }

    /**
     * Get tenant by ID
     */
    @GetMapping("/{tenantId}")
    @Operation(summary = "Get tenant by ID", description = "Retrieve tenant information by tenant ID")
    public ResponseEntity<TenantDto> getTenantById(@PathVariable UUID tenantId) {
        return tenantQueryService.getTenantById(tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tenant by code
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Get tenant by code", description = "Retrieve tenant information by tenant code")
    public ResponseEntity<TenantDto> getTenantByCode(@PathVariable String code) {
        return tenantQueryService.getTenantByCode(code)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tenant by domain
     */
    @GetMapping("/domain/{domain}")
    @Operation(summary = "Get tenant by domain", description = "Retrieve tenant information by domain")
    public ResponseEntity<TenantDto> getTenantByDomain(@PathVariable String domain) {
        return tenantQueryService.getTenantByDomain(domain)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active tenants
     */
    @GetMapping
    @Operation(summary = "Get all active tenants", description = "Retrieve all active tenants")
    public ResponseEntity<List<TenantDto>> getAllActiveTenants() {
        List<TenantDto> tenants = tenantQueryService.getAllActiveTenants();
        return ResponseEntity.ok(tenants);
    }

    /**
     * Get tenants by tier
     */
    @GetMapping("/tier/{tier}")
    @Operation(summary = "Get tenants by tier", description = "Retrieve tenants by tier (basic, premium, enterprise)")
    public ResponseEntity<List<TenantDto>> getTenantsByTier(@PathVariable String tier) {
        List<TenantDto> tenants = tenantQueryService.getTenantsByTier(tier);
        return ResponseEntity.ok(tenants);
    }

    /**
     * Get all tenants for a user
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user tenants", description = "Retrieve all tenants accessible by a specific user")
    public ResponseEntity<List<TenantDto>> getTenantsByUserId(@PathVariable UUID userId) {
        List<TenantDto> tenants = tenantQueryService.getTenantsByUserId(userId);
        return ResponseEntity.ok(tenants);
    }

    /**
     * Get default tenant for user
     */
    @GetMapping("/user/{userId}/default")
    @Operation(summary = "Get user's default tenant", description = "Retrieve user's default tenant")
    public ResponseEntity<TenantDto> getDefaultTenantByUserId(@PathVariable UUID userId) {
        return tenantQueryService.getDefaultTenantByUserId(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Count active users in tenant
     */
    @GetMapping("/{tenantId}/users/count")
    @Operation(summary = "Count tenant users", description = "Get count of active users in tenant")
    public ResponseEntity<Long> countActiveUsers(@PathVariable UUID tenantId) {
        long count = tenantQueryService.countActiveUsers(tenantId);
        return ResponseEntity.ok(count);
    }

    /**
     * Search tenants
     */
    @GetMapping("/search")
    @Operation(summary = "Search tenants", description = "Search tenants by name or code")
    public ResponseEntity<List<TenantDto>> searchTenants(@RequestParam String search) {
        List<TenantDto> tenants = tenantQueryService.searchTenants(search);
        return ResponseEntity.ok(tenants);
    }

    /**
     * Create a new tenant
     */
    @PostMapping
    @Operation(summary = "Create tenant", description = "Create a new tenant")
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody CreateTenantCommand command) {
        try {
            var tenant = tenantCommandService.createTenant(command);
            // Return created tenant as DTO
            TenantDto dto = tenantQueryService.getTenantById(tenant.getId()).orElse(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update tenant
     */
    @PutMapping("/{tenantId}")
    @Operation(summary = "Update tenant", description = "Update tenant information")
    public ResponseEntity<TenantDto> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantCommand command) {
        try {
            var tenant = tenantCommandService.updateTenant(tenantId, command);
            // Return updated tenant as DTO
            TenantDto dto = tenantQueryService.getTenantById(tenant.getId()).orElse(null);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activate tenant
     */
    @PostMapping("/{tenantId}/activate")
    @Operation(summary = "Activate tenant", description = "Activate a suspended or inactive tenant")
    public ResponseEntity<Void> activateTenant(@PathVariable UUID tenantId) {
        try {
            tenantCommandService.activateTenant(tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Suspend tenant
     */
    @PostMapping("/{tenantId}/suspend")
    @Operation(summary = "Suspend tenant", description = "Suspend a tenant")
    public ResponseEntity<Void> suspendTenant(
            @PathVariable UUID tenantId,
            @RequestParam String reason) {
        try {
            tenantCommandService.suspendTenant(tenantId, reason);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete tenant (soft delete)
     */
    @DeleteMapping("/{tenantId}")
    @Operation(summary = "Delete tenant", description = "Delete a tenant (soft delete)")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID tenantId) {
        try {
            tenantCommandService.deleteTenant(tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update tenant branding
     */
    @PutMapping("/{tenantId}/branding")
    @Operation(summary = "Update tenant branding", description = "Update tenant's custom branding and colors")
    public ResponseEntity<Void> updateBranding(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String customBranding,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String secondaryColor,
            @RequestParam(required = false) String customCss) {
        try {
            tenantCommandService.updateBranding(
                tenantId,
                customBranding,
                logoUrl,
                primaryColor,
                secondaryColor,
                customCss
            );
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
