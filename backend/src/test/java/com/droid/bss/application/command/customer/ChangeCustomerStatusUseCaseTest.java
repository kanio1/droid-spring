package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.*;
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
@DisplayName("ChangeCustomerStatusUseCase Application Layer")
class ChangeCustomerStatusUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private ChangeCustomerStatusUseCase changeCustomerStatusUseCase;

    @BeforeEach
    void setUp() {
        changeCustomerStatusUseCase = new ChangeCustomerStatusUseCase(customerRepository);
    }

    @Test
    @DisplayName("should change customer status successfully")
    void shouldChangeCustomerStatusSuccessfully() {
        // Given
        String customerId = UUID.randomUUID().toString();
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(customerId, "SUSPENDED");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);

        Customer suspendedCustomer = existingCustomer.suspend();

        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(suspendedCustomer);

        // When
        Customer result = changeCustomerStatusUseCase.handle(command);

        // Then
        assertThat(result.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        verify(customerRepository).findById(expectedId);
        verify(customerRepository).save(argThat(customer ->
            customer.getStatus() == CustomerStatus.SUSPENDED
        ));
    }

    @Test
    @DisplayName("should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        String customerId = UUID.randomUUID().toString();
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(customerId, "SUSPENDED");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> changeCustomerStatusUseCase.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer not found with ID: " + expectedId);
        
        verify(customerRepository).findById(expectedId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw exception when trying to change to same status")
    void shouldThrowExceptionWhenTryingToChangeToSameStatus() {
        // Given
        String customerId = UUID.randomUUID().toString();
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(customerId, "ACTIVE");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer activeCustomer = Customer.create(personalInfo, contactInfo);
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(activeCustomer));

        // When & Then
        assertThatThrownBy(() -> changeCustomerStatusUseCase.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot change status from ACTIVE to ACTIVE");
        
        verify(customerRepository).findById(expectedId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("should change status to TERMINATED and prevent further modifications")
    void shouldChangeStatusToTerminatedAndPreventFurtherModifications() {
        // Given
        String customerId = UUID.randomUUID().toString();
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(customerId, "TERMINATED");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);
        
        Customer terminatedCustomer = existingCustomer.terminate();
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(terminatedCustomer);

        // When
        Customer result = changeCustomerStatusUseCase.handle(command);

        // Then
        assertThat(result.getStatus()).isEqualTo(CustomerStatus.TERMINATED);
        assertThat(result.canBeModified()).isFalse();
        verify(customerRepository).findById(expectedId);
        verify(customerRepository).save(argThat(customer -> 
            customer.getStatus() == CustomerStatus.TERMINATED &&
            !customer.canBeModified()
        ));
    }

    @Test
    @DisplayName("should activate inactive customer")
    void shouldActivateInactiveCustomer() {
        // Given
        String customerId = UUID.randomUUID().toString();
        ChangeCustomerStatusCommand command = new ChangeCustomerStatusCommand(customerId, "ACTIVE");

        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer activeCustomer = Customer.create(personalInfo, contactInfo).deactivate();
        
        Customer reactivatedCustomer = activeCustomer.reactivate();
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(activeCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(reactivatedCustomer);

        // When
        Customer result = changeCustomerStatusUseCase.handle(command);

        // Then
        assertThat(result.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(result.isActive()).isTrue();
        verify(customerRepository).findById(expectedId);
        verify(customerRepository).save(argThat(customer -> 
            customer.getStatus() == CustomerStatus.ACTIVE &&
            customer.isActive()
        ));
    }

    @Test
    @DisplayName("should handle multiple status transitions")
    void shouldHandleMultipleStatusTransitions() {
        // Given
        String customerId = UUID.randomUUID().toString();
        
        CustomerId expectedId = new CustomerId(UUID.fromString(customerId));
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer existingCustomer = Customer.create(personalInfo, contactInfo);
        
        // First: ACTIVE -> SUSPENDED
        ChangeCustomerStatusCommand suspendCommand = new ChangeCustomerStatusCommand(customerId, "SUSPENDED");
        Customer suspendedCustomer = existingCustomer.suspend();
        
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(suspendedCustomer);

        // When - First transition
        Customer result1 = changeCustomerStatusUseCase.handle(suspendCommand);

        // Then
        assertThat(result1.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        verify(customerRepository).save(argThat(customer -> customer.getStatus() == CustomerStatus.SUSPENDED));
        
        // Reset mocks for second transition
        reset(customerRepository);
        when(customerRepository.findById(eq(expectedId))).thenReturn(Optional.of(suspendedCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(existingCustomer);

        // When - Second transition: SUSPENDED -> ACTIVE
        ChangeCustomerStatusCommand activateCommand = new ChangeCustomerStatusCommand(customerId, "ACTIVE");
        Customer reactivatedCustomer = suspendedCustomer.reactivate();
        when(customerRepository.save(any(Customer.class))).thenReturn(reactivatedCustomer);
        
        Customer result2 = changeCustomerStatusUseCase.handle(activateCommand);

        // Then
        assertThat(result2.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        verify(customerRepository).save(argThat(customer -> customer.getStatus() == CustomerStatus.ACTIVE));
    }
}
