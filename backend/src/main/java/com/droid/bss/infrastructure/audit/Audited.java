package com.droid.bss.infrastructure.audit;

import com.droid.bss.domain.audit.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Audit Annotation
 *
 * Marks methods that should be automatically audited
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The action type for this operation
     */
    AuditAction action();

    /**
     * The entity type being operated on
     */
    String entityType() default "";

    /**
     * A description template (can use {arg0}, {arg1}, etc. for parameters)
     */
    String description() default "";
}
