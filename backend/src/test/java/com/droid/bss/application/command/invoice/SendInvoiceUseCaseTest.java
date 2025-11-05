package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.SendInvoiceCommand;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.invoice.Invoice;
import com.droid.bss.domain.invoice.InvoiceId;
import com.droid.bss.domain.invoice.InvoiceItem;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for SendInvoiceUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendInvoiceUseCase Application Layer")
class SendInvoiceUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SendInvoiceUseCase sendInvoiceUseCase;

    @Test
    @DisplayName("Should send draft invoice successfully")
    void shouldSendDraftInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Customer requested invoice",
            false // sendEmail
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product A", 2, new BigDecimal("99.99"), new BigDecimal("199.98"))),
            new BigDecimal("199.98"),
            new BigDecimal("0.00"),
            new BigDecimal("199.98"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product A", 2, new BigDecimal("99.99"), new BigDecimal("199.98"))),
            new BigDecimal("199.98"),
            new BigDecimal("0.00"),
            new BigDecimal("199.98"),
            InvoiceStatus.SENT
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sentInvoice);

        // Act
        Invoice result = sendInvoiceUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(result.getSentDate()).isNotNull();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository).save(any(Invoice.class));
        verify(notificationService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should send draft invoice and email customer when requested")
    void shouldSendDraftInvoiceAndEmailCustomer() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Please find attached invoice",
            true // sendEmail
        );

        CustomerInfo personalInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");
        ContactInfo contactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product B", 1, new BigDecimal("149.99"), new BigDecimal("149.99"))),
            new BigDecimal("149.99"),
            new BigDecimal("34.50"),
            new BigDecimal("184.49"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product B", 1, new BigDecimal("149.99"), new BigDecimal("149.99"))),
            new BigDecimal("149.99"),
            new BigDecimal("34.50"),
            new BigDecimal("184.49"),
            InvoiceStatus.SENT
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sentInvoice);

        // Act
        Invoice result = sendInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(result.getSentDate()).isNotNull();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository).save(any(Invoice.class));
        verify(notificationService).sendEmail(
            eq("jane.smith@example.com"),
            eq("Invoice INV-2025"),
            contains("Please find attached invoice")
        );
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Please pay invoice",
            false
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            sendInvoiceUseCase.handle(command);
        }, "Should throw exception when invoice not found");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(notificationService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when trying to send already sent invoice")
    void shouldThrowExceptionWhenTryingToSendAlreadySentInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Reminder: invoice sent",
            false
        );

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product C", 1, new BigDecimal("99.99"), new BigDecimal("99.99"))),
            new BigDecimal("99.99"),
            new BigDecimal("0.00"),
            new BigDecimal("99.99"),
            InvoiceStatus.SENT
        );

        // Set sent date
        try {
            java.lang.reflect.Method setSentDateMethod = Invoice.class.getDeclaredMethod("setSentDate", LocalDateTime.class);
            setSentDateMethod.setAccessible(true);
            setSentDateMethod.invoke(sentInvoice, LocalDateTime.now().minusDays(1));
        } catch (Exception e) {
            // Ignore reflection errors
        }

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(sentInvoice));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            sendInvoiceUseCase.handle(command);
        }, "Should throw exception when invoice already sent");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(notificationService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when trying to send paid invoice")
    void shouldThrowExceptionWhenTryingToSendPaidInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Invoice already paid",
            false
        );

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice paidInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product D", 3, new BigDecimal("50.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("34.50"),
            new BigDecimal("184.50"),
            InvoiceStatus.PAID
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(paidInvoice));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            sendInvoiceUseCase.handle(command);
        }, "Should throw exception when invoice is already paid");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(notificationService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when trying to send cancelled invoice")
    void shouldThrowExceptionWhenTryingToSendCancelledInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Cancelled invoice",
            false
        );

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice cancelledInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product E", 1, new BigDecimal("200.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("0.00"),
            new BigDecimal("200.00"),
            InvoiceStatus.CANCELLED
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(cancelledInvoice));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            sendInvoiceUseCase.handle(command);
        }, "Should throw exception when invoice is cancelled");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verify(notificationService, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should record sent date when invoice is sent")
    void shouldRecordSentDateWhenInvoiceIsSent() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();
        LocalDateTime beforeSend = LocalDateTime.now();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Invoice for your review",
            false
        );

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product F", 2, new BigDecimal("75.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("0.00"),
            new BigDecimal("150.00"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product F", 2, new BigDecimal("75.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("0.00"),
            new BigDecimal("150.00"),
            InvoiceStatus.SENT
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sentInvoice);

        // Act
        Invoice result = sendInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getSentDate()).isNotNull();
        assertThat(result.getSentDate()).isAfter(beforeSend);
        assertThat(result.getSentDate()).isBefore(LocalDateTime.now().plusSeconds(1));

        verify(invoiceRepository).save(argThat(inv ->
            inv.getSentDate() != null &&
            inv.getSentDate().isAfter(beforeSend)
        ));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should send email with custom message")
    void shouldSendEmailWithCustomMessage() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();
        String customMessage = "Dear Customer, your invoice is ready for payment. Thank you!";

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            customMessage,
            true
        );

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product G", 1, new BigDecimal("299.99"), new BigDecimal("299.99"))),
            new BigDecimal("299.99"),
            new BigDecimal("69.00"),
            new BigDecimal("368.99"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product G", 1, new BigDecimal("299.99"), new BigDecimal("299.99"))),
            new BigDecimal("299.99"),
            new BigDecimal("69.00"),
            new BigDecimal("368.99"),
            InvoiceStatus.SENT
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sentInvoice);

        // Act
        Invoice result = sendInvoiceUseCase.handle(command);

        // Assert
        verify(notificationService).sendEmail(
            eq("eve.adams@example.com"),
            eq("Invoice INV-2025"),
            eq(customMessage)
        );

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should not send email when sendEmail is false")
    void shouldNotSendEmailWhenSendEmailIsFalse() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Manual send only",
            false
        );

        CustomerInfo personalInfo = new CustomerInfo("Frank", "Miller", "66666666666", "6666666666");
        ContactInfo contactInfo = new ContactInfo("frank.miller@example.com", "+48666666666");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product H", 1, new BigDecimal("125.00"), new BigDecimal("125.00"))),
            new BigDecimal("125.00"),
            new BigDecimal("0.00"),
            new BigDecimal("125.00"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product H", 1, new BigDecimal("125.00"), new BigDecimal("125.00"))),
            new BigDecimal("125.00"),
            new BigDecimal("0.00"),
            new BigDecimal("125.00"),
            InvoiceStatus.SENT
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sentInvoice);

        // Act
        Invoice result = sendInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);
        verify(notificationService, never()).sendEmail(any(), any(), any());

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should handle invoice with tax correctly")
    void shouldHandleInvoiceWithTaxCorrectly() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        SendInvoiceCommand command = new SendInvoiceCommand(
            invoiceId,
            "Invoice with tax",
            false
        );

        CustomerInfo personalInfo = new CustomerInfo("Grace", "Hopper", "77777777777", "7777777777");
        ContactInfo contactInfo = new ContactInfo("grace.hopper@example.com", "+48777777777");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product I", 2, new BigDecimal("100.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("46.00"), // 23% tax
            new BigDecimal("246.00"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        Invoice sentInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product I", 2, new BigDecimal("100.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("46.00"),
            new BigDecimal("246.00"),
            InvoiceStatus.SENT
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sentInvoice);

        // Act
        Invoice result = sendInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("200.00"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("46.00"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("246.00"));
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.SENT);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // Helper classes
    private static class OrderId {
        private final UUID value;

        public OrderId(UUID value) {
            this.value = value;
        }

        public UUID getValue() {
            return value;
        }
    }
}
