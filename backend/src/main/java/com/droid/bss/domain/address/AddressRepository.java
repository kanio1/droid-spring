package com.droid.bss.domain.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Address entity
 */
@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {

    /**
     * Find all addresses for a customer (excluding deleted)
     */
    List<AddressEntity> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    /**
     * Find active addresses for a customer
     */
    List<AddressEntity> findByCustomerIdAndStatusAndDeletedAtIsNull(UUID customerId, AddressStatus status);

    /**
     * Find active addresses for a customer (with pagination)
     */
    Page<AddressEntity> findByCustomerIdAndStatusAndDeletedAtIsNull(UUID customerId, AddressStatus status, Pageable pageable);

    /**
     * Find addresses by type for a customer
     */
    List<AddressEntity> findByCustomerIdAndTypeAndDeletedAtIsNull(UUID customerId, AddressType type);

    /**
     * Find addresses by type for a customer (with pagination)
     */
    Page<AddressEntity> findByCustomerIdAndTypeAndDeletedAtIsNull(UUID customerId, AddressType type, Pageable pageable);

    /**
     * Find primary address for a customer and type
     */
    Optional<AddressEntity> findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(UUID customerId, AddressType type);

    /**
     * Find primary address for a customer (any type)
     */
    @Query("SELECT a FROM AddressEntity a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.isPrimary = true " +
           "AND a.deletedAt IS NULL " +
           "ORDER BY a.createdAt ASC")
    Optional<AddressEntity> findPrimaryAddressByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find addresses by country
     */
    List<AddressEntity> findByCountryAndDeletedAtIsNull(Country country);

    /**
     * Find addresses by postal code
     */
    List<AddressEntity> findByPostalCodeAndDeletedAtIsNull(String postalCode);

    /**
     * Find all primary addresses (excluding deleted)
     */
    @Query("SELECT a FROM AddressEntity a WHERE a.isPrimary = true AND a.deletedAt IS NULL")
    List<AddressEntity> findAllPrimaryAddresses();

    /**
     * Find active addresses with geographic coordinates
     */
    @Query("SELECT a FROM AddressEntity a " +
           "WHERE a.status = 'ACTIVE' " +
           "AND a.deletedAt IS NULL " +
           "AND a.latitude IS NOT NULL " +
           "AND a.longitude IS NOT NULL")
    List<AddressEntity> findActiveAddressesWithCoordinates();

    /**
     * Find addresses by city
     */
    List<AddressEntity> findByCityAndDeletedAtIsNull(String city);

    /**
     * Find addresses by type and status
     */
    List<AddressEntity> findByTypeAndStatusAndDeletedAtIsNull(AddressType type, AddressStatus status);

    /**
     * Find addresses by type and status (with pagination)
     */
    Page<AddressEntity> findByTypeAndStatusAndDeletedAtIsNull(AddressType type, AddressStatus status, Pageable pageable);

    /**
     * Check if customer has any active addresses
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AddressEntity a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.status = 'ACTIVE' " +
           "AND a.deletedAt IS NULL")
    boolean existsActiveAddressesByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Count addresses by type for a customer
     */
    @Query("SELECT COUNT(a) FROM AddressEntity a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.type = :type " +
           "AND a.deletedAt IS NULL")
    long countByCustomerIdAndType(@Param("customerId") UUID customerId, @Param("type") AddressType type);

    /**
     * Find address by ID (excluding deleted)
     */
    Optional<AddressEntity> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * Find all non-deleted addresses (active and inactive)
     */
    @Query("SELECT a FROM AddressEntity a WHERE a.deletedAt IS NULL")
    List<AddressEntity> findAllNonDeleted();

    /**
     * Find addresses with search term (street, city, region)
     */
    @Query("SELECT a FROM AddressEntity a " +
           "WHERE a.deletedAt IS NULL " +
           "AND (LOWER(a.street) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(a.region) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<AddressEntity> searchByTerm(@Param("searchTerm") String searchTerm);

    /**
     * Count all non-deleted addresses
     */
    @Query("SELECT COUNT(a) FROM AddressEntity a WHERE a.deletedAt IS NULL")
    long countAll();

    /**
     * Count addresses by status
     */
    @Query("SELECT COUNT(a) FROM AddressEntity a WHERE a.status = :status AND a.deletedAt IS NULL")
    long countByStatus(@Param("status") AddressStatus status);
}
