package com.droid.bss.application.query.monitoring;

import com.droid.bss.domain.monitoring.OptimizationRecommendation;
import com.droid.bss.domain.monitoring.OptimizationRecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetOptimizationRecommendationsUseCase {

    private final OptimizationRecommendationRepository optimizationRecommendationRepository;

    public GetOptimizationRecommendationsUseCase(OptimizationRecommendationRepository optimizationRecommendationRepository) {
        this.optimizationRecommendationRepository = optimizationRecommendationRepository;
    }

    public List<OptimizationRecommendation> getByCustomerId(Long customerId) {
        return optimizationRecommendationRepository.findByCustomerId(customerId);
    }

    public List<OptimizationRecommendation> getByCustomerIdAndStatus(Long customerId, String status) {
        return optimizationRecommendationRepository.findByCustomerIdAndStatus(customerId, status);
    }

    public List<OptimizationRecommendation> getByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return optimizationRecommendationRepository.findByCustomerIdAndResourceType(customerId, resourceType);
    }

    public List<OptimizationRecommendation> getByCustomerIdAndSeverity(Long customerId, String severity) {
        return optimizationRecommendationRepository.findByCustomerIdAndSeverity(customerId, severity);
    }

    public List<OptimizationRecommendation> getByStatus(String status) {
        return optimizationRecommendationRepository.findByStatus(status);
    }
}
