package com.droid.bss.application.query.invoice;

import com.droid.bss.application.dto.invoice.GetInvoiceByIdQuery;
import com.droid.bss.application.dto.invoice.InvoiceDto;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.invoice.Invoice;
import com.droid.bss.domain.invoice.InvoiceId;
import com.droid.bss.domain.invoice.InvoiceItem;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for GetInvoiceByIdUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetInvoiceByIdUseCase Query Side")
class GetInvoiceByIdUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private GetInvoiceByIdUseCase getInvoiceByIdUseCase;

    @Test
    @DisplayName("Should return invoice by ID successfully")
    void shouldReturnInvoiceById() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(
                new InvoiceItem("Product A", 2, new BigDecimal("99.99"), new BigDecimal("199.98")),
                new InvoiceItem("Product B", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
            ),
            new BigDecimal("249.98"),
            new BigDecimal("57.50"), // 23% tax
            new BigDecimal("307.48"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getCustomerId()).isEqualTo(customer.getId().toString());
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("249.98"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("57.50"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("307.48"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with DRAFT status")
    void shouldReturnInvoiceWithDraftStatus() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");
        ContactInfo contactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product C", 1, new BigDecimal("150.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("34.50"),
            new BigDecimal("184.50"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getId()).isEqualTo(invoiceId);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with PAID status")
    void shouldReturnInvoiceWithPaidStatus() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product D", 3, new BigDecimal("75.00"), new BigDecimal("225.00"))),
            new BigDecimal("225.00"),
            new BigDecimal("51.75"),
            new BigDecimal("276.75"),
            InvoiceStatus.PAID
        );

        // Set paid date
        try {
            java.lang.reflect.Method setPaidDateMethod = Invoice.class.getDeclaredMethod("setPaidDate", LocalDateTime.class);
            setPaidDateMethod.setAccessible(true);
            setPaidDateMethod.invoke(invoice, LocalDateTime.now().minusDays(2));
        } catch (Exception e) {
            // Ignore reflection errors
        }

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(result.getPaidDate()).isNotNull();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with CANCELLED status")
    void shouldReturnInvoiceWithCancelledStatus() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product E", 1, new BigDecimal("400.00"), new BigDecimal("400.00"))),
            new BigDecimal("400.00"),
            new BigDecimal("92.00"),
            new BigDecimal("492.00"),
            InvoiceStatus.CANCELLED
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getId()).isEqualTo(invoiceId);
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("492.00"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with all item details")
    void shouldReturnInvoiceWithAllItemDetails() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
        Customer customer = Customer.create(personalInfo, contactInfo);

        List<InvoiceItem> items = List.of(
            new InvoiceItem("Premium Product", 2, new BigDecimal("250.00"), new BigDecimal("500.00")),
            new InvoiceItem("Standard Product", 5, new BigDecimal("80.00"), new BigDecimal("400.00")),
            new InvoiceItem("Budget Product", 10, new BigDecimal("30.00"), new BigDecimal("300.00"))
        );

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            items,
            new BigDecimal("1200.00"),
            new BigDecimal("276.00"),
            new BigDecimal("1476.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getItems()).hasSize(3);

        // Verify first item
        assertThat(result.getItems().get(0).getDescription()).isEqualTo("Premium Product");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualTo(new BigDecimal("250.00"));
        assertThat(result.getItems().get(0).getTotalPrice()).isEqualTo(new BigDecimal("500.00"));

        // Verify second item
        assertThat(result.getItems().get(1).getDescription()).isEqualTo("Standard Product");
        assertThat(result.getItems().get(1).getQuantity()).isEqualTo(5);
        assertThat(result.getItems().get(1).getUnitPrice()).isEqualTo(new BigDecimal("80.00"));
        assertThat(result.getItems().get(1).getTotalPrice()).isEqualTo(new BigDecimal("400.00"));

        // Verify third item
        assertThat(result.getItems().get(2).getDescription()).isEqualTo("Budget Product");
        assertThat(result.getItems().get(2).getQuantity()).isEqualTo(10);
        assertThat(result.getItems().get(2).getUnitPrice()).isEqualTo(new BigDecimal("30.00"));
        assertThat(result.getItems().get(2).getTotalPrice()).isEqualTo(new BigDecimal("300.00"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with correct dates")
    void shouldReturnInvoiceWithCorrectDates() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();
        LocalDateTime issueDate = LocalDateTime.now().minusDays(5);
        LocalDateTime dueDate = LocalDateTime.now().plusDays(25);

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product F", 1, new BigDecimal("180.00"), new BigDecimal("180.00"))),
            new BigDecimal("180.00"),
            new BigDecimal("41.40"),
            new BigDecimal("221.40"),
            InvoiceStatus.DRAFT
        );

        // Set dates using reflection
        try {
            java.lang.reflect.Method setIssueDateMethod = Invoice.class.getDeclaredMethod("setIssueDate", LocalDateTime.class);
            setIssueDateMethod.setAccessible(true);
            setIssueDateMethod.invoke(invoice, issueDate);

            java.lang.reflect.Method setDueDateMethod = Invoice.class.getDeclaredMethod("setDueDate", LocalDateTime.class);
            setDueDateMethod.setAccessible(true);
            setDueDateMethod.invoke(invoice, dueDate);
        } catch (Exception e) {
            // Ignore reflection errors
        }

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getIssueDate()).isNotNull();
        assertThat(result.getDueDate()).isNotNull();
        assertThat(result.getIssueDate().toLocalDate()).isEqualTo(issueDate.toLocalDate());
        assertThat(result.getDueDate().toLocalDate()).isEqualTo(dueDate.toLocalDate());

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should calculate totals correctly for invoice with tax")
    void shouldCalculateTotalsCorrectlyForInvoiceWithTax() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(
                new InvoiceItem("Product G", 2, new BigDecimal("100.00"), new BigDecimal("200.00")),
                new InvoiceItem("Product H", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
            ),
            new BigDecimal("250.00"),
            new BigDecimal("57.50"), // 23% tax
            new BigDecimal("307.50"),
            InvoiceStatus.PAID
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("250.00"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("57.50"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("307.50"));
        assertThat(result.getTotal()).isEqualTo(
            result.getSubtotal().add(result.getTax())
        );

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should calculate totals correctly for invoice without tax")
    void shouldCalculateTotalsCorrectlyForInvoiceWithoutTax() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Frank", "Miller", "66666666666", "6666666666");
        ContactInfo contactInfo = new ContactInfo("frank.miller@example.com", "+48666666666");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product I", 3, new BigDecimal("66.66"), new BigDecimal("199.98"))),
            new BigDecimal("199.98"),
            new BigDecimal("0.00"), // No tax
            new BigDecimal("199.98"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("199.98"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("0.00"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("199.98"));
        assertThat(result.getTotal()).isEqualTo(result.getSubtotal());

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            getInvoiceByIdUseCase.handle(query);
        }, "Should throw exception when invoice not found");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with sent date when status is SENT")
    void shouldReturnInvoiceWithSentDateWhenStatusIsSent() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Grace", "Hopper", "77777777777", "7777777777");
        ContactInfo contactInfo = new ContactInfo("grace.hopper@example.com", "+48777777777");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Product J", 1, new BigDecimal("500.00"), new BigDecimal("500.00"))),
            new BigDecimal("500.00"),
            new BigDecimal("115.00"),
            new BigDecimal("615.00"),
            InvoiceStatus.SENT
        );

        // Set sent date
        LocalDateTime sentDate = LocalDateTime.now().minusDays(3);
        try {
            java.lang.reflect.Method setSentDateMethod = Invoice.class.getDeclaredMethod("setSentDate", LocalDateTime.class);
            setSentDateMethod.setAccessible(true);
            setSentDateMethod.invoke(invoice, sentDate);
        } catch (Exception e) {
            // Ignore reflection errors
        }

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getSentDate()).isNotNull();
        assertThat(result.getSentDate()).isEqualTo(sentDate);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return invoice with single item")
    void shouldReturnInvoiceWithSingleItem() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Henry", "Ford", "88888888888", "8888888888");
        ContactInfo contactInfo = new ContactInfo("henry.ford@example.com", "+48888888888");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            List.of(new InvoiceItem("Single Product", 1, new BigDecimal("999.99"), new BigDecimal("999.99"))),
            new BigDecimal("999.99"),
            new BigDecimal("230.00"),
            new BigDecimal("1229.99"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getDescription()).isEqualTo("Single Product");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualTo(new BigDecimal("999.99"));
        assertThat(result.getItems().get(0).getTotalPrice()).isEqualTo(new BigDecimal("999.99"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("1229.99"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return correct order ID for invoice")
    void shouldReturnCorrectOrderIdForInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();

        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Irene", "Adler", "99999999999", "9999999999");
        ContactInfo contactInfo = new ContactInfo("irene.adler@example.com", "+48999999999");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.fromString(orderId)),
            List.of(new InvoiceItem("Product K", 1, new BigDecimal("123.45"), new BigDecimal("123.45"))),
            new BigDecimal("123.45"),
            new BigDecimal("0.00"),
            new BigDecimal("123.45"),
            InvoiceStatus.DRAFT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act
        InvoiceDto result = getInvoiceByIdUseCase.handle(query);

        // Assert
        assertThat(result.getOrderId()).isEqualTo(orderId);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
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
