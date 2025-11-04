package com.droid.bss.application.command.service;

import com.droid.bss.application.dto.service.CreateServiceActivationCommand;
import com.droid.bss.application.dto.service.ServiceActivationResponse;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateServiceActivationUseCase
 */
@ExtendWith(MockitoExtension.class)
class CreateServiceActivationUseCaseTest {

    @Mock
    private ServiceActivationService service;

    @Mock
    private ServiceActivationRepository activationRepository;

    @Mock
    private ServiceActivationStepRepository stepRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private CreateServiceActivationUseCase useCase;

    @Test
    void handle_whenValidCommand_shouldCreateActivation() {
        // Arrange
        CustomerEntity customer = new CustomerEntity();
        customer.setId("customer-123");

        ServiceEntity serviceEntity = new ServiceEntity(
                "INTERNET-100M",
                "Internet 100 Mbps",
                null,
                ServiceType.INTERNET,
                ServiceStatus.ACTIVE,
                "BROADBAND"
        );

        CreateServiceActivationCommand command = new CreateServiceActivationCommand(
                "customer-123",
                "INTERNET-100M",
                null,
                null,
                "Test notes"
        );

        when(customerRepository.findById("customer-123")).thenReturn(Optional.of(customer));
        when(serviceRepository.findActiveByServiceCode("INTERNET-100M")).thenReturn(Optional.of(serviceEntity));
        when(service.checkActivationEligibility("customer-123", "INTERNET-100M"))
                .thenReturn(new ActivationEligibility(true, null));
        when(activationRepository.save(any(ServiceActivationEntity.class)))
                .thenAnswer(invocation -> {
                    ServiceActivationEntity saved = invocation.getArgument(0);
                    saved.setId("activation-123");
                    return saved;
                });

        // Act
        ServiceActivationResponse response = useCase.handle(command);

        // Assert
        assertNotNull(response);
        assertEquals("customer-123", response.customerId());
        assertEquals("INTERNET-100M", response.serviceCode());
        assertEquals("PENDING", response.status());
        assertEquals(0, response.retryCount());
        assertEquals(3, response.maxRetries());

        verify(customerRepository).findById("customer-123");
        verify(serviceRepository).findActiveByServiceCode("INTERNET-100M");
        verify(service).checkActivationEligibility("customer-123", "INTERNET-100M");
        verify(activationRepository).save(any(ServiceActivationEntity.class));
    }

    @Test
    void handle_whenCustomerNotFound_shouldThrowException() {
        // Arrange
        CreateServiceActivationCommand command = new CreateServiceActivationCommand(
                "invalid-customer",
                "INTERNET-100M",
                null,
                null,
                null
        );

        when(customerRepository.findById("invalid-customer")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> useCase.handle(command));
        verify(customerRepository).findById("invalid-customer");
    }

    @Test
    void handle_whenNotEligible_shouldThrowException() {
        // Arrange
        CustomerEntity customer = new CustomerEntity();
        customer.setId("customer-123");

        ServiceEntity serviceEntity = new ServiceEntity(
                "INTERNET-100M",
                "Internet 100 Mbps",
                null,
                ServiceType.INTERNET,
                ServiceStatus.ACTIVE,
                "BROADBAND"
        );

        CreateServiceActivationCommand command = new CreateServiceActivationCommand(
                "customer-123",
                "INTERNET-100M",
                null,
                null,
                null
        );

        when(customerRepository.findById("customer-123")).thenReturn(Optional.of(customer));
        when(serviceRepository.findActiveByServiceCode("INTERNET-100M")).thenReturn(Optional.of(serviceEntity));
        when(service.checkActivationEligibility("customer-123", "INTERNET-100M"))
                .thenReturn(new ActivationEligibility(false, "Service already active"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> useCase.handle(command));
    }
}
