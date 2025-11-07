package com.droid.bss.infrastructure.messaging.metrics;

/**
 * Event Processing Health Status
 */
public class EventHealthStatus {
    private final String status;
    private final String message;
    private final double successRate;

    public EventHealthStatus(String status, String message, double successRate) {
        this.status = status;
        this.message = message;
        this.successRate = successRate;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public double getSuccessRate() {
        return successRate;
    }
}
