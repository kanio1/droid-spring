package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.workforce.EmployeeEntity;
import com.droid.bss.domain.workforce.EmployeeStatus;
import com.droid.bss.domain.workforce.EmployeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for employees
 */
@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, String> {

    /**
     * Find by employee code
     */
    Optional<EmployeeEntity> findByEmployeeCode(String employeeCode);

    /**
     * Find by email
     */
    Optional<EmployeeEntity> findByEmail(String email);

    /**
     * Find employees by type
     */
    List<EmployeeEntity> findByEmployeeType(EmployeeType employeeType);

    /**
     * Find employees by status
     */
    List<EmployeeEntity> findByStatus(EmployeeStatus status);

    /**
     * Find employees by territory
     */
    List<EmployeeEntity> findByTerritoryContaining(String territory);

    /**
     * Find employees by service area
     */
    List<EmployeeEntity> findByServiceArea(String serviceArea);

    /**
     * Find technicians on call
     */
    @Query("SELECT e FROM EmployeeEntity e WHERE e.onCall = true AND e.status = 'ACTIVE'")
    List<EmployeeEntity> findOnCallTechnicians();

    /**
     * Find employees with specific skill
     */
    @Query("SELECT e FROM EmployeeEntity e JOIN e.skills s WHERE s.skillName = :skillName AND s.active = true AND e.status = 'ACTIVE'")
    List<EmployeeEntity> findBySkill(@Param("skillName") String skillName);

    /**
     * Find employees by department
     */
    List<EmployeeEntity> findByDepartment(String department);

    /**
     * Count employees by status
     */
    @Query("SELECT COUNT(e) FROM EmployeeEntity e WHERE e.status = :status")
    long countByStatus(@Param("status") EmployeeStatus status);

    /**
     * Find employees hired in date range
     */
    @Query("SELECT e FROM EmployeeEntity e WHERE e.hireDate BETWEEN :startDate AND :endDate")
    List<EmployeeEntity> findByHireDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find technicians with capacity (max daily jobs not reached)
     */
    @Query("SELECT e FROM EmployeeEntity e WHERE e.status = 'ACTIVE' AND e.maxDailyJobs > COALESCE((SELECT COUNT(a) FROM e.assignments a WHERE a.status = 'IN_PROGRESS' OR a.status = 'ASSIGNED'), 0)")
    List<EmployeeEntity> findAvailableTechnicians();

    /**
     * Find employees by territory and skill
     */
    @Query("SELECT DISTINCT e FROM EmployeeEntity e JOIN e.skills s WHERE e.territory LIKE %:territory% AND s.skillName = :skillName AND s.active = true AND e.status = 'ACTIVE'")
    List<EmployeeEntity> findByTerritoryAndSkill(@Param("territory") String territory, @Param("skillName") String skillName);
}
