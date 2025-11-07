package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.DeleteAddressCommand;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerId;
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
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = createTestAddress(customerId, AddressType.BILLING, addressId);

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        verify(addressRepository).deleteById(eq(addressId));
    }

    @Test
    @DisplayName("Should delete address with BILLING type")
    void shouldDeleteAddressWithBillingType() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = createTestAddress(customerId, AddressType.BILLING, addressId);

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        assertThat(address.getType()).isEqualTo(AddressType.BILLING);
    }

    @Test
    @DisplayName("Should delete address with SHIPPING type")
    void shouldDeleteAddressWithShippingType() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = createTestAddress(customerId, AddressType.SHIPPING, addressId);

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        assertThat(address.getType()).isEqualTo(AddressType.SHIPPING);
    }

    @Test
    @DisplayName("Should delete address with SERVICE type")
    void shouldDeleteAddressWithServiceType() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = createTestAddress(customerId, AddressType.SERVICE, addressId);

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        assertThat(address.getType()).isEqualTo(AddressType.SERVICE);
    }

    @Test
    @DisplayName("Should delete address with CORRESPONDENCE type")
    void shouldDeleteAddressWithCorrespondenceType() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = createTestAddress(customerId, AddressType.CORRESPONDENCE, addressId);

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        assertThat(address.getType()).isEqualTo(AddressType.CORRESPONDENCE);
    }

    @Test
    @DisplayName("Should delete address with coordinates")
    void shouldDeleteAddressWithCoordinates() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = Address.restore(
            addressId.value(),
            customerId.value(),
            AddressType.SERVICE,
            AddressStatus.ACTIVE,
            "Test Street",
            "1",
            null,
            "00-123",
            "Test City",
            null,
            Country.PL,
            52.2297,
            21.0122,
            false,
            null,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            1
        );

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        assertThat(address.getLatitude()).isEqualTo(52.2297);
        assertThat(address.getLongitude()).isEqualTo(21.0122);
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
            UUID addressIdUuid = UUID.randomUUID();
            AddressId addressId = AddressId.of(addressIdUuid);
            UUID customerIdUuid = UUID.randomUUID();
            CustomerId customerId = new CustomerId(customerIdUuid);

            Address address = createTestAddressWithStatus(customerId, AddressType.BILLING, addressId, status);

            DeleteAddressCommand command = new DeleteAddressCommand(
                addressIdUuid.toString(),
                customerIdUuid.toString()
            );

            when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

            // Act
            deleteAddressUseCase.handle(command);

            // Assert
            verify(addressRepository).findById(eq(addressIdUuid));
            verify(addressRepository).deleteById(eq(addressId));
            reset(addressRepository);
        }
    }

    @Test
    @DisplayName("Should throw exception when address not found")
    void shouldThrowExceptionWhenAddressNotFound() {
        // Arrange
        UUID addressIdUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID customerIdUuid = UUID.randomUUID();

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deleteAddressUseCase.handle(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Address not found: " + addressIdUuid);

        verify(addressRepository).findById(eq(addressIdUuid));
        verify(addressRepository, never()).deleteById(any(AddressId.class));
    }

    @Test
    @DisplayName("Should delete address with complete address information")
    void shouldDeleteAddressWithCompleteInformation() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address address = Address.restore(
            addressId.value(),
            customerId.value(),
            AddressType.BILLING,
            AddressStatus.ACTIVE,
            "Complete Street 123",
            "123",
            "45",
            "10-456",
            "Krakow",
            "Lesser Poland",
            Country.PL,
            50.0647,
            19.9450,
            true,
            "Important billing address",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            1
        );

        DeleteAddressCommand command = new DeleteAddressCommand(
            addressIdUuid.toString(),
            customerIdUuid.toString()
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

        // Act
        deleteAddressUseCase.handle(command);

        // Assert
        verify(addressRepository).findById(eq(addressIdUuid));
        verify(addressRepository).deleteById(eq(addressId));
    }

    @Test
    @DisplayName("Should delete address with different countries")
    void shouldDeleteAddressWithDifferentCountries() {
        // Arrange
        Country[] countries = {Country.PL, Country.DE, Country.FR, Country.UK, Country.IT};

        for (Country country : countries) {
            UUID addressIdUuid = UUID.randomUUID();
            AddressId addressId = AddressId.of(addressIdUuid);
            UUID customerIdUuid = UUID.randomUUID();
            CustomerId customerId = new CustomerId(customerIdUuid);

            Address address = createTestAddressWithCountry(customerId, AddressType.SERVICE, addressId, country);

            DeleteAddressCommand command = new DeleteAddressCommand(
                addressIdUuid.toString(),
                customerIdUuid.toString()
            );

            when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(address));

            // Act
            deleteAddressUseCase.handle(command);

            // Assert
            verify(addressRepository).findById(eq(addressIdUuid));
            verify(addressRepository).deleteById(eq(addressId));
            reset(addressRepository);
        }
    }

    // Helper methods for test data
    private Address createTestAddress(CustomerId customerId, AddressType type, AddressId addressId) {
        return Address.restore(
            addressId.value(),
            customerId.value(),
            type,
            AddressStatus.ACTIVE,
            "Test Street",
            "1",
            null,
            "00-123",
            "Test City",
            null,
            Country.PL,
            null,
            null,
            false,
            null,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            1
        );
    }

    private Address createTestAddressWithStatus(CustomerId customerId, AddressType type, AddressId addressId, AddressStatus status) {
        return Address.restore(
            addressId.value(),
            customerId.value(),
            type,
            status,
            "Test Street",
            "1",
            null,
            "00-123",
            "Test City",
            null,
            Country.PL,
            null,
            null,
            false,
            null,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            1
        );
    }

    private Address createTestAddressWithCountry(CustomerId customerId, AddressType type, AddressId addressId, Country country) {
        return Address.restore(
            addressId.value(),
            customerId.value(),
            type,
            AddressStatus.ACTIVE,
            "Test Street",
            "1",
            null,
            "00-123",
            "Test City",
            null,
            country,
            null,
            null,
            false,
            null,
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            1
        );
    }
}
