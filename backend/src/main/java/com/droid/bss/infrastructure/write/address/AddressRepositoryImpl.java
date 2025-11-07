package com.droid.bss.infrastructure.write.address;

import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of AddressRepository (write-side port)
 */
@Repository
@Transactional
public class AddressRepositoryImpl implements AddressRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Address> findById(AddressId id) {
        AddressEntity entity = entityManager.find(AddressEntity.class, id.value());
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<Address> findByCustomerId(CustomerId customerId) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId.value());
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCustomerIdAndType(CustomerId customerId, AddressType type) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId.value());
        query.setParameter("type", type);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Address> findPrimaryByCustomerId(CustomerId customerId) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.isPrimary = true AND a.deletedAt IS NULL ORDER BY a.createdAt ASC",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId.value());
        query.setMaxResults(1);
        List<AddressEntity> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(toDomain(results.get(0)));
    }

    @Override
    public Optional<Address> findPrimaryByCustomerIdAndType(CustomerId customerId, AddressType type) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.isPrimary = true AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId.value());
        query.setParameter("type", type);
        query.setMaxResults(1);
        List<AddressEntity> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(toDomain(results.get(0)));
    }

    @Override
    public Optional<Address> findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(UUID customerId, AddressType type) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.isPrimary = true AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId);
        query.setParameter("type", type);
        query.setMaxResults(1);
        List<AddressEntity> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(toDomain(results.get(0)));
    }

    @Override
    public List<Address> findByType(AddressType type) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.type = :type AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("type", type);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByStatus(AddressStatus status) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.status = :status AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("status", status);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCountry(Country country) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.country = :country AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("country", country);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCity(String city) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE LOWER(a.city) = LOWER(:city) AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("city", city);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByPostalCode(String postalCode) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.postalCode = :postalCode AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("postalCode", postalCode);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findActiveByCustomerId(CustomerId customerId) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId.value());
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Address save(Address address) {
        AddressEntity entity = toEntity(address);
        AddressEntity merged;

        // Check if this is a new entity or existing update
        if (entity.getId() == null) {
            // New entity - persist
            entityManager.persist(entity);
            merged = entity;
        } else {
            // Existing entity - merge
            merged = entityManager.merge(entity);
        }

        entityManager.flush();
        return toDomain(merged);
    }

    @Override
    public void deleteById(AddressId id) {
        AddressEntity entity = entityManager.find(AddressEntity.class, id.value());
        if (entity != null) {
            entity.setDeletedAt(java.time.LocalDateTime.now());
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
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.deletedAt IS NULL",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();
        return count != null ? count : 0;
    }

    @Override
    public long countActiveByCustomerId(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE' AND a.deletedAt IS NULL",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();
        return count != null ? count : 0;
    }

    @Override
    public boolean hasPrimaryAddress(CustomerId customerId) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.isPrimary = true AND a.deletedAt IS NULL",
            Long.class
        ).setParameter("customerId", customerId.value())
         .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public boolean hasPrimaryAddressOfType(CustomerId customerId, AddressType type) {
        Long count = entityManager.createQuery(
            "SELECT COUNT(a) FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.isPrimary = true AND a.deletedAt IS NULL",
            Long.class
        ).setParameter("customerId", customerId.value())
         .setParameter("type", type)
         .getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public List<Address> findByCustomerIdAndTypeAndDeletedAtIsNull(UUID customerId, AddressType type, org.springframework.data.domain.Pageable pageable) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.type = :type AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId);
        query.setParameter("type", type);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCustomerIdAndStatusAndDeletedAtIsNull(UUID customerId, AddressStatus status, org.springframework.data.domain.Pageable pageable) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.status = :status AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId);
        query.setParameter("status", status);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCustomerIdAndDeletedAtIsNull(UUID customerId) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("customerId", customerId);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> searchByTerm(String term) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE (LOWER(a.street) LIKE LOWER(:term) OR LOWER(a.city) LIKE LOWER(:term) OR a.postalCode LIKE :term) AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("term", "%" + term + "%");
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByTypeAndStatusAndDeletedAtIsNull(AddressType type, AddressStatus status, org.springframework.data.domain.Pageable pageable) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.type = :type AND a.status = :status AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("type", type);
        query.setParameter("status", status);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findByCountryAndDeletedAtIsNull(Country country) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.country = :country AND a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setParameter("country", country);
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Address> findAll(org.springframework.data.domain.Pageable pageable) {
        TypedQuery<AddressEntity> query = entityManager.createQuery(
            "SELECT a FROM AddressEntity a WHERE a.deletedAt IS NULL",
            AddressEntity.class
        );
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return query.getResultList().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // Helper methods for mapping between Entity and Domain
    private Address toDomain(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        // Use the restore method pattern from Payment
        return Address.restore(
            entity.getId(),
            entity.getCustomer().getId(),
            entity.getType(),
            entity.getStatus(),
            entity.getStreet(),
            entity.getHouseNumber(),
            entity.getApartmentNumber(),
            entity.getPostalCode(),
            entity.getCity(),
            entity.getRegion(),
            entity.getCountry(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getIsPrimary() != null && entity.getIsPrimary(),
            entity.getNotes(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion().intValue()
        );
    }

    private AddressEntity toEntity(Address address) {
        if (address == null) {
            return null;
        }

        AddressEntity entity = new AddressEntity();
        entity.setId(address.getId().value());
        entity.setType(address.getType());
        entity.setStatus(address.getStatus());
        entity.setStreet(address.getStreet());
        entity.setHouseNumber(address.getHouseNumber());
        entity.setApartmentNumber(address.getApartmentNumber());
        entity.setPostalCode(address.getPostalCode());
        entity.setCity(address.getCity());
        entity.setRegion(address.getRegion());
        entity.setCountry(address.getCountry());
        entity.setLatitude(address.getLatitude());
        entity.setLongitude(address.getLongitude());
        entity.setIsPrimary(address.isPrimary());
        entity.setNotes(address.getNotes());
        entity.setCreatedAt(address.getCreatedAt());
        entity.setUpdatedAt(address.getUpdatedAt());
        entity.setVersion((long) address.getVersion());

        // Load customer reference
        if (address.getCustomerId() != null) {
            CustomerEntity customer = entityManager.find(CustomerEntity.class, address.getCustomerId().value());
            entity.setCustomer(customer);
        }

        return entity;
    }
}
