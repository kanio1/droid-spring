package com.droid.bss.domain.asset;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PON Fiber Segment entity for tracking individual fiber paths in the network
 */
@Entity
@Table(name = "pon_fiber_segments")
public class PonSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pon_network_id")
    private PonNetworkEntity ponNetwork;

    @NotNull
    @Column(name = "segment_id", length = 100)
    private String segmentId;

    @Column(name = "segment_name", length = 255)
    private String segmentName;

    @NotNull
    @Column(name = "fiber_type", length = 100)
    private String fiberType; // G.652, G.657, G.655

    @NotNull
    @Column(name = "segment_type", length = 100)
    private String segmentType; // FEEDER, DISTRIBUTION, DROP

    @Column(name = "length_km", precision = 10, scale = 3)
    private BigDecimal lengthKm;

    @Column(name = "fiber_count")
    private Integer fiberCount;

    @Column(name = "fibers_in_use")
    private Integer fibersInUse = 0;

    @Column(name = "from_location", length = 500)
    private String fromLocation;

    @Column(name = "to_location", length = 500)
    private String toLocation;

    @Column(name = "from_coordinates", length = 200)
    private String fromCoordinates;

    @Column(name = "to_coordinates", length = 200)
    private String toCoordinates;

    @Column(name = "installation_date")
    private LocalDateTime installationDate;

    @Column(name = "attenuation_db_per_km", precision = 5, scale = 3)
    private BigDecimal attenuationDbPerKm;

    @Column(name = "total_attenuation_db", precision = 5, scale = 3)
    private BigDecimal totalAttenuationDb;

    @Column(name = "status", length = 50)
    private String status; // ACTIVE, INACTIVE, FAULT, MAINTENANCE

    @Column(name = "last_tested")
    private LocalDateTime lastTested;

    @Column(name = "test_results", length = 1000)
    private String testResults;

    @Column(name = "contractor", length = 255)
    private String contractor;

    @Column(name = "warranty_expiry")
    private LocalDateTime warrantyExpiry;

    @Column(name = "notes", length = 2000)
    private String notes;

    public PonSegmentEntity() {
    }

    public PonSegmentEntity(PonNetworkEntity ponNetwork, String segmentId, String fiberType, String segmentType) {
        this.ponNetwork = ponNetwork;
        this.segmentId = segmentId;
        this.fiberType = fiberType;
        this.segmentType = segmentType;
        this.status = "INACTIVE";
    }

    public void calculateAttenuation() {
        if (lengthKm != null && attenuationDbPerKm != null) {
            this.totalAttenuationDb = lengthKm.multiply(attenuationDbPerKm);
        }
    }

    public void addFiber() {
        this.fibersInUse = (this.fibersInUse != null ? this.fibersInUse : 0) + 1;
        if (this.fibersInUse > 0) {
            this.status = "ACTIVE";
        }
    }

    public void removeFiber() {
        if (this.fibersInUse != null && this.fibersInUse > 0) {
            this.fibersInUse--;
            if (this.fibersInUse == 0) {
                this.status = "INACTIVE";
            }
        }
    }

    public boolean isUnderWarranty() {
        if (warrantyExpiry == null) return false;
        return warrantyExpiry.isAfter(LocalDateTime.now());
    }

    public boolean needsTesting() {
        if (lastTested == null) return true;
        return lastTested.isBefore(LocalDateTime.now().minusMonths(12));
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

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public String getFiberType() {
        return fiberType;
    }

    public void setFiberType(String fiberType) {
        this.fiberType = fiberType;
    }

    public String getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(String segmentType) {
        this.segmentType = segmentType;
    }

    public BigDecimal getLengthKm() {
        return lengthKm;
    }

    public void setLengthKm(BigDecimal lengthKm) {
        this.lengthKm = lengthKm;
    }

    public Integer getFiberCount() {
        return fiberCount;
    }

    public void setFiberCount(Integer fiberCount) {
        this.fiberCount = fiberCount;
    }

    public Integer getFibersInUse() {
        return fibersInUse;
    }

    public void setFibersInUse(Integer fibersInUse) {
        this.fibersInUse = fibersInUse;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getFromCoordinates() {
        return fromCoordinates;
    }

    public void setFromCoordinates(String fromCoordinates) {
        this.fromCoordinates = fromCoordinates;
    }

    public String getToCoordinates() {
        return toCoordinates;
    }

    public void setToCoordinates(String toCoordinates) {
        this.toCoordinates = toCoordinates;
    }

    public LocalDateTime getInstallationDate() {
        return installationDate;
    }

    public void setInstallationDate(LocalDateTime installationDate) {
        this.installationDate = installationDate;
    }

    public BigDecimal getAttenuationDbPerKm() {
        return attenuationDbPerKm;
    }

    public void setAttenuationDbPerKm(BigDecimal attenuationDbPerKm) {
        this.attenuationDbPerKm = attenuationDbPerKm;
        calculateAttenuation();
    }

    public BigDecimal getTotalAttenuationDb() {
        return totalAttenuationDb;
    }

    public void setTotalAttenuationDb(BigDecimal totalAttenuationDb) {
        this.totalAttenuationDb = totalAttenuationDb;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastTested() {
        return lastTested;
    }

    public void setLastTested(LocalDateTime lastTested) {
        this.lastTested = lastTested;
    }

    public String getTestResults() {
        return testResults;
    }

    public void setTestResults(String testResults) {
        this.testResults = testResults;
    }

    public String getContractor() {
        return contractor;
    }

    public void setContractor(String contractor) {
        this.contractor = contractor;
    }

    public LocalDateTime getWarrantyExpiry() {
        return warrantyExpiry;
    }

    public void setWarrantyExpiry(LocalDateTime warrantyExpiry) {
        this.warrantyExpiry = warrantyExpiry;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
