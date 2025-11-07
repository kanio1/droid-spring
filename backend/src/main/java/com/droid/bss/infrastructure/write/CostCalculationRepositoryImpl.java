package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.monitoring.CostCalculation;
import com.droid.bss.domain.monitoring.CostCalculationRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CostCalculationRepositoryImpl implements CostCalculationRepository {

    private final SpringDataCostCalculationRepository springDataRepository;

    public CostCalculationRepositoryImpl(SpringDataCostCalculationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CostCalculation> findById(Long id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CostCalculation> findByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate) {
        return springDataRepository.findByCustomerIdAndPeriod(customerId, startDate, endDate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CostCalculation> findByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return springDataRepository.findByCustomerIdAndResourceType(customerId, resourceType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CostCalculation> findByStatus(String status) {
        return springDataRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CostCalculation save(CostCalculation calculation) {
        var entity = toEntity(calculation);
        var savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public void deleteByCustomerIdAndPeriod(Long customerId, Instant startDate, Instant endDate) {
        springDataRepository.deleteByCustomerIdAndPeriod(customerId, startDate, endDate);
    }

    private CostCalculation toDomain(com.droid.bss.infrastructure.database.entity.CostCalculationEntity entity) {
        if (entity == null) {
            return null;
        }
        var calculation = new CostCalculation();
        calculation.setId(entity.getId());
        calculation.setCustomerId(entity.getCustomerId());
        calculation.setResourceType(entity.getResourceType());
        calculation.setBillingPeriod(entity.getBillingPeriod());
        calculation.setPeriodStart(entity.getPeriodStart());
        calculation.setPeriodEnd(entity.getPeriodEnd());
        calculation.setTotalUsage(entity.getTotalUsage());
        calculation.setBaseCost(entity.getBaseCost());
        calculation.setOverageCost(entity.getOverageCost());
        calculation.setTotalCost(entity.getTotalCost());
        calculation.setCurrency(entity.getCurrency());
        calculation.setStatus(entity.getStatus());
        calculation.setCalculatedAt(entity.getCalculatedAt());
        if (entity.getCostModel() != null) {
            calculation.setCostModel(toCostModelDomain(entity.getCostModel()));
        }
        return calculation;
    }

    private com.droid.bss.infrastructure.database.entity.CostCalculationEntity toEntity(CostCalculation domain) {
        if (domain == null) {
            return null;
        }
        var entity = new com.droid.bss.infrastructure.database.entity.CostCalculationEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setResourceType(domain.getResourceType());
        entity.setBillingPeriod(domain.getBillingPeriod());
        entity.setPeriodStart(domain.getPeriodStart());
        entity.setPeriodEnd(domain.getPeriodEnd());
        entity.setTotalUsage(domain.getTotalUsage());
        entity.setBaseCost(domain.getBaseCost());
        entity.setOverageCost(domain.getOverageCost());
        entity.setTotalCost(domain.getTotalCost());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus());
        entity.setCalculatedAt(domain.getCalculatedAt());
        if (domain.getCostModel() != null) {
            entity.setCostModel(toCostModelEntity(domain.getCostModel()));
        }
        return entity;
    }

    private com.droid.bss.domain.monitoring.CostModel toCostModelDomain(
            com.droid.bss.infrastructure.database.entity.CostModelEntity entity) {
        if (entity == null) {
            return null;
        }
        var model = new com.droid.bss.domain.monitoring.CostModel();
        model.setId(entity.getId());
        model.setModelName(entity.getModelName());
        model.setDescription(entity.getDescription());
        model.setBillingPeriod(entity.getBillingPeriod());
        model.setBaseCost(entity.getBaseCost());
        model.setOverageRate(entity.getOverageRate());
        model.setIncludedUsage(entity.getIncludedUsage());
        model.setCurrency(entity.getCurrency());
        model.setActive(entity.isActive());
        model.setCreatedAt(entity.getCreatedAt());
        model.setUpdatedAt(entity.getUpdatedAt());
        return model;
    }

    private com.droid.bss.infrastructure.database.entity.CostModelEntity toCostModelEntity(
            com.droid.bss.domain.monitoring.CostModel domain) {
        if (domain == null) {
            return null;
        }
        var entity = new com.droid.bss.infrastructure.database.entity.CostModelEntity();
        entity.setId(domain.getId());
        entity.setModelName(domain.getModelName());
        entity.setDescription(domain.getDescription());
        entity.setBillingPeriod(domain.getBillingPeriod());
        entity.setBaseCost(domain.getBaseCost());
        entity.setOverageRate(domain.getOverageRate());
        entity.setIncludedUsage(domain.getIncludedUsage());
        entity.setCurrency(domain.getCurrency());
        entity.setActive(domain.isActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
