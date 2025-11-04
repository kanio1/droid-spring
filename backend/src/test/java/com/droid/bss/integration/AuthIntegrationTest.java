package com.droid.bss.integration;

import org.springframework.context.annotation.Import;

import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.application.dto.customer.CustomerResponse;
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
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for authentication scenarios
 * Tests various authentication states with API endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.droid.bss.integration.config.IntegrationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "security.oauth2.audience=bss-backend"
})
@DisplayName("Authentication Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        customerRepository.findAll(0, Integer.MAX_VALUE).forEach(
                customer -> customerRepository.deleteById(customer.getId())
        );
    }

    @Test
    @DisplayName("Should allow request with valid JWT token")
    void shouldAllowRequestWithValidJwtToken() {
        // Given - Valid JWT token
        String validToken = createValidJwtToken();
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "12345678901", "1234567890",
                "john.doe@example.com", "+48123456789");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
                "/api/customers", request, CustomerResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().firstName()).isEqualTo("John");
        assertThat(response.getBody().lastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Should deny request without JWT token (401)")
    void shouldDenyRequestWithoutJwtToken() {
        // Given - Request without any authentication
        CreateCustomerCommand command = new CreateCustomerCommand(
                "Jane", "Doe", "12345678902", "1234567891",
                "jane.doe@example.com", "+48123456789");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/customers", request, String.class);

        // Then - Should return 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should deny request with invalid JWT token (401)")
    void shouldDenyRequestWithInvalidJwtToken() {
        // Given - Invalid JWT token
        String invalidToken = "invalid.jwt.token";
        CreateCustomerCommand command = new CreateCustomerCommand(
                "Bob", "Smith", "12345678903", "1234567892",
                "bob.smith@example.com", "+48123456789");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(invalidToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/customers", request, String.class);

        // Then - Should return 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should deny GET request without JWT token (401)")
    void shouldDenyGetRequestWithoutJwtToken() {
        // Given - Customer exists
        CustomerId customerId = createTestCustomer();

        // When - GET request without authentication
        ResponseEntity<CustomerResponse> response = restTemplate.getForEntity(
                "/api/customers/{id}", CustomerResponse.class, customerId.toString());

        // Then - Should return 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should allow PUT request with valid JWT token")
    void shouldAllowPutRequestWithValidJwtToken() {
        // Given - Customer exists
        CustomerId customerId = createTestCustomer();
        String validToken = createValidJwtToken();

        // And - Update command
        com.droid.bss.application.dto.customer.UpdateCustomerCommand command =
                new com.droid.bss.application.dto.customer.UpdateCustomerCommand(
                        customerId.toString(), "Jane", "Updated",
                        "12345678901", "1234567890",
                        "jane.updated@example.com", "+48123456789");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<com.droid.bss.application.dto.customer.UpdateCustomerCommand> request =
                new HttpEntity<>(command, headers);

        // When
        ResponseEntity<CustomerResponse> response = restTemplate.exchange(
                "/api/customers/{id}", HttpMethod.PUT, request, CustomerResponse.class,
                customerId.toString());

        // Then - Should succeed
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().firstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("Should deny DELETE request without JWT token (401)")
    void shouldDenyDeleteRequestWithoutJwtToken() {
        // Given - Customer exists
        CustomerId customerId = createTestCustomer();

        // When - DELETE request without authentication
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/customers/{id}", HttpMethod.DELETE, null, String.class,
                customerId.toString());

        // Then - Should return 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should allow DELETE request with valid JWT token (204)")
    void shouldAllowDeleteRequestWithValidJwtToken() {
        // Given - Customer exists
        CustomerId customerId = createTestCustomer();
        String validToken = createValidJwtToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(validToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/customers/{id}", HttpMethod.DELETE, request, Void.class,
                customerId.toString());

        // Then - Should return 204 No Content
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Should deny request with malformed JWT token (401)")
    void shouldDenyRequestWithMalformedJwtToken() {
        // Given - Malformed JWT token
        String malformedToken = "not.a.valid.jwt.token.format";
        CreateCustomerCommand command = new CreateCustomerCommand(
                "Alice", "Brown", "12345678904", "1234567893",
                "alice.brown@example.com", "+48123456789");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(malformedToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/customers", request, String.class);

        // Then - Should return 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should deny request with empty Bearer token (401)")
    void shouldDenyRequestWithEmptyBearerToken() {
        // Given - Empty Bearer token
        String emptyToken = "";
        CreateCustomerCommand command = new CreateCustomerCommand(
                "Charlie", "Davis", "12345678905", "1234567894",
                "charlie.davis@example.com", "+48123456789");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(emptyToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateCustomerCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/customers", request, String.class);

        // Then - Should return 401 Unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should allow multiple requests with valid token")
    void shouldAllowMultipleRequestsWithValidToken() {
        // Given - Valid JWT token
        String validToken = createValidJwtToken();

        // When - Make multiple requests
        // Request 1: Create customer
        CreateCustomerCommand command1 = new CreateCustomerCommand(
                "User1", "Test1", "12345678901", "1234567890",
                "user1@test.com", "+48123456789");
        ResponseEntity<CustomerResponse> response1 = makeAuthenticatedRequest(
                "/api/customers", command1, validToken, CustomerResponse.class);

        // Request 2: Create another customer
        CreateCustomerCommand command2 = new CreateCustomerCommand(
                "User2", "Test2", "12345678902", "1234567891",
                "user2@test.com", "+48123456789");
        ResponseEntity<CustomerResponse> response2 = makeAuthenticatedRequest(
                "/api/customers", command2, validToken, CustomerResponse.class);

        // Then - Both requests should succeed
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response1.getBody()).isNotNull();
        assertThat(response2.getBody()).isNotNull();
        assertThat(response1.getBody().email()).isEqualTo("user1@test.com");
        assertThat(response2.getBody().email()).isEqualTo("user2@test.com");
    }

    private <T, R> ResponseEntity<R> makeAuthenticatedRequest(String url, T body, String token, Class<R> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, responseType);
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
