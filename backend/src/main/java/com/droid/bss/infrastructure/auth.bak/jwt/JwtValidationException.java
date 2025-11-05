package com.droid.bss.infrastructure.auth.jwt;

/**
 * Exception thrown when JWT token validation fails.
 *
 * @since 1.0
 */
public class JwtValidationException extends RuntimeException {

    private final String tokenId;
    private final String reason;

    /**
     * Constructs a new JwtValidationException with the specified detail message.
     *
     * @param message the detail message
     */
    public JwtValidationException(String message) {
        super(message);
        this.tokenId = null;
        this.reason = message;
    }

    /**
     * Constructs a new JwtValidationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
        this.tokenId = null;
        this.reason = message;
    }

    /**
     * Constructs a new JwtValidationException with the specified reason and token ID.
     *
     * @param reason the reason for validation failure
     * @param tokenId the JWT token ID (jti claim)
     */
    public JwtValidationException(String reason, String tokenId) {
        super(reason);
        this.reason = reason;
        this.tokenId = tokenId;
    }

    /**
     * Constructs a new JwtValidationException with the specified reason, token ID, and cause.
     *
     * @param reason the reason for validation failure
     * @param tokenId the JWT token ID (jti claim)
     * @param cause the cause
     */
    public JwtValidationException(String reason, String tokenId, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
        this.tokenId = tokenId;
    }

    /**
     * Creates an exception for expired token.
     *
     * @param expirationTime the expiration timestamp
     * @param tokenId the token ID
     * @return JwtValidationException instance
     */
    public static JwtValidationException expiredToken(long expirationTime, String tokenId) {
        return new JwtValidationException(
            "Token expired at: " + expirationTime,
            tokenId
        );
    }

    /**
     * Creates an exception for invalid signature.
     *
     * @param tokenId the token ID
     * @return JwtValidationException instance
     */
    public static JwtValidationException invalidSignature(String tokenId) {
        return new JwtValidationException(
            "Token has invalid signature",
            tokenId
        );
    }

    /**
     * Creates an exception for malformed token.
     *
     * @param reason the reason
     * @return JwtValidationException instance
     */
    public static JwtValidationException malformedToken(String reason) {
        return new JwtValidationException("Token is malformed: " + reason);
    }

    /**
     * Creates an exception for unsupported token.
     *
     * @param reason the reason
     * @return JwtValidationException instance
     */
    public static JwtValidationException unsupportedToken(String reason) {
        return new JwtValidationException("Token is unsupported: " + reason);
    }

    /**
     * Creates an exception for invalid issuer.
     *
     * @param expectedIssuer the expected issuer
     * @param actualIssuer the actual issuer
     * @return JwtValidationException instance
     */
    public static JwtValidationException invalidIssuer(String expectedIssuer, String actualIssuer) {
        return new JwtValidationException(
            String.format("Invalid issuer. Expected: %s, Actual: %s", expectedIssuer, actualIssuer)
        );
    }

    /**
     * Creates an exception for invalid audience.
     *
     * @param expectedAudience the expected audience
     * @param actualAudience the actual audience
     * @return JwtValidationException instance
     */
    public static JwtValidationException invalidAudience(String expectedAudience, String actualAudience) {
        return new JwtValidationException(
            String.format("Invalid audience. Expected: %s, Actual: %s", expectedAudience, actualAudience)
        );
    }

    /**
     * Creates an exception for token not yet valid.
     *
     * @param notBefore the not before timestamp
     * @return JwtValidationException instance
     */
    public static JwtValidationException tokenNotYetValid(long notBefore) {
        return new JwtValidationException(
            "Token is not valid before: " + notBefore
        );
    }

    /**
     * Creates an exception for missing required claim.
     *
     * @param claimName the claim name
     * @return JwtValidationException instance
     */
    public static JwtValidationException missingClaim(String claimName) {
        return new JwtValidationException(
            "Token is missing required claim: " + claimName
        );
    }

    /**
     * Creates an exception for invalid claim value.
     *
     * @param claimName the claim name
     * @param claimValue the claim value
     * @return JwtValidationException instance
     */
    public static JwtValidationException invalidClaimValue(String claimName, String claimValue) {
        return new JwtValidationException(
            String.format("Token has invalid claim value. Claim: %s, Value: %s", claimName, claimValue)
        );
    }

    /**
     * Gets the token ID.
     *
     * @return the token ID (may be null)
     */
    public String getTokenId() {
        return tokenId;
    }

    /**
     * Gets the reason for validation failure.
     *
     * @return the reason
     */
    public String getReason() {
        return reason;
    }
}
