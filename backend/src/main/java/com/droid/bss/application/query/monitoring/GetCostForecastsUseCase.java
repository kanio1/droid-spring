package com.droid.bss.application.query.monitoring;

import com.droid.bss.domain.monitoring.CostForecast;
import com.droid.bss.domain.monitoring.CostForecastRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class GetCostForecastsUseCase {

    private final CostForecastRepository costForecastRepository;

    public GetCostForecastsUseCase(CostForecastRepository costForecastRepository) {
        this.costForecastRepository = costForecastRepository;
    }

    public List<CostForecast> getByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return costForecastRepository.findByCustomerIdAndResourceType(customerId, resourceType);
    }

    public List<CostForecast> getByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate) {
        return costForecastRepository.findByCustomerIdAndPeriod(customerId, startDate, endDate);
    }

    public List<CostForecast> getByForecastPeriodStart(Instant forecastPeriodStart) {
        return costForecastRepository.findByForecastPeriodStart(forecastPeriodStart);
    }
}
