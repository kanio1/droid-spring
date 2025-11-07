package com.droid.bss.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command to create a new user.
 */
public record CreateUserCommand(
    @NotBlank(message = "First name cannot be blank")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    String firstName,

    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    String lastName,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,

    @NotBlank(message = "Keycloak ID cannot be blank")
    String keycloakId
) {
}
