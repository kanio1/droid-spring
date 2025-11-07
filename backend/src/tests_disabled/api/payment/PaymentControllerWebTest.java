package com.droid.bss.api.payment;

import com.droid.bss.application.command.payment.ChangePaymentStatusUseCase;
import com.droid.bss.application.command.payment.CreatePaymentUseCase;
import com.droid.bss.application.command.payment.DeletePaymentUseCase;
import com.droid.bss.application.command.payment.UpdatePaymentUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.payment.ChangePaymentStatusCommand;
import com.droid.bss.application.dto.payment.CreatePaymentCommand;
import com.droid.bss.application.dto.payment.PaymentResponse;
import com.droid.bss.application.dto.payment.UpdatePaymentCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.InvoiceType;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.payment.PaymentStatus;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = PaymentController.class,
    excludeAutoConfiguration = {
        com.droid.bss.infrastructure.security.WebMvcConfig.class
    }
)
@Import(PaymentControllerWebTest.TestSecurityConfiguration.class)
@TestPropertySource(properties = "security.oauth2.audience=bss-backend")
@DisplayName("PaymentController Web layer")
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreatePaymentUseCase createPaymentUseCase;

    @MockBean
    private UpdatePaymentUseCase updatePaymentUseCase;

    @MockBean
    private ChangePaymentStatusUseCase changePaymentStatusUseCase;

    @MockBean
    private DeletePaymentUseCase deletePaymentUseCase;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private com.droid.bss.infrastructure.resilience.RateLimitingService rateLimitingService;

    private ObjectMapper objectMapper;
    private PaymentEntity testPayment;
    private CustomerEntity testCustomer;
    private InvoiceEntity testInvoice;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create test customer
        testCustomer = new CustomerEntity();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setFirstName("Jan");
        testCustomer.setLastName("Kowalski");
        testCustomer.setEmail("jan.kowalski@example.com");
        testCustomer.setStatus(CustomerStatus.ACTIVE);

        // Create test invoice
        testInvoice = new InvoiceEntity();
        testInvoice.setId(UUID.randomUUID());
        testInvoice.setInvoiceNumber("INV-001");
        testInvoice.setCustomer(testCustomer);
        testInvoice.setInvoiceType(InvoiceType.ONE_TIME);
        testInvoice.setStatus(InvoiceStatus.SENT);

        // Create test payment
        testPayment = new PaymentEntity();
        testPayment.setId(UUID.randomUUID());
        testPayment.setPaymentNumber("PAY-20251030-12345678");
        testPayment.setCustomer(testCustomer);
        testPayment.setInvoice(testInvoice);
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setCurrency("PLN");
        testPayment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        testPayment.setPaymentStatus(PaymentStatus.PENDING);
        testPayment.setPaymentDate(LocalDate.now());
        testPayment.setReferenceNumber("REF-001");
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        // Configure rate limiting mock
        when(rateLimitingService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRateLimitKey(anyString())).thenReturn("test:key");
    }

    @Test
    @DisplayName("should create payment successfully")
    @Disabled("Test scaffolding - implementation required")
    void shouldCreatePayment() throws Exception {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand(
                testCustomer.getId().toString(),
                testInvoice.getId().toString(),
                new BigDecimal("99.99"),
                "PLN",
                PaymentMethod.BANK_TRANSFER,
                LocalDate.now(),
                "REF-001",
                "Test payment"
        );

        when(createPaymentUseCase.handle(command))
                .thenReturn(testPayment.getId());

        when(paymentRepository.findById(testPayment.getId()))
                .thenReturn(Optional.of(testPayment));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(testPayment.getId().toString()));

        verify(createPaymentUseCase).handle(command);
    }

    @Test
    @DisplayName("should get payment by ID")
    @Disabled("Test scaffolding - implementation required")
    void shouldGetPaymentById() throws Exception {
        // Given
        when(paymentRepository.findById(testPayment.getId()))
                .thenReturn(Optional.of(testPayment));

        // When & Then
        mockMvc.perform(get("/api/payments/{id}", testPayment.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "read");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPayment.getId().toString()))
                .andExpect(jsonPath("$.paymentNumber").value(testPayment.getPaymentNumber()))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"));

        verify(paymentRepository).findById(testPayment.getId());
    }

    @Test
    @DisplayName("should return 404 when payment not found")
    @Disabled("Test scaffolding - implementation required")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(paymentRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/payments/{id}", nonExistentId)
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "read");
                        })))
                .andExpect(status().isNotFound());

        verify(paymentRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("should get all payments with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldGetAllPayments() throws Exception {
        // Given
        List<PaymentEntity> payments = List.of(testPayment);
        org.springframework.data.domain.Page<PaymentEntity> paymentPage =
                new org.springframework.data.domain.PageImpl<>(payments);

        when(paymentRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(paymentPage);

        // When & Then
        mockMvc.perform(get("/api/payments")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "read");
                        }))
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(paymentRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("should update payment")
    @Disabled("Test scaffolding - implementation required")
    void shouldUpdatePayment() throws Exception {
        // Given
        UpdatePaymentCommand command = new UpdatePaymentCommand(
                testPayment.getId().toString(),
                new BigDecimal("199.99"),
                "EUR",
                PaymentMethod.CARD,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "REF-002",
                "Updated payment"
        );

        PaymentEntity updatedPayment = new PaymentEntity();
        updatedPayment.setId(testPayment.getId());
        updatedPayment.setPaymentNumber(testPayment.getPaymentNumber());
        updatedPayment.setAmount(new BigDecimal("199.99"));
        updatedPayment.setCurrency("EUR");
        updatedPayment.setPaymentMethod(PaymentMethod.CARD);

        when(updatePaymentUseCase.handle(command))
                .thenReturn(updatedPayment);

        // When & Then
        mockMvc.perform(put("/api/payments/{id}", testPayment.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(199.99))
                .andExpect(jsonPath("$.currency").value("EUR"));

        verify(updatePaymentUseCase).handle(command);
    }

    @Test
    @DisplayName("should change payment status")
    @Disabled("Test scaffolding - implementation required")
    void shouldChangePaymentStatus() throws Exception {
        // Given
        ChangePaymentStatusCommand command = new ChangePaymentStatusCommand(
                testPayment.getId().toString(),
                PaymentStatus.COMPLETED,
                "Payment completed successfully"
        );

        PaymentEntity updatedPayment = new PaymentEntity();
        updatedPayment.setId(testPayment.getId());
        updatedPayment.setPaymentStatus(PaymentStatus.COMPLETED);

        when(changePaymentStatusUseCase.handle(command))
                .thenReturn(updatedPayment);

        // When & Then
        mockMvc.perform(put("/api/payments/{id}/status", testPayment.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));

        verify(changePaymentStatusUseCase).handle(command);
    }

    @Test
    @DisplayName("should delete payment")
    @Disabled("Test scaffolding - implementation required")
    void shouldDeletePayment() throws Exception {
        // Given
        when(deletePaymentUseCase.handle(testPayment.getId().toString()))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/payments/{id}", testPayment.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        })))
                .andExpect(status().isNoContent());

        verify(deletePaymentUseCase).handle(testPayment.getId().toString());
    }

    @Test
    @DisplayName("should reject unauthorized request")
    @Disabled("Test scaffolding - implementation required")
    void shouldRejectUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
            );
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
            return http.build();
        }
    }
}
