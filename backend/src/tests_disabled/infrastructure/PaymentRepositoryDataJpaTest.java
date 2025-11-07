package com.droid.bss.infrastructure;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
import org.junit.jupiter.api.Disabled;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.invoice.repository.InvoiceRepository;
import com.droid.bss.domain.payment.*;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test scaffolding for PaymentRepository
 * Tests all custom query methods and CRUD operations
 */
@DataJpaTest
@Testcontainers
@EnableJpaAuditing
@Transactional
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@DisplayName("PaymentRepository JPA Layer - Test Scaffolding")
class PaymentRepositoryDataJpaTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerEntityRepository customerEntityRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private PaymentEntity testPayment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerEntityRepository.deleteAll();
    }

    private CustomerEntity createFreshCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+48123456789");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer = customerEntityRepository.saveAndFlush(customer);
        entityManager.clear();
        customer = customerEntityRepository.findById(customer.getId()).orElseThrow();
        return customer;
    }

    private InvoiceEntity createFreshInvoice(CustomerEntity customer) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8));
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now().minusDays(10));
        invoice.setDueDate(LocalDate.now().plusDays(20));
        invoice.setSubtotal(new BigDecimal("99.99"));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        invoice = invoiceRepository.saveAndFlush(invoice);
        entityManager.clear();
        invoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        return invoice;
    }

    // CRUD Operations

    @Test
    @DisplayName("should save and retrieve payment by ID")
    void shouldSaveAndRetrievePaymentById() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity savedPayment = paymentRepository.save(payment);
        entityManager.flush();
        entityManager.refresh(savedPayment);

        // When
        var retrieved = paymentRepository.findById(savedPayment.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPaymentNumber()).isEqualTo("PAY-001");
        assertThat(retrieved.get().getAmount()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("should save multiple payments and retrieve all")
    void shouldSaveMultiplePaymentsAndRetrieveAll() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            new BigDecimal("199.99"),
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();

        // When
        List<PaymentEntity> allPayments = paymentRepository.findAll();

        // Then
        assertThat(allPayments).hasSize(2);
    }

    @Test
    @DisplayName("should delete payment by ID")
    void shouldDeletePaymentById() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity savedPayment = paymentRepository.save(payment);
        entityManager.flush();
        entityManager.refresh(savedPayment);

        // When
        paymentRepository.deleteById(savedPayment.getId());
        entityManager.flush();

        // Then
        assertThat(paymentRepository.findById(savedPayment.getId())).isEmpty();
    }

    // Custom Query Methods

    @Test
    @DisplayName("should find payment by payment number")
    void shouldFindPaymentByPaymentNumber() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = paymentRepository.findByPaymentNumber("PAY-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    @DisplayName("should return empty when payment number not found")
    void shouldReturnEmptyWhenPaymentNumberNotFound() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = paymentRepository.findByPaymentNumber("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find payments by customer with pagination")
    void shouldFindPaymentsByCustomerWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> customerPayments = paymentRepository.findByCustomer(customer, pageable);

        // Then
        assertThat(customerPayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find payments by customer ID with pagination")
    void shouldFindPaymentsByCustomerIdWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> customerPayments = paymentRepository.findByCustomerId(customer.getId(), pageable);

        // Then
        assertThat(customerPayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find payments by invoice with pagination")
    void shouldFindPaymentsByInvoiceWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> invoicePayments = paymentRepository.findByInvoice(invoice, pageable);

        // Then
        assertThat(invoicePayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find payments by invoice ID with pagination")
    void shouldFindPaymentsByInvoiceIdWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> invoicePayments = paymentRepository.findByInvoiceId(invoice.getId(), pageable);

        // Then
        assertThat(invoicePayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find payments by payment status with pagination")
    void shouldFindPaymentsByPaymentStatusWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity completedPayment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity completedPayment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity pendingPayment = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(completedPayment1, completedPayment2, pendingPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> completedPayments = paymentRepository.findByPaymentStatus(PaymentStatus.COMPLETED, pageable);

        // Then
        assertThat(completedPayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find payments by payment method with pagination")
    void shouldFindPaymentsByPaymentMethodWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity cardPayment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity cardPayment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity bankPayment = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(cardPayment1, cardPayment2, bankPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> cardPayments = paymentRepository.findByPaymentMethod(PaymentMethod.CREDIT_CARD, pageable);

        // Then
        assertThat(cardPayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find completed payments")
    void shouldFindCompletedPayments() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity completedPayment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity pendingPayment = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(completedPayment, pendingPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> completedPayments = paymentRepository.findCompletedPayments(PaymentStatus.COMPLETED, pageable);

        // Then
        assertThat(completedPayments.getContent()).hasSize(1);
        assertThat(completedPayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should find pending payments")
    void shouldFindPendingPayments() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity pendingPayment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        PaymentEntity pendingPayment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        PaymentEntity completedPayment = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(pendingPayment1, pendingPayment2, completedPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> pendingPayments = paymentRepository.findPendingPayments(PaymentStatus.PENDING, pageable);

        // Then
        assertThat(pendingPayments.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find payments by payment date range")
    void shouldFindPaymentsByPaymentDateRange() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity currentPayment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity pastPayment = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now().minusDays(30)
        );
        PaymentEntity futurePayment = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now().plusDays(30)
        );
        paymentRepository.saveAll(List.of(currentPayment, pastPayment, futurePayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> rangePayments = paymentRepository.findByPaymentDateRange(
            LocalDate.now().minusDays(10),
            LocalDate.now().plusDays(10),
            pageable);

        // Then
        assertThat(rangePayments.getContent()).hasSize(1);
        assertThat(rangePayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should search payments by payment number or reference number")
    void shouldSearchPaymentsByPaymentNumberOrReferenceNumber() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        payment2.setReferenceNumber("REF-12345");

        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> searchResults = paymentRepository.searchPayments("REF", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-002");
    }

    @Test
    @DisplayName("should find payments by gateway")
    void shouldFindPaymentsByGateway() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity stripePayment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        stripePayment.setGateway("STRIPE");

        PaymentEntity paypalPayment = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paypalPayment.setGateway("PAYPAL");

        paymentRepository.saveAll(List.of(stripePayment, paypalPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> stripePayments = paymentRepository.findByGateway("STRIPE", pageable);

        // Then
        assertThat(stripePayments.getContent()).hasSize(1);
        assertThat(stripePayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should find payment by transaction ID")
    void shouldFindPaymentByTransactionId() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        payment.setTransactionId("TXN-123456789");
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = paymentRepository.findByTransactionId("TXN-123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should count payments by payment status")
    void shouldCountPaymentsByPaymentStatus() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity completedPayment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity completedPayment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity pendingPayment = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(completedPayment1, completedPayment2, pendingPayment));
        entityManager.flush();

        // When
        long completedCount = paymentRepository.countByPaymentStatus(PaymentStatus.COMPLETED);
        long pendingCount = paymentRepository.countByPaymentStatus(PaymentStatus.PENDING);

        // Then
        assertThat(completedCount).isEqualTo(2);
        assertThat(pendingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should count payments by customer")
    void shouldCountPaymentsByCustomer() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();

        // When
        long customerPaymentCount = paymentRepository.countByCustomer(customer);

        // Then
        assertThat(customerPaymentCount).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if payment number exists")
    void shouldCheckIfPaymentNumberExists() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.save(payment);
        entityManager.flush();

        // When & Then
        assertThat(paymentRepository.existsByPaymentNumber("PAY-001")).isTrue();
        assertThat(paymentRepository.existsByPaymentNumber("NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("should find failed payments that can be retried")
    void shouldFindFailedPaymentsThatCanBeRetried() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity failedPayment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.FAILED,
            LocalDate.now()
        );
        PaymentEntity completedPayment = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(failedPayment, completedPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> failedPayments = paymentRepository.findFailedPayments(PaymentStatus.FAILED, pageable);

        // Then
        assertThat(failedPayments.getContent()).hasSize(1);
        assertThat(failedPayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should find refunded payments")
    void shouldFindRefundedPayments() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity refundedPayment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.REFUNDED,
            LocalDate.now()
        );
        PaymentEntity completedPayment = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(refundedPayment, completedPayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> refundedPayments = paymentRepository.findRefundedPayments(PaymentStatus.REFUNDED, pageable);

        // Then
        assertThat(refundedPayments.getContent()).hasSize(1);
        assertThat(refundedPayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should find payments by amount range")
    void shouldFindPaymentsByAmountRange() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity smallPayment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("50.00"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity largePayment = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            new BigDecimal("200.00"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(smallPayment, largePayment));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> rangePayments = paymentRepository.findByAmountRange(
            40.00,
            150.00,
            pageable);

        // Then
        assertThat(rangePayments.getContent()).hasSize(1);
        assertThat(rangePayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-001");
    }

    @Test
    @DisplayName("should calculate total payments by customer and date range")
    void shouldCalculateTotalPaymentsByCustomerAndDateRange() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity currentPayment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity currentPayment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            new BigDecimal("50.00"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity pastPayment = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            new BigDecimal("100.00"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now().minusDays(60)
        );
        paymentRepository.saveAll(List.of(currentPayment1, currentPayment2, pastPayment));
        entityManager.flush();

        // When
        Double total = paymentRepository.sumPaymentsByCustomerAndDateRange(
            customer.getId(),
            LocalDate.now().minusDays(30),
            LocalDate.now(),
            PaymentStatus.COMPLETED);

        // Then
        assertThat(total).isEqualTo(149.99);
    }

    @Test
    @DisplayName("should find payments by payment method and status")
    void shouldFindPaymentsByPaymentMethodAndStatus() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity cardCompleted = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity bankCompleted = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity bankPending = new PaymentEntity(
            "PAY-003",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING,
            LocalDate.now()
        );
        paymentRepository.saveAll(List.of(cardCompleted, bankCompleted, bankPending));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> bankCompletedPayments = paymentRepository.findByPaymentMethodAndStatus(
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED,
            pageable);

        // Then
        assertThat(bankCompletedPayments.getContent()).hasSize(1);
        assertThat(bankCompletedPayments.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-002");
    }

    @Test
    @DisplayName("should return empty when no payments match search criteria")
    void shouldReturnEmptyWhenNoPaymentsMatchSearchCriteria() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> nonExistentPayments = paymentRepository.findByPaymentStatus(PaymentStatus.REFUNDED, pageable);

        // Then
        assertThat(nonExistentPayments.getContent()).isEmpty();
        assertThat(nonExistentPayments.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("should handle case-insensitive search")
    void shouldHandleCaseInsensitiveSearch() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = createFreshInvoice(customer);
        PaymentEntity payment1 = new PaymentEntity(
            "PAY-001",
            customer,
            invoice,
            new BigDecimal("99.99"),
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        PaymentEntity payment2 = new PaymentEntity(
            "PAY-002",
            customer,
            invoice,
            BigDecimal.ONE,
            "PLN",
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED,
            LocalDate.now()
        );
        payment2.setReferenceNumber("CREDIT-CARD-PAYMENT");

        paymentRepository.saveAll(List.of(payment1, payment2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PaymentEntity> searchResults = paymentRepository.searchPayments("credit", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getPaymentNumber()).isEqualTo("PAY-002");
    }
}
