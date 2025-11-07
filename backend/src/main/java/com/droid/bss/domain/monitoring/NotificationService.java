package com.droid.bss.domain.monitoring;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for sending notifications via multiple channels
 */
@Service
public class NotificationService {

    private final List<NotificationChannel> channels;

    public NotificationService(List<NotificationChannel> channels) {
        this.channels = channels;
    }

    public void sendNotification(NotificationPreference preference, String severity, String message) {
        if (!preference.shouldNotify(severity)) {
            return;
        }

        for (NotificationChannel channel : channels) {
            if (shouldUseChannel(channel, preference)) {
                try {
                    channel.send(preference, severity, message);
                } catch (Exception e) {
                    // Log error but don't fail the entire notification
                    System.err.println("Failed to send notification via " + channel.getChannelType() + ": " + e.getMessage());
                }
            }
        }
    }

    private boolean shouldUseChannel(NotificationChannel channel, NotificationPreference preference) {
        return switch (channel.getChannelType()) {
            case EMAIL -> preference.isEmailEnabled() && preference.getEmail() != null;
            case SMS -> preference.isSmsEnabled() && preference.getPhoneNumber() != null;
            case SLACK -> preference.isSlackEnabled() && preference.getSlackChannel() != null;
        };
    }
}
