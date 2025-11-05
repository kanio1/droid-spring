package com.droid.bss.infrastructure.auth.oidc;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the response from token introspection endpoint.
 *
 * @since 1.0
 */
public class TokenIntrospectionResponse {

    private final boolean active;
    private final String subject;
    private final String issuer;
    private final String audience;
    private final String clientId;
    private final Instant issuedAt;
    private final Instant expiration;
    private final Instant notBefore;
    private final String scope;
    private final String tokenType;
    private final String userName;
    private final Map<String, Object> claims;

    private TokenIntrospectionResponse(boolean active, String subject, String issuer,
                                       String audience, String clientId,
                                       Instant issuedAt, Instant expiration,
                                       Instant notBefore, String scope,
                                       String tokenType, String userName,
                                       Map<String, Object> claims) {
        this.active = active;
        this.subject = subject;
        this.issuer = issuer;
        this.audience = audience;
        this.clientId = clientId;
        this.issuedAt = issuedAt;
        this.expiration = expiration;
        this.notBefore = notBefore;
        this.scope = scope;
        this.tokenType = tokenType;
        this.userName = userName;
        this.claims = claims != null ? claims : Map.of();
    }

    /**
     * Creates an active token introspection response.
     *
     * @param subject the subject
     * @param issuer the issuer
     * @param audience the audience
     * @param clientId the client ID
     * @param issuedAt the issued at time
     * @param expiration the expiration time
     * @param notBefore the not before time (may be null)
     * @param scope the scope (may be null)
     * @param tokenType the token type (may be null)
     * @param userName the username (may be null)
     * @param claims additional claims (may be null)
     * @return TokenIntrospectionResponse for active token
     */
    public static TokenIntrospectionResponse active(String subject, String issuer, String audience,
                                                   String clientId, Instant issuedAt, Instant expiration,
                                                   Instant notBefore, String scope,
                                                   String tokenType, String userName,
                                                   Map<String, Object> claims) {
        return new TokenIntrospectionResponse(true, subject, issuer, audience, clientId,
                                             issuedAt, expiration, notBefore, scope,
                                             tokenType, userName, claims);
    }

    /**
     * Creates an inactive token introspection response.
     *
     * @return TokenIntrospectionResponse for inactive token
     */
    public static TokenIntrospectionResponse inactive() {
        return new TokenIntrospectionResponse(false, null, null, null, null,
                                            null, null, null, null, null, null, null);
    }

    /**
     * Checks if the token is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Checks if the token is inactive.
     *
     * @return true if inactive
     */
    public boolean isInactive() {
        return !active;
    }

    /**
     * Gets the subject.
     *
     * @return the subject (may be null if inactive)
     */
    public Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Gets the issuer.
     *
     * @return the issuer (may be null if inactive)
     */
    public Optional<String> getIssuer() {
        return Optional.ofNullable(issuer);
    }

    /**
     * Gets the audience.
     *
     * @return the audience (may be null if inactive)
     */
    public Optional<String> getAudience() {
        return Optional.ofNullable(audience);
    }

    /**
     * Gets the client ID.
     *
     * @return the client ID (may be null if inactive)
     */
    public Optional<String> getClientId() {
        return Optional.ofNullable(clientId);
    }

    /**
     * Gets the issued at time.
     *
     * @return the issued at time (may be null if inactive)
     */
    public Optional<Instant> getIssuedAt() {
        return Optional.ofNullable(issuedAt);
    }

    /**
     * Gets the expiration time.
     *
     * @return the expiration time (may be null if inactive)
     */
    public Optional<Instant> getExpiration() {
        return Optional.ofNullable(expiration);
    }

    /**
     * Gets the not before time.
     *
     * @return the not before time (may be null if inactive)
     */
    public Optional<Instant> getNotBefore() {
        return Optional.ofNullable(notBefore);
    }

    /**
     * Gets the scope.
     *
     * @return the scope (may be null if inactive)
     */
    public Optional<String> getScope() {
        return Optional.ofNullable(scope);
    }

    /**
     * Gets the token type.
     *
     * @return the token type (may be null if inactive)
     */
    public Optional<String> getTokenType() {
        return Optional.ofNullable(tokenType);
    }

    /**
     * Gets the username.
     *
     * @return the username (may be null if inactive)
     */
    public Optional<String> getUserName() {
        return Optional.ofNullable(userName);
    }

    /**
     * Gets all claims.
     *
     * @return the claims map
     */
    public Map<String, Object> getClaims() {
        return claims;
    }

    /**
     * Gets a specific claim value.
     *
     * @param name the claim name
     * @return the claim value
     */
    public Optional<Object> getClaim(String name) {
        return Optional.ofNullable(claims.get(name));
    }

    /**
     * Checks if token is expired.
     *
     * @return true if expired or inactive
     */
    public boolean isExpired() {
        return !active || (expiration != null && Instant.now().isAfter(expiration));
    }

    /**
     * Checks if token is not yet valid.
     *
     * @return true if not yet valid or inactive
     */
    public boolean isNotYetValid() {
        return !active || (notBefore != null && Instant.now().isBefore(notBefore));
    }

    /**
     * Gets remaining time to expiration in seconds.
     *
     * @return the remaining time in seconds (0 if expired or inactive)
     */
    public long getRemainingTimeInSeconds() {
        if (!active || expiration == null) {
            return 0;
        }
        long remaining = expiration.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenIntrospectionResponse that = (TokenIntrospectionResponse) o;
        return active == that.active &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(issuer, that.issuer) &&
               Objects.equals(audience, that.audience) &&
               Objects.equals(clientId, that.clientId) &&
               Objects.equals(issuedAt, that.issuedAt) &&
               Objects.equals(expiration, that.expiration) &&
               Objects.equals(notBefore, that.notBefore) &&
               Objects.equals(scope, that.scope) &&
               Objects.equals(tokenType, that.tokenType) &&
               Objects.equals(userName, that.userName) &&
               Objects.equals(claims, that.claims);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, subject, issuer, audience, clientId, issuedAt,
                          expiration, notBefore, scope, tokenType, userName, claims);
    }

    @Override
    public String toString() {
        return "TokenIntrospectionResponse{" +
               "active=" + active +
               ", subject='" + subject + '\'' +
               ", issuer='" + issuer + '\'' +
               ", audience='" + audience + '\'' +
               ", clientId='" + clientId + '\'' +
               ", issuedAt=" + issuedAt +
               ", expiration=" + expiration +
               ", notBefore=" + notBefore +
               ", scope='" + scope + '\'' +
               ", tokenType='" + tokenType + '\'' +
               ", userName='" + userName + '\'' +
               '}';
    }
}
