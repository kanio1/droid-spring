package com.droid.bss.domain.asset;

import com.droid.bss.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

/**
 * Asset entity for tracking equipment and hardware inventory
 */
@Entity
@Table(name = "assets")
@Where(clause = "deleted_at IS NULL")
public class AssetEntity extends BaseEntity {

    @NotNull
    @Column(name = "asset_tag", unique = true, length = 100)
    private String assetTag;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", length = 50)
    private AssetType assetType;

    @NotNull
    @Column(length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "serial_number", length = 255, unique = true)
    private String serialNumber;

    @Column(name = "model_number", length = 255)
    private String modelNumber;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AssetStatus status;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_expiry")
    private LocalDate warrantyExpiry;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "assigned_to_type", length = 100)
    private String assignedToType; // CUSTOMER, LOCATION, DEPARTMENT

    @Column(name = "assigned_to_id", length = 36)
    private String assignedToId;

    @Column(name = "assigned_to_name", length = 255)
    private String assignedToName;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "cost_center", length = 100)
    private String costCenter;

    @Column(name = "notes", length = 2000)
    private String notes;

    public AssetEntity() {
    }

    public AssetEntity(String assetTag, AssetType assetType, String name, AssetStatus status) {
        this.assetTag = assetTag;
        this.assetType = assetType;
        this.name = name;
        this.status = status;
    }

    // Helper methods
    public boolean isAvailable() {
        return status == AssetStatus.AVAILABLE;
    }

    public boolean isInUse() {
        return status == AssetStatus.IN_USE;
    }

    public boolean isUnderWarranty() {
        if (warrantyExpiry == null) return false;
        return warrantyExpiry.isAfter(LocalDate.now());
    }

    public void assignTo(String type, String id, String name) {
        this.assignedToType = type;
        this.assignedToId = id;
        this.assignedToName = name;
        this.assignedDate = LocalDate.now();
        this.status = AssetStatus.IN_USE;
    }

    public void release() {
        this.assignedToType = null;
        this.assignedToId = null;
        this.assignedToName = null;
        this.assignedDate = null;
        this.status = AssetStatus.AVAILABLE;
    }

    // Getters and Setters
    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
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

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
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

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getWarrantyExpiry() {
        return warrantyExpiry;
    }

    public void setWarrantyExpiry(LocalDate warrantyExpiry) {
        this.warrantyExpiry = warrantyExpiry;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAssignedToType() {
        return assignedToType;
    }

    public void setAssignedToType(String assignedToType) {
        this.assignedToType = assignedToType;
    }

    public String getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
