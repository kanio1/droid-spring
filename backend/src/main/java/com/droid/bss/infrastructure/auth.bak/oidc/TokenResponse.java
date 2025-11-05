package com.droid.bss.infrastructure.auth.oidc;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a token response from OIDC endpoint.
 *
 * @since 1.0
 */
public class TokenResponse {

    private final boolean success;
    private final String accessToken;
    private final String refreshToken;
    private final String idToken;
    private final String tokenType;
    private final long expiresIn;
    private final String scope;
    private final String error;
    private final Instant timestamp;

    private TokenResponse(boolean success, String accessToken, String refreshToken,
                         String idToken, String tokenType, long expiresIn,
                         String scope, String error, Instant timestamp) {
        this.success = success;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.scope = scope;
        this.error = error;
        this.timestamp = timestamp;
    }

    /**
     * Creates a successful token response.
     *
     * @param accessToken the access token
     * @param refreshToken the refresh token (may be null)
     * @param idToken the ID token (may be null)
     * @param tokenType the token type (e.g., "Bearer")
     * @param expiresIn the expiration time in seconds
     * @param scope the granted scopes (may be null)
     * @return TokenResponse indicating success
     */
    public static TokenResponse success(String accessToken, String refreshToken,
                                       String idToken, String tokenType, long expiresIn,
                                       String scope) {
        return new TokenResponse(true, accessToken, refreshToken, idToken,
                               tokenType, expiresIn, scope, null, Instant.now());
    }

    /**
     * Creates a failed token response.
     *
     * @param error the error message
     * @return TokenResponse indicating failure
     */
    public static TokenResponse failure(String error) {
        return new TokenResponse(false, null, null, null, null, 0, null, error, Instant.now());
    }

    /**
     * Creates a failed token response with error and timestamp.
     *
     * @param error the error message
     * @param timestamp the timestamp of the response
     * @return TokenResponse indicating failure
     */
    public static TokenResponse failure(String error, Instant timestamp) {
        return new TokenResponse(false, null, null, null, null, 0, null, error, timestamp);
    }

    /**
     * Checks if response was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if response failed.
     *
     * @return true if failed
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Gets the access token.
     *
     * @return the access token (may be null if failed)
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Gets the refresh token.
     *
     * @return the refresh token (may be null)
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Gets the ID token.
     *
     * @return the ID token (may be null)
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Gets the token type.
     *
     * @return the token type (may be null)
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Gets the expiration time in seconds.
     *
     * @return the expiration time
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Gets the scope.
     *
     * @return the scope (may be null)
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the error message.
     *
     * @return the error message (may be null if successful)
     */
    public String getError() {
        return error;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Calculates the expiration timestamp.
     *
     * @return the expiration timestamp
     */
    public Instant getExpiresAt() {
        return timestamp.plusSeconds(expiresIn);
    }

    /**
     * Checks if token is expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(getExpiresAt());
    }

    /**
     * Checks if refresh token is available.
     *
     * @return true if refresh token is present
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }

    /**
     * Checks if ID token is available.
     *
     * @return true if ID token is present
     */
    public boolean hasIdToken() {
        return idToken != null && !idToken.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenResponse that = (TokenResponse) o;
        return success == that.success &&
               expiresIn == that.expiresIn &&
               Objects.equals(accessToken, that.accessToken) &&
               Objects.equals(refreshToken, that.refreshToken) &&
               Objects.equals(idToken, that.idToken) &&
               Objects.equals(tokenType, that.tokenType) &&
               Objects.equals(scope, that.scope) &&
               Objects.equals(error, that.error) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, accessToken, refreshToken, idToken, tokenType,
                          expiresIn, scope, error, timestamp);
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
               "success=" + success +
               ", accessToken='" + (accessToken != null ? "[PROTECTED]" : null) + '\'' +
               ", refreshToken='" + (refreshToken != null ? "[PROTECTED]" : null) + '\'' +
               ", idToken='" + (idToken != null ? "[PROTECTED]" : null) + '\'' +
               ", tokenType='" + tokenType + '\'' +
               ", expiresIn=" + expiresIn +
               ", scope='" + scope + '\'' +
               ", error='" + error + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
