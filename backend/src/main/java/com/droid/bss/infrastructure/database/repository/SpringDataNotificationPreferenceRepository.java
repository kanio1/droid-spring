package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.infrastructure.database.entity.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data repository for NotificationPreference
 */
@Repository
public interface SpringDataNotificationPreferenceRepository
        extends JpaRepository<NotificationPreferenceEntity, Long> {

    Optional<NotificationPreferenceEntity> findByCustomerId(Long customerId);
}
