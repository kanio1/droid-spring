package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.monitoring.CostModel;
import com.droid.bss.domain.monitoring.CostModelRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CostModelRepositoryImpl implements CostModelRepository {

    private final SpringDataCostModelRepository springDataRepository;

    public CostModelRepositoryImpl(SpringDataCostModelRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CostModel> findById(Long id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CostModel> findByModelName(String modelName) {
        return springDataRepository.findByModelName(modelName).map(this::toDomain);
    }

    @Override
    public List<CostModel> findByActiveTrue() {
        return springDataRepository.findByActiveTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CostModel save(CostModel costModel) {
        var entity = toEntity(costModel);
        var savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    private CostModel toDomain(com.droid.bss.infrastructure.database.entity.CostModelEntity entity) {
        if (entity == null) {
            return null;
        }
        CostModel model = new CostModel();
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

    private com.droid.bss.infrastructure.database.entity.CostModelEntity toEntity(CostModel domain) {
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
