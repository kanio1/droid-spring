package com.droid.bss.domain.user;

import java.util.Objects;

/**
 * Permission entity - represents a specific permission in the system.
 * Implements Comparable for natural ordering in TreeSet.
 */
public class Permission implements Comparable<Permission> {

    private final String resource;
    private final String action;
    private final String description;

    public Permission(String resource, String action, String description) {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("Resource cannot be null or blank");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action cannot be null or blank");
        }
        this.resource = resource;
        this.action = action;
        this.description = description;
    }

    /**
     * Creates a standard permission.
     */
    public static Permission of(String resource, String action) {
        return new Permission(resource, action, null);
    }

    /**
     * Creates a permission with description.
     */
    public static Permission of(String resource, String action, String description) {
        return new Permission(resource, action, description);
    }

    /**
     * Creates a permission from a combined string "resource:action".
     */
    public static Permission fromString(String resourceAction) {
        if (resourceAction == null || resourceAction.isBlank()) {
            throw new IllegalArgumentException("Resource-action string cannot be null or blank");
        }
        String[] parts = resourceAction.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid resource-action format. Expected 'resource:action'");
        }
        return new Permission(parts[0].trim(), parts[1].trim(), null);
    }

    /**
     * Updates the description of the permission.
     */
    public Permission updateDescription(String newDescription) {
        return new Permission(resource, action, newDescription);
    }

    /**
     * Gets the permission key in "resource:action" format.
     */
    public String getKey() {
        return resource + ":" + action;
    }

    // Business logic

    public boolean matches(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }

    public boolean matches(Permission other) {
        return this.resource.equals(other.resource) && this.action.equals(other.action);
    }

    public boolean isWildcard() {
        return action.equals("*");
    }

    public boolean isResourceWildcard() {
        return resource.equals("*");
    }

    public boolean implies(Permission other) {
        if (other == null) {
            return false;
        }
        if (this.equals(other)) {
            return true;
        }
        if (this.isWildcard() || this.isResourceWildcard()) {
            return true;
        }
        if (other.isWildcard()) {
            return true;
        }
        return false;
    }

    // Getters
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public String getDescription() { return description; }

    // Comparable implementation for TreeSet
    @Override
    public int compareTo(Permission other) {
        int resourceCompare = this.resource.compareTo(other.resource);
        if (resourceCompare != 0) {
            return resourceCompare;
        }
        return this.action.compareTo(other.action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return resource.equals(that.resource) && action.equals(that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, action);
    }

    @Override
    public String toString() {
        if (description != null) {
            return "%s[%s:%s]".formatted(description, resource, action);
        }
        return "%s:%s".formatted(resource, action);
    }
}
