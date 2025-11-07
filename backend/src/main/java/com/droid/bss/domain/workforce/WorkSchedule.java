package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Work schedule for employees
 */
@Entity
@Table(name = "work_schedules")
@EntityListeners(AuditingEntityListener.class)
public class WorkSchedule {

    @Id
    private String id;

    @Column(nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column
    private LocalTime startTime;

    @Column
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    @Column
    private String shiftName;

    @Column
    private String territory;

    @Column
    private String serviceArea;

    @Column
    private Integer maxJobs;

    @Column
    private String breakTime;

    @Column
    private String notes;

    @Column
    private Boolean onCall = false;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduledTimeOff> timeOff = new ArrayList<>();

    @Column
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Constructors
    public WorkSchedule() {
    }

    public WorkSchedule(String employeeId, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime, ScheduleType scheduleType) {
        this.employeeId = employeeId;
        this.scheduleDate = scheduleDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleType = scheduleType;
        this.status = ScheduleStatus.SCHEDULED;
    }

    // Business methods
    public void confirm() {
        if (this.status == ScheduleStatus.SCHEDULED) {
            this.status = ScheduleStatus.CONFIRMED;
        }
    }

    public void cancel(String reason) {
        this.status = ScheduleStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes : "") + "\nCancelled: " + reason;
    }

    public void complete() {
        this.status = ScheduleStatus.COMPLETED;
    }

    public void addTimeOff(LocalTime startTime, LocalTime endTime, String reason) {
        ScheduledTimeOff timeOff = new ScheduledTimeOff(this, startTime, endTime, reason);
        this.timeOff.add(timeOff);
    }

    public boolean isAvailableAt(LocalTime time) {
        if (this.status != ScheduleStatus.CONFIRMED && this.status != ScheduleStatus.SCHEDULED) {
            return false;
        }

        // Check if time is within work hours
        if (time.isBefore(startTime) || time.isAfter(endTime)) {
            return false;
        }

        // Check if time is in time off
        for (ScheduledTimeOff off : timeOff) {
            if (time.equals(off.getStartTime()) || (time.isAfter(off.getStartTime()) && time.isBefore(off.getEndTime()))) {
                return false;
            }
        }

        return true;
    }

    public int getWorkingHours() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toHoursPart();
    }

    // Getters and Setters
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }

    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }

    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getScheduleDate() { return scheduleDate; }

    public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }

    public LocalTime getStartTime() { return startTime; }

    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }

    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public ScheduleType getScheduleType() { return scheduleType; }

    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public ScheduleStatus getStatus() { return status; }

    public void setStatus(ScheduleStatus status) { this.status = status; }

    public String getShiftName() { return shiftName; }

    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public String getTerritory() { return territory; }

    public void setTerritory(String territory) { this.territory = territory; }

    public String getServiceArea() { return serviceArea; }

    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }

    public Integer getMaxJobs() { return maxJobs; }

    public void setMaxJobs(Integer maxJobs) { this.maxJobs = maxJobs; }

    public String getBreakTime() { return breakTime; }

    public void setBreakTime(String breakTime) { this.breakTime = breakTime; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getOnCall() { return onCall; }

    public void setOnCall(Boolean onCall) { this.onCall = onCall; }

    public List<ScheduledTimeOff> getTimeOff() { return timeOff; }

    public void setTimeOff(List<ScheduledTimeOff> timeOff) { this.timeOff = timeOff; }

    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
