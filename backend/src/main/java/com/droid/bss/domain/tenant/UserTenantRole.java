/**
 * User Role within a Tenant
 *
 * Defines the role a user has within a specific tenant
 * A user can have different roles in different tenants
 */
package com.droid.bss.domain.tenant;

public enum UserTenantRole {
    /**
     * Super admin - full access to all features across all tenants
     * Can manage all tenants
     */
    SUPER_ADMIN,

    /**
     * Tenant owner - complete control over the tenant
     * Can manage all aspects of the tenant
     */
    OWNER,

    /**
     * Tenant administrator - can manage most tenant settings
     * Cannot delete tenant or change billing
     */
    ADMIN,

    /**
     * Manager - can manage users, customers, and operations
     * Cannot access tenant settings or billing
     */
    MANAGER,

    /**
     * Support agent - can view and edit customer data
     * Cannot access financial information
     */
    SUPPORT,

    /**
     * Regular user - basic access to assigned customers
     * Limited permissions
     */
    USER,

    /**
     * Read-only access to tenant data
     * Cannot modify any data
     */
    VIEWER,

    /**
     * Temporary access for external auditors
     * Read-only with audit trail
     */
    AUDITOR
}
