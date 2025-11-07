package com.droid.bss.application.query.customer;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.infrastructure.read.CustomerReadRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerQueryService
 */
class CustomerQueryServiceTest {

    @Mock
    private CustomerReadRepository customerReadRepository;

    @InjectMocks
    private CustomerQueryService customerQueryService;

    @Test
    @DisplayName("Should find customer by ID")
    void shouldFindCustomerById() {
        // Given
        UUID customerId = UUID.randomUUID();
        CustomerEntity customer = CustomerEntity.builder()
            .id(customerId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();

        when(customerReadRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));

        // When
        Optional<CustomerEntity> result = customerQueryService.findById(customerId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(customerId, result.get().getId());
        assertEquals("John", result.get().getFirstName());
        assertEquals("Doe", result.get().getLastName());

        verify(customerReadRepository, times(1)).findById(eq(customerId));
    }

    @Test
    @DisplayName("Should return empty when customer not found")
    void shouldReturnEmptyWhenCustomerNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(customerReadRepository.findById(eq(nonExistentId))).thenReturn(Optional.empty());

        // When
        Optional<CustomerEntity> result = customerQueryService.findById(nonExistentId);

        // Then
        assertFalse(result.isPresent());

        verify(customerReadRepository, times(1)).findById(eq(nonExistentId));
    }

    @Test
    @DisplayName("Should find all customers")
    void shouldFindAllCustomers() {
        // Given
        CustomerEntity customer1 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .status(CustomerStatus.ACTIVE)
            .build();

        CustomerEntity customer2 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Jane")
            .lastName("Smith")
            .email("jane@example.com")
            .status(CustomerStatus.ACTIVE)
            .build();

        List<CustomerEntity> customers = Arrays.asList(customer1, customer2);
        when(customerReadRepository.findAll()).thenReturn(customers);

        // When
        List<CustomerEntity> result = customerQueryService.findAll();

        // Then
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Jane", result.get(1).getFirstName());

        verify(customerReadRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find customers by status")
    void shouldFindCustomersByStatus() {
        // Given
        CustomerEntity activeCustomer1 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Bob")
            .lastName("Brown")
            .email("bob@example.com")
            .status(CustomerStatus.ACTIVE)
            .build();

        CustomerEntity activeCustomer2 = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Alice")
            .lastName("Johnson")
            .email("alice@example.com")
            .status(CustomerStatus.ACTIVE)
            .build();

        List<CustomerEntity> activeCustomers = Arrays.asList(activeCustomer1, activeCustomer2);
        when(customerReadRepository.findByStatus(eq(CustomerStatus.ACTIVE))).thenReturn(activeCustomers);

        // When
        List<CustomerEntity> result = customerQueryService.findByStatus(CustomerStatus.ACTIVE);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getStatus() == CustomerStatus.ACTIVE));

        verify(customerReadRepository, times(1)).findByStatus(eq(CustomerStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should search customers by email")
    void shouldSearchCustomersByEmail() {
        // Given
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Test")
            .lastName("User")
            .email("test.user@example.com")
            .status(CustomerStatus.ACTIVE)
            .build();

        List<CustomerEntity> customers = Arrays.asList(customer);
        when(customerReadRepository.findByEmailContainingIgnoreCase(eq("test.user"))).thenReturn(customers);

        // When
        List<CustomerEntity> result = customerQueryService.searchByEmail("test.user");

        // Then
        assertEquals(1, result.size());
        assertEquals("test.user@example.com", result.get(0).getEmail());

        verify(customerReadRepository, times(1)).findByEmailContainingIgnoreCase(eq("test.user"));
    }

    @Test
    @DisplayName("Should count customers by status")
    void shouldCountCustomersByStatus() {
        // Given
        when(customerReadRepository.countByStatus(eq(CustomerStatus.ACTIVE))).thenReturn(10L);
        when(customerReadRepository.countByStatus(eq(CustomerStatus.INACTIVE))).thenReturn(3L);

        // When
        long activeCount = customerQueryService.countByStatus(CustomerStatus.ACTIVE);
        long inactiveCount = customerQueryService.countByStatus(CustomerStatus.INACTIVE);

        // Then
        assertEquals(10, activeCount);
        assertEquals(3, inactiveCount);

        verify(customerReadRepository, times(1)).countByStatus(eq(CustomerStatus.ACTIVE));
        verify(customerReadRepository, times(1)).countByStatus(eq(CustomerStatus.INACTIVE));
    }

    @Test
    @DisplayName("Should get total customer count")
    void shouldGetTotalCustomerCount() {
        // Given
        when(customerReadRepository.count()).thenReturn(25L);

        // When
        long total = customerQueryService.getTotalCount();

        // Then
        assertEquals(25, total);

        verify(customerReadRepository, times(1)).count();
    }

    @Test
    @DisplayName("Should return true when customer exists")
    void shouldReturnTrueWhenCustomerExists() {
        // Given
        UUID customerId = UUID.randomUUID();
        when(customerReadRepository.existsById(eq(customerId))).thenReturn(true);

        // When
        boolean exists = customerQueryService.existsById(customerId);

        // Then
        assertTrue(exists);

        verify(customerReadRepository, times(1)).existsById(eq(customerId));
    }

    @Test
    @DisplayName("Should return false when customer does not exist")
    void shouldReturnFalseWhenCustomerDoesNotExist() {
        // Given
        UUID customerId = UUID.randomUUID();
        when(customerReadRepository.existsById(eq(customerId))).thenReturn(false);

        // When
        boolean exists = customerQueryService.existsById(customerId);

        // Then
        assertFalse(exists);

        verify(customerReadRepository, times(1)).existsById(eq(customerId));
    }
}
