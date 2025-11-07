package com.droid.bss.domain.asset;

import com.droid.bss.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Optical Network Unit (OUN) entity for multi-dwelling unit equipment tracking
 * Used for fiber optic networks serving multiple customers in apartment buildings
 */
@Entity
@Table(name = "oun_units")
@Where(clause = "deleted_at IS NULL")
public class OunEntity extends BaseEntity {

    @NotNull
    @Column(name = "oun_id", unique = true, length = 100)
    private String ounId;

    @NotNull
    @Column(length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull
    @Column(name = "model_number", length = 255)
    private String modelNumber;

    @Column(name = "serial_number", length = 255, unique = true)
    private String serialNumber;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AssetStatus status;

    // OUN-specific properties
    @Column(name = "port_count")
    private Integer portCount;

    @Column(name = "active_ports")
    private Integer activePorts = 0;

    @Column(name = "gpon_technology", length = 100)
    private String gponTechnology; // GPON, XGS-PON, NG-PON2

    @Column(name = "max_distance_km")
    private Double maxDistanceKm;

    @Column(name = "optical_wavelength")
    private String opticalWavelength; // 1310nm, 1490nm, 1550nm

    @Column(name = "splitter_ratio", length = 50)
    private String splitterRatio; // 1:8, 1:16, 1:32, 1:64

    // Network topology
    @Column(name = "olt_device_id", length = 100)
    private String oltDeviceId;

    @Column(name = "olt_port", length = 100)
    private String oltPort;

    @Column(name = "fiber_cable_id", length = 100)
    private String fiberCableId;

    @Column(name = "connected_to_olt", columnDefinition = "boolean default false")
    private Boolean connectedToOlt = false;

    @Column(name = "connection_status", length = 50)
    private String connectionStatus; // CONNECTED, DISCONNECTED, FAULT

    // Location and physical info
    @Column(name = "building_name", length = 255)
    private String buildingName;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "room_number", length = 100)
    private String roomNumber;

    @Column(name = "location_coordinates", length = 1000)
    private String locationCoordinates; // Lat, Long

    @Column(name = "address", length = 1000)
    private String address;

    // Performance metrics
    @Column(name = "uptime_hours")
    private Long uptimeHours = 0L;

    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @Column(name = "next_maintenance_due")
    private LocalDateTime nextMaintenanceDue;

    @Column(name = "optical_power_dbm")
    private Double opticalPowerDbm; // Optical power in dBm

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Column(name = "software_version", length = 100)
    private String softwareVersion;

    // Customer service
    @Column(name = "served_customers")
    private Integer servedCustomers = 0;

    @Column(name = "max_customers")
    private Integer maxCustomers;

    @Column(name = "service_area_description", length = 1000)
    private String serviceAreaDescription;

    // Notes
    @Column(name = "notes", length = 2000)
    private String notes;

    // Active customer connections (if needed for tracking)
    @OneToMany(mappedBy = "ounUnit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OunCustomerPort> customerPorts = new ArrayList<>();

    public OunEntity() {
    }

    public OunEntity(String ounId, String name, String modelNumber, AssetStatus status) {
        this.ounId = ounId;
        this.name = name;
        this.modelNumber = modelNumber;
        this.status = status;
    }

    // Helper methods
    public boolean isOnline() {
        return status == AssetStatus.IN_USE && Boolean.TRUE.equals(connectedToOlt);
    }

    public boolean isInMaintenance() {
        return status == AssetStatus.MAINTENANCE;
    }

    public boolean isAvailableCapacity() {
        if (maxCustomers == null) return true;
        return servedCustomers < maxCustomers;
    }

    public double getUtilizationPercentage() {
        if (maxCustomers == null || maxCustomers == 0) return 0.0;
        return (double) servedCustomers / maxCustomers * 100;
    }

    public void connectToOlt(String oltDeviceId, String oltPort) {
        this.oltDeviceId = oltDeviceId;
        this.oltPort = oltPort;
        this.connectedToOlt = true;
        this.connectionStatus = "CONNECTED";
    }

    public void disconnectFromOlt() {
        this.oltDeviceId = null;
        this.oltPort = null;
        this.connectedToOlt = false;
        this.connectionStatus = "DISCONNECTED";
    }

    public void addCustomerPort(Integer portNumber) {
        // Implementation would create new OunCustomerPort
        this.activePorts = (this.activePorts != null ? this.activePorts : 0) + 1;
    }

    public void updateOpticalPower(Double power) {
        this.opticalPowerDbm = power;
        // Alert if power is below threshold (e.g., -28 dBm for GPON)
    }

    public void recordMaintenance() {
        this.lastMaintenance = LocalDateTime.now();
        // Calculate next maintenance date (e.g., 6 months later)
        if (this.nextMaintenanceDue == null) {
            this.nextMaintenanceDue = LocalDateTime.now().plusMonths(6);
        }
    }

    public void updateUptime() {
        this.uptimeHours = (this.uptimeHours != null ? this.uptimeHours : 0L) + 1;
    }

    // Getters and Setters
    public String getOunId() {
        return ounId;
    }

    public void setOunId(String ounId) {
        this.ounId = ounId;
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

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public Integer getPortCount() {
        return portCount;
    }

    public void setPortCount(Integer portCount) {
        this.portCount = portCount;
    }

    public Integer getActivePorts() {
        return activePorts;
    }

    public void setActivePorts(Integer activePorts) {
        this.activePorts = activePorts;
    }

    public String getGponTechnology() {
        return gponTechnology;
    }

    public void setGponTechnology(String gponTechnology) {
        this.gponTechnology = gponTechnology;
    }

    public Double getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public void setMaxDistanceKm(Double maxDistanceKm) {
        this.maxDistanceKm = maxDistanceKm;
    }

    public String getOpticalWavelength() {
        return opticalWavelength;
    }

    public void setOpticalWavelength(String opticalWavelength) {
        this.opticalWavelength = opticalWavelength;
    }

    public String getSplitterRatio() {
        return splitterRatio;
    }

    public void setSplitterRatio(String splitterRatio) {
        this.splitterRatio = splitterRatio;
    }

    public String getOltDeviceId() {
        return oltDeviceId;
    }

    public void setOltDeviceId(String oltDeviceId) {
        this.oltDeviceId = oltDeviceId;
    }

    public String getOltPort() {
        return oltPort;
    }

    public void setOltPort(String oltPort) {
        this.oltPort = oltPort;
    }

    public String getFiberCableId() {
        return fiberCableId;
    }

    public void setFiberCableId(String fiberCableId) {
        this.fiberCableId = fiberCableId;
    }

    public Boolean getConnectedToOlt() {
        return connectedToOlt;
    }

    public void setConnectedToOlt(Boolean connectedToOlt) {
        this.connectedToOlt = connectedToOlt;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getLocationCoordinates() {
        return locationCoordinates;
    }

    public void setLocationCoordinates(String locationCoordinates) {
        this.locationCoordinates = locationCoordinates;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getUptimeHours() {
        return uptimeHours;
    }

    public void setUptimeHours(Long uptimeHours) {
        this.uptimeHours = uptimeHours;
    }

    public LocalDateTime getLastMaintenance() {
        return lastMaintenance;
    }

    public void setLastMaintenance(LocalDateTime lastMaintenance) {
        this.lastMaintenance = lastMaintenance;
    }

    public LocalDateTime getNextMaintenanceDue() {
        return nextMaintenanceDue;
    }

    public void setNextMaintenanceDue(LocalDateTime nextMaintenanceDue) {
        this.nextMaintenanceDue = nextMaintenanceDue;
    }

    public Double getOpticalPowerDbm() {
        return opticalPowerDbm;
    }

    public void setOpticalPowerDbm(Double opticalPowerDbm) {
        this.opticalPowerDbm = opticalPowerDbm;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
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

    public Integer getServedCustomers() {
        return servedCustomers;
    }

    public void setServedCustomers(Integer servedCustomers) {
        this.servedCustomers = servedCustomers;
    }

    public Integer getMaxCustomers() {
        return maxCustomers;
    }

    public void setMaxCustomers(Integer maxCustomers) {
        this.maxCustomers = maxCustomers;
    }

    public String getServiceAreaDescription() {
        return serviceAreaDescription;
    }

    public void setServiceAreaDescription(String serviceAreaDescription) {
        this.serviceAreaDescription = serviceAreaDescription;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<OunCustomerPort> getCustomerPorts() {
        return customerPorts;
    }

    public void setCustomerPorts(List<OunCustomerPort> customerPorts) {
        this.customerPorts = customerPorts;
    }
}
