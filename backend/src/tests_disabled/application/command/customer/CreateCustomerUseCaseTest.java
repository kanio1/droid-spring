package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.*;
import com.droid.bss.domain.customer.event.CustomerEventPublisher;
import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.application.dto.customer.UpdateCustomerCommand;
import com.droid.bss.application.dto.customer.ChangeCustomerStatusCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCustomerUseCase Application Layer")
class CreateCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerEntityRepository customerEntityRepository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    private CreateCustomerUseCase createCustomerUseCase;

    @BeforeEach
    void setUp() {
        createCustomerUseCase = new CreateCustomerUseCase(
            customerRepository,
            customerEntityRepository,
            eventPublisher
        );
    }

    @Test
    @DisplayName("should create customer successfully")
    void shouldCreateCustomerSuccessfully() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "12345678901", "1234567890",
                "john.doe@example.com", "+48123456789");

        CustomerInfo expectedInfo = command.toCustomerInfo();
        ContactInfo expectedContact = command.toContactInfo();

        Customer savedCustomer = Customer.create(expectedInfo, expectedContact);
        when(customerRepository.existsByPesel(eq("12345678901"))).thenReturn(false);
        when(customerRepository.existsByNip(eq("1234567890"))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerEntityRepository.save(any(CustomerEntity.class))).thenReturn(any(CustomerEntity.class));

        // When
        CustomerId result = createCustomerUseCase.handle(command);

        // Then
        assertThat(result).isEqualTo(savedCustomer.getId());
        verify(customerRepository).existsByPesel("12345678901");
        verify(customerRepository).existsByNip("1234567890");
        verify(customerRepository).save(argThat(customer ->
            customer.getPersonalInfo().firstName().equals("John") &&
            customer.getContactInfo().email().equals("john.doe@example.com")
        ));
        verify(eventPublisher).publishCustomerCreated(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("should create customer without optional fields")
    void shouldCreateCustomerWithoutOptionalFields() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "Jane", "Doe", null, null,
                "jane.doe@example.com", null);

        Customer savedCustomer = Customer.create(command.toCustomerInfo(), command.toContactInfo());
        when(customerRepository.existsByPesel(isNull())).thenReturn(false);
        when(customerRepository.existsByNip(isNull())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerEntityRepository.save(any(CustomerEntity.class))).thenReturn(any(CustomerEntity.class));

        // When
        CustomerId result = createCustomerUseCase.handle(command);

        // Then
        assertThat(result).isEqualTo(savedCustomer.getId());
        verify(customerRepository).existsByPesel(null);
        verify(customerRepository).existsByNip(null);
        verify(eventPublisher).publishCustomerCreated(any(CustomerEntity.class));
    }

    @Test
    @DisplayName("should throw exception when PESEL already exists")
    void shouldThrowExceptionWhenPeselAlreadyExists() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "12345678901", "1234567890", 
                "john.doe@example.com", "+48123456789");
        
        when(customerRepository.existsByPesel(eq("12345678901"))).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> createCustomerUseCase.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer with PESEL 12345678901 already exists");
        
        verify(customerRepository).existsByPesel("12345678901");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw exception when NIP already exists")
    void shouldThrowExceptionWhenNipAlreadyExists() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "12345678901", "1234567890", 
                "john.doe@example.com", "+48123456789");
        
        when(customerRepository.existsByPesel(eq("12345678901"))).thenReturn(false);
        when(customerRepository.existsByNip(eq("1234567890"))).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> createCustomerUseCase.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer with NIP 1234567890 already exists");
        
        verify(customerRepository).existsByPesel("12345678901");
        verify(customerRepository).existsByNip("1234567890");
        verify(customerRepository, never()).save(any());
    }
}
