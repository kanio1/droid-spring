package com.droid.bss.domain.monitoring;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for OptimizationRecommendation
 */
public interface OptimizationRecommendationRepository {

    Optional<OptimizationRecommendation> findById(Long id);

    List<OptimizationRecommendation> findByCustomerId(Long customerId);

    List<OptimizationRecommendation> findByCustomerIdAndStatus(Long customerId, String status);

    List<OptimizationRecommendation> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    List<OptimizationRecommendation> findByCustomerIdAndSeverity(Long customerId, String severity);

    List<OptimizationRecommendation> findByStatus(String status);

    OptimizationRecommendation save(OptimizationRecommendation recommendation);

    void deleteById(Long id);

    void deleteByCustomerIdAndResourceType(Long customerId, String resourceType);
}
