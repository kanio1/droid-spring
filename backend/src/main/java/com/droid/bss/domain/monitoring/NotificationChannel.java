package com.droid.bss.domain.monitoring;

/**
 * Interface for notification channels (email, SMS, Slack)
 */
public interface NotificationChannel {

    enum ChannelType {
        EMAIL, SMS, SLACK
    }

    ChannelType getChannelType();

    void send(NotificationPreference preference, String severity, String message);
}
