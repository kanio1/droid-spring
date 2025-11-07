package com.droid.bss.application.query.user;

import com.droid.bss.domain.user.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetUsersUseCase
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetUsersUseCase")
class GetUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUsersUseCase getUsersUseCase;

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should get all users with pagination")
        void shouldGetAllUsersWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            User user2 = User.create("keycloak-2", new UserInfo("Jane", "Smith", "jane@example.com"));
            Page<User> userPage = new PageImpl<>(List.of(user1, user2), pageable, 2);

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery();

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("john@example.com");
            assertThat(result.getContent().get(1).email()).isEqualTo("jane@example.com");

            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("should filter users by search query")
        void shouldFilterUsersBySearchQuery() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            User user2 = User.create("keycloak-2", new UserInfo("Jane", "Smith", "jane@example.com"));
            Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

            when(userRepository.findBySearchQuery("john", pageable)).thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery("john");

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("john@example.com");
            verify(userRepository).findBySearchQuery("john", pageable);
        }

        @Test
        @DisplayName("should filter users by status")
        void shouldFilterUsersByStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

            when(userRepository.findByStatus(UserStatus.ACTIVE, pageable)).thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery(null, UserStatus.ACTIVE);

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findByStatus(UserStatus.ACTIVE, pageable);
        }

        @Test
        @DisplayName("should filter users by role")
        void shouldFilterUsersByRole() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            user1.assignRole(new Role("ADMIN", "Administrator role"));
            Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

            when(userRepository.findByRole("ADMIN", pageable)).thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery(null, null, "ADMIN");

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).roles()).contains("ADMIN");
            verify(userRepository).findByRole("ADMIN", pageable);
        }

        @Test
        @DisplayName("should filter users by multiple criteria")
        void shouldFilterUsersByMultipleCriteria() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            user1.assignRole(new Role("ADMIN", "Administrator role"));
            Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

            when(userRepository.findBySearchQueryAndStatusAndRole("john", UserStatus.ACTIVE, "ADMIN", pageable))
                .thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery("john", UserStatus.ACTIVE, "ADMIN");

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findBySearchQueryAndStatusAndRole("john", UserStatus.ACTIVE, "ADMIN", pageable);
        }

        @Test
        @DisplayName("should exclude deleted users by default")
        void shouldExcludeDeletedUsersByDefault() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            User deletedUser = User.create("keycloak-2", new UserInfo("Deleted", "User", "deleted@example.com"));
            deletedUser.markAsDeleted("admin");
            Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

            when(userRepository.findByDeletedFalse(pageable)).thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery();

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("john@example.com");
            verify(userRepository).findByDeletedFalse(pageable);
        }

        @Test
        @DisplayName("should return empty page when no users match criteria")
        void shouldReturnEmptyPageWhenNoUsersMatchCriteria() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            GetUsersQuery query = new GetUsersQuery("nonexistent", UserStatus.ACTIVE, "ADMIN");

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should map user roles to response")
        void shouldMapUserRolesToResponse() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            User user1 = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            user1.assignRole(new Role("ADMIN", "Administrator role"));
            user1.assignRole(new Role("USER", "Regular user role"));
            Page<User> userPage = new PageImpl<>(List.of(user1), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            GetUsersQuery query = new GetUsersQuery();

            // When
            Page<UserResponse> result = getUsersUseCase.execute(query, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            UserResponse response = result.getContent().get(0);
            assertThat(response.roles()).hasSize(2);
            assertThat(response.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
        }
    }
}
