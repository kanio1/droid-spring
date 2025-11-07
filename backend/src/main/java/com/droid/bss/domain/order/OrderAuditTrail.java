package com.droid.bss.domain.order;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Order audit trail for tracking all status changes and actions
 */
@Entity
@Table(name = "order_audit_trail")
public class OrderAuditTrail extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @NotNull
    @Column(name = "action", length = 100, nullable = false)
    private String action; // CREATED, APPROVED, REJECTED, PROCESSING, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "performed_by", length = 255)
    private String performedBy; // User ID or system

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private java.util.Map<String, Object> metadata;

    // Constructors
    public OrderAuditTrail() {
        this.timestamp = LocalDateTime.now();
    }

    public OrderAuditTrail(OrderEntity order, String action, String performedBy) {
        this();
        this.order = order;
        this.action = action;
        this.performedBy = performedBy;
    }

    public OrderAuditTrail(OrderEntity order, String action, String oldStatus, String newStatus, String performedBy) {
        this(order, action, performedBy);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    // Business methods
    public static OrderAuditTrail created(OrderEntity order, String performedBy) {
        return new OrderAuditTrail(order, "CREATED", performedBy);
    }

    public static OrderAuditTrail approved(OrderEntity order, String oldStatus, String performedBy, String reason) {
        OrderAuditTrail audit = new OrderAuditTrail(order, "APPROVED", oldStatus, "APPROVED", performedBy);
        audit.setReason(reason);
        return audit;
    }

    public static OrderAuditTrail rejected(OrderEntity order, String oldStatus, String performedBy, String reason) {
        OrderAuditTrail audit = new OrderAuditTrail(order, "REJECTED", oldStatus, "REJECTED", performedBy);
        audit.setReason(reason);
        return audit;
    }

    public static OrderAuditTrail cancelled(OrderEntity order, String oldStatus, String performedBy, String reason) {
        OrderAuditTrail audit = new OrderAuditTrail(order, "CANCELLED", oldStatus, "CANCELLED", performedBy);
        audit.setReason(reason);
        return audit;
    }

    public static OrderAuditTrail statusChanged(OrderEntity order, String oldStatus, String newStatus, String performedBy) {
        return new OrderAuditTrail(order, "STATUS_CHANGED", oldStatus, newStatus, performedBy);
    }

    public static OrderAuditTrail completed(OrderEntity order, String oldStatus, String performedBy) {
        return new OrderAuditTrail(order, "COMPLETED", oldStatus, "COMPLETED", performedBy);
    }

    public boolean isStatusChange() {
        return oldStatus != null && newStatus != null && !oldStatus.equals(newStatus);
    }

    public boolean isTerminalAction() {
        return "COMPLETED".equals(action) || "CANCELLED".equals(action) || "REJECTED".equals(action);
    }

    // Getters and Setters
    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
