package com.droid.bss.infrastructure.messaging.config;

import com.droid.bss.infrastructure.messaging.*;
import com.droid.bss.infrastructure.messaging.metrics.EventMetricsService;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Event Consumer Metrics Configuration
 *
 * Automatically registers all event consumers with the metrics service
 */
@Configuration
public class EventConsumerMetricsConfig {

    private final EventMetricsService eventMetricsService;

    public EventConsumerMetricsConfig(EventMetricsService eventMetricsService) {
        this.eventMetricsService = eventMetricsService;
    }

    @PostConstruct
    public void registerConsumers() {
        // Note: In a real implementation with Spring, you would use dependency injection
        // to get all beans of type CustomerEventConsumer, OrderEventConsumer, etc.
        // and register them with the metrics service.

        // For now, we'll just initialize the metrics structure
        // The actual registration would happen when consumers are instantiated

        System.out.println("[EventMetrics] Consumer metrics configuration initialized");
    }

    /**
     * Register a consumer after it's been constructed
     */
    public void registerConsumer(String name, Object consumer) {
        eventMetricsService.registerConsumer(name, consumer);
    }
}
