package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.DeleteAddressCommand;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for DeleteAddressUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAddressUseCase Application Layer")
class DeleteAddressUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private DeleteAddressUseCase deleteAddressUseCase;

    @Test
    @DisplayName("Should delete address successfully")
    void shouldDeleteAddressSuccessfully() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.BILLING);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressId));
        verify(addressRepository).save(argThat(addr -> addr.isDeleted()));
        assertThat(address.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should soft delete address")
    void shouldSoftDeleteAddress() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.SHIPPING);
        address.setIsPrimary(false);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).save(argThat(addr ->
            addr.getDeletedAt() != null &&
            !addr.isActive() &&
            addr.isDeleted()
        ));
    }

    @Test
    @DisplayName("Should delete address with BILLING type")
    void shouldDeleteAddressWithBillingType() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.BILLING);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressId));
        assertThat(address.getType()).isEqualTo(AddressType.BILLING);
    }

    @Test
    @DisplayName("Should delete address with SHIPPING type")
    void shouldDeleteAddressWithShippingType() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.SHIPPING);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressId));
        assertThat(address.getType()).isEqualTo(AddressType.SHIPPING);
    }

    @Test
    @DisplayName("Should delete address with SERVICE type")
    void shouldDeleteAddressWithServiceType() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.SERVICE);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressId));
        assertThat(address.getType()).isEqualTo(AddressType.SERVICE);
    }

    @Test
    @DisplayName("Should delete address with CORRESPONDENCE type")
    void shouldDeleteAddressWithCorrespondenceType() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.CORRESPONDENCE);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressId));
        assertThat(address.getType()).isEqualTo(AddressType.CORRESPONDENCE);
    }

    @Test
    @DisplayName("Should delete primary address")
    void shouldDeletePrimaryAddress() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.BILLING);
        address.setIsPrimary(true);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).save(argThat(addr ->
            addr.isDeleted() && !addr.isActive()
        ));
    }

    @Test
    @DisplayName("Should delete address with coordinates")
    void shouldDeleteAddressWithCoordinates() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = createTestAddress(addressId, customer, AddressType.SERVICE);
        address.setLatitude(52.2297);
        address.setLongitude(21.0122);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressId));
        assertThat(address.getLatitude()).isEqualTo(52.2297);
        assertThat(address.getLongitude()).isEqualTo(21.0122);
        assertThat(address.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should delete address with all address statuses")
    void shouldDeleteAddressWithAllStatuses() {
        // Arrange
        AddressStatus[] statuses = {
            AddressStatus.ACTIVE,
            AddressStatus.INACTIVE,
            AddressStatus.PENDING
        };

        for (AddressStatus status : statuses) {
            UUID addressId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            DeleteAddressCommand command = new DeleteAddressCommand(
                addressId.toString(),
                customerId.toString()
            );

            CustomerEntity customer = createTestCustomer(customerId);
            AddressEntity address = createTestAddress(addressId, customer, AddressType.BILLING);
            address.setStatus(status);

            when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
            when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

            // Act
            deleteAddressUseCase.handle(command);

            // Assert
            verify(addressRepository).findById(eq(addressId));
            verify(addressRepository).save(argThat(addr -> addr.isDeleted()));
            reset(addressRepository);
        }
    }

    @Test
    @DisplayName("Should throw exception when address not found")
    void shouldThrowExceptionWhenAddressNotFound() {
        // Arrange
        UUID addressId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deleteAddressUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Address not found: " + addressId);

        verify(addressRepository).findById(eq(addressId));
        verify(addressRepository, never()).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to delete already deleted address")
    void shouldThrowExceptionWhenDeletingAlreadyDeletedAddress() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity deletedAddress = createTestAddress(addressId, customer, AddressType.BILLING);
        deletedAddress.softDelete();

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(deletedAddress));

        // Act & Assert
        assertThatThrownBy(() -> deleteAddressUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Address is already deleted");

        verify(addressRepository).findById(eq(addressId));
        verify(addressRepository, never()).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should delete address with complete address information")
    void shouldDeleteAddressWithCompleteInformation() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressId.toString(),
            customerId.toString()
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = new AddressEntity(
            customer,
            AddressType.BILLING,
            AddressStatus.ACTIVE,
            "Complete Street 123",
            "123",
            "45",
            "10-456",
            "Krakow",
            "Lesser Poland",
            Country.PL
        );
        address.setId(addressId);
        address.setLatitude(50.0647);
        address.setLongitude(19.9450);
        address.setIsPrimary(true);
        address.setNotes("Important billing address");

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).save(argThat(addr ->
            addr.isDeleted() &&
            addr.getStreet().equals("Complete Street 123") &&
            addr.getHouseNumber().equals("123") &&
            addr.getApartmentNumber().equals("45") &&
            addr.getPostalCode().equals("10-456") &&
            addr.getCity().equals("Krakow")
        ));
    }

    @Test
    @DisplayName("Should delete address with different countries")
    void shouldDeleteAddressWithDifferentCountries() {
        // Arrange
        Country[] countries = {Country.PL, Country.DE, Country.FR, Country.UK, Country.IT};

        for (Country country : countries) {
            UUID addressId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            DeleteAddressCommand command = new DeleteAddressCommand(
                addressId.toString(),
                customerId.toString()
            );

            CustomerEntity customer = createTestCustomer(customerId);
            AddressEntity address = createTestAddress(addressId, customer, AddressType.SERVICE);
            address.setCountry(country);

            when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(address));
            when(addressRepository.save(any(AddressEntity.class))).thenReturn(address);

            // Act
            deleteAddressUseCase.handle(command);

            // Assert
            verify(addressRepository).findById(eq(addressId));
            verify(addressRepository).save(argThat(addr -> addr.isDeleted()));
            reset(addressRepository);
        }
    }

    // Helper methods for test data
    private CustomerEntity createTestCustomer(UUID customerId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        return customer;
    }

    private AddressEntity createTestAddress(UUID addressId, CustomerEntity customer, AddressType type) {
        AddressEntity address = new AddressEntity(
            customer,
            type,
            AddressStatus.ACTIVE,
            "Test Street",
            "00-123",
            "Test City",
            Country.PL
        );
        address.setId(addressId);
        address.setHouseNumber("1");
        address.setIsPrimary(false);
        return address;
    }
}
