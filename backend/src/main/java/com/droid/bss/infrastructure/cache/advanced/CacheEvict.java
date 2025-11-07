package com.droid.bss.infrastructure.cache.advanced;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cache evict annotation for invalidating cache
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {

    /**
     * Cache key pattern to evict
     */
    String value() default "";

    /**
     * All entries flag
     */
    boolean allEntries() default false;

    /**
     * Cache namespace
     */
    String namespace() default "";
}
