package com.droid.bss.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for rate limiting API endpoints
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiting {

    /**
     * Maximum number of requests allowed in the time window
     */
    int value() default 100;

    /**
     * Time window in seconds
     */
    int timeWindow() default 60;

    /**
     * Redis key prefix
     */
    String keyPrefix() default "rate_limit";
}
