package com.droid.bss.infrastructure.auth.jwt;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents user principal information extracted from a JWT token.
 *
 * @since 1.0
 */
public class UserPrincipal implements Principal {

    private final String userId;
    private final String username;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;
    private final String issuer;

    public UserPrincipal(String userId, String username, String email,
                         List<String> roles, List<String> permissions, String issuer) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.email = email;
        this.roles = roles != null ? roles : List.of();
        this.permissions = permissions != null ? permissions : List.of();
        this.issuer = issuer;
    }

    /**
     * Creates a UserPrincipal from standard JWT claims.
     *
     * @param userId the user ID claim
     * @param username the username claim
     * @param email the email claim
     * @param roles the roles claim
     * @param permissions the permissions claim
     * @param issuer the issuer claim
     * @return UserPrincipal instance
     */
    public static UserPrincipal fromClaims(String userId, String username, String email,
                                          List<String> roles, List<String> permissions,
                                          String issuer) {
        return new UserPrincipal(userId, username, email, roles, permissions, issuer);
    }

    /**
     * Creates a UserPrincipal from Keycloak token claims.
     *
     * @param sub the subject claim
     * @param preferredUsername the preferred username claim
     * @param email the email claim
     * @param realmRoles the realm roles
     * @param resourceRoles the resource-specific roles
     * @param issuer the issuer claim
     * @return UserPrincipal instance
     */
    public static UserPrincipal fromKeycloakClaims(String sub, String preferredUsername, String email,
                                                   List<String> realmRoles, List<String> resourceRoles,
                                                   String issuer) {
        List<String> allRoles = new java.util.ArrayList<>();
        if (realmRoles != null) allRoles.addAll(realmRoles);
        if (resourceRoles != null) allRoles.addAll(resourceRoles);

        return new UserPrincipal(sub, preferredUsername, email, allRoles, List.of(), issuer);
    }

    @Override
    public String getName() {
        return username;
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the email address.
     *
     * @return the email address (may be null)
     */
    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    /**
     * Gets the roles.
     *
     * @return list of roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Gets the permissions.
     *
     * @return list of permissions
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Gets the issuer.
     *
     * @return the issuer
     */
    public Optional<String> getIssuer() {
        return Optional.ofNullable(issuer);
    }

    /**
     * Checks if the user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Checks if the user has a specific permission.
     *
     * @param permission the permission to check
     * @return true if user has the permission
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * Checks if the user has any of the specified roles.
     *
     * @param roles the roles to check
     * @return true if user has at least one of the roles
     */
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user has any of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if user has at least one of the permissions
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
               "userId='" + userId + '\'' +
               ", username='" + username + '\'' +
               ", email='" + email + '\'' +
               ", roles=" + roles +
               ", permissions=" + permissions +
               ", issuer='" + issuer + '\'' +
               '}';
    }
}
