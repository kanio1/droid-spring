package com.droid.bss.infrastructure.workflow;

import com.droid.bss.domain.workflow.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Workflow Repository
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    /**
     * Find workflow by name
     */
    Optional<Workflow> findByName(String name);

    /**
     * Find active workflows by trigger event
     */
    @Query("SELECT w FROM Workflow w WHERE w.triggerEvent = :triggerEvent AND w.active = true ORDER BY w.version DESC")
    List<Workflow> findActiveByTriggerEvent(@Param("triggerEvent") String triggerEvent);

    /**
     * Find workflow by trigger event and name
     */
    @Query("SELECT w FROM Workflow w WHERE w.triggerEvent = :triggerEvent AND w.name = :name AND w.active = true ORDER BY w.version DESC LIMIT 1")
    Optional<Workflow> findByTriggerEventAndName(@Param("triggerEvent") String triggerEvent, @Param("name") String name);

    /**
     * Find all active workflows
     */
    @Query("SELECT w FROM Workflow w WHERE w.active = true ORDER BY w.name")
    List<Workflow> findAllActive();
}
