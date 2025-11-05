package com.droid.bss.domain.asset;

/**
 * Types of assets in the inventory
 */
public enum AssetType {
    ROUTER("Router - Network routing device"),
    SWITCH("Switch - Network switching device"),
    MODEM("Modem - Signal modulation/demodulation device"),
    SET_TOP_BOX("Set-Top Box - Television receiver device"),
    FIBER_OPTIC_CABLE("Fiber Optic Cable - High-speed data transmission"),
    COPPER_CABLE("Copper Cable - Traditional network cable"),
    POWER_SUPPLY("Power Supply - Electrical power unit"),
    ANTENNA("Antenna - Signal reception/transmission device"),
    SERVER("Server - Computing hardware"),
    STORAGE_DEVICE("Storage Device - Data storage hardware"),
    ACCESS_POINT("Access Point - Wireless network device"),
    REPEATER("Repeater - Signal amplification device"),
    DVR("DVR - Digital Video Recorder"),
    CAMERA("Camera - Video surveillance device"),
    SENSOR("Sensor - Monitoring device");

    private final String description;

    AssetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
