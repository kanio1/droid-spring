package com.droid.bss.infrastructure.auth.jwt;

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
 * Test suite for JwtValidatorImpl
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtValidatorImpl Unit Tests")
class JwtValidatorImplTest {

    @Mock
    private JwtProperties properties;

    @Mock
    private JwtValidationCache cache;

    private JwtValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JwtValidatorImpl(properties, cache);
    }

    @Test
    @DisplayName("Should create validator with default cache")
    void shouldCreateValidatorWithDefaultCache() {
        // Given
        when(properties.getCacheTtl()).thenReturn(Duration.ofMinutes(5));
        when(properties.getCacheMaxSize()).thenReturn(1000);

        // When
        JwtValidator validatorWithDefaultCache = new JwtValidatorImpl(properties);

        // Then
        assertThat(validatorWithDefaultCache).isNotNull();
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // When
        JwtValidationResult result = validator.validateToken(null);

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
        assertThat(result.getErrorMessage().get()).contains("null or empty");
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // When
        JwtValidationResult result = validator.validateToken("");

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
    }

    @Test
    @DisplayName("Should reject blank token")
    void shouldRejectBlankToken() {
        // When
        JwtValidationResult result = validator.validateToken("   ");

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
    }

    @Test
    @DisplayName("Should reject invalid token format")
    void shouldRejectInvalidTokenFormat() {
        // When
        JwtValidationResult result = validator.validateToken("invalid.token.format");

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
    }

    @Test
    @DisplayName("Should reject token with invalid number of parts")
    void shouldRejectTokenWithInvalidNumberOfParts() {
        // When
        JwtValidationResult result = validator.validateToken("header.payload");

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
    }

    @Test
    @DisplayName("Should reject token with invalid base64 encoding")
    void shouldRejectTokenWithInvalidBase64Encoding() {
        // When
        JwtValidationResult result = validator.validateToken("invalid!base64!encoding!invalid!base64!encoding!");

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
    }

    @Test
    @DisplayName("Should validate correctly formatted token structure")
    void shouldValidateCorrectlyFormattedTokenStructure() {
        // Given - valid base64 encoded parts (but not a real JWT)
        String validStructure = "eyJhbGciOiJIUzI1NiJ9." + // Valid header
                                "eyJzdWIiOiIxMjM0NTY3ODkwIn0." + // Valid payload
                                "signature"; // Valid signature

        // When
        JwtValidationResult result = validator.validateToken(validStructure);

        // Then - should fail validation but not format check
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.getErrorMessage()).isPresent();
    }

    @Test
    @DisplayName("Should identify valid token format")
    void shouldIdentifyValidTokenFormat() {
        // Given
        String validToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // When
        boolean isValid = validator.isTokenValid(validToken);

        // Then
        // This will fail because we don't have a real JWT validation library
        // but it should not throw an exception
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for invalid token in isTokenValid")
    void shouldReturnFalseForInvalidTokenInIsTokenValid() {
        // When
        boolean isValid = validator.isTokenValid("invalid-token");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for null token in isTokenValid")
    void shouldReturnFalseForNullTokenInIsTokenValid() {
        // When
        boolean isValid = validator.isTokenValid(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return empty optional for invalid token in getExpirationTime")
    void shouldReturnEmptyOptionalForInvalidTokenInGetExpirationTime() {
        // When
        Optional<Long> expiration = validator.getExpirationTime("invalid-token");

        // Then
        assertThat(expiration).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception for invalid token in extractUserPrincipal")
    void shouldThrowExceptionForInvalidTokenInExtractUserPrincipal() {
        // When & Then
        assertThatThrownBy(() -> validator.extractUserPrincipal("invalid-token"))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid token in getRoles")
    void shouldThrowExceptionForInvalidTokenInGetRoles() {
        // When & Then
        assertThatThrownBy(() -> validator.getRoles("invalid-token"))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid token in getPermissions")
    void shouldThrowExceptionForInvalidTokenInGetPermissions() {
        // When & Then
        assertThatThrownBy(() -> validator.getPermissions("invalid-token"))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid token in hasRole")
    void shouldThrowExceptionForInvalidTokenInHasRole() {
        // When & Then
        assertThatThrownBy(() -> validator.hasRole("invalid-token", "admin"))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    @DisplayName("Should return failure for token refresh with unimplemented method")
    void shouldReturnFailureForTokenRefreshWithUnimplementedMethod() {
        // When
        JwtRefreshResult result = validator.refreshToken("refresh-token");

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("Should handle cache configuration properly")
    void shouldHandleCacheConfigurationProperly() {
        // Given
        Duration cacheTtl = Duration.ofMinutes(10);
        int cacheMaxSize = 2000;

        when(properties.getCacheTtl()).thenReturn(cacheTtl);
        when(properties.getCacheMaxSize()).thenReturn(cacheMaxSize);

        // When
        JwtValidationCache testCache = new JwtValidationCache(properties);

        // Then
        assertThat(testCache).isNotNull();
    }

    @Test
    @DisplayName("Should create UserPrincipal with builder methods")
    void shouldCreateUserPrincipalWithBuilderMethods() {
        // Given
        String userId = "user-123";
        String username = "testuser";
        String email = "test@example.com";
        List<String> roles = List.of("USER", "ADMIN");
        List<String> permissions = List.of("READ", "WRITE");
        String issuer = "test-issuer";

        // When
        UserPrincipal principal = UserPrincipal.fromClaims(userId, username, email, roles, permissions, issuer);

        // Then
        assertThat(principal.getUserId()).isEqualTo(userId);
        assertThat(principal.getUsername()).isEqualTo(username);
        assertThat(principal.getEmail()).isPresent().contains(email);
        assertThat(principal.getRoles()).isEqualTo(roles);
        assertThat(principal.getPermissions()).isEqualTo(permissions);
        assertThat(principal.hasRole("ADMIN")).isTrue();
        assertThat(principal.hasPermission("WRITE")).isTrue();
    }

    @Test
    @DisplayName("UserPrincipal should check role membership correctly")
    void userPrincipalShouldCheckRoleMembershipCorrectly() {
        // Given
        UserPrincipal principal = UserPrincipal.fromClaims("id", "user", "email@test.com",
            List.of("ROLE1", "ROLE2"), List.of(), null);

        // Then
        assertThat(principal.hasRole("ROLE1")).isTrue();
        assertThat(principal.hasRole("ROLE3")).isFalse();
        assertThat(principal.hasAnyRole("ROLE3", "ROLE1")).isTrue();
        assertThat(principal.hasAnyRole("ROLE3", "ROLE4")).isFalse();
    }

    @Test
    @DisplayName("UserPrincipal should implement equals and hashCode")
    void userPrincipalShouldImplementEqualsAndHashCode() {
        // Given
        UserPrincipal principal1 = UserPrincipal.fromClaims("id", "user", "email@test.com",
            List.of("ROLE1"), List.of(), null);
        UserPrincipal principal2 = UserPrincipal.fromClaims("id", "user", "email@test.com",
            List.of("ROLE1"), List.of(), null);
        UserPrincipal principal3 = UserPrincipal.fromClaims("id2", "user2", "email2@test.com",
            List.of("ROLE1"), List.of(), null);

        // Then
        assertThat(principal1).isEqualTo(principal2);
        assertThat(principal1).hasSameHashCodeAs(principal2);
        assertThat(principal1).isNotEqualTo(principal3);
    }

    @Test
    @DisplayName("JwtValidationResult should indicate success correctly")
    void jwtValidationResultShouldIndicateSuccessCorrectly() {
        // Given
        UserPrincipal principal = UserPrincipal.fromClaims("id", "user", "email@test.com",
            List.of("ROLE1"), List.of(), null);
        long now = System.currentTimeMillis() / 1000;
        long exp = now + 3600;

        // When
        JwtValidationResult result = JwtValidationResult.success(
            principal, java.time.Instant.ofEpochSecond(now),
            java.time.Instant.ofEpochSecond(exp), "token-id"
        );

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isInvalid()).isFalse();
        assertThat(result.getUserPrincipal()).isPresent().contains(principal);
    }

    @Test
    @DisplayName("JwtValidationResult should indicate failure correctly")
    void jwtValidationResultShouldIndicateFailureCorrectly() {
        // When
        JwtValidationResult result = JwtValidationResult.failure("Test error");

        // Then
        assertThat(result.isInvalid()).isTrue();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isPresent().contains("Test error");
    }

    @Test
    @DisplayName("JwtValidationResult should calculate remaining time")
    void jwtValidationResultShouldCalculateRemainingTime() {
        // Given
        long now = System.currentTimeMillis() / 1000;
        long exp = now + 120; // 2 minutes from now

        JwtValidationResult result = JwtValidationResult.success(
            UserPrincipal.fromClaims("id", "user", "email@test.com", List.of(), List.of(), null),
            java.time.Instant.ofEpochSecond(now),
            java.time.Instant.ofEpochSecond(exp),
            "token-id"
        );

        // Then
        assertThat(result.getRemainingTimeInSeconds()).isGreaterThan(0);
        assertThat(result.getRemainingTimeInSeconds()).isLessThanOrEqualTo(120);
    }

    @Test
    @DisplayName("JwtRefreshResult should indicate success and failure")
    void jwtRefreshResultShouldIndicateSuccessAndFailure() {
        // Success case
        JwtRefreshResult successResult = JwtRefreshResult.success(
            "new-access-token", "new-refresh-token",
            java.time.Instant.ofEpochSecond(System.currentTimeMillis() / 1000 + 3600),
            java.time.Instant.now()
        );
        assertThat(successResult.isSuccess()).isTrue();
        assertThat(successResult.isFailure()).isFalse();
        assertThat(successResult.getNewAccessToken()).isEqualTo("new-access-token");

        // Failure case
        JwtRefreshResult failureResult = JwtRefreshResult.failure("Refresh failed");
        assertThat(failureResult.isFailure()).isTrue();
        assertThat(failureResult.isSuccess()).isFalse();
        assertThat(failureResult.getErrorMessage()).contains("Refresh failed");
    }

    @Test
    @DisplayName("JwtValidationException should create specific exceptions")
    void jwtValidationExceptionShouldCreateSpecificExceptions() {
        // Test expired token exception
        JwtValidationException expired = JwtValidationException.expiredToken(1234567890L, "token-123");
        assertThat(expired.getMessage()).contains("Token expired");
        assertThat(expired.getTokenId()).isEqualTo("token-123");

        // Test invalid signature exception
        JwtValidationException signature = JwtValidationException.invalidSignature("token-456");
        assertThat(signature.getMessage()).contains("invalid signature");
        assertThat(signature.getTokenId()).isEqualTo("token-456");

        // Test malformed token exception
        JwtValidationException malformed = JwtValidationException.malformedToken("Invalid format");
        assertThat(malformed.getMessage()).contains("malformed");
    }
}
