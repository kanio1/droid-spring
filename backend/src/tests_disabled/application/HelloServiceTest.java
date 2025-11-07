package com.droid.bss.application;

import com.droid.bss.application.dto.HelloResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("HelloService")
class HelloServiceTest {

    private final HelloService service = new HelloService();

    @Test
    @DisplayName("should build greeting message with subject and roles (positive scenario)")
    void shouldBuildGreetingWithSubjectAndRoles() {
        Jwt principal = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("alice")
            .claim("realm_access.roles", List.of("student", "mentor"))
            .build();

        HelloResponse response = service.greet(principal);

        assertThat(response.message()).isEqualTo("Hello, alice");
        assertThat(response.subject()).isEqualTo("alice");
        assertThat(response.roles()).containsExactly("student", "mentor");
    }

    @Test
    @DisplayName("should return empty roles when claim exists but list is empty (positive resilience)")
    void shouldReturnEmptyRolesWhenClaimEmptyList() {
        Jwt principal = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("bob")
            .claim("realm_access.roles", List.of())
            .build();

        HelloResponse response = service.greet(principal);

        assertThat(response.roles()).isEmpty();
        assertThat(response.message()).isEqualTo("Hello, bob");
    }

    @Test
    @DisplayName("should return empty roles when claim is missing (negative input)")
    void shouldReturnEmptyRolesWhenClaimMissing() {
        Jwt principal = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("carol")
            .build();

        HelloResponse response = service.greet(principal);

        assertThat(response.roles()).isEmpty();
        assertThat(response.message()).isEqualTo("Hello, carol");
    }

    @Test
    @DisplayName("should throw NullPointerException when principal is null (negative scenario)")
    void shouldThrowWhenPrincipalNull() {
        assertThrows(NullPointerException.class, () -> service.greet(null));
    }
}
