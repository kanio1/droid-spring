package com.droid.bss.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Security Annotations
 * Annotations for RBAC and security control
 */
public class SecurityAnnotations {

    /**
     * Require specific permission
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequirePermission {
        String value();
    }

    /**
     * Require any of the specified permissions
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireAnyPermission {
        String[] value();
    }

    /**
     * Require all specified permissions
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireAllPermissions {
        String[] value();
    }

    /**
     * Require specific role
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireRole {
        String value();
    }

    /**
     * Require any of the specified roles
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireAnyRole {
        String[] value();
    }

    /**
     * Require admin role
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireAdmin {
    }

    /**
     * Require authenticated user
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireAuthenticated {
    }

    /**
     * Resource owner permission
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireOwner {
        String value(); // Parameter name containing owner ID
    }

    /**
     * Rate limiting
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimited {
        int requests() default 100;
        int windowSeconds() default 60;
    }

    /**
     * Audit log
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AuditLog {
        String action();
        String entity();
    }
}
