package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Individual tasks within a work order
 */
@Entity
@Table(name = "work_order_tasks")
@EntityListeners(AuditingEntityListener.class)
public class WorkOrderTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderEntity workOrder;

    @Column(nullable = false)
    private String description;

    @Column
    private Integer taskOrder;

    @Column
    private Boolean required = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderTaskStatus status;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String completionNotes;

    @Column
    private String performedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public WorkOrderTask() {
    }

    public WorkOrderTask(WorkOrderEntity workOrder, String description, Boolean required, Integer order) {
        this.workOrder = workOrder;
        this.description = description;
        this.required = required;
        this.taskOrder = order;
        this.status = WorkOrderTaskStatus.PENDING;
    }

    // Business methods
    public void start(String performedBy) {
        this.status = WorkOrderTaskStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.performedBy = performedBy;
    }

    public void complete(String completionNotes) {
        this.status = WorkOrderTaskStatus.COMPLETED;
        this.completionNotes = completionNotes;
        this.completedAt = LocalDateTime.now();

        if (this.startedAt != null) {
            this.durationMinutes = (int) java.time.Duration.between(this.startedAt, this.completedAt).toMinutes();
        }
    }

    public void skip(String reason) {
        this.status = WorkOrderTaskStatus.SKIPPED;
        this.completionNotes = "Skipped: " + reason;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == WorkOrderTaskStatus.COMPLETED;
    }

    public boolean canStart() {
        // Basic dependency check - in real implementation, would check previous tasks
        return this.status == WorkOrderTaskStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public WorkOrderEntity getWorkOrder() { return workOrder; }

    public void setWorkOrder(WorkOrderEntity workOrder) { this.workOrder = workOrder; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public Integer getTaskOrder() { return taskOrder; }

    public void setTaskOrder(Integer taskOrder) { this.taskOrder = taskOrder; }

    public Boolean getRequired() { return required; }

    public void setRequired(Boolean required) { this.required = required; }

    public WorkOrderTaskStatus getStatus() { return status; }

    public void setStatus(WorkOrderTaskStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }

    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getDurationMinutes() { return durationMinutes; }

    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getCompletionNotes() { return completionNotes; }

    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public String getPerformedBy() { return performedBy; }

    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
