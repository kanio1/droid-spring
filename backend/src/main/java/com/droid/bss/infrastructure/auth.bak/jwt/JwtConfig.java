package com.droid.bss.infrastructure.auth.jwt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for JWT Validation.
 *
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnProperty(name = "app.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class JwtConfig {

    /**
     * Creates a JWT validator bean.
     *
     * @param properties the JWT properties
     * @return JwtValidator instance
     */
    @Bean
    @Primary
    public JwtValidator jwtValidator(JwtProperties properties) {
        return new JwtValidatorImpl(properties);
    }

    /**
     * Creates a JWT validation cache configuration.
     *
     * @param properties the JWT properties
     * @return JwtValidationCache instance
     */
    @Bean
    public JwtValidationCache jwtValidationCache(JwtProperties properties) {
        return new JwtValidationCache(properties);
    }
}
