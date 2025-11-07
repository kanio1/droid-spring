package com.droid.bss.infrastructure.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Monitoring Annotations
 * Used to automatically monitor and record performance metrics
 */
public class MonitoringAnnotations {

    /**
     * Monitor Kafka processing operations
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorKafkaProcessing {
    }

    /**
     * Monitor Redis processing operations
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorRedisProcessing {
    }

    /**
     * Monitor PostgreSQL processing operations
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorPostgresProcessing {
    }

    /**
     * Monitor order processing operations
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorOrderProcessing {
    }

    /**
     * Monitor payment processing operations
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorPaymentProcessing {
    }

    /**
     * Monitor general event processing operations
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MonitorEventProcessing {
    }
}
