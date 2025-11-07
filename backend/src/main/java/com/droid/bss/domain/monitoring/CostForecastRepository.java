package com.droid.bss.domain.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for CostForecast
 */
public interface CostForecastRepository {

    Optional<CostForecast> findById(Long id);

    List<CostForecast> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    List<CostForecast> findByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate);

    List<CostForecast> findByForecastPeriodStart(Instant forecastPeriodStart);

    CostForecast save(CostForecast forecast);

    void deleteById(Long id);

    void deleteByCustomerIdAndResourceType(Long customerId, String resourceType);
}
