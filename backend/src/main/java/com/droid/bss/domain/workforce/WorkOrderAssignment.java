package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Work order assignment to technicians
 */
@Entity
@Table(name = "work_order_assignments")
@EntityListeners(AuditingEntityListener.class)
public class WorkOrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderEntity workOrder;

    @Column(nullable = false)
    private String employeeId;

    @Column
    private String employeeName;

    @Column
    private LocalDateTime assignedAt;

    @Column
    private LocalDateTime acceptedAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String status;

    @Column
    private String notes;

    @Column
    private String assignedBy;

    @Column
    private String completedBy;

    @Column(columnDefinition = "TEXT")
    private String assignmentNotes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public WorkOrderAssignment() {
    }

    public WorkOrderAssignment(WorkOrderEntity workOrder, String employeeId) {
        this.workOrder = workOrder;
        this.employeeId = employeeId;
        this.status = "ASSIGNED";
        this.assignedAt = LocalDateTime.now();
    }

    // Business methods
    public void accept() {
        if (this.status.equals("ASSIGNED")) {
            this.status = "ACCEPTED";
            this.acceptedAt = LocalDateTime.now();
        }
    }

    public void start() {
        if (this.status.equals("ACCEPTED") || this.status.equals("ASSIGNED")) {
            this.status = "IN_PROGRESS";
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = "REJECTED";
        this.notes = "Rejected: " + reason;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }

    public long getDurationMinutes() {
        if (startedAt == null || completedAt == null) return 0;
        return java.time.Duration.between(startedAt, completedAt).toMinutes();
    }

    // Getters and Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public WorkOrderEntity getWorkOrder() { return workOrder; }

    public void setWorkOrder(WorkOrderEntity workOrder) { this.workOrder = workOrder; }

    public String getEmployeeId() { return employeeId; }

    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }

    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDateTime getAssignedAt() { return assignedAt; }

    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }

    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }

    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public String getAssignedBy() { return assignedBy; }

    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getCompletedBy() { return completedBy; }

    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public String getAssignmentNotes() { return assignmentNotes; }

    public void setAssignmentNotes(String assignmentNotes) { this.assignmentNotes = assignmentNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
