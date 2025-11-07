/**
 * Tenant Query Service
 *
 * Application service for tenant read operations
 * Implements query use cases
 */
package com.droid.bss.application.query.tenant;

import com.droid.bss.domain.tenant.Tenant;
import com.droid.bss.domain.tenant.TenantRepository;
import com.droid.bss.application.dto.tenant.TenantDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for tenant query operations
 * Handles read operations and data transformation
 */
@Service
@Transactional(readOnly = true)
public class TenantQueryService {

    private final TenantRepository tenantRepository;

    @Autowired
    public TenantQueryService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Get tenant by ID
     */
    public Optional<TenantDto> getTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
            .map(this::toDto);
    }

    /**
     * Get tenant by code
     */
    public Optional<TenantDto> getTenantByCode(String code) {
        return tenantRepository.findByCode(code)
            .map(this::toDto);
    }

    /**
     * Get tenant by domain
     */
    public Optional<TenantDto> getTenantByDomain(String domain) {
        return tenantRepository.findByDomain(domain)
            .map(this::toDto);
    }

    /**
     * Get all active tenants
     */
    public List<TenantDto> getAllActiveTenants() {
        return tenantRepository.findByStatus(com.droid.bss.domain.tenant.TenantStatus.ACTIVE)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get tenants by tier
     */
    public List<TenantDto> getTenantsByTier(String tier) {
        return tenantRepository.findByStatusAndTier(tier)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get all tenants for a specific user
     */
    public List<TenantDto> getTenantsByUserId(UUID userId) {
        return tenantRepository.findTenantsByUserId(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get default tenant for user
     */
    public Optional<TenantDto> getDefaultTenantByUserId(UUID userId) {
        return tenantRepository.findDefaultTenantByUserId(userId)
            .map(this::toDto);
    }

    /**
     * Count active users in tenant
     */
    public long countActiveUsers(UUID tenantId) {
        return tenantRepository.countActiveUsers(tenantId);
    }

    /**
     * Get tenants with custom branding
     */
    public List<TenantDto> getTenantsWithBranding() {
        return tenantRepository.findTenantsWithBranding()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Search tenants
     */
    public List<TenantDto> searchTenants(String search) {
        return tenantRepository.searchTenants(search)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Transform Tenant entity to DTO
     */
    private TenantDto toDto(Tenant tenant) {
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setCode(tenant.getCode());
        dto.setDomain(tenant.getDomain());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setPhone(tenant.getPhone());
        dto.setAddress(tenant.getAddress());
        dto.setCity(tenant.getCity());
        dto.setState(tenant.getState());
        dto.setPostalCode(tenant.getPostalCode());
        dto.setCountry(tenant.getCountry());
        dto.setStatus(tenant.getStatus());
        dto.setCustomBranding(tenant.getCustomBranding());
        dto.setLogoUrl(tenant.getLogoUrl());
        dto.setTimezone(tenant.getTimezone());
        dto.setLocale(tenant.getLocale());
        dto.setCurrency(tenant.getCurrency());
        dto.setIndustry(tenant.getIndustry());
        dto.setTenantTier(tenant.getTenantTier());
        dto.setMaxUsers(tenant.getMaxUsers());
        dto.setMaxCustomers(tenant.getMaxCustomers());
        dto.setStorageQuotaMb(tenant.getStorageQuotaMb());
        dto.setApiRateLimit(tenant.getApiRateLimit());
        dto.setSettings(tenant.getSettings());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());

        return dto;
    }
}
