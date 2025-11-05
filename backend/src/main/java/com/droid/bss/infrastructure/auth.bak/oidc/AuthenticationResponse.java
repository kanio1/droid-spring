package com.droid.bss.infrastructure.auth.oidc;

import com.droid.bss.infrastructure.auth.jwt.UserPrincipal;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the response from an OIDC authentication flow.
 *
 * @since 1.0
 */
public class AuthenticationResponse {

    private final boolean success;
    private final String accessToken;
    private final String refreshToken;
    private final String idToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserPrincipal userPrincipal;
    private final String error;
    private final String state;

    private AuthenticationResponse(boolean success, String accessToken, String refreshToken,
                                  String idToken, String tokenType, long expiresIn,
                                  UserPrincipal userPrincipal, String error, String state) {
        this.success = success;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idToken = idToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userPrincipal = userPrincipal;
        this.error = error;
        this.state = state;
    }

    /**
     * Creates a successful authentication response.
     *
     * @param accessToken the access token
     * @param refreshToken the refresh token (may be null)
     * @param idToken the ID token (may be null)
     * @param tokenType the token type (e.g., "Bearer")
     * @param expiresIn the expiration time in seconds
     * @param userPrincipal the user principal
     * @param state the state parameter (may be null)
     * @return AuthenticationResponse indicating success
     */
    public static AuthenticationResponse success(String accessToken, String refreshToken,
                                                String idToken, String tokenType, long expiresIn,
                                                UserPrincipal userPrincipal, String state) {
        return new AuthenticationResponse(true, accessToken, refreshToken, idToken,
                                        tokenType, expiresIn, userPrincipal, null, state);
    }

    /**
     * Creates a failed authentication response.
     *
     * @param error the error message
     * @param state the state parameter (may be null)
     * @return AuthenticationResponse indicating failure
     */
    public static AuthenticationResponse failure(String error, String state) {
        return new AuthenticationResponse(false, null, null, null, null, null, error, state);
    }

    /**
     * Creates a failed authentication response with all nulls.
     *
     * @param error the error message
     * @return AuthenticationResponse indicating failure
     */
    public static AuthenticationResponse failure(String error) {
        return failure(error, null);
    }

    /**
     * Checks if authentication was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if authentication failed.
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
     * Gets the user principal.
     *
     * @return the user principal (may be null if failed)
     */
    public UserPrincipal getUserPrincipal() {
        return userPrincipal;
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
     * Gets the state parameter.
     *
     * @return the state parameter (may be null)
     */
    public String getState() {
        return state;
    }

    /**
     * Calculates the expiration timestamp.
     *
     * @return the expiration timestamp
     */
    public Instant getExpiresAt() {
        return Instant.now().plusSeconds(expiresIn);
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
     * Checks if a refresh token is available.
     *
     * @return true if refresh token is present
     */
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.isBlank();
    }

    /**
     * Checks if an ID token is available.
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
        AuthenticationResponse that = (AuthenticationResponse) o;
        return success == that.success &&
               expiresIn == that.expiresIn &&
               Objects.equals(accessToken, that.accessToken) &&
               Objects.equals(refreshToken, that.refreshToken) &&
               Objects.equals(idToken, that.idToken) &&
               Objects.equals(tokenType, that.tokenType) &&
               Objects.equals(userPrincipal, that.userPrincipal) &&
               Objects.equals(error, that.error) &&
               Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, accessToken, refreshToken, idToken, tokenType,
                          expiresIn, userPrincipal, error, state);
    }

    @Override
    public String toString() {
        return "AuthenticationResponse{" +
               "success=" + success +
               ", accessToken='" + (accessToken != null ? "[PROTECTED]" : null) + '\'' +
               ", refreshToken='" + (refreshToken != null ? "[PROTECTED]" : null) + '\'' +
               ", idToken='" + (idToken != null ? "[PROTECTED]" : null) + '\'' +
               ", tokenType='" + tokenType + '\'' +
               ", expiresIn=" + expiresIn +
               ", userPrincipal=" + userPrincipal +
               ", error='" + error + '\'' +
               ", state='" + state + '\'' +
               '}';
    }
}
