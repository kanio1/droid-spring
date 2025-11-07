package com.droid.bss.domain.customer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Customer Domain Event
 * Used for broadcasting customer-related events via RSocket and Kafka
 */
public class CustomerEvent {

    private final String eventType;
    private final UUID customerId;
    private final String customerName;
    private final String email;
    private final CustomerStatus status;
    private final LocalDateTime timestamp;
    private final Map<String, Object> data;

    public CustomerEvent(String eventType, UUID customerId, String customerName,
                        String email, CustomerStatus status,
                        LocalDateTime timestamp, Map<String, Object> data) {
        this.eventType = eventType;
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.status = status;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static CustomerEvent created(UUID customerId, String customerName,
                                       String email, CustomerStatus status) {
        return new CustomerEvent(
            "CUSTOMER_CREATED",
            customerId,
            customerName,
            email,
            status,
            LocalDateTime.now(),
            Map.of("operation", "create")
        );
    }

    public static CustomerEvent updated(UUID customerId, String customerName,
                                       String email, CustomerStatus status) {
        return new CustomerEvent(
            "CUSTOMER_UPDATED",
            customerId,
            customerName,
            email,
            status,
            LocalDateTime.now(),
            Map.of("operation", "update")
        );
    }

    public static CustomerEvent deleted(UUID customerId, String customerName,
                                       String email) {
        return new CustomerEvent(
            "CUSTOMER_DELETED",
            customerId,
            customerName,
            email,
            null,
            LocalDateTime.now(),
            Map.of("operation", "delete")
        );
    }

    // Getters
    public String getEventType() { return eventType; }
    public UUID getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public CustomerStatus getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}
