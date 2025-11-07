package com.droid.bss.infrastructure.auth.jwt;

/**
 * Stub interface for JWT validation
 * Minimal implementation for testing purposes
 */
public interface JwtValidator {

    /**
     * Validate a JWT token
     */
    boolean validate(String token);

    /**
     * Extract claims from token
     */
    JwtClaims getClaims(String token);

    /**
     * Get the principal from token
     */
    Object getPrincipal(String token);
}

/**
 * Stub class for JWT claims
 * Minimal implementation for testing purposes
 */
class JwtClaims {

    private String subject;
    private String issuer;
    private long expiration;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
