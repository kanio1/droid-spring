package com.droid.bss.domain.asset;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * PON Optical Closure entity for tracking fiber distribution closures
 */
@Entity
@Table(name = "pon_optical_closures")
public class PonOpticalClosureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pon_network_id")
    private PonNetworkEntity ponNetwork;

    @NotNull
    @Column(name = "closure_id", length = 100)
    private String closureId;

    @Column(name = "closure_name", length = 255)
    private String closureName;

    @NotNull
    @Column(name = "closure_type", length = 100)
    private String closureType; // SPLICE_CLOSURE, DISTRIBUTION_CLOSURE, TERMINATION_CLOSURE

    @Column(length = 500)
    private String location;

    @Column(name = "coordinates", length = 200)
    private String coordinates;

    @Column(name = "installation_date")
    private LocalDateTime installationDate;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "serial_number", length = 255)
    private String serialNumber;

    @Column(name = "ip_rating", length = 50)
    private String ipRating; // IP65, IP68, etc.

    @Column(name = "capacity_fibers")
    private Integer capacityFibers;

    @Column(name = "fibers_in_use")
    private Integer fibersInUse = 0;

    @Column(name = "splices_count")
    private Integer splicesCount = 0;

    @Column(name = "status", length = 50)
    private String status; // ACTIVE, INACTIVE, MAINTENANCE

    @Column(name = "last_inspection")
    private LocalDateTime lastInspection;

    @Column(name = "next_inspection_due")
    private LocalDateTime nextInspectionDue;

    @Column(name = "environmental_conditions", length = 1000)
    private String environmentalConditions;

    @Column(name = "notes", length = 2000)
    private String notes;

    public PonOpticalClosureEntity() {
    }

    public PonOpticalClosureEntity(PonNetworkEntity ponNetwork, String closureId, String closureType) {
        this.ponNetwork = ponNetwork;
        this.closureId = closureId;
        this.closureType = closureType;
        this.status = "INACTIVE";
    }

    public int getAvailableCapacity() {
        if (capacityFibers == null) return 0;
        return capacityFibers - (fibersInUse != null ? fibersInUse : 0);
    }

    public double getUtilizationPercentage() {
        if (capacityFibers == null || capacityFibers == 0) return 0.0;
        return (double) (fibersInUse != null ? fibersInUse : 0) / capacityFibers * 100;
    }

    public void addFiber() {
        this.fibersInUse = (this.fibersInUse != null ? this.fibersInUse : 0) + 1;
        this.splicesCount = (this.splicesCount != null ? this.splicesCount : 0) + 1;
        if (this.fibersInUse > 0) {
            this.status = "ACTIVE";
        }
    }

    public void removeFiber() {
        if (this.fibersInUse != null && this.fibersInUse > 0) {
            this.fibersInUse--;
            this.splicesCount--;
            if (this.fibersInUse == 0) {
                this.status = "INACTIVE";
            }
        }
    }

    public void scheduleInspection() {
        this.lastInspection = LocalDateTime.now();
        this.nextInspectionDue = LocalDateTime.now().plusMonths(12); // Annual inspection
    }

    public boolean needsInspection() {
        if (nextInspectionDue == null) return false;
        return nextInspectionDue.isBefore(LocalDateTime.now());
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

    public String getClosureId() {
        return closureId;
    }

    public void setClosureId(String closureId) {
        this.closureId = closureId;
    }

    public String getClosureName() {
        return closureName;
    }

    public void setClosureName(String closureName) {
        this.closureName = closureName;
    }

    public String getClosureType() {
        return closureType;
    }

    public void setClosureType(String closureType) {
        this.closureType = closureType;
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

    public String getIpRating() {
        return ipRating;
    }

    public void setIpRating(String ipRating) {
        this.ipRating = ipRating;
    }

    public Integer getCapacityFibers() {
        return capacityFibers;
    }

    public void setCapacityFibers(Integer capacityFibers) {
        this.capacityFibers = capacityFibers;
    }

    public Integer getFibersInUse() {
        return fibersInUse;
    }

    public void setFibersInUse(Integer fibersInUse) {
        this.fibersInUse = fibersInUse;
    }

    public Integer getSplicesCount() {
        return splicesCount;
    }

    public void setSplicesCount(Integer splicesCount) {
        this.splicesCount = splicesCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastInspection() {
        return lastInspection;
    }

    public void setLastInspection(LocalDateTime lastInspection) {
        this.lastInspection = lastInspection;
    }

    public LocalDateTime getNextInspectionDue() {
        return nextInspectionDue;
    }

    public void setNextInspectionDue(LocalDateTime nextInspectionDue) {
        this.nextInspectionDue = nextInspectionDue;
    }

    public String getEnvironmentalConditions() {
        return environmentalConditions;
    }

    public void setEnvironmentalConditions(String environmentalConditions) {
        this.environmentalConditions = environmentalConditions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
