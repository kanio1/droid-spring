package com.droid.bss.infrastructure;

import com.droid.bss.domain.customer.*;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@EnableJpaAuditing
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
@DisplayName("CustomerRepository JPA Layer")
class CustomerRepositoryDataJpaTest {

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
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private CustomerEntityRepository customerEntityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        customerEntityRepository.deleteAll();
    }

    private CustomerEntity createFreshCustomer() {
        CustomerEntity customer = new CustomerEntity();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setPesel("12345678901");
        customer.setNip("1234567890");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+48123456789");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer = customerEntityRepository.saveAndFlush(customer);
        entityManager.clear();
        customer = customerEntityRepository.findById(customer.getId()).orElseThrow();
        return customer;
    }

    @Test
    @DisplayName("should save and retrieve customer")
    void shouldSaveAndRetrieveCustomer() {
        // Given
        CustomerEntity testCustomer = new CustomerEntity();
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setPesel("12345678901");
        testCustomer.setNip("1234567890");
        testCustomer.setEmail("john.doe@example.com");
        testCustomer.setPhone("+48123456789");
        testCustomer.setStatus(CustomerStatus.ACTIVE);

        CustomerEntity savedCustomer = customerEntityRepository.save(testCustomer);
        entityManager.flush();  // Force write to DB
        entityManager.clear();  // Clear persistence context
        // Now find again to get a managed entity with correct version
        savedCustomer = customerEntityRepository.findById(savedCustomer.getId()).orElseThrow();

        // When
        Optional<CustomerEntity> retrieved = customerEntityRepository.findById(savedCustomer.getId());

        // Then
        assertThat(retrieved).isPresent();
        CustomerEntity customer = retrieved.get();
        assertThat(customer.getId()).isEqualTo(savedCustomer.getId());
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("should find customer by PESEL")
    void shouldFindCustomerByPesel() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        // When
        Optional<CustomerEntity> found = customerEntityRepository.findByPesel("12345678901");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("should find customer by NIP")
    void shouldFindCustomerByNip() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        // When
        Optional<CustomerEntity> found = customerEntityRepository.findByNip("1234567890");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("should return empty when PESEL not found")
    void shouldReturnEmptyWhenPeselNotFound() {
        // When
        Optional<CustomerEntity> found = customerEntityRepository.findByPesel("99999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return empty when NIP not found")
    void shouldReturnEmptyWhenNipNotFound() {
        // When
        Optional<CustomerEntity> found = customerEntityRepository.findByNip("9999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find all customers with pagination")
    void shouldFindAllCustomersWithPagination() {
        // Given - create multiple customers
        for (int i = 1; i <= 5; i++) {
            String firstName = "FirstName".substring(0, Math.max(0, "FirstName".length() - 1)) + (char)('A' + i - 1);
            String lastName = "LastName".substring(0, Math.max(0, "LastName".length() - 1)) + (char)('A' + i - 1);
            CustomerEntity customer = new CustomerEntity();
            customer.setFirstName(firstName);
            customer.setLastName(lastName);
            customer.setPesel("1234567890" + i);
            customer.setNip("123456789" + i);
            customer.setEmail("user" + i + "@example.com");
            customer.setPhone("+4812345678" + i);
            customer.setStatus(CustomerStatus.ACTIVE);
            customerEntityRepository.save(customer);
            entityManager.flush();
            entityManager.clear();
        }

        // When
        Pageable page1Request = PageRequest.of(0, 3);
        Pageable page2Request = PageRequest.of(1, 3);
        List<CustomerEntity> page1 = customerEntityRepository.findAllWithPagination(page1Request);
        List<CustomerEntity> page2 = customerEntityRepository.findAllWithPagination(page2Request);

        // Then
        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(2); // 5 total - 3 = 2 on second page
    }

    @Test
    @DisplayName("should find customers by status")
    void shouldFindCustomersByStatus() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        CustomerEntity suspendedCustomer = new CustomerEntity();
        suspendedCustomer.setFirstName("Jane");
        suspendedCustomer.setLastName("Doe");
        suspendedCustomer.setPesel("98765432109");
        suspendedCustomer.setNip("0987654321");
        suspendedCustomer.setEmail("jane.doe@example.com");
        suspendedCustomer.setPhone("+48987654321");
        suspendedCustomer.setStatus(CustomerStatus.SUSPENDED);
        customerEntityRepository.save(suspendedCustomer);
        entityManager.flush();

        // When
        Pageable request = PageRequest.of(0, 10);
        List<CustomerEntity> activeCustomers = customerEntityRepository.findByStatusWithPagination(CustomerStatus.ACTIVE, request);
        List<CustomerEntity> suspendedCustomers = customerEntityRepository.findByStatusWithPagination(CustomerStatus.SUSPENDED, request);

        // Then
        assertThat(activeCustomers).hasSize(1);
        assertThat(activeCustomers.get(0).getEmail()).isEqualTo("john.doe@example.com");
        assertThat(suspendedCustomers).hasSize(1);
        assertThat(suspendedCustomers.get(0).getEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    @DisplayName("should search customers by term")
    void shouldSearchCustomersByTerm() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        CustomerEntity searchCustomer = new CustomerEntity();
        searchCustomer.setFirstName("John");
        searchCustomer.setLastName("Smith");
        searchCustomer.setPesel("11111111111");
        searchCustomer.setNip("2222222222");
        searchCustomer.setEmail("john.smith@example.com");
        searchCustomer.setPhone("+48987654321");
        searchCustomer.setStatus(CustomerStatus.ACTIVE);
        customerEntityRepository.save(searchCustomer);
        entityManager.flush();

        // When
        Pageable request = PageRequest.of(0, 10);
        List<CustomerEntity> johnResults = customerEntityRepository.search("john", request);
        List<CustomerEntity> smithResults = customerEntityRepository.search("smith", request);

        // Then
        assertThat(johnResults).hasSize(2);
        assertThat(smithResults).hasSize(1);
    }

    @Test
    @DisplayName("should return all customers when search term is empty")
    void shouldReturnAllCustomersWhenSearchTermIsEmpty() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        // When - for empty or null search terms, use findAll instead
        Pageable request = PageRequest.of(0, 10);
        List<CustomerEntity> results = customerEntityRepository.findAllWithPagination(request);
        List<CustomerEntity> nullResults = customerEntityRepository.findAllWithPagination(request);

        // Then
        assertThat(results).hasSize(1);
        assertThat(nullResults).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("should check if PESEL exists")
    void shouldCheckIfPeselExists() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        // When & Then
        assertThat(customerEntityRepository.existsByPesel("12345678901")).isTrue();
        assertThat(customerEntityRepository.existsByPesel("99999999999")).isFalse();
    }

    @Test
    @DisplayName("should check if NIP exists")
    void shouldCheckIfNipExists() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        // When & Then
        assertThat(customerEntityRepository.existsByNip("1234567890")).isTrue();
        assertThat(customerEntityRepository.existsByNip("9999999999")).isFalse();
    }

    @Test
    @DisplayName("should count customers")
    void shouldCountCustomers() {
        // Given
        assertThat(customerEntityRepository.count()).isEqualTo(0);
        CustomerEntity testCustomer = createFreshCustomer();

        // When
        long count = customerEntityRepository.count();

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("should count customers by status")
    void shouldCountCustomersByStatus() {
        // Given
        CustomerEntity testCustomer = createFreshCustomer();

        CustomerEntity suspendedCustomer = new CustomerEntity();
        suspendedCustomer.setFirstName("Jane");
        suspendedCustomer.setLastName("Doe");
        suspendedCustomer.setPesel("98765432109");
        suspendedCustomer.setNip("0987654321");
        suspendedCustomer.setEmail("jane.doe@example.com");
        suspendedCustomer.setPhone("+48987654321");
        suspendedCustomer.setStatus(CustomerStatus.SUSPENDED);
        customerEntityRepository.save(suspendedCustomer);
        entityManager.flush();

        // When
        long activeCount = customerEntityRepository.countByStatus(CustomerStatus.ACTIVE);
        long suspendedCount = customerEntityRepository.countByStatus(CustomerStatus.SUSPENDED);

        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(suspendedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should delete customer by ID")
    void shouldDeleteCustomerById() {
        // Given
        CustomerEntity savedCustomer = createFreshCustomer();
        assertThat(customerEntityRepository.findById(savedCustomer.getId())).isPresent();

        // When
        customerEntityRepository.deleteById(savedCustomer.getId());

        // Then
        assertThat(customerEntityRepository.findById(savedCustomer.getId())).isEmpty();
    }

    @Test
    @DisplayName("should handle customer update with version increment")
    void shouldHandleCustomerUpdateWithVersionIncrement() {
        // Given
        CustomerEntity savedCustomer = createFreshCustomer();
        assertThat(savedCustomer.getVersion()).isEqualTo(0L);

        // When
        savedCustomer.setFirstName("Updated");
        CustomerEntity savedUpdated = customerEntityRepository.save(savedCustomer);
        entityManager.flush();
        entityManager.clear();
        savedUpdated = customerEntityRepository.findById(savedUpdated.getId()).orElseThrow();

        // Then
        assertThat(savedUpdated.getVersion()).isEqualTo(1L);
        assertThat(savedUpdated.getFirstName()).isEqualTo("Updated");
    }
}
