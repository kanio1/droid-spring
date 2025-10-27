package com.droid.bss.domain.customer;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Customer {
    
    private final CustomerId id;
    private final CustomerInfo personalInfo;
    private final ContactInfo contactInfo;
    private CustomerStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final int version;
    
    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods (create, updatePersonalInfo, etc.) for domain operations.
     */
    Customer(
        CustomerId id,
        CustomerInfo personalInfo,
        ContactInfo contactInfo,
        CustomerStatus status,
        LocalDateTime createdAt,
        int version
    ) {
        this.id = Objects.requireNonNull(id, "Customer ID cannot be null");
        this.personalInfo = Objects.requireNonNull(personalInfo, "Personal info cannot be null");
        this.contactInfo = Objects.requireNonNull(contactInfo, "Contact info cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = this.createdAt;
        this.version = version;
    }
    
    public static Customer create(CustomerInfo personalInfo, ContactInfo contactInfo) {
        CustomerId id = CustomerId.generate();
        return new Customer(
            id,
            personalInfo,
            contactInfo,
            CustomerStatus.ACTIVE,
            LocalDateTime.now(),
            1
        );
    }
    
    public Customer updatePersonalInfo(CustomerInfo newPersonalInfo) {
        Objects.requireNonNull(newPersonalInfo, "Personal info cannot be null");
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated customer");
        }
        return new Customer(
            this.id,
            newPersonalInfo,
            this.contactInfo,
            this.status,
            this.createdAt,
            this.version + 1
        );
    }
    
    public Customer updateContactInfo(ContactInfo newContactInfo) {
        Objects.requireNonNull(newContactInfo, "Contact info cannot be null");
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated customer");
        }
        return new Customer(
            this.id,
            this.personalInfo,
            newContactInfo,
            this.status,
            this.createdAt,
            this.version + 1
        );
    }
    
    public Customer changeStatus(CustomerStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");
        
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                "Cannot change status from %s to %s".formatted(this.status, newStatus)
            );
        }
        
        return new Customer(
            this.id,
            this.personalInfo,
            this.contactInfo,
            newStatus,
            this.createdAt,
            this.version + 1
        );
    }
    
    public Customer deactivate() {
        return changeStatus(CustomerStatus.INACTIVE);
    }
    
    public Customer suspend() {
        return changeStatus(CustomerStatus.SUSPENDED);
    }
    
    public Customer reactivate() {
        return changeStatus(CustomerStatus.ACTIVE);
    }
    
    public Customer terminate() {
        return changeStatus(CustomerStatus.TERMINATED);
    }
    
    /**
     * Public factory method for testing purposes.
     * Use factory methods (create, updatePersonalInfo, etc.) for normal domain operations.
     */
    public static Customer testCustomer(CustomerId id, CustomerInfo personalInfo, ContactInfo contactInfo, 
                                       CustomerStatus status, LocalDateTime createdAt, int version) {
        return new Customer(id, personalInfo, contactInfo, status, createdAt, version);
    }
    
    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
    
    public boolean isTerminated() {
        return status == CustomerStatus.TERMINATED;
    }
    
    public boolean canBeModified() {
        return status != CustomerStatus.TERMINATED;
    }
    
    // Getters
    public CustomerId getId() { return id; }
    public CustomerInfo getPersonalInfo() { return personalInfo; }
    public ContactInfo getContactInfo() { return contactInfo; }
    public CustomerStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
}
