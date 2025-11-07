package com.droid.bss.domain.user;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA entity for User aggregate.
 */
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @ElementCollection
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role_name")
    private Set<String> roleNames;

    public UserEntity() {
        this.roleNames = new TreeSet<>();
    }

    public UserEntity(
        UUID id,
        String keycloakId,
        String firstName,
        String lastName,
        String email,
        UserStatus status,
        Set<String> roleNames
    ) {
        setId(id);
        this.keycloakId = keycloakId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.roleNames = roleNames != null ? new TreeSet<>(roleNames) : new TreeSet<>();
    }

    /**
     * Creates a UserEntity from a User domain aggregate.
     */
    public static UserEntity from(User user) {
        UserId userId = user.getId();
        UserInfo userInfo = user.getUserInfo();
        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        return new UserEntity(
            userId.value(),
            userInfo.keycloakId(),
            userInfo.firstName(),
            userInfo.lastName(),
            userInfo.email(),
            user.getStatus(),
            roleNames
        );
    }

    /**
     * Converts this entity to a User domain aggregate.
     */
    public User toDomain() {
        UserInfo userInfo = new UserInfo(firstName, lastName, email, keycloakId);

        // Reconstruct roles from names
        Set<Role> roles = roleNames.stream()
            .map(Role::fromKeycloak)
            .collect(Collectors.toCollection(TreeSet::new));

        // Use package-private factory method
        return User.withId(
            new UserId(getId()),
            userInfo,
            roles
        );
    }

    // Getters and setters
    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Set<String> getRoleNames() { return new TreeSet<>(roleNames); }
    public void setRoleNames(Set<String> roleNames) {
        this.roleNames = roleNames != null ? new TreeSet<>(roleNames) : new TreeSet<>();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
