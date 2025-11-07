package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.workforce.WorkSchedule;
import com.droid.bss.domain.workforce.ScheduleStatus;
import com.droid.bss.domain.workforce.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for work schedules
 */
@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, String> {

    /**
     * Find by employee and date
     */
    List<WorkSchedule> findByEmployeeIdAndScheduleDate(String employeeId, LocalDate scheduleDate);

    /**
     * Find by date
     */
    List<WorkSchedule> findByScheduleDate(LocalDate scheduleDate);

    /**
     * Find by status
     */
    List<WorkSchedule> findByStatus(ScheduleStatus status);

    /**
     * Find by employee
     */
    List<WorkSchedule> findByEmployeeId(String employeeId);

    /**
     * Find by territory
     */
    List<WorkSchedule> findByTerritoryContaining(String territory);

    /**
     * Find by service area
     */
    List<WorkSchedule> findByServiceArea(String serviceArea);

    /**
     * Find by schedule type
     */
    List<WorkSchedule> findByScheduleType(ScheduleType scheduleType);

    /**
     * Find on-call schedules for a date
     */
    @Query("SELECT w FROM WorkSchedule w WHERE w.scheduleDate = :date AND w.onCall = true AND w.status != 'CANCELLED'")
    List<WorkSchedule> findOnCallSchedules(@Param("date") LocalDate date);

    /**
     * Find schedules in date range
     */
    @Query("SELECT w FROM WorkSchedule w WHERE w.scheduleDate BETWEEN :startDate AND :endDate")
    List<WorkSchedule> findByScheduleDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find schedules by employee and date range
     */
    @Query("SELECT w FROM WorkSchedule w WHERE w.employeeId = :employeeId AND w.scheduleDate BETWEEN :startDate AND :endDate")
    List<WorkSchedule> findByEmployeeIdAndDateRange(@Param("employeeId") String employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count scheduled hours for employee
     */
    @Query("SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (w.endTime - w.startTime))/3600), 0) FROM WorkSchedule w WHERE w.employeeId = :employeeId AND w.scheduleDate BETWEEN :startDate AND :endDate AND w.status != 'CANCELLED'")
    int getScheduledHours(@Param("employeeId") String employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find available employees for a date
     */
    @Query("SELECT w FROM WorkSchedule w WHERE w.scheduleDate = :date AND w.status IN ('SCHEDULED', 'CONFIRMED') AND w.maxJobs > COALESCE((SELECT COUNT(a) FROM WorkOrderAssignment a WHERE a.employeeId = w.employeeId AND a.status IN ('ASSIGNED', 'IN_PROGRESS')), 0)")
    List<WorkSchedule> findAvailableEmployees(@Param("date") LocalDate date);
}
