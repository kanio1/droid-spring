package com.droid.bss.infrastructure.cache.advanced;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cache put annotation for updating cache
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {

    /**
     * Cache key
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
}
