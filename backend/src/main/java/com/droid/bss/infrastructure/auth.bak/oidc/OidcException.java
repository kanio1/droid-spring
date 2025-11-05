package com.droid.bss.infrastructure.auth.oidc;

/**
 * Exception thrown when OIDC operations fail.
 *
 * @since 1.0
 */
public class OidcException extends RuntimeException {

    private final String errorCode;
    private final String description;

    /**
     * Constructs a new OidcException with the specified detail message.
     *
     * @param message the detail message
     */
    public OidcException(String message) {
        super(message);
        this.errorCode = null;
        this.description = message;
    }

    /**
     * Constructs a new OidcException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public OidcException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.description = message;
    }

    /**
     * Constructs a new OidcException with error code and description.
     *
     * @param errorCode the error code
     * @param description the error description
     */
    public OidcException(String errorCode, String description) {
        super(description);
        this.errorCode = errorCode;
        this.description = description;
    }

    /**
     * Constructs a new OidcException with error code, description, and cause.
     *
     * @param errorCode the error code
     * @param description the error description
     * @param cause the cause
     */
    public OidcException(String errorCode, String description, Throwable cause) {
        super(description, cause);
        this.errorCode = errorCode;
        this.description = description;
    }

    /**
     * Creates an exception for invalid configuration.
     *
     * @param config the configuration parameter
     * @return OidcException instance
     */
    public static OidcException invalidConfiguration(String config) {
        return new OidcException("Invalid Configuration",
            "Invalid OIDC configuration: " + config);
    }

    /**
     * Creates an exception for network errors.
     *
     * @param url the URL that failed
     * @param cause the underlying cause
     * @return OidcException instance
     */
    public static OidcException networkError(String url, Throwable cause) {
        return new OidcException("Network Error",
            "Failed to connect to OIDC provider: " + url,
            cause);
    }

    /**
     * Creates an exception for authentication failures.
     *
     * @param reason the reason for failure
     * @return OidcException instance
     */
    public static OidcException authenticationFailed(String reason) {
        return new OidcException("Authentication Failed",
            "OIDC authentication failed: " + reason);
    }

    /**
     * Creates an exception for invalid authorization code.
     *
     * @param code the authorization code
     * @return OidcException instance
     */
    public static OidcException invalidAuthorizationCode(String code) {
        return new OidcException("Invalid Request",
            "Invalid or expired authorization code: " + code);
    }

    /**
     * Creates an exception for invalid client credentials.
     *
     * @param clientId the client ID
     * @return OidcException instance
     */
    public static OidcException invalidClientCredentials(String clientId) {
        return new OidcException("Invalid Client",
            "Invalid client credentials for client: " + clientId);
    }

    /**
     * Creates an exception for unsupported grant type.
     *
     * @param grantType the grant type
     * @return OidcException instance
     */
    public static OidcException unsupportedGrantType(String grantType) {
        return new OidcException("Unsupported Grant Type",
            "Unsupported grant type: " + grantType);
    }

    /**
     * Creates an exception for invalid scope.
     *
     * @param scope the invalid scope
     * @return OidcException instance
     */
    public static OidcException invalidScope(String scope) {
        return new OidcException("Invalid Scope",
            "Invalid or unauthorized scope: " + scope);
    }

    /**
     * Creates an exception for insufficient scope.
     *
     * @param required the required scope
     * @param actual the actual scope
     * @return OidcException instance
     */
    public static OidcException insufficientScope(String required, String actual) {
        return new OidcException("Insufficient Scope",
            "Insufficient scope. Required: " + required + ", Actual: " + actual);
    }

    /**
     * Creates an exception for invalid redirect URI.
     *
     * @param redirectUri the redirect URI
     * @return OidcException instance
     */
    public static OidcException invalidRedirectUri(String redirectUri) {
        return new OidcException("Invalid Redirect URI",
            "Redirect URI not registered: " + redirectUri);
    }

    /**
     * Creates an exception for state mismatch.
     *
     * @param expected the expected state
     * @param actual the actual state
     * @return OidcException instance
     */
    public static OidcException stateMismatch(String expected, String actual) {
        return new OidcException("State Mismatch",
            "State parameter mismatch. Expected: " + expected + ", Actual: " + actual);
    }

    /**
     * Creates an exception for token expiration.
     *
     * @param tokenId the token ID
     * @param expiration the expiration time
     * @return OidcException instance
     */
    public static OidcException tokenExpired(String tokenId, long expiration) {
        return new OidcException("Token Expired",
            "Token expired at: " + expiration + ", ID: " + tokenId);
    }

    /**
     * Creates an exception for invalid token.
     *
     * @param tokenId the token ID (may be null)
     * @param reason the reason for invalidity
     * @return OidcException instance
     */
    public static OidcException invalidToken(String tokenId, String reason) {
        return new OidcException("Invalid Token",
            "Token is invalid: " + reason + (tokenId != null ? ", ID: " + tokenId : ""));
    }

    /**
     * Creates an exception for user info retrieval failure.
     *
     * @param subject the user subject
     * @param cause the underlying cause
     * @return OidcException instance
     */
    public static OidcException userInfoFailed(String subject, Throwable cause) {
        return new OidcException("User Info Failed",
            "Failed to retrieve user info for subject: " + subject,
            cause);
    }

    /**
     * Creates an exception for introspection failures.
     *
     * @param tokenId the token ID
     * @param cause the underlying cause
     * @return OidcException instance
     */
    public static OidcException introspectionFailed(String tokenId, Throwable cause) {
        return new OidcException("Introspection Failed",
            "Failed to introspect token: " + tokenId,
            cause);
    }

    /**
     * Gets the error code.
     *
     * @return the error code (may be null)
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
