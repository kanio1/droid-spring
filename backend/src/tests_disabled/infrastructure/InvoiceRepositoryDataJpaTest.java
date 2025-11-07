package com.droid.bss.infrastructure;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.invoice.*;
import com.droid.bss.domain.invoice.repository.InvoiceEntityRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test scaffolding for InvoiceRepository
 * Tests all custom query methods and CRUD operations
 */
@DataJpaTest
@Testcontainers
@EnableJpaAuditing
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@DisplayName("InvoiceRepository JPA Layer - Test Scaffolding")
class InvoiceRepositoryDataJpaTest {

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
    private InvoiceEntityRepository invoiceRepository;

    @Autowired
    private CustomerEntityRepository customerEntityRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        customerEntityRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
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
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        invoice = invoiceRepository.saveAndFlush(invoice);
        entityManager.clear();
        invoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        return invoice;
    }

    // CRUD Operations

    @Test
    @DisplayName("should save and retrieve invoice by ID")
    void shouldSaveAndRetrieveInvoiceById() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now().minusDays(10));
        invoice.setDueDate(LocalDate.now().plusDays(20));
        invoice.setSubtotal(new BigDecimal("99.99"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.refresh(savedInvoice);

        // When
        var retrieved = invoiceRepository.findById(savedInvoice.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getInvoiceNumber()).isEqualTo("INV-001");
        assertThat(retrieved.get().getTotalAmount()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("should save multiple invoices and retrieve all")
    void shouldSaveMultipleInvoicesAndRetrieveAll() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();

        InvoiceEntity invoice1 = new InvoiceEntity();
        invoice1.setInvoiceNumber("INV-001");
        invoice1.setCustomer(customer);
        invoice1.setInvoiceType(InvoiceType.RECURRING);
        invoice1.setStatus(InvoiceStatus.SENT);
        invoice1.setIssueDate(LocalDate.now().minusDays(10));
        invoice1.setDueDate(LocalDate.now().plusDays(20));
        invoice1.setSubtotal(new BigDecimal("99.99"));
        invoice1.setTaxAmount(new BigDecimal("0.00"));
        invoice1.setTotalAmount(new BigDecimal("99.99"));
        invoice1.setCurrency("PLN");

        InvoiceEntity invoice2 = new InvoiceEntity();
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setCustomer(customer);
        invoice2.setInvoiceType(InvoiceType.USAGE);
        invoice2.setStatus(InvoiceStatus.DRAFT);
        invoice2.setIssueDate(LocalDate.now());
        invoice2.setDueDate(LocalDate.now().plusDays(30));
        invoice2.setSubtotal(new BigDecimal("199.99"));
        invoice2.setTaxAmount(new BigDecimal("0.00"));
        invoice2.setTotalAmount(new BigDecimal("199.99"));
        invoice2.setCurrency("PLN");

        // When
        invoiceRepository.saveAll(List.of(invoice1, invoice2));
        entityManager.flush();
        entityManager.clear();

        // Then
        List<InvoiceEntity> allInvoices = invoiceRepository.findAll();
        assertThat(allInvoices).hasSize(2);
    }

    @Test
    @DisplayName("should delete invoice by ID")
    void shouldDeleteInvoiceById() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now().minusDays(10));
        invoice.setDueDate(LocalDate.now().plusDays(20));
        invoice.setSubtotal(new BigDecimal("99.99"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.refresh(savedInvoice);

        // When
        invoiceRepository.deleteById(savedInvoice.getId());
        entityManager.flush();

        // Then
        assertThat(invoiceRepository.findById(savedInvoice.getId())).isEmpty();
    }

    // Custom Query Methods

    @Test
    @DisplayName("should find invoice by invoice number")
    void shouldFindInvoiceByInvoiceNumber() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now().minusDays(10));
        invoice.setDueDate(LocalDate.now().plusDays(20));
        invoice.setSubtotal(new BigDecimal("99.99"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = invoiceRepository.findByInvoiceNumber("INV-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceType()).isEqualTo(InvoiceType.RECURRING);
    }

    @Test
    @DisplayName("should return empty when invoice number not found")
    void shouldReturnEmptyWhenInvoiceNumberNotFound() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now().minusDays(10));
        invoice.setDueDate(LocalDate.now().plusDays(20));
        invoice.setSubtotal(new BigDecimal("99.99"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = invoiceRepository.findByInvoiceNumber("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find invoices by customer with pagination")
    void shouldFindInvoicesByCustomerWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice1 = new InvoiceEntity();
        invoice1.setInvoiceNumber("INV-001");
        invoice1.setCustomer(customer);
        invoice1.setInvoiceType(InvoiceType.RECURRING);
        invoice1.setStatus(InvoiceStatus.SENT);
        invoice1.setIssueDate(LocalDate.now().minusDays(10));
        invoice1.setDueDate(LocalDate.now().plusDays(20));
        invoice1.setSubtotal(new BigDecimal("99.99"));
        invoice1.setTaxAmount(new BigDecimal("0.00"));
        invoice1.setTotalAmount(new BigDecimal("99.99"));
        invoice1.setCurrency("PLN");
        InvoiceEntity invoice2 = new InvoiceEntity();
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setCustomer(customer);
        invoice2.setInvoiceType(InvoiceType.RECURRING);
        invoice2.setStatus(InvoiceStatus.SENT);
        invoice2.setIssueDate(LocalDate.now());
        invoice2.setDueDate(LocalDate.now().plusDays(30));
        invoice2.setSubtotal(BigDecimal.ONE);
        invoice2.setTaxAmount(BigDecimal.ZERO);
        invoice2.setTotalAmount(BigDecimal.ONE);
        invoice2.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(invoice1, invoice2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> customerInvoices = invoiceRepository.findByCustomer(customer, pageable);

        // Then
        assertThat(customerInvoices.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find invoices by customer ID with pagination")
    void shouldFindInvoicesByCustomerIdWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice1 = new InvoiceEntity();
        invoice1.setInvoiceNumber("INV-001");
        invoice1.setCustomer(customer);
        invoice1.setInvoiceType(InvoiceType.RECURRING);
        invoice1.setStatus(InvoiceStatus.SENT);
        invoice1.setIssueDate(LocalDate.now().minusDays(10));
        invoice1.setDueDate(LocalDate.now().plusDays(20));
        invoice1.setSubtotal(new BigDecimal("99.99"));
        invoice1.setTaxAmount(new BigDecimal("0.00"));
        invoice1.setTotalAmount(new BigDecimal("99.99"));
        invoice1.setCurrency("PLN");
        InvoiceEntity invoice2 = new InvoiceEntity();
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setCustomer(customer);
        invoice2.setInvoiceType(InvoiceType.RECURRING);
        invoice2.setStatus(InvoiceStatus.SENT);
        invoice2.setIssueDate(LocalDate.now());
        invoice2.setDueDate(LocalDate.now().plusDays(30));
        invoice2.setSubtotal(BigDecimal.ONE);
        invoice2.setTaxAmount(BigDecimal.ZERO);
        invoice2.setTotalAmount(BigDecimal.ONE);
        invoice2.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(invoice1, invoice2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> customerInvoices = invoiceRepository.findByCustomerId(customer.getId(), pageable);

        // Then
        assertThat(customerInvoices.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find invoices by status with pagination")
    void shouldFindInvoicesByStatusWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity sentInvoice1 = new InvoiceEntity();
        sentInvoice1.setInvoiceNumber("INV-001");
        sentInvoice1.setCustomer(customer);
        sentInvoice1.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice1.setStatus(InvoiceStatus.SENT);
        sentInvoice1.setIssueDate(LocalDate.now().minusDays(10));
        sentInvoice1.setDueDate(LocalDate.now().plusDays(20));
        sentInvoice1.setSubtotal(new BigDecimal("99.99"));
        sentInvoice1.setTaxAmount(new BigDecimal("0.00"));
        sentInvoice1.setTotalAmount(new BigDecimal("99.99"));
        sentInvoice1.setCurrency("PLN");
        InvoiceEntity sentInvoice2 = new InvoiceEntity();
        sentInvoice2.setInvoiceNumber("INV-002");
        sentInvoice2.setCustomer(customer);
        sentInvoice2.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice2.setStatus(InvoiceStatus.SENT);
        sentInvoice2.setIssueDate(LocalDate.now());
        sentInvoice2.setDueDate(LocalDate.now().plusDays(30));
        sentInvoice2.setSubtotal(BigDecimal.ONE);
        sentInvoice2.setTaxAmount(BigDecimal.ZERO);
        sentInvoice2.setTotalAmount(BigDecimal.ONE);
        sentInvoice2.setCurrency("PLN");
        InvoiceEntity draftInvoice = new InvoiceEntity();
        draftInvoice.setInvoiceNumber("INV-003");
        draftInvoice.setCustomer(customer);
        draftInvoice.setInvoiceType(InvoiceType.USAGE);
        draftInvoice.setStatus(InvoiceStatus.DRAFT);
        draftInvoice.setIssueDate(LocalDate.now());
        draftInvoice.setDueDate(LocalDate.now().plusDays(30));
        draftInvoice.setSubtotal(BigDecimal.ONE);
        draftInvoice.setTaxAmount(BigDecimal.ZERO);
        draftInvoice.setTotalAmount(BigDecimal.ONE);
        draftInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(sentInvoice1, sentInvoice2, draftInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> sentInvoices = invoiceRepository.findByStatus(InvoiceStatus.SENT, pageable);

        // Then
        assertThat(sentInvoices.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find invoices by invoice type with pagination")
    void shouldFindInvoicesByInvoiceTypeWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity subscriptionInvoice = new InvoiceEntity();
        subscriptionInvoice.setInvoiceNumber("INV-001");
        subscriptionInvoice.setCustomer(customer);
        subscriptionInvoice.setInvoiceType(InvoiceType.RECURRING);
        subscriptionInvoice.setStatus(InvoiceStatus.SENT);
        subscriptionInvoice.setIssueDate(LocalDate.now().minusDays(10));
        subscriptionInvoice.setDueDate(LocalDate.now().plusDays(20));
        subscriptionInvoice.setSubtotal(new BigDecimal("99.99"));
        subscriptionInvoice.setTaxAmount(new BigDecimal("0.00"));
        subscriptionInvoice.setTotalAmount(new BigDecimal("99.99"));
        subscriptionInvoice.setCurrency("PLN");
        InvoiceEntity usageInvoice = new InvoiceEntity();
        usageInvoice.setInvoiceNumber("INV-002");
        usageInvoice.setCustomer(customer);
        usageInvoice.setInvoiceType(InvoiceType.USAGE);
        usageInvoice.setStatus(InvoiceStatus.SENT);
        usageInvoice.setIssueDate(LocalDate.now());
        usageInvoice.setDueDate(LocalDate.now().plusDays(30));
        usageInvoice.setSubtotal(BigDecimal.ONE);
        usageInvoice.setTaxAmount(BigDecimal.ZERO);
        usageInvoice.setTotalAmount(BigDecimal.ONE);
        usageInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(subscriptionInvoice, usageInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> subscriptionInvoices = invoiceRepository.findByInvoiceType(InvoiceType.RECURRING, pageable);

        // Then
        assertThat(subscriptionInvoices.getContent()).hasSize(1);
        assertThat(subscriptionInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("should find unpaid invoices")
    void shouldFindUnpaidInvoices() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity sentInvoice = new InvoiceEntity();
        sentInvoice.setInvoiceNumber("INV-001");
        sentInvoice.setCustomer(customer);
        sentInvoice.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice.setStatus(InvoiceStatus.SENT);
        sentInvoice.setIssueDate(LocalDate.now().minusDays(10));
        sentInvoice.setDueDate(LocalDate.now().plusDays(20));
        sentInvoice.setSubtotal(new BigDecimal("99.99"));
        sentInvoice.setTaxAmount(new BigDecimal("0.00"));
        sentInvoice.setTotalAmount(new BigDecimal("99.99"));
        sentInvoice.setCurrency("PLN");
        InvoiceEntity overdueInvoice = new InvoiceEntity();
        overdueInvoice.setInvoiceNumber("INV-002");
        overdueInvoice.setCustomer(customer);
        overdueInvoice.setInvoiceType(InvoiceType.RECURRING);
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
        overdueInvoice.setIssueDate(LocalDate.now().minusDays(30));
        overdueInvoice.setDueDate(LocalDate.now().minusDays(10));
        overdueInvoice.setSubtotal(BigDecimal.ONE);
        overdueInvoice.setTaxAmount(BigDecimal.ZERO);
        overdueInvoice.setTotalAmount(BigDecimal.ONE);
        overdueInvoice.setCurrency("PLN");
        InvoiceEntity paidInvoice = new InvoiceEntity();
        paidInvoice.setInvoiceNumber("INV-003");
        paidInvoice.setCustomer(customer);
        paidInvoice.setInvoiceType(InvoiceType.RECURRING);
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setIssueDate(LocalDate.now());
        paidInvoice.setDueDate(LocalDate.now().plusDays(30));
        paidInvoice.setPaidDate(LocalDate.now());
        paidInvoice.setSubtotal(BigDecimal.ONE);
        paidInvoice.setTaxAmount(BigDecimal.ZERO);
        paidInvoice.setTotalAmount(BigDecimal.ONE);
        paidInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(sentInvoice, overdueInvoice, paidInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> unpaidInvoices = invoiceRepository.findUnpaidInvoices(
            List.of(InvoiceStatus.SENT, InvoiceStatus.OVERDUE, InvoiceStatus.DRAFT),
            pageable);

        // Then
        assertThat(unpaidInvoices.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find overdue invoices")
    void shouldFindOverdueInvoices() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity overdueInvoice = new InvoiceEntity();
        overdueInvoice.setInvoiceNumber("INV-001");
        overdueInvoice.setCustomer(customer);
        overdueInvoice.setInvoiceType(InvoiceType.RECURRING);
        overdueInvoice.setStatus(InvoiceStatus.SENT);
        overdueInvoice.setIssueDate(LocalDate.now().minusDays(30));
        overdueInvoice.setDueDate(LocalDate.now().minusDays(10));
        overdueInvoice.setSubtotal(BigDecimal.ONE);
        overdueInvoice.setTaxAmount(BigDecimal.ZERO);
        overdueInvoice.setTotalAmount(BigDecimal.ONE);
        overdueInvoice.setCurrency("PLN");
        InvoiceEntity sentInvoice = new InvoiceEntity();
        sentInvoice.setInvoiceNumber("INV-002");
        sentInvoice.setCustomer(customer);
        sentInvoice.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice.setStatus(InvoiceStatus.SENT);
        sentInvoice.setIssueDate(LocalDate.now());
        sentInvoice.setDueDate(LocalDate.now().plusDays(30));
        sentInvoice.setSubtotal(BigDecimal.ONE);
        sentInvoice.setTaxAmount(BigDecimal.ZERO);
        sentInvoice.setTotalAmount(BigDecimal.ONE);
        sentInvoice.setCurrency("PLN");
        InvoiceEntity paidInvoice = new InvoiceEntity();
        paidInvoice.setInvoiceNumber("INV-003");
        paidInvoice.setCustomer(customer);
        paidInvoice.setInvoiceType(InvoiceType.RECURRING);
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setIssueDate(LocalDate.now());
        paidInvoice.setDueDate(LocalDate.now().plusDays(30));
        paidInvoice.setSubtotal(BigDecimal.ONE);
        paidInvoice.setTaxAmount(BigDecimal.ZERO);
        paidInvoice.setTotalAmount(BigDecimal.ONE);
        paidInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(overdueInvoice, sentInvoice, paidInvoice));
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate currentDate = LocalDate.now();

        // When
        Page<InvoiceEntity> overdueInvoices = invoiceRepository.findOverdueInvoices(
            List.of(InvoiceStatus.OVERDUE, InvoiceStatus.SENT),
            currentDate,
            pageable);

        // Then
        assertThat(overdueInvoices.getContent()).hasSize(1);
        assertThat(overdueInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("should find invoices by issue date range")
    void shouldFindInvoicesByIssueDateRange() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        CustomerEntity customer3 = createFreshCustomer();
        InvoiceEntity currentInvoice = new InvoiceEntity();
        currentInvoice.setInvoiceNumber("INV-001");
        currentInvoice.setCustomer(customer1);
        currentInvoice.setInvoiceType(InvoiceType.RECURRING);
        currentInvoice.setStatus(InvoiceStatus.SENT);
        currentInvoice.setIssueDate(LocalDate.now());
        currentInvoice.setDueDate(LocalDate.now().plusDays(30));
        currentInvoice.setSubtotal(BigDecimal.ONE);
        currentInvoice.setTaxAmount(BigDecimal.ZERO);
        currentInvoice.setTotalAmount(BigDecimal.ONE);
        currentInvoice.setCurrency("PLN");
        InvoiceEntity pastInvoice = new InvoiceEntity();
        pastInvoice.setInvoiceNumber("INV-002");
        pastInvoice.setCustomer(customer2);
        pastInvoice.setInvoiceType(InvoiceType.RECURRING);
        pastInvoice.setStatus(InvoiceStatus.SENT);
        pastInvoice.setIssueDate(LocalDate.now().minusDays(30));
        pastInvoice.setDueDate(LocalDate.now().plusDays(30));
        pastInvoice.setSubtotal(BigDecimal.ONE);
        pastInvoice.setTaxAmount(BigDecimal.ZERO);
        pastInvoice.setTotalAmount(BigDecimal.ONE);
        pastInvoice.setCurrency("PLN");
        InvoiceEntity futureInvoice = new InvoiceEntity();
        futureInvoice.setInvoiceNumber("INV-003");
        futureInvoice.setCustomer(customer3);
        futureInvoice.setInvoiceType(InvoiceType.RECURRING);
        futureInvoice.setStatus(InvoiceStatus.DRAFT);
        futureInvoice.setIssueDate(LocalDate.now().plusDays(30));
        futureInvoice.setDueDate(LocalDate.now().plusDays(60));
        futureInvoice.setSubtotal(BigDecimal.ONE);
        futureInvoice.setTaxAmount(BigDecimal.ZERO);
        futureInvoice.setTotalAmount(BigDecimal.ONE);
        futureInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(currentInvoice, pastInvoice, futureInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> rangeInvoices = invoiceRepository.findByIssueDateRange(
            LocalDate.now().minusDays(10),
            LocalDate.now().plusDays(10),
            pageable);

        // Then
        assertThat(rangeInvoices.getContent()).hasSize(1);
        assertThat(rangeInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("should find invoices by due date range")
    void shouldFindInvoicesByDueDateRange() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        CustomerEntity customer3 = createFreshCustomer();
        InvoiceEntity currentInvoice = new InvoiceEntity();
        currentInvoice.setInvoiceNumber("INV-001");
        currentInvoice.setCustomer(customer1);
        currentInvoice.setInvoiceType(InvoiceType.RECURRING);
        currentInvoice.setStatus(InvoiceStatus.SENT);
        currentInvoice.setIssueDate(LocalDate.now());
        currentInvoice.setDueDate(LocalDate.now());
        currentInvoice.setSubtotal(BigDecimal.ONE);
        currentInvoice.setTaxAmount(BigDecimal.ZERO);
        currentInvoice.setTotalAmount(BigDecimal.ONE);
        currentInvoice.setCurrency("PLN");
        InvoiceEntity pastInvoice = new InvoiceEntity();
        pastInvoice.setInvoiceNumber("INV-002");
        pastInvoice.setCustomer(customer2);
        pastInvoice.setInvoiceType(InvoiceType.RECURRING);
        pastInvoice.setStatus(InvoiceStatus.SENT);
        pastInvoice.setIssueDate(LocalDate.now().minusDays(30));
        pastInvoice.setDueDate(LocalDate.now().minusDays(10));
        pastInvoice.setSubtotal(BigDecimal.ONE);
        pastInvoice.setTaxAmount(BigDecimal.ZERO);
        pastInvoice.setTotalAmount(BigDecimal.ONE);
        pastInvoice.setCurrency("PLN");
        InvoiceEntity futureInvoice = new InvoiceEntity();
        futureInvoice.setInvoiceNumber("INV-003");
        futureInvoice.setCustomer(customer3);
        futureInvoice.setInvoiceType(InvoiceType.RECURRING);
        futureInvoice.setStatus(InvoiceStatus.DRAFT);
        futureInvoice.setIssueDate(LocalDate.now());
        futureInvoice.setDueDate(LocalDate.now().plusDays(30));
        futureInvoice.setSubtotal(BigDecimal.ONE);
        futureInvoice.setTaxAmount(BigDecimal.ZERO);
        futureInvoice.setTotalAmount(BigDecimal.ONE);
        futureInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(currentInvoice, pastInvoice, futureInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> rangeInvoices = invoiceRepository.findByDueDateRange(
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            pageable);

        // Then
        assertThat(rangeInvoices.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should search invoices by invoice number or notes")
    void shouldSearchInvoicesByInvoiceNumberOrNotes() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity invoice1 = new InvoiceEntity();
        invoice1.setInvoiceNumber("INV-001");
        invoice1.setCustomer(customer1);
        invoice1.setInvoiceType(InvoiceType.RECURRING);
        invoice1.setStatus(InvoiceStatus.SENT);
        invoice1.setIssueDate(LocalDate.now());
        invoice1.setDueDate(LocalDate.now().plusDays(30));
        invoice1.setSubtotal(BigDecimal.ONE);
        invoice1.setTaxAmount(BigDecimal.ZERO);
        invoice1.setTotalAmount(BigDecimal.ONE);
        invoice1.setCurrency("PLN");
        InvoiceEntity invoice2 = new InvoiceEntity();
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setCustomer(customer2);
        invoice2.setInvoiceType(InvoiceType.RECURRING);
        invoice2.setStatus(InvoiceStatus.SENT);
        invoice2.setIssueDate(LocalDate.now());
        invoice2.setDueDate(LocalDate.now().plusDays(30));
        invoice2.setSubtotal(BigDecimal.ONE);
        invoice2.setTaxAmount(BigDecimal.ZERO);
        invoice2.setTotalAmount(BigDecimal.ONE);
        invoice2.setCurrency("PLN");
        invoice2.setNotes("Urgent payment required");

        invoiceRepository.saveAll(List.of(invoice1, invoice2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> searchResults = invoiceRepository.search("urgent", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-002");
    }

    @Test
    @DisplayName("should find paid invoices")
    void shouldFindPaidInvoices() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity paidInvoice = new InvoiceEntity();
        paidInvoice.setInvoiceNumber("INV-001");
        paidInvoice.setCustomer(customer1);
        paidInvoice.setInvoiceType(InvoiceType.RECURRING);
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setIssueDate(LocalDate.now().minusDays(30));
        paidInvoice.setDueDate(LocalDate.now().minusDays(10));
        paidInvoice.setPaidDate(LocalDate.now().minusDays(5));
        paidInvoice.setSubtotal(BigDecimal.ONE);
        paidInvoice.setTaxAmount(BigDecimal.ZERO);
        paidInvoice.setTotalAmount(BigDecimal.ONE);
        paidInvoice.setCurrency("PLN");
        InvoiceEntity unpaidInvoice = new InvoiceEntity();
        unpaidInvoice.setInvoiceNumber("INV-002");
        unpaidInvoice.setCustomer(customer2);
        unpaidInvoice.setInvoiceType(InvoiceType.RECURRING);
        unpaidInvoice.setStatus(InvoiceStatus.SENT);
        unpaidInvoice.setIssueDate(LocalDate.now());
        unpaidInvoice.setDueDate(LocalDate.now().plusDays(30));
        unpaidInvoice.setSubtotal(BigDecimal.ONE);
        unpaidInvoice.setTaxAmount(BigDecimal.ZERO);
        unpaidInvoice.setTotalAmount(BigDecimal.ONE);
        unpaidInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(paidInvoice, unpaidInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> paidInvoices = invoiceRepository.findPaidInvoices(InvoiceStatus.PAID, pageable);

        // Then
        assertThat(paidInvoices.getContent()).hasSize(1);
        assertThat(paidInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("should count invoices by status")
    void shouldCountInvoicesByStatus() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        CustomerEntity customer3 = createFreshCustomer();
        InvoiceEntity sentInvoice1 = new InvoiceEntity();
        sentInvoice1.setInvoiceNumber("INV-001");
        sentInvoice1.setCustomer(customer1);
        sentInvoice1.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice1.setStatus(InvoiceStatus.SENT);
        sentInvoice1.setIssueDate(LocalDate.now());
        sentInvoice1.setDueDate(LocalDate.now().plusDays(30));
        sentInvoice1.setSubtotal(BigDecimal.ONE);
        sentInvoice1.setTaxAmount(BigDecimal.ZERO);
        sentInvoice1.setTotalAmount(BigDecimal.ONE);
        sentInvoice1.setCurrency("PLN");
        InvoiceEntity sentInvoice2 = new InvoiceEntity();
        sentInvoice2.setInvoiceNumber("INV-002");
        sentInvoice2.setCustomer(customer2);
        sentInvoice2.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice2.setStatus(InvoiceStatus.SENT);
        sentInvoice2.setIssueDate(LocalDate.now());
        sentInvoice2.setDueDate(LocalDate.now().plusDays(30));
        sentInvoice2.setSubtotal(BigDecimal.ONE);
        sentInvoice2.setTaxAmount(BigDecimal.ZERO);
        sentInvoice2.setTotalAmount(BigDecimal.ONE);
        sentInvoice2.setCurrency("PLN");
        InvoiceEntity draftInvoice = new InvoiceEntity();
        draftInvoice.setInvoiceNumber("INV-003");
        draftInvoice.setCustomer(customer3);
        draftInvoice.setInvoiceType(InvoiceType.USAGE);
        draftInvoice.setStatus(InvoiceStatus.DRAFT);
        draftInvoice.setIssueDate(LocalDate.now());
        draftInvoice.setDueDate(LocalDate.now().plusDays(30));
        draftInvoice.setSubtotal(BigDecimal.ONE);
        draftInvoice.setTaxAmount(BigDecimal.ZERO);
        draftInvoice.setTotalAmount(BigDecimal.ONE);
        draftInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(sentInvoice1, sentInvoice2, draftInvoice));
        entityManager.flush();
        entityManager.clear();

        // When
        long sentCount = invoiceRepository.countByStatus(InvoiceStatus.SENT);
        long draftCount = invoiceRepository.countByStatus(InvoiceStatus.DRAFT);

        // Then
        assertThat(sentCount).isEqualTo(2);
        assertThat(draftCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should check if invoice number exists")
    void shouldCheckIfInvoiceNumberExists() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setSubtotal(BigDecimal.ONE);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ONE);
        invoice.setCurrency("PLN");
        invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertThat(invoiceRepository.existsByInvoiceNumber("INV-001")).isTrue();
        assertThat(invoiceRepository.existsByInvoiceNumber("NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("should find invoices sent via email")
    void shouldFindInvoicesSentViaEmail() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity sentInvoice = new InvoiceEntity();
        sentInvoice.setInvoiceNumber("INV-001");
        sentInvoice.setCustomer(customer1);
        sentInvoice.setInvoiceType(InvoiceType.RECURRING);
        sentInvoice.setStatus(InvoiceStatus.SENT);
        sentInvoice.setIssueDate(LocalDate.now());
        sentInvoice.setDueDate(LocalDate.now().plusDays(30));
        sentInvoice.setSubtotal(BigDecimal.ONE);
        sentInvoice.setTaxAmount(BigDecimal.ZERO);
        sentInvoice.setTotalAmount(BigDecimal.ONE);
        sentInvoice.setCurrency("PLN");
        sentInvoice.setSentToEmail("john.doe@example.com");
        sentInvoice.setSentAt(LocalDateTime.now());
        InvoiceEntity unsentInvoice = new InvoiceEntity();
        unsentInvoice.setInvoiceNumber("INV-002");
        unsentInvoice.setCustomer(customer2);
        unsentInvoice.setInvoiceType(InvoiceType.RECURRING);
        unsentInvoice.setStatus(InvoiceStatus.DRAFT);
        unsentInvoice.setIssueDate(LocalDate.now());
        unsentInvoice.setDueDate(LocalDate.now().plusDays(30));
        unsentInvoice.setSubtotal(BigDecimal.ONE);
        unsentInvoice.setTaxAmount(BigDecimal.ZERO);
        unsentInvoice.setTotalAmount(BigDecimal.ONE);
        unsentInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(sentInvoice, unsentInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> sentInvoices = invoiceRepository.findSentInvoices(pageable);

        // Then
        assertThat(sentInvoices.getContent()).hasSize(1);
        assertThat(sentInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("should find recent invoices")
    void shouldFindRecentInvoices() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity oldInvoice = new InvoiceEntity();
        oldInvoice.setInvoiceNumber("INV-001");
        oldInvoice.setCustomer(customer1);
        oldInvoice.setInvoiceType(InvoiceType.RECURRING);
        oldInvoice.setStatus(InvoiceStatus.PAID);
        oldInvoice.setIssueDate(LocalDate.now().minusDays(30));
        oldInvoice.setDueDate(LocalDate.now().minusDays(10));
        oldInvoice.setPaidDate(LocalDate.now().minusDays(5));
        oldInvoice.setSubtotal(BigDecimal.ONE);
        oldInvoice.setTaxAmount(BigDecimal.ZERO);
        oldInvoice.setTotalAmount(BigDecimal.ONE);
        oldInvoice.setCurrency("PLN");
        InvoiceEntity recentInvoice = new InvoiceEntity();
        recentInvoice.setInvoiceNumber("INV-002");
        recentInvoice.setCustomer(customer2);
        recentInvoice.setInvoiceType(InvoiceType.RECURRING);
        recentInvoice.setStatus(InvoiceStatus.SENT);
        recentInvoice.setIssueDate(LocalDate.now());
        recentInvoice.setDueDate(LocalDate.now().plusDays(30));
        recentInvoice.setSubtotal(BigDecimal.ONE);
        recentInvoice.setTaxAmount(BigDecimal.ZERO);
        recentInvoice.setTotalAmount(BigDecimal.ONE);
        recentInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(oldInvoice, recentInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> recentInvoices = invoiceRepository.findRecentInvoices(pageable);

        // Then
        assertThat(recentInvoices.getContent()).hasSize(2);
        assertThat(recentInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-002"); // Most recent first
    }

    @Test
    @DisplayName("should find invoices with total amount greater than")
    void shouldFindInvoicesWithTotalAmountGreaterThan() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity smallInvoice = new InvoiceEntity();
        smallInvoice.setInvoiceNumber("INV-001");
        smallInvoice.setCustomer(customer1);
        smallInvoice.setInvoiceType(InvoiceType.RECURRING);
        smallInvoice.setStatus(InvoiceStatus.SENT);
        smallInvoice.setIssueDate(LocalDate.now());
        smallInvoice.setDueDate(LocalDate.now().plusDays(30));
        smallInvoice.setSubtotal(new BigDecimal("50.00"));
        smallInvoice.setTaxAmount(BigDecimal.ZERO);
        smallInvoice.setTotalAmount(new BigDecimal("50.00"));
        smallInvoice.setCurrency("PLN");
        InvoiceEntity largeInvoice = new InvoiceEntity();
        largeInvoice.setInvoiceNumber("INV-002");
        largeInvoice.setCustomer(customer2);
        largeInvoice.setInvoiceType(InvoiceType.RECURRING);
        largeInvoice.setStatus(InvoiceStatus.SENT);
        largeInvoice.setIssueDate(LocalDate.now());
        largeInvoice.setDueDate(LocalDate.now().plusDays(30));
        largeInvoice.setSubtotal(new BigDecimal("200.00"));
        largeInvoice.setTaxAmount(BigDecimal.ZERO);
        largeInvoice.setTotalAmount(new BigDecimal("200.00"));
        largeInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(smallInvoice, largeInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> largeInvoices = invoiceRepository.findByTotalAmountGreaterThan(BigDecimal.valueOf(100.00), pageable);

        // Then
        assertThat(largeInvoices.getContent()).hasSize(1);
        assertThat(largeInvoices.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-002");
    }

    @Test
    @DisplayName("should find invoices by billing period")
    void shouldFindInvoicesByBillingPeriod() {
        // Given - fix version conflict
        LocalDate periodStart = LocalDate.of(2024, 1, 1);
        LocalDate periodEnd = LocalDate.of(2024, 12, 31);
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        CustomerEntity customer3 = createFreshCustomer();

        InvoiceEntity periodInvoice1 = new InvoiceEntity();
        periodInvoice1.setInvoiceNumber("INV-001");
        periodInvoice1.setCustomer(customer1);
        periodInvoice1.setInvoiceType(InvoiceType.RECURRING);
        periodInvoice1.setStatus(InvoiceStatus.SENT);
        periodInvoice1.setIssueDate(LocalDate.now());
        periodInvoice1.setDueDate(LocalDate.now().plusDays(30));
        periodInvoice1.setBillingPeriodStart(periodStart);
        periodInvoice1.setBillingPeriodEnd(periodEnd);
        periodInvoice1.setSubtotal(BigDecimal.ONE);
        periodInvoice1.setTaxAmount(BigDecimal.ZERO);
        periodInvoice1.setTotalAmount(BigDecimal.ONE);
        periodInvoice1.setCurrency("PLN");

        InvoiceEntity periodInvoice2 = new InvoiceEntity();
        periodInvoice2.setInvoiceNumber("INV-002");
        periodInvoice2.setCustomer(customer2);
        periodInvoice2.setInvoiceType(InvoiceType.RECURRING);
        periodInvoice2.setStatus(InvoiceStatus.SENT);
        periodInvoice2.setIssueDate(LocalDate.now());
        periodInvoice2.setDueDate(LocalDate.now().plusDays(30));
        periodInvoice2.setBillingPeriodStart(periodStart);
        periodInvoice2.setBillingPeriodEnd(periodEnd);
        periodInvoice2.setSubtotal(BigDecimal.ONE);
        periodInvoice2.setTaxAmount(BigDecimal.ZERO);
        periodInvoice2.setTotalAmount(BigDecimal.ONE);
        periodInvoice2.setCurrency("PLN");

        InvoiceEntity differentPeriodInvoice = new InvoiceEntity();
        differentPeriodInvoice.setInvoiceNumber("INV-003");
        differentPeriodInvoice.setCustomer(customer3);
        differentPeriodInvoice.setInvoiceType(InvoiceType.RECURRING);
        differentPeriodInvoice.setStatus(InvoiceStatus.SENT);
        differentPeriodInvoice.setIssueDate(LocalDate.now());
        differentPeriodInvoice.setDueDate(LocalDate.now().plusDays(30));
        differentPeriodInvoice.setBillingPeriodStart(LocalDate.of(2023, 1, 1));
        differentPeriodInvoice.setBillingPeriodEnd(LocalDate.of(2023, 12, 31));
        differentPeriodInvoice.setSubtotal(BigDecimal.ONE);
        differentPeriodInvoice.setTaxAmount(BigDecimal.ZERO);
        differentPeriodInvoice.setTotalAmount(BigDecimal.ONE);
        differentPeriodInvoice.setCurrency("PLN");

        invoiceRepository.saveAll(List.of(periodInvoice1, periodInvoice2, differentPeriodInvoice));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> periodInvoices = invoiceRepository.findByBillingPeriod(periodStart, periodEnd, pageable);

        // Then
        assertThat(periodInvoices.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find invoices needing to be sent")
    void shouldFindInvoicesNeedingToBeSent() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity draftInvoice = new InvoiceEntity();
        draftInvoice.setInvoiceNumber("INV-001");
        draftInvoice.setCustomer(customer1);
        draftInvoice.setInvoiceType(InvoiceType.RECURRING);
        draftInvoice.setStatus(InvoiceStatus.DRAFT);
        draftInvoice.setIssueDate(LocalDate.now());
        draftInvoice.setDueDate(LocalDate.now().plusDays(30));
        draftInvoice.setSubtotal(BigDecimal.ONE);
        draftInvoice.setTaxAmount(BigDecimal.ZERO);
        draftInvoice.setTotalAmount(BigDecimal.ONE);
        draftInvoice.setCurrency("PLN");
        InvoiceEntity paidInvoice = new InvoiceEntity();
        paidInvoice.setInvoiceNumber("INV-002");
        paidInvoice.setCustomer(customer2);
        paidInvoice.setInvoiceType(InvoiceType.RECURRING);
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setIssueDate(LocalDate.now());
        paidInvoice.setDueDate(LocalDate.now().plusDays(30));
        paidInvoice.setSubtotal(BigDecimal.ONE);
        paidInvoice.setTaxAmount(BigDecimal.ZERO);
        paidInvoice.setTotalAmount(BigDecimal.ONE);
        paidInvoice.setCurrency("PLN");
        invoiceRepository.saveAll(List.of(draftInvoice, paidInvoice));
        entityManager.flush();
        entityManager.clear();

        // When
        List<InvoiceEntity> invoicesToSend = invoiceRepository.findInvoicesToSend(InvoiceStatus.DRAFT);

        // Then
        assertThat(invoicesToSend).hasSize(1);
        assertThat(invoicesToSend.get(0).getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("should return empty when no invoices match search criteria")
    void shouldReturnEmptyWhenNoInvoicesMatchSearchCriteria() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setSubtotal(BigDecimal.ONE);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ONE);
        invoice.setCurrency("PLN");
        invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> nonExistentInvoices = invoiceRepository.findByStatus(InvoiceStatus.PAID, pageable);

        // Then
        assertThat(nonExistentInvoices.getContent()).isEmpty();
        assertThat(nonExistentInvoices.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("should handle case-insensitive search")
    void shouldHandleCaseInsensitiveSearch() {
        // Given - fix version conflict
        CustomerEntity customer1 = createFreshCustomer();
        CustomerEntity customer2 = createFreshCustomer();
        InvoiceEntity invoice1 = new InvoiceEntity();
        invoice1.setInvoiceNumber("INV-001");
        invoice1.setCustomer(customer1);
        invoice1.setInvoiceType(InvoiceType.RECURRING);
        invoice1.setStatus(InvoiceStatus.SENT);
        invoice1.setIssueDate(LocalDate.now());
        invoice1.setDueDate(LocalDate.now().plusDays(30));
        invoice1.setSubtotal(BigDecimal.ONE);
        invoice1.setTaxAmount(BigDecimal.ZERO);
        invoice1.setTotalAmount(BigDecimal.ONE);
        invoice1.setCurrency("PLN");
        InvoiceEntity invoice2 = new InvoiceEntity();
        invoice2.setInvoiceNumber("INV-002");
        invoice2.setCustomer(customer2);
        invoice2.setInvoiceType(InvoiceType.RECURRING);
        invoice2.setStatus(InvoiceStatus.SENT);
        invoice2.setIssueDate(LocalDate.now());
        invoice2.setDueDate(LocalDate.now().plusDays(30));
        invoice2.setSubtotal(BigDecimal.ONE);
        invoice2.setTaxAmount(BigDecimal.ZERO);
        invoice2.setTotalAmount(BigDecimal.ONE);
        invoice2.setCurrency("PLN");
        invoice2.setNotes("URGENT PAYMENT REQUIRED");

        invoiceRepository.saveAll(List.of(invoice1, invoice2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<InvoiceEntity> searchResults = invoiceRepository.searchInvoices("urgent", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getInvoiceNumber()).isEqualTo("INV-002");
    }

    @Test
    @DisplayName("should handle pagination with multiple pages")
    void shouldHandlePaginationWithMultiplePages() {
        // Given - fix version conflict
        for (int i = 1; i <= 25; i++) {
            CustomerEntity customer = createFreshCustomer();
            InvoiceEntity invoice = new InvoiceEntity();
            invoice.setInvoiceNumber("INV-" + String.format("%03d", i));
            invoice.setCustomer(customer);
            invoice.setInvoiceType(InvoiceType.RECURRING);
            invoice.setStatus(InvoiceStatus.SENT);
            invoice.setIssueDate(LocalDate.now());
            invoice.setDueDate(LocalDate.now().plusDays(30));
            invoice.setSubtotal(BigDecimal.ONE);
            invoice.setTaxAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(BigDecimal.ONE);
            invoice.setCurrency("PLN");
            invoiceRepository.save(invoice);
        }
        entityManager.flush();
        entityManager.clear();

        // When
        Page<InvoiceEntity> page1 = invoiceRepository.findByStatus(InvoiceStatus.SENT, PageRequest.of(0, 10));
        Page<InvoiceEntity> page2 = invoiceRepository.findByStatus(InvoiceStatus.SENT, PageRequest.of(1, 10));
        Page<InvoiceEntity> page3 = invoiceRepository.findByStatus(InvoiceStatus.SENT, PageRequest.of(2, 10));

        // Then
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(10);
        assertThat(page3.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(25);
    }
}
