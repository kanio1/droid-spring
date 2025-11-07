package com.droid.bss.application.service.monitoring;

import com.droid.bss.application.dto.monitoring.NotificationPreferenceRequest;
import com.droid.bss.application.dto.monitoring.NotificationPreferenceResponse;
import com.droid.bss.domain.monitoring.NotificationPreference;
import com.droid.bss.domain.monitoring.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Function;

/**
 * Use case for managing notification preferences
 */
@Service
@Transactional
public class ManageNotificationPreferenceUseCase {

    private final NotificationPreferenceRepository repository;

    public ManageNotificationPreferenceUseCase(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    public Optional<NotificationPreferenceResponse> getPreferenceById(Long id) {
        return repository.findById(id)
                .map(this::toResponse);
    }

    public Optional<NotificationPreferenceResponse> getPreferenceByCustomerId(Long customerId) {
        return repository.findByCustomerId(customerId)
                .map(this::toResponse);
    }

    public NotificationPreferenceResponse createPreference(NotificationPreferenceRequest request) {
        NotificationPreference preference = new NotificationPreference(
                request.getCustomerId(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getSlackChannel(),
                request.getEmailEnabled(),
                request.getSmsEnabled(),
                request.getSlackEnabled(),
                request.getCriticalAlertsOnly()
        );

        // Set timestamps
        long now = System.currentTimeMillis();
        preference.setCreatedAt(now);
        preference.setUpdatedAt(now);

        NotificationPreference saved = repository.save(preference);
        return toResponse(saved);
    }

    public Optional<NotificationPreferenceResponse> updatePreference(Long id, NotificationPreferenceRequest request) {
        return repository.findById(id)
                .map(preference -> {
                    preference.setEmail(request.getEmail());
                    preference.setPhoneNumber(request.getPhoneNumber());
                    preference.setSlackChannel(request.getSlackChannel());
                    preference.setEmailEnabled(request.getEmailEnabled());
                    preference.setSmsEnabled(request.getSmsEnabled());
                    preference.setSlackEnabled(request.getSlackEnabled());
                    preference.setCriticalAlertsOnly(request.getCriticalAlertsOnly());
                    preference.setUpdatedAt(System.currentTimeMillis());

                    NotificationPreference saved = repository.save(preference);
                    return toResponse(saved);
                });
    }

    public boolean deletePreference(Long id) {
        if (repository.findById(id).isPresent()) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.getId(),
                preference.getCustomerId(),
                preference.getEmail(),
                preference.getPhoneNumber(),
                preference.getSlackChannel(),
                preference.isEmailEnabled(),
                preference.isSmsEnabled(),
                preference.isSlackEnabled(),
                preference.isCriticalAlertsOnly(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }
}
