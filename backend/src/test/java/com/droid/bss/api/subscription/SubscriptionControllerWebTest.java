package com.droid.bss.api.subscription;

import com.droid.bss.application.command.subscription.ChangeSubscriptionStatusUseCase;
import com.droid.bss.application.command.subscription.CreateSubscriptionUseCase;
import com.droid.bss.application.command.subscription.DeleteSubscriptionUseCase;
import com.droid.bss.application.command.subscription.UpdateSubscriptionUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.subscription.ChangeSubscriptionStatusCommand;
import com.droid.bss.application.dto.subscription.CreateSubscriptionCommand;
import com.droid.bss.application.dto.subscription.SubscriptionResponse;
import com.droid.bss.application.dto.subscription.UpdateSubscriptionCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.OrderType;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductCategory;
import com.droid.bss.domain.product.ProductStatus;
import com.droid.bss.domain.product.ProductType;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = SubscriptionController.class,
    excludeAutoConfiguration = {
        com.droid.bss.infrastructure.security.WebMvcConfig.class
    }
)
@Import(SubscriptionControllerWebTest.TestSecurityConfiguration.class)
@TestPropertySource(properties = "security.oauth2.audience=bss-backend")
@DisplayName("SubscriptionController Web layer")
class SubscriptionControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateSubscriptionUseCase createSubscriptionUseCase;

    @MockBean
    private UpdateSubscriptionUseCase updateSubscriptionUseCase;

    @MockBean
    private ChangeSubscriptionStatusUseCase changeSubscriptionStatusUseCase;

    @MockBean
    private DeleteSubscriptionUseCase deleteSubscriptionUseCase;

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    private ObjectMapper objectMapper;
    private SubscriptionEntity testSubscription;
    private CustomerEntity testCustomer;
    private ProductEntity testProduct;
    private OrderEntity testOrder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create test customer
        testCustomer = new CustomerEntity();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setFirstName("Anna");
        testCustomer.setLastName("Nowak");
        testCustomer.setEmail("anna.nowak@example.com");
        testCustomer.setStatus(CustomerStatus.ACTIVE);

        // Create test product
        testProduct = new ProductEntity();
        testProduct.setId(UUID.randomUUID());
        testProduct.setProductCode("PROD-001");
        testProduct.setName("Test Product");
        testProduct.setDescription("Test product description");
        testProduct.setProductType(ProductType.SERVICE);
        testProduct.setCategory(ProductCategory.MOBILE);
        testProduct.setStatus(ProductStatus.ACTIVE);

        // Create test order
        testOrder = new OrderEntity();
        testOrder.setId(UUID.randomUUID());
        testOrder.setOrderNumber("ORD-001");
        testOrder.setCustomer(testCustomer);
        testOrder.setOrderType(OrderType.NEW_SUBSCRIPTION);
        testOrder.setStatus(OrderStatus.COMPLETED);

        // Create test subscription
        testSubscription = new SubscriptionEntity();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setSubscriptionNumber("SUB-20251030-12345678");
        testSubscription.setCustomer(testCustomer);
        testSubscription.setProduct(testProduct);
        testSubscription.setOrder(testOrder);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setStartDate(LocalDate.now());
        testSubscription.setEndDate(LocalDate.now().plusMonths(12));
        testSubscription.setBillingStart(LocalDate.now());
        testSubscription.setNextBillingDate(LocalDate.now().plusMonths(1));
        testSubscription.setBillingPeriod("MONTHLY");
        testSubscription.setPrice(new BigDecimal("99.99"));
        testSubscription.setCurrency("PLN");
        testSubscription.setAutoRenew(true);
        testSubscription.setCreatedAt(LocalDateTime.now());
        testSubscription.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("should create subscription successfully")
    @Disabled("Test scaffolding - implementation required")
    void shouldCreateSubscription() throws Exception {
        // Given
        CreateSubscriptionCommand command = new CreateSubscriptionCommand(
                testCustomer.getId().toString(),
                testProduct.getId().toString(),
                testOrder.getId().toString(),
                LocalDate.now(),
                LocalDate.now().plusMonths(12),
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                "MONTHLY",
                new BigDecimal("99.99"),
                "PLN",
                true
        );

        when(createSubscriptionUseCase.handle(command))
                .thenReturn(testSubscription.getId());

        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));

        // When & Then
        mockMvc.perform(post("/api/subscriptions")
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(testSubscription.getId().toString()))
                .andExpect(jsonPath("$.subscriptionNumber").value(testSubscription.getSubscriptionNumber()));

        verify(createSubscriptionUseCase).handle(command);
    }

    @Test
    @DisplayName("should get subscription by ID")
    @Disabled("Test scaffolding - implementation required")
    void shouldGetSubscriptionById() throws Exception {
        // Given
        when(subscriptionRepository.findById(testSubscription.getId()))
                .thenReturn(Optional.of(testSubscription));

        // When & Then
        mockMvc.perform(get("/api/subscriptions/{id}", testSubscription.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "read");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testSubscription.getId().toString()))
                .andExpect(jsonPath("$.subscriptionNumber").value(testSubscription.getSubscriptionNumber()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.autoRenew").value(true));

        verify(subscriptionRepository).findById(testSubscription.getId());
    }

    @Test
    @DisplayName("should return 404 when subscription not found")
    @Disabled("Test scaffolding - implementation required")
    void shouldReturn404WhenSubscriptionNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(subscriptionRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/subscriptions/{id}", nonExistentId)
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "read");
                        })))
                .andExpect(status().isNotFound());

        verify(subscriptionRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("should get all subscriptions with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldGetAllSubscriptions() throws Exception {
        // Given
        List<SubscriptionEntity> subscriptions = List.of(testSubscription);
        org.springframework.data.domain.Page<SubscriptionEntity> subscriptionPage =
                new org.springframework.data.domain.PageImpl<>(subscriptions);

        when(subscriptionRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(subscriptionPage);

        // When & Then
        mockMvc.perform(get("/api/subscriptions")
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

        verify(subscriptionRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("should update subscription")
    @Disabled("Test scaffolding - implementation required")
    void shouldUpdateSubscription() throws Exception {
        // Given
        UpdateSubscriptionCommand command = new UpdateSubscriptionCommand(
                testSubscription.getId().toString(),
                LocalDate.now().plusMonths(24),
                LocalDate.now().plusMonths(2),
                new BigDecimal("129.99"),
                "EUR",
                "MONTHLY",
                true
        );

        SubscriptionEntity updatedSubscription = new SubscriptionEntity();
        updatedSubscription.setId(testSubscription.getId());
        updatedSubscription.setSubscriptionNumber(testSubscription.getSubscriptionNumber());
        updatedSubscription.setPrice(new BigDecimal("129.99"));
        updatedSubscription.setCurrency("EUR");

        when(updateSubscriptionUseCase.handle(command))
                .thenReturn(updatedSubscription);

        // When & Then
        mockMvc.perform(put("/api/subscriptions/{id}", testSubscription.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(129.99))
                .andExpect(jsonPath("$.currency").value("EUR"));

        verify(updateSubscriptionUseCase).handle(command);
    }

    @Test
    @DisplayName("should change subscription status")
    @Disabled("Test scaffolding - implementation required")
    void shouldChangeSubscriptionStatus() throws Exception {
        // Given
        ChangeSubscriptionStatusCommand command = new ChangeSubscriptionStatusCommand(
                testSubscription.getId().toString(),
                SubscriptionStatus.SUSPENDED,
                "Payment failed"
        );

        SubscriptionEntity updatedSubscription = new SubscriptionEntity();
        updatedSubscription.setId(testSubscription.getId());
        updatedSubscription.setStatus(SubscriptionStatus.SUSPENDED);

        when(changeSubscriptionStatusUseCase.handle(command))
                .thenReturn(updatedSubscription);

        // When & Then
        mockMvc.perform(put("/api/subscriptions/{id}/status", testSubscription.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        }))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(changeSubscriptionStatusUseCase).handle(command);
    }

    @Test
    @DisplayName("should delete subscription")
    @Disabled("Test scaffolding - implementation required")
    void shouldDeleteSubscription() throws Exception {
        // Given
        when(deleteSubscriptionUseCase.handle(testSubscription.getId().toString()))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/subscriptions/{id}", testSubscription.getId())
                        .with(jwt().jwt(jwt -> {
                            jwt.subject("test-user");
                            jwt.claim("scope", "write");
                        })))
                .andExpect(status().isNoContent());

        verify(deleteSubscriptionUseCase).handle(testSubscription.getId().toString());
    }

    @Test
    @DisplayName("should reject unauthorized request")
    @Disabled("Test scaffolding - implementation required")
    void shouldRejectUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/subscriptions"))
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
