package com.droid.bss.application.service.monitoring;

import com.droid.bss.domain.monitoring.Alert;
import com.droid.bss.domain.monitoring.AlertRepository;
import com.droid.bss.domain.monitoring.NotificationPreference;
import com.droid.bss.domain.monitoring.NotificationPreferenceRepository;
import com.droid.bss.domain.monitoring.NotificationService;
import com.droid.bss.domain.monitoring.ResourceMetric;
import com.droid.bss.domain.monitoring.ResourceThreshold;
import com.droid.bss.domain.monitoring.ResourceThresholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Use case for evaluating metrics against thresholds and managing alerts
 */
@Service
@Transactional
public class EvaluateThresholdUseCase {

    private final ResourceThresholdRepository thresholdRepository;
    private final AlertRepository alertRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationService notificationService;

    public EvaluateThresholdUseCase(ResourceThresholdRepository thresholdRepository,
                                    AlertRepository alertRepository,
                                    NotificationPreferenceRepository preferenceRepository,
                                    NotificationService notificationService) {
        this.thresholdRepository = thresholdRepository;
        this.alertRepository = alertRepository;
        this.preferenceRepository = preferenceRepository;
        this.notificationService = notificationService;
    }

    /**
     * Evaluate a metric against thresholds and create/resolve alerts
     */
    public List<Alert> evaluateMetric(ResourceMetric metric) {
        Optional<ResourceThreshold> thresholdOpt = thresholdRepository
                .findByCustomerIdAndResourceIdAndMetricType(
                        metric.getCustomerId(),
                        metric.getResourceId(),
                        metric.getMetricType());

        if (thresholdOpt.isEmpty() || !thresholdOpt.get().isEnabled()) {
            return List.of();
        }

        ResourceThreshold threshold = thresholdOpt.get();
        String severity = threshold.getSeverity(metric.getValue());

        if ("OK".equals(severity)) {
            return resolveAlertsIfNeeded(metric, threshold);
        } else {
            return createOrUpdateAlert(metric, threshold, severity);
        }
    }

    /**
     * Evaluate multiple metrics against thresholds
     */
    public List<Alert> evaluateMetrics(List<ResourceMetric> metrics) {
        return metrics.stream()
                .map(this::evaluateMetric)
                .flatMap(List::stream)
                .toList();
    }

    private List<Alert> createOrUpdateAlert(ResourceMetric metric, ResourceThreshold threshold, String severity) {
        String status = "OPEN";
        List<String> activeStatuses = Arrays.asList("OPEN", "ACKNOWLEDGED");

        List<Alert> existingAlerts = alertRepository
                .findByCustomerIdAndResourceIdAndMetricTypeAndStatusIn(
                        metric.getCustomerId(),
                        metric.getResourceId(),
                        metric.getMetricType(),
                        activeStatuses);

        if (!existingAlerts.isEmpty()) {
            Alert existingAlert = existingAlerts.get(0);

            if (!existingAlert.getSeverity().equals(severity)) {
                existingAlert.setSeverity(severity);
                existingAlert.setCurrentValue(metric.getValue());
                existingAlert.setThresholdValue(
                        "CRITICAL".equals(severity) ?
                                threshold.getCriticalThreshold() :
                                threshold.getWarningThreshold());
                existingAlert.setThresholdType(severity);
                existingAlert.setMessage(buildAlertMessage(metric, severity,
                        threshold.getOperator(), existingAlert.getThresholdValue()));
                Alert saved = alertRepository.save(existingAlert);

                // Send notification for severity change
                sendNotification(metric.getCustomerId(), severity, saved.getMessage());

                return List.of(saved);
            }

            return existingAlerts;
        }

        Alert newAlert = new Alert(
                metric.getCustomerId(),
                metric.getResourceId(),
                metric.getMetricType(),
                severity,
                status,
                metric.getValue(),
                "CRITICAL".equals(severity) ?
                        threshold.getCriticalThreshold() :
                        threshold.getWarningThreshold(),
                severity,
                buildAlertMessage(metric, severity, threshold.getOperator(),
                        "CRITICAL".equals(severity) ?
                                threshold.getCriticalThreshold() :
                                threshold.getWarningThreshold()),
                Instant.now(),
                metric.getSource()
        );

        Alert saved = alertRepository.save(newAlert);

        // Send notification for new alert
        sendNotification(metric.getCustomerId(), severity, saved.getMessage());

        return List.of(saved);
    }

    private List<Alert> resolveAlertsIfNeeded(ResourceMetric metric, ResourceThreshold threshold) {
        List<String> activeStatuses = Arrays.asList("OPEN", "ACKNOWLEDGED");

        List<Alert> activeAlerts = alertRepository
                .findByCustomerIdAndResourceIdAndMetricTypeAndStatusIn(
                        metric.getCustomerId(),
                        metric.getResourceId(),
                        metric.getMetricType(),
                        activeStatuses);

        for (Alert alert : activeAlerts) {
            alert.resolve("system");
        }

        return activeAlerts.stream()
                .map(alertRepository::save)
                .toList();
    }

    private String buildAlertMessage(ResourceMetric metric, String severity,
                                    String operator, BigDecimal thresholdValue) {
        String direction = "GT".equals(operator) ? "above" : "below";
        return String.format("[%s] %s for resource %d is %s %s (threshold: %s %s)",
                severity,
                metric.getMetricType(),
                metric.getResourceId(),
                metric.getValue(),
                metric.getUnit(),
                direction,
                thresholdValue + " " + metric.getUnit());
    }

    private void sendNotification(Long customerId, String severity, String message) {
        Optional<NotificationPreference> preference = preferenceRepository.findByCustomerId(customerId);
        if (preference.isPresent()) {
            notificationService.sendNotification(preference.get(), severity, message);
        }
    }
}
