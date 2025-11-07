/**
 * Tenant DTO
 *
 * Data Transfer Object for tenant information
 */
package com.droid.bss.application.dto.tenant;

import com.droid.bss.domain.tenant.TenantSettings;
import com.droid.bss.domain.tenant.TenantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TenantDto {

    private UUID id;
    private String name;
    private String code;
    private String domain;
    private String contactEmail;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private TenantStatus status;
    private String customBranding;
    private String logoUrl;
    private String timezone;
    private String locale;
    private String currency;
    private String industry;
    private String tenantTier;
    private Integer maxUsers;
    private Integer maxCustomers;
    private Long storageQuotaMb;
    private Integer apiRateLimit;
    private TenantSettings settings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // Constructors
    public TenantDto() {}

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
}
