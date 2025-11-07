package com.droid.bss.application.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Command to assign roles to a user.
 */
public record AssignRolesCommand(
    @NotNull(message = "Roles cannot be null")
    Set<String> roles
) {
}
