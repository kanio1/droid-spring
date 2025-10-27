package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.*;
import com.droid.bss.application.dto.customer.UpdateCustomerCommand;
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
@DisplayName("UpdateCustomerUseCase Application Layer")
class UpdateCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private UpdateCustomerUseCase updateCustomerUseCase;

    @BeforeEach
    void setUp() {
        updateCustomerUseCase = new UpdateCustomerUseCase(customerRepository);
    }

    @Test
    @DisplayName("should update customer successfully")
    void shouldUpdateCustomerSuccessfully() {
        // Given
        String customerId = UUID.randomUUID().toString();
        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId, "Jane", "Smith", "98765432109", "0987654321", 
                "jane.smith@example.com", "+48987654321");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);
        
        CustomerInfo newInfo = command.toCustomerInfo();
        ContactInfo newContact = command.toContactInfo();
        Customer updatedCustomer = existingCustomer.updatePersonalInfo(newInfo)
                .updateContactInfo(newContact);
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);

        // When
        Customer result = updateCustomerUseCase.handle(command);

        // Then
        assertThat(result.getPersonalInfo().firstName()).isEqualTo("Jane");
        assertThat(result.getPersonalInfo().lastName()).isEqualTo("Smith");
        assertThat(result.getContactInfo().email()).isEqualTo("jane.smith@example.com");
        assertThat(result.getContactInfo().phone()).isEqualTo("+48987654321");
        
        verify(customerRepository).findById(expectedId);
        verify(customerRepository).save(argThat(customer -> 
            customer.getPersonalInfo().firstName().equals("Jane") &&
            customer.getContactInfo().email().equals("jane.smith@example.com")
        ));
    }

    @Test
    @DisplayName("should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        String customerId = UUID.randomUUID().toString();
        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId, "Jane", "Smith", "98765432109", "0987654321", 
                "jane.smith@example.com", "+48987654321");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> updateCustomerUseCase.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer not found with ID: " + expectedId);
        
        verify(customerRepository).findById(expectedId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw exception when trying to update terminated customer")
    void shouldThrowExceptionWhenTryingToUpdateTerminatedCustomer() {
        // Given
        String customerId = UUID.randomUUID().toString();
        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId, "Jane", "Smith", "98765432109", "0987654321", 
                "jane.smith@example.com", "+48987654321");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer terminatedCustomer = Customer.create(personalInfo, contactInfo).terminate();
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(terminatedCustomer));

        // When & Then
        assertThatThrownBy(() -> updateCustomerUseCase.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot modify terminated customer");
        
        verify(customerRepository).findById(expectedId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("should only update personal info")
    void shouldOnlyUpdatePersonalInfo() {
        // Given
        String customerId = UUID.randomUUID().toString();
        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId, "Jane", "Smith", "12345678901", "1234567890", 
                "john.doe@example.com", "+48123456789"); // Keep same contact info

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);
        
        Customer updatedCustomer = existingCustomer.updatePersonalInfo(command.toCustomerInfo());
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);

        // When
        Customer result = updateCustomerUseCase.handle(command);

        // Then
        assertThat(result.getPersonalInfo().firstName()).isEqualTo("Jane");
        assertThat(result.getContactInfo().email()).isEqualTo("john.doe@example.com"); // Unchanged
        verify(customerRepository).save(argThat(customer -> 
            customer.getPersonalInfo().firstName().equals("Jane") &&
            customer.getContactInfo().email().equals("john.doe@example.com")
        ));
    }

    @Test
    @DisplayName("should only update contact info")
    void shouldOnlyUpdateContactInfo() {
        // Given
        String customerId = UUID.randomUUID().toString();
        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customerId, "John", "Doe", "12345678901", "1234567890", 
                "jane.smith@example.com", "+48987654321"); // Keep same personal info

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);
        
        Customer updatedCustomer = existingCustomer.updateContactInfo(command.toContactInfo());
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);

        // When
        Customer result = updateCustomerUseCase.handle(command);

        // Then
        assertThat(result.getPersonalInfo().firstName()).isEqualTo("John"); // Unchanged
        assertThat(result.getContactInfo().email()).isEqualTo("jane.smith@example.com");
        verify(customerRepository).save(argThat(customer -> 
            customer.getPersonalInfo().firstName().equals("John") &&
            customer.getContactInfo().email().equals("jane.smith@example.com")
        ));
    }
}
