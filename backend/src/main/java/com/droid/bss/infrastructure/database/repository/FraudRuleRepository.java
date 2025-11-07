package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.fraud.FraudRuleEntity;
import com.droid.bss.domain.fraud.FraudRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for fraud rules
 */
@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRuleEntity, String> {

    /**
     * Find by rule code
     */
    FraudRuleEntity findByRuleCode(String ruleCode);

    /**
     * Find enabled rules
     */
    @Query("SELECT r FROM FraudRuleEntity r WHERE r.enabled = true ORDER BY r.priority ASC")
    List<FraudRuleEntity> findEnabledRules();

    /**
     * Find by rule type
     */
    List<FraudRuleEntity> findByRuleType(FraudRuleType ruleType);

    /**
     * Find by category
     */
    List<FraudRuleEntity> findByCategory(String category);

    /**
     * Find by source entity
     */
    List<FraudRuleEntity> findBySourceEntity(String sourceEntity);

    /**
     * Find rules that require review
     */
    @Query("SELECT r FROM FraudRuleEntity r WHERE r.requiresReview = true AND r.enabled = true")
    List<FraudRuleEntity> findRulesRequiringReview();

    /**
     * Count enabled rules
     */
    @Query("SELECT COUNT(r) FROM FraudRuleEntity r WHERE r.enabled = true")
    long countEnabledRules();

    /**
     * Find rules by priority range
     */
    @Query("SELECT r FROM FraudRuleEntity r WHERE r.priority BETWEEN :minPriority AND :maxPriority AND r.enabled = true")
    List<FraudRuleEntity> findByPriorityRange(@Param("minPriority") int minPriority, @Param("maxPriority") int maxPriority);
}
