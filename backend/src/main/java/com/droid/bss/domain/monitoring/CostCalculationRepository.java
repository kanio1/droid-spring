package com.droid.bss.domain.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for CostCalculation
 */
public interface CostCalculationRepository {

    Optional<CostCalculation> findById(Long id);

    List<CostCalculation> findByCustomerIdAndPeriod(
            Long customerId, Instant startDate, Instant endDate);

    List<CostCalculation> findByCustomerIdAndResourceType(
            Long customerId, String resourceType);

    List<CostCalculation> findByStatus(String status);

    CostCalculation save(CostCalculation calculation);

    void deleteById(Long id);

    void deleteByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate);
}
