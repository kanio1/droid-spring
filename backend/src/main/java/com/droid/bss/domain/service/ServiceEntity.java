package com.droid.bss.domain.service;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a service in the BSS system
 * Examples: Internet, Telephony, TV, Cloud Services
 */
@Entity
@Table(name = "services")
public class ServiceEntity extends BaseEntity {

    @NotNull
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String serviceCode;

    @NotNull
    @Size(max = 200)
    @Column(nullable = false)
    private String name;

    @Size(max = 1000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status;

    @Size(max = 100)
    private String category;

    @Column(name = "provisioning_time_minutes")
    private Integer provisioningTimeMinutes;

    @Column(name = "deprovisioning_time_minutes")
    private Integer deprovisioningTimeMinutes;

    @ElementCollection
    @CollectionTable(
        name = "service_dependencies",
        joinColumns = @JoinColumn(name = "service_id")
    )
    @Column(name = "depends_on_service_id")
    private Set<String> dependsOnServiceCodes = new HashSet<>();

    @Column(name = "requires_equipment")
    private Boolean requiresEquipment = false;

    @Column(name = "auto_provision")
    private Boolean autoProvision = true;

    @Column(name = "provisioning_script")
    @Size(max = 500)
    private String provisioningScript;

    protected ServiceEntity() {
    }

    public ServiceEntity(
            String serviceCode,
            String name,
            String description,
            ServiceType serviceType,
            ServiceStatus status,
            String category) {
        this.serviceCode = serviceCode;
        this.name = name;
        this.description = description;
        this.serviceType = serviceType;
        this.status = status;
        this.category = category;
    }

    // Getters and Setters
    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
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

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getProvisioningTimeMinutes() {
        return provisioningTimeMinutes;
    }

    public void setProvisioningTimeMinutes(Integer provisioningTimeMinutes) {
        this.provisioningTimeMinutes = provisioningTimeMinutes;
    }

    public Integer getDeprovisioningTimeMinutes() {
        return deprovisioningTimeMinutes;
    }

    public void setDeprovisioningTimeMinutes(Integer deprovisioningTimeMinutes) {
        this.deprovisioningTimeMinutes = deprovisioningTimeMinutes;
    }

    public Set<String> getDependsOnServiceCodes() {
        return dependsOnServiceCodes;
    }

    public void setDependsOnServiceCodes(Set<String> dependsOnServiceCodes) {
        this.dependsOnServiceCodes = dependsOnServiceCodes;
    }

    public Boolean getRequiresEquipment() {
        return requiresEquipment;
    }

    public void setRequiresEquipment(Boolean requiresEquipment) {
        this.requiresEquipment = requiresEquipment;
    }

    public Boolean getAutoProvision() {
        return autoProvision;
    }

    public void setAutoProvision(Boolean autoProvision) {
        this.autoProvision = autoProvision;
    }

    public String getProvisioningScript() {
        return provisioningScript;
    }

    public void setProvisioningScript(String provisioningScript) {
        this.provisioningScript = provisioningScript;
    }

    // Business methods
    public void addDependency(String serviceCode) {
        this.dependsOnServiceCodes.add(serviceCode);
    }

    public void removeDependency(String serviceCode) {
        this.dependsOnServiceCodes.remove(serviceCode);
    }

    public boolean hasDependencies() {
        return !dependsOnServiceCodes.isEmpty();
    }

    public boolean dependsOn(String serviceCode) {
        return dependsOnServiceCodes.contains(serviceCode);
    }

    public boolean isActive() {
        return ServiceStatus.ACTIVE == this.status;
    }
}
