package com.droid.bss.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security Service
 * Provides RBAC, authentication, and authorization services
 */
@Service
public class SecurityService {

    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);

    // Simulated user store (in production, use proper user repository)
    private final Map<String, UserIdentity> users = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userRoles = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userPermissions = new ConcurrentHashMap<>();
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    public SecurityService() {
        // Initialize with default admin user
        UserIdentity adminUser = new UserIdentity(
            "admin-1",
            "admin",
            "admin@company.com",
            Set.of("ADMIN"),
            Set.of("read", "write", "delete", "admin")
        );
        users.put("admin-1", adminUser);
        userRoles.put("admin-1", Set.of("ADMIN"));
        userPermissions.put("admin-1", Set.of("read", "write", "delete", "admin"));
    }

    /**
     * Get current user
     */
    public Optional<UserIdentity> getCurrentUser() {
        // In production, get from security context
        return Optional.ofNullable(users.get("admin-1"));
    }

    /**
     * Check if user has permission
     */
    public boolean hasPermission(String permission) {
        return getCurrentUser()
            .map(user -> userPermissions.getOrDefault(user.id(), Set.of()).contains(permission))
            .orElse(false);
    }

    /**
     * Check if user has any of the permissions
     */
    public boolean hasAnyPermission(String[] permissions) {
        Set<String> userPerms = getCurrentUser()
            .map(user -> userPermissions.getOrDefault(user.id(), Set.of()))
            .orElse(Set.of());

        return Arrays.stream(permissions)
            .anyMatch(userPerms::contains);
    }

    /**
     * Check if user has all permissions
     */
    public boolean hasAllPermissions(String[] permissions) {
        Set<String> userPerms = getCurrentUser()
            .map(user -> userPermissions.getOrDefault(user.id(), Set.of()))
            .orElse(Set.of());

        return Arrays.stream(permissions)
            .allMatch(userPerms::contains);
    }

    /**
     * Check if user has role
     */
    public boolean hasRole(String role) {
        return getCurrentUser()
            .map(user -> userRoles.getOrDefault(user.id(), Set.of()).contains(role))
            .orElse(false);
    }

    /**
     * Check if user has any of the roles
     */
    public boolean hasAnyRole(String[] roles) {
        Set<String> userRoleSet = getCurrentUser()
            .map(user -> userRoles.getOrDefault(user.id(), Set.of()))
            .orElse(Set.of());

        return Arrays.stream(roles)
            .anyMatch(userRoleSet::contains);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if user owns resource
     */
    public boolean isOwner(String resourceId, String resourceType) {
        UserIdentity user = getCurrentUser().orElse(null);
        if (user == null) {
            return false;
        }

        // Simple ownership check - user owns their own resources
        return resourceId.equals(user.id()) ||
               resourceId.equals(user.username());
    }

    /**
     * Create user
     */
    public void createUser(String id, String username, String email, Set<String> roles, Set<String> permissions) {
        UserIdentity user = new UserIdentity(id, username, email, roles, permissions);
        users.put(id, user);
        userRoles.put(id, roles);
        userPermissions.put(id, permissions);

        log.info("Created user: {} with roles: {}", username, roles);
    }

    /**
     * Update user roles
     */
    public void updateUserRoles(String userId, Set<String> roles) {
        userRoles.put(userId, roles);
        UserIdentity user = users.get(userId);
        if (user != null) {
            users.put(userId, new UserIdentity(
                user.id(),
                user.username(),
                user.email(),
                roles,
                userPermissions.getOrDefault(userId, Set.of())
            ));
        }
        log.info("Updated roles for user: {} to {}", userId, roles);
    }

    /**
     * Add session
     */
    public void addSession(String sessionId, UserSession session) {
        activeSessions.put(sessionId, session);
    }

    /**
     * Remove session
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    /**
     * Get session
     */
    public Optional<UserSession> getSession(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry ->
            entry.getValue().expiresAt() < now
        );
    }

    /**
     * User identity record
     */
    public record UserIdentity(
        String id,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions
    ) {}

    /**
     * User session record
     */
    public record UserSession(
        String userId,
        String sessionToken,
        long createdAt,
        long expiresAt
    ) {}
}
