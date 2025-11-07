package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@Transactional
public class AddressRepositoryImpl implements AddressRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public AddressRepositoryImpl() {}

    @Override
    public Address save(Address address) {
        AddressEntity entity = AddressEntity.from(address);

        // Check if this is a new entity
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.id = :id",
            Long.class
        ).setParameter("id", entity.getId())
         .getSingleResult();

        boolean exists = count != null && count > 0;

        // Save using merge
        AddressEntity merged = entityManager.merge(entity);
        entityManager.flush();

        // Return as domain
        return merged.toDomain();
    }

    @Override
    public Optional<Address> findById(AddressId id) {
        AddressEntity entity = entityManager.find(AddressEntity.class, id.value());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<Address> findByCustomerId(CustomerId customerId) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCustomerIdAndType(CustomerId customerId, AddressType type) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("customerId", customerId.value())
         .setParameter("type", type)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findPrimaryByCustomerId(CustomerId customerId) {
        try {
            AddressEntity entity = entityManager.createQuery(
                "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.isPrimary = true ORDER BY a.createdAt DESC",
                AddressEntity.class
            ).setParameter("customerId", customerId.value())
             .setMaxResults(1)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Address> findPrimaryByCustomerIdAndType(CustomerId customerId, AddressType type) {
        try {
            AddressEntity entity = entityManager.createQuery(
                "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.isPrimary = true ORDER BY a.createdAt DESC",
                AddressEntity.class
            ).setParameter("customerId", customerId.value())
             .setParameter("type", type)
             .setMaxResults(1)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Address> findByType(AddressType type) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.type = :type ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("type", type)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByStatus(AddressStatus status) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.status = :status ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("status", status)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCountry(Country country) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.country = :country ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("country", country)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCity(String city) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.city = :city ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("city", city)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByPostalCode(String postalCode) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.postalCode = :postalCode ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("postalCode", postalCode)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findActiveByCustomerId(CustomerId customerId) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE' ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("customerId", customerId.value())
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(AddressId id) {
        AddressEntity entity = entityManager.find(AddressEntity.class, id.value());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    public boolean existsById(AddressId id) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.id = :id",
            Long.class
        ).setParameter("id", id.value())
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public long countByCustomerId(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();

        return count != null ? count : 0;
    }

    @Override
    public long countActiveByCustomerId(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE'",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();

        return count != null ? count : 0;
    }

    @Override
    public boolean hasPrimaryAddress(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.isPrimary = true",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();

        return count != null && count > 0;
    }

    @Override
    public boolean hasPrimaryAddressOfType(CustomerId customerId, AddressType type) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.isPrimary = true",
            Long.class
        ).setParameter("customerId", customerId.value())
         .setParameter("type", type)
         .getSingleResult();

        return count != null && count > 0;
    }

    // Legacy method for backward compatibility
    @Override
    public Optional<Address> findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(UUID customerId, AddressType type) {
        try {
            AddressEntity entity = entityManager.createQuery(
                "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.isPrimary = true AND a.deletedAt IS NULL ORDER BY a.createdAt DESC",
                AddressEntity.class
            ).setParameter("customerId", customerId)
             .setParameter("type", type)
             .setMaxResults(1)
             .getSingleResult();

            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Address> findByCustomerIdAndTypeAndDeletedAtIsNull(UUID customerId, AddressType type, Pageable pageable) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.deletedAt IS NULL ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("customerId", customerId)
         .setParameter("type", type)
         .setFirstResult((int) pageable.getOffset())
         .setMaxResults(pageable.getPageSize())
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCustomerIdAndStatusAndDeletedAtIsNull(UUID customerId, AddressStatus status, Pageable pageable) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.status = :status AND a.deletedAt IS NULL ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("customerId", customerId)
         .setParameter("status", status)
         .setFirstResult((int) pageable.getOffset())
         .setMaxResults(pageable.getPageSize())
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCustomerIdAndDeletedAtIsNull(UUID customerId) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.deletedAt IS NULL ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("customerId", customerId)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> searchByTerm(String term) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.street LIKE :term OR a.city LIKE :term OR a.postalCode LIKE :term ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("term", "%" + term + "%")
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByTypeAndStatusAndDeletedAtIsNull(AddressType type, AddressStatus status, Pageable pageable) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.type = :type AND a.status = :status AND a.deletedAt IS NULL ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("type", type)
         .setParameter("status", status)
         .setFirstResult((int) pageable.getOffset())
         .setMaxResults(pageable.getPageSize())
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCountryAndDeletedAtIsNull(Country country) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.country = :country AND a.deletedAt IS NULL ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setParameter("country", country)
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findAll(Pageable pageable) {
        List<AddressEntity> entities = entityManager.createQuery(
            "SELECT a FROM AddressEntity a ORDER BY a.createdAt DESC",
            AddressEntity.class
        ).setFirstResult((int) pageable.getOffset())
         .setMaxResults(pageable.getPageSize())
         .getResultList();

        return entities.stream()
            .map(AddressEntity::toDomain)
            .collect(Collectors.toList());
    }
}
