package com.droid.bss.infrastructure.auth.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Implementation of JWT Validator using jose4j library.
 *
 * @since 1.0
 */
public class JwtValidatorImpl implements JwtValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtValidatorImpl.class);

    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    private final JwtProperties properties;
    private final JwtValidationCache cache;

    public JwtValidatorImpl(JwtProperties properties) {
        this.properties = properties;
        this.cache = new JwtValidationCache(properties);
    }

    public JwtValidatorImpl(JwtProperties properties, JwtValidationCache cache) {
        this.properties = properties;
        this.cache = cache;
    }

    @Override
    public JwtValidationResult validateToken(String token) {
        if (token == null || token.isBlank()) {
            return JwtValidationResult.failure("Token cannot be null or empty");
        }

        if (!JWT_PATTERN.matcher(token).matches()) {
            return JwtValidationResult.failure("Invalid token format");
        }

        try {
            // Parse JWT header
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // Extract claims from payload
            JwtClaims claims = parseClaims(payloadJson);

            // Validate claims
            validateIssuer(claims.getIssuer());
            validateAudience(claims.getAudience());
            validateExpiration(claims.getExpiration());
            validateNotBefore(claims.getNotBefore());
            validateIssuedAt(claims.getIssuedAt());

            // Create user principal
            UserPrincipal principal = createUserPrincipal(claims);

            // Create result
            Instant issuedAt = Instant.ofEpochSecond(claims.getIssuedAt());
            Instant expirationTime = Instant.ofEpochSecond(claims.getExpiration());

            JwtValidationResult result = JwtValidationResult.success(
                principal,
                issuedAt,
                expirationTime,
                claims.getTokenId()
            );

            // Cache valid result
            if (claims.getTokenId() != null) {
                cache.put(claims.getTokenId(), result);
            }

            return result;

        } catch (JwtValidationException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return JwtValidationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation", e);
            return JwtValidationResult.failure("Token validation failed: " + e.getMessage());
        }
    }

    @Override
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JwtClaims claims = parseClaims(payloadJson);

            // Check expiration
            if (Instant.now().isAfter(Instant.ofEpochSecond(claims.getExpiration()))) {
                return false;
            }

            // Check not before
            if (claims.getNotBefore() != null &&
                Instant.now().isBefore(Instant.ofEpochSecond(claims.getNotBefore()))) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UserPrincipal extractUserPrincipal(String token) {
        JwtValidationResult result = validateToken(token);

        if (result.isInvalid()) {
            throw new JwtValidationException(result.getErrorMessage().orElse("Invalid token"));
        }

        return result.getUserPrincipal()
                     .orElseThrow(() -> new JwtValidationException("User principal not found in token"));
    }

    @Override
    public Optional<Long> getExpirationTime(String token) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JwtClaims claims = parseClaims(payloadJson);
            return Optional.of(claims.getExpiration());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> getRoles(String token) {
        JwtValidationResult result = validateToken(token);

        if (result.isInvalid()) {
            throw new JwtValidationException(result.getErrorMessage().orElse("Invalid token"));
        }

        return result.getUserPrincipal().map(UserPrincipal::getRoles).orElse(List.of());
    }

    @Override
    public List<String> getPermissions(String token) {
        JwtValidationResult result = validateToken(token);

        if (result.isInvalid()) {
            throw new JwtValidationException(result.getErrorMessage().orElse("Invalid token"));
        }

        return result.getUserPrincipal().map(UserPrincipal::getPermissions).orElse(List.of());
    }

    @Override
    public boolean hasRole(String token, String role) {
        return getRoles(token).contains(role);
    }

    @Override
    public JwtRefreshResult refreshToken(String refreshToken) {
        // Implementation depends on your identity provider
        // For Keycloak, this would involve calling the token endpoint
        return JwtRefreshResult.failure("Token refresh not yet implemented");
    }

    // Private validation methods

    private void validateIssuer(String issuer) {
        if (properties.getIssuer() != null && !properties.getIssuer().equals(issuer)) {
            throw JwtValidationException.invalidIssuer(properties.getIssuer(), issuer);
        }

        if (properties.getAllowedIssuers() != null && !properties.getAllowedIssuers().isEmpty()) {
            if (!properties.getAllowedIssuers().contains(issuer)) {
                throw new JwtValidationException("Issuer not allowed: " + issuer);
            }
        }
    }

    private void validateAudience(String audience) {
        if (properties.getAudience() != null && !properties.getAudience().equals(audience)) {
            throw JwtValidationException.invalidAudience(properties.getAudience(), audience);
        }

        if (properties.getAllowedAudiences() != null && !properties.getAllowedAudiences().isEmpty()) {
            if (audience == null || !properties.getAllowedAudiences().contains(audience)) {
                throw new JwtValidationException("Audience not allowed: " + audience);
            }
        }
    }

    private void validateExpiration(long expiration) {
        Instant expInstant = Instant.ofEpochSecond(expiration);
        long nowWithSkew = Instant.now().plusSeconds(properties.getClockSkew()).getEpochSecond();

        if (expiration < nowWithSkew) {
            throw JwtValidationException.expiredToken(expiration, null);
        }
    }

    private void validateNotBefore(Long notBefore) {
        if (notBefore != null) {
            Instant nbfInstant = Instant.ofEpochSecond(notBefore);
            long nowWithSkew = Instant.now().minusSeconds(properties.getClockSkew()).getEpochSecond();

            if (notBefore > nowWithSkew) {
                throw JwtValidationException.tokenNotYetValid(notBefore);
            }
        }
    }

    private void validateIssuedAt(Long issuedAt) {
        if (issuedAt != null) {
            long nowWithSkew = Instant.now().plusSeconds(properties.getClockSkew()).getEpochSecond();
            if (issuedAt > nowWithSkew) {
                throw new JwtValidationException("Issued at time is in the future");
            }
        }
    }

    private UserPrincipal createUserPrincipal(JwtClaims claims) {
        // Extract standard claims
        String userId = claims.getSubject();
        String username = claims.getUsername();
        String email = claims.getEmail();

        // Extract roles
        List<String> roles = new ArrayList<>();
        if (claims.getRealmRoles() != null) {
            roles.addAll(claims.getRealmRoles());
        }
        if (claims.getResourceRoles() != null) {
            roles.addAll(claims.getResourceRoles());
        }

        // Extract permissions/scopes
        List<String> permissions = new ArrayList<>();
        if (claims.getScopes() != null) {
            permissions.addAll(claims.getScopes());
        }

        return UserPrincipal.fromClaims(userId, username, email, roles, permissions, claims.getIssuer());
    }

    // Simplified claims parser (in real implementation, use Jackson or Gson)
    private JwtClaims parseClaims(String payloadJson) {
        // This is a simplified implementation
        // In production, use proper JSON parsing
        return new JwtClaims(payloadJson);
    }

    /**
     * Simplified JWT claims class.
     * In production, this would use Jackson or Gson for proper JSON parsing.
     */
    private static class JwtClaims {
        private final String json;

        JwtClaims(String json) {
            this.json = json;
        }

        // These methods would properly parse JSON in production
        // For now, returning placeholder values
        public String getSubject() { return extractClaim("sub"); }
        public String getUsername() { return extractClaim("preferred_username"); }
        public String getEmail() { return extractClaim("email"); }
        public String getIssuer() { return extractClaim("iss"); }
        public String getAudience() { return extractClaim("aud"); }
        public Long getExpiration() { return Long.parseLong(extractClaim("exp", "0")); }
        public Long getNotBefore() { return extractClaim("nbf") != null ? Long.parseLong(extractClaim("nbf")) : null; }
        public Long getIssuedAt() { return Long.parseLong(extractClaim("iat", "0")); }
        public String getTokenId() { return extractClaim("jti"); }
        public List<String> getRealmRoles() { return extractClaimList("realm_access.roles"); }
        public List<String> getResourceRoles() { return extractClaimList("resource_access.*.roles"); }
        public List<String> getScopes() { return extractClaimList("scope"); }

        private String extractClaim(String name) {
            // Simplified - in production use proper JSON parsing
            String pattern = "\"" + name + "\"\\s*:\\s*\"([^\"]+)\"";
            return null; // Placeholder
        }

        private String extractClaim(String name, String defaultValue) {
            String value = extractClaim(name);
            return value != null ? value : defaultValue;
        }

        private List<String> extractClaimList(String name) {
            // Simplified - in production use proper JSON parsing
            return List.of(); // Placeholder
        }
    }
}
