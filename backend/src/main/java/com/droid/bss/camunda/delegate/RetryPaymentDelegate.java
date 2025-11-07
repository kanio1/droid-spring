package com.droid.bss.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Camunda Java Delegate for retrying payments
 * Used in Payment Failed Recovery workflow
 */
@Component("retryPaymentDelegate")
public class RetryPaymentDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(RetryPaymentDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing RetryPaymentDelegate for process instance: {}", execution.getProcessInstanceId());

        try {
            String paymentId = (String) execution.getVariable("paymentId");
            String customerId = (String) execution.getVariable("customerId");
            Double amount = execution.getVariable("amount") != null ?
                (Double) execution.getVariable("amount") : 0.0;

            logger.info("Retrying payment {} for customer: {} amount: ${}",
                paymentId, customerId, amount);

            // In a real implementation, this would call payment gateway API
            // For now, we'll simulate payment retry

            // Simulate API call delay
            Thread.sleep(500);

            // Simulate payment retry (90% success rate for demo)
            boolean success = Math.random() > 0.1;

            String status = success ? "COMPLETED" : "FAILED";
            String responseCode = success ? "000" : "005";

            // Set result variables
            execution.setVariable("payment_status", status);
            execution.setVariable("payment_response_code", responseCode);
            execution.setVariable("payment_retry_attempted", true);
            execution.setVariable("payment_retry_at", java.time.Instant.now().toString());

            if (success) {
                logger.info("Payment retry successful for payment: {}", paymentId);
            } else {
                logger.warn("Payment retry failed for payment: {}", paymentId);
            }

        } catch (Exception e) {
            logger.error("Failed to retry payment in process instance: {}", execution.getProcessInstanceId(), e);
            execution.setVariable("payment_status", "FAILED");
            execution.setVariable("payment_error", e.getMessage());
            execution.setVariable("payment_retry_attempted", false);
            throw e;
        }
    }
}
