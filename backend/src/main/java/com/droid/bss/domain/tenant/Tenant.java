/**
 * Tenant Domain Entity
 *
 * Represents a tenant (organization/company) in the multi-tenant BSS system.
 * All customer data, orders, invoices, and other business entities are scoped to a tenant.
 *
 * Multi-tenancy model: Database per Tenant (with row-level security)
 * Each tenant has complete data isolation.
 */
package com.droid.bss.domain.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tenant aggregate root entity
 */
@Entity
@Table(name = "tenants")
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String name;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true, nullable = false)
    private String code;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String domain;

    @Size(max = 100)
    private String contactEmail;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String state;

    @Size(max = 10)
    private String postalCode;

    @Size(max = 50)
    private String country;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TenantStatus status;

    @Size(max = 500)
    private String customBranding; // JSON config for tenant-specific theming

    @Size(max = 100)
    private String logoUrl;

    @Size(max = 50)
    private String timezone;

    @Size(max = 10)
    private String locale;

    @Size(max = 50)
    private String currency;

    @Size(max = 50)
    private String industry;

    @Size(max = 10)
    private String tenantTier; // basic, premium, enterprise

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_customers")
    private Integer maxCustomers;

    @Column(name = "storage_quota_mb")
    private Long storageQuotaMb;

    @Column(name = "api_rate_limit")
    private Integer apiRateLimit;

    @Embedded
    private TenantSettings settings;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Junction table for User-Tenant many-to-many relationship
     * Users can belong to multiple tenants
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserTenant> userTenants = new HashSet<>();

    // Constructors
    public Tenant() {}

    public Tenant(String name, String code, String domain, TenantStatus status) {
        this.name = name;
        this.code = code;
        this.domain = domain;
        this.status = status;
    }

    // Business methods
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return status == TenantStatus.SUSPENDED;
    }

    public boolean canAddUser() {
        if (maxUsers == null) return true;
        return userTenants.stream()
            .filter(ut -> !ut.isDeleted())
            .count() < maxUsers;
    }

    public boolean canAddCustomer(int count) {
        if (maxCustomers == null) return true;
        // This would need a query to count actual customers
        return true; // Simplified for now
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    public void suspend(String reason) {
        this.status = TenantStatus.SUSPENDED;
        if (this.settings == null) {
            this.settings = new TenantSettings();
        }
        this.settings.setSuspensionReason(reason);
    }

    public void delete() {
        this.status = TenantStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getCustomBranding() {
        return customBranding;
    }

    public void setCustomBranding(String customBranding) {
        this.customBranding = customBranding;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getTenantTier() {
        return tenantTier;
    }

    public void setTenantTier(String tenantTier) {
        this.tenantTier = tenantTier;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxCustomers() {
        return maxCustomers;
    }

    public void setMaxCustomers(Integer maxCustomers) {
        this.maxCustomers = maxCustomers;
    }

    public Long getStorageQuotaMb() {
        return storageQuotaMb;
    }

    public void setStorageQuotaMb(Long storageQuotaMb) {
        this.storageQuotaMb = storageQuotaMb;
    }

    public Integer getApiRateLimit() {
        return apiRateLimit;
    }

    public void setApiRateLimit(Integer apiRateLimit) {
        this.apiRateLimit = apiRateLimit;
    }

    public TenantSettings getSettings() {
        return settings;
    }

    public void setSettings(TenantSettings settings) {
        this.settings = settings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Set<UserTenant> getUserTenants() {
        return userTenants;
    }

    public void setUserTenants(Set<UserTenant> userTenants) {
        this.userTenants = userTenants;
    }
}
