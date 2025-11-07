package com.droid.bss.api.events;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events (SSE) Controller
 *
 * Provides real-time event streaming to connected clients
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Event streaming API")
public class EventsController {

    private static final Logger log = LoggerFactory.getLogger(EventsController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Object> eventCache = new ConcurrentHashMap<>();

    /**
     * Server-Sent Events endpoint for real-time event streaming
     *
     * @return SseEmitter for event streaming
     */
    @GetMapping("/stream")
    @Operation(
        summary = "Stream events via Server-Sent Events",
        description = "Establishes a persistent connection to receive real-time events"
    )
    @ApiResponse(responseCode = "200", description = "SSE connection established")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public SseEmitter streamEvents() {
        SseEmitter emitter = new SseEmitter(0L); // No timeout

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of(
                    "status", "connected",
                    "timestamp", LocalDateTime.now().format(FORMATTER),
                    "message", "Event stream connected"
                ))
            );
        } catch (IOException e) {
            log.error("Failed to send initial event", e);
        }

        // Store emitter for broadcasting
        emitters.add(emitter);

        // Clean up on completion
        emitter.onCompletion(() -> {
            log.info("SSE connection completed");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timed out");
            emitters.remove(emitter);
        });

        emitter.onError(throwable -> {
            log.error("SSE connection error", throwable);
            emitters.remove(emitter);
        });

        return emitter;
    }

    /**
     * Send event to all connected SSE clients
     */
    public void broadcastEvent(String eventName, Object data) {
        CompletableFuture.runAsync(() -> {
            String eventId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().format(FORMATTER);

            // Cache the event
            eventCache.put(eventId, data);

            // Create event payload
            Map<String, Object> eventPayload = Map.of(
                "id", eventId,
                "name", eventName,
                "data", data,
                "timestamp", timestamp
            );

            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name(eventName)
                        .id(eventId)
                        .data(eventPayload)
                    );
                    return false; // Keep emitter
                } catch (IOException e) {
                    log.warn("Failed to send event to SSE client, removing emitter", e);
                    return true; // Remove emitter
                }
            });
        });
    }

    /**
     * Send heartbeat to keep connections alive
     */
    public void sendHeartbeat() {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(Map.of(
                        "timestamp", timestamp,
                        "type", "keepalive"
                    ))
                );
                return false;
            } catch (IOException e) {
                log.warn("Failed to send heartbeat to SSE client, removing emitter", e);
                return true;
            }
        });
    }

    /**
     * Get recent events from cache
     */
    public Map<String, Object> getRecentEvents(int limit) {
        return eventCache.entrySet().stream()
            .skip(Math.max(0, eventCache.size() - limit))
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    /**
     * Clear event cache
     */
    public void clearCache() {
        eventCache.clear();
    }

    /**
     * Get number of connected clients
     */
    public int getConnectedClients() {
        return emitters.size();
    }
}
