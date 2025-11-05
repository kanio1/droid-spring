package com.droid.bss.infrastructure.auth.oidc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for OIDC integration.
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "app.oidc")
@Validated
public class OidcProperties {

    /**
     * Whether OIDC is enabled.
     */
    private boolean enabled = true;

    /**
     * The OIDC issuer URI (e.g., https://keycloak.example.com/realms/myrealm).
     */
    @NotBlank(message = "Issuer URI cannot be blank")
    private String issuerUri;

    /**
     * The client ID registered with the OIDC provider.
     */
    @NotBlank(message = "Client ID cannot be blank")
    private String clientId;

    /**
     * The client secret (for confidential clients).
     */
    private String clientSecret;

    /**
     * The redirect URI for authorization code flow.
     */
    @NotBlank(message = "Redirect URI cannot be blank")
    private String redirectUri;

    /**
     * The logout redirect URI.
     */
    private String postLogoutRedirectUri;

    /**
     * The requested scopes (space-separated or comma-separated).
     */
    @NotEmpty(message = "Scopes cannot be empty")
    private List<String> scopes = List.of("openid", "profile", "email");

    /**
     * The authorization grant type (authorization_code, client_credentials, etc.).
     */
    private String grantType = "authorization_code";

    /**
     * The response type (code, token, id_token, etc.).
     */
    private String responseType = "code";

    /**
     * Token endpoint timeout.
     */
    private Duration tokenTimeout = Duration.ofSeconds(30);

    /**
     * UserInfo endpoint timeout.
     */
    private Duration userInfoTimeout = Duration.ofSeconds(10);

    /**
     * Introspection endpoint timeout.
     */
    private Duration introspectionTimeout = Duration.ofSeconds(10);

    /**
     * Enable PKCE (Proof Key for Code Exchange).
     */
    private boolean enablePkce = true;

    /**
     * Enable JWT client assertion for private key JWT.
     */
    private boolean enableClientAssertion = false;

    /**
     * Client authentication method (client_secret_basic, client_secret_post, private_key_jwt, etc.).
     */
    private String clientAuthMethod = "client_secret_basic";

    /**
     * Connection pool size for HTTP client.
     */
    private int connectionPoolSize = 10;

    /**
     * Maximum redirect count for HTTP client.
     */
    private int maxRedirects = 5;

    /**
     * Cache TTL for user info responses.
     */
    private Duration userInfoCacheTtl = Duration.ofMinutes(5);

    /**
     * Maximum cache size for user info responses.
     */
    private int userInfoCacheMaxSize = 1000;

    /**
     * Enable token introspection caching.
     */
    private boolean enableIntrospectionCache = true;

    /**
     * Cache TTL for introspection responses.
     */
    private Duration introspectionCacheTtl = Duration.ofMinutes(1);

    /**
     * Maximum cache size for introspection responses.
     */
    private int introspectionCacheMaxSize = 5000;

    /**
     * Scope separator (space or comma).
     */
    private String scopeSeparator = " ";

    /**
     * The OIDC provider name (Keycloak, Auth0, Okta, etc.).
     */
    private String provider = "keycloak";

    /**
     * Additional custom claims to request.
     */
    private List<String> additionalClaims;

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public Duration getTokenTimeout() {
        return tokenTimeout;
    }

    public void setTokenTimeout(Duration tokenTimeout) {
        this.tokenTimeout = tokenTimeout;
    }

    public Duration getUserInfoTimeout() {
        return userInfoTimeout;
    }

    public void setUserInfoTimeout(Duration userInfoTimeout) {
        this.userInfoTimeout = userInfoTimeout;
    }

    public Duration getIntrospectionTimeout() {
        return introspectionTimeout;
    }

    public void setIntrospectionTimeout(Duration introspectionTimeout) {
        this.introspectionTimeout = introspectionTimeout;
    }

    public boolean isEnablePkce() {
        return enablePkce;
    }

    public void setEnablePkce(boolean enablePkce) {
        this.enablePkce = enablePkce;
    }

    public boolean isEnableClientAssertion() {
        return enableClientAssertion;
    }

    public void setEnableClientAssertion(boolean enableClientAssertion) {
        this.enableClientAssertion = enableClientAssertion;
    }

    public String getClientAuthMethod() {
        return clientAuthMethod;
    }

    public void setClientAuthMethod(String clientAuthMethod) {
        this.clientAuthMethod = clientAuthMethod;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public Duration getUserInfoCacheTtl() {
        return userInfoCacheTtl;
    }

    public void setUserInfoCacheTtl(Duration userInfoCacheTtl) {
        this.userInfoCacheTtl = userInfoCacheTtl;
    }

    public int getUserInfoCacheMaxSize() {
        return userInfoCacheMaxSize;
    }

    public void setUserInfoCacheMaxSize(int userInfoCacheMaxSize) {
        this.userInfoCacheMaxSize = userInfoCacheMaxSize;
    }

    public boolean isEnableIntrospectionCache() {
        return enableIntrospectionCache;
    }

    public void setEnableIntrospectionCache(boolean enableIntrospectionCache) {
        this.enableIntrospectionCache = enableIntrospectionCache;
    }

    public Duration getIntrospectionCacheTtl() {
        return introspectionCacheTtl;
    }

    public void setIntrospectionCacheTtl(Duration introspectionCacheTtl) {
        this.introspectionCacheTtl = introspectionCacheTtl;
    }

    public int getIntrospectionCacheMaxSize() {
        return introspectionCacheMaxSize;
    }

    public void setIntrospectionCacheMaxSize(int introspectionCacheMaxSize) {
        this.introspectionCacheMaxSize = introspectionCacheMaxSize;
    }

    public String getScopeSeparator() {
        return scopeSeparator;
    }

    public void setScopeSeparator(String scopeSeparator) {
        this.scopeSeparator = scopeSeparator;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getAdditionalClaims() {
        return additionalClaims;
    }

    public void setAdditionalClaims(List<String> additionalClaims) {
        this.additionalClaims = additionalClaims;
    }

    /**
     * Gets scopes as a string.
     *
     * @return scopes joined with the configured separator
     */
    public String getScopesAsString() {
        return String.join(scopeSeparator, scopes);
    }

    /**
     * Checks if this is a confidential client.
     *
     * @return true if client secret is configured
     */
    public boolean isConfidentialClient() {
        return clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * Checks if authorization code flow is enabled.
     *
     * @return true if authorization_code grant type
     */
    public boolean isAuthorizationCodeFlow() {
        return "authorization_code".equals(grantType);
    }

    /**
     * Checks if PKCE is enabled.
     *
     * @return true if PKCE is enabled and authorization code flow is used
     */
    public boolean isPkceEnabled() {
        return enablePkce && isAuthorizationCodeFlow();
    }

    /**
     * Checks if user info caching is enabled.
     *
     * @return true if caching is enabled
     */
    public boolean isUserInfoCacheEnabled() {
        return userInfoCacheTtl != null && userInfoCacheTtl.toMillis() > 0 && userInfoCacheMaxSize > 0;
    }

    /**
     * Checks if introspection caching is enabled.
     *
     * @return true if caching is enabled
     */
    public boolean isIntrospectionCacheEnabled() {
        return enableIntrospectionCache &&
               introspectionCacheTtl != null && introspectionCacheTtl.toMillis() > 0 &&
               introspectionCacheMaxSize > 0;
    }

    /**
     * Checks if this is Keycloak provider.
     *
     * @return true if provider is keycloak
     */
    public boolean isKeycloak() {
        return "keycloak".equalsIgnoreCase(provider);
    }

    /**
     * Checks if this is Auth0 provider.
     *
     * @return true if provider is auth0
     */
    public boolean isAuth0() {
        return "auth0".equalsIgnoreCase(provider);
    }

    /**
     * Checks if this is Okta provider.
     *
     * @return true if provider is okta
     */
    public boolean isOkta() {
        return "okta".equalsIgnoreCase(provider);
    }

    @Override
    public String toString() {
        return "OidcProperties{" +
               "enabled=" + enabled +
               ", issuerUri='" + issuerUri + '\'' +
               ", clientId='" + clientId + '\'' +
               ", clientSecret='[PROTECTED]'" +
               ", redirectUri='" + redirectUri + '\'' +
               ", scopes=" + scopes +
               ", grantType='" + grantType + '\'' +
               ", provider='" + provider + '\'' +
               ", enablePkce=" + enablePkce +
               ", isConfidentialClient=" + isConfidentialClient() +
               '}';
    }
}
