package com.droid.bss.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Camunda Java Delegate for sending emails
 * Used in Customer Onboarding and Payment Failed workflows
 */
@Component("sendEmailDelegate")
public class SendEmailDelegate implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SendEmailDelegate.class);

    @Autowired
    private JavaMailSender emailSender;

    // Package-private setter for testing
    void setEmailSender(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info("Executing SendEmailDelegate for process instance: {}", execution.getProcessInstanceId());

        try {
            // Get parameters from BPMN inputOutput
            String to = (String) execution.getVariable("to");
            String subject = (String) execution.getVariable("subject");
            String template = (String) execution.getVariable("template");
            String customerName = (String) execution.getVariable("customer_name");
            String customerId = (String) execution.getVariable("customerId");
            Double amount = execution.getVariable("amount") != null ?
                (Double) execution.getVariable("amount") : null;
            String paymentId = (String) execution.getVariable("paymentId");

            // Build email body based on template
            String body = buildEmailBody(template, customerName, customerId, amount, paymentId);

            // Create and send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            logger.info("Sending email to: {} with subject: {}", to, subject);
            emailSender.send(message);

            // Set result variables
            execution.setVariable("email_sent", true);
            execution.setVariable("email_sent_at", java.time.Instant.now().toString());

            logger.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email in process instance: {}", execution.getProcessInstanceId(), e);
            execution.setVariable("email_sent", false);
            execution.setVariable("email_error", e.getMessage());
            throw e;
        }
    }

    private String buildEmailBody(String template, String customerName, String customerId,
                                   Double amount, String paymentId) {
        if (template == null) {
            template = "default";
        }

        return switch (template.toLowerCase()) {
            case "welcome_email" -> buildWelcomeEmail(customerName, customerId);
            case "payment_failed" -> buildPaymentFailedEmail(customerName, amount, paymentId);
            default -> buildDefaultEmail(customerName);
        };
    }

    private String buildWelcomeEmail(String customerName, String customerId) {
        return String.format(
            "Dear %s,\n\n" +
            "Welcome to BSS! We're excited to have you as a customer.\n\n" +
            "Your customer ID: %s\n\n" +
            "You now have access to:\n" +
            "- Customer portal\n" +
            "- Basic support\n" +
            "- Your account dashboard\n\n" +
            "If you have any questions, please don't hesitate to contact us.\n\n" +
            "Best regards,\n" +
            "The BSS Team",
            customerName != null ? customerName : "Customer",
            customerId
        );
    }

    private String buildPaymentFailedEmail(String customerName, Double amount, String paymentId) {
        return String.format(
            "Dear %s,\n\n" +
            "We were unable to process your recent payment of %s.\n\n" +
            "Payment ID: %s\n" +
            "Amount: $%.2f\n\n" +
            "This could be due to:\n" +
            "- Insufficient funds\n" +
            "- Expired credit card\n" +
            "- Bank restrictions\n\n" +
            "Please update your payment information to avoid service interruption.\n\n" +
            "You can update your payment details in the customer portal.\n\n" +
            "If you need assistance, please contact our billing team.\n\n" +
            "Best regards,\n" +
            "The BSS Team",
            customerName != null ? customerName : "Customer",
            paymentId,
            amount != null ? amount : 0.0
        );
    }

    private String buildDefaultEmail(String customerName) {
        return String.format(
            "Dear %s,\n\n" +
            "Thank you for your business.\n\n" +
            "Best regards,\n" +
            "The BSS Team",
            customerName != null ? customerName : "Customer"
        );
    }
}
