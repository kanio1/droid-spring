package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.*;
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
@DisplayName("DeleteCustomerUseCase Application Layer")
class DeleteCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private DeleteCustomerUseCase deleteCustomerUseCase;

    @BeforeEach
    void setUp() {
        deleteCustomerUseCase = new DeleteCustomerUseCase(customerRepository);
    }

    @Test
    @DisplayName("should delete customer successfully")
    void shouldDeleteCustomerSuccessfully() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(true);

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should return false when customer not found")
    void shouldReturnFalseWhenCustomerNotFound() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(false);

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isFalse();
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should delete terminated customer")
    void shouldDeleteTerminatedCustomer() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer terminatedCustomer = Customer.create(personalInfo, contactInfo).terminate();
        
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(true);

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isTrue();
        assertThat(terminatedCustomer.canBeModified()).isFalse(); // Still terminated
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should handle active customer deletion")
    void shouldHandleActiveCustomerDeletion() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer activeCustomer = Customer.create(personalInfo, contactInfo);
        
        assertThat(activeCustomer.canBeModified()).isTrue(); // Active customer can be modified
        
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(true);

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should handle suspended customer deletion")
    void shouldHandleSuspendedCustomerDeletion() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer suspendedCustomer = Customer.create(personalInfo, contactInfo).suspend();
        
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(true);

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should handle inactive customer deletion")
    void shouldHandleInactiveCustomerDeletion() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer inactiveCustomer = Customer.create(personalInfo, contactInfo).deactivate();
        
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(true);

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isTrue();
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should return false when delete fails in repository")
    void shouldReturnFalseWhenDeleteFailsInRepository() {
        // Given
        String customerId = UUID.randomUUID().toString();
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        
        when(customerRepository.deleteById(eq(expectedId))).thenReturn(false); // Delete failed

        // When
        boolean result = deleteCustomerUseCase.handle(customerId);

        // Then
        assertThat(result).isFalse();
        verify(customerRepository).deleteById(expectedId);
    }

    @Test
    @DisplayName("should handle null customerId")
    void shouldHandleNullCustomerId() {
        // When & Then
        assertThatThrownBy(() -> deleteCustomerUseCase.handle(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer ID cannot be null or empty");
        
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("should handle empty customerId")
    void shouldHandleEmptyCustomerId() {
        // When & Then
        assertThatThrownBy(() -> deleteCustomerUseCase.handle(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer ID cannot be null or empty");
        
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("should handle invalid UUID format")
    void shouldHandleInvalidUuidFormat() {
        // Given
        String invalidId = "invalid-uuid-format";

        // When & Then
        assertThatThrownBy(() -> deleteCustomerUseCase.handle(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid customer ID format");
        
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).deleteById(any());
    }
}
