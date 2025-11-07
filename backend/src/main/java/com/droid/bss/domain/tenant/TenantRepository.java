/**
 * Tenant Repository - Port (Hexagonal Architecture)
 *
 * Interface for tenant data access
 * This is a PORT in hexagonal architecture
 */
package com.droid.bss.domain.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Tenant entity
 * Extends JpaRepository providing basic CRUD operations
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find tenant by code
     */
    Optional<Tenant> findByCode(String code);

    /**
     * Find tenant by domain
     */
    Optional<Tenant> findByDomain(String domain);

    /**
     * Find all active tenants
     */
    List<Tenant> findByStatus(TenantStatus status);

    /**
     * Find tenant by code and status
     */
    Optional<Tenant> findByCodeAndStatus(String code, TenantStatus status);

    /**
     * Find active tenants with tier
     */
    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE' AND t.tenantTier = :tier")
    List<Tenant> findByStatusAndTier(@Param("tier") String tier);

    /**
     * Check if tenant code exists
     */
    boolean existsByCode(String code);

    /**
     * Check if domain exists
     */
    boolean existsByDomain(String domain);

    /**
     * Find all tenants for a specific user
     * This is a complex query that will need to be implemented
     */
    @Query("""
        SELECT t FROM Tenant t
        JOIN t.userTenants ut
        WHERE ut.userId = :userId
        AND ut.isActive = true
        AND ut.deletedAt IS NULL
        ORDER BY ut.isDefault DESC, t.name ASC
        """)
    List<Tenant> findTenantsByUserId(@Param("userId") UUID userId);

    /**
     * Find default tenant for user
     */
    @Query("""
        SELECT t FROM Tenant t
        JOIN t.userTenants ut
        WHERE ut.userId = :userId
        AND ut.isDefault = true
        AND ut.isActive = true
        AND ut.deletedAt IS NULL
        """)
    Optional<Tenant> findDefaultTenantByUserId(@Param("userId") UUID userId);

    /**
     * Count users in tenant
     */
    @Query("""
        SELECT COUNT(ut) FROM UserTenant ut
        WHERE ut.tenantId = :tenantId
        AND ut.isActive = true
        AND ut.deletedAt IS NULL
        """)
    long countActiveUsers(@Param("tenantId") UUID tenantId);

    /**
     * Find tenants with custom branding
     */
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.customBranding IS NOT NULL
        AND t.customBranding != ''
        """)
    List<Tenant> findTenantsWithBranding();

    /**
     * Find tenants that support real-time features
     */
    @Query("""
        SELECT t FROM Tenant t
        WHERE t.settings.enableRealtimeNotifications = true
        """)
    List<Tenant> findTenantsWithRealtimeEnabled();

    /**
     * Search tenants by name or code
     */
    @Query("""
        SELECT t FROM Tenant t
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    List<Tenant> searchTenants(@Param("search") String search);
}
