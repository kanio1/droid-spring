package com.droid.bss.infrastructure.write;

import com.droid.bss.infrastructure.database.entity.CostForecastEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataCostForecastRepository extends JpaRepository<CostForecastEntity, Long> {

    List<CostForecastEntity> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    @Query("SELECT cf FROM CostForecastEntity cf WHERE cf.customerId = :customerId AND cf.forecastPeriodStart >= :startDate AND cf.forecastPeriodEnd <= :endDate ORDER BY cf.forecastPeriodStart DESC")
    List<CostForecastEntity> findByCustomerIdAndPeriod(
            @Param("customerId") Long customerId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    List<CostForecastEntity> findByForecastPeriodStart(Instant forecastPeriodStart);

    void deleteByCustomerIdAndResourceType(Long customerId, String resourceType);
}
