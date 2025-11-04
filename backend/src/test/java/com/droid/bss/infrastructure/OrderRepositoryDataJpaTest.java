package com.droid.bss.infrastructure;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.order.*;
import com.droid.bss.domain.order.repository.OrderRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Test scaffolding for OrderRepository
 * Tests all custom query methods and CRUD operations
 */
@DataJpaTest
@Testcontainers
@EnableJpaAuditing
@Transactional
@DirtiesContext
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@DisplayName("OrderRepository JPA Layer - Test Scaffolding")
class OrderRepositoryDataJpaTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private CustomerEntityRepository customerEntityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private OrderEntity testOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        customerEntityRepository.deleteAll();  // Clear customers to avoid conflicts
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
        // Let JPA generate the ID
        customer = customerEntityRepository.saveAndFlush(customer);
        // Clear entity manager to ensure we get fresh data from DB
        entityManager.clear();
        // Re-fetch to ensure we have the correct version
        customer = customerEntityRepository.findById(customer.getId()).orElseThrow();
        return customer;
    }

    // CRUD Operations

    @Test
    @DisplayName("should save and retrieve order by ID")
    void shouldSaveAndRetrieveOrderById() {
        // Given
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.refresh(savedOrder);

        // When
        var retrieved = orderRepository.findById(savedOrder.getId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getOrderNumber()).isEqualTo("ORD-001");
        assertThat(retrieved.get().getCustomer().getId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("should save multiple orders and retrieve all")
    void shouldSaveMultipleOrdersAndRetrieveAll() {
        // Given - create fresh customer to avoid version conflict
        CustomerEntity freshCustomer = createFreshCustomer();

        OrderEntity order1 = new OrderEntity(
            "ORD-001",
            freshCustomer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity order2 = new OrderEntity(
            "ORD-002",
            freshCustomer,
            OrderType.UPGRADE,
            OrderStatus.PROCESSING,
            OrderPriority.HIGH,
            new BigDecimal("199.99"),
            "PLN",
            LocalDate.now(),
            "API",
            "SALES002"
        );
        orderRepository.saveAll(List.of(order1, order2));
        entityManager.flush();
        entityManager.clear();

        // When
        List<OrderEntity> allOrders = orderRepository.findAll();

        // Then
        assertThat(allOrders).hasSize(2);
    }

    @Test
    @DisplayName("should delete order by ID")
    void shouldDeleteOrderById() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.refresh(savedOrder);

        // When
        orderRepository.deleteById(savedOrder.getId());

        // Then
        assertThat(orderRepository.findById(savedOrder.getId())).isEmpty();
    }

    // Custom Query Methods

    @Test
    @DisplayName("should find order by order number")
    void shouldFindOrderByOrderNumber() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = orderRepository.findByOrderNumber("ORD-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTotalAmount()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("should return empty when order number not found")
    void shouldReturnEmptyWhenOrderNumberNotFound() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // When
        var found = orderRepository.findByOrderNumber("NON-EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find orders by customer with pagination")
    void shouldFindOrdersByCustomerWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order1 = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity order2 = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(order1, order2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> customerOrders = orderRepository.findByCustomer(customer, pageable);

        // Then
        assertThat(customerOrders.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find orders by customer ID with pagination")
    void shouldFindOrdersByCustomerIdWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order1 = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity order2 = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(order1, order2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> customerOrders = orderRepository.findByCustomerId(customer.getId(), pageable);

        // Then
        assertThat(customerOrders.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find orders by status with pagination")
    void shouldFindOrdersByStatusWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity pendingOrder1 = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity pendingOrder2 = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity completedOrder = new OrderEntity(
            "ORD-003",
            customer,
            OrderType.CANCELLATION,
            OrderStatus.COMPLETED,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(pendingOrder1, pendingOrder2, completedOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING, pageable);

        // Then
        assertThat(pendingOrders.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("should find orders by order type with pagination")
    void shouldFindOrdersByOrderTypeWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity newSubscriptionOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity upgradeOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(newSubscriptionOrder, upgradeOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> newSubscriptionOrders = orderRepository.findByOrderType(OrderType.NEW_SUBSCRIPTION, pageable);

        // Then
        assertThat(newSubscriptionOrders.getContent()).hasSize(1);
        assertThat(newSubscriptionOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("should find orders by priority with pagination")
    void shouldFindOrdersByPriorityWithPagination() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity normalPriorityOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity highPriorityOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.HIGH,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(normalPriorityOrder, highPriorityOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> highPriorityOrders = orderRepository.findByPriority(OrderPriority.HIGH, pageable);

        // Then
        assertThat(highPriorityOrders.getContent()).hasSize(1);
        assertThat(highPriorityOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("should find pending orders")
    void shouldFindPendingOrders() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity pendingOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity processingOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PROCESSING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(pendingOrder, processingOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> pendingOrders = orderRepository.findPendingOrders(OrderStatus.PENDING, pageable);

        // Then
        assertThat(pendingOrders.getContent()).hasSize(1);
        assertThat(pendingOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("should find orders by date range")
    void shouldFindOrdersByDateRange() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity currentOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity pastOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.COMPLETED,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now().minusDays(30),
            "WEB",
            "SALES001"
        );
        OrderEntity futureOrder = new OrderEntity(
            "ORD-003",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now().plusDays(30),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(currentOrder, pastOrder, futureOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> rangeOrders = orderRepository.findByDateRange(
            LocalDate.now().minusDays(10),
            LocalDate.now().plusDays(10),
            pageable);

        // Then
        assertThat(rangeOrders.getContent()).hasSize(1);
        assertThat(rangeOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("should find overdue orders")
    void shouldFindOverdueOrders() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity overdueOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now().minusDays(15),
            "WEB",
            "SALES001"
        );
        overdueOrder.setPromisedDate(LocalDate.now().minusDays(5));

        OrderEntity notOverdueOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        notOverdueOrder.setPromisedDate(LocalDate.now().plusDays(10));

        orderRepository.saveAll(List.of(overdueOrder, notOverdueOrder));
        entityManager.flush();

        // When
        List<OrderEntity> overdueOrders = orderRepository.findOverdueOrders(
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED);

        // Then
        assertThat(overdueOrders).hasSize(1);
        assertThat(overdueOrders.get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("should search orders by order number or notes")
    void shouldSearchOrdersByOrderNumberOrNotes() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order1 = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity order2 = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        order2.setNotes("Urgent upgrade request");

        orderRepository.saveAll(List.of(order1, order2));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> searchResults = orderRepository.searchOrders("urgent", pageable);

        // Then
        assertThat(searchResults.getContent()).hasSize(1);
        assertThat(searchResults.getContent().get(0).getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("should find orders by order channel")
    void shouldFindOrdersByOrderChannel() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity webOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity apiOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "API",
            "SALES001"
        );
        orderRepository.saveAll(List.of(webOrder, apiOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> apiOrders = orderRepository.findByOrderChannel("API", pageable);

        // Then
        assertThat(apiOrders.getContent()).hasSize(1);
        assertThat(apiOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("should find orders by sales rep ID")
    void shouldFindOrdersBySalesRepId() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity rep1Order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity rep2Order = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES002"
        );
        orderRepository.saveAll(List.of(rep1Order, rep2Order));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> rep1Orders = orderRepository.findBySalesRepId("SALES001", pageable);

        // Then
        assertThat(rep1Orders.getContent()).hasSize(1);
        assertThat(rep1Orders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("should count orders by status")
    void shouldCountOrdersByStatus() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity pendingOrder1 = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity pendingOrder2 = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity completedOrder = new OrderEntity(
            "ORD-003",
            customer,
            OrderType.CANCELLATION,
            OrderStatus.COMPLETED,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(pendingOrder1, pendingOrder2, completedOrder));
        entityManager.flush();

        // When
        long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedCount = orderRepository.countByStatus(OrderStatus.COMPLETED);

        // Then
        assertThat(pendingCount).isEqualTo(2);
        assertThat(completedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should count orders by customer")
    void shouldCountOrdersByCustomer() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order1 = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity order2 = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(order1, order2));
        entityManager.flush();

        // When
        long customerOrderCount = orderRepository.countByCustomer(customer);

        // Then
        assertThat(customerOrderCount).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if order number exists")
    void shouldCheckIfOrderNumberExists() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.save(order);
        entityManager.flush();

        // When & Then
        assertThat(orderRepository.existsByOrderNumber("ORD-001")).isTrue();
        assertThat(orderRepository.existsByOrderNumber("NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("should find recent orders")
    void shouldFindRecentOrders() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity oldOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.COMPLETED,
            OrderPriority.NORMAL,
            BigDecimal.ONE,
            "PLN",
            LocalDate.now().minusDays(30),
            "WEB",
            "SALES001"
        );
        OrderEntity recentOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(oldOrder, recentOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> recentOrders = orderRepository.findRecentOrders(pageable);

        // Then
        assertThat(recentOrders.getContent()).hasSize(2);
        assertThat(recentOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-002"); // Most recent first
    }

    @Test
    @DisplayName("should find orders with total amount greater than")
    void shouldFindOrdersWithTotalAmountGreaterThan() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity smallOrder = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("50.00"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        OrderEntity largeOrder = new OrderEntity(
            "ORD-002",
            customer,
            OrderType.UPGRADE,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("200.00"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.saveAll(List.of(smallOrder, largeOrder));
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> largeOrders = orderRepository.findByTotalAmountGreaterThan(100.00, pageable);

        // Then
        assertThat(largeOrders.getContent()).hasSize(1);
        assertThat(largeOrders.getContent().get(0).getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("should return empty when no orders match search criteria")
    void shouldReturnEmptyWhenNoOrdersMatchSearchCriteria() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        OrderEntity order = new OrderEntity(
            "ORD-001",
            customer,
            OrderType.NEW_SUBSCRIPTION,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            new BigDecimal("99.99"),
            "PLN",
            LocalDate.now(),
            "WEB",
            "SALES001"
        );
        orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<OrderEntity> nonExistentOrders = orderRepository.findByStatus(OrderStatus.CANCELLED, pageable);

        // Then
        assertThat(nonExistentOrders.getContent()).isEmpty();
        assertThat(nonExistentOrders.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("should handle pagination for large result sets")
    void shouldHandlePaginationForLargeResultSets() {
        // Given - fix version conflict
        CustomerEntity customer = createFreshCustomer();
        for (int i = 1; i <= 25; i++) {
            OrderEntity order = new OrderEntity(
                "ORD-" + String.format("%03d", i),
                customer,
                OrderType.NEW_SUBSCRIPTION,
                OrderStatus.PENDING,
                OrderPriority.NORMAL,
                BigDecimal.ONE,
                "PLN",
                LocalDate.now(),
                "WEB",
                "SALES001"
            );
            orderRepository.save(order);
        }
        entityManager.flush();
        entityManager.clear();

        // When
        Page<OrderEntity> page1 = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 10));
        Page<OrderEntity> page2 = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(1, 10));
        Page<OrderEntity> page3 = orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(2, 10));

        // Then
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(10);
        assertThat(page3.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(25);
    }
}
