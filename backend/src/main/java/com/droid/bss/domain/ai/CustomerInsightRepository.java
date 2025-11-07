/**
 * Customer Insight Repository
 *
 * Data access layer for AI-generated customer insights
 */

package com.droid.bss.domain.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerInsightRepository extends JpaRepository<CustomerInsight, String> {

    /**
     * Find insights for a specific customer
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.customerId = :customerId AND i.tenantId = :tenantId ORDER BY i.priority DESC, i.createdAt DESC")
    Page<CustomerInsight> findByCustomerId(@Param("customerId") String customerId, @Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find active insights for a customer
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.customerId = :customerId AND i.tenantId = :tenantId AND i.status = 'ACTIVE' AND (i.expiresAt IS NULL OR i.expiresAt > :now) ORDER BY i.priority DESC")
    List<CustomerInsight> findActiveInsightsForCustomer(@Param("customerId") String customerId, @Param("tenantId") String tenantId, @Param("now") LocalDateTime now);

    /**
     * Find insights by type
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.insightType = :type ORDER BY i.createdAt DESC")
    Page<CustomerInsight> findByInsightType(@Param("tenantId") String tenantId, @Param("type") CustomerInsight.InsightType type, Pageable pageable);

    /**
     * Find high-confidence insights
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.confidenceScore >= :minConfidence ORDER BY i.confidenceScore DESC, i.priority DESC")
    List<CustomerInsight> findHighConfidenceInsights(@Param("tenantId") String tenantId, @Param("minConfidence") Double minConfidence);

    /**
     * Find insights by model
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.modelName = :modelName ORDER BY i.createdAt DESC")
    List<CustomerInsight> findByModelName(@Param("tenantId") String tenantId, @Param("modelName") String modelName);

    /**
     * Find expired insights
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.expiresAt IS NOT NULL AND i.expiresAt < :now AND i.status = 'ACTIVE'")
    List<CustomerInsight> findExpiredInsights(@Param("now") LocalDateTime now);

    /**
     * Find insights by priority
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.priority >= :minPriority ORDER BY i.priority DESC, i.createdAt DESC")
    Page<CustomerInsight> findByPriority(@Param("tenantId") String tenantId, @Param("minPriority") Integer minPriority, Pageable pageable);

    /**
     * Count active insights per customer
     */
    @Query("SELECT i.customerId, COUNT(i) FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.status = 'ACTIVE' AND (i.expiresAt IS NULL OR i.expiresAt > :now) GROUP BY i.customerId")
    List<Object[]> countActiveInsightsPerCustomer(@Param("tenantId") String tenantId, @Param("now") LocalDateTime now);

    /**
     * Get insights statistics
     */
    @Query("SELECT i.insightType, COUNT(i), AVG(i.confidenceScore) FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.createdAt >= :since GROUP BY i.insightType")
    List<Object[]> getInsightStatistics(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    /**
     * Mark insight as viewed
     */
    @Modifying
    @Query("UPDATE CustomerInsight i SET i.status = 'VIEWED', i.viewedAt = :viewedAt WHERE i.id = :id")
    void markAsViewed(@Param("id") String id, @Param("viewedAt") LocalDateTime viewedAt);

    /**
     * Mark insight as dismissed
     */
    @Modifying
    @Query("UPDATE CustomerInsight i SET i.status = 'DISMISSED', i.dismissedAt = :dismissedAt WHERE i.id = :id")
    void markAsDismissed(@Param("id") String id, @Param("dismissedAt") LocalDateTime dismissedAt);

    /**
     * Delete expired insights
     */
    @Modifying
    @Query("DELETE FROM CustomerInsight i WHERE i.expiresAt IS NOT NULL AND i.expiresAt < :cutoff")
    void deleteExpired(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Find insights by category
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.category = :category ORDER BY i.priority DESC, i.createdAt DESC")
    Page<CustomerInsight> findByCategory(@Param("tenantId") String tenantId, @Param("category") String category, Pageable pageable);

    /**
     * Check if customer has insights of specific type
     */
    @Query("SELECT COUNT(i) > 0 FROM CustomerInsight i WHERE i.customerId = :customerId AND i.tenantId = :tenantId AND i.insightType = :type AND i.status = 'ACTIVE'")
    boolean hasActiveInsightOfType(@Param("customerId") String customerId, @Param("tenantId") String tenantId, @Param("type") CustomerInsight.InsightType type);

    /**
     * Find recent insights across all customers
     */
    @Query("SELECT i FROM CustomerInsight i WHERE i.tenantId = :tenantId AND i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<CustomerInsight> findRecentInsights(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);
}
