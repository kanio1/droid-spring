package com.droid.bss.infrastructure.event.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Spring configuration for event handling infrastructure.
 *
 * @since 1.0
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(EventHandlerProperties.class)
@ConditionalOnProperty(name = "app.event.handler.enabled", havingValue = "true", matchIfMissing = true)
public class EventHandlerConfig {

    private final EventHandlerProperties handlerProperties;

    public EventHandlerConfig(EventHandlerProperties handlerProperties) {
        this.handlerProperties = handlerProperties;
    }

    /**
     * Creates the event handler registry.
     *
     * @return the registry bean
     */
    @Bean
    @ConditionalOnMissingBean
    public EventHandlerRegistry eventHandlerRegistry() {
        return new EventHandlerRegistry();
    }

    /**
     * Creates the CloudEvent handler.
     *
     * @param registry the event handler registry
     * @param objectMapper the object mapper
     * @param executor the executor for async processing
     * @return the CloudEvent handler
     */
    @Bean
    @ConditionalOnMissingBean
    public CloudEventHandler cloudEventHandler(EventHandlerRegistry registry,
                                                ObjectMapper objectMapper,
                                                Executor executor) {
        return new CloudEventHandler(
            registry,
            objectMapper,
            executor,
            handlerProperties.getAutoAcknowledge()
        );
    }

    /**
     * Creates the async executor for event handlers.
     *
     * @return the executor
     */
    @Bean
    public Executor eventHandlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(handlerProperties.getConcurrency());
        executor.setMaxPoolSize(handlerProperties.getConcurrency() * 2);
        executor.setQueueCapacity(handlerProperties.getBatchSize());
        executor.setThreadNamePrefix("event-handler-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
