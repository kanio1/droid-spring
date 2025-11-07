package com.droid.bss.domain.asset;

/**
 * Types of network elements
 */
public enum NetworkElementType {
    ROUTER("Router - Network routing device"),
    CORE_ROUTER("Core Router - Main network routing node"),
    EDGE_ROUTER("Edge Router - Network edge routing device"),
    CORE_SWITCH("Core Switch - Central network switching device"),
    ACCESS_SWITCH("Access Switch - Local access switching device"),
    DISTRIBUTION_SWITCH("Distribution Switch - Intermediate switching device"),
    FIBER_NODE("Fiber Node - Fiber optic network termination"),
    OLT("OLT - Optical Line Terminal - Central GPON equipment"),
    ONT("ONT - Optical Network Terminal - Customer premise equipment"),
    OUN("OUN - Optical Network Unit - Multi-dwelling unit equipment"),
    SPLITTER("Splitter - Passive optical splitter for PON"),
    GPON_PORT("GPON Port - Gigabit PON interface"),
    BASE_STATION("Base Station - Wireless access point"),
    BTS("BTS - Base Transceiver Station"),
    NODE_B("Node B - 3G base station"),
    ENODEB("eNodeB - 4G/LTE base station"),
    GATEWAY("Gateway - Network protocol converter"),
    VPN_GATEWAY("VPN Gateway - VPN endpoint device"),
    VPN_CONCENTRATOR("VPN Concentrator - Central VPN device"),
    FIREWALL("Firewall - Network security device"),
    LOAD_BALANCER("Load Balancer - Traffic distribution device"),
    WAP("Wireless Access Point - WiFi access device"),
    FIBER_TERMINAL("Fiber Terminal - Fiber optic termination point");

    private final String description;

    NetworkElementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
