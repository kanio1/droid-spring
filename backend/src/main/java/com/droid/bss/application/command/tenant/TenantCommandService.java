/**
 * Tenant Command Service
 *
 * Application service for tenant write operations
 * Implements use cases for tenant management
 */
package com.droid.bss.application.command.tenant;

import com.droid.bss.domain.tenant.*;
import com.droid.bss.application.dto.tenant.CreateTenantCommand;
import com.droid.bss.application.dto.tenant.UpdateTenantCommand;
import com.droid.bss.application.dto.tenant.TenantDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for tenant command operations
 * Handles business logic for tenant management
 */
@Service
@Transactional
public class TenantCommandService {

    private final TenantRepository tenantRepository;

    @Autowired
    public TenantCommandService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Create a new tenant
     */
    public Tenant createTenant(CreateTenantCommand command) {
        // Validate uniqueness
        if (tenantRepository.existsByCode(command.getCode())) {
            throw new IllegalArgumentException("Tenant code already exists: " + command.getCode());
        }

        if (tenantRepository.existsByDomain(command.getDomain())) {
            throw new IllegalArgumentException("Domain already registered: " + command.getDomain());
        }

        // Create tenant
        Tenant tenant = new Tenant(
            command.getName(),
            command.getCode(),
            command.getDomain(),
            command.getStatus() != null ? command.getStatus() : TenantStatus.TRIAL
        );

        // Set additional properties
        tenant.setContactEmail(command.getContactEmail());
        tenant.setPhone(command.getPhone());
        tenant.setAddress(command.getAddress());
        tenant.setCity(command.getCity());
        tenant.setState(command.getState());
        tenant.setPostalCode(command.getPostalCode());
        tenant.setCountry(command.getCountry());
        tenant.setTimezone(command.getTimezone());
        tenant.setLocale(command.getLocale());
        tenant.setCurrency(command.getCurrency());
        tenant.setIndustry(command.getIndustry());
        tenant.setTenantTier(command.getTenantTier());
        tenant.setMaxUsers(command.getMaxUsers());
        tenant.setMaxCustomers(command.getMaxCustomers());
        tenant.setStorageQuotaMb(command.getStorageQuotaMb());
        tenant.setApiRateLimit(command.getApiRateLimit());

        // Set settings if provided
        if (command.getSettings() != null) {
            tenant.setSettings(command.getSettings());
        }

        return tenantRepository.save(tenant);
    }

    /**
     * Update existing tenant
     */
    public Tenant updateTenant(UUID tenantId, UpdateTenantCommand command) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        // Update allowed fields
        if (command.getName() != null) {
            tenant.setName(command.getName());
        }

        if (command.getContactEmail() != null) {
            tenant.setContactEmail(command.getContactEmail());
        }

        if (command.getPhone() != null) {
            tenant.setPhone(command.getPhone());
        }

        if (command.getAddress() != null) {
            tenant.setAddress(command.getAddress());
        }

        if (command.getCity() != null) {
            tenant.setCity(command.getCity());
        }

        if (command.getState() != null) {
            tenant.setState(command.getState());
        }

        if (command.getPostalCode() != null) {
            tenant.setPostalCode(command.getPostalCode());
        }

        if (command.getCountry() != null) {
            tenant.setCountry(command.getCountry());
        }

        if (command.getTimezone() != null) {
            tenant.setTimezone(command.getTimezone());
        }

        if (command.getLocale() != null) {
            tenant.setLocale(command.getLocale());
        }

        if (command.getCurrency() != null) {
            tenant.setCurrency(command.getCurrency());
        }

        if (command.getIndustry() != null) {
            tenant.setIndustry(command.getIndustry());
        }

        if (command.getMaxUsers() != null) {
            tenant.setMaxUsers(command.getMaxUsers());
        }

        if (command.getMaxCustomers() != null) {
            tenant.setMaxCustomers(command.getMaxCustomers());
        }

        if (command.getStorageQuotaMb() != null) {
            tenant.setStorageQuotaMb(command.getStorageQuotaMb());
        }

        if (command.getApiRateLimit() != null) {
            tenant.setApiRateLimit(command.getApiRateLimit());
        }

        return tenantRepository.save(tenant);
    }

    /**
     * Activate a tenant
     */
    public void activateTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.activate();
        tenantRepository.save(tenant);
    }

    /**
     * Suspend a tenant
     */
    public void suspendTenant(UUID tenantId, String reason) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.suspend(reason);
        tenantRepository.save(tenant);
    }

    /**
     * Delete a tenant (soft delete)
     */
    public void deleteTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        tenant.delete();
        tenantRepository.save(tenant);
    }

    /**
     * Update tenant branding
     */
    public void updateBranding(UUID tenantId, String customBranding, String logoUrl,
                                String primaryColor, String secondaryColor, String customCss) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        if (customBranding != null) {
            tenant.setCustomBranding(customBranding);
        }

        if (logoUrl != null) {
            tenant.setLogoUrl(logoUrl);
        }

        if (primaryColor != null) {
            if (tenant.getSettings() == null) {
                tenant.setSettings(new TenantSettings());
            }
            tenant.getSettings().setPrimaryColor(primaryColor);
        }

        if (secondaryColor != null) {
            if (tenant.getSettings() == null) {
                tenant.setSettings(new TenantSettings());
            }
            tenant.getSettings().setSecondaryColor(secondaryColor);
        }

        if (customCss != null) {
            if (tenant.getSettings() == null) {
                tenant.setSettings(new TenantSettings());
            }
            tenant.getSettings().setCustomCss(customCss);
        }

        tenantRepository.save(tenant);
    }
}
