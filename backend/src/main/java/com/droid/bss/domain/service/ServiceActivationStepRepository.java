package com.droid.bss.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ServiceActivationStepEntity
 */
@Repository
public interface ServiceActivationStepRepository extends JpaRepository<ServiceActivationStepEntity, String> {

    /**
     * Find steps for an activation ordered by step order
     */
    @Query("SELECT s FROM ServiceActivationStepEntity s WHERE s.activation = :activation ORDER BY s.stepOrder ASC")
    List<ServiceActivationStepEntity> findByActivationOrderByStepOrder(@Param("activation") ServiceActivationEntity activation);

    /**
     * Find pending steps
     */
    @Query("SELECT s FROM ServiceActivationStepEntity s WHERE s.status = 'PENDING' ORDER BY s.createdAt ASC")
    List<ServiceActivationStepEntity> findPending();

    /**
     * Find steps by status
     */
    @Query("SELECT s FROM ServiceActivationStepEntity s WHERE s.status = :status")
    List<ServiceActivationStepEntity> findByStatus(@Param("status") ServiceActivationStepStatus status);

    /**
     * Find failed steps that can be retried
     */
    @Query("SELECT s FROM ServiceActivationStepEntity s WHERE s.status = 'FAILED' AND s.retryCount < s.maxRetries")
    List<ServiceActivationStepEntity> findFailedButRetryable();
}
