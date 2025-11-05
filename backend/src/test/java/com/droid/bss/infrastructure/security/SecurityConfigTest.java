package com.droid.bss.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test for SecurityConfig
 * Following Arrange-Act-Assert pattern
 */
@SpringBootTest(classes = SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
    "security.oauth2.audience=bss-backend",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/bss"
})
@DisplayName("SecurityConfig Infrastructure Layer")
class SecurityConfigTest {

    @MockBean
    private Jwt jwt;

    @MockBean
    private HttpSecurity httpSecurity;

    @Value("${security.oauth2.audience}")
    private String audience;

    @Test
    @DisplayName("Should configure JWT authentication converter")
    void shouldConfigureJwtAuthenticationConverter() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        // Act
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        // Assert
        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("Should merge realm and resource authorities")
    void shouldMergeRealmAndResourceAuthorities() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);
        JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

        GrantedAuthority realmAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
        GrantedAuthority resourceAuthority = new SimpleGrantedAuthority("ROLE_USER");

        // Act
        Collection<GrantedAuthority> realmAuthorities = List.of(realmAuthority);
        Collection<GrantedAuthority> resourceAuthorities = List.of(resourceAuthority);

        // Use reflection to access private method
        java.lang.reflect.Method mergeMethod = SecurityConfig.class.getDeclaredMethod(
            "mergeAuthorities",
            Collection.class,
            Collection.class
        );
        mergeMethod.setAccessible(true);

        Collection<GrantedAuthority> merged = (Collection<GrantedAuthority>) mergeMethod.invoke(
            config,
            realmAuthorities,
            resourceAuthorities
        );

        // Assert
        assertThat(merged).isNotNull();
        assertThat(merged).hasSize(2);
        assertThat(merged).contains(realmAuthority, resourceAuthority);
    }

    @Test
    @DisplayName("Should extract resource roles from JWT")
    void shouldExtractResourceRolesFromJwt() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "bss-backend", Map.of(
                "roles", List.of("admin", "user", "viewer")
            )
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).hasSize(3);
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_admin", "ROLE_user", "ROLE_viewer");
    }

    @Test
    @DisplayName("Should handle JWT without resource access")
    void shouldHandleJwtWithoutResourceAccess() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        when(jwt.getClaimAsMap("resource_access")).thenReturn(null);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should handle JWT with empty resource access")
    void shouldHandleJwtWithEmptyResourceAccess() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "other-service", Map.of(
                "roles", List.of("admin")
            )
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should add ROLE_ prefix to roles without it")
    void shouldAddRolePrefixToRolesWithoutIt() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "bss-backend", Map.of(
                "roles", List.of("admin", "user")
            )
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .allMatch(role -> role.startsWith("ROLE_"));
    }

    @Test
    @DisplayName("Should not duplicate ROLE_ prefix if already present")
    void shouldNotDuplicateRolePrefixIfAlreadyPresent() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "bss-backend", Map.of(
                "roles", List.of("ROLE_admin", "user")
            )
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_admin", "ROLE_user");
    }

    @Test
    @DisplayName("Should handle invalid resource access format")
    void shouldHandleInvalidResourceAccessFormat() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "bss-backend", "invalid"
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should handle invalid roles format")
    void shouldHandleInvalidRolesFormat() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "bss-backend", Map.of(
                "roles", "invalid"
            )
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should handle null realm authorities")
    void shouldHandleNullRealmAuthorities() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method mergeMethod = SecurityConfig.class.getDeclaredMethod(
            "mergeAuthorities",
            Collection.class,
            Collection.class
        );
        mergeMethod.setAccessible(true);

        Collection<GrantedAuthority> merged = (Collection<GrantedAuthority>) mergeMethod.invoke(
            config,
            (Collection<GrantedAuthority>) null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Assert
        assertThat(merged).isNotNull();
        assertThat(merged).hasSize(1);
        assertThat(merged).contains(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("Should configure audience from property")
    void shouldConfigureAudienceFromProperty() {
        // Arrange & Act
        assertThat(audience).isEqualTo("bss-backend");
    }

    @Test
    @DisplayName("Should preserve order in merged authorities")
    void shouldPreserveOrderInMergedAuthorities() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        GrantedAuthority realm1 = new SimpleGrantedAuthority("ROLE_REALM1");
        GrantedAuthority realm2 = new SimpleGrantedAuthority("ROLE_REALM2");
        GrantedAuthority resource1 = new SimpleGrantedAuthority("ROLE_RESOURCE1");
        GrantedAuthority resource2 = new SimpleGrantedAuthority("ROLE_RESOURCE2");

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method mergeMethod = SecurityConfig.class.getDeclaredMethod(
            "mergeAuthorities",
            Collection.class,
            Collection.class
        );
        mergeMethod.setAccessible(true);

        Collection<GrantedAuthority> merged = (Collection<GrantedAuthority>) mergeMethod.invoke(
            config,
            List.of(realm1, realm2),
            List.of(resource1, resource2)
        );

        // Assert
        List<String> authorities = merged.stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        assertThat(authorities).containsExactly(
            "ROLE_REALM1",
            "ROLE_REALM2",
            "ROLE_RESOURCE1",
            "ROLE_RESOURCE2"
        );
    }

    @Test
    @DisplayName("Should handle empty role lists")
    void shouldHandleEmptyRoleLists() {
        // Arrange
        SecurityConfig config = new SecurityConfig(audience);

        Map<String, Object> resourceAccess = Map.of(
            "bss-backend", Map.of(
                "roles", List.of()
            )
        );

        when(jwt.getClaimAsMap("resource_access")).thenReturn(resourceAccess);

        // Act
        // Use reflection to access private method
        java.lang.reflect.Method extractMethod = SecurityConfig.class.getDeclaredMethod(
            "extractResourceRoles",
            Jwt.class
        );
        extractMethod.setAccessible(true);

        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) extractMethod.invoke(
            config,
            jwt
        );

        // Assert
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }

    @Configuration
    static class TestConfig {
        @Bean
        public SecurityConfig securityConfig(@Value("${security.oauth2.audience}") String audience) {
            return new SecurityConfig(audience);
        }
    }
}
