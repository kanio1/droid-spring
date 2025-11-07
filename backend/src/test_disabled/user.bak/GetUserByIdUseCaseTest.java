package com.droid.bss.application.query.user;

import com.droid.bss.domain.user.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetUserByIdUseCase
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserByIdUseCase")
class GetUserByIdUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserByIdUseCase getUserByIdUseCase;

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should get user by ID")
        void shouldGetUserById() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            GetUserByIdQuery query = new GetUserByIdQuery(userId);

            // When
            UserResponse result = getUserByIdUseCase.execute(query);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(userId);
            assertThat(result.email()).isEqualTo("john@example.com");
            assertThat(result.firstName()).isEqualTo("John");
            assertThat(result.lastName()).isEqualTo("Doe");
            assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.keycloakId()).isEqualTo("keycloak-123");
            assertThat(result.fullName()).isEqualTo("John Doe");

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("should get user with roles")
        void shouldGetUserWithRoles() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            user.assignRole(new Role("ADMIN", "Administrator role"));
            user.assignRole(new Role("USER", "Regular user role"));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            GetUserByIdQuery query = new GetUserByIdQuery(userId);

            // When
            UserResponse result = getUserByIdUseCase.execute(query);

            // Then
            assertThat(result.roles()).hasSize(2);
            assertThat(result.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("should get active user status")
        void shouldGetActiveUserStatus() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            GetUserByIdQuery query = new GetUserByIdQuery(userId);

            // When
            UserResponse result = getUserByIdUseCase.execute(query);

            // Then
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("should get inactive user status")
        void shouldGetInactiveUserStatus() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            user.changeStatus(UserStatus.INACTIVE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            GetUserByIdQuery query = new GetUserByIdQuery(userId);

            // When
            UserResponse result = getUserByIdUseCase.execute(query);

            // Then
            assertThat(result.isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            GetUserByIdQuery query = new GetUserByIdQuery(userId);

            // When
            Executable executable = () -> getUserByIdUseCase.execute(query);

            // Then
            assertThatIllegalArgumentException()
                .isThrownBy(executable)
                .withMessage("User not found with id: " + userId);
        }

        @Test
        @DisplayName("should exclude deleted user by default")
        void shouldExcludeDeletedUserByDefault() {
            // Given
            UUID userId = UUID.randomUUID();
            User deletedUser = User.create("keycloak-123", new UserInfo("Deleted", "User", "deleted@example.com"));
            deletedUser.markAsDeleted("admin");
            when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

            GetUserByIdQuery query = new GetUserByIdQuery(userId);

            // When
            Executable executable = () -> getUserByIdUseCase.execute(query);

            // Then
            assertThatIllegalStateException()
                .isThrownBy(executable)
                .withMessage("User with id " + userId + " is deleted");
        }
    }
}
