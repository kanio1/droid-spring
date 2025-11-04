package com.droid.bss.domain.customer.event;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for Customer-related CloudEvents
 * Implements CloudEvents v1.0 specification
 */
public abstract class CustomerEvent {

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
    protected final UUID customerId;
    protected final String firstName;
    protected final String lastName;
    protected final String email;
    protected final CustomerStatus status;
    protected final LocalDateTime occurredAt;

    protected CustomerEvent(
            String eventType,
            CustomerEntity customer,
            CustomerStatus previousStatus
    ) {
        this.id = UUID.randomUUID().toString();
        this.source = "urn:droid:bss:customer:" + customer.getId();
        this.type = eventType;
        this.specversion = "1.0";
        this.datacontenttype = "application/json";
        this.time = LocalDateTime.now();

        this.customerId = customer.getId();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.email = customer.getEmail();
        this.status = customer.getStatus();
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
    public UUID getCustomerId() { return customerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public CustomerStatus getStatus() { return status; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * Event fired when a new customer is created
 */
class CustomerCreatedEvent extends CustomerEvent {

    public CustomerCreatedEvent(CustomerEntity customer) {
        super("com.droid.bss.customer.created.v1", customer, null);
    }
}

/**
 * Event fired when customer information is updated
 */
class CustomerUpdatedEvent extends CustomerEvent {

    public CustomerUpdatedEvent(CustomerEntity customer) {
        super("com.droid.bss.customer.updated.v1", customer, null);
    }
}

/**
 * Event fired when customer status is changed
 */
class CustomerStatusChangedEvent extends CustomerEvent {

    private final CustomerStatus previousStatus;

    public CustomerStatusChangedEvent(CustomerEntity customer, CustomerStatus previousStatus) {
        super("com.droid.bss.customer.statusChanged.v1", customer, previousStatus);
        this.previousStatus = previousStatus;
    }

    public CustomerStatus getPreviousStatus() {
        return previousStatus;
    }
}

/**
 * Event fired when customer is terminated
 */
class CustomerTerminatedEvent extends CustomerEvent {

    private final String terminationReason;

    public CustomerTerminatedEvent(CustomerEntity customer, String terminationReason) {
        super("com.droid.bss.customer.terminated.v1", customer, null);
        this.terminationReason = terminationReason;
    }

    public String getTerminationReason() { return terminationReason; }
}
