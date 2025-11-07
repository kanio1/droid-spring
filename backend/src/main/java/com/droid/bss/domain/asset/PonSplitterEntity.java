package com.droid.bss.domain.asset;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * PON Splitter entity for tracking optical splitters in the network
 */
@Entity
@Table(name = "pon_splitters")
public class PonSplitterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pon_network_id")
    private PonNetworkEntity ponNetwork;

    @NotNull
    @Column(name = "splitter_id", length = 100)
    private String splitterId;

    @Column(name = "splitter_name", length = 255)
    private String splitterName;

    @NotNull
    @Column(name = "splitter_ratio", length = 50)
    private String splitterRatio; // 1:2, 1:4, 1:8, 1:16, 1:32, 1:64

    @Column(name = "stage", length = 50)
    private String stage; // PRIMARY, SECONDARY

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "coordinates", length = 200)
    private String coordinates; // Lat, Long

    @Column(name = "installation_date")
    private LocalDateTime installationDate;

    @Column(name = "splitter_type", length = 100)
    private String splitterType; // PLC, FBT

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "serial_number", length = 255)
    private String serialNumber;

    @Column(name = "wavelength_range", length = 100)
    private String wavelengthRange;

    @Column(name = "insertion_loss_db")
    private Double insertionLossDb;

    @Column(name = "return_loss_db")
    private Double returnLossDb;

    @Column(name = "status", length = 50)
    private String status; // ACTIVE, INACTIVE, FAULT

    @Column(name = "ports_in_use")
    private Integer portsInUse = 0;

    @Column(name = "total_ports")
    private Integer totalPorts;

    @Column(name = "notes", length = 2000)
    private String notes;

    public PonSplitterEntity() {
    }

    public PonSplitterEntity(PonNetworkEntity ponNetwork, String splitterId, String splitterRatio, String stage) {
        this.ponNetwork = ponNetwork;
        this.splitterId = splitterId;
        this.splitterRatio = splitterRatio;
        this.stage = stage;
        this.status = "INACTIVE";
    }

    public int getAvailablePorts() {
        if (totalPorts == null) return 0;
        return totalPorts - (portsInUse != null ? portsInUse : 0);
    }

    public double getUtilizationPercentage() {
        if (totalPorts == null || totalPorts == 0) return 0.0;
        return (double) (portsInUse != null ? portsInUse : 0) / totalPorts * 100;
    }

    public void activatePort() {
        this.portsInUse = (this.portsInUse != null ? this.portsInUse : 0) + 1;
        if (this.portsInUse > 0) {
            this.status = "ACTIVE";
        }
    }

    public void deactivatePort() {
        if (this.portsInUse != null && this.portsInUse > 0) {
            this.portsInUse--;
            if (this.portsInUse == 0) {
                this.status = "INACTIVE";
            }
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PonNetworkEntity getPonNetwork() {
        return ponNetwork;
    }

    public void setPonNetwork(PonNetworkEntity ponNetwork) {
        this.ponNetwork = ponNetwork;
    }

    public String getSplitterId() {
        return splitterId;
    }

    public void setSplitterId(String splitterId) {
        this.splitterId = splitterId;
    }

    public String getSplitterName() {
        return splitterName;
    }

    public void setSplitterName(String splitterName) {
        this.splitterName = splitterName;
    }

    public String getSplitterRatio() {
        return splitterRatio;
    }

    public void setSplitterRatio(String splitterRatio) {
        this.splitterRatio = splitterRatio;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public LocalDateTime getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(LocalDateTime installationDate) {
        this.installationDate = installationDate;
    }

    public String getSplitterType() {
        return splitterType;
    }

    public void setSplitterType(String splitterType) {
        this.splitterType = splitterType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getWavelengthRange() {
        return wavelengthRange;
    }

    public void setWavelengthRange(String wavelengthRange) {
        this.wavelengthRange = wavelengthRange;
    }

    public Double getInsertionLossDb() {
        return insertionLossDb;
    }

    public void setInsertionLossDb(Double insertionLossDb) {
        this.insertionLossDb = insertionLossDb;
    }

    public Double getReturnLossDb() {
        return returnLossDb;
    }

    public void setReturnLossDb(Double returnLossDb) {
        this.returnLossDb = returnLossDb;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPortsInUse() {
        return portsInUse;
    }

    public void setPortsInUse(Integer portsInUse) {
        this.portsInUse = portsInUse;
    }

    public Integer getTotalPorts() {
        return totalPorts;
    }

    public void setTotalPorts(Integer totalPorts) {
        this.totalPorts = totalPorts;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
