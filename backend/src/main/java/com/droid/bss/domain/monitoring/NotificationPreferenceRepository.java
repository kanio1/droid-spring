package com.droid.bss.domain.monitoring;

import java.util.Optional;

/**
 * Repository port for NotificationPreference
 */
public interface NotificationPreferenceRepository {

    Optional<NotificationPreference> findById(Long id);

    Optional<NotificationPreference> findByCustomerId(Long customerId);

    NotificationPreference save(NotificationPreference preference);

    void deleteById(Long id);
}
