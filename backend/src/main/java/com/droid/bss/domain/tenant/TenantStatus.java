/**
 * Tenant Status Enum
 *
 * Defines the lifecycle states of a tenant in the system
 */
package com.droid.bss.domain.tenant;

public enum TenantStatus {
    /**
     * Tenant is active and fully operational
     */
    ACTIVE,

    /**
     * Tenant is suspended due to payment issues or policy violations
     * Access is restricted but data is preserved
     */
    SUSPENDED,

    /**
     * Tenant is temporarily disabled
     * Can be reactivated
     */
    INACTIVE,

    /**
     * Tenant has been deleted
     * Data may be archived or purged based on retention policy
     */
    DELETED,

    /**
     * Tenant is in trial period
     * Limited features and quotas
     */
    TRIAL,

    /**
     * Tenant has expired trial
     * Needs to upgrade to continue
     */
    EXPIRED
}
