package com.droid.bss.infrastructure.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    private final String audience;

    SecurityConfig(@Value("${security.oauth2.audience}") String audience) {
        this.audience = audience;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(registry -> registry
                .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class, PrometheusScrapeEndpoint.class)).permitAll()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter realmRoleConverter = new JwtGrantedAuthoritiesConverter();
        realmRoleConverter.setAuthoritiesClaimName("realm_access.roles");
        realmRoleConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> mergeAuthorities(realmRoleConverter.convert(jwt), extractResourceRoles(jwt)));
        return converter;
    }

    private Collection<GrantedAuthority> mergeAuthorities(Collection<GrantedAuthority> realmAuthorities, Collection<GrantedAuthority> resourceAuthorities) {
        Set<GrantedAuthority> merged = new LinkedHashSet<>();
        if (realmAuthorities != null) {
            merged.addAll(realmAuthorities);
        }
        merged.addAll(resourceAuthorities);
        return List.copyOf(merged);
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || !resourceAccess.containsKey(audience)) {
            return List.of();
        }

        Object clientAccessRaw = resourceAccess.get(audience);
        if (!(clientAccessRaw instanceof Map<?, ?> clientAccess)) {
            return List.of();
        }

        Object roles = clientAccess.get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream()
                .map(Object::toString)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(grantedAuthority -> (GrantedAuthority) grantedAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return List.of();
    }
}
