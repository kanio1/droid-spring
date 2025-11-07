package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.customer.CustomerStatus;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChangeCustomerStatusUseCase
 */
class ChangeCustomerStatusUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ChangeCustomerStatusUseCase changeCustomerStatusUseCase;

    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // Setup security context
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("test-user");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should change customer status successfully")
    void shouldChangeCustomerStatus() {
        // Given
        CustomerEntity existingCustomer = CustomerEntity.builder()
            .id(customerId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .status(CustomerStatus.PENDING)
            .createdAt(LocalDateTime.now().minusDays(10))
            .updatedAt(LocalDateTime.now().minusDays(10))
            .build();

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(existingCustomer));

        CustomerEntity updatedCustomer = CustomerEntity.builder()
            .id(customerId)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(existingCustomer.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .build();

        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(updatedCustomer);

        // When
        CustomerEntity result = changeCustomerStatusUseCase.execute(
            customerId, CustomerStatus.ACTIVE
        );

        // Then
        assertNotNull(result);
        assertEquals(CustomerStatus.ACTIVE, result.getStatus());
        assertEquals(customerId, result.getId());
        assertEquals("John", result.getFirstName());

        verify(customerRepository, times(1)).findById(eq(customerId));
        verify(customerRepository, times(1)).save(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(customerRepository.findById(eq(nonExistentId))).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            changeCustomerStatusUseCase.execute(nonExistentId, CustomerStatus.ACTIVE);
        });

        verify(customerRepository, times(1)).findById(eq(nonExistentId));
        verify(customerRepository, never()).save(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("Should update audit fields when changing status")
    void shouldUpdateAuditFields() {
        // Given
        CustomerEntity existingCustomer = CustomerEntity.builder()
            .id(customerId)
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .status(CustomerStatus.PENDING)
            .createdAt(LocalDateTime.now().minusDays(5))
            .updatedAt(LocalDateTime.now().minusDays(5))
            .build();

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(existingCustomer));

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        when(customerRepository.save(captor.capture())).thenReturn(CustomerEntity.builder().build());

        // When
        changeCustomerStatusUseCase.execute(customerId, CustomerStatus.SUSPENDED);

        // Then
        CustomerEntity captured = captor.getValue();
        assertEquals(CustomerStatus.SUSPENDED, captured.getStatus());
        assertNotNull(captured.getUpdatedAt());
        assertEquals("test-user", captured.getUpdatedBy());
    }

    @Test
    @DisplayName("Should handle all valid status transitions")
    void shouldHandleAllValidStatusTransitions() {
        // Given
        CustomerEntity customer = CustomerEntity.builder()
            .id(customerId)
            .firstName("Bob")
            .lastName("Johnson")
            .email("bob.johnson@example.com")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(CustomerEntity.builder().build());

        // When/Then - all transitions should work
        assertDoesNotThrow(() -> {
            changeCustomerStatusUseCase.execute(customerId, CustomerStatus.SUSPENDED);
            changeCustomerStatusUseCase.execute(customerId, CustomerStatus.ACTIVE);
            changeCustomerStatusUseCase.execute(customerId, CustomerStatus.INACTIVE);
        });

        verify(customerRepository, times(3)).save(any(CustomerEntity.class));
    }
}
