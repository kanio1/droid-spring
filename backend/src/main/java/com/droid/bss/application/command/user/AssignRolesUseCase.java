package com.droid.bss.application.command.user;

import com.droid.bss.application.dto.user.AssignRolesCommand;
import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.domain.user.Role;
import com.droid.bss.domain.user.User;
import com.droid.bss.domain.user.UserEntity;
import com.droid.bss.domain.user.UserRepository;
import com.droid.bss.infrastructure.keycloak.KeycloakUserAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for assigning roles to a user.
 */
@Service
public class AssignRolesUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssignRolesUseCase.class);

    private final UserRepository userRepository;
    private final KeycloakUserAdapter keycloakUserAdapter;

    public AssignRolesUseCase(
        UserRepository userRepository,
        KeycloakUserAdapter keycloakUserAdapter
    ) {
        this.userRepository = userRepository;
        this.keycloakUserAdapter = keycloakUserAdapter;
    }

    /**
     * Assigns roles to a user.
     */
    @Transactional
    public UserResponse execute(UUID userId, AssignRolesCommand command) {
        log.info("Assigning roles to user {}: {}", userId, command.roles());

        // Find user by ID
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if (optionalUserEntity.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        UserEntity userEntity = optionalUserEntity.get();

        // Convert to domain object
        User currentUser = userEntity.toDomain();

        // Create role objects from names
        Set<Role> roles = command.roles().stream()
            .map(Role::fromKeycloak)
            .collect(Collectors.toCollection(TreeSet::new));

        // Assign roles
        User updatedUser = currentUser.assignRoles(roles);

        // Persist to database
        UserEntity updatedEntity = UserEntity.from(updatedUser);
        updatedEntity.setId(userEntity.getId());
        updatedEntity.setCreatedAt(userEntity.getCreatedAt());
        updatedEntity.setCreatedBy(userEntity.getCreatedBy());
        updatedEntity.setVersion(userEntity.getVersion());
        updatedEntity = userRepository.save(updatedEntity);

        // Sync with Keycloak
        User finalUser = updatedEntity.toDomain();
        try {
            keycloakUserAdapter.assignRoles(
                finalUser.getUserInfo().keycloakId(),
                command.roles()
            );
        } catch (Exception e) {
            log.error("Failed to assign roles in Keycloak", e);
        }

        log.info("Roles assigned successfully to user: {}", userId);

        return UserResponse.from(finalUser);
    }
}
