package com.droid.bss.domain.user;

/**
 * Enumeration of user status values.
 */
public enum UserStatus {
    /**
     * User is active and can access the system.
     */
    ACTIVE,

    /**
     * User is inactive and cannot access the system.
     */
    INACTIVE,

    /**
     * User is temporarily disabled.
     */
    SUSPENDED,

    /**
     * User is pending email verification.
     */
    PENDING_VERIFICATION,

    /**
     * User account is terminated and cannot be used.
     */
    TERMINATED;

    /**
     * Checks if the user can transition to the new status.
     */
    public boolean canTransitionTo(UserStatus newStatus) {
        if (this == newStatus) {
            return true;
        }

        return switch (this) {
            case PENDING_VERIFICATION -> newStatus == ACTIVE || newStatus == TERMINATED;
            case ACTIVE -> newStatus == INACTIVE || newStatus == SUSPENDED || newStatus == TERMINATED;
            case INACTIVE -> newStatus == ACTIVE || newStatus == TERMINATED;
            case SUSPENDED -> newStatus == ACTIVE || newStatus == INACTIVE || newStatus == TERMINATED;
            case TERMINATED -> false; // Terminal state
        };
    }

    /**
     * Checks if the user is active.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if the user is inactive.
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }

    /**
     * Checks if the user is suspended.
     */
    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    /**
     * Checks if the user is terminated.
     */
    public boolean isTerminated() {
        return this == TERMINATED;
    }

    /**
     * Checks if the user is pending verification.
     */
    public boolean isPendingVerification() {
        return this == PENDING_VERIFICATION;
    }
}
