/**
 * Update Tenant Command DTO
 *
 * Command object for updating a tenant
 * All fields are optional for partial updates
 */
package com.droid.bss.application.dto.tenant;

import jakarta.validation.constraints.*;

import java.util.UUID;

public class UpdateTenantCommand {

    @Size(max = 100)
    private String name;

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

    @Size(max = 50)
    private String timezone;

    @Size(max = 10)
    private String locale;

    @Size(max = 10)
    private String currency;

    @Size(max = 50)
    private String industry;

    @Min(1)
    private Integer maxUsers;

    @Min(1)
    private Integer maxCustomers;

    @Min(1)
    private Long storageQuotaMb;

    @Min(1)
    private Integer apiRateLimit;

    // Constructors
    public UpdateTenantCommand() {}

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
