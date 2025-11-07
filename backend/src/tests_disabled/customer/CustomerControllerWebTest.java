package com.droid.bss.api.customer;

import com.droid.bss.application.command.customer.ChangeCustomerStatusUseCase;
import com.droid.bss.application.command.customer.CreateCustomerUseCase;
import com.droid.bss.application.command.customer.DeleteCustomerUseCase;
import com.droid.bss.application.command.customer.UpdateCustomerUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.customer.ChangeCustomerStatusCommand;
import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.application.dto.customer.UpdateCustomerCommand;
import com.droid.bss.application.query.customer.CustomerQueryService;
import com.droid.bss.domain.customer.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = CustomerController.class,
    excludeAutoConfiguration = {
        com.droid.bss.infrastructure.security.WebMvcConfig.class
    }
)
@Import(CustomerControllerWebTest.TestSecurityConfiguration.class)
@TestPropertySource(properties = "security.oauth2.audience=bss-backend")
@DisplayName("CustomerController Web layer")
@Disabled("Temporarily disabled - requires full infrastructure")

class CustomerControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockBean
    private UpdateCustomerUseCase updateCustomerUseCase;

    @MockBean
    private ChangeCustomerStatusUseCase changeCustomerStatusUseCase;

    @MockBean
    private DeleteCustomerUseCase deleteCustomerUseCase;

    @MockBean
    private CustomerQueryService customerQueryService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private com.droid.bss.infrastructure.resilience.RateLimitingService rateLimitingService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Configure rate limiting mock to allow all requests
        when(rateLimitingService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRateLimitKey(anyString())).thenReturn("test:key");
    }

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
            return new JwtAuthenticationConverter();
        }

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
            http.authorizeHttpRequests(registry -> registry
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
            return http.build();
        }
    }

    @Test
    @DisplayName("should create customer successfully")
    void shouldCreateCustomerSuccessfully() throws Exception {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "12345678901", "1234567890", 
                "john.doe@example.com", "+48123456789");
        
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        when(createCustomerUseCase.handle(any(CreateCustomerCommand.class))).thenReturn(customerId);

        CustomerResponse response = createCustomerResponse(customerId);
        when(customerQueryService.findById(customerId.toString())).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(post("/api/customers")
                .with(jwt().jwt(builder -> builder.subject("test-user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("should return 400 when creating customer with invalid data")
    void shouldReturn400WhenCreatingCustomerWithInvalidData() throws Exception {
        // Given
        CreateCustomerCommand invalidCommand = new CreateCustomerCommand(
                "", "Doe", "123", "123", 
                "invalid-email", "123");

        // When & Then
        mockMvc.perform(post("/api/customers")
                .with(jwt().jwt(builder -> builder.subject("test-user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCommand)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should get customer by ID")
    void shouldGetCustomerById() throws Exception {
        // Given
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        CustomerResponse response = createCustomerResponse(customerId);
        when(customerQueryService.findById(customerId.toString())).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(get("/api/customers/{id}", customerId.toString())
                .with(jwt().jwt(builder -> builder.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        String nonExistentId = UUID.randomUUID().toString();
        when(customerQueryService.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/customers/{id}", nonExistentId)
                .with(jwt().jwt(builder -> builder.subject("test-user"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should get all customers with pagination")
    void shouldGetAllCustomersWithPagination() throws Exception {
        // Given
        CustomerId customerId1 = new CustomerId(UUID.randomUUID());
        CustomerId customerId2 = new CustomerId(UUID.randomUUID());
        
        PageResponse<CustomerResponse> pageResponse = new PageResponse<>(
                List.of(createCustomerResponse(customerId1), createCustomerResponse(customerId2)),
                0, 20, 2L, 1, true, false, false
        );
        
        when(customerQueryService.findAll(0, 20, "createdAt,desc")).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/customers")
                .with(jwt().jwt(builder -> builder.subject("test-user")))
                .param("page", "0")
                .param("size", "20")
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("should update customer successfully")
    void shouldUpdateCustomerSuccessfully() throws Exception {
        // Given
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId.toString(), "Jane", "Smith", "12345678901", 
                "1234567890", "jane.smith@example.com", "+48123456789");
        
        Customer updatedCustomer = createTestCustomer(customerId);
        when(updateCustomerUseCase.handle(any(UpdateCustomerCommand.class))).thenReturn(updatedCustomer);

        CustomerResponse response = createCustomerResponse(customerId);
        when(customerQueryService.findById(customerId.toString())).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(put("/api/customers/{id}", customerId.toString())
                .with(jwt().jwt(builder -> builder.subject("test-user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("should change customer status")
    void shouldChangeCustomerStatus() throws Exception {
        // Given
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(
                customerId.toString(), "SUSPENDED");
        
        Customer suspendedCustomer = createTestCustomer(customerId).suspend();
        when(changeCustomerStatusUseCase.handle(any(ChangeCustomerStatusCommand.class))).thenReturn(suspendedCustomer);

        CustomerResponse response = createCustomerResponse(customerId);
        when(customerQueryService.findById(customerId.toString())).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(put("/api/customers/{id}/status", customerId.toString())
                .with(jwt().jwt(builder -> builder.subject("test-user")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("should delete customer successfully")
    void shouldDeleteCustomerSuccessfully() throws Exception {
        // Given
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        when(deleteCustomerUseCase.handle(customerId.toString())).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/customers/{id}", customerId.toString())
                .with(jwt().jwt(builder -> builder.subject("test-user"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 401 when unauthorized")
    void shouldReturn401WhenUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    private CustomerResponse createCustomerResponse(CustomerId customerId) {
        return new CustomerResponse(
                customerId.value(),
                "John", "Doe", "12345678901", "1234567890",
                "john.doe@example.com", "+48123456789",
                "ACTIVE", "Aktywny",
                LocalDateTime.now(), LocalDateTime.now(), 1
        );
    }

    private Customer createTestCustomer(CustomerId customerId) {
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        return Customer.testCustomer(customerId, personalInfo, contactInfo, CustomerStatus.ACTIVE, 
                                    LocalDateTime.now(), 1);
    }
}
