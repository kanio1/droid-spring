package com.droid.bss.infrastructure.event.sourcing.repository;

import com.droid.bss.infrastructure.event.sourcing.entity.SnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for snapshot entities
 */
@Repository
public interface SnapshotEntityRepository extends JpaRepository<SnapshotEntity, String> {

    /**
     * Find latest snapshot for an aggregate
     */
    Optional<SnapshotEntity> findByAggregateId(String aggregateId);

    /**
     * Check if aggregate has a snapshot
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SnapshotEntity s WHERE s.aggregateId = :aggregateId")
    boolean existsByAggregateId(@Param("aggregateId") String aggregateId);

    /**
     * Delete snapshot for an aggregate
     */
    @Modifying
    @Query("DELETE FROM SnapshotEntity s WHERE s.aggregateId = :aggregateId")
    void deleteByAggregateId(@Param("aggregateId") String aggregateId);
}
