package com.droid.bss.domain.service.event;

import com.droid.bss.domain.service.*;
import com.droid.bss.domain.customer.CustomerEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for Service-related CloudEvents
 * Implements CloudEvents v1.0 specification
 */
public abstract class ServiceEvent {

    /**
     * CloudEvents required fields
     */
    protected final String id;
    protected final String source;
    protected final String type;
    protected final String specversion;
    protected final String datacontenttype;
    protected final LocalDateTime time;

    /**
     * Event data
     */
    protected final UUID serviceId;
    protected final String serviceCode;
    protected final String serviceName;
    protected final ServiceType serviceType;
    protected final ServiceStatus status;
    protected final LocalDateTime occurredAt;

    protected ServiceEvent(
            String eventType,
            ServiceEntity service
    ) {
        this.id = UUID.randomUUID().toString();
        this.source = "urn:droid:bss:service:" + service.getId();
        this.type = eventType;
        this.specversion = "1.0";
        this.datacontenttype = "application/json";
        this.time = LocalDateTime.now();

        this.serviceId = service.getId();
        this.serviceCode = service.getServiceCode();
        this.serviceName = service.getServiceName();
        this.serviceType = service.getServiceType();
        this.status = service.getStatus();
        this.occurredAt = LocalDateTime.now();
    }

    // Getters for CloudEvents required fields
    public String getId() { return id; }
    public String getSource() { return source; }
    public String getType() { return type; }
    public String getSpecversion() { return specversion; }
    public String getDatacontenttype() { return datacontenttype; }
    public LocalDateTime getTime() { return time; }

    // Getters for event data
    public UUID getServiceId() { return serviceId; }
    public String getServiceCode() { return serviceCode; }
    public String getServiceName() { return serviceName; }
    public ServiceType getServiceType() { return serviceType; }
    public ServiceStatus getStatus() { return status; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * Event fired when a new service is created
 */
class ServiceCreatedEvent extends ServiceEvent {

    public ServiceCreatedEvent(ServiceEntity service) {
        super("com.droid.bss.service.created.v1", service);
    }
}

/**
 * Event fired when service information is updated
 */
class ServiceUpdatedEvent extends ServiceEvent {

    public ServiceUpdatedEvent(ServiceEntity service) {
        super("com.droid.bss.service.updated.v1", service);
    }
}

/**
 * Event fired when service is activated
 */
class ServiceActivatedEvent extends ServiceEvent {

    private final UUID customerId;
    private final String customerEmail;
    private final LocalDateTime activationDate;

    public ServiceActivatedEvent(ServiceEntity service, ServiceActivationEntity activation) {
        super("com.droid.bss.service.activated.v1", service);
        this.customerId = activation.getCustomer().getId();
        this.customerEmail = activation.getCustomer().getEmail();
        this.activationDate = activation.getActivationDate() != null
            ? activation.getActivationDate()
            : LocalDateTime.now();
    }

    public UUID getCustomerId() { return customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public LocalDateTime getActivationDate() { return activationDate; }
}

/**
 * Event fired when service activation is completed successfully
 */
class ServiceActivationCompletedEvent extends ServiceEvent {

    private final UUID customerId;
    private final UUID activationId;
    private final String correlationId;
    private final LocalDateTime completedAt;

    public ServiceActivationCompletedEvent(ServiceEntity service, ServiceActivationEntity activation) {
        super("com.droid.bss.service.activation.completed.v1", service);
        this.customerId = activation.getCustomer().getId();
        this.activationId = activation.getId();
        this.correlationId = activation.getCorrelationId();
        this.completedAt = LocalDateTime.now();
    }

    public UUID getCustomerId() { return customerId; }
    public UUID getActivationId() { return activationId; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

/**
 * Event fired when service activation fails
 */
class ServiceActivationFailedEvent extends ServiceEvent {

    private final UUID customerId;
    private final UUID activationId;
    private final String errorMessage;
    private final int retryCount;
    private final LocalDateTime failedAt;

    public ServiceActivationFailedEvent(ServiceEntity service, ServiceActivationEntity activation, String errorMessage) {
        super("com.droid.bss.service.activation.failed.v1", service);
        this.customerId = activation.getCustomer().getId();
        this.activationId = activation.getId();
        this.errorMessage = errorMessage;
        this.retryCount = activation.getRetryCount() != null ? activation.getRetryCount() : 0;
        this.failedAt = LocalDateTime.now();
    }

    public UUID getCustomerId() { return customerId; }
    public UUID getActivationId() { return activationId; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getFailedAt() { return failedAt; }
}

/**
 * Event fired when service deactivation starts
 */
class ServiceDeactivatedEvent extends ServiceEvent {

    private final UUID customerId;
    private final String reason;
    private final LocalDateTime deactivationDate;

    public ServiceDeactivatedEvent(ServiceEntity service, UUID customerId, String reason) {
        super("com.droid.bss.service.deactivated.v1", service);
        this.customerId = customerId;
        this.reason = reason;
        this.deactivationDate = LocalDateTime.now();
    }

    public UUID getCustomerId() { return customerId; }
    public String getReason() { return reason; }
    public LocalDateTime getDeactivationDate() { return deactivationDate; }
}

/**
 * Event fired when service deactivation is completed
 */
class ServiceDeactivationCompletedEvent extends ServiceEvent {

    private final UUID customerId;
    private final UUID deactivationId;
    private final LocalDateTime completedAt;

    public ServiceDeactivationCompletedEvent(ServiceEntity service, UUID customerId, UUID deactivationId) {
        super("com.droid.bss.service.deactivation.completed.v1", service);
        this.customerId = customerId;
        this.deactivationId = deactivationId;
        this.completedAt = LocalDateTime.now();
    }

    public UUID getCustomerId() { return customerId; }
    public UUID getDeactivationId() { return deactivationId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

/**
 * Event fired when service activation status changes
 */
class ServiceActivationStatusChangedEvent extends ServiceEvent {

    private final UUID activationId;
    private final UUID customerId;
    private final ActivationStatus previousStatus;
    private final ActivationStatus newStatus;
    private final String correlationId;
    private final LocalDateTime changedAt;

    public ServiceActivationStatusChangedEvent(
            ServiceEntity service,
            ServiceActivationEntity activation,
            ActivationStatus previousStatus) {
        super("com.droid.bss.service.activation.statusChanged.v1", service);
        this.activationId = activation.getId();
        this.customerId = activation.getCustomer().getId();
        this.previousStatus = previousStatus;
        this.newStatus = activation.getStatus();
        this.correlationId = activation.getCorrelationId();
        this.changedAt = LocalDateTime.now();
    }

    public UUID getActivationId() { return activationId; }
    public UUID getCustomerId() { return customerId; }
    public ActivationStatus getPreviousStatus() { return previousStatus; }
    public ActivationStatus getNewStatus() { return newStatus; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
