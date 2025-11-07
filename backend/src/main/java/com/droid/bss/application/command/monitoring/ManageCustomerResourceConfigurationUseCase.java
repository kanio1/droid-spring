package com.droid.bss.application.command.monitoring;

import com.droid.bss.domain.monitoring.CustomerResourceConfiguration;
import com.droid.bss.domain.monitoring.CustomerResourceConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class ManageCustomerResourceConfigurationUseCase {

    private final CustomerResourceConfigurationRepository configurationRepository;

    public ManageCustomerResourceConfigurationUseCase(CustomerResourceConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public CustomerResourceConfiguration create(Long customerId, String resourceType, String resourceId,
                                               String resourceName, String region, BigDecimal maxLimit,
                                               BigDecimal warningThreshold, BigDecimal criticalThreshold,
                                               BigDecimal budgetLimit, String budgetCurrency,
                                               String alertEmail, String alertPhone, String alertSlackWebhook,
                                               boolean autoScalingEnabled, BigDecimal scaleUpThreshold,
                                               BigDecimal scaleDownThreshold, String status) {

        CustomerResourceConfiguration config = new CustomerResourceConfiguration(
                customerId, resourceType, resourceId, resourceName, region,
                maxLimit, warningThreshold, criticalThreshold, budgetLimit, budgetCurrency, status);

        config.setAlertEmail(alertEmail);
        config.setAlertPhone(alertPhone);
        config.setAlertSlackWebhook(alertSlackWebhook);
        config.setAutoScalingEnabled(autoScalingEnabled);
        config.setScaleUpThreshold(scaleUpThreshold);
        config.setScaleDownThreshold(scaleDownThreshold);

        return configurationRepository.save(config);
    }

    public Optional<CustomerResourceConfiguration> update(Long id, String resourceName, String region,
                                                         BigDecimal maxLimit, BigDecimal warningThreshold,
                                                         BigDecimal criticalThreshold, BigDecimal budgetLimit,
                                                         String budgetCurrency, String alertEmail,
                                                         String alertPhone, String alertSlackWebhook,
                                                         boolean autoScalingEnabled, BigDecimal scaleUpThreshold,
                                                         BigDecimal scaleDownThreshold, String status) {

        Optional<CustomerResourceConfiguration> configOpt = configurationRepository.findById(id);
        if (configOpt.isPresent()) {
            CustomerResourceConfiguration config = configOpt.get();
            config.setResourceName(resourceName);
            config.setRegion(region);
            config.setMaxLimit(maxLimit);
            config.setWarningThreshold(warningThreshold);
            config.setCriticalThreshold(criticalThreshold);
            config.setBudgetLimit(budgetLimit);
            config.setBudgetCurrency(budgetCurrency);
            config.setAlertEmail(alertEmail);
            config.setAlertPhone(alertPhone);
            config.setAlertSlackWebhook(alertSlackWebhook);
            config.setAutoScalingEnabled(autoScalingEnabled);
            config.setScaleUpThreshold(scaleUpThreshold);
            config.setScaleDownThreshold(scaleDownThreshold);
            config.setStatus(status);
            config.updateTimestamps();
            return Optional.of(configurationRepository.save(config));
        }
        return Optional.empty();
    }

    public boolean delete(Long id) {
        Optional<CustomerResourceConfiguration> configOpt = configurationRepository.findById(id);
        if (configOpt.isPresent()) {
            configurationRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
