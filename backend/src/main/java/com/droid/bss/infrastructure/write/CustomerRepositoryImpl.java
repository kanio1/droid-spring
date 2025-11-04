package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.customer.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.event.CustomerEventPublisher;
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

@Repository
@Transactional
public class CustomerRepositoryImpl implements CustomerRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final CustomerEventPublisher eventPublisher;

    public CustomerRepositoryImpl(CustomerEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = CustomerEntity.from(customer);
        CustomerStatus previousStatus = entity.getStatus();

        // Check if this is a new entity by querying COUNT (don't load the entity to avoid conflicts)
        Long count = entityManager.createQuery(
            "SELECT COUNT(c) FROM CustomerEntity c WHERE c.id = :id",
            Long.class
        ).setParameter("id", entity.getId())
         .getSingleResult();

        boolean exists = count != null && count > 0;

        // Always use merge to handle both new and existing entities
        CustomerEntity merged = entityManager.merge(entity);
        entityManager.flush();

        // Publish appropriate event based on whether this was new or existing
        if (!exists) {
            // New entity - was created by merge
            eventPublisher.publishCustomerCreated(merged);
        } else {
            // Existing entity - publish events based on changes
            if (!previousStatus.equals(merged.getStatus())) {
                eventPublisher.publishCustomerStatusChanged(merged, previousStatus);
            } else {
                eventPublisher.publishCustomerUpdated(merged);
            }
        }

        return merged.toDomain();
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        CustomerEntity entity = entityManager.find(CustomerEntity.class, customerId.value());
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }
    
    @Override
    public Optional<Customer> findByPesel(String pesel) {
        if (pesel == null) return Optional.empty();
        
        TypedQuery<CustomerEntity> query = entityManager.createQuery(
            "SELECT c FROM CustomerEntity c WHERE c.pesel = :pesel", 
            CustomerEntity.class
        );
        query.setParameter("pesel", pesel);
        
        try {
            CustomerEntity entity = query.getSingleResult();
            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Customer> findByNip(String nip) {
        if (nip == null) return Optional.empty();
        
        TypedQuery<CustomerEntity> query = entityManager.createQuery(
            "SELECT c FROM CustomerEntity c WHERE c.nip = :nip", 
            CustomerEntity.class
        );
        query.setParameter("nip", nip);
        
        try {
            CustomerEntity entity = query.getSingleResult();
            return Optional.of(entity.toDomain());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public List<Customer> findAll(int page, int size) {
        TypedQuery<CustomerEntity> query = entityManager.createQuery(
            "SELECT c FROM CustomerEntity c ORDER BY c.createdAt DESC", 
            CustomerEntity.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList().stream()
                .map(CustomerEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Customer> findByStatus(CustomerStatus status, int page, int size) {
        TypedQuery<CustomerEntity> query = entityManager.createQuery(
            "SELECT c FROM CustomerEntity c WHERE c.status = :status ORDER BY c.createdAt DESC", 
            CustomerEntity.class
        );
        query.setParameter("status", status);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList().stream()
                .map(CustomerEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Customer> search(String searchTerm, int page, int size) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll(page, size);
        }
        
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        
        TypedQuery<CustomerEntity> query = entityManager.createQuery(
            "SELECT c FROM CustomerEntity c WHERE " +
            "LOWER(c.firstName) LIKE :search OR " +
            "LOWER(c.lastName) LIKE :search OR " +
            "LOWER(c.email) LIKE :search OR " +
            "c.pesel LIKE :search OR " +
            "c.nip LIKE :search " +
            "ORDER BY c.createdAt DESC", 
            CustomerEntity.class
        );
        query.setParameter("search", searchPattern);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList().stream()
                .map(CustomerEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByPesel(String pesel) {
        if (pesel == null) return false;
        
        Long count = entityManager.createQuery(
            "SELECT COUNT(c) FROM CustomerEntity c WHERE c.pesel = :pesel", 
            Long.class
        )
        .setParameter("pesel", pesel)
        .getSingleResult();
        
        return count > 0;
    }
    
    @Override
    public boolean existsByNip(String nip) {
        if (nip == null) return false;
        
        Long count = entityManager.createQuery(
            "SELECT COUNT(c) FROM CustomerEntity c WHERE c.nip = :nip", 
            Long.class
        )
        .setParameter("nip", nip)
        .getSingleResult();
        
        return count > 0;
    }
    
    @Override
    public long count() {
        return entityManager.createQuery(
            "SELECT COUNT(c) FROM CustomerEntity c", 
            Long.class
        ).getSingleResult();
    }
    
    @Override
    public long countByStatus(CustomerStatus status) {
        return entityManager.createQuery(
            "SELECT COUNT(c) FROM CustomerEntity c WHERE c.status = :status", 
            Long.class
        )
        .setParameter("status", status)
        .getSingleResult();
    }
    
    @Override
    public boolean deleteById(CustomerId customerId) {
        CustomerEntity entity = entityManager.find(CustomerEntity.class, customerId.value());
        if (entity != null) {
            entityManager.remove(entity);
            entityManager.flush();
            return true;
        }
        return false;
    }

    @Override
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM CustomerEntity c").executeUpdate();
    }
}
