package com.droid.bss.infrastructure.auth.oidc;

/**
 * Stub interface for OIDC client
 * Minimal implementation for testing purposes
 */
public interface OidcClient {

    /**
     * Get user info
     */
    UserInfo getUserInfo(String accessToken);

    /**
     * Introspect token
     */
    TokenIntrospection introspectToken(String token);

    /**
     * Exchange authorization code for tokens
     */
    TokenResponse exchangeCode(String code, String redirectUri);

    /**
     * Refresh access token
     */
    TokenResponse refreshToken(String refreshToken);
}

/**
 * Stub class for token response
 * Minimal implementation for testing purposes
 */
class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private int expiresIn;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }
}
