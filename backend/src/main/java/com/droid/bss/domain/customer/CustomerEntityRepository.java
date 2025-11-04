package com.droid.bss.domain.customer;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for CustomerEntity
 */
@Repository
public interface CustomerEntityRepository extends JpaRepository<CustomerEntity, UUID> {

    /**
     * Find customer entity by ID
     */
    Optional<CustomerEntity> findById(UUID id);

    /**
     * Find customer by PESEL
     */
    Optional<CustomerEntity> findByPesel(String pesel);

    /**
     * Find customer by NIP
     */
    Optional<CustomerEntity> findByNip(String nip);

    /**
     * Find all customers with pagination
     */
    @Query("SELECT c FROM CustomerEntity c ORDER BY c.createdAt DESC")
    List<CustomerEntity> findAllWithPagination(Pageable pageable);

    /**
     * Find customers by status with pagination
     */
    @Query("SELECT c FROM CustomerEntity c WHERE c.status = :status ORDER BY c.createdAt DESC")
    List<CustomerEntity> findByStatusWithPagination(@Param("status") CustomerStatus status, Pageable pageable);

    /**
     * Search customers by term
     */
    @Query("SELECT c FROM CustomerEntity c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.pesel LIKE CONCAT('%', :searchTerm, '%') OR " +
           "c.nip LIKE CONCAT('%', :searchTerm, '%') " +
           "ORDER BY c.createdAt DESC")
    List<CustomerEntity> search(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Check if PESEL exists
     */
    boolean existsByPesel(String pesel);

    /**
     * Check if NIP exists
     */
    boolean existsByNip(String nip);

    /**
     * Count customers by status
     */
    long countByStatus(CustomerStatus status);
}
