package com.droid.bss.integration;

import org.springframework.context.annotation.Import;

import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.application.dto.invoice.CreateInvoiceCommand;
import com.droid.bss.application.dto.invoice.InvoiceResponse;
import com.droid.bss.application.dto.invoice.UpdateInvoiceCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.invoice.repository.InvoiceEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UpdateInvoiceUseCase
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.droid.bss.integration.config.IntegrationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "security.oauth2.audience=bss-backend"
})
@DisplayName("UpdateInvoice Integration Tests")
class UpdateInvoiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InvoiceEntityRepository invoiceRepository;

    @Autowired
    private CustomerEntityRepository customerEntityRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String validJwtToken = createValidJwtToken();

    @AfterEach
    void cleanup() {
        invoiceRepository.deleteAll();
        customerEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("Should update invoice successfully through REST API")
    void shouldUpdateInvoiceSuccessfully() {
        // Given - Create customer and invoice
        UUID customerId = createTestCustomer();
        UUID invoiceId = createTestInvoice(customerId);

        // When - Update invoice
        UpdateInvoiceCommand updateCommand = new UpdateInvoiceCommand(
                invoiceId,
                "INV-001-UPDATED",
                customerId.toString(),
                InvoiceType.USAGE,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(25),
                LocalDate.now().plusDays(25),
                new BigDecimal("150.00"),
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                new BigDecimal("165.00"),
                "USD",
                30,
                new BigDecimal("5.00"),
                "Updated invoice notes",
                "https://example.com/invoice-001-updated.pdf",
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5),
                0L
        );

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<UpdateInvoiceCommand> request = new HttpEntity<>(updateCommand, headers);

        ResponseEntity<InvoiceResponse> response = restTemplate.exchange(
                "/api/invoices/" + invoiceId,
                HttpMethod.PUT,
                request,
                InvoiceResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(invoiceId.toString());
        assertThat(response.getBody().invoiceNumber()).isEqualTo("INV-001-UPDATED");
        assertThat(response.getBody().invoiceType()).isEqualTo("USAGE");
        assertThat(response.getBody().subtotal()).isEqualTo(new BigDecimal("150.00"));
        assertThat(response.getBody().totalAmount()).isEqualTo(new BigDecimal("165.00"));
        assertThat(response.getBody().currency()).isEqualTo("USD");
        assertThat(response.getBody().notes()).isEqualTo("Updated invoice notes");
        assertThat(response.getBody().version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent invoice")
    void shouldReturn404WhenUpdatingNonExistentInvoice() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        UpdateInvoiceCommand updateCommand = new UpdateInvoiceCommand(
                nonExistentId,
                "INV-NONEXISTENT",
                UUID.randomUUID().toString(),
                InvoiceType.USAGE,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                new BigDecimal("11"),
                "PLN",
                30,
                BigDecimal.ZERO,
                "Test notes",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                0L
        );

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<UpdateInvoiceCommand> request = new HttpEntity<>(updateCommand, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/invoices/" + nonExistentId,
                HttpMethod.PUT,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return 400 when due date is before issue date")
    void shouldReturn400WhenDueDateIsBeforeIssueDate() {
        // Given - Create customer and invoice
        UUID customerId = createTestCustomer();
        UUID invoiceId = createTestInvoice(customerId);

        // When - Try to update with invalid date range
        UpdateInvoiceCommand updateCommand = new UpdateInvoiceCommand(
                invoiceId,
                "INV-001",
                customerId.toString(),
                InvoiceType.USAGE,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(20),
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                new BigDecimal("11"),
                "PLN",
                30,
                BigDecimal.ZERO,
                "Test notes",
                null,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5),
                0L
        );

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<UpdateInvoiceCommand> request = new HttpEntity<>(updateCommand, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/invoices/" + invoiceId,
                HttpMethod.PUT,
                request,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Due date must be after issue date");
    }

    @Test
    @DisplayName("Should return 409 when version conflict occurs")
    void shouldReturn409WhenVersionConflictOccurs() {
        // Given - Create customer and invoice
        UUID customerId = createTestCustomer();
        UUID invoiceId = createTestInvoice(customerId);

        // First update - this will increment version to 1
        UpdateInvoiceCommand firstUpdate = new UpdateInvoiceCommand(
                invoiceId,
                "INV-001-FIRST",
                customerId.toString(),
                InvoiceType.USAGE,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                new BigDecimal("11"),
                "PLN",
                30,
                BigDecimal.ZERO,
                "First update",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                0L
        );

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<UpdateInvoiceCommand> firstRequest = new HttpEntity<>(firstUpdate, headers);
        restTemplate.exchange(
                "/api/invoices/" + invoiceId,
                HttpMethod.PUT,
                firstRequest,
                InvoiceResponse.class
        );

        // Second update attempt with stale version (0 instead of 1)
        UpdateInvoiceCommand staleUpdate = new UpdateInvoiceCommand(
                invoiceId,
                "INV-001-STALE",
                customerId.toString(),
                InvoiceType.USAGE,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                new BigDecimal("11"),
                "PLN",
                30,
                BigDecimal.ZERO,
                "Stale update",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                0L
        );

        HttpEntity<UpdateInvoiceCommand> secondRequest = new HttpEntity<>(staleUpdate, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/invoices/" + invoiceId,
                HttpMethod.PUT,
                secondRequest,
                String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("Invoice has been modified by another process");
    }

    /**
     * Helper method to create a test customer
     */
    private UUID createTestCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+48123456789");
        customer.setPesel("12345678901");
        customer.setNip("1234567890");
        customer.setStatus(CustomerStatus.ACTIVE);

        customer = customerEntityRepository.save(customer);
        return customer.getId();
    }

    /**
     * Helper method to create a test invoice
     */
    private UUID createTestInvoice(UUID customerId) {
        CustomerEntity customer = customerEntityRepository.findById(customerId).orElseThrow();

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-001");
        invoice.setCustomer(customer);
        invoice.setInvoiceType(InvoiceType.RECURRING);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(LocalDate.now().minusDays(10));
        invoice.setDueDate(LocalDate.now().plusDays(20));
        invoice.setSubtotal(new BigDecimal("99.99"));
        invoice.setTaxAmount(new BigDecimal("0.00"));
        invoice.setTotalAmount(new BigDecimal("99.99"));
        invoice.setCurrency("PLN");
        invoice.setVersion(0L);

        invoice = invoiceRepository.save(invoice);
        return invoice.getId();
    }

    /**
     * Helper method to create authorization headers with JWT
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(validJwtToken);
        return headers;
    }

    /**
     * Create a valid JWT token for testing
     */
    private String createValidJwtToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXVzZXIiLCJlbWFpbCI6InRlc3RAdXNlci5leGFtcGxlLmNvbSIsImlhdCI6MTYxNjc3ODk5OSwiZXhwIjoxOTMyMzU0OTk5fQ.dummy-signature-for-testing-only";
    }
}
