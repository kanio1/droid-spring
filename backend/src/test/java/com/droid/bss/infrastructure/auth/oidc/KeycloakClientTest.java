package com.droid.bss.infrastructure.auth.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for KeycloakClient
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakClient Unit Tests")
class KeycloakClientTest {

    @Mock
    private OidcProperties properties;

    @Mock
    private UserInfoCache userInfoCache;

    @Mock
    private TokenIntrospectionCache introspectionCache;

    private OidcClient client;

    @BeforeEach
    void setUp() {
        when(properties.getIssuerUri()).thenReturn("https://keycloak.example.com/realms/myrealm");
        when(properties.getClientId()).thenReturn("myclient");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8080/callback");
        when(properties.getScopes()).thenReturn(List.of("openid", "profile", "email"));
        when(properties.getScopesAsString()).thenReturn("openid profile email");
        when(properties.getScopesAsString()).thenReturn("openid profile email");
        when(properties.getUserInfoCacheTtl()).thenReturn(Duration.ofMinutes(5));
        when(properties.getUserInfoCacheMaxSize()).thenReturn(1000);
        when(properties.getIntrospectionCacheTtl()).thenReturn(Duration.ofMinutes(1));
        when(properties.getIntrospectionCacheMaxSize()).thenReturn(5000);
        when(properties.getScopesAsString()).thenReturn("openid profile email");

        client = new KeycloakClient(properties, userInfoCache, introspectionCache);
    }

    @Test
    @DisplayName("Should create client with default caches")
    void shouldCreateClientWithDefaultCaches() {
        // Given
        when(properties.getUserInfoCacheTtl()).thenReturn(Duration.ofMinutes(5));
        when(properties.getUserInfoCacheMaxSize()).thenReturn(1000);
        when(properties.getIntrospectionCacheTtl()).thenReturn(Duration.ofMinutes(1));
        when(properties.getIntrospectionCacheMaxSize()).thenReturn(5000);

        // When
        OidcClient defaultClient = new KeycloakClient(properties);

        // Then
        assertThat(defaultClient).isNotNull();
    }

    @Test
    @DisplayName("Should authenticate successfully with valid code")
    void shouldAuthenticateSuccessfullyWithValidCode() {
        // Given
        String code = "valid-auth-code";
        String redirectUri = "http://localhost:8080/callback";

        // When
        AuthenticationResponse response = client.authenticate(code, redirectUri);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getIdToken()).isNotNull();
        assertThat(response.getUserPrincipal()).isNotNull();
        assertThat(response.getUserPrincipal().getUsername()).isEqualTo("testuser");
        assertThat(response.getExpiresIn()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should fail authentication with null code")
    void shouldFailAuthenticationWithNullCode() {
        // When
        AuthenticationResponse response = client.authenticate(null, "http://localhost:8080/callback");

        // Then
        assertThat(response.isFailure()).isTrue();
        assertThat(response.getError()).contains("required");
    }

    @Test
    @DisplayName("Should fail authentication with blank code")
    void shouldFailAuthenticationWithBlankCode() {
        // When
        AuthenticationResponse response = client.authenticate("   ", "http://localhost:8080/callback");

        // Then
        assertThat(response.isFailure()).isTrue();
        assertThat(response.getError()).contains("required");
    }

    @Test
    @DisplayName("Should fail authentication with null redirect URI")
    void shouldFailAuthenticationWithNullRedirectUri() {
        // When
        AuthenticationResponse response = client.authenticate("code", null);

        // Then
        assertThat(response.isFailure()).isTrue();
        assertThat(response.getError()).contains("required");
    }

    @Test
    @DisplayName("Should get user info successfully")
    void shouldGetUserInfoSuccessfully() {
        // Given
        String accessToken = "valid-access-token";
        when(userInfoCache.get(accessToken)).thenReturn(null);

        // When
        UserInfo userInfo = client.getUserInfo(accessToken);

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getSubject()).isNotNull();
        assertThat(userInfo.getEmail()).isPresent().contains("test@example.com");
        assertThat(userInfo.getRoles()).contains("USER", "ADMIN");
    }

    @Test
    @DisplayName("Should use cached user info if available")
    void shouldUseCachedUserInfoIfAvailable() {
        // Given
        String accessToken = "valid-access-token";
        UserInfo cachedUser = UserInfo.builder()
            .subject("cached-user")
            .email("cached@example.com")
            .build();
        when(userInfoCache.get(accessToken)).thenReturn(cachedUser);

        // When
        UserInfo userInfo = client.getUserInfo(accessToken);

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getSubject()).isEqualTo("cached-user");
        verify(userInfoCache).get(accessToken);
    }

    @Test
    @DisplayName("Should throw exception for null access token in getUserInfo")
    void shouldThrowExceptionForNullAccessTokenInGetUserInfo() {
        // When & Then
        assertThatThrownBy(() -> client.getUserInfo(null))
            .isInstanceOf(OidcException.class);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        String refreshToken = "valid-refresh-token";

        // When
        TokenResponse response = client.refreshToken(refreshToken);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getExpiresIn()).isGreaterThan(0);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Should fail token refresh with null refresh token")
    void shouldFailTokenRefreshWithNullRefreshToken() {
        // When
        TokenResponse response = client.refreshToken(null);

        // Then
        assertThat(response.isFailure()).isTrue();
        assertThat(response.getError()).contains("required");
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Given
        String refreshToken = "valid-refresh-token";

        // When
        assertThatCode(() -> client.logout(refreshToken))
            .doesNotThrowAnyException();

        // Then
        verify(introspectionCache).clear();
    }

    @Test
    @DisplayName("Should not throw exception for null refresh token in logout")
    void shouldNotThrowExceptionForNullRefreshTokenInLogout() {
        // When & Then
        assertThatCode(() -> client.logout(null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should generate authorization URL")
    void shouldGenerateAuthorizationUrl() {
        // Given
        String state = "random-state";
        String scope = "openid profile email";

        // When
        String authUrl = client.getAuthorizationUrl(state, scope);

        // Then
        assertThat(authUrl).isNotNull();
        assertThat(authUrl).contains("https://keycloak.example.com/realms/myrealm");
        assertThat(authUrl).contains("client_id=myclient");
        assertThat(authUrl).contains("redirect_uri=");
        assertThat(authUrl).contains("response_type=code");
        assertThat(authUrl).contains("scope=");
        assertThat(authUrl).contains("state=random-state");
    }

    @Test
    @DisplayName("Should generate authorization URL with null state")
    void shouldGenerateAuthorizationUrlWithNullState() {
        // When
        String authUrl = client.getAuthorizationUrl(null, "openid");

        // Then
        assertThat(authUrl).isNotNull();
        assertThat(authUrl).doesNotContain("state=");
    }

    @Test
    @DisplayName("Should validate matching state")
    void shouldValidateMatchingState() {
        // Given
        String state = "test-state";
        String expectedState = "test-state";

        // When
        boolean isValid = client.validateState(state, expectedState);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate non-matching state")
    void shouldInvalidateNonMatchingState() {
        // Given
        String state = "test-state";
        String expectedState = "different-state";

        // When
        boolean isValid = client.validateState(state, expectedState);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate state when both are null")
    void shouldValidateStateWhenBothAreNull() {
        // When
        boolean isValid = client.validateState(null, null);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate state when only one is null")
    void shouldInvalidateStateWhenOnlyOneIsNull() {
        // When
        boolean isValid1 = client.validateState("state", null);
        boolean isValid2 = client.validateState(null, "state");

        // Then
        assertThat(isValid1).isFalse();
        assertThat(isValid2).isFalse();
    }

    @Test
    @DisplayName("Should get ID token")
    void shouldGetIdToken() {
        // Given
        String code = "valid-code";
        String redirectUri = "http://localhost:8080/callback";

        // When
        String idToken = client.getIdToken(code, redirectUri);

        // Then
        assertThat(idToken).isNotNull();
        assertThat(idToken).isNotBlank();
    }

    @Test
    @DisplayName("Should introspect token successfully")
    void shouldIntrospectTokenSuccessfully() {
        // Given
        String token = "valid-token";
        when(introspectionCache.get(token)).thenReturn(null);

        // When
        TokenIntrospectionResponse response = client.introspectToken(token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isActive()).isTrue();
        assertThat(response.getSubject()).isNotNull();
        assertThat(response.getIssuer()).isPresent().contains(properties.getIssuerUri());
    }

    @Test
    @DisplayName("Should use cached introspection result if available")
    void shouldUseCachedIntrospectionResultIfAvailable() {
        // Given
        String token = "valid-token";
        TokenIntrospectionResponse cachedResponse = TokenIntrospectionResponse.active(
            "cached-user", "issuer", "audience", "clientId",
            java.time.Instant.now(), java.time.Instant.now(),
            null, "scope", "type", "username", null
        );
        when(introspectionCache.get(token)).thenReturn(cachedResponse);

        // When
        TokenIntrospectionResponse response = client.introspectToken(token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("cached-user");
        verify(introspectionCache).get(token);
    }

    @Test
    @DisplayName("Should throw exception for null token in introspectToken")
    void shouldThrowExceptionForNullTokenInIntrospectToken() {
        // When & Then
        assertThatThrownBy(() -> client.introspectToken(null))
            .isInstanceOf(OidcException.class);
    }

    @Test
    @DisplayName("AuthenticationResponse should indicate success correctly")
    void authenticationResponseShouldIndicateSuccessCorrectly() {
        // Given
        UserPrincipal principal = UserPrincipal.fromClaims("id", "user", "email@test.com",
            List.of("ROLE1"), List.of(), null);

        // When
        AuthenticationResponse response = AuthenticationResponse.success(
            "access-token", "refresh-token", "id-token", "Bearer", 3600, principal, "state"
        );

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isFailure()).isFalse();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUserPrincipal()).isPresent().contains(principal);
        assertThat(response.hasRefreshToken()).isTrue();
        assertThat(response.hasIdToken()).isTrue();
    }

    @Test
    @DisplayName("AuthenticationResponse should indicate failure correctly")
    void authenticationResponseShouldIndicateFailureCorrectly() {
        // When
        AuthenticationResponse response = AuthenticationResponse.failure("Test error", "state");

        // Then
        assertThat(response.isFailure()).isTrue();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Test error");
        assertThat(response.getState()).isEqualTo("state");
    }

    @Test
    @DisplayName("UserInfo should build correctly")
    void userInfoShouldBuildCorrectly() {
        // When
        UserInfo userInfo = UserInfo.builder()
            .subject("test-subject")
            .name("Test User")
            .preferredUsername("testuser")
            .email("test@example.com")
            .emailVerified(true)
            .roles(List.of("USER", "ADMIN"))
            .permissions(List.of("READ", "WRITE"))
            .build();

        // Then
        assertThat(userInfo.getSubject()).isEqualTo("test-subject");
        assertThat(userInfo.getName()).isPresent().contains("Test User");
        assertThat(userInfo.getEmail()).isPresent().contains("test@example.com");
        assertThat(userInfo.isEmailVerified()).isTrue();
        assertThat(userInfo.hasRole("ADMIN")).isTrue();
        assertThat(userInfo.hasPermission("WRITE")).isTrue();
    }

    @Test
    @DisplayName("TokenResponse should indicate success and failure")
    void tokenResponseShouldIndicateSuccessAndFailure() {
        // Success
        TokenResponse successResponse = TokenResponse.success(
            "access", "refresh", "id", "Bearer", 3600, "scope"
        );
        assertThat(successResponse.isSuccess()).isTrue();
        assertThat(successResponse.getAccessToken()).isEqualTo("access");

        // Failure
        TokenResponse failureResponse = TokenResponse.failure("Error");
        assertThat(failureResponse.isFailure()).isTrue();
        assertThat(failureResponse.getError()).contains("Error");
    }

    @Test
    @DisplayName("TokenIntrospectionResponse should indicate active and inactive")
    void tokenIntrospectionResponseShouldIndicateActiveAndInactive() {
        // Active
        TokenIntrospectionResponse activeResponse = TokenIntrospectionResponse.active(
            "sub", "iss", "aud", "client",
            java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600),
            null, "scope", "type", "username", null
        );
        assertThat(activeResponse.isActive()).isTrue();
        assertThat(activeResponse.isInactive()).isFalse();

        // Inactive
        TokenIntrospectionResponse inactiveResponse = TokenIntrospectionResponse.inactive();
        assertThat(inactiveResponse.isInactive()).isTrue();
        assertThat(inactiveResponse.isActive()).isFalse();
    }

    @Test
    @DisplayName("OidcException should create specific exceptions")
    void oidcExceptionShouldCreateSpecificExceptions() {
        // Test authentication failed
        OidcException authFailed = OidcException.authenticationFailed("Invalid credentials");
        assertThat(authFailed.getMessage()).contains("authentication failed");

        // Test invalid state
        OidcException stateMismatch = OidcException.stateMismatch("expected", "actual");
        assertThat(stateMismatch.getMessage()).contains("State parameter mismatch");

        // Test token expired
        OidcException tokenExpired = OidcException.tokenExpired("token-123", 1234567890L);
        assertThat(tokenExpired.getMessage()).contains("Token expired");
        assertThat(tokenExpired.getErrorCode()).isEqualTo("Token Expired");
    }
}
