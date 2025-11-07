package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateCustomerUseCase
 */
class CreateCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CreateCustomerUseCase createCustomerUseCase;

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
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomerWithValidData() {
        // Given
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phone("+1234567890")
            .pesel("12345678901")
            .nip("PL1234567890")
            .status(CustomerStatus.ACTIVE)
            .build();

        CustomerEntity savedCustomer = CustomerEntity.builder()
            .id(java.util.UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phone("+1234567890")
            .pesel("12345678901")
            .nip("PL1234567890")
            .status(CustomerStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(savedCustomer);

        // When
        CustomerEntity result = createCustomerUseCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals(CustomerStatus.ACTIVE, result.getStatus());

        verify(customerRepository, times(1)).save(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("Should create customer with default PENDING status")
    void shouldCreateCustomerWithDefaultPendingStatus() {
        // Given
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .build();

        CustomerEntity savedCustomer = CustomerEntity.builder()
            .id(java.util.UUID.randomUUID())
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .status(CustomerStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(savedCustomer);

        // When
        CustomerEntity result = createCustomerUseCase.execute(command);

        // Then
        assertNotNull(result);
        assertEquals(CustomerStatus.PENDING, result.getStatus());
        verify(customerRepository, times(1)).save(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("Should set audit fields on create")
    void shouldSetAuditFieldsOnCreate() {
        // Given
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .firstName("Bob")
            .lastName("Johnson")
            .email("bob.johnson@example.com")
            .build();

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        when(customerRepository.save(captor.capture())).thenReturn(CustomerEntity.builder().build());

        // When
        createCustomerUseCase.execute(command);

        // Then
        CustomerEntity captured = captor.getValue();
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        assertEquals("test-user", captured.getCreatedBy());
    }
}
