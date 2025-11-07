package com.droid.bss.application.service.monitoring;

import com.droid.bss.domain.monitoring.Alert;
import com.droid.bss.domain.monitoring.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Use case for retrieving alerts
 */
@Service
@Transactional(readOnly = true)
public class GetAlertsUseCase {

    private final AlertRepository alertRepository;

    public GetAlertsUseCase(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> getAlertsByCustomerAndTimeRange(
            Long customerId, Instant startTime, Instant endTime) {
        if (startTime != null && endTime != null) {
            return alertRepository.findByTriggeredAtBetween(startTime, endTime);
        } else {
            return alertRepository.findByCustomerId(customerId);
        }
    }

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByStatusIn(Arrays.asList("OPEN", "ACKNOWLEDGED"));
    }

    public List<Alert> getActiveAlertsByCustomer(Long customerId) {
        List<Alert> allCustomerAlerts = alertRepository.findByCustomerId(customerId);
        return allCustomerAlerts.stream()
                .filter(Alert::isActive)
                .toList();
    }

    public List<Alert> getAlertsByResource(Long resourceId) {
        return alertRepository.findByResourceId(resourceId);
    }

    public List<Alert> getAlertsBySeverity(String severity) {
        return alertRepository.findBySeverity(severity);
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }
}
