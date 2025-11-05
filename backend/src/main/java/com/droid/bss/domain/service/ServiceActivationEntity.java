package com.droid.bss.domain.service;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a service activation for a specific customer
 */
@Entity
@Table(name = "service_activations")
public class ServiceActivationEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivationStatus status;

    @Column(name = "activation_date")
    private LocalDateTime activationDate;

    @Column(name = "deactivation_date")
    private LocalDateTime deactivationDate;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @Size(max = 1000)
    @Column(name = "activation_notes", columnDefinition = "TEXT")
    private String activationNotes;

    @Size(max = 1000)
    @Column(name = "deactivation_notes", columnDefinition = "TEXT")
    private String deactivationNotes;

    @Column(name = "correlation_id")
    private String correlationId;

    @OneToMany(mappedBy = "activation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceActivationStepEntity> steps = new ArrayList<>();

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    protected ServiceActivationEntity() {
    }

    public ServiceActivationEntity(
            CustomerEntity customer,
            ServiceEntity service,
            ActivationStatus status) {
        this.customer = customer;
        this.service = service;
        this.status = status;
    }

    // Getters and Setters
    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public ActivationStatus getStatus() {
        return status;
    }

    public void setStatus(ActivationStatus status) {
        this.status = status;
    }

    public LocalDateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDateTime activationDate) {
        this.activationDate = activationDate;
    }

    public LocalDateTime getDeactivationDate() {
        return deactivationDate;
    }

    public void setDeactivationDate(LocalDateTime deactivationDate) {
        this.deactivationDate = deactivationDate;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getActivationNotes() {
        return activationNotes;
    }

    public void setActivationNotes(String activationNotes) {
        this.activationNotes = activationNotes;
    }

    public String getDeactivationNotes() {
        return deactivationNotes;
    }

    public void setDeactivationNotes(String deactivationNotes) {
        this.deactivationNotes = deactivationNotes;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<ServiceActivationStepEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<ServiceActivationStepEntity> steps) {
        this.steps = steps;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    // Business methods
    public void addStep(ServiceActivationStepEntity step) {
        this.steps.add(step);
        step.setActivation(this);
    }

    public void removeStep(ServiceActivationStepEntity step) {
        this.steps.remove(step);
        step.setActivation(null);
    }

    public boolean isActive() {
        return ActivationStatus.ACTIVE == this.status;
    }

    public boolean isProvisioning() {
        return ActivationStatus.PROVISIONING == this.status;
    }

    public boolean isPending() {
        return ActivationStatus.PENDING == this.status || ActivationStatus.SCHEDULED == this.status;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
