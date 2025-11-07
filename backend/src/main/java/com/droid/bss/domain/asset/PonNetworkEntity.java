package com.droid.bss.domain.asset;

import com.droid.bss.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Passive Optical Network (PON) entity for tracking GPON/XGS-PON infrastructure
 * Represents the entire PON network from OLT to splitters to customer premises
 */
@Entity
@Table(name = "pon_networks")
@Where(clause = "deleted_at IS NULL")
public class PonNetworkEntity extends BaseEntity {

    @NotNull
    @Column(name = "network_id", unique = true, length = 100)
    private String networkId;

    @NotNull
    @Column(length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull
    @Column(length = 50)
    private String ponType; // GPON, XGS-PON, NG-PON2

    @NotNull
    @Column(length = 50)
    private String status; // PLANNED, DEPLOYED, ACTIVE, MAINTENANCE, DECOMMISSIONED

    // Network topology
    @Column(name = "olt_device_id", length = 100)
    private String oltDeviceId;

    @Column(name = "olt_port", length = 100)
    private String oltPort;

    @Column(name = "olt_slot", length = 100)
    private String oltSlot;

    @Column(name = "olt_interface", length = 100)
    private String oltInterface; // e.g., G-0/0/0

    // Splitter configuration
    @Column(name = "first_splitter_id", length = 100)
    private String firstSplitterId;

    @Column(name = "first_splitter_ratio", length = 50)
    private String firstSplitterRatio; // 1:2, 1:4, 1:8

    @Column(name = "second_splitter_id", length = 100)
    private String secondSplitterId;

    @Column(name = "second_splitter_ratio", length = 50)
    private String secondSplitterRatio; // 1:2, 1:4, 1:8

    // Path and distance
    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "fiber_cable_type", length = 100)
    private String fiberCableType; // G.652, G.657, etc.

    @Column(name = "optical_wavelength", length = 100)
    private String opticalWavelength; // 1310nm, 1490nm, 1550nm

    // Coverage area
    @Column(name = "service_area_name", length = 255)
    private String serviceAreaName;

    @Column(name = "coverage_area_coordinates", length = 2000)
    private String coverageAreaCoordinates; // GeoJSON or coordinates list

    @Column(name = "service_area_description", length = 2000)
    private String serviceAreaDescription;

    @Column(name = "zip_codes_served", length = 1000)
    private String zipCodesServed; // Comma-separated list

    @Column(name = "districts_served", length = 1000)
    private String districtsServed; // Areas/districts covered

    // Customer capacity
    @Column(name = "max_customers_theoretical")
    private Integer maxCustomersTheoretical;

    @Column(name = "max_customers_physical")
    private Integer maxCustomersPhysical;

    @Column(name = "active_customers")
    private Integer activeCustomers = 0;

    @Column(name = "available_ports")
    private Integer availablePorts = 0;

    // Performance metrics
    @Column(name = "average_signal_power_dbm")
    private Double averageSignalPowerDbm;

    @Column(name = "min_signal_power_dbm")
    private Double minSignalPowerDbm;

    @Column(name = "max_signal_power_dbm")
    private Double maxSignalPowerDbm;

    @Column(name = "optical_budget_db")
    private Double opticalBudgetDb;

    @Column(name = "attenuation_db")
    private Double attenuationDb;

    // Timing
    @Column(name = "deployment_date")
    private LocalDateTime deploymentDate;

    @Column(name = "activation_date")
    private LocalDateTime activationDate;

    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @Column(name = "next_maintenance_due")
    private LocalDateTime nextMaintenanceDue;

    // Equipment list
    @Column(name = "olt_device_name", length = 255)
    private String oltDeviceName;

    @Column(name = "splitter_count")
    private Integer splitterCount = 0;

    @Column(name = "fiber_splice_count")
    private Integer fiberSpliceCount = 0;

    @Column(name = "closure_count")
    private Integer closureCount = 0; // Fiber distribution closures

    // Capacity planning
    @Column(name = "upgrade_date_planned")
    private LocalDateTime upgradeDatePlanned;

    @Column(name = "upgrade_type_planned", length = 100)
    private String upgradeTypePlanned; // GPON->XGS-PON, capacity increase, etc.

    @Column(name = "utilization_percentage")
    private Double utilizationPercentage;

    // Notes
    @Column(name = "technical_notes", length = 4000)
    private String technicalNotes;

    @Column(name = "contractor_notes", length = 2000)
    private String contractorNotes;

    // Associated network elements and OUNs
    @OneToMany(mappedBy = "ponNetwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PonSplitterEntity> splitters = new ArrayList<>();

    @OneToMany(mappedBy = "ponNetwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PonOpticalClosureEntity> closures = new ArrayList<>();

    @OneToMany(mappedBy = "ponNetwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PonSegmentEntity> fiberSegments = new ArrayList<>();

    public PonNetworkEntity() {
    }

    public PonNetworkEntity(String networkId, String name, String ponType, String status) {
        this.networkId = networkId;
        this.name = name;
        this.ponType = ponType;
        this.status = status;
    }

    // Helper methods
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isAtCapacity() {
        if (maxCustomersPhysical == null) return false;
        return activeCustomers >= maxCustomersPhysical;
    }

    public boolean isUpgradeNeeded() {
        if (utilizationPercentage == null) return false;
        return utilizationPercentage > 80.0;
    }

    public int getAvailableCapacity() {
        if (maxCustomersPhysical == null) return 0;
        return maxCustomersPhysical - activeCustomers;
    }

    public void activateNetwork() {
        this.status = "ACTIVE";
        this.activationDate = LocalDateTime.now();
    }

    public void addSplitter(String splitterId, String ratio, Integer position) {
        // Implementation for adding splitters to the network
    }

    public void updateSignalPower(double avg, double min, double max) {
        this.averageSignalPowerDbm = avg;
        this.minSignalPowerDbm = min;
        this.maxSignalPowerDbm = max;
    }

    public void calculateUtilization() {
        if (maxCustomersPhysical != null && maxCustomersPhysical > 0) {
            this.utilizationPercentage = (double) activeCustomers / maxCustomersPhysical * 100;
        }
    }

    public void recordMaintenance() {
        this.lastMaintenance = LocalDateTime.now();
        // Next maintenance in 6 months
        this.nextMaintenanceDue = LocalDateTime.now().plusMonths(6);
    }

    public boolean needsMaintenance() {
        if (nextMaintenanceDue == null) return false;
        return nextMaintenanceDue.isBefore(LocalDateTime.now());
    }

    public String getFullPath() {
        StringBuilder path = new StringBuilder();
        if (oltDeviceName != null) path.append(oltDeviceName);
        if (oltPort != null) path.append(":").append(oltPort);
        if (firstSplitterId != null) path.append(" -> ").append(firstSplitterId);
        if (secondSplitterId != null) path.append(" -> ").append(secondSplitterId);
        return path.toString();
    }

    // Getters and Setters
    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
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

    public String getPonType() {
        return ponType;
    }

    public void setPonType(String ponType) {
        this.ponType = ponType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getOltSlot() {
        return oltSlot;
    }

    public void setOltSlot(String oltSlot) {
        this.oltSlot = oltSlot;
    }

    public String getOltInterface() {
        return oltInterface;
    }

    public void setOltInterface(String oltInterface) {
        this.oltInterface = oltInterface;
    }

    public String getFirstSplitterId() {
        return firstSplitterId;
    }

    public void setFirstSplitterId(String firstSplitterId) {
        this.firstSplitterId = firstSplitterId;
    }

    public String getFirstSplitterRatio() {
        return firstSplitterRatio;
    }

    public void setFirstSplitterRatio(String firstSplitterRatio) {
        this.firstSplitterRatio = firstSplitterRatio;
    }

    public String getSecondSplitterId() {
        return secondSplitterId;
    }

    public void setSecondSplitterId(String secondSplitterId) {
        this.secondSplitterId = secondSplitterId;
    }

    public String getSecondSplitterRatio() {
        return secondSplitterRatio;
    }

    public void setSecondSplitterRatio(String secondSplitterRatio) {
        this.secondSplitterRatio = secondSplitterRatio;
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public String getFiberCableType() {
        return fiberCableType;
    }

    public void setFiberCableType(String fiberCableType) {
        this.fiberCableType = fiberCableType;
    }

    public String getOpticalWavelength() {
        return opticalWavelength;
    }

    public void setOpticalWavelength(String opticalWavelength) {
        this.opticalWavelength = opticalWavelength;
    }

    public String getServiceAreaName() {
        return serviceAreaName;
    }

    public void setServiceAreaName(String serviceAreaName) {
        this.serviceAreaName = serviceAreaName;
    }

    public String getCoverageAreaCoordinates() {
        return coverageAreaCoordinates;
    }

    public void setCoverageAreaCoordinates(String coverageAreaCoordinates) {
        this.coverageAreaCoordinates = coverageAreaCoordinates;
    }

    public String getServiceAreaDescription() {
        return serviceAreaDescription;
    }

    public void setServiceAreaDescription(String serviceAreaDescription) {
        this.serviceAreaDescription = serviceAreaDescription;
    }

    public String getZipCodesServed() {
        return zipCodesServed;
    }

    public void setZipCodesServed(String zipCodesServed) {
        this.zipCodesServed = zipCodesServed;
    }

    public String getDistrictsServed() {
        return districtsServed;
    }

    public void setDistrictsServed(String districtsServed) {
        this.districtsServed = districtsServed;
    }

    public Integer getMaxCustomersTheoretical() {
        return maxCustomersTheoretical;
    }

    public void setMaxCustomersTheoretical(Integer maxCustomersTheoretical) {
        this.maxCustomersTheoretical = maxCustomersTheoretical;
    }

    public Integer getMaxCustomersPhysical() {
        return maxCustomersPhysical;
    }

    public void setMaxCustomersPhysical(Integer maxCustomersPhysical) {
        this.maxCustomersPhysical = maxCustomersPhysical;
    }

    public Integer getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(Integer activeCustomers) {
        this.activeCustomers = activeCustomers;
        calculateUtilization();
    }

    public Integer getAvailablePorts() {
        return availablePorts;
    }

    public void setAvailablePorts(Integer availablePorts) {
        this.availablePorts = availablePorts;
    }

    public Double getAverageSignalPowerDbm() {
        return averageSignalPowerDbm;
    }

    public void setAverageSignalPowerDbm(Double averageSignalPowerDbm) {
        this.averageSignalPowerDbm = averageSignalPowerDbm;
    }

    public Double getMinSignalPowerDbm() {
        return minSignalPowerDbm;
    }

    public void setMinSignalPowerDbm(Double minSignalPowerDbm) {
        this.minSignalPowerDbm = minSignalPowerDbm;
    }

    public Double getMaxSignalPowerDbm() {
        return maxSignalPowerDbm;
    }

    public void setMaxSignalPowerDbm(Double maxSignalPowerDbm) {
        this.maxSignalPowerDbm = maxSignalPowerDbm;
    }

    public Double getOpticalBudgetDb() {
        return opticalBudgetDb;
    }

    public void setOpticalBudgetDb(Double opticalBudgetDb) {
        this.opticalBudgetDb = opticalBudgetDb;
    }

    public Double getAttenuationDb() {
        return attenuationDb;
    }

    public void setAttenuationDb(Double attenuationDb) {
        this.attenuationDb = attenuationDb;
    }

    public LocalDateTime getDeploymentDate() {
        return deploymentDate;
    }

    public void setDeploymentDate(LocalDateTime deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    public LocalDateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDateTime activationDate) {
        this.activationDate = activationDate;
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

    public String getOltDeviceName() {
        return oltDeviceName;
    }

    public void setOltDeviceName(String oltDeviceName) {
        this.oltDeviceName = oltDeviceName;
    }

    public Integer getSplitterCount() {
        return splitterCount;
    }

    public void setSplitterCount(Integer splitterCount) {
        this.splitterCount = splitterCount;
    }

    public Integer getFiberSpliceCount() {
        return fiberSpliceCount;
    }

    public void setFiberSpliceCount(Integer fiberSpliceCount) {
        this.fiberSpliceCount = fiberSpliceCount;
    }

    public Integer getClosureCount() {
        return closureCount;
    }

    public void setClosureCount(Integer closureCount) {
        this.closureCount = closureCount;
    }

    public LocalDateTime getUpgradeDatePlanned() {
        return upgradeDatePlanned;
    }

    public void setUpgradeDatePlanned(LocalDateTime upgradeDatePlanned) {
        this.upgradeDatePlanned = upgradeDatePlanned;
    }

    public String getUpgradeTypePlanned() {
        return upgradeTypePlanned;
    }

    public void setUpgradeTypePlanned(String upgradeTypePlanned) {
        this.upgradeTypePlanned = upgradeTypePlanned;
    }

    public Double getUtilizationPercentage() {
        return utilizationPercentage;
    }

    public void setUtilizationPercentage(Double utilizationPercentage) {
        this.utilizationPercentage = utilizationPercentage;
    }

    public String getTechnicalNotes() {
        return technicalNotes;
    }

    public void setTechnicalNotes(String technicalNotes) {
        this.technicalNotes = technicalNotes;
    }

    public String getContractorNotes() {
        return contractorNotes;
    }

    public void setContractorNotes(String contractorNotes) {
        this.contractorNotes = contractorNotes;
    }

    public List<PonSplitterEntity> getSplitters() {
        return splitters;
    }

    public void setSplitters(List<PonSplitterEntity> splitters) {
        this.splitters = splitters;
    }

    public List<PonOpticalClosureEntity> getClosures() {
        return closures;
    }

    public void setClosures(List<PonOpticalClosureEntity> closures) {
        this.closures = closures;
    }

    public List<PonSegmentEntity> getFiberSegments() {
        return fiberSegments;
    }

    public void setFiberSegments(List<PonSegmentEntity> fiberSegments) {
        this.fiberSegments = fiberSegments;
    }
}
