package com.droid.bss.infrastructure;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import org.junit.jupiter.api.Disabled;
import com.droid.bss.domain.order.OrderType;
import com.droid.bss.domain.product.*;
import com.droid.bss.domain.product.repository.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test scaffolding for SubscriptionRepository
 * Tests all custom query methods and CRUD operations
 */
@DataJpaTest
@Testcontainers
@DisplayName("SubscriptionRepository JPA Layer - Test Scaffolding")
class SubscriptionRepositoryDataJpaTest {

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
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CustomerEntityRepository customerEntityRepository;

    @Autowired
    private ProductRepository productRepository;

    private SubscriptionEntity testSubscription;
    private CustomerEntity testCustomer;
    private ProductEntity testProduct;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        productRepository.deleteAll();
        customerEntityRepository.deleteAll();

        // Create test customer
        testCustomer = new CustomerEntity();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setEmail("john.doe@example.com");
        testCustomer.setPhone("+48123456789");
        testCustomer.setStatus(CustomerStatus.ACTIVE);
        testCustomer = customerEntityRepository.save(testCustomer);

        // Create test product
        testProduct = new ProductEntity(
            "PROD-001",
            "Test Product",
            "Test product description",
            ProductType.SERVICE,
            ProductCategory.MOBILE,
            new BigDecimal("99.99"),
            "PLN",
            "MONTHLY",
            ProductStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(365)
        );
        testProduct = productRepository.save(testProduct);

        testSubscription = new SubscriptionEntity(
            "SUB-001",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            "MONTHLY",
            new BigDecimal("99.99"),
            "PLN",
            BigDecimal.ZERO,
            true
        );
        testSubscription.setNextBillingDate(LocalDate.now().plusDays(5));
    }

    // CRUD Operations

    @Test
    @DisplayName("should save and retrieve subscription by ID")
    @Disabled("Test scaffolding - implementation required")
    void shouldSaveAndRetrieveSubscriptionById() {
        // Given
        SubscriptionEntity savedSubscription = subscriptionRepository.save(testSubscription);

        // When
        var retrieved = subscriptionRepository.findById(savedSubscription.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getSubscriptionNumber()).isEqualTo("SUB-001");
        assertThat(retrieved.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("should save multiple subscriptions and retrieve all")
    @Disabled("Test scaffolding - implementation required")
    void shouldSaveMultipleSubscriptionsAndRetrieveAll() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            new BigDecimal("999.99"),
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));

        // When
        List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();

        // Then
        assertThat(allSubscriptions).hasSize(2);
    }

    @Test
    @DisplayName("should delete subscription by ID")
    @Disabled("Test scaffolding - implementation required")
    void shouldDeleteSubscriptionById() {
        // Given
        SubscriptionEntity savedSubscription = subscriptionRepository.save(testSubscription);

        // When
        subscriptionRepository.deleteById(savedSubscription.getId());

        // Then
        assertThat(subscriptionRepository.findById(savedSubscription.getId())).isEmpty();
    }

    // Custom Query Methods

    @Test
    @DisplayName("should find subscription by subscription number")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionBySubscriptionNumber() {
        // Given
        subscriptionRepository.save(testSubscription);

        // When
        var found = subscriptionRepository.findBySubscriptionNumber("SUB-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("should return empty when subscription number not found")
    @Disabled("Test scaffolding - implementation required")
    void shouldReturnEmptyWhenSubscriptionNumberNotFound() {
        // When
        var found = subscriptionRepository.findBySubscriptionNumber("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find subscriptions by customer with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByCustomerWithPagination() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> customerSubscriptions = subscriptionRepository.findByCustomer(testCustomer, pageable);

        // Then
        assertThat(customerSubscriptions.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find subscriptions by customer ID with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByCustomerIdWithPagination() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> customerSubscriptions = subscriptionRepository.findByCustomerId(testCustomer.getId(), pageable);

        // Then
        assertThat(customerSubscriptions.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find subscriptions by product with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByProductWithPagination() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> productSubscriptions = subscriptionRepository.findByProduct(testProduct, pageable);

        // Then
        assertThat(productSubscriptions.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find subscriptions by status with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByStatusWithPagination() {
        // Given
        SubscriptionEntity activeSubscription1 = testSubscription;
        SubscriptionEntity activeSubscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        SubscriptionEntity suspendedSubscription = new SubscriptionEntity(
            "SUB-003",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.SUSPENDED,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            false
        );
        subscriptionRepository.saveAll(List.of(activeSubscription1, activeSubscription2, suspendedSubscription));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE, pageable);

        // Then
        assertThat(activeSubscriptions.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find active subscriptions")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindActiveSubscriptions() {
        // Given
        SubscriptionEntity activeSubscription = testSubscription;
        SubscriptionEntity suspendedSubscription = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.SUSPENDED,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(activeSubscription, suspendedSubscription));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> activeSubscriptions = subscriptionRepository.findActiveSubscriptions(SubscriptionStatus.ACTIVE, pageable);

        // Then
        assertThat(activeSubscriptions.getContent()).hasSize(1);
        assertThat(activeSubscriptions.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should find subscriptions by billing period")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByBillingPeriod() {
        // Given
        SubscriptionEntity monthlySubscription = testSubscription;
        SubscriptionEntity yearlySubscription = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(monthlySubscription, yearlySubscription));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> monthlySubscriptions = subscriptionRepository.findByBillingPeriod("MONTHLY", pageable);

        // Then
        assertThat(monthlySubscriptions.getContent()).hasSize(1);
        assertThat(monthlySubscriptions.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should find subscriptions expiring within given days")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsExpiringWithinGivenDays() {
        // Given
        SubscriptionEntity expiringSoonSubscription = new SubscriptionEntity(
            "SUB-001",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            "MONTHLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        expiringSoonSubscription.setEndDate(LocalDate.now().plusDays(15));

        SubscriptionEntity expiringLaterSubscription = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            "MONTHLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        expiringLaterSubscription.setEndDate(LocalDate.now().plusDays(100));

        SubscriptionEntity noEndDateSubscription = testSubscription;

        subscriptionRepository.saveAll(List.of(expiringSoonSubscription, expiringLaterSubscription, noEndDateSubscription));

        // When
        List<SubscriptionEntity> expiringSubscriptions = subscriptionRepository.findExpiringSubscriptions(LocalDate.now().plusDays(30));

        // Then
        assertThat(expiringSubscriptions).hasSize(1);
        assertThat(expiringSubscriptions.get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should find subscriptions for renewal")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsForRenewal() {
        // Given
        SubscriptionEntity renewalSubscription = new SubscriptionEntity(
            "SUB-001",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            "MONTHLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        renewalSubscription.setNextBillingDate(LocalDate.now());

        SubscriptionEntity futureRenewalSubscription = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            "MONTHLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        futureRenewalSubscription.setNextBillingDate(LocalDate.now().plusDays(30));

        SubscriptionEntity noAutoRenewSubscription = new SubscriptionEntity(
            "SUB-003",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            "MONTHLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            false
        );
        noAutoRenewSubscription.setNextBillingDate(LocalDate.now());

        subscriptionRepository.saveAll(List.of(renewalSubscription, futureRenewalSubscription, noAutoRenewSubscription));

        // When
        List<SubscriptionEntity> subscriptionsForRenewal = subscriptionRepository.findSubscriptionsForRenewal();

        // Then
        assertThat(subscriptionsForRenewal).hasSize(1);
        assertThat(subscriptionsForRenewal.get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should find subscriptions by product ID with pagination")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByProductIdWithPagination() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> productSubscriptions = subscriptionRepository.findByProductId(testProduct.getId(), pageable);

        // Then
        assertThat(productSubscriptions.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should count subscriptions by customer")
    @Disabled("Test scaffolding - implementation required")
    void shouldCountSubscriptionsByCustomer() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));

        // When
        long customerSubscriptionCount = subscriptionRepository.countByCustomer(testCustomer);

        // Then
        assertThat(customerSubscriptionCount).isEqualTo(2);
    }

    @Test
    @DisplayName("should count subscriptions by status")
    @Disabled("Test scaffolding - implementation required")
    void shouldCountSubscriptionsByStatus() {
        // Given
        SubscriptionEntity activeSubscription1 = testSubscription;
        SubscriptionEntity activeSubscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        SubscriptionEntity suspendedSubscription = new SubscriptionEntity(
            "SUB-003",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.SUSPENDED,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            false
        );
        subscriptionRepository.saveAll(List.of(activeSubscription1, activeSubscription2, suspendedSubscription));

        // When
        long activeCount = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long suspendedCount = subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(suspendedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should check if subscription number exists")
    @Disabled("Test scaffolding - implementation required")
    void shouldCheckIfSubscriptionNumberExists() {
        // Given
        subscriptionRepository.save(testSubscription);

        // When & Then
        assertThat(subscriptionRepository.existsBySubscriptionNumber("SUB-001")).isTrue();
        assertThat(subscriptionRepository.existsBySubscriptionNumber("NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("should find subscriptions with auto-renewal enabled")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsWithAutoRenewalEnabled() {
        // Given
        SubscriptionEntity autoRenewSubscription = testSubscription;
        SubscriptionEntity noAutoRenewSubscription = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            false
        );
        subscriptionRepository.saveAll(List.of(autoRenewSubscription, noAutoRenewSubscription));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> autoRenewSubscriptions = subscriptionRepository.findAutoRenewSubscriptions(pageable);

        // Then
        assertThat(autoRenewSubscriptions.getContent()).hasSize(1);
        assertThat(autoRenewSubscriptions.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should find suspended subscriptions")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSuspendedSubscriptions() {
        // Given
        SubscriptionEntity suspendedSubscription = new SubscriptionEntity(
            "SUB-001",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.SUSPENDED,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            false
        );
        SubscriptionEntity activeSubscription = testSubscription;
        subscriptionRepository.saveAll(List.of(suspendedSubscription, activeSubscription));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> suspendedSubscriptions = subscriptionRepository.findSuspendedSubscriptions(SubscriptionStatus.SUSPENDED, pageable);

        // Then
        assertThat(suspendedSubscriptions.getContent()).hasSize(1);
        assertThat(suspendedSubscriptions.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should find subscriptions by date range")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsByDateRange() {
        // Given
        SubscriptionEntity currentSubscription = testSubscription;
        SubscriptionEntity pastSubscription = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().minusDays(60),
            LocalDate.now().minusDays(60),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        SubscriptionEntity futureSubscription = new SubscriptionEntity(
            "SUB-003",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(30),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(currentSubscription, pastSubscription, futureSubscription));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> rangeSubscriptions = subscriptionRepository.findByStartDateRange(
            LocalDate.now().minusDays(10),
            LocalDate.now().plusDays(10),
            pageable);

        // Then
        assertThat(rangeSubscriptions.getContent()).hasSize(1);
        assertThat(rangeSubscriptions.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-001");
    }

    @Test
    @DisplayName("should search subscriptions by subscription number")
    @Disabled("Test scaffolding - implementation required")
    void shouldSearchSubscriptionsBySubscriptionNumber() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> searchResults = subscriptionRepository.searchSubscriptions("002", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-002");
    }

    @Test
    @DisplayName("should return empty when no subscriptions match search criteria")
    @Disabled("Test scaffolding - implementation required")
    void shouldReturnEmptyWhenNoSubscriptionsMatchSearchCriteria() {
        // Given
        subscriptionRepository.save(testSubscription);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> nonExistentSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.CANCELLED, pageable);

        // Then
        assertThat(nonExistentSubscriptions.getContent()).isEmpty();
        assertThat(nonExistentSubscriptions.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("should handle case-insensitive search")
    @Disabled("Test scaffolding - implementation required")
    void shouldHandleCaseInsensitiveSearch() {
        // Given
        SubscriptionEntity subscription1 = testSubscription;
        SubscriptionEntity subscription2 = new SubscriptionEntity(
            "SUB-PREMIUM-001",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionRepository.saveAll(List.of(subscription1, subscription2));
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<SubscriptionEntity> searchResults = subscriptionRepository.searchSubscriptions("premium", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getSubscriptionNumber()).isEqualTo("SUB-PREMIUM-001");
    }

    @Test
    @DisplayName("should handle subscriptions without end dates")
    @Disabled("Test scaffolding - implementation required")
    void shouldHandleSubscriptionsWithoutEndDates() {
        // Given
        SubscriptionEntity subscriptionWithoutEndDate = testSubscription;
        SubscriptionEntity subscriptionWithEndDate = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );
        subscriptionWithEndDate.setEndDate(LocalDate.now().plusYears(1));
        subscriptionRepository.saveAll(List.of(subscriptionWithoutEndDate, subscriptionWithEndDate));

        // When
        List<SubscriptionEntity> expiringSubscriptions = subscriptionRepository.findExpiringSubscriptions(LocalDate.now().plusDays(30));

        // Then
        assertThat(expiringSubscriptions).hasSize(0); // No subscriptions expiring in 30 days
    }

    @Test
    @DisplayName("should find subscriptions with configuration map")
    @Disabled("Test scaffolding - implementation required")
    void shouldFindSubscriptionsWithConfigurationMap() {
        // Given
        Map<String, Object> config = new HashMap<>();
        config.put("feature_flag", true);
        config.put("max_users", 100);

        SubscriptionEntity subscriptionWithConfig = testSubscription;
        subscriptionWithConfig.setConfiguration(config);

        SubscriptionEntity subscriptionWithoutConfig = new SubscriptionEntity(
            "SUB-002",
            testCustomer,
            testProduct,
            null,
            SubscriptionStatus.ACTIVE,
            LocalDate.now(),
            LocalDate.now(),
            "YEARLY",
            BigDecimal.ONE,
            "PLN",
            BigDecimal.ZERO,
            true
        );

        subscriptionRepository.saveAll(List.of(subscriptionWithConfig, subscriptionWithoutConfig));

        // When
        var retrieved = subscriptionRepository.findById(subscriptionWithConfig.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getConfiguration()).isNotNull();
        assertThat(retrieved.get().getConfiguration().get("feature_flag")).isEqualTo(true);
        assertThat(retrieved.get().getConfiguration().get("max_users")).isEqualTo(100);
    }
}
