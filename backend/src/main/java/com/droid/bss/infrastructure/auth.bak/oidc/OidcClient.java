package com.droid.bss.infrastructure.auth.oidc;

import com.droid.bss.infrastructure.auth.jwt.UserPrincipal;

/**
 * Port (Interface) for OIDC (OpenID Connect) Client operations.
 *
 * This interface defines the contract for OIDC client implementations,
 * providing authentication, user info retrieval, and token management.
 *
 * @since 1.0
 */
public interface OidcClient {

    /**
     * Authenticates user using authorization code flow.
     *
     * @param code the authorization code from the callback
     * @param redirectUri the redirect URI used in the auth request
     * @return AuthenticationResponse containing tokens and user info
     * @throws OidcException if authentication fails
     */
    AuthenticationResponse authenticate(String code, String redirectUri);

    /**
     * Retrieves user information using access token.
     *
     * @param accessToken the access token
     * @return UserInfo containing user details
     * @throws OidcException if retrieval fails
     */
    UserInfo getUserInfo(String accessToken);

    /**
     * Refreshes access token using refresh token.
     *
     * @param refreshToken the refresh token
     * @return TokenResponse containing new tokens
     * @throws OidcException if refresh fails
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * Logs out user by invalidating tokens.
     *
     * @param refreshToken the refresh token to invalidate
     * @throws OidcException if logout fails
     */
    void logout(String refreshToken);

    /**
     * Gets the authorization URL for initiating OAuth2 flow.
     *
     * @param state optional state parameter for CSRF protection
     * @param scope the requested scopes
     * @return the authorization URL
     */
    String getAuthorizationUrl(String state, String scope);

    /**
     * Validates the state parameter to prevent CSRF attacks.
     *
     * @param state the state parameter from callback
     * @param expectedState the expected state value
     * @return true if valid
     */
    boolean validateState(String state, String expectedState);

    /**
     * Gets the ID token from authentication response.
     *
     * @param code the authorization code
     * @param redirectUri the redirect URI
     * @return the ID token string
     * @throws OidcException if token retrieval fails
     */
    String getIdToken(String code, String redirectUri);

    /**
     * Introspects a token to check if it's active and get claims.
     *
     * @param token the token to introspect
     * @return TokenIntrospectionResponse containing token status and claims
     * @throws OidcException if introspection fails
     */
    TokenIntrospectionResponse introspectToken(String token);
}
