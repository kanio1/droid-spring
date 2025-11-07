package com.droid.bss.infrastructure.event.sourcing.projections;

import com.droid.bss.infrastructure.event.sourcing.AbstractProjection;
import com.droid.bss.infrastructure.event.sourcing.StoredEvent;

/**
 * Customer read model projection
 */
public class CustomerReadModel extends AbstractProjection {

    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private int totalOrders;
    private double totalSpent;

    @Override
    public String getName() {
        return "CustomerReadModel";
    }

    @Override
    public void handleEvent(StoredEvent event) {
        String eventType = event.getEventType();

        switch (eventType) {
            case "customer.created":
                handleCustomerCreated(event);
                break;
            case "customer.updated":
                handleCustomerUpdated(event);
                break;
            case "customer.statusChanged":
                handleCustomerStatusChanged(event);
                break;
            case "order.completed":
                handleOrderCompleted(event);
                break;
            default:
                // Ignore other event types
                break;
        }

        updateVersion();
    }

    private void handleCustomerCreated(StoredEvent event) {
        // Parse event data and update read model
        // In a real implementation, you would parse the JSON event data
        String eventData = event.getEventData();
        markUpToDate();
    }

    private void handleCustomerUpdated(StoredEvent event) {
        // Update customer information
        String eventData = event.getEventData();
        markUpToDate();
    }

    private void handleCustomerStatusChanged(StoredEvent event) {
        // Update customer status
        String eventData = event.getEventData();
        markUpToDate();
    }

    private void handleOrderCompleted(StoredEvent event) {
        // Update statistics when order is completed
        totalOrders++;
        markOutOfDate();
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public double getTotalSpent() {
        return totalSpent;
    }
}
