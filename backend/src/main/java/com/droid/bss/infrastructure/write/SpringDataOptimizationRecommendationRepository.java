package com.droid.bss.infrastructure.write;

import com.droid.bss.infrastructure.database.entity.OptimizationRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataOptimizationRecommendationRepository extends JpaRepository<OptimizationRecommendationEntity, Long> {

    List<OptimizationRecommendationEntity> findByCustomerId(Long customerId);

    List<OptimizationRecommendationEntity> findByCustomerIdAndStatus(Long customerId, String status);

    List<OptimizationRecommendationEntity> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    List<OptimizationRecommendationEntity> findByCustomerIdAndSeverity(Long customerId, String severity);

    List<OptimizationRecommendationEntity> findByStatus(String status);

    void deleteByCustomerIdAndResourceType(Long customerId, String resourceType);
}
