package com.droid.bss.integration;

import com.droid.bss.application.dto.customer.*;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.domain.customer.*;
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
import org.springframework.web.client.RestClientException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Customer CRUD operations with authentication
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.droid.bss.integration.config.IntegrationTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Customer CRUD Integration Tests")
class CustomerCrudIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String validJwtToken = createValidJwtToken();

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        customerRepository.findAll(0, Integer.MAX_VALUE).forEach(
                customer -> customerRepository.deleteById(customer.getId())
        );
    }

    @Test
    @DisplayName("Should create customer with valid JWT and data")
    void shouldCreateCustomerWithValidData() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "12345678901", "1234567890",
                "john.doe@example.com", "+48123456789");

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
                "/api/customers", request, CustomerResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().firstName()).isEqualTo("John");
        assertThat(response.getBody().lastName()).isEqualTo("Doe");
        assertThat(response.getBody().email()).isEqualTo("john.doe@example.com");
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should return 400 when creating customer with invalid data")
    void shouldReturn400WhenCreatingCustomerWithInvalidData() {
        // Given
        CreateCustomerCommand invalidCommand = new CreateCustomerCommand(
                "", "Doe", "123", "123",
                "invalid-email", "123");

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(invalidCommand, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/customers", request, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should get existing customer")
    void shouldGetExistingCustomer() {
        // Given - Create a customer first
        CustomerId customerId = createTestCustomer();

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.getForEntity(
                "/api/customers/{id}", CustomerResponse.class, customerId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(customerId.toString());
        assertThat(response.getBody().firstName()).isEqualTo("John");
        assertThat(response.getBody().lastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should return 404 for non-existent customer")
    void shouldReturn404ForNonExistentCustomer() {
        // Given
        String nonExistentId = UUID.randomUUID().toString();

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.getForEntity(
                "/api/customers/{id}", CustomerResponse.class, nonExistentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should update customer")
    void shouldUpdateCustomer() {
        // Given - Create a customer first
        CustomerId customerId = createTestCustomer();

        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId.toString(), "Jane", "Smith", "12345678901",
                "1234567890", "jane.smith@example.com", "+48123456789");

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<UpdateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.exchange(
                "/api/customers/{id}", HttpMethod.PUT, request, CustomerResponse.class,
                customerId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().firstName()).isEqualTo("Jane");
        assertThat(response.getBody().lastName()).isEqualTo("Smith");
        assertThat(response.getBody().email()).isEqualTo("jane.smith@example.com");
    }

    @Test
    @DisplayName("Should change customer status")
    void shouldChangeCustomerStatus() {
        // Given - Create a customer first
        CustomerId customerId = createTestCustomer();

        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(
                customerId.toString(), "SUSPENDED");

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<ChangeCustomerStatusCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.exchange(
                "/api/customers/{id}/status", HttpMethod.PUT, request, CustomerResponse.class,
                customerId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("Should delete customer")
    void shouldDeleteCustomer() {
        // Given - Create a customer first
        CustomerId customerId = createTestCustomer();

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/customers/{id}", HttpMethod.DELETE, request, Void.class,
                customerId.toString());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify customer is deleted
        ResponseEntity<CustomerResponse> getResponse = restTemplate.getForEntity(
                "/api/customers/{id}", CustomerResponse.class, customerId.toString());
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should search customers with pagination")
    void shouldSearchCustomersWithPagination() {
        // Given - Create multiple customers
        createTestCustomer("John", "Doe", "john.doe@example.com");
        createTestCustomer("Jane", "Smith", "jane.smith@example.com");
        createTestCustomer("Bob", "Johnson", "bob.johnson@example.com");

        // When
        ResponseEntity<PageResponse<CustomerResponse>> response = restTemplate.getForEntity(
                "/api/customers?page=0&size=10&sort=createdAt,desc",
                (Class<PageResponse<CustomerResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(3);
        assertThat(response.getBody().page()).isEqualTo(0);
        assertThat(response.getBody().size()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should get customers by status")
    void shouldGetCustomersByStatus() {
        // Given - Create customers with different statuses
        createTestCustomer("John", "Doe", "john.doe@example.com"); // ACTIVE
        CustomerId suspendedId = createTestCustomer("Jane", "Smith", "jane.smith@example.com");

        // Change one to SUSPENDED
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(
                suspendedId.toString(), "SUSPENDED");
        HttpHeaders headers = createAuthHeaders();
        restTemplate.exchange(
                "/api/customers/{id}/status", HttpMethod.PUT,
                new HttpEntity<>(command, headers), CustomerResponse.class, suspendedId.toString());

        // When - Get active customers
        ResponseEntity<PageResponse<CustomerResponse>> activeResponse = restTemplate.getForEntity(
                "/api/customers/by-status/ACTIVE?page=0&size=10",
                (Class<PageResponse<CustomerResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(activeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activeResponse.getBody()).isNotNull();
        assertThat(activeResponse.getBody().content()).hasSize(1);
        assertThat(activeResponse.getBody().content().get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should search customers by search term")
    void shouldSearchCustomersBySearchTerm() {
        // Given - Create customers with different names
        createTestCustomer("John", "Doe", "john.doe@example.com");
        createTestCustomer("Jane", "Smith", "jane.smith@example.com");

        // When - Search for "John"
        ResponseEntity<PageResponse<CustomerResponse>> response = restTemplate.getForEntity(
                "/api/customers/search?searchTerm=John&page=0&size=10",
                (Class<PageResponse<CustomerResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).firstName()).isEqualTo("John");
    }

    private CustomerId createTestCustomer() {
        return createTestCustomer("John", "Doe", "john.doe@example.com");
    }

    private CustomerId createTestCustomer(String firstName, String lastName, String email) {
        CustomerInfo personalInfo = new CustomerInfo(firstName, lastName, "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo(email, "+48123456789");
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        Customer customer = Customer.testCustomer(
                customerId, personalInfo, contactInfo, CustomerStatus.ACTIVE,
                LocalDateTime.now(), 1);

        return customerRepository.save(customer).getId();
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validJwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createValidJwtToken() {
        // In a real integration test, you would use Keycloak test container or wiremock
        // For now, return a mock token that will pass JWT parsing
        // The security configuration expects a valid JWT structure
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
               "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJlbWFpbCI6InRlc3RAdXNlci5jb20iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0" +
               "LXVzZXIiLCJyb2xlIjoiVVNFUiIsImF1ZCI6ImJzc2JhY2tlbmQiLCJpc19hbm9ueW1vdXMiOnRydWUsImV4cCI6MTY0" +
               "NTk1OTIyMiwiYWRtaW4iOmZhbHNlfQ." +
               "dummy_signature_for_testing_only";
    }
}
