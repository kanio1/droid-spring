package com.droid.bss.infrastructure.auth.oidc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for OIDC client (supports Keycloak, Auth0, Okta, etc.).
 *
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(OidcProperties.class)
@ConditionalOnProperty(name = "app.oidc.enabled", havingValue = "true", matchIfMissing = true)
public class KeycloakConfig {

    /**
     * Creates an OIDC client bean.
     *
     * @param properties the OIDC properties
     * @return OidcClient instance
     */
    @Bean
    @Primary
    public OidcClient oidcClient(OidcProperties properties) {
        // In a real implementation, this would create the appropriate client
        // based on the provider (Keycloak, Auth0, Okta, etc.)
        return new KeycloakClient(properties);
    }

    /**
     * Creates a UserInfo cache bean.
     *
     * @param properties the OIDC properties
     * @return UserInfoCache instance
     */
    @Bean
    public UserInfoCache userInfoCache(OidcProperties properties) {
        return new UserInfoCache(properties);
    }

    /**
     * Creates a TokenIntrospection cache bean.
     *
     * @param properties the OIDC properties
     * @return TokenIntrospectionCache instance
     */
    @Bean
    public TokenIntrospectionCache introspectionCache(OidcProperties properties) {
        return new TokenIntrospectionCache(properties);
    }
}
