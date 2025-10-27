package com.droid.bss.api;

import com.droid.bss.application.HelloService;
import com.droid.bss.application.dto.HelloResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HelloController.class)
@Import(HelloControllerWebTest.TestSecurityConfiguration.class)
@TestPropertySource(properties = "security.oauth2.audience=bss-backend")
@DisplayName("HelloController Web layer")
class HelloControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HelloService helloService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
            http.authorizeHttpRequests(registry -> registry
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setPrincipalClaimName("sub");
            return converter;
        }
    }

    @Test
    @DisplayName("should return greeting JSON when request carries JWT (positive)")
    void shouldReturnGreetingWhenAuthenticated() throws Exception {
        when(helloService.greet(any())).thenReturn(new HelloResponse("Hello, dana", "dana", List.of("student")));

        mockMvc.perform(get("/api/hello")
                .with(jwt().jwt(builder -> builder
                    .subject("dana")
                    .claim("realm_access.roles", List.of("student")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Hello, dana"))
            .andExpect(jsonPath("$.subject").value("dana"))
            .andExpect(jsonPath("$.roles[0]").value("student"));
    }

    @Test
    @DisplayName("should reject request without JWT (negative)")
    void shouldReturnUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/api/hello"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return empty roles array when service reports none (positive resilience)")
    void shouldReturnEmptyRolesArray() throws Exception {
        when(helloService.greet(any())).thenReturn(new HelloResponse("Hello, finn", "finn", List.of()));

        mockMvc.perform(get("/api/hello")
                .with(jwt().jwt(builder -> builder.subject("finn"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles").isArray())
            .andExpect(jsonPath("$.roles").isEmpty());
    }

    @Test
    @DisplayName("should reject unsupported HTTP method (negative)")
    void shouldReturnMethodNotAllowedOnPost() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/hello")
                .with(jwt().jwt(builder -> builder.subject("grace"))))
            .andExpect(status().isMethodNotAllowed());
    }
}
