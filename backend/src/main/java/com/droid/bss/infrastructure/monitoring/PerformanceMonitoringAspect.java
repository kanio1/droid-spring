package com.droid.bss.infrastructure.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance Monitoring Aspect
 * Automatically monitors and records performance metrics for key business operations
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);

    private final BssMetrics bssMetrics;

    public PerformanceMonitoringAspect(BssMetrics bssMetrics) {
        this.bssMetrics = bssMetrics;
    }

    /**
     * Monitor Kafka event publishing
     */
    @Around("@annotation(MonitorKafkaProcessing)")
    public Object monitorKafkaProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            bssMetrics.recordEvent("kafka.publish", "kafka", "system");
            return result;
        } catch (Exception e) {
            bssMetrics.recordKafkaError();
            log.error("Kafka processing error in {}", joinPoint.getSignature().getName(), e);
            throw e;
        } finally {
            long duration = System.nanoTime() - start;
            bssMetrics.recordKafkaProcessingTime(duration);
        }
    }

    /**
     * Monitor Redis operations
     */
    @Around("@annotation(MonitorRedisProcessing)")
    public Object monitorRedisProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            bssMetrics.recordEvent("redis.operation", "redis", "system");
            return result;
        } catch (Exception e) {
            bssMetrics.recordRedisError();
            log.error("Redis processing error in {}", joinPoint.getSignature().getName(), e);
            throw e;
        } finally {
            long duration = System.nanoTime() - start;
            bssMetrics.recordRedisProcessingTime(duration);
        }
    }

    /**
     * Monitor PostgreSQL operations
     */
    @Around("@annotation(MonitorPostgresProcessing)")
    public Object monitorPostgresProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            bssMetrics.recordEvent("postgres.operation", "postgresql", "system");
            return result;
        } catch (Exception e) {
            bssMetrics.recordPostgreSQLError();
            log.error("PostgreSQL processing error in {}", joinPoint.getSignature().getName(), e);
            throw e;
        } finally {
            long duration = System.nanoTime() - start;
            bssMetrics.recordPostgresProcessingTime(duration);
        }
    }

    /**
     * Monitor order processing
     */
    @Around("@annotation(MonitorOrderProcessing)")
    public Object monitorOrderProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            bssMetrics.recordOrder();
            bssMetrics.recordEvent("order.processed", "order-service", "system");
            return result;
        } catch (Exception e) {
            bssMetrics.recordError("order.processing");
            log.error("Order processing error in {}", joinPoint.getSignature().getName(), e);
            throw e;
        } finally {
            long duration = System.nanoTime() - start;
            bssMetrics.recordOrderProcessingTime(duration);
        }
    }

    /**
     * Monitor payment processing
     */
    @Around("@annotation(MonitorPaymentProcessing)")
    public Object monitorPaymentProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            bssMetrics.recordPayment();
            bssMetrics.recordEvent("payment.processed", "payment-service", "system");
            return result;
        } catch (Exception e) {
            bssMetrics.recordError("payment.processing");
            log.error("Payment processing error in {}", joinPoint.getSignature().getName(), e);
            throw e;
        } finally {
            long duration = System.nanoTime() - start;
            bssMetrics.recordPaymentProcessingTime(duration);
        }
    }

    /**
     * Monitor general event processing
     */
    @Around("@annotation(MonitorEventProcessing)")
    public Object monitorEventProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            bssMetrics.recordEvent();
            return result;
        } catch (Exception e) {
            bssMetrics.recordError("event.processing");
            log.error("Event processing error in {}", joinPoint.getSignature().getName(), e);
            throw e;
        } finally {
            long duration = System.nanoTime() - start;
            bssMetrics.recordEventProcessingTime(duration);
        }
    }
}
