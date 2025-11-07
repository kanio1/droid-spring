package com.droid.bss.application.dto.monitoring;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for notification preference requests
 */
public class NotificationPreferenceRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @Email(message = "Email must be valid")
    private String email;

    private String phoneNumber;

    private String slackChannel;

    @NotNull(message = "Email enabled flag is required")
    private Boolean emailEnabled;

    @NotNull(message = "SMS enabled flag is required")
    private Boolean smsEnabled;

    @NotNull(message = "Slack enabled flag is required")
    private Boolean slackEnabled;

    @NotNull(message = "Critical alerts only flag is required")
    private Boolean criticalAlertsOnly;

    public NotificationPreferenceRequest() {
    }

    public NotificationPreferenceRequest(Long customerId, String email, String phoneNumber,
                                        String slackChannel, Boolean emailEnabled,
                                        Boolean smsEnabled, Boolean slackEnabled,
                                        Boolean criticalAlertsOnly) {
        this.customerId = customerId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.slackChannel = slackChannel;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.slackEnabled = slackEnabled;
        this.criticalAlertsOnly = criticalAlertsOnly;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSlackChannel() {
        return slackChannel;
    }

    public void setSlackChannel(String slackChannel) {
        this.slackChannel = slackChannel;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public Boolean getSlackEnabled() {
        return slackEnabled;
    }

    public void setSlackEnabled(Boolean slackEnabled) {
        this.slackEnabled = slackEnabled;
    }

    public Boolean getCriticalAlertsOnly() {
        return criticalAlertsOnly;
    }

    public void setCriticalAlertsOnly(Boolean criticalAlertsOnly) {
        this.criticalAlertsOnly = criticalAlertsOnly;
    }
}
