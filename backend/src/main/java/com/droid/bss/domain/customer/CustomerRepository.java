package com.droid.bss.domain.customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    
    /**
     * Save customer (create or update)
     */
    Customer save(Customer customer);
    
    /**
     * Find customer by ID
     */
    Optional<Customer> findById(CustomerId customerId);
    
    /**
     * Find customer by PESEL
     */
    Optional<Customer> findByPesel(String pesel);
    
    /**
     * Find customer by NIP
     */
    Optional<Customer> findByNip(String nip);
    
    /**
     * Find all customers with pagination
     */
    List<Customer> findAll(int page, int size);
    
    /**
     * Find customers by status
     */
    List<Customer> findByStatus(CustomerStatus status, int page, int size);
    
    /**
     * Search customers by name or email
     */
    List<Customer> search(String searchTerm, int page, int size);
    
    /**
     * Check if customer exists by PESEL
     */
    boolean existsByPesel(String pesel);
    
    /**
     * Check if customer exists by NIP
     */
    boolean existsByNip(String nip);
    
    /**
     * Count all customers
     */
    long count();
    
    /**
     * Count customers by status
     */
    long countByStatus(CustomerStatus status);
    
    /**
     * Delete customer by ID
     */
    boolean deleteById(CustomerId customerId);

    /**
     * Delete all customers
     */
    void deleteAll();
}
