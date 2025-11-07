package com.droid.bss.infrastructure.notification;

import com.droid.bss.domain.monitoring.NotificationChannel;
import com.droid.bss.domain.monitoring.NotificationPreference;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of EmailNotificationChannel
 * Minimal implementation for testing purposes
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(NotificationPreference preference, String severity, String message) {
        // Stub implementation
        System.out.println("[EMAIL SIMULATION] To: " + preference.getEmail());
        System.out.println("[EMAIL SIMULATION] Severity: " + severity);
        System.out.println("[EMAIL SIMULATION] Message: " + message);
    }
}
