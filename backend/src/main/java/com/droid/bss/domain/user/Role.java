package com.droid.bss.domain.user;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Role entity - represents a user role with associated permissions.
 * Implements Comparable for natural ordering in TreeSet.
 */
public class Role implements Comparable<Role> {

    private final String name;
    private final String displayName;
    private final String description;
    private final int level;
    private final boolean systemRole;
    private final Set<Permission> permissions;

    public Role(
        String name,
        String displayName,
        String description,
        int level,
        boolean systemRole,
        Set<Permission> permissions
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Role display name cannot be null or blank");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Role level cannot be negative");
        }

        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.level = level;
        this.systemRole = systemRole;
        this.permissions = permissions != null ? new TreeSet<>(permissions) : new TreeSet<>();
    }

    /**
     * Creates a system role (cannot be deleted).
     */
    public static Role systemRole(String name, String displayName, String description, int level, Set<Permission> permissions) {
        return new Role(name, displayName, description, level, true, permissions);
    }

    /**
     * Creates a custom role (can be deleted).
     */
    public static Role customRole(String name, String displayName, String description, int level, Set<Permission> permissions) {
        return new Role(name, displayName, description, level, false, permissions);
    }

    /**
     * Creates a role from Keycloak.
     */
    public static Role fromKeycloak(String name, String description) {
        return new Role(
            name,
            name.replace("_", " "),
            description,
            getDefaultLevel(name),
            isSystemRole(name),
            new TreeSet<>()
        );
    }

    /**
     * Creates a role from Keycloak with default description.
     */
    public static Role fromKeycloak(String name) {
        return fromKeycloak(name, "Role from Keycloak: " + name);
    }

    private static int getDefaultLevel(String name) {
        return switch (name) {
            case "SUPER_ADMIN" -> 1;
            case "ADMIN" -> 2;
            case "MANAGER" -> 3;
            case "OPERATOR" -> 4;
            case "ANALYST" -> 5;
            case "VIEWER" -> 6;
            case "bss-user" -> 7;
            default -> 10; // Custom roles
        };
    }

    private static boolean isSystemRole(String name) {
        return name.equals("SUPER_ADMIN") ||
               name.equals("ADMIN") ||
               name.equals("MANAGER") ||
               name.equals("OPERATOR") ||
               name.equals("ANALYST") ||
               name.equals("VIEWER") ||
               name.equals("bss-user");
    }

    /**
     * Adds a permission to the role.
     */
    public Role addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        if (this.permissions.contains(permission)) {
            return this; // Permission already exists
        }
        Set<Permission> newPermissions = new TreeSet<>(this.permissions);
        newPermissions.add(permission);
        return new Role(name, displayName, description, level, systemRole, newPermissions);
    }

    /**
     * Removes a permission from the role.
     */
    public Role removePermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        if (!this.permissions.contains(permission)) {
            return this; // Permission not found
        }
        Set<Permission> newPermissions = new TreeSet<>(this.permissions);
        newPermissions.remove(permission);
        return new Role(name, displayName, description, level, systemRole, newPermissions);
    }

    /**
     * Sets all permissions for the role.
     */
    public Role setPermissions(Set<Permission> newPermissions) {
        if (newPermissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }
        return new Role(name, displayName, description, level, systemRole, newPermissions);
    }

    /**
     * Updates role information.
     */
    public Role updateInfo(String newDisplayName, String newDescription) {
        if (newDisplayName == null || newDisplayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be null or blank");
        }
        return new Role(
            name,
            newDisplayName,
            newDescription,
            level,
            systemRole,
            permissions
        );
    }

    // Business logic

    public boolean hasPermission(String resource, String action) {
        return permissions.stream()
            .anyMatch(p -> p.getResource().equals(resource) && p.getAction().equals(action));
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasAnyPermission(Set<String> resourceActionPairs) {
        return permissions.stream()
            .anyMatch(p -> resourceActionPairs.contains(p.getResource() + ":" + p.getAction()));
    }

    public boolean hasAllPermissions(Set<String> resourceActionPairs) {
        return resourceActionPairs.stream()
            .allMatch(pair -> {
                String[] parts = pair.split(":");
                return parts.length == 2 && hasPermission(parts[0], parts[1]);
            });
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public boolean isHigherLevelThan(Role other) {
        return this.level < other.level;
    }

    public boolean isLowerLevelThan(Role other) {
        return this.level > other.level;
    }

    public boolean canManage(Role other) {
        return this.level < other.level && !other.systemRole;
    }

    // Getters
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getLevel() { return level; }
    public boolean isSystemRoleFlag() { return systemRole; }
    public Set<Permission> getPermissions() { return new TreeSet<>(permissions); }

    // Comparable implementation for TreeSet
    @Override
    public int compareTo(Role other) {
        int levelCompare = Integer.compare(this.level, other.level);
        if (levelCompare != 0) {
            return levelCompare;
        }
        return this.name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Role{name='%s', level=%d, system=%s}".formatted(name, level, systemRole);
    }
}
