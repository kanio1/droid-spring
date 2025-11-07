package com.droid.bss.application.command.user;

import com.droid.bss.application.dto.user.UpdateUserCommand;
import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.domain.user.User;
import com.droid.bss.domain.user.UserInfo;
import com.droid.bss.domain.user.UserEntity;
import com.droid.bss.domain.user.UserRepository;
import com.droid.bss.infrastructure.keycloak.KeycloakUserAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for updating a user.
 */
@Service
public class UpdateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserUseCase.class);

    private final UserRepository userRepository;
    private final KeycloakUserAdapter keycloakUserAdapter;

    public UpdateUserUseCase(
        UserRepository userRepository,
        KeycloakUserAdapter keycloakUserAdapter
    ) {
        this.userRepository = userRepository;
        this.keycloakUserAdapter = keycloakUserAdapter;
    }

    /**
     * Updates a user.
     */
    @Transactional
    public UserResponse execute(UUID userId, UpdateUserCommand command) {
        log.info("Updating user: {}", userId);

        // Find user by ID
        Optional<UserEntity> optionalUserEntity = userRepository.findById(userId);
        if (optionalUserEntity.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        UserEntity userEntity = optionalUserEntity.get();

        // Check if email is being changed and if it's already taken
        if (!userEntity.getEmail().equals(command.email())) {
            Optional<UserEntity> existingUserByEmail = userRepository.findByEmail(command.email());
            if (existingUserByEmail.isPresent()) {
                throw new IllegalArgumentException("User with email '%s' already exists".formatted(command.email()));
            }
        }

        // Convert to domain object
        User currentUser = userEntity.toDomain();

        // Create updated user info
        UserInfo updatedUserInfo = new UserInfo(
            command.firstName(),
            command.lastName(),
            command.email(),
            currentUser.getUserInfo().keycloakId() // Keep original Keycloak ID
        );

        // Update user
        User updatedUser = currentUser.updateInfo(updatedUserInfo);

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
            keycloakUserAdapter.updateUser(finalUser.getUserInfo().keycloakId(), finalUser);
        } catch (Exception e) {
            log.error("Failed to update user in Keycloak", e);
        }

        log.info("User updated successfully: {}", userId);

        return UserResponse.from(finalUser);
    }
}
