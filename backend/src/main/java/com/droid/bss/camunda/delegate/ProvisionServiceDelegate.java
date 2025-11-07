package com.droid.bss.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Camunda Java Delegate for provisioning services
 * Used in Customer Onboarding workflow
 */
@Component("provisionServiceDelegate")
public class ProvisionServiceDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ProvisionServiceDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing ProvisionServiceDelegate for process instance: {}", execution.getProcessInstanceId());

        try {
            String customerId = (String) execution.getVariable("customerId");

            // Get services to provision (from BPMN)
            Object servicesObj = execution.getVariable("services");
            List<String> services = null;

            if (servicesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> servicesList = (List<String>) servicesObj;
                services = servicesList;
            } else if (servicesObj instanceof String) {
                // Parse comma-separated string
                services = List.of(((String) servicesObj).split(","));
            }

            logger.info("Provisioning services {} for customer: {}", services, customerId);

            // In a real implementation, this would call a service provisioning API
            // For now, we'll simulate the provisioning

            // Simulate provisioning delay
            Thread.sleep(1000);

            // Mock provisioning results
            Map<String, Object> provisioningResults = simulateProvisioning(customerId, services);

            // Set result variables
            execution.setVariable("services_provisioned", true);
            execution.setVariable("provisioning_results", provisioningResults);
            execution.setVariable("services_provisioned_at", java.time.Instant.now().toString());

            logger.info("Services provisioned successfully for customer: {}", customerId);

        } catch (Exception e) {
            logger.error("Failed to provision services in process instance: {}", execution.getProcessInstanceId(), e);
            execution.setVariable("services_provisioned", false);
            execution.setVariable("provisioning_error", e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> simulateProvisioning(String customerId, List<String> services) {
        // This simulates the provisioning process
        // In a real implementation, this would interact with actual services

        logger.info("Simulating provisioning for customer: {} with services: {}", customerId, services);

        return Map.of(
            "customer_id", customerId,
            "services", services,
            "status", "provisioned",
            "provisioning_id", "prov-" + System.currentTimeMillis(),
            "services_detail", services.stream()
                .map(service -> Map.of(
                    "service_name", service.trim(),
                    "status", "active",
                    "provisioned_at", java.time.Instant.now().toString()
                ))
                .toList()
        );
    }
}
