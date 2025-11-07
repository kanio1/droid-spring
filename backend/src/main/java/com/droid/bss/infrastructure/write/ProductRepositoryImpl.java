package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.product.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class ProductRepositoryImpl implements ProductRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public ProductRepositoryImpl() {}

    @Override
    public Product save(Product product) {
        ProductEntity entity = ProductEntity.from(product);

        // Check if this is a new entity
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM ProductEntity p WHERE p.id = :id",
            Long.class
        ).setParameter("id", entity.getId())
         .getSingleResult();

        boolean exists = count != null && count > 0;

        // Save using merge
        ProductEntity merged = entityManager.merge(entity);
        entityManager.flush();

        // Return as domain
        return merged.toDomain();
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        ProductEntity entity = entityManager.find(ProductEntity.class, id.value());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public Optional<Product> findByProductCode(String productCode) {
        try {
            ProductEntity entity = entityManager.createQuery(
                "SELECT p FROM ProductEntity p WHERE p.productCode = :productCode",
                ProductEntity.class
            ).setParameter("productCode", productCode)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Product> findAll() {
        List<ProductEntity> entities = entityManager.createQuery(
            "SELECT p FROM ProductEntity p ORDER BY p.createdAt DESC",
            ProductEntity.class
        ).getResultList();

        return entities.stream()
            .map(ProductEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        List<ProductEntity> entities = entityManager.createQuery(
            "SELECT p FROM ProductEntity p WHERE p.status = :status ORDER BY p.createdAt DESC",
            ProductEntity.class
        ).setParameter("status", status)
         .getResultList();

        return entities.stream()
            .map(ProductEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByCategory(ProductCategory category) {
        List<ProductEntity> entities = entityManager.createQuery(
            "SELECT p FROM ProductEntity p WHERE p.category = :category ORDER BY p.createdAt DESC",
            ProductEntity.class
        ).setParameter("category", category)
         .getResultList();

        return entities.stream()
            .map(ProductEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Product> findActiveProducts() {
        List<ProductEntity> entities = entityManager.createQuery(
            "SELECT p FROM ProductEntity p WHERE p.status = 'ACTIVE' ORDER BY p.createdAt DESC",
            ProductEntity.class
        ).getResultList();

        return entities.stream()
            .map(ProductEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(ProductId id) {
        ProductEntity entity = entityManager.find(ProductEntity.class, id.value());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public boolean existsById(ProductId id) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM ProductEntity p WHERE p.id = :id",
            Long.class
        ).setParameter("id", id.value())
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(p) FROM ProductEntity p WHERE p.productCode = :productCode",
            Long.class
        ).setParameter("productCode", productCode)
         .getSingleResult();

        return count != null && count > 0;
    }
}
