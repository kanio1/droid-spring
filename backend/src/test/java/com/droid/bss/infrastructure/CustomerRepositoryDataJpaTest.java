package com.droid.bss.infrastructure;

import com.droid.bss.domain.customer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Import(com.droid.bss.infrastructure.write.CustomerRepositoryImpl.class)
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
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        customerRepository.deleteById(testCustomerId());
        
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        testCustomer = Customer.create(personalInfo, contactInfo);
    }

    @Test
    @DisplayName("should save and retrieve customer")
    void shouldSaveAndRetrieveCustomer() {
        // Given
        Customer savedCustomer = customerRepository.save(testCustomer);

        // When
        Optional<Customer> retrieved = customerRepository.findById(savedCustomer.getId());

        // Then
        assertThat(retrieved).isPresent();
        Customer customer = retrieved.get();
        assertThat(customer.getId()).isEqualTo(savedCustomer.getId());
        assertThat(customer.getPersonalInfo().firstName()).isEqualTo("John");
        assertThat(customer.getPersonalInfo().lastName()).isEqualTo("Doe");
        assertThat(customer.getContactInfo().email()).isEqualTo("john.doe@example.com");
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("should find customer by PESEL")
    void shouldFindCustomerByPesel() {
        // Given
        customerRepository.save(testCustomer);

        // When
        Optional<Customer> found = customerRepository.findByPesel("12345678901");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getContactInfo().email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("should find customer by NIP")
    void shouldFindCustomerByNip() {
        // Given
        customerRepository.save(testCustomer);

        // When
        Optional<Customer> found = customerRepository.findByNip("1234567890");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getContactInfo().email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("should return empty when PESEL not found")
    void shouldReturnEmptyWhenPeselNotFound() {
        // When
        Optional<Customer> found = customerRepository.findByPesel("99999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return empty when NIP not found")
    void shouldReturnEmptyWhenNipNotFound() {
        // When
        Optional<Customer> found = customerRepository.findByNip("9999999999");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should find all customers with pagination")
    void shouldFindAllCustomersWithPagination() {
        // Given - create multiple customers
        for (int i = 1; i <= 5; i++) {
            CustomerInfo personalInfo = new CustomerInfo("FirstName" + i, "LastName" + i, "1234567890" + i, "123456789" + i);
            ContactInfo contactInfo = new ContactInfo("user" + i + "@example.com", "+4812345678" + i);
            customerRepository.save(Customer.create(personalInfo, contactInfo));
        }

        // When
        List<Customer> page1 = customerRepository.findAll(0, 3);
        List<Customer> page2 = customerRepository.findAll(1, 3);

        // Then
        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(3); // 6 total - 3 = 3 on second page
        assertThat(page1.get(0).getPersonalInfo().firstName()).isEqualTo("FirstName5"); // Most recent first
    }

    @Test
    @DisplayName("should find customers by status")
    void shouldFindCustomersByStatus() {
        // Given
        customerRepository.save(testCustomer);
        
        CustomerInfo suspendedInfo = new CustomerInfo("Jane", "Doe", "98765432109", "0987654321");
        ContactInfo suspendedContact = new ContactInfo("jane.doe@example.com", "+48987654321");
        Customer suspendedCustomer = Customer.create(suspendedInfo, suspendedContact).suspend();
        customerRepository.save(suspendedCustomer);

        // When
        List<Customer> activeCustomers = customerRepository.findByStatus(CustomerStatus.ACTIVE, 0, 10);
        List<Customer> suspendedCustomers = customerRepository.findByStatus(CustomerStatus.SUSPENDED, 0, 10);

        // Then
        assertThat(activeCustomers).hasSize(1);
        assertThat(activeCustomers.get(0).getContactInfo().email()).isEqualTo("john.doe@example.com");
        assertThat(suspendedCustomers).hasSize(1);
        assertThat(suspendedCustomers.get(0).getContactInfo().email()).isEqualTo("jane.doe@example.com");
    }

    @Test
    @DisplayName("should search customers by term")
    void shouldSearchCustomersByTerm() {
        // Given
        customerRepository.save(testCustomer);
        
        CustomerInfo searchInfo = new CustomerInfo("John", "Smith", "11111111111", "2222222222");
        ContactInfo searchContact = new ContactInfo("john.smith@example.com", "+48987654321");
        customerRepository.save(Customer.create(searchInfo, searchContact));

        // When
        List<Customer> johnResults = customerRepository.search("john", 0, 10);
        List<Customer> smithResults = customerRepository.search("smith", 0, 10);

        // Then
        assertThat(johnResults).hasSize(2);
        assertThat(smithResults).hasSize(1);
    }

    @Test
    @DisplayName("should return all customers when search term is empty")
    void shouldReturnAllCustomersWhenSearchTermIsEmpty() {
        // Given
        customerRepository.save(testCustomer);

        // When
        List<Customer> results = customerRepository.search("", 0, 10);
        List<Customer> nullResults = customerRepository.search(null, 0, 10);

        // Then
        assertThat(results).hasSize(1);
        assertThat(nullResults).hasSize(1);
        assertThat(results.get(0).getContactInfo().email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("should check if PESEL exists")
    void shouldCheckIfPeselExists() {
        // Given
        customerRepository.save(testCustomer);

        // When & Then
        assertThat(customerRepository.existsByPesel("12345678901")).isTrue();
        assertThat(customerRepository.existsByPesel("99999999999")).isFalse();
    }

    @Test
    @DisplayName("should check if NIP exists")
    void shouldCheckIfNipExists() {
        // Given
        customerRepository.save(testCustomer);

        // When & Then
        assertThat(customerRepository.existsByNip("1234567890")).isTrue();
        assertThat(customerRepository.existsByNip("9999999999")).isFalse();
    }

    @Test
    @DisplayName("should count customers")
    void shouldCountCustomers() {
        // Given
        assertThat(customerRepository.count()).isEqualTo(0);
        customerRepository.save(testCustomer);

        // When
        long count = customerRepository.count();

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("should count customers by status")
    void shouldCountCustomersByStatus() {
        // Given
        customerRepository.save(testCustomer);
        
        CustomerInfo suspendedInfo = new CustomerInfo("Jane", "Doe", "98765432109", "0987654321");
        ContactInfo suspendedContact = new ContactInfo("jane.doe@example.com", "+48987654321");
        Customer suspendedCustomer = Customer.create(suspendedInfo, suspendedContact).suspend();
        customerRepository.save(suspendedCustomer);

        // When
        long activeCount = customerRepository.countByStatus(CustomerStatus.ACTIVE);
        long suspendedCount = customerRepository.countByStatus(CustomerStatus.SUSPENDED);

        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(suspendedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should delete customer by ID")
    void shouldDeleteCustomerById() {
        // Given
        Customer savedCustomer = customerRepository.save(testCustomer);
        assertThat(customerRepository.findById(savedCustomer.getId())).isPresent();

        // When
        boolean deleted = customerRepository.deleteById(savedCustomer.getId());

        // Then
        assertThat(deleted).isTrue();
        assertThat(customerRepository.findById(savedCustomer.getId())).isEmpty();
    }

    @Test
    @DisplayName("should return false when deleting non-existent customer")
    void shouldReturnFalseWhenDeletingNonExistentCustomer() {
        // When
        boolean deleted = customerRepository.deleteById(testCustomerId());

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("should handle customer update with version increment")
    void shouldHandleCustomerUpdateWithVersionIncrement() {
        // Given
        Customer savedCustomer = customerRepository.save(testCustomer);
        assertThat(savedCustomer.getVersion()).isEqualTo(1);

        // When
        CustomerInfo newInfo = new CustomerInfo("Updated", "Name", "12345678901", "1234567890");
        Customer updatedCustomer = savedCustomer.updatePersonalInfo(newInfo);
        Customer savedUpdated = customerRepository.save(updatedCustomer);

        // Then
        assertThat(savedUpdated.getVersion()).isEqualTo(2);
        assertThat(savedUpdated.getPersonalInfo().firstName()).isEqualTo("Updated");
    }

    private CustomerId testCustomerId() {
        return new CustomerId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
    }
}
