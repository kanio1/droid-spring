package com.droid.bss.infrastructure.database.repository;

import com.droid.bss.domain.monitoring.NotificationPreference;
import com.droid.bss.domain.monitoring.NotificationPreferenceRepository;
import com.droid.bss.infrastructure.database.entity.NotificationPreferenceEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.function.Function;

/**
 * JPA implementation of NotificationPreferenceRepository
 */
@Repository
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final SpringDataNotificationPreferenceRepository springDataRepository;

    public NotificationPreferenceRepositoryImpl(
            SpringDataNotificationPreferenceRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<NotificationPreference> findById(Long id) {
        return springDataRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<NotificationPreference> findByCustomerId(Long customerId) {
        return springDataRepository.findByCustomerId(customerId)
                .map(this::toDomain);
    }

    @Override
    public NotificationPreference save(NotificationPreference preference) {
        NotificationPreferenceEntity entity = toEntity(preference);
        NotificationPreferenceEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    private NotificationPreference toDomain(NotificationPreferenceEntity entity) {
        if (entity == null) {
            return null;
        }

        NotificationPreference preference = new NotificationPreference();
        preference.setId(entity.getId());
        preference.setCustomerId(entity.getCustomerId());
        preference.setEmail(entity.getEmail());
        preference.setPhoneNumber(entity.getPhoneNumber());
        preference.setSlackChannel(entity.getSlackChannel());
        preference.setEmailEnabled(entity.isEmailEnabled());
        preference.setSmsEnabled(entity.isSmsEnabled());
        preference.setSlackEnabled(entity.isSlackEnabled());
        preference.setCriticalAlertsOnly(entity.isCriticalAlertsOnly());
        preference.setCreatedAt(entity.getCreatedAt());
        preference.setUpdatedAt(entity.getUpdatedAt());
        return preference;
    }

    private NotificationPreferenceEntity toEntity(NotificationPreference preference) {
        if (preference == null) {
            return null;
        }

        NotificationPreferenceEntity entity = new NotificationPreferenceEntity();
        entity.setId(preference.getId());
        entity.setCustomerId(preference.getCustomerId());
        entity.setEmail(preference.getEmail());
        entity.setPhoneNumber(preference.getPhoneNumber());
        entity.setSlackChannel(preference.getSlackChannel());
        entity.setEmailEnabled(preference.isEmailEnabled());
        entity.setSmsEnabled(preference.isSmsEnabled());
        entity.setSlackEnabled(preference.isSlackEnabled());
        entity.setCriticalAlertsOnly(preference.isCriticalAlertsOnly());
        entity.setCreatedAt(preference.getCreatedAt());
        entity.setUpdatedAt(preference.getUpdatedAt());
        return entity;
    }
}
