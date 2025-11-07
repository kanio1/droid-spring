package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.workforce.TimeSheetEntry;
import com.droid.bss.domain.workforce.TimeSheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for time sheet entries
 */
@Repository
public interface TimeSheetRepository extends JpaRepository<TimeSheetEntry, String> {

    /**
     * Find by employee and date
     */
    List<TimeSheetEntry> findByEmployeeIdAndWorkDate(String employeeId, LocalDate workDate);

    /**
     * Find by employee
     */
    List<TimeSheetEntry> findByEmployeeId(String employeeId);

    /**
     * Find by work date
     */
    List<TimeSheetEntry> findByWorkDate(LocalDate workDate);

    /**
     * Find by status
     */
    List<TimeSheetEntry> findByStatus(TimeSheetStatus status);

    /**
     * Find by work order
     */
    List<TimeSheetEntry> findByWorkOrderId(String workOrderId);

    /**
     * Find in date range
     */
    List<TimeSheetEntry> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find by employee and date range
     */
    List<TimeSheetEntry> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Find overdue entries
     */
    @Query("SELECT t FROM TimeSheetEntry t WHERE t.workDate < :yesterday AND t.status = 'IN_PROGRESS'")
    List<TimeSheetEntry> findOverdueEntries(@Param("yesterday") LocalDate yesterday);

    /**
     * Get total hours for employee in period
     */
    @Query("SELECT COALESCE(SUM(t.regularHours + t.overtimeHours + t.doubleTimeHours), 0) FROM TimeSheetEntry t WHERE t.employeeId = :employeeId AND t.workDate BETWEEN :startDate AND :endDate AND t.status = 'APPROVED'")
    int getTotalHours(@Param("employeeId") String employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get total billable hours for employee in period
     */
    @Query("SELECT COALESCE(SUM(t.regularHours + t.overtimeHours + t.doubleTimeHours), 0) FROM TimeSheetEntry t WHERE t.employeeId = :employeeId AND t.workDate BETWEEN :startDate AND :endDate AND t.status = 'APPROVED' AND t.billable = true")
    int getTotalBillableHours(@Param("employeeId") String employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count pending approvals
     */
    @Query("SELECT COUNT(t) FROM TimeSheetEntry t WHERE t.status = 'COMPLETED' OR t.status = 'PENDING_APPROVAL'")
    long countPendingApproval();

    /**
     * Find entries needing approval
     */
    @Query("SELECT t FROM TimeSheetEntry t WHERE t.status = 'COMPLETED' OR t.status = 'PENDING_APPROVAL'")
    List<TimeSheetEntry> findPendingApproval();
}
