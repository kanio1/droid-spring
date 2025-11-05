package com.droid.bss.application.dto.asset;

import com.droid.bss.domain.asset.NetworkElementEntity;
import com.droid.bss.domain.asset.NetworkElementType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for network element data
 */
public record NetworkElementResponse(
        UUID id,
        String elementId,
        String elementType,
        String name,
        String description,
        String ipAddress,
        String macAddress,
        String firmwareVersion,
        String softwareVersion,
        String location,
        String rackPosition,
        Integer portCount,
        String capacity,
        String status,
        LocalDateTime lastHeartbeat,
        LocalDateTime operationalSince,
        Boolean maintenanceMode
) {

    public static NetworkElementResponse from(NetworkElementEntity element) {
        return new NetworkElementResponse(
                element.getId() != null ? element.getId() : null,
                element.getElementId(),
                element.getElementType().name(),
                element.getName(),
                element.getDescription(),
                element.getIpAddress(),
                element.getMacAddress(),
                element.getFirmwareVersion(),
                element.getSoftwareVersion(),
                element.getLocation(),
                element.getRackPosition(),
                element.getPortCount(),
                element.getCapacity(),
                element.getStatus().name(),
                element.getLastHeartbeat(),
                element.getOperationalSince(),
                element.getMaintenanceMode()
        );
    }
}
