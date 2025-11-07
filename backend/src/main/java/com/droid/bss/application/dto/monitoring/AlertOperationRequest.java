package com.droid.bss.application.dto.monitoring;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for alert operation requests (acknowledge, resolve)
 */
public class AlertOperationRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    public AlertOperationRequest() {
    }

    public AlertOperationRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
