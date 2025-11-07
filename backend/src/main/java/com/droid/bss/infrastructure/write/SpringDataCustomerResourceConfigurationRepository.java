package com.droid.bss.infrastructure.write;

import com.droid.bss.infrastructure.database.entity.CustomerResourceConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataCustomerResourceConfigurationRepository extends JpaRepository<CustomerResourceConfigurationEntity, Long> {

    List<CustomerResourceConfigurationEntity> findByCustomerId(Long customerId);

    List<CustomerResourceConfigurationEntity> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    List<CustomerResourceConfigurationEntity> findByResourceId(String resourceId);

    List<CustomerResourceConfigurationEntity> findByStatus(String status);

    @Query("DELETE FROM CustomerResourceConfigurationEntity c WHERE c.customerId = :customerId AND c.resourceType = :resourceType")
    void deleteByCustomerIdAndResourceType(
            @Param("customerId") Long customerId,
            @Param("resourceType") String resourceType);
}
