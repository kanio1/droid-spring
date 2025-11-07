package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Scheduled time off during work schedule
 */
@Entity
@Table(name = "scheduled_time_off")
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTimeOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkSchedule schedule;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column
    private String reason;

    @Column
    private String type;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ScheduledTimeOff() {
    }

    public ScheduledTimeOff(WorkSchedule schedule, LocalTime startTime, LocalTime endTime, String reason) {
        this.schedule = schedule;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.type = "BREAK";
    }

    // Getters and Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public WorkSchedule getSchedule() { return schedule; }

    public void setSchedule(WorkSchedule schedule) { this.schedule = schedule; }

    public LocalTime getStartTime() { return startTime; }

    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }

    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getDurationMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }
}
