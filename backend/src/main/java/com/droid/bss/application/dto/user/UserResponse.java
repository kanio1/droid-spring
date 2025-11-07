package com.droid.bss.application.dto.user;

import com.droid.bss.domain.user.UserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for User.
 */
public record UserResponse(
    UUID id,
    String keycloakId,
    String firstName,
    String lastName,
    String email,
    String fullName,
    UserStatus status,
    Set<String> roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    @JsonProperty("isActive")
    boolean active,

    @JsonProperty("isTerminated")
    boolean terminated
) {

    public static UserResponse from(com.droid.bss.domain.user.User user) {
        return new UserResponse(
            user.getId().value(),
            user.getUserInfo().keycloakId(),
            user.getUserInfo().firstName(),
            user.getUserInfo().lastName(),
            user.getUserInfo().email(),
            user.getUserInfo().getFullName(),
            user.getStatus(),
            user.getRoles().stream()
                .map(com.droid.bss.domain.user.Role::getName)
                .collect(java.util.stream.Collectors.toSet()),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.isActive(),
            user.isTerminated()
        );
    }
}
