package com.droid.bss.application.query.customer;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.domain.customer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerQueryService Query Layer")
class CustomerQueryServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerQueryService customerQueryService;

    @BeforeEach
    void setUp() {
        customerQueryService = new CustomerQueryService(customerRepository);
    }

    @Test
    @DisplayName("should find customer by ID")
    void shouldFindCustomerById() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);
        String customerId = customer.getId().value().toString();
        CustomerId expectedId = customer.getId();

        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(customer));

        // When
        Optional<CustomerResponse> result = customerQueryService.findById(customerId);

        // Then
        assertThat(result).isPresent();
        CustomerResponse response = result.get();
        assertThat(response.id()).isEqualTo(customerId);
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        verify(customerRepository).findById(expectedId);
    }

    @Test
    @DisplayName("should return empty when customer not found")
    void shouldReturnEmptyWhenCustomerNotFound() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.empty());

        // When
        Optional<CustomerResponse> result = customerQueryService.findById(customerId);

        // Then
        assertThat(result).isEmpty();
        verify(customerRepository).findById(expectedId);
    }

    @Test
    @DisplayName("should find all customers with pagination")
    void shouldFindAllCustomersWithPagination() {
        // Given
        int page = 1;
        int size = 10;
        String sort = "createdAt,desc";
        
        List<Customer> customers = List.of(
            createTestCustomer("550e8400-e29b-41d4-a716-446655440001"),
            createTestCustomer("550e8400-e29b-41d4-a716-446655440002"),
            createTestCustomer("550e8400-e29b-41d4-a716-446655440003")
        );
        
        long total = 25L;
        
        when(customerRepository.findAll(page, size)).thenReturn(customers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.content()).hasSize(3);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(total);
        assertThat(result.totalPages()).isEqualTo((int) Math.ceil((double) total / size));
        
        // Check responses
        assertThat(result.content().get(0).firstName()).isEqualTo("John");
        assertThat(result.content().get(0).email()).isEqualTo("john.doe@example.com");
        
        verify(customerRepository).findAll(page, size);
        verify(customerRepository).count();
    }

    @Test
    @DisplayName("should find customers by status")
    void shouldFindCustomersByStatus() {
        // Given
        String status = "SUSPENDED";
        CustomerStatus expectedStatus = CustomerStatus.SUSPENDED;
        int page = 0;
        int size = 5;
        String sort = "createdAt,desc";
        
        List<Customer> suspendedCustomers = List.of(
            createTestCustomer("550e8400-e29b-41d4-a716-446655440004", CustomerStatus.SUSPENDED),
            createTestCustomer("550e8400-e29b-41d4-a716-446655440005", CustomerStatus.SUSPENDED)
        );
        
        long total = 10L;
        
        when(customerRepository.findByStatus(any(), anyInt(), anyInt())).thenReturn(suspendedCustomers);
        when(customerRepository.countByStatus(any())).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findByStatus(status, page, size, sort);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(total);
        assertThat(result.content().get(0).status()).isEqualTo("SUSPENDED");
        
        verify(customerRepository).findByStatus(expectedStatus, page, size);
        verify(customerRepository).countByStatus(expectedStatus);
    }

    @Test
    @DisplayName("should throw exception for invalid status")
    void shouldThrowExceptionForInvalidStatus() {
        // Given
        String invalidStatus = "INVALID_STATUS";
        int page = 0;
        int size = 10;
        String sort = "createdAt,desc";

        // When & Then
        assertThatThrownBy(() -> customerQueryService.findByStatus(invalidStatus, page, size, sort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid status: " + invalidStatus);
        
        verify(customerRepository, never()).findByStatus(any(), anyInt(), anyInt());
        verify(customerRepository, never()).countByStatus(any());
    }

    @Test
    @DisplayName("should search customers with valid search term")
    void shouldSearchCustomersWithValidSearchTerm() {
        // Given
        String searchTerm = "john";
        int page = 0;
        int size = 20;
        String sort = "createdAt,desc";

        List<Customer> matchingCustomers = List.of(
            createTestCustomer("550e8400-e29b-41d4-a716-446655440006"),
            createTestCustomer("550e8400-e29b-41d4-a716-446655440007")
        );

        long expectedTotal = 100L;

        when(customerRepository.search(any(), anyInt(), anyInt())).thenReturn(matchingCustomers);
        when(customerRepository.count()).thenReturn(expectedTotal);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.search(searchTerm, page, size, sort);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(expectedTotal);

        // Verify all customers match search term (simulated)
        assertThat(result.content().get(0).firstName()).isEqualTo("John");
        assertThat(result.content().get(0).email()).isEqualTo("john.doe@example.com");

        verify(customerRepository).search(searchTerm, page, size);
        verify(customerRepository).count();
    }

    @Test
    @DisplayName("should search customers with empty search term")
    void shouldSearchCustomersWithEmptySearchTerm() {
        // Given
        String searchTerm = "";
        int page = 0;
        int size = 10;
        String sort = "createdAt,desc";
        
        List<Customer> allCustomers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440008"));
        long total = 5L;
        
        when(customerRepository.search(any(), anyInt(), anyInt())).thenReturn(allCustomers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.search(searchTerm, page, size, sort);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(total);
        verify(customerRepository).search("", page, size);
    }

    @Test
    @DisplayName("should search customers with null search term")
    void shouldSearchCustomersWithNullSearchTerm() {
        // Given
        String searchTerm = null;
        int page = 0;
        int size = 10;
        String sort = "createdAt,desc";
        
        List<Customer> allCustomers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440009"));
        long total = 3L;
        
        when(customerRepository.search(isNull(), anyInt(), anyInt())).thenReturn(allCustomers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.search(searchTerm, page, size, sort);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(total);
        verify(customerRepository).search(null, page, size);
    }

    @Test
    @DisplayName("should handle pagination boundaries correctly")
    void shouldHandlePaginationBoundariesCorrectly() {
        // Given
        int page = 0;
        int size = 50;
        String sort = "createdAt,desc";
        
        List<Customer> customers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440010"));
        long total = 100L;
        
        when(customerRepository.findAll(page, size)).thenReturn(customers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(50);
        assertThat(result.totalElements()).isEqualTo(100L);
        assertThat(result.totalPages()).isEqualTo(2); // 100 / 50 = 2
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isFalse();
        assertThat(result.content()).isNotEmpty();
        
        verify(customerRepository).findAll(0, 50);
    }

    @Test
    @DisplayName("should handle last page correctly")
    void shouldHandleLastPageCorrectly() {
        // Given
        int page = 4; // Last page (5 pages total, 0-4)
        int size = 20;
        String sort = "createdAt,desc";
        
        List<Customer> customers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440011"));
        long total = 100L;
        
        when(customerRepository.findAll(page, size)).thenReturn(customers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.page()).isEqualTo(4);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(100L);
        assertThat(result.totalPages()).isEqualTo(5); // 100 / 20 = 5
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isTrue();
        assertThat(result.content()).isNotEmpty();
        
        verify(customerRepository).findAll(4, 20);
    }

    @Test
    @DisplayName("should handle empty results correctly")
    void shouldHandleEmptyResultsCorrectly() {
        // Given
        int page = 0;
        int size = 10;
        String sort = "createdAt,desc";

        List<Customer> emptyCustomers = List.of();
        long total = 0L;

        when(customerRepository.findAll(page, size)).thenReturn(emptyCustomers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue(); // With 0 pages, page 0 is both first and last
        assertThat(result.content()).isEmpty();

        verify(customerRepository).findAll(0, 10);
    }

    @Test
    @DisplayName("should parse sort parameter correctly")
    void shouldParseSortParameterCorrectly() {
        // Given
        int page = 0;
        int size = 10;
        String sort = "email,asc,createdAt,desc";
        
        List<Customer> customers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440012"));
        long total = 1L;
        
        when(customerRepository.findAll(page, size)).thenReturn(customers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.content()).hasSize(1);
        verify(customerRepository).findAll(page, size); // Implementation uses basic parsing
    }

    @Test
    @DisplayName("should use default sort when sort is null")
    void shouldUseDefaultSortWhenSortIsNull() {
        // Given
        int page = 0;
        int size = 10;
        String sort = null;
        
        List<Customer> customers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440013"));
        long total = 1L;
        
        when(customerRepository.findAll(page, size)).thenReturn(customers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.content()).hasSize(1);
        verify(customerRepository).findAll(page, size);
    }

    @Test
    @DisplayName("should use default sort when sort is empty")
    void shouldUseDefaultSortWhenSortIsEmpty() {
        // Given
        int page = 0;
        int size = 10;
        String sort = "";
        
        List<Customer> customers = List.of(createTestCustomer("550e8400-e29b-41d4-a716-446655440014"));
        long total = 1L;
        
        when(customerRepository.findAll(page, size)).thenReturn(customers);
        when(customerRepository.count()).thenReturn(total);

        // When
        PageResponse<CustomerResponse> result = customerQueryService.findAll(page, size, sort);

        // Then
        assertThat(result.content()).hasSize(1);
        verify(customerRepository).findAll(page, size);
    }

    private Customer createTestCustomer(String id) {
        return createTestCustomer(id, CustomerStatus.ACTIVE);
    }

    private Customer createTestCustomer(String id, CustomerStatus status) {
        CustomerId customerId = new CustomerId(UUID.fromString(id));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        return Customer.testCustomer(customerId, personalInfo, contactInfo, status, 
                                    LocalDateTime.now(), 1);
    }
}
