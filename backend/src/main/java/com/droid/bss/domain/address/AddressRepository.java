package com.droid.bss.domain.address;

import com.droid.bss.domain.customer.CustomerId;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AddressRepository - DDD Port for Address Aggregate
 * This is the interface (port) that the domain depends on.
 * The implementation will be in the infrastructure layer.
 */
public interface AddressRepository {

    /**
     * Find address by ID
     */
    Optional<Address> findById(AddressId id);

    /**
     * Find addresses by customer
     */
    List<Address> findByCustomerId(CustomerId customerId);

    /**
     * Find addresses by customer and type
     */
    List<Address> findByCustomerIdAndType(CustomerId customerId, AddressType type);

    /**
     * Find primary address by customer
     */
    Optional<Address> findPrimaryByCustomerId(CustomerId customerId);

    /**
     * Find primary address by customer and type
     */
    Optional<Address> findPrimaryByCustomerIdAndType(CustomerId customerId, AddressType type);

    /**
     * Find addresses by type
     */
    List<Address> findByType(AddressType type);

    /**
     * Find addresses by status
     */
    List<Address> findByStatus(AddressStatus status);

    /**
     * Find addresses by country
     */
    List<Address> findByCountry(Country country);

    /**
     * Find addresses by city
     */
    List<Address> findByCity(String city);

    /**
     * Find addresses by postal code
     */
    List<Address> findByPostalCode(String postalCode);

    /**
     * Find active addresses by customer
     */
    List<Address> findActiveByCustomerId(CustomerId customerId);

    /**
     * Save address (create or update)
     */
    Address save(Address address);

    /**
     * Delete address by ID
     */
    void deleteById(AddressId id);

    /**
     * Check if address exists by ID
     */
    boolean existsById(AddressId id);

    /**
     * Count addresses by customer
     */
    long countByCustomerId(CustomerId customerId);

    /**
     * Count active addresses by customer
     */
    long countActiveByCustomerId(CustomerId customerId);

    /**
     * Check if customer has primary address
     */
    boolean hasPrimaryAddress(CustomerId customerId);

    /**
     * Check if customer has primary address of specific type
     */
    boolean hasPrimaryAddressOfType(CustomerId customerId, AddressType type);

    /**
     * Find primary address by customer and type (legacy method for backward compatibility)
     */
    Optional<Address> findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(UUID customerId, AddressType type);

    /**
     * Find addresses by customer ID and type (with soft delete filter)
     */
    List<Address> findByCustomerIdAndTypeAndDeletedAtIsNull(UUID customerId, AddressType type, Pageable pageable);

    /**
     * Find addresses by customer ID and status (with soft delete filter)
     */
    List<Address> findByCustomerIdAndStatusAndDeletedAtIsNull(UUID customerId, AddressStatus status, Pageable pageable);

    /**
     * Find addresses by customer ID (with soft delete filter)
     */
    List<Address> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    /**
     * Search addresses by term
     */
    List<Address> searchByTerm(String term);

    /**
     * Find addresses by type and status (with soft delete filter)
     */
    List<Address> findByTypeAndStatusAndDeletedAtIsNull(AddressType type, AddressStatus status, Pageable pageable);

    /**
     * Find addresses by country (with soft delete filter)
     */
    List<Address> findByCountryAndDeletedAtIsNull(Country country);

    /**
     * Find all addresses with pagination
     */
    List<Address> findAll(Pageable pageable);
}

