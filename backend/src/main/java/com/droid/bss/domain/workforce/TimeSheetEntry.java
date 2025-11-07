package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Time tracking for employees
 */
@Entity
@Table(name = "time_sheet_entries")
@EntityListeners(AuditingEntityListener.class)
public class TimeSheetEntry {

    @Id
    private String id;

    @Column(nullable = false)
    private String employeeId;

    @Column
    private String employeeName;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column
    private LocalDateTime clockIn;

    @Column
    private LocalDateTime clockOut;

    @Column
    private LocalTime breakStart;

    @Column
    private LocalTime breakEnd;

    @Column
    private Integer breakMinutes = 0;

    @Column
    private Integer regularHours = 0;

    @Column
    private Integer overtimeHours = 0;

    @Column
    private Integer doubleTimeHours = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSheetStatus status;

    @Column
    private String workOrderId;

    @Column
    private String workOrderNumber;

    @Column
    private String jobDescription;

    @Column
    private String notes;

    @Column
    private String location;

    @Column
    private Boolean billable = true;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal hourlyRate;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Column
    private String approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "timeSheetEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetBreak> breaks = new ArrayList<>();

    @Column
    private String createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // Constructors
    public TimeSheetEntry() {
    }

    public TimeSheetEntry(String employeeId, LocalDate workDate, LocalDateTime clockIn) {
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.clockIn = clockIn;
        this.status = TimeSheetStatus.IN_PROGRESS;
    }

    // Business methods
    public void clockIn() {
        this.status = TimeSheetStatus.IN_PROGRESS;
    }

    public void clockOut(LocalDateTime clockOutTime) {
        this.clockOut = clockOutTime;
        this.status = TimeSheetStatus.COMPLETED;
        calculateHours();
    }

    public void addBreak(LocalTime start, LocalTime end, String reason) {
        TimeSheetBreak timeBreak = new TimeSheetBreak(this, start, end, reason);
        this.breaks.add(timeBreak);
        calculateBreakTime();
    }

    public void calculateHours() {
        if (clockIn == null || clockOut == null) return;

        long totalMinutes = java.time.Duration.between(clockIn, clockOut).toMinutes();
        totalMinutes -= breakMinutes;

        // Regular hours (8 per day)
        regularHours = (int) Math.min(totalMinutes / 60, 8);

        // Overtime hours (4-12 hours)
        long overtimeMinutes = Math.max(0, totalMinutes - (8 * 60));
        overtimeHours = (int) Math.min(overtimeMinutes / 60, 4);

        // Double time (over 12 hours)
        long doubleTimeMinutes = Math.max(0, totalMinutes - (12 * 60));
        doubleTimeHours = (int) (doubleTimeMinutes / 60);
    }

    public void calculateBreakTime() {
        this.breakMinutes = breaks.stream()
                .mapToInt(b -> (int) java.time.Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                .sum();
    }

    public void approve(String approvedBy) {
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
        this.status = TimeSheetStatus.APPROVED;
    }

    public void reject(String reason) {
        this.status = TimeSheetStatus.REJECTED;
        this.notes = (this.notes != null ? this.notes : "") + "\nRejected: " + reason;
    }

    public int getTotalWorkMinutes() {
        if (clockIn == null || clockOut == null) return 0;
        return (int) java.time.Duration.between(clockIn, clockOut).toMinutes() - breakMinutes;
    }

    public int getTotalWorkHours() {
        return getTotalWorkMinutes() / 60;
    }

    public boolean isOverdue() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return workDate.isBefore(yesterday) && this.status == TimeSheetStatus.IN_PROGRESS;
    }

    // Getters and Setters
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }

    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }

    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getWorkDate() { return workDate; }

    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public LocalDateTime getClockIn() { return clockIn; }

    public void setClockIn(LocalDateTime clockIn) { this.clockIn = clockIn; }

    public LocalDateTime getClockOut() { return clockOut; }

    public void setClockOut(LocalDateTime clockOut) { this.clockOut = clockOut; }

    public LocalTime getBreakStart() { return breakStart; }

    public void setBreakStart(LocalTime breakStart) { this.breakStart = breakStart; }

    public LocalTime getBreakEnd() { return breakEnd; }

    public void setBreakEnd(LocalTime breakEnd) { this.breakEnd = breakEnd; }

    public Integer getBreakMinutes() { return breakMinutes; }

    public void setBreakMinutes(Integer breakMinutes) { this.breakMinutes = breakMinutes; }

    public Integer getRegularHours() { return regularHours; }

    public void setRegularHours(Integer regularHours) { this.regularHours = regularHours; }

    public Integer getOvertimeHours() { return overtimeHours; }

    public void setOvertimeHours(Integer overtimeHours) { this.overtimeHours = overtimeHours; }

    public Integer getDoubleTimeHours() { return doubleTimeHours; }

    public void setDoubleTimeHours(Integer doubleTimeHours) { this.doubleTimeHours = doubleTimeHours; }

    public TimeSheetStatus getStatus() { return status; }

    public void setStatus(TimeSheetStatus status) { this.status = status; }

    public String getWorkOrderId() { return workOrderId; }

    public void setWorkOrderId(String workOrderId) { this.workOrderId = workOrderId; }

    public String getWorkOrderNumber() { return workOrderNumber; }

    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

    public String getJobDescription() { return jobDescription; }

    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public Boolean getBillable() { return billable; }

    public void setBillable(Boolean billable) { this.billable = billable; }

    public java.math.BigDecimal getHourlyRate() { return hourlyRate; }

    public void setHourlyRate(java.math.BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public java.math.BigDecimal getTotalAmount() { return totalAmount; }

    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getApprovedBy() { return approvedBy; }

    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }

    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public List<TimeSheetBreak> getBreaks() { return breaks; }

    public void setBreaks(List<TimeSheetBreak> breaks) { this.breaks = breaks; }

    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
