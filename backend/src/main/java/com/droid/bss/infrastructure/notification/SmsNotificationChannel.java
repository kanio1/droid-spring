package com.droid.bss.infrastructure.notification;

import com.droid.bss.domain.monitoring.NotificationChannel;
import com.droid.bss.domain.monitoring.NotificationPreference;
import org.springframework.stereotype.Component;

/**
 * SMS notification channel implementation
 * In production, integrate with Twilio, AWS SNS, or similar service
 */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SMS;
    }

    @Override
    public void send(NotificationPreference preference, String severity, String message) {
        // In production, integrate with actual SMS provider like Twilio
        String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        String authToken = System.getenv("TWILIO_AUTH_TOKEN");
        String fromNumber = System.getenv("TWILIO_FROM_NUMBER");

        if (accountSid == null || authToken == null || fromNumber == null) {
            // Simulation mode
            System.out.println("[SMS SIMULATION] To: " + preference.getPhoneNumber());
            System.out.println("[SMS SIMULATION] Severity: " + severity);
            System.out.println("[SMS SIMULATION] Message: " + message);
            return;
        }

        // TODO: Implement actual Twilio integration
        // Example:
        // Twilio.init(accountSid, authToken);
        // Message.creator(new PhoneNumber(preference.getPhoneNumber()),
        //                 new PhoneNumber(fromNumber),
        //                 "Alert: " + severity + " - " + message)
        //     .create();

        System.out.println("SMS sent successfully to: " + preference.getPhoneNumber());
    }
}
