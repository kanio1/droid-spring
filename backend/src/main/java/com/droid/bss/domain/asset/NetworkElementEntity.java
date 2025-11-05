package com.droid.bss.domain.asset;

import com.droid.bss.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * Network element entity for infrastructure tracking
 */
@Entity
@Table(name = "network_elements")
@Where(clause = "deleted_at IS NULL")
public class NetworkElementEntity extends BaseEntity {

    @NotNull
    @Column(name = "element_id", unique = true, length = 100)
    private String elementId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "element_type", length = 50)
    private NetworkElementType elementType;

    @NotNull
    @Column(length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "ip_address", length = 45) // IPv6 compatible
    private String ipAddress;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Column(name = "software_version", length = 100)
    private String softwareVersion;

    @Column(length = 100)
    private String location;

    @Column(name = "rack_position", length = 100)
    private String rackPosition;

    @Column(name = "port_count")
    private Integer portCount;

    @Column(name = "capacity", length = 255)
    private String capacity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AssetStatus status;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "operational_since")
    private LocalDateTime operationalSince;

    @Column(name = "maintenance_mode")
    private Boolean maintenanceMode = false;

    @Column(name = "maintenance_window_start")
    private LocalDateTime maintenanceWindowStart;

    @Column(name = "maintenance_window_end")
    private LocalDateTime maintenanceWindowEnd;

    public NetworkElementEntity() {
    }

    public NetworkElementEntity(String elementId, NetworkElementType elementType, String name, AssetStatus status) {
        this.elementId = elementId;
        this.elementType = elementType;
        this.name = name;
        this.status = status;
    }

    // Helper methods
    public boolean isOnline() {
        if (lastHeartbeat == null) return false;
        return lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    public boolean isInMaintenance() {
        return Boolean.TRUE.equals(maintenanceMode);
    }

    public boolean needsHeartbeat() {
        return lastHeartbeat == null || lastHeartbeat.isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    public void enterMaintenance() {
        this.maintenanceMode = true;
        this.maintenanceWindowStart = LocalDateTime.now();
    }

    public void exitMaintenance() {
        this.maintenanceMode = false;
        this.maintenanceWindowEnd = LocalDateTime.now();
    }

    // Getters and Setters
    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public NetworkElementType getElementType() {
        return elementType;
    }

    public void setElementType(NetworkElementType elementType) {
        this.elementType = elementType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRackPosition() {
        return rackPosition;
    }

    public void setRackPosition(String rackPosition) {
        this.rackPosition = rackPosition;
    }

    public Integer getPortCount() {
        return portCount;
    }

    public void setPortCount(Integer portCount) {
        this.portCount = portCount;
    }

    public String getCapacity() {
        return capacity;
    }

    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public LocalDateTime getOperationalSince() {
        return operationalSince;
    }

    public void setOperationalSince(LocalDateTime operationalSince) {
        this.operationalSince = operationalSince;
    }

    public Boolean getMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(Boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public LocalDateTime getMaintenanceWindowStart() {
        return maintenanceWindowStart;
    }

    public void setMaintenanceWindowStart(LocalDateTime maintenanceWindowStart) {
        this.maintenanceWindowStart = maintenanceWindowStart;
    }

    public LocalDateTime getMaintenanceWindowEnd() {
        return maintenanceWindowEnd;
    }

    public void setMaintenanceWindowEnd(LocalDateTime maintenanceWindowEnd) {
        this.maintenanceWindowEnd = maintenanceWindowEnd;
    }
}
