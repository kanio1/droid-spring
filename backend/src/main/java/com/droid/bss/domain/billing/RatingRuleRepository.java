package com.droid.bss.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RatingRuleEntity
 */
@Repository
public interface RatingRuleRepository extends JpaRepository<RatingRuleEntity, String> {

    /**
     * Find active rating rules for a date
     */
    @Query("SELECT r FROM RatingRuleEntity r WHERE r.active = true " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
           "ORDER BY r.name")
    List<RatingRuleEntity> findActiveOnDate(@Param("date") LocalDate date);

    /**
     * Find rating rules by usage type
     */
    @Query("SELECT r FROM RatingRuleEntity r WHERE r.usageType = :usageType AND r.active = true " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)")
    List<RatingRuleEntity> findByUsageTypeAndDate(
            @Param("usageType") UsageType usageType,
            @Param("date") LocalDate date
    );

    /**
     * Find rating rules matching usage details
     */
    @Query("SELECT r FROM RatingRuleEntity r WHERE r.active = true " +
           "AND r.usageType = :usageType " +
           "AND (r.destinationType IS NULL OR r.destinationType = :destinationType) " +
           "AND (r.ratePeriod IS NULL OR r.ratePeriod = :ratePeriod) " +
           "AND r.effectiveFrom <= :date " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)")
    List<RatingRuleEntity> findMatchingRules(
            @Param("usageType") UsageType usageType,
            @Param("destinationType") DestinationType destinationType,
            @Param("ratePeriod") RatePeriod ratePeriod,
            @Param("date") LocalDate date
    );

    /**
     * Find by rule code
     */
    Optional<RatingRuleEntity> findByRuleCode(String ruleCode);

    /**
     * Check if rule code exists
     */
    @Query("SELECT COUNT(r) > 0 FROM RatingRuleEntity r WHERE r.ruleCode = :ruleCode")
    boolean existsByRuleCode(@Param("ruleCode") String ruleCode);
}
