package com.droid.bss.domain.product.repository;

import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductCategory;
import com.droid.bss.domain.product.ProductStatus;
import com.droid.bss.domain.product.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProductEntity
 */
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    /**
     * Find product by product code
     */
    Optional<ProductEntity> findByProductCode(String productCode);

    /**
     * Find products by status
     */
    Page<ProductEntity> findByStatus(ProductStatus status, Pageable pageable);

    /**
     * Find products by type
     */
    Page<ProductEntity> findByProductType(ProductType productType, Pageable pageable);

    /**
     * Find products by category
     */
    Page<ProductEntity> findByCategory(ProductCategory category, Pageable pageable);

    /**
     * Find active products (status = ACTIVE and within validity period)
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.status = :status " +
           "AND (p.validityStart IS NULL OR p.validityStart <= CURRENT_DATE) " +
           "AND (p.validityEnd IS NULL OR p.validityEnd >= CURRENT_DATE)")
    Page<ProductEntity> findActiveProducts(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * Search products by name or description
     */
    @Query("SELECT p FROM ProductEntity p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ProductEntity> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find products by status with features (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"features"})
    @Query("SELECT p FROM ProductEntity p WHERE p.status = :status")
    Page<ProductEntity> findByStatusWithFeatures(@Param("status") ProductStatus status, Pageable pageable);

    /**
     * Find all products with features (optimized to avoid N+1 queries)
     */
    @EntityGraph(attributePaths = {"features"})
    @Query("SELECT p FROM ProductEntity p")
    Page<ProductEntity> findAllWithFeatures(Pageable pageable);

    /**
     * Find products by price range
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<ProductEntity> findByPriceRange(@Param("minPrice") Double minPrice,
                                         @Param("maxPrice") Double maxPrice,
                                         Pageable pageable);

    /**
     * Find products expiring within given days
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.validityEnd IS NOT NULL " +
           "AND p.validityEnd BETWEEN CURRENT_DATE AND :endDate")
    List<ProductEntity> findExpiringProducts(@Param("endDate") LocalDate endDate);

    /**
     * Check if product code exists
     */
    boolean existsByProductCode(String productCode);

    /**
     * Find products by feature key
     */
    @Query("SELECT p FROM ProductEntity p JOIN p.features f WHERE f.featureKey = :featureKey")
    List<ProductEntity> findByFeatureKey(@Param("featureKey") String featureKey);

    /**
     * Count products by status
     */
    long countByStatus(ProductStatus status);

    /**
     * Find products by multiple types
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.productType IN :types")
    Page<ProductEntity> findByProductTypeIn(@Param("types") List<ProductType> types, Pageable pageable);

    /**
     * Find products with specific billing period
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.billingPeriod = :billingPeriod")
    Page<ProductEntity> findByBillingPeriod(@Param("billingPeriod") String billingPeriod, Pageable pageable);
}
