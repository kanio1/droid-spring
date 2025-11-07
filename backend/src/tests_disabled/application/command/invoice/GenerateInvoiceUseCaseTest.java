package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.GenerateInvoiceCommand;
import com.droid.bss.application.dto.invoice.InvoiceDto;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.invoice.Invoice;
import com.droid.bss.domain.invoice.InvoiceItem;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.order.Order;
import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.order.OrderRepository;
import com.droid.bss.domain.payment.Payment;
import com.droid.bss.domain.payment.PaymentRepository;
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
 * Test for GenerateInvoiceUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateInvoiceUseCase Application Layer")
class GenerateInvoiceUseCaseTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private GenerateInvoiceUseCase generateInvoiceUseCase;

    @Test
    @DisplayName("Should generate invoice from paid order successfully")
    void shouldGenerateInvoiceFromPaidOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025",
            null // dueDate null, should be calculated
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Order is paid
        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(new InvoiceItem("Product A", 2, new BigDecimal("99.99"), new BigDecimal("199.98"))),
            new BigDecimal("199.98"),
            new BigDecimal("0.00"), // tax
            new BigDecimal("199.98"), // total
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getCustomerId()).isEqualTo(customer.getId().toString());
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("199.98"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("199.98"));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should generate invoice with tax calculation")
    void shouldGenerateInvoiceWithTaxCalculation() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025-001",
            LocalDate.now().plusDays(30).toString()
        );

        CustomerInfo personalInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");
        ContactInfo contactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrderWithMultipleItems(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(
                new InvoiceItem("Product A", 1, new BigDecimal("100.00"), new BigDecimal("100.00")),
                new InvoiceItem("Product B", 2, new BigDecimal("50.00"), new BigDecimal("100.00"))
            ),
            new BigDecimal("200.00"),
            new BigDecimal("46.00"), // 23% tax
            new BigDecimal("246.00"),
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("200.00"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("46.00"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("246.00"));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025",
            null
        );

        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            generateInvoiceUseCase.handle(command);
        }, "Should throw exception when order not found");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when order is not paid")
    void shouldThrowExceptionWhenOrderIsNotPaid() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025",
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        // Order is still PENDING (not paid)
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            generateInvoiceUseCase.handle(command);
        }, "Should throw exception when order is not paid");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when invoice already exists for order")
    void shouldThrowExceptionWhenInvoiceAlreadyExistsForOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025",
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        // Invoice already exists
        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByOrderId(eq(expectedOrderId))).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            generateInvoiceUseCase.handle(command);
        }, "Should throw exception when invoice already exists");

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).existsByOrderId(eq(expectedOrderId));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should calculate due date when not provided")
    void shouldCalculateDueDateWhenNotProvided() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025",
            null // dueDate not provided
        );

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(new InvoiceItem("Product", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        // Due date should be calculated (30 days from now)
        assertThat(result.getDueDate()).isNotNull();

        verify(invoiceRepository).save(argThat(invoice ->
            invoice.getDueDate() != null &&
            invoice.getDueDate().isAfter(LocalDateTime.now())
        ));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should use provided due date when specified")
    void shouldUseProvidedDueDateWhenSpecified() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        LocalDate customDueDate = LocalDate.now().plusDays(60);

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025-002",
            customDueDate.toString()
        );

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(new InvoiceItem("Product", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getDueDate()).isEqualTo(customDueDate.atStartOfDay());

        verify(invoiceRepository).save(argThat(invoice ->
            invoice.getDueDate().toLocalDate().equals(customDueDate)
        ));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should generate invoice number automatically when not provided")
    void shouldGenerateInvoiceNumberAutomatically() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            null, // invoiceNumber not provided
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(new InvoiceItem("Product", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getInvoiceNumber()).isNotNull();
        assertThat(result.getInvoiceNumber()).startsWith("INV-");

        verify(invoiceRepository).save(argThat(invoice ->
            invoice.getInvoiceNumber() != null &&
            invoice.getInvoiceNumber().startsWith("INV-")
        ));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should generate invoice with multiple items from order")
    void shouldGenerateInvoiceWithMultipleItemsFromOrder() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025-MULTI",
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Frank", "Miller", "66666666666", "6666666666");
        ContactInfo contactInfo = new ContactInfo("frank.miller@example.com", "+48666666666");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrderWithMultipleItems(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(
                new InvoiceItem("Product A", 2, new BigDecimal("100.00"), new BigDecimal("200.00")),
                new InvoiceItem("Product B", 1, new BigDecimal("50.00"), new BigDecimal("50.00")),
                new InvoiceItem("Product C", 3, new BigDecimal("25.00"), new BigDecimal("75.00"))
            ),
            new BigDecimal("325.00"),
            new BigDecimal("74.75"), // 23% tax
            new BigDecimal("399.75"),
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getItems()).hasSize(3);
        assertThat(result.getSubtotal()).isEqualTo(new BigDecimal("325.00"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("74.75"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("399.75"));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should create invoice with DRAFT status by default")
    void shouldCreateInvoiceWithDraftStatusByDefault() {
        // Arrange
        String orderId = UUID.randomUUID().toString();

        GenerateInvoiceCommand command = new GenerateInvoiceCommand(
            orderId,
            "INV-2025",
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Grace", "Hopper", "77777777777", "7777777777");
        ContactInfo contactInfo = new ContactInfo("grace.hopper@example.com", "+48777777777");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Order order = createTestOrder(customer.getId());
        OrderId expectedOrderId = new OrderId(UUID.fromString(orderId));

        setOrderStatus(order, "PAID");

        Invoice expectedInvoice = Invoice.create(
            customer.getId(),
            expectedOrderId,
            List.of(new InvoiceItem("Product", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            InvoiceStatus.DRAFT
        );

        when(orderRepository.findById(eq(expectedOrderId))).thenReturn(Optional.of(order));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(expectedInvoice);

        // Act
        InvoiceDto result = generateInvoiceUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo("DRAFT");

        verify(invoiceRepository).save(argThat(invoice ->
            invoice.getStatus() == InvoiceStatus.DRAFT
        ));

        verify(orderRepository).findById(eq(expectedOrderId));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // Helper methods
    private Order createTestOrder(CustomerId customerId) {
        Order order = Order.create(
            customerId,
            List.of(new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("99.99"))),
            "SHIPPING_ADDRESS"
        );
        return order;
    }

    private Order createTestOrderWithMultipleItems(CustomerId customerId) {
        List<OrderItem> items = List.of(
            new OrderItem(new ProductId(UUID.randomUUID()), 1, new BigDecimal("100.00")),
            new OrderItem(new ProductId(UUID.randomUUID()), 2, new BigDecimal("50.00"))
        );
        Order order = Order.create(customerId, items, "SHIPPING_ADDRESS");
        return order;
    }

    private void setOrderStatus(Order order, String status) {
        try {
            java.lang.reflect.Method setStatusMethod = Order.class.getDeclaredMethod("setStatus", String.class);
            setStatusMethod.setAccessible(true);
            setStatusMethod.invoke(order, status);
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    // Helper class for ProductId
    private static class ProductId {
        private final UUID value;

        public ProductId(UUID value) {
            this.value = value;
        }

        public UUID getValue() {
            return value;
        }
    }
}
