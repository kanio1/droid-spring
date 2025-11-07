package com.droid.bss.infrastructure.write;

import com.droid.bss.infrastructure.database.entity.CostCalculationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataCostCalculationRepository extends JpaRepository<CostCalculationEntity, Long> {

    @Query("SELECT cc FROM CostCalculationEntity cc WHERE cc.customerId = :customerId AND cc.periodStart >= :startDate AND cc.periodEnd <= :endDate ORDER BY cc.periodStart DESC")
    List<CostCalculationEntity> findByCustomerIdAndPeriod(
            @Param("customerId") Long customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    List<CostCalculationEntity> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    List<CostCalculationEntity> findByStatus(String status);

    @Query("DELETE FROM CostCalculationEntity cc WHERE cc.customerId = :customerId AND cc.periodStart >= :startDate AND cc.periodEnd <= :endDate")
    void deleteByCustomerIdAndPeriod(
            @Param("customerId") Long customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
