package com.droid.bss.infrastructure.auth.jwt;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of a JWT token refresh operation.
 *
 * @since 1.0
 */
public class JwtRefreshResult {

    private final boolean success;
    private final String errorMessage;
    private final String newAccessToken;
    private final String refreshToken;
    private final Instant expirationTime;
    private final Instant issuedAt;

    private JwtRefreshResult(boolean success, String errorMessage,
                            String newAccessToken, String refreshToken,
                            Instant expirationTime, Instant issuedAt) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.newAccessToken = newAccessToken;
        this.refreshToken = refreshToken;
        this.expirationTime = expirationTime;
        this.issuedAt = issuedAt;
    }

    /**
     * Creates a successful refresh result.
     *
     * @param newAccessToken the new access token
     * @param refreshToken the new refresh token (may be null if not rotated)
     * @param expirationTime the expiration timestamp
     * @param issuedAt the issued at timestamp
     * @return JwtRefreshResult indicating success
     */
    public static JwtRefreshResult success(String newAccessToken, String refreshToken,
                                          Instant expirationTime, Instant issuedAt) {
        return new JwtRefreshResult(true, null, newAccessToken, refreshToken, expirationTime, issuedAt);
    }

    /**
     * Creates a failed refresh result.
     *
     * @param errorMessage the error message
     * @return JwtRefreshResult indicating failure
     */
    public static JwtRefreshResult failure(String errorMessage) {
        return new JwtRefreshResult(false, errorMessage, null, null, null, null);
    }

    /**
     * Creates a failed refresh result with exception.
     *
     * @param exception the exception
     * @return JwtRefreshResult indicating failure
     */
    public static JwtRefreshResult failure(Exception exception) {
        return new JwtRefreshResult(false, exception.getMessage(), null, null, null, null);
    }

    /**
     * Checks if refresh was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if refresh failed.
     *
     * @return true if failed
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Gets the error message.
     *
     * @return the error message (may be null if successful)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the new access token.
     *
     * @return the access token (may be null if failed)
     */
    public String getNewAccessToken() {
        return newAccessToken;
    }

    /**
     * Gets the refresh token.
     *
     * @return the refresh token (may be null if not provided or failed)
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Gets the expiration timestamp.
     *
     * @return the expiration timestamp (may be null if failed)
     */
    public Instant getExpirationTime() {
        return expirationTime;
    }

    /**
     * Gets the issued at timestamp.
     *
     * @return the issued at timestamp (may be null if failed)
     */
    public Instant getIssuedAt() {
        return issuedAt;
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
     * Checks if the refresh token was rotated.
     *
     * @return true if refresh token was updated
     */
    public boolean isRefreshTokenRotated() {
        return refreshToken != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtRefreshResult that = (JwtRefreshResult) o;
        return success == that.success &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(newAccessToken, that.newAccessToken) &&
               Objects.equals(refreshToken, that.refreshToken) &&
               Objects.equals(expirationTime, that.expirationTime) &&
               Objects.equals(issuedAt, that.issuedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, errorMessage, newAccessToken, refreshToken, expirationTime, issuedAt);
    }

    @Override
    public String toString() {
        return "JwtRefreshResult{" +
               "success=" + success +
               ", errorMessage='" + errorMessage + '\'' +
               ", newAccessToken='" + newAccessToken + '\'' +
               ", refreshToken='" + refreshToken + '\'' +
               ", expirationTime=" + expirationTime +
               ", issuedAt=" + issuedAt +
               '}';
    }
}
