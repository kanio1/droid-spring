package com.droid.bss.api.admin;

import com.droid.bss.application.command.user.*;
import com.droid.bss.application.query.user.*;
import com.droid.bss.domain.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminUserController
 */
@WebMvcTest(AdminUserController.class)
@DisplayName("AdminUserController")
class AdminUserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateUserUseCase createUserUseCase;

    @MockBean
    private UpdateUserUseCase updateUserUseCase;

    @MockBean
    private AssignRolesUseCase assignRolesUseCase;

    @MockBean
    private ChangeUserStatusUseCase changeUserStatusUseCase;

    @MockBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockBean
    private GetUsersUseCase getUsersUseCase;

    @MockBean
    private GetUserByIdUseCase getUserByIdUseCase;

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetUsers {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return list of users with pagination")
        void shouldReturnListOfUsersWithPagination() throws Exception {
            // Given
            User user = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);
            UserResponse userResponse = UserResponse.from(user);

            when(getUsersUseCase.execute(any(GetUsersQuery.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1));

            // When & Then
            mockMvc.perform(get("/api/admin/users")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

            verify(getUsersUseCase).execute(any(GetUsersQuery.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter users by search query")
        void shouldFilterUsersBySearchQuery() throws Exception {
            // Given
            User user = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);
            Page<User> userPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1);

            when(getUsersUseCase.execute(
                eq(new GetUsersQuery("john", null, null)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1));

            // When & Then
            mockMvc.perform(get("/api/admin/users")
                    .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"));

            verify(getUsersUseCase).execute(
                eq(new GetUsersQuery("john", null, null)),
                any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should filter users by status")
        void shouldFilterUsersByStatus() throws Exception {
            // Given
            User user = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);
            Page<User> userPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1);

            when(getUsersUseCase.execute(
                eq(new GetUsersQuery(null, UserStatus.ACTIVE, null)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1));

            // When & Then
            mockMvc.perform(get("/api/admin/users")
                    .param("status", "ACTIVE"))
                .andExpect(status().isOk());

            verify(getUsersUseCase).execute(
                eq(new GetUsersQuery(null, UserStatus.ACTIVE, null)),
                any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{id}")
    class GetUserById {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return user by ID")
        void shouldReturnUserById() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            User user = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);

            when(getUserByIdUseCase.execute(eq(new GetUserByIdQuery(userId))))
                .thenReturn(userResponse);

            // When & Then
            mockMvc.perform(get("/api/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

            verify(getUserByIdUseCase).execute(eq(new GetUserByIdQuery(userId)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            when(getUserByIdUseCase.execute(eq(new GetUserByIdQuery(userId))))
                .thenThrow(new IllegalArgumentException("User not found with id: " + userId));

            // When & Then
            mockMvc.perform(get("/api/admin/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));

            verify(getUserByIdUseCase).execute(eq(new GetUserByIdQuery(userId)));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/users")
    class CreateUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() throws Exception {
            // Given
            CreateUserCommand command = new CreateUserCommand(
                "keycloak-123",
                "John",
                "Doe",
                "john@example.com"
            );
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);

            when(createUserUseCase.execute(eq(command), eq("admin")))
                .thenReturn(user);

            // When & Then
            mockMvc.perform(post("/api/admin/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

            verify(createUserUseCase).execute(eq(command), eq("admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            // Given
            CreateUserCommand command = new CreateUserCommand(
                "keycloak-123",
                "John",
                "Doe",
                "john@example.com"
            );

            when(createUserUseCase.execute(eq(command), eq("admin")))
                .thenThrow(new IllegalArgumentException("User with email john@example.com already exists"));

            // When & Then
            mockMvc.perform(post("/api/admin/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"));

            verify(createUserUseCase).execute(eq(command), eq("admin"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{id}")
    class UpdateUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            UpdateUserCommand command = new UpdateUserCommand(
                "Jane",
                "Smith",
                "jane@example.com"
            );
            User user = User.create("keycloak-123", new UserInfo("Jane", "Smith", "jane@example.com"));
            UserResponse userResponse = UserResponse.from(user);

            when(updateUserUseCase.execute(eq(userId), eq(command), eq("admin")))
                .thenReturn(user);

            // When & Then
            mockMvc.perform(put("/api/admin/users/{id}", userId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));

            verify(updateUserUseCase).execute(eq(userId), eq(command), eq("admin"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{id}/roles")
    class AssignRoles {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should assign roles to user")
        void shouldAssignRolesToUser() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            Set<String> roleNames = Set.of("ADMIN", "USER");
            AssignRolesCommand command = new AssignRolesCommand(roleNames);
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);

            when(assignRolesUseCase.execute(eq(userId), eq(command), eq("admin")))
                .thenReturn(user);

            // When & Then
            mockMvc.perform(put("/api/admin/users/{id}/roles", userId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));

            verify(assignRolesUseCase).execute(eq(userId), eq(command), eq("admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should clear all roles when empty set is provided")
        void shouldClearAllRolesWhenEmptySetIsProvided() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            AssignRolesCommand command = new AssignRolesCommand(Set.of());
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);

            when(assignRolesUseCase.execute(eq(userId), eq(command), eq("admin")))
                .thenReturn(user);

            // When & Then
            mockMvc.perform(put("/api/admin/users/{id}/roles", userId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk());

            verify(assignRolesUseCase).execute(eq(userId), eq(command), eq("admin"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/users/{id}/status")
    class ChangeUserStatus {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should change user status")
        void shouldChangeUserStatus() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            ChangeUserStatusCommand command = new ChangeUserStatusCommand(UserStatus.INACTIVE);
            User user = User.create("keycloak-123", new UserInfo("John", "Doe", "john@example.com"));
            user.changeStatus(UserStatus.INACTIVE);
            UserResponse userResponse = UserResponse.from(user);

            when(changeUserStatusUseCase.execute(eq(userId), eq(command), eq("admin")))
                .thenReturn(user);

            // When & Then
            mockMvc.perform(put("/api/admin/users/{id}/status", userId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

            verify(changeUserStatusUseCase).execute(eq(userId), eq(command), eq("admin"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/users/{id}")
    class DeleteUser {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should delete user successfully")
        void shouldDeleteUserSuccessfully() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(delete("/api/admin/users/{id}", userId)
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(deleteUserUseCase).execute(eq(userId), eq("admin"));
        }
    }

    @Nested
    @DisplayName("security")
    class Security {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when user does not have ADMIN role")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenUserDoesNotHaveAdminRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("should allow access for SUPER_ADMIN role")
        void shouldAllowAccessForSuperAdminRole() throws Exception {
            // Given
            User user = User.create("keycloak-1", new UserInfo("John", "Doe", "john@example.com"));
            UserResponse userResponse = UserResponse.from(user);
            Page<User> userPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1);

            when(getUsersUseCase.execute(any(GetUsersQuery.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1));

            // When & Then
            mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());

            verify(getUsersUseCase).execute(any(GetUsersQuery.class), any(Pageable.class));
        }
    }
}
