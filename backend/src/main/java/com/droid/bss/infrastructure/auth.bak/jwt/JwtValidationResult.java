package com.droid.bss.infrastructure.auth.jwt;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of a JWT token validation operation.
 *
 * @since 1.0
 */
public class JwtValidationResult {

    private final boolean valid;
    private final String errorMessage;
    private final UserPrincipal userPrincipal;
    private final Instant issuedAt;
    private final Instant expirationTime;
    private final String tokenId;

    private JwtValidationResult(boolean valid, String errorMessage,
                                UserPrincipal userPrincipal,
                                Instant issuedAt, Instant expirationTime,
                                String tokenId) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.userPrincipal = userPrincipal;
        this.issuedAt = issuedAt;
        this.expirationTime = expirationTime;
        this.tokenId = tokenId;
    }

    /**
     * Creates a successful validation result.
     *
     * @param userPrincipal the user principal
     * @param issuedAt the issued at timestamp
     * @param expirationTime the expiration timestamp
     * @param tokenId the token ID (jti claim)
     * @return JwtValidationResult indicating success
     */
    public static JwtValidationResult success(UserPrincipal userPrincipal,
                                              Instant issuedAt,
                                              Instant expirationTime,
                                              String tokenId) {
        return new JwtValidationResult(true, null, userPrincipal, issuedAt, expirationTime, tokenId);
    }

    /**
     * Creates a failed validation result.
     *
     * @param errorMessage the error message
     * @return JwtValidationResult indicating failure
     */
    public static JwtValidationResult failure(String errorMessage) {
        return new JwtValidationResult(false, errorMessage, null, null, null, null);
    }

    /**
     * Creates a failed validation result with exception message.
     *
     * @param exception the exception
     * @return JwtValidationResult indicating failure
     */
    public static JwtValidationResult failure(Exception exception) {
        return new JwtValidationResult(false,
                                     exception.getMessage(),
                                     null, null, null, null);
    }

    /**
     * Checks if validation was successful.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Checks if validation failed.
     *
     * @return true if invalid
     */
    public boolean isInvalid() {
        return !valid;
    }

    /**
     * Gets the error message.
     *
     * @return the error message (may be null if valid)
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Gets the user principal.
     *
     * @return the user principal (may be null if invalid)
     */
    public Optional<UserPrincipal> getUserPrincipal() {
        return Optional.ofNullable(userPrincipal);
    }

    /**
     * Gets the issued at timestamp.
     *
     * @return the issued at timestamp (may be null if invalid)
     */
    public Optional<Instant> getIssuedAt() {
        return Optional.ofNullable(issuedAt);
    }

    /**
     * Gets the expiration timestamp.
     *
     * @return the expiration timestamp (may be null if invalid)
     */
    public Optional<Instant> getExpirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    /**
     * Gets the token ID (jti claim).
     *
     * @return the token ID (may be null if invalid)
     */
    public Optional<String> getTokenId() {
        return Optional.ofNullable(tokenId);
    }

    /**
     * Checks if the token is expired.
     *
     * @return true if token is expired
     */
    public boolean isExpired() {
        return expirationTime != null && Instant.now().isAfter(expirationTime);
    }

    /**
     * Checks if the token is not yet valid (iat in future).
     *
     * @return true if token is not yet valid
     */
    public boolean isNotYetValid() {
        return issuedAt != null && Instant.now().isBefore(issuedAt);
    }

    /**
     * Gets the remaining time to expiration in seconds.
     *
     * @return the remaining time in seconds (0 if expired or invalid)
     */
    public long getRemainingTimeInSeconds() {
        if (!valid || expirationTime == null) {
            return 0;
        }
        long remaining = expirationTime.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtValidationResult that = (JwtValidationResult) o;
        return valid == that.valid &&
               Objects.equals(userPrincipal, that.userPrincipal) &&
               Objects.equals(issuedAt, that.issuedAt) &&
               Objects.equals(expirationTime, that.expirationTime) &&
               Objects.equals(tokenId, that.tokenId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, userPrincipal, issuedAt, expirationTime, tokenId);
    }

    @Override
    public String toString() {
        return "JwtValidationResult{" +
               "valid=" + valid +
               ", errorMessage='" + errorMessage + '\'' +
               ", userPrincipal=" + userPrincipal +
               ", issuedAt=" + issuedAt +
               ", expirationTime=" + expirationTime +
               ", tokenId='" + tokenId + '\'' +
               '}';
    }
}
