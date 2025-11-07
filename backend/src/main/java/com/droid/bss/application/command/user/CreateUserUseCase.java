package com.droid.bss.application.command.user;

import com.droid.bss.application.dto.user.CreateUserCommand;
import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.domain.user.User;
import com.droid.bss.domain.user.UserInfo;
import com.droid.bss.domain.user.UserRepository;
import com.droid.bss.infrastructure.keycloak.KeycloakUserAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for creating a new user.
 */
@Service
public class CreateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateUserUseCase.class);

    private final UserRepository userRepository;
    private final KeycloakUserAdapter keycloakUserAdapter;

    public CreateUserUseCase(
        UserRepository userRepository,
        KeycloakUserAdapter keycloakUserAdapter
    ) {
        this.userRepository = userRepository;
        this.keycloakUserAdapter = keycloakUserAdapter;
    }

    /**
     * Creates a new user.
     */
    @Transactional
    public UserResponse execute(CreateUserCommand command) {
        log.info("Creating new user with email: {}", command.email());

        // Check if user with email already exists
        Optional<com.droid.bss.domain.user.UserEntity> existingUserByEmail =
            userRepository.findByEmail(command.email());
        if (existingUserByEmail.isPresent()) {
            throw new IllegalArgumentException("User with email '%s' already exists".formatted(command.email()));
        }

        // Check if user with Keycloak ID already exists
        Optional<com.droid.bss.domain.user.UserEntity> existingUserByKeycloakId =
            userRepository.findByKeycloakId(command.keycloakId());
        if (existingUserByKeycloakId.isPresent()) {
            throw new IllegalArgumentException("User with Keycloak ID '%s' already exists".formatted(command.keycloakId()));
        }

        // Create user domain object
        UserInfo userInfo = new UserInfo(
            command.firstName(),
            command.lastName(),
            command.email(),
            command.keycloakId()
        );
        User user = User.create(userInfo);

        // Persist to database
        com.droid.bss.domain.user.UserEntity userEntity =
            com.droid.bss.domain.user.UserEntity.from(user);
        userEntity = userRepository.save(userEntity);

        // Sync with Keycloak
        User savedUser = userEntity.toDomain();
        try {
            keycloakUserAdapter.createUser(savedUser);
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak", e);
            // Decide on error handling strategy:
            // Option 1: Rollback (throw exception)
            // Option 2: Log and continue (user created in DB but not in Keycloak)
            // For now, we continue and log the error
        }

        log.info("User created successfully: {}", user.getId().value());

        return UserResponse.from(savedUser);
    }
}
