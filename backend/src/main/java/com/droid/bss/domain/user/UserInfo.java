package com.droid.bss.domain.user;

import java.util.Objects;

/**
 * Value object representing user personal and contact information.
 */
public record UserInfo(
    String firstName,
    String lastName,
    String email,
    String keycloakId
) {

    public UserInfo {
        Objects.requireNonNull(firstName, "First name cannot be null");
        Objects.requireNonNull(lastName, "Last name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(keycloakId, "Keycloak ID cannot be null");

        if (firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
    }

    /**
     * Gets the full name of the user.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Creates a new UserInfo with updated first name.
     */
    public UserInfo updateFirstName(String newFirstName) {
        return new UserInfo(newFirstName, lastName, email, keycloakId);
    }

    /**
     * Creates a new UserInfo with updated last name.
     */
    public UserInfo updateLastName(String newLastName) {
        return new UserInfo(firstName, newLastName, email, keycloakId);
    }

    /**
     * Creates a new UserInfo with updated email.
     */
    public UserInfo updateEmail(String newEmail) {
        return new UserInfo(firstName, lastName, newEmail, keycloakId);
    }
}
