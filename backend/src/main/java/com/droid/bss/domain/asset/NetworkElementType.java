package com.droid.bss.domain.asset;

/**
 * Types of network elements
 */
public enum NetworkElementType {
    CORE_ROUTER("Core Router - Main network routing node"),
    EDGE_ROUTER("Edge Router - Network edge routing device"),
    CORE_SWITCH("Core Switch - Central network switching device"),
    ACCESS_SWITCH("Access Switch - Local access switching device"),
    FIBER_NODE("Fiber Node - Fiber optic network termination"),
    OLT("OLT - Optical Line Terminal"),
    ONT("ONT - Optical Network Terminal"),
    BASE_STATION("Base Station - Wireless access point"),
    BTS("BTS - Base Transceiver Station"),
    NODE_B("Node B - 3G base station"),
    ENODEB("eNodeB - 4G/LTE base station"),
    GATEWAY("Gateway - Network protocol converter"),
    FIREWALL("Firewall - Network security device"),
    LOAD_BALANCER("Load Balancer - Traffic distribution device");

    private final String description;

    NetworkElementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
