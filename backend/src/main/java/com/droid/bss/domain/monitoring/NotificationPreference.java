package com.droid.bss.domain.monitoring;

import java.util.Objects;

/**
 * Domain entity representing notification preferences for a customer
 */
public class NotificationPreference {

    private Long id;
    private Long customerId;
    private String email;
    private String phoneNumber;
    private String slackChannel;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean slackEnabled;
    private boolean criticalAlertsOnly;
    private Long createdAt;
    private Long updatedAt;

    public NotificationPreference() {
    }

    public NotificationPreference(Long customerId, String email, String phoneNumber,
                                  String slackChannel, boolean emailEnabled, boolean smsEnabled,
                                  boolean slackEnabled, boolean criticalAlertsOnly) {
        this.customerId = customerId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.slackChannel = slackChannel;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.slackEnabled = slackEnabled;
        this.criticalAlertsOnly = criticalAlertsOnly;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public boolean isSlackEnabled() {
        return slackEnabled;
    }

    public void setSlackEnabled(boolean slackEnabled) {
        this.slackEnabled = slackEnabled;
    }

    public boolean isCriticalAlertsOnly() {
        return criticalAlertsOnly;
    }

    public void setCriticalAlertsOnly(boolean criticalAlertsOnly) {
        this.criticalAlertsOnly = criticalAlertsOnly;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean shouldNotify(String severity) {
        if (!emailEnabled && !smsEnabled && !slackEnabled) {
            return false;
        }

        if (criticalAlertsOnly && !"CRITICAL".equals(severity)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationPreference that = (NotificationPreference) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NotificationPreference{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", email='" + email + '\'' +
                ", emailEnabled=" + emailEnabled +
                ", smsEnabled=" + smsEnabled +
                ", slackEnabled=" + slackEnabled +
                ", criticalAlertsOnly=" + criticalAlertsOnly +
                '}';
    }
}
