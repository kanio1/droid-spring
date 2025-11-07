package com.droid.bss.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Camunda Java Delegate for creating support tickets
 * Used in Customer Onboarding and Payment Failed workflows
 */
@Component("createTicketDelegate")
public class CreateTicketDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(CreateTicketDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing CreateTicketDelegate for process instance: {}", execution.getProcessInstanceId());

        try {
            String customerId = (String) execution.getVariable("customerId");
            String queue = (String) execution.getVariable("queue");
            String priority = (String) execution.getVariable("priority");
            String subject = (String) execution.getVariable("subject");
            String description = (String) execution.getVariable("description");

            logger.info("Creating ticket for customer: {} in queue: {} with priority: {}",
                customerId, queue, priority);

            // In a real implementation, this would create a ticket in a help desk system
            // For now, we'll simulate ticket creation

            String ticketId = createMockTicket(customerId, queue, priority, subject, description);

            // Set result variables
            execution.setVariable("ticket_created", true);
            execution.setVariable("ticket_id", ticketId);
            execution.setVariable("ticket_created_at", java.time.Instant.now().toString());

            logger.info("Created ticket: {} for customer: {}", ticketId, customerId);

        } catch (Exception e) {
            logger.error("Failed to create ticket in process instance: {}", execution.getProcessInstanceId(), e);
            execution.setVariable("ticket_created", false);
            execution.setVariable("ticket_error", e.getMessage());
            throw e;
        }
    }

    private String createMockTicket(String customerId, String queue, String priority,
                                     String subject, String description) {
        // This simulates ticket creation
        // In a real implementation, this would integrate with a help desk system like:
        // - Jira Service Management
        // - Zendesk
        // - Freshservice
        // - etc.

        logger.info("Simulating ticket creation:");
        logger.info("  Customer ID: {}", customerId);
        logger.info("  Queue: {}", queue);
        logger.info("  Priority: {}", priority);
        logger.info("  Subject: {}", subject);
        logger.info("  Description: {}", description);

        // Generate a mock ticket ID
        String ticketId = "TICKET-" + System.currentTimeMillis();

        return ticketId;
    }
}
