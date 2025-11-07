package com.droid.bss.domain.product;

import java.util.List;
import java.util.Optional;

/**
 * ProductRepository - DDD Port for Product Aggregate
 * This is the interface (port) that the domain depends on.
 * The implementation will be in the infrastructure layer.
 */
public interface ProductRepository {

    /**
     * Find product by ID
     */
    Optional<Product> findById(ProductId id);

    /**
     * Find product by product code
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * Find all products
     */
    List<Product> findAll();

    /**
     * Find products by status
     */
    List<Product> findByStatus(ProductStatus status);

    /**
     * Find products by category
     */
    List<Product> findByCategory(ProductCategory category);

    /**
     * Find active products
     */
    List<Product> findActiveProducts();

    /**
     * Save product (create or update)
     */
    Product save(Product product);

    /**
     * Delete product by ID
     */
    void deleteById(ProductId id);

    /**
     * Check if product exists by ID
     */
    boolean existsById(ProductId id);

    /**
     * Check if product code exists
     */
    boolean existsByProductCode(String productCode);
}
