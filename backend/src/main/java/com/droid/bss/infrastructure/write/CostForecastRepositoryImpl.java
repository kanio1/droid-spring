package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.monitoring.CostForecast;
import com.droid.bss.domain.monitoring.CostForecastRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CostForecastRepositoryImpl implements CostForecastRepository {

    private final SpringDataCostForecastRepository springDataRepository;

    public CostForecastRepositoryImpl(SpringDataCostForecastRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CostForecast> findById(Long id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CostForecast> findByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return springDataRepository.findByCustomerIdAndResourceType(customerId, resourceType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CostForecast> findByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate) {
        return springDataRepository.findByCustomerIdAndPeriod(customerId, startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CostForecast> findByForecastPeriodStart(Instant forecastPeriodStart) {
        return springDataRepository.findByForecastPeriodStart(forecastPeriodStart).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CostForecast save(CostForecast forecast) {
        var entity = toEntity(forecast);
        var savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public void deleteByCustomerIdAndResourceType(Long customerId, String resourceType) {
        springDataRepository.deleteByCustomerIdAndResourceType(customerId, resourceType);
    }

    private CostForecast toDomain(com.droid.bss.infrastructure.database.entity.CostForecastEntity entity) {
        if (entity == null) {
            return null;
        }
        var forecast = new CostForecast();
        forecast.setId(entity.getId());
        forecast.setCustomerId(entity.getCustomerId());
        forecast.setResourceType(entity.getResourceType());
        forecast.setBillingPeriod(entity.getBillingPeriod());
        forecast.setForecastPeriodStart(entity.getForecastPeriodStart());
        forecast.setForecastPeriodEnd(entity.getForecastPeriodEnd());
        forecast.setPredictedCost(entity.getPredictedCost());
        forecast.setLowerBound(entity.getLowerBound());
        forecast.setUpperBound(entity.getUpperBound());
        forecast.setTrendDirection(entity.getTrendDirection());
        forecast.setConfidenceLevel(entity.getConfidenceLevel());
        forecast.setCalculatedAt(entity.getCalculatedAt());
        forecast.setForecastModel(entity.getForecastModel());
        return forecast;
    }

    private com.droid.bss.infrastructure.database.entity.CostForecastEntity toEntity(CostForecast domain) {
        if (domain == null) {
            return null;
        }
        var entity = new com.droid.bss.infrastructure.database.entity.CostForecastEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setResourceType(domain.getResourceType());
        entity.setBillingPeriod(domain.getBillingPeriod());
        entity.setForecastPeriodStart(domain.getForecastPeriodStart());
        entity.setForecastPeriodEnd(domain.getForecastPeriodEnd());
        entity.setPredictedCost(domain.getPredictedCost());
        entity.setLowerBound(domain.getLowerBound());
        entity.setUpperBound(domain.getUpperBound());
        entity.setTrendDirection(domain.getTrendDirection());
        entity.setConfidenceLevel(domain.getConfidenceLevel());
        entity.setCalculatedAt(domain.getCalculatedAt());
        entity.setForecastModel(domain.getForecastModel());
        return entity;
    }
}
