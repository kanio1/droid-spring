package com.droid.bss.infrastructure.cache.advanced;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cacheable annotation for method-level caching
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {

    /**
     * Cache key (auto-generated if empty)
     */
    String value() default "";

    /**
     * TTL in seconds
     */
    long ttl() default 300;

    /**
     * Cache namespace
     */
    String namespace() default "";

    /**
     * Whether to use L1 cache only
     */
    boolean l1Only() default false;

    /**
     * Whether to use L2 cache only
     */
    boolean l2Only() default false;
}
