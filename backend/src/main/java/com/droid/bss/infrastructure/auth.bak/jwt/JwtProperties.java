package com.droid.bss.infrastructure.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for JWT validation.
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "app.jwt")
@Validated
public class JwtProperties {

    /**
     * Whether JWT validation is enabled.
     */
    private boolean enabled = true;

    /**
     * Secret key used to sign and verify JWT tokens.
     * Can be a raw secret or a Base64-encoded secret.
     */
    @NotBlank(message = "Secret key cannot be blank")
    private String secretKey;

    /**
     * The issuer of the JWT tokens (iss claim).
     */
    @NotBlank(message = "Issuer cannot be blank")
    private String issuer;

    /**
     * The audience that the JWT tokens are intended for (aud claim).
     */
    private String audience;

    /**
     * The public key for verifying tokens in PEM format.
     * Used for RS256/ES256 algorithms instead of secret key.
     */
    private String publicKey;

    /**
     * The private key for signing tokens in PEM format.
     * Used for RS256/ES256 algorithms.
     */
    private String privateKey;

    /**
     * The algorithm used for JWT signing (HS256, HS512, RS256, ES256, etc.).
     */
    @NotBlank(message = "Algorithm cannot be blank")
    private String algorithm = "HS256";

    /**
     * Default expiration time for tokens if not specified.
     */
    private Duration defaultExpiration = Duration.ofMinutes(15);

    /**
     * Refresh token expiration time.
     */
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /**
     * Clock skew allowance in seconds to account for time differences.
     */
    private long clockSkew = 60; // 60 seconds

    /**
     * List of allowed issuers (empty means any issuer is allowed if issuer is set).
     */
    private List<String> allowedIssuers;

    /**
     * List of allowed audiences (empty means any audience is allowed if audience is set).
     */
    private List<String> allowedAudiences;

    /**
     * Cache TTL for validation results.
     */
    private Duration cacheTtl = Duration.ofMinutes(5);

    /**
     * Maximum cache size for validation results.
     */
    private int cacheMaxSize = 1000;

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Duration getDefaultExpiration() {
        return defaultExpiration;
    }

    public void setDefaultExpiration(Duration defaultExpiration) {
        this.defaultExpiration = defaultExpiration;
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(Duration refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public long getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(long clockSkew) {
        this.clockSkew = clockSkew;
    }

    public List<String> getAllowedIssuers() {
        return allowedIssuers;
    }

    public void setAllowedIssuers(List<String> allowedIssuers) {
        this.allowedIssuers = allowedIssuers;
    }

    public List<String> getAllowedAudiences() {
        return allowedAudiences;
    }

    public void setAllowedAudiences(List<String> allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    /**
     * Checks if the token uses asymmetric encryption (RS256, ES256, etc.).
     *
     * @return true if asymmetric algorithm
     */
    public boolean isAsymmetricAlgorithm() {
        return algorithm.startsWith("RS") || algorithm.startsWith("ES") || algorithm.startsWith("PS");
    }

    /**
     * Checks if the token uses symmetric encryption (HS256, HS512, etc.).
     *
     * @return true if symmetric algorithm
     */
    public boolean isSymmetricAlgorithm() {
        return algorithm.startsWith("HS");
    }

    /**
     * Checks if caching is enabled.
     *
     * @return true if cache is enabled
     */
    public boolean isCacheEnabled() {
        return cacheTtl != null && cacheTtl.toMillis() > 0 && cacheMaxSize > 0;
    }

    @Override
    public String toString() {
        return "JwtProperties{" +
               "enabled=" + enabled +
               ", secretKey='[PROTECTED]'" +
               ", issuer='" + issuer + '\'' +
               ", audience='" + audience + '\'' +
               ", publicKey='[PROTECTED]'" +
               ", privateKey='[PROTECTED]'" +
               ", algorithm='" + algorithm + '\'' +
               ", defaultExpiration=" + defaultExpiration +
               ", refreshTokenExpiration=" + refreshTokenExpiration +
               ", clockSkew=" + clockSkew +
               ", allowedIssuers=" + allowedIssuers +
               ", allowedAudiences=" + allowedAudiences +
               ", cacheTtl=" + cacheTtl +
               ", cacheMaxSize=" + cacheMaxSize +
               '}';
    }
}
