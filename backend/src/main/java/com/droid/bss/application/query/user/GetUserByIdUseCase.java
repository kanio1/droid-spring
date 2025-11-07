package com.droid.bss.application.query.user;

import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.domain.user.UserEntity;
import com.droid.bss.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for retrieving a user by ID.
 */
@Service
public class GetUserByIdUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetUserByIdUseCase.class);

    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets a user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse execute(UUID userId) {
        log.debug("Getting user by ID: {}", userId);

        Optional<UserEntity> userEntityOptional = userRepository.findById(userId);
        if (userEntityOptional.isEmpty()) {
            log.warn("User not found with ID: {}", userId);
            return null;
        }

        UserEntity userEntity = userEntityOptional.get();
        UserResponse userResponse = UserResponse.from(userEntity.toDomain());

        log.debug("Found user: {}", userResponse.email());
        return userResponse;
    }
}
