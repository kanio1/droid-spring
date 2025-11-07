package com.droid.bss.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SendEmailDelegate
 */
@ExtendWith(MockitoExtension.class)
class SendEmailDelegateTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private DelegateExecution execution;

    private SendEmailDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new SendEmailDelegate();
        delegate.setEmailSender(emailSender);
    }

    @Test
    void testExecuteWithWelcomeEmailTemplate() {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-123");
        when(execution.getVariable("to")).thenReturn("test@example.com");
        when(execution.getVariable("subject")).thenReturn("Welcome!");
        when(execution.getVariable("template")).thenReturn("welcome_email");
        when(execution.getVariable("customer_name")).thenReturn("John Doe");
        when(execution.getVariable("customerId")).thenReturn("customer-123");
        when(execution.getVariable("amount")).thenReturn(null);
        when(execution.getVariable("paymentId")).thenReturn(null);

        // When
        assertDoesNotThrow(() -> delegate.execute(execution));

        // Then
        verify(emailSender).send(any(SimpleMailMessage.class));
        verify(execution).setVariable("email_sent", true);
        verify(execution).setVariable(eq("email_sent_at"), anyString());

        // Verify email content
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("test@example.com", sentMessage.getTo()[0]);
        assertEquals("Welcome!", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("Dear John Doe"));
        assertTrue(sentMessage.getText().contains("Welcome to BSS"));
        assertTrue(sentMessage.getText().contains("customer-123"));
    }

    @Test
    void testExecuteWithPaymentFailedTemplate() {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-456");
        when(execution.getVariable("to")).thenReturn("customer@example.com");
        when(execution.getVariable("subject")).thenReturn("Payment Failed");
        when(execution.getVariable("template")).thenReturn("payment_failed");
        when(execution.getVariable("customer_name")).thenReturn("Jane Smith");
        when(execution.getVariable("customerId")).thenReturn("customer-456");
        when(execution.getVariable("amount")).thenReturn(99.99);
        when(execution.getVariable("paymentId")).thenReturn("payment-789");

        // When
        assertDoesNotThrow(() -> delegate.execute(execution));

        // Then
        verify(emailSender).send(any(SimpleMailMessage.class));
        verify(execution).setVariable("email_sent", true);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Dear Jane Smith"));
        assertTrue(sentMessage.getText().contains("Payment ID: payment-789"));
        assertTrue(sentMessage.getText().contains("Amount: $99.99"));
        assertTrue(sentMessage.getText().contains("Unable to process"));
    }

    @Test
    void testExecuteWithDefaultTemplate() {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-789");
        when(execution.getVariable("to")).thenReturn("test@example.com");
        when(execution.getVariable("subject")).thenReturn("Notification");
        when(execution.getVariable("template")).thenReturn("unknown_template");
        when(execution.getVariable("customer_name")).thenReturn("Test User");
        when(execution.getVariable("customerId")).thenReturn("customer-789");
        when(execution.getVariable("amount")).thenReturn(null);
        when(execution.getVariable("paymentId")).thenReturn(null);

        // When
        assertDoesNotThrow(() -> delegate.execute(execution));

        // Then
        verify(emailSender).send(any(SimpleMailMessage.class));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getText().contains("Dear Test User"));
        assertTrue(sentMessage.getText().contains("Thank you for your business"));
    }

    @Test
    void testExecuteHandlesEmailFailure() {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-error");
        when(execution.getVariable("to")).thenReturn("test@example.com");
        when(execution.getVariable("subject")).thenReturn("Test");
        when(execution.getVariable("template")).thenReturn("welcome_email");
        when(execution.getVariable("customer_name")).thenReturn("Test");
        when(execution.getVariable("customerId")).thenReturn("customer-error");
        when(execution.getVariable("amount")).thenReturn(null);
        when(execution.getVariable("paymentId")).thenReturn(null);

        doThrow(new RuntimeException("Email failed")).when(emailSender).send(any(SimpleMailMessage.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> delegate.execute(execution));
        assertEquals("Email failed", exception.getMessage());

        verify(execution).setVariable("email_sent", false);
        verify(execution).setVariable(eq("email_error"), contains("Email failed"));
    }

    @Test
    void testBuildWelcomeEmail() {
        // When
        String email = delegate.buildEmailBody("welcome_email", "Alice", "customer-100");

        // Then
        assertTrue(email.contains("Dear Alice"));
        assertTrue(email.contains("Welcome to BSS"));
        assertTrue(email.contains("Your customer ID: customer-100"));
        assertTrue(email.contains("Customer portal"));
        assertTrue(email.contains("Basic support"));
    }

    @Test
    void testBuildPaymentFailedEmail() {
        // When
        String email = delegate.buildEmailBody("payment_failed", "Bob", "customer-200", 199.99, "payment-300");

        // Then
        assertTrue(email.contains("Dear Bob"));
        assertTrue(email.contains("Unable to process your recent payment"));
        assertTrue(email.contains("Payment ID: payment-300"));
        assertTrue(email.contains("Amount: $199.99"));
        assertTrue(email.contains("Insufficient funds"));
        assertTrue(email.contains("update your payment information"));
    }

    @Test
    void testBuildDefaultEmail() {
        // When
        String email = delegate.buildEmailBody("default", "Charlie", null, null, null);

        // Then
        assertTrue(email.contains("Dear Charlie"));
        assertTrue(email.contains("Thank you for your business"));
    }
}
