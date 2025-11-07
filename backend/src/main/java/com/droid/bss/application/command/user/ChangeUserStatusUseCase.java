package com.droid.bss.application.command.user;

import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.domain.user.User;
import com.droid.bss.domain.user.UserEntity;
import com.droid.bss.domain.user.UserRepository;
import com.droid.bss.domain.user.UserStatus;
import com.droid.bss.infrastructure.keycloak.KeycloakUserAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for changing user status.
 */
@Service
public class ChangeUserStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangeUserStatusUseCase.class);

    private final UserRepository userRepository;
    private final KeycloakUserAdapter keycloakUserAdapter;

    public ChangeUserStatusUseCase(
        UserRepository userRepository,
        KeycloakUserAdapter keycloakUserAdapter
    ) {
        this.userRepository = userRepository;
        this.keycloakUserAdapter = keycloakUserAdapter;
    }

    /**
     * Changes user status.
     */
    @Transactional
    public UserResponse execute(UUID userId, UserStatus newStatus) {
        log.info("Changing status of user {} to {}", userId, newStatus);

        // Find user by ID
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if (optionalUserEntity.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        UserEntity userEntity = optionalUserEntity.get();

        // Convert to domain object
        User currentUser = userEntity.toDomain();

        // Change status
        User updatedUser = currentUser.changeStatus(newStatus);

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
            keycloakUserAdapter.changeUserStatus(
                finalUser.getUserInfo().keycloakId(),
                newStatus
            );
        } catch (Exception e) {
            log.error("Failed to change user status in Keycloak", e);
        }

        log.info("User status changed successfully: {}", userId);

        return UserResponse.from(finalUser);
    }
}
