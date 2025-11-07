package com.droid.bss.integration;

import org.springframework.context.annotation.Import;

import com.droid.bss.application.dto.customer.ChangeCustomerStatusCommand;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.application.dto.product.CreateProductCommand;
import com.droid.bss.application.dto.product.ProductResponse;
import com.droid.bss.domain.customer.*;
import com.droid.bss.domain.order.*;
import com.droid.bss.domain.order.repository.OrderRepository;
import com.droid.bss.domain.product.*;
import com.droid.bss.domain.product.repository.ProductRepository;
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
 * Integration test for end-to-end order flow
 * Tests the complete order lifecycle from creation to completion
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(com.droid.bss.integration.config.IntegrationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "security.oauth2.audience=bss-backend"
})
@DisplayName("Order Flow Integration Tests")
class OrderFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final String validJwtToken = createValidJwtToken();

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.findAll(0, Integer.MAX_VALUE).forEach(
                customer -> customerRepository.deleteById(customer.getId())
        );
    }

    @Test
    @DisplayName("Should complete full order lifecycle")
    void shouldCompleteFullOrderLifecycle() {
        // ===== STEP 1: Create Customer =====
        // Given - Create a customer
        CustomerId customerId = createTestCustomer("John", "Doe", "john.doe@example.com");
        assertThat(customerId).isNotNull();

        // When - Get customer
        ResponseEntity<CustomerResponse> customerResponse = restTemplate.getForEntity(
                "/api/customers/{id}", CustomerResponse.class, customerId.toString());

        // Then - Customer should be created and active
        assertThat(customerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customerResponse.getBody()).isNotNull();
        assertThat(customerResponse.getBody().status()).isEqualTo("ACTIVE");

        // ===== STEP 2: Create Product =====
        // Given - Create a product
        UUID productId = createTestProduct("MOBILE-001", "Mobile Service Premium");

        // When - Get product
        ResponseEntity<ProductResponse> productResponse = restTemplate.getForEntity(
                "/api/products/{id}", ProductResponse.class, productId);

        // Then - Product should be created
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(productResponse.getBody()).isNotNull();
        assertThat(productResponse.getBody().id()).isEqualTo(productId);

        // ===== STEP 3: Create Order with Items =====
        // When - Create order in database
        OrderEntity order = createOrder(customerId, productId, OrderType.NEW, OrderStatus.PENDING);

        // Then - Order should be created with PENDING status
        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);

        // Verify order item details
        OrderItemEntity orderItem = order.getItems().get(0);
        assertThat(orderItem.getItemName()).isEqualTo("Mobile Service Premium");
        assertThat(orderItem.getQuantity()).isEqualTo(1);
        assertThat(orderItem.getUnitPrice()).isEqualTo(BigDecimal.valueOf(29.99));

        // ===== STEP 4: Update Order Status to APPROVED =====
        // When - Update order status
        order.setStatus(OrderStatus.APPROVED);
        OrderEntity approvedOrder = orderRepository.save(order);

        // Then - Order should be APPROVED
        assertThat(approvedOrder.getStatus()).isEqualTo(OrderStatus.APPROVED);

        // ===== STEP 5: Update Order Status to IN_PROGRESS =====
        // When - Update order status to IN_PROGRESS
        approvedOrder.setStatus(OrderStatus.IN_PROGRESS);
        OrderEntity inProgressOrder = orderRepository.save(approvedOrder);

        // Then - Order should be IN_PROGRESS
        assertThat(inProgressOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);

        // ===== STEP 6: Activate Order Items =====
        // When - Activate order items
        for (OrderItemEntity item : inProgressOrder.getItems()) {
            item.setStatus(OrderItemStatus.ACTIVE);
            item.setActivationDate(LocalDate.now());
        }
        OrderEntity activeOrder = orderRepository.save(inProgressOrder);

        // Then - Items should be ACTIVE
        assertThat(activeOrder.getItems().get(0).getStatus()).isEqualTo(OrderItemStatus.ACTIVE);
        assertThat(activeOrder.getItems().get(0).getActivationDate()).isEqualTo(LocalDate.now());

        // ===== STEP 7: Complete Order =====
        // When - Mark order as completed
        activeOrder.markAsCompleted();
        OrderEntity completedOrder = orderRepository.save(activeOrder);

        // Then - Order should be COMPLETED
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completedOrder.getCompletedDate()).isEqualTo(LocalDate.now());

        // ===== VERIFICATION: Check total amount =====
        assertThat(completedOrder.getTotalAmount()).isEqualTo(BigDecimal.valueOf(29.99));
    }

    @Test
    @DisplayName("Should handle order cancellation flow")
    void shouldHandleOrderCancellationFlow() {
        // Given - Create customer and product
        CustomerId customerId = createTestCustomer("Jane", "Smith", "jane.smith@example.com");
        UUID productId = createTestProduct("BROADBAND-001", "Broadband Internet");

        // When - Create order
        OrderEntity order = createOrder(customerId, productId, OrderType.NEW, OrderStatus.PENDING);

        // Then - Order is PENDING
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

        // When - Approve order
        order.setStatus(OrderStatus.APPROVED);
        orderRepository.save(order);

        // When - Cancel order
        order.setStatus(OrderStatus.CANCELLED);
        OrderEntity cancelledOrder = orderRepository.save(order);

        // Then - Order should be CANCELLED
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.canBeCancelled()).isTrue();
    }

    @Test
    @DisplayName("Should handle order with multiple items")
    void shouldHandleOrderWithMultipleItems() {
        // Given - Create customer and products
        CustomerId customerId = createTestCustomer("Bob", "Johnson", "bob.johnson@example.com");
        UUID productId1 = createTestProduct("MOBILE-001", "Mobile Service");
        UUID productId2 = createTestProduct("BROADBAND-001", "Broadband Service");

        // When - Create order with multiple items
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        CustomerEntity customerEntity = CustomerEntity.from(customer);
        OrderEntity order = new OrderEntity(
                "ORD-2024-001",
                customerEntity,
                OrderType.NEW,
                OrderStatus.PENDING,
                OrderPriority.NORMAL,
                BigDecimal.ZERO,
                "PLN",
                LocalDate.now(),
                "WEB",
                "sales-rep-001"
        );

        // Add first item
        ProductEntity product1 = productRepository.findById(productId1).orElseThrow();
        OrderItemEntity item1 = new OrderItemEntity(
                order, product1, OrderItemType.PRODUCT, "MOBILE-001",
                "Mobile Service", 1, BigDecimal.valueOf(29.99),
                BigDecimal.ZERO, BigDecimal.valueOf(23.00)
        );
        order.addItem(item1);

        // Add second item
        ProductEntity product2 = productRepository.findById(productId2).orElseThrow();
        OrderItemEntity item2 = new OrderItemEntity(
                order, product2, OrderItemType.PRODUCT, "BROADBAND-001",
                "Broadband Service", 1, BigDecimal.valueOf(49.99),
                BigDecimal.ZERO, BigDecimal.valueOf(23.00)
        );
        order.addItem(item2);

        OrderEntity savedOrder = orderRepository.save(order);

        // Then - Order should have 2 items
        assertThat(savedOrder.getItems()).hasSize(2);

        // And total should be calculated correctly
        assertThat(savedOrder.getTotalAmount()).isEqualTo(BigDecimal.valueOf(79.98));
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

    private UUID createTestProduct(String productCode, String name) {
        return createTestProduct(productCode, name, ProductStatus.ACTIVE);
    }

    private UUID createTestProduct(String productCode, String name, ProductStatus status) {
        ProductEntity product = new ProductEntity();
        product.setId(UUID.randomUUID());
        product.setProductCode(productCode);
        product.setName(name);
        product.setDescription("Test product");
        product.setProductType(ProductType.SERVICE);
        product.setCategory(ProductCategory.MOBILE);
        product.setPrice(BigDecimal.valueOf(29.99));
        product.setCurrency("PLN");
        product.setBillingPeriod("monthly");
        product.setStatus(status);
        product.setValidityStart(LocalDate.now());
        product.setValidityEnd(LocalDate.now().plusMonths(12));
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setCreatedBy("test-user");
        product.setUpdatedBy("test-user");
        product.setVersion(1L);

        productRepository.save(product);
        return product.getId();
    }

    private OrderEntity createOrder(CustomerId customerId, UUID productId, OrderType orderType, OrderStatus status) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        CustomerEntity customerEntity = CustomerEntity.from(customer);
        ProductEntity product = productRepository.findById(productId).orElseThrow();

        OrderEntity order = new OrderEntity(
                "ORD-TEST-" + System.currentTimeMillis(),
                customerEntity,
                orderType,
                status,
                OrderPriority.NORMAL,
                BigDecimal.ZERO,
                "PLN",
                LocalDate.now(),
                "API",
                "test-rep"
        );

        // Add order item
        OrderItemEntity orderItem = new OrderItemEntity(
                order, product, OrderItemType.PRODUCT, product.getProductCode(),
                product.getName(), 1, product.getPrice(),
                BigDecimal.ZERO, BigDecimal.valueOf(23.00)
        );
        order.addItem(orderItem);

        return orderRepository.save(order);
    }

    private String createValidJwtToken() {
        // In a real integration test, you would use Keycloak test container or wiremock
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
               "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJlbWFpbCI6InRlc3RAdXNlci5jb20iLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0" +
               "LXVzZXIiLCJyb2xlIjoiVVNFUiIsImF1ZCI6ImJzc2JhY2tlbmQiLCJpc19hbm9ueW1vdXMiOnRydWUsImV4cCI6MTY0" +
               "NTk1OTIyMiwiYWRtaW4iOmZhbHNlfQ." +
               "dummy_signature_for_testing_only";
    }
}
