/**
 * User-Tenant Junction Table
 *
 * Many-to-many relationship between User and Tenant
 * Stores user roles and permissions within each tenant
 */
package com.droid.bss.domain.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Junction entity for User-Tenant many-to-many relationship
 * Contains user roles and permissions specific to a tenant
 */
@Entity
@Table(name = "user_tenants")
@EntityListeners(AuditingEntityListener.class)
@IdClass(UserTenantId.class)
public class UserTenant {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserTenantRole role;

    @ElementCollection
    @CollectionTable(
        name = "user_tenant_permissions",
        joinColumns = {
            @JoinColumn(name = "user_id"),
            @JoinColumn(name = "tenant_id")
        }
    )
    @Column(name = "permission")
    private Set<String> permissions = new java.util.HashSet<>();

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Reference back to Tenant for bidirectional navigation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    // Constructors
    public UserTenant() {}

    public UserTenant(UUID userId, UUID tenantId, UserTenantRole role) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
    }

    // Business methods
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isActive() {
        return isActive != null && isActive && !isDeleted();
    }

    public void activate() {
        this.isActive = true;
        this.acceptedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }

    public void markLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    public boolean hasPermission(String permission) {
        if (isDeleted() || !isActive()) {
            return false;
        }
        return permissions.contains(permission) || permissions.contains("*");
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UserTenantRole getRole() {
        return role;
    }

    public void setRole(UserTenantRole role) {
        this.role = role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(String permission) {
        this.permissions.add(permission);
    }

    public void removePermission(String permission) {
        this.permissions.remove(permission);
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
