package com.droid.bss.domain.subscription;

/**
 * Subscription status enumeration
 */
public enum SubscriptionStatus {
    ACTIVE,
    SUSPENDED,
    CANCELLED,
    EXPIRED;

    public boolean canTransitionTo(SubscriptionStatus newStatus) {
        return switch (this) {
            case ACTIVE -> newStatus == SUSPENDED || newStatus == CANCELLED || newStatus == EXPIRED;
            case SUSPENDED -> newStatus == ACTIVE || newStatus == CANCELLED || newStatus == EXPIRED;
            case CANCELLED, EXPIRED -> false; // Terminal states
        };
    }
}
