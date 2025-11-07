package com.droid.bss.application.query.user;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.domain.user.UserEntity;
import com.droid.bss.domain.user.UserRepository;
import com.droid.bss.domain.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Use case for retrieving users with filtering and pagination.
 */
@Service
public class GetUsersUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetUsersUseCase.class);

    private final UserRepository userRepository;

    public GetUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gets users with optional filtering.
     */
    public PageResponse<UserResponse> execute(
        Optional<String> searchTerm,
        Optional<UserStatus> status,
        Optional<String> roleName,
        Pageable pageable
    ) {
        log.debug("Getting users with filters - search: {}, status: {}, role: {}, page: {}, size: {}",
            searchTerm.orElse("none"),
            status.map(Enum::name).orElse("none"),
            roleName.orElse("none"),
            pageable.getPageNumber(),
            pageable.getPageSize());

        Page<UserEntity> usersPage = userRepository.findUsersWithFilters(
            searchTerm.filter(StringUtils::hasText).orElse(null),
            status.orElse(null),
            roleName.filter(StringUtils::hasText).orElse(null),
            pageable
        );

        Page<UserResponse> responsePage = usersPage.map(userEntity -> {
            UserResponse userResponse = UserResponse.from(userEntity.toDomain());
            return userResponse;
        });

        log.debug("Found {} users (total: {})",
            responsePage.getNumberOfElements(),
            responsePage.getTotalElements());

        return PageResponse.of(
            responsePage.getContent(),
            responsePage.getNumber(),
            responsePage.getSize(),
            responsePage.getTotalElements()
        );
    }
}
