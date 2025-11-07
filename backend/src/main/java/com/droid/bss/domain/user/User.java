package com.droid.bss.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * User aggregate root - represents a system user.
 */
public class User {

    private final UserId id;
    private final UserInfo userInfo;
    private UserStatus status;
    private final Set<Role> roles;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods (create, updateInfo, etc.) for domain operations.
     */
    User(
        UserId id,
        UserInfo userInfo,
        UserStatus status,
        Set<Role> roles,
        LocalDateTime createdAt,
        int version
    ) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.userInfo = Objects.requireNonNull(userInfo, "User info cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.roles = Objects.requireNonNull(roles, "Roles cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = this.createdAt;
        this.version = version;
    }

    /**
     * Creates a new user with the provided information.
     */
    public static User create(UserInfo userInfo) {
        return new User(
            UserId.generate(),
            userInfo,
            UserStatus.PENDING_VERIFICATION,
            new TreeSet<>(), // Empty role set initially
            LocalDateTime.now(),
            1
        );
    }

    /**
     * Creates a user with existing ID (for repository operations).
     */
    public static User withId(UserId id, UserInfo userInfo, Set<Role> roles) {
        return new User(
            id,
            userInfo,
            UserStatus.ACTIVE,
            new TreeSet<>(roles),
            LocalDateTime.now(),
            1
        );
    }

    /**
     * Updates the user information.
     */
    public User updateInfo(UserInfo newUserInfo) {
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated user");
        }
        return new User(
            this.id,
            newUserInfo,
            this.status,
            this.roles,
            this.createdAt,
            this.version + 1
        );
    }

    /**
     * Changes the user status.
     */
    public User changeStatus(UserStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                "Cannot change status from %s to %s".formatted(this.status, newStatus)
            );
        }
        return new User(
            this.id,
            this.userInfo,
            newStatus,
            this.roles,
            this.createdAt,
            this.version + 1
        );
    }

    /**
     * Assigns a role to the user.
     */
    public User assignRole(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated user");
        }
        if (this.roles.contains(role)) {
            return this; // Role already assigned
        }
        Set<Role> newRoles = new TreeSet<>(this.roles);
        newRoles.add(role);
        return new User(
            this.id,
            this.userInfo,
            this.status,
            newRoles,
            this.createdAt,
            this.version + 1
        );
    }

    /**
     * Removes a role from the user.
     */
    public User removeRole(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated user");
        }
        if (!this.roles.contains(role)) {
            return this; // Role not assigned
        }
        Set<Role> newRoles = new TreeSet<>(this.roles);
        newRoles.remove(role);
        return new User(
            this.id,
            this.userInfo,
            this.status,
            newRoles,
            this.createdAt,
            this.version + 1
        );
    }

    /**
     * Assigns multiple roles to the user.
     */
    public User assignRoles(Set<Role> roles) {
        Objects.requireNonNull(roles, "Roles cannot be null");
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated user");
        }
        Set<Role> newRoles = new TreeSet<>(roles);
        return new User(
            this.id,
            this.userInfo,
            this.status,
            newRoles,
            this.createdAt,
            this.version + 1
        );
    }

    /**
     * Activates the user account.
     */
    public User activate() {
        return changeStatus(UserStatus.ACTIVE);
    }

    /**
     * Deactivates the user account.
     */
    public User deactivate() {
        return changeStatus(UserStatus.INACTIVE);
    }

    /**
     * Suspends the user account.
     */
    public User suspend() {
        return changeStatus(UserStatus.SUSPENDED);
    }

    /**
     * Terminates the user account.
     */
    public User terminate() {
        return changeStatus(UserStatus.TERMINATED);
    }

    /**
     * Verifies the user email.
     */
    public User verifyEmail() {
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            return changeStatus(UserStatus.ACTIVE);
        }
        return this;
    }

    // Business logic methods

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isInactive() {
        return status == UserStatus.INACTIVE;
    }

    public boolean isSuspended() {
        return status == UserStatus.SUSPENDED;
    }

    public boolean isTerminated() {
        return status == UserStatus.TERMINATED;
    }

    public boolean isPendingVerification() {
        return status == UserStatus.PENDING_VERIFICATION;
    }

    public boolean canBeModified() {
        return status != UserStatus.TERMINATED;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }

    public boolean hasAnyRole(Set<String> roleNames) {
        return roles.stream().anyMatch(role -> roleNames.contains(role.getName()));
    }

    public boolean hasAllRoles(Set<String> roleNames) {
        Set<String> userRoleNames = roles.stream()
            .map(Role::getName)
            .collect(java.util.stream.Collectors.toSet());
        return userRoleNames.containsAll(roleNames);
    }

    // Getters
    public UserId getId() { return id; }
    public UserInfo getUserInfo() { return userInfo; }
    public UserStatus getStatus() { return status; }
    public Set<Role> getRoles() { return new TreeSet<>(roles); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
}
