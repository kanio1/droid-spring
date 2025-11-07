package com.droid.bss.integration;

import org.springframework.context.annotation.Import;

import com.droid.bss.application.dto.product.*;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.domain.product.*;
import com.droid.bss.domain.product.repository.ProductRepository;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Product CRUD operations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.droid.bss.integration.config.IntegrationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "security.oauth2.audience=bss-backend"
})
@DisplayName("Product CRUD Integration Tests")
class ProductCrudIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String validJwtToken = createValidJwtToken();

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create product with valid data")
    void shouldCreateProduct() {
        // Given
        CreateProductCommand command = new CreateProductCommand(
                "MOBILE-001", "Mobile Service Premium", "Premium mobile service",
                ProductType.SERVICE, ProductCategory.MOBILE, BigDecimal.valueOf(29.99),
                "PLN", "monthly", ProductStatus.ACTIVE,
                LocalDate.now(), LocalDate.now().plusMonths(12));

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<CreateProductCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<ProductResponse> response = restTemplate.postForEntity(
                "/api/products", request, ProductResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().productCode()).isEqualTo("MOBILE-001");
        assertThat(response.getBody().name()).isEqualTo("Mobile Service Premium");
        assertThat(response.getBody().price()).isEqualTo(BigDecimal.valueOf(29.99));
        assertThat(response.getBody().status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should get product by ID")
    void shouldGetProductById() {
        // Given - Create a product first
        UUID productId = createTestProduct();

        // When
        ResponseEntity<ProductResponse> response = restTemplate.getForEntity(
                "/api/products/{id}", ProductResponse.class, productId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(productId);
        assertThat(response.getBody().productCode()).isEqualTo("MOBILE-001");
        assertThat(response.getBody().name()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should return 404 for non-existent product")
    void shouldReturn404ForNonExistentProduct() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        ResponseEntity<ProductResponse> response = restTemplate.getForEntity(
                "/api/products/{id}", ProductResponse.class, nonExistentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should get all products with pagination")
    void shouldGetAllProductsWithPagination() {
        // Given - Create multiple products
        createTestProduct("MOBILE-001");
        createTestProduct("MOBILE-002");
        createTestProduct("BROADBAND-001");

        // When
        ResponseEntity<PageResponse<ProductResponse>> response = restTemplate.getForEntity(
                "/api/products?page=0&size=10&sort=createdAt,desc",
                (Class<PageResponse<ProductResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(3);
        assertThat(response.getBody().page()).isEqualTo(0);
        assertThat(response.getBody().size()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should get products by status")
    void shouldGetProductsByStatus() {
        // Given - Create products with different statuses
        createTestProduct("MOBILE-001", ProductStatus.ACTIVE); // ACTIVE
        createTestProduct("MOBILE-002", ProductStatus.INACTIVE); // INACTIVE

        // When - Get active products
        ResponseEntity<PageResponse<ProductResponse>> response = restTemplate.getForEntity(
                "/api/products/by-status/ACTIVE?page=0&size=10",
                (Class<PageResponse<ProductResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should search products")
    void shouldSearchProducts() {
        // Given - Create products with different names
        createTestProduct("MOBILE-001", "Mobile Service Premium");
        createTestProduct("BROADBAND-001", "Broadband Internet");

        // When - Search for "Mobile"
        ResponseEntity<PageResponse<ProductResponse>> response = restTemplate.getForEntity(
                "/api/products/search?searchTerm=Mobile&page=0&size=10",
                (Class<PageResponse<ProductResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).name()).contains("Mobile");
    }

    @Test
    @DisplayName("Should get active products")
    void shouldGetActiveProducts() {
        // Given - Create active products within validity period
        createTestProduct("MOBILE-001",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                LocalDate.now().plusMonths(12));

        // When
        ResponseEntity<PageResponse<ProductResponse>> response = restTemplate.getForEntity(
                "/api/products/active?page=0&size=10",
                (Class<PageResponse<ProductResponse>>) (Object) PageResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should update product")
    void shouldUpdateProduct() {
        // Given - Create a product first
        UUID productId = createTestProduct();

        UpdateProductCommand command = new UpdateProductCommand(
                productId,
                "MOBILE-001-UPDATED",
                "Updated Product Name",
                "Updated description",
                ProductType.SERVICE,
                ProductCategory.MOBILE,
                BigDecimal.valueOf(39.99),
                "PLN",
                "monthly",
                ProductStatus.ACTIVE,
                LocalDate.now(),
                LocalDate.now().plusMonths(12),
                1L // version for optimistic locking
        );

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<UpdateProductCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                "/api/products/{id}", HttpMethod.PUT, request, ProductResponse.class,
                productId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Updated Product Name");
        assertThat(response.getBody().price()).isEqualTo(BigDecimal.valueOf(39.99));
    }

    @Test
    @DisplayName("Should change product status")
    void shouldChangeProductStatus() {
        // Given - Create a product first
        UUID productId = createTestProduct("MOBILE-001", ProductStatus.ACTIVE);

        ChangeProductStatusCommand command = new ChangeProductStatusCommand(
                productId,
                ProductStatus.INACTIVE,
                1L // version for optimistic locking
        );

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<ChangeProductStatusCommand> request = new HttpEntity<>(command, headers);

        // When
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                "/api/products/{id}/status", HttpMethod.PUT, request, ProductResponse.class,
                productId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() {
        // Given - Create a product first
        UUID productId = createTestProduct("MOBILE-001", ProductStatus.ACTIVE);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/products/{id}?version={version}",
                HttpMethod.DELETE, request, Void.class, productId, 1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify product is deleted
        ResponseEntity<ProductResponse> getResponse = restTemplate.getForEntity(
                "/api/products/{id}", ProductResponse.class, productId);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UUID createTestProduct() {
        return createTestProduct("MOBILE-001", "Test Product");
    }

    private UUID createTestProduct(String productCode) {
        return createTestProduct(productCode, "Test Product");
    }

    private UUID createTestProduct(String productCode, String name) {
        return createTestProduct(productCode, name, ProductStatus.ACTIVE);
    }

    private UUID createTestProduct(String productCode, ProductStatus status) {
        return createTestProduct(productCode, "Test Product", status);
    }

    private UUID createTestProduct(String productCode, String name, ProductStatus status) {
        return createTestProduct(productCode, name, status, LocalDate.now(), LocalDate.now().plusMonths(12));
    }

    private UUID createTestProduct(String productCode, ProductStatus status,
                                    LocalDate validityStart, LocalDate validityEnd) {
        return createTestProduct(productCode, "Test Product", status, validityStart, validityEnd);
    }

    private UUID createTestProduct(String productCode, String name, ProductStatus status,
                                    LocalDate validityStart, LocalDate validityEnd) {
        ProductEntity product = new ProductEntity();
        product.setId(UUID.randomUUID());
        product.setProductCode(productCode);
        product.setName(name);
        product.setDescription("Test description");
        product.setProductType(ProductType.SERVICE);
        product.setCategory(ProductCategory.MOBILE);
        product.setPrice(BigDecimal.valueOf(29.99));
        product.setCurrency("PLN");
        product.setBillingPeriod("monthly");
        product.setStatus(status);
        product.setValidityStart(validityStart);
        product.setValidityEnd(validityEnd);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setCreatedBy("test-user");
        product.setUpdatedBy("test-user");
        product.setVersion(1L);

        productRepository.save(product);
        return product.getId();
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
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
               "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJlbWFpbCI6InRlc3RAdXNlci5jb20iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0" +
               "LXVzZXIiLCJyb2xlIjoiVVNFUiIsImF1ZCI6ImJzc2JhY2tlbmQiLCJpc19hbm9ueW1vdXMiOnRydWUsImV4cCI6MTY0" +
               "NTk1OTIyMiwiYWRtaW4iOmZhbHNlfQ." +
               "dummy_signature_for_testing_only";
    }
}
