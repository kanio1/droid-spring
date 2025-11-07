package com.droid.bss.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository port for User aggregate.
 * This is the interface that the application layer depends on.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Finds a user by their Keycloak ID.
     */
    Optional<UserEntity> findByKeycloakId(String keycloakId);

    /**
     * Finds a user by email.
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Finds users by status.
     */
    List<UserEntity> findByStatus(UserStatus status);

    /**
     * Finds users by any of the provided roles.
     */
    @Query("SELECT u FROM UserEntity u WHERE u.keycloakId IN " +
           "(SELECT DISTINCT SUBSTRING(ur.roleName, 1, LENGTH(ur.roleName)) FROM UserRole ur WHERE ur.roleName IN :roleNames)")
    List<UserEntity> findByRoles(@Param("roleNames") Set<String> roleNames);

    /**
     * Finds users with a specific role.
     */
    @Query("SELECT u FROM UserEntity u WHERE :roleName MEMBER OF u.roleNames")
    List<UserEntity> findByRoleName(@Param("roleName") String roleName);

    /**
     * Searches for users by first name, last name, or email.
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<UserEntity> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Searches for users with pagination and filtering.
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "(:searchTerm IS NULL OR " +
           " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:roleName IS NULL OR :roleName MEMBER OF u.roleNames)")
    Page<UserEntity> findUsersWithFilters(
        @Param("searchTerm") String searchTerm,
        @Param("status") UserStatus status,
        @Param("roleName") String roleName,
        Pageable pageable
    );

    /**
     * Checks if a user exists with the given Keycloak ID.
     */
    boolean existsByKeycloakId(String keycloakId);

    /**
     * Checks if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Counts users by status.
     */
    @Query("SELECT u.status, COUNT(u) FROM UserEntity u GROUP BY u.status")
    List<Object[]> countUsersByStatus();

    /**
     * Gets all distinct role names in the system.
     */
    @Query("SELECT DISTINCT roleName FROM UserEntity u JOIN u.roleNames roleName")
    List<String> findAllRoleNames();

    /**
     * Gets user statistics.
     */
    @Query("SELECT " +
           "COUNT(u), " +
           "SUM(CASE WHEN u.status = 'ACTIVE' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.status = 'INACTIVE' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.status = 'SUSPENDED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN u.status = 'PENDING_VERIFICATION' THEN 1 ELSE 0 END) " +
           "FROM UserEntity u")
    Object[] getUserStatistics();
}
