package com.droid.bss.infrastructure.auth.jwt;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Port (Interface) for JWT Token Validation
 *
 * This interface defines the contract for JWT token validation operations.
 * Implementations provide different strategies for validating JWT tokens
 * from various identity providers (Keycloak, Auth0, etc.).
 *
 * @since 1.0
 */
public interface JwtValidator {

    /**
     * Validates a JWT token and returns the claims if valid.
     *
     * @param token the JWT token to validate
     * @return JwtValidationResult containing claims and validation status
     * @throws JwtValidationException if token is invalid or expired
     */
    JwtValidationResult validateToken(String token);

    /**
     * Checks if a token is valid without throwing exceptions.
     *
     * @param token the JWT token to check
     * @return true if token is valid, false otherwise
     */
    boolean isTokenValid(String token);

    /**
     * Extracts user principal information from a valid token.
     *
     * @param token the JWT token
     * @return UserPrincipal containing user information
     * @throws JwtValidationException if token is invalid
     */
    UserPrincipal extractUserPrincipal(String token);

    /**
     * Gets the expiration time of a token.
     *
     * @param token the JWT token
     * @return Optional containing expiration timestamp in milliseconds
     */
    Optional<Long> getExpirationTime(String token);

    /**
     * Gets the roles from a token.
     *
     * @param token the JWT token
     * @return List of role names
     * @throws JwtValidationException if token is invalid
     */
    List<String> getRoles(String token);

    /**
     * Gets the permissions/scopes from a token.
     *
     * @param token the JWT token
     * @return List of permission/scope names
     * @throws JwtValidationException if token is invalid
     */
    List<String> getPermissions(String token);

    /**
     * Checks if a token has a specific role.
     *
     * @param token the JWT token
     * @param role the role to check
     * @return true if token contains the role
     * @throws JwtValidationException if token is invalid
     */
    boolean hasRole(String token, String role);

    /**
     * Refreshes an expired token using a refresh token.
     *
     * @param refreshToken the refresh token
     * @return JwtRefreshResult containing new access token
     * @throws JwtValidationException if refresh token is invalid
     */
    JwtRefreshResult refreshToken(String refreshToken);
}
