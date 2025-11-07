package com.droid.bss.infrastructure.outbox.annotation;

import org.springframework.context.annotation.Import;
import com.droid.bss.infrastructure.outbox.config.OutboxConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Outbox Pattern
 *
 * Annotation to enable Outbox Pattern functionality
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({OutboxConfig.class})
public @interface EnableOutbox {
}
