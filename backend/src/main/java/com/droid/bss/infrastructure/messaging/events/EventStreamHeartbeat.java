package com.droid.bss.infrastructure.messaging.events;

import com.droid.bss.api.events.EventsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Event Stream Heartbeat
 *
 * Sends periodic heartbeats to keep SSE connections alive
 */
@Component
public class EventStreamHeartbeat {

    private static final Logger log = LoggerFactory.getLogger(EventStreamHeartbeat.class);

    private final EventsController eventsController;

    public EventStreamHeartbeat(EventsController eventsController) {
        this.eventsController = eventsController;
    }

    /**
     * Send heartbeat every 30 seconds to keep SSE connections alive
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        try {
            int connectedClients = eventsController.getConnectedClients();

            if (connectedClients > 0) {
                log.debug("Sending heartbeat to {} SSE clients", connectedClients);
                eventsController.sendHeartbeat();
            }
        } catch (Exception e) {
            log.error("Error sending heartbeat to SSE clients", e);
        }
    }
}
