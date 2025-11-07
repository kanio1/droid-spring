package com.droid.bss.api.admin;

import com.droid.bss.application.command.user.AssignRolesUseCase;
import com.droid.bss.application.command.user.ChangeUserStatusUseCase;
import com.droid.bss.application.command.user.CreateUserUseCase;
import com.droid.bss.application.command.user.UpdateUserUseCase;
import com.droid.bss.application.dto.user.AssignRolesCommand;
import com.droid.bss.application.dto.user.CreateUserCommand;
import com.droid.bss.application.dto.user.UpdateUserCommand;
import com.droid.bss.application.dto.user.UserResponse;
import com.droid.bss.application.query.user.GetUserByIdUseCase;
import com.droid.bss.application.query.user.GetUsersUseCase;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.user.UserStatus;
import com.droid.bss.infrastructure.audit.Audited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for admin user management operations.
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin User Management", description = "Endpoints for managing users")
public class AdminUserController {

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final GetUsersUseCase getUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final AssignRolesUseCase assignRolesUseCase;
    private final ChangeUserStatusUseCase changeUserStatusUseCase;

    public AdminUserController(
        CreateUserUseCase createUserUseCase,
        UpdateUserUseCase updateUserUseCase,
        GetUsersUseCase getUsersUseCase,
        GetUserByIdUseCase getUserByIdUseCase,
        AssignRolesUseCase assignRolesUseCase,
        ChangeUserStatusUseCase changeUserStatusUseCase
    ) {
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.getUsersUseCase = getUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.assignRolesUseCase = assignRolesUseCase;
        this.changeUserStatusUseCase = changeUserStatusUseCase;
    }

    /**
     * Gets all users with optional filtering and pagination.
     */
    @GetMapping
    @Operation(summary = "Get users", description = "Retrieves users with optional search, status, and role filtering")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) UserStatus status,
        @RequestParam(required = false) String role,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(getUsersUseCase.execute(
            Optional.ofNullable(search),
            Optional.ofNullable(status),
            Optional.ofNullable(role),
            pageable
        ));
    }

    /**
     * Gets a user by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse user = getUserByIdUseCase.execute(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * Creates a new user.
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new user")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.USER_CREATE, entityType = "User", description = "Creating new user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserCommand command) {
        UserResponse user = createUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Updates a user.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.USER_UPDATE, entityType = "User", description = "Updating user {id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserCommand command
    ) {
        UserResponse user = updateUserUseCase.execute(id, command);
        return ResponseEntity.ok(user);
    }

    /**
     * Assigns roles to a user.
     */
    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign roles", description = "Assigns roles to a user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.USER_UPDATE, entityType = "User", description = "Assigning roles to user {id}")
    public ResponseEntity<UserResponse> assignRoles(
        @PathVariable UUID id,
        @Valid @RequestBody AssignRolesCommand command
    ) {
        UserResponse user = assignRolesUseCase.execute(id, command);
        return ResponseEntity.ok(user);
    }

    /**
     * Changes user status.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Change user status", description = "Changes the status of a user")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.USER_UPDATE, entityType = "User", description = "Changing status for user {id}")
    public ResponseEntity<UserResponse> changeUserStatus(
        @PathVariable UUID id,
        @RequestParam UserStatus status
    ) {
        UserResponse user = changeUserStatusUseCase.execute(id, status);
        return ResponseEntity.ok(user);
    }

    /**
     * Deletes a user (soft delete by changing status to TERMINATED).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user (sets status to TERMINATED)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.USER_DELETE, entityType = "User", description = "Deleting user {id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        changeUserStatusUseCase.execute(id, UserStatus.TERMINATED);
        return ResponseEntity.noContent().build();
    }
}
