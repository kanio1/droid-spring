package com.droid.bss.infrastructure.notification;

import com.droid.bss.domain.monitoring.NotificationChannel;
import com.droid.bss.domain.monitoring.NotificationPreference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Slack notification channel implementation
 */
@Component
public class SlackNotificationChannel implements NotificationChannel {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SlackNotificationChannel() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SLACK;
    }

    @Override
    public void send(NotificationPreference preference, String severity, String message) {
        String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");

        if (webhookUrl == null) {
            // Simulation mode
            System.out.println("[SLACK SIMULATION] Channel: " + preference.getSlackChannel());
            System.out.println("[SLACK SIMULATION] Severity: " + severity);
            System.out.println("[SLACK SIMULATION] Message: " + message);
            return;
        }

        try {
            // Create Slack message payload using Map
            var payload = new java.util.HashMap<String, Object>();
            payload.put("channel", preference.getSlackChannel());
            payload.put("text", "Alert Notification: " + severity);
            payload.put("username", "BSS Monitoring Bot");

            var attachment = new java.util.HashMap<String, Object>();
            attachment.put("text", message);
            attachment.put("color", "danger".equals(severity.toLowerCase()) ? "danger" : "warning");
            attachment.put("mrkdwn_in", new String[]{"text"});

            var attachments = new java.util.ArrayList<>();
            attachments.add(attachment);
            payload.put("attachments", attachments);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            restTemplate.postForEntity(webhookUrl, jsonPayload, String.class);
            System.out.println("Slack message sent successfully to channel: " + preference.getSlackChannel());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
}
