package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Work order entity for tracking service tasks
 */
@Entity
@Table(name = "work_orders")
@EntityListeners(AuditingEntityListener.class)
public class WorkOrderEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String workOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String priority;

    @Column
    private String customerId;

    @Column
    private String serviceAddress;

    @Column
    private String serviceType;

    @Column
    private String requiredSkill;

    @Column
    private Integer estimatedDuration;

    @Column
    private LocalDate scheduledDate;

    @Column
    private LocalDateTime scheduledStartTime;

    @Column
    private LocalDateTime scheduledEndTime;

    @Column
    private LocalDate requestedDate;

    @Column
    private LocalDate dueDate;

    @Column
    private String customerContact;

    @Column
    private String customerPhone;

    @Column
    private String notes;

    @Column
    private String attachments;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderTask> tasks = new ArrayList<>();

    @Column
    private Integer actualDuration;

    @Column
    private String completionNotes;

    @Column
    private Integer customerSatisfactionRating;

    @Column
    private Boolean requiresFollowUp = false;

    @Column
    private String followUpNotes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public WorkOrderEntity() {
    }

    public WorkOrderEntity(String workOrderNumber, WorkOrderType type, String title) {
        this.workOrderNumber = workOrderNumber;
        this.type = type;
        this.title = title;
        this.status = WorkOrderStatus.PENDING;
    }

    // Business methods
    public void assignToTechnician(String employeeId) {
        if (this.status != WorkOrderStatus.PENDING) {
            throw new IllegalStateException("Can only assign pending work orders");
        }
        WorkOrderAssignment assignment = new WorkOrderAssignment(this, employeeId);
        this.assignments.add(assignment);
        this.status = WorkOrderStatus.ASSIGNED;
    }

    public void startWork(String employeeId) {
        if (this.status != WorkOrderStatus.ASSIGNED) {
            throw new IllegalStateException("Can only start assigned work orders");
        }
        this.status = WorkOrderStatus.IN_PROGRESS;
        this.scheduledStartTime = LocalDateTime.now();
    }

    public void completeWork(String completionNotes) {
        if (this.status != WorkOrderStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete work orders in progress");
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.completionNotes = completionNotes;
        this.scheduledEndTime = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (this.status == WorkOrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed work orders");
        }
        this.status = WorkOrderStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes : "") + "\nCancelled: " + reason;
    }

    public void reschedule(LocalDate newDate, LocalDateTime newStartTime, LocalDateTime newEndTime) {
        this.scheduledDate = newDate;
        this.scheduledStartTime = newStartTime;
        this.scheduledEndTime = newEndTime;
        if (this.status == WorkOrderStatus.SCHEDULED) {
            this.status = WorkOrderStatus.PENDING;
        }
    }

    public boolean isOverdue() {
        if (dueDate == null) return false;
        return LocalDate.now().isAfter(dueDate) && this.status != WorkOrderStatus.COMPLETED;
    }

    public void addTask(String description, Boolean required, Integer order) {
        WorkOrderTask task = new WorkOrderTask(this, description, required, order);
        tasks.add(task);
    }

    public int getCompletionPercentage() {
        if (tasks.isEmpty()) {
            return status == WorkOrderStatus.COMPLETED ? 100 : 0;
        }
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == WorkOrderTaskStatus.COMPLETED).count();
        return (int) (completedTasks * 100 / tasks.size());
    }

    // Getters and Setters
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getWorkOrderNumber() { return workOrderNumber; }

    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

    public WorkOrderType getType() { return type; }

    public void setType(WorkOrderType type) { this.type = type; }

    public WorkOrderStatus getStatus() { return status; }

    public void setStatus(WorkOrderStatus status) { this.status = status; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }

    public void setPriority(String priority) { this.priority = priority; }

    public String getCustomerId() { return customerId; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getServiceAddress() { return serviceAddress; }

    public void setServiceAddress(String serviceAddress) { this.serviceAddress = serviceAddress; }

    public String getServiceType() { return serviceType; }

    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getRequiredSkill() { return requiredSkill; }

    public void setRequiredSkill(String requiredSkill) { this.requiredSkill = requiredSkill; }

    public Integer getEstimatedDuration() { return estimatedDuration; }

    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public LocalDate getScheduledDate() { return scheduledDate; }

    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getScheduledStartTime() { return scheduledStartTime; }

    public void setScheduledStartTime(LocalDateTime scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }

    public LocalDateTime getScheduledEndTime() { return scheduledEndTime; }

    public void setScheduledEndTime(LocalDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }

    public LocalDate getRequestedDate() { return requestedDate; }

    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }

    public LocalDate getDueDate() { return dueDate; }

    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getCustomerContact() { return customerContact; }

    public void setCustomerContact(String customerContact) { this.customerContact = customerContact; }

    public String getCustomerPhone() { return customerPhone; }

    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public String getAttachments() { return attachments; }

    public void setAttachments(String attachments) { this.attachments = attachments; }

    public List<WorkOrderAssignment> getAssignments() { return assignments; }

    public void setAssignments(List<WorkOrderAssignment> assignments) { this.assignments = assignments; }

    public List<WorkOrderTask> getTasks() { return tasks; }

    public void setTasks(List<WorkOrderTask> tasks) { this.tasks = tasks; }

    public Integer getActualDuration() { return actualDuration; }

    public void setActualDuration(Integer actualDuration) { this.actualDuration = actualDuration; }

    public String getCompletionNotes() { return completionNotes; }

    public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }

    public Integer getCustomerSatisfactionRating() { return customerSatisfactionRating; }

    public void setCustomerSatisfactionRating(Integer customerSatisfactionRating) { this.customerSatisfactionRating = customerSatisfactionRating; }

    public Boolean getRequiresFollowUp() { return requiresFollowUp; }

    public void setRequiresFollowUp(Boolean requiresFollowUp) { this.requiresFollowUp = requiresFollowUp; }

    public String getFollowUpNotes() { return followUpNotes; }

    public void setFollowUpNotes(String followUpNotes) { this.followUpNotes = followUpNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
