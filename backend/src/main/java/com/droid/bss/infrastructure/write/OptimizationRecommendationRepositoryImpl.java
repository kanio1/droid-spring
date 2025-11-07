package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.monitoring.OptimizationRecommendation;
import com.droid.bss.domain.monitoring.OptimizationRecommendationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OptimizationRecommendationRepositoryImpl implements OptimizationRecommendationRepository {

    private final SpringDataOptimizationRecommendationRepository springDataRepository;

    public OptimizationRecommendationRepositoryImpl(SpringDataOptimizationRecommendationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<OptimizationRecommendation> findById(Long id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<OptimizationRecommendation> findByCustomerId(Long customerId) {
        return springDataRepository.findByCustomerId(customerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationRecommendation> findByCustomerIdAndStatus(Long customerId, String status) {
        return springDataRepository.findByCustomerIdAndStatus(customerId, status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationRecommendation> findByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return springDataRepository.findByCustomerIdAndResourceType(customerId, resourceType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationRecommendation> findByCustomerIdAndSeverity(Long customerId, String severity) {
        return springDataRepository.findByCustomerIdAndSeverity(customerId, severity).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OptimizationRecommendation> findByStatus(String status) {
        return springDataRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public OptimizationRecommendation save(OptimizationRecommendation recommendation) {
        var entity = toEntity(recommendation);
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

    private OptimizationRecommendation toDomain(com.droid.bss.infrastructure.database.entity.OptimizationRecommendationEntity entity) {
        if (entity == null) {
            return null;
        }
        var rec = new OptimizationRecommendation();
        rec.setId(entity.getId());
        rec.setCustomerId(entity.getCustomerId());
        rec.setResourceType(entity.getResourceType());
        rec.setResourceId(entity.getResourceId());
        rec.setRecommendationType(entity.getRecommendationType());
        rec.setSeverity(entity.getSeverity());
        rec.setTitle(entity.getTitle());
        rec.setDescription(entity.getDescription());
        rec.setPotentialSavings(entity.getPotentialSavings());
        rec.setCurrency(entity.getCurrency());
        rec.setCurrentCost(entity.getCurrentCost());
        rec.setProjectedCost(entity.getProjectedCost());
        rec.setStatus(entity.getStatus());
        rec.setCreatedAt(entity.getCreatedAt());
        rec.setAcknowledgedAt(entity.getAcknowledgedAt());
        rec.setImplementedAt(entity.getImplementedAt());
        rec.setDetails(entity.getDetails());
        return rec;
    }

    private com.droid.bss.infrastructure.database.entity.OptimizationRecommendationEntity toEntity(OptimizationRecommendation domain) {
        if (domain == null) {
            return null;
        }
        var entity = new com.droid.bss.infrastructure.database.entity.OptimizationRecommendationEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setResourceType(domain.getResourceType());
        entity.setResourceId(domain.getResourceId());
        entity.setRecommendationType(domain.getRecommendationType());
        entity.setSeverity(domain.getSeverity());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setPotentialSavings(domain.getPotentialSavings());
        entity.setCurrency(domain.getCurrency());
        entity.setCurrentCost(domain.getCurrentCost());
        entity.setProjectedCost(domain.getProjectedCost());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setAcknowledgedAt(domain.getAcknowledgedAt());
        entity.setImplementedAt(domain.getImplementedAt());
        entity.setDetails(domain.getDetails());
        return entity;
    }
}
