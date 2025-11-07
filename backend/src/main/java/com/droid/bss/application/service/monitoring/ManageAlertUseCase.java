package com.droid.bss.application.service.monitoring;

import com.droid.bss.domain.monitoring.Alert;
import com.droid.bss.domain.monitoring.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for managing alerts (acknowledge, resolve, delete)
 */
@Service
@Transactional
public class ManageAlertUseCase {

    private final AlertRepository alertRepository;

    public ManageAlertUseCase(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Optional<Alert> acknowledgeAlert(Long alertId, String userId) {
        return alertRepository.findById(alertId)
                .map(alert -> {
                    if (alert.isActive()) {
                        alert.acknowledge(userId);
                        return alertRepository.save(alert);
                    }
                    return alert;
                });
    }

    public Optional<Alert> resolveAlert(Long alertId, String userId) {
        return alertRepository.findById(alertId)
                .map(alert -> {
                    if (alert.isActive()) {
                        alert.resolve(userId);
                        return alertRepository.save(alert);
                    }
                    return alert;
                });
    }

    public boolean deleteAlert(Long alertId) {
        if (alertRepository.findById(alertId).isPresent()) {
            alertRepository.deleteById(alertId);
            return true;
        }
        return false;
    }
}
