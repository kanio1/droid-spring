package com.droid.bss.infrastructure.auth.oidc;

/**
 * Stub class for OIDC properties
 * Minimal implementation for testing purposes
 */
public class OidcProperties {

    private String issuer;
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
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
}
