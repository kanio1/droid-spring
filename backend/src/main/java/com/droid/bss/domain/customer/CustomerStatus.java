package com.droid.bss.domain.customer;

public enum CustomerStatus {
    
    ACTIVE("Aktywny"),
    INACTIVE("Nieaktywny"),
    SUSPENDED("Zawieszony"),
    TERMINATED("Rozwiązany");
    
    private final String displayName;
    
    CustomerStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean canTransitionTo(CustomerStatus newStatus) {
        return switch (this) {
            case ACTIVE -> newStatus == INACTIVE || newStatus == SUSPENDED || newStatus == TERMINATED;
            case INACTIVE -> newStatus == ACTIVE || newStatus == TERMINATED;
            case SUSPENDED -> newStatus == ACTIVE || newStatus == TERMINATED;
            case TERMINATED -> false; // Terminal state
        };
    }
}
