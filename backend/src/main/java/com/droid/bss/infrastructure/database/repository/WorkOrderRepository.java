package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.workforce.WorkOrderEntity;
import com.droid.bss.domain.workforce.WorkOrderStatus;
import com.droid.bss.domain.workforce.WorkOrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for work orders
 */
@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, String> {

    /**
     * Find by work order number
     */
    Optional<WorkOrderEntity> findByWorkOrderNumber(String workOrderNumber);

    /**
     * Find by type
     */
    List<WorkOrderEntity> findByType(WorkOrderType type);

    /**
     * Find by status
     */
    List<WorkOrderEntity> findByStatus(WorkOrderStatus status);

    /**
     * Find by customer ID
     */
    List<WorkOrderEntity> findByCustomerId(String customerId);

    /**
     * Find by required skill
     */
    List<WorkOrderEntity> findByRequiredSkill(String requiredSkill);

    /**
     * Find by scheduled date
     */
    List<WorkOrderEntity> findByScheduledDate(LocalDate scheduledDate);

    /**
     * Find by territory
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.serviceAddress LIKE %:territory%")
    List<WorkOrderEntity> findByTerritory(@Param("territory") String territory);

    /**
     * Find pending work orders
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.status = 'PENDING' OR w.status = 'SCHEDULED'")
    List<WorkOrderEntity> findPendingWorkOrders();

    /**
     * Find overdue work orders
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.dueDate < :today AND w.status != 'COMPLETED' AND w.status != 'CANCELLED'")
    List<WorkOrderEntity> findOverdueWorkOrders(@Param("today") LocalDate today);

    /**
     * Find work orders by employee
     */
    @Query("SELECT w FROM WorkOrderEntity w JOIN w.assignments a WHERE a.employeeId = :employeeId")
    List<WorkOrderEntity> findByEmployee(@Param("employeeId") String employeeId);

    /**
     * Find work orders in date range
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.scheduledDate BETWEEN :startDate AND :endDate")
    List<WorkOrderEntity> findByScheduledDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find emergency work orders
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.type = 'EMERGENCY' AND w.status != 'COMPLETED' AND w.status != 'CANCELLED'")
    List<WorkOrderEntity> findEmergencyWorkOrders();

    /**
     * Count work orders by status
     */
    @Query("SELECT COUNT(w) FROM WorkOrderEntity w WHERE w.status = :status")
    long countByStatus(@Param("status") WorkOrderStatus status);

    /**
     * Find work orders requiring specific skills
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.requiredSkill IN :skills AND w.status = 'PENDING'")
    List<WorkOrderEntity> findByRequiredSkillIn(@Param("skills") List<String> skills);

    /**
     * Find work orders for a territory and date range
     */
    @Query("SELECT w FROM WorkOrderEntity w WHERE w.serviceAddress LIKE %:territory% AND w.scheduledDate BETWEEN :startDate AND :endDate")
    List<WorkOrderEntity> findByTerritoryAndDateRange(@Param("territory") String territory, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
