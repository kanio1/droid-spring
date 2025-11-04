package com.droid.bss.domain.service;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Represents a step in the service activation process
 */
@Entity
@Table(name = "service_activation_steps")
public class ServiceActivationStepEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activation_id", nullable = false)
    private ServiceActivationEntity activation;

    @NotNull
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @NotNull
    @Size(max = 100)
    @Column(nullable = false)
    private String stepName;

    @Size(max = 500)
    @Column(name = "step_description", columnDefinition = "TEXT")
    private String stepDescription;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceActivationStepStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Size(max = 1000)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Size(max = 200)
    @Column(name = "provisioning_system")
    private String provisioningSystem;

    @Size(max = 500)
    @Column(name = "provisioning_command")
    private String provisioningCommand;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    protected ServiceActivationStepEntity() {
    }

    public ServiceActivationStepEntity(
            Integer stepOrder,
            String stepName,
            String stepDescription,
            ServiceActivationStepStatus status) {
        this.stepOrder = stepOrder;
        this.stepName = stepName;
        this.stepDescription = stepDescription;
        this.status = status;
    }

    // Getters and Setters
    public ServiceActivationEntity getActivation() {
        return activation;
    }

    public void setActivation(ServiceActivationEntity activation) {
        this.activation = activation;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStepDescription() {
        return stepDescription;
    }

    public void setStepDescription(String stepDescription) {
        this.stepDescription = stepDescription;
    }

    public ServiceActivationStepStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceActivationStepStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public String getProvisioningSystem() {
        return provisioningSystem;
    }

    public void setProvisioningSystem(String provisioningSystem) {
        this.provisioningSystem = provisioningSystem;
    }

    public String getProvisioningCommand() {
        return provisioningCommand;
    }

    public void setProvisioningCommand(String provisioningCommand) {
        this.provisioningCommand = provisioningCommand;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    // Business methods
    public void start() {
        this.status = ServiceActivationStepStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ServiceActivationStepStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        if (this.startedAt != null) {
            this.executionTimeMs = java.time.Duration.between(
                this.startedAt,
                this.completedAt
            ).toMillis();
        }
    }

    public void fail(String errorMessage) {
        this.status = ServiceActivationStepStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return ServiceActivationStepStatus.COMPLETED == this.status;
    }

    public boolean isInProgress() {
        return ServiceActivationStepStatus.IN_PROGRESS == this.status;
    }

    public boolean isFailed() {
        return ServiceActivationStepStatus.FAILED == this.status;
    }

    public boolean isPending() {
        return ServiceActivationStepStatus.PENDING == this.status;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
