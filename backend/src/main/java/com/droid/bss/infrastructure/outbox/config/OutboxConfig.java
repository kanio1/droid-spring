package com.droid.bss.infrastructure.outbox.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Outbox Pattern Configuration
 *
 * Enables scheduled publishing and transaction management
 */
@Configuration
@EnableScheduling
@EnableTransactionManagement
@Slf4j
public class OutboxConfig {

    // Configuration class for Outbox Pattern
    // The @EnableScheduling annotation enables the @Scheduled annotation
    // used in OutboxEventPublisher for periodic event publishing
}
