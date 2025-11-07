package com.droid.bss.domain.workforce;

/**
 * Work order type enumeration
 */
public enum WorkOrderType {
    INSTALLATION("New service installation"),
    REPAIR("Service repair"),
    MAINTENANCE("Scheduled maintenance"),
    INSPECTION("Quality inspection"),
    DISCONNECT("Service disconnect"),
    RELOCATION("Service relocation"),
    UPGRADE("Service upgrade"),
    TROUBLESHOOTING("Problem diagnosis and resolution"),
    EMERGENCY("Emergency service call"),
    SURVEY("Site survey");

    private final String description;

    WorkOrderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresFieldVisit() {
        return this != TROUBLESHOOTING || this != SURVEY;
    }

    public int getPriority() {
        switch (this) {
            case EMERGENCY: return 1;
            case REPAIR: return 2;
            case INSTALLATION: return 3;
            case TROUBLESHOOTING: return 4;
            case RELOCATION: return 5;
            case UPGRADE: return 6;
            case DISCONNECT: return 7;
            case MAINTENANCE: return 8;
            case INSPECTION: return 9;
            case SURVEY: return 10;
            default: return 5;
        }
    }
}
