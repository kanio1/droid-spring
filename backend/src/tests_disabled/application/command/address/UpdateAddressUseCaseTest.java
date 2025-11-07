package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.UpdateAddressCommand;
import com.droid.bss.application.dto.address.AddressResponse;
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
 * Test for UpdateAddressUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateAddressUseCase Application Layer")
class UpdateAddressUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private UpdateAddressUseCase updateAddressUseCase;

    @Test
    @DisplayName("Should update address successfully")
    void shouldUpdateAddressSuccessfully() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.BILLING, addressId);

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "BILLING",
            "Updated Street 456",
            "456",
            "78",
            "10-456",
            "Krakow",
            "Lesser Poland",
            "PL",
            50.0647,
            19.9450,
            true
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateAddress(
            "Updated Street 456",
            "456",
            "78",
            "10-456",
            "Krakow",
            "Lesser Poland",
            Country.PL,
            50.0647,
            19.9450,
            "Updated notes"
        );
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.street()).isEqualTo("Updated Street 456");
        assertThat(result.houseNumber()).isEqualTo("456");
        assertThat(result.apartmentNumber()).isEqualTo("78");
        assertThat(result.postalCode()).isEqualTo("10-456");
        assertThat(result.city()).isEqualTo("Krakow");
        assertThat(result.region()).isEqualTo("Lesser Poland");
        assertThat(result.latitude()).isEqualTo(50.0647);
        assertThat(result.longitude()).isEqualTo(19.9450);

        verify(addressRepository).findById(eq(addressIdUuid));
        verify(addressRepository).save(argThat(addr ->
            addr.getStreet().equals("Updated Street 456") &&
            addr.getCity().equals("Krakow")
        ));
    }

    @Test
    @DisplayName("Should update address street")
    void shouldUpdateAddressStreet() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.SHIPPING, addressId);

        String newStreet = "New Main Street";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "SHIPPING",
            newStreet,
            "100",
            null,
            "20-001",
            "Lublin",
            null,
            "PL",
            null,
            null,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateAddress(
            newStreet,
            "100",
            null,
            "20-001",
            "Lublin",
            null,
            Country.PL,
            null,
            null,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.street()).isEqualTo(newStreet);

        verify(addressRepository).save(argThat(addr -> addr.getStreet().equals(newStreet)));
    }

    @Test
    @DisplayName("Should update address city")
    void shouldUpdateAddressCity() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.SERVICE, addressId);

        String newCity = "Gdansk";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "SERVICE",
            "Street",
            "1",
            null,
            "80-001",
            newCity,
            "Pomerania",
            "PL",
            null,
            null,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateAddress(
            "Street",
            "1",
            null,
            "80-001",
            newCity,
            "Pomerania",
            Country.PL,
            null,
            null,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.city()).isEqualTo(newCity);

        verify(addressRepository).save(argThat(addr -> addr.getCity().equals(newCity)));
    }

    @Test
    @DisplayName("Should update address postal code")
    void shouldUpdateAddressPostalCode() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.CORRESPONDENCE, addressId);

        String newPostalCode = "30-002";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "CORRESPONDENCE",
            "Mail Street",
            "50",
            "10",
            newPostalCode,
            "Krakow",
            null,
            "PL",
            null,
            null,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateAddress(
            "Mail Street",
            "50",
            "10",
            newPostalCode,
            "Krakow",
            null,
            Country.PL,
            null,
            null,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.postalCode()).isEqualTo(newPostalCode);

        verify(addressRepository).save(argThat(addr -> addr.getPostalCode().equals(newPostalCode)));
    }

    @Test
    @DisplayName("Should update coordinates")
    void shouldUpdateCoordinates() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.SERVICE, addressId);

        Double newLatitude = 52.2297;
        Double newLongitude = 21.0122;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "SERVICE",
            "Street",
            "1",
            null,
            "00-123",
            "Warsaw",
            null,
            "PL",
            newLatitude,
            newLongitude,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateCoordinates(newLatitude, newLongitude);
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.latitude()).isEqualTo(newLatitude);
        assertThat(result.longitude()).isEqualTo(newLongitude);

        verify(addressRepository).save(argThat(addr ->
            addr.getLatitude() != null && addr.getLongitude() != null));
    }

    @Test
    @DisplayName("Should update house and apartment numbers")
    void shouldUpdateHouseAndApartmentNumbers() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.BILLING, addressId);

        String newHouseNumber = "999";
        String newApartmentNumber = "88";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "BILLING",
            "Street",
            newHouseNumber,
            newApartmentNumber,
            "00-123",
            "Warsaw",
            null,
            "PL",
            null,
            null,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateAddress(
            "Street",
            newHouseNumber,
            newApartmentNumber,
            "00-123",
            "Warsaw",
            null,
            Country.PL,
            null,
            null,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.houseNumber()).isEqualTo(newHouseNumber);
        assertThat(result.apartmentNumber()).isEqualTo(newApartmentNumber);

        verify(addressRepository).save(argThat(addr ->
            addr.getHouseNumber().equals(newHouseNumber) &&
            addr.getApartmentNumber().equals(newApartmentNumber)));
    }

    @Test
    @DisplayName("Should update region")
    void shouldUpdateRegion() {
        // Arrange
        UUID addressIdUuid = UUID.randomUUID();
        AddressId addressId = AddressId.of(addressIdUuid);
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        Address existingAddress = createTestAddress(customerId, AddressType.SERVICE, addressId);

        String newRegion = "Lower Silesia";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "SERVICE",
            "Street",
            "1",
            null,
            "50-001",
            "Wroclaw",
            newRegion,
            "PL",
            null,
            null,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

        Address updatedAddress = existingAddress.updateAddress(
            "Street",
            "1",
            null,
            "50-001",
            "Wroclaw",
            newRegion,
            Country.PL,
            null,
            null,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.region()).isEqualTo(newRegion);

        verify(addressRepository).save(argThat(addr -> addr.getRegion().equals(newRegion)));
    }

    @Test
    @DisplayName("Should throw exception when address not found")
    void shouldThrowExceptionWhenAddressNotFound() {
        // Arrange
        UUID addressIdUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID customerIdUuid = UUID.randomUUID();

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressIdUuid.toString(),
            1L,
            customerIdUuid.toString(),
            "BILLING",
            "Street",
            "1",
            null,
            "00-123",
            "Warsaw",
            null,
            "PL",
            null,
            null,
            false
        );

        when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateAddressUseCase.handle(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Address not found: " + addressIdUuid);

        verify(addressRepository).findById(eq(addressIdUuid));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Should update all address types")
    void shouldUpdateAllAddressTypes() {
        // Arrange
        AddressType[] addressTypes = {
            AddressType.BILLING,
            AddressType.SHIPPING,
            AddressType.SERVICE,
            AddressType.CORRESPONDENCE
        };

        for (AddressType addressType : addressTypes) {
            UUID addressIdUuid = UUID.randomUUID();
            AddressId addressId = AddressId.of(addressIdUuid);
            UUID customerIdUuid = UUID.randomUUID();
            CustomerId customerId = new CustomerId(customerIdUuid);

            Address existingAddress = createTestAddress(customerId, addressType, addressId);

            UpdateAddressCommand command = new UpdateAddressCommand(
                addressIdUuid.toString(),
                1L,
                customerIdUuid.toString(),
                addressType.name(),
                "Street",
                "1",
                null,
                "00-123",
                "Warsaw",
                null,
                "PL",
                null,
                null,
                false
            );

            when(addressRepository.findById(eq(addressIdUuid))).thenReturn(Optional.of(existingAddress));

            Address updatedAddress = existingAddress.updateAddress(
                "Street",
                "1",
                null,
                "00-123",
                "Warsaw",
                null,
                Country.PL,
                null,
                null,
                null
            );
            when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);

            // Act
            AddressResponse result = updateAddressUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(addressType.name());

            verify(addressRepository).findById(eq(addressIdUuid));
            verify(addressRepository).save(any(Address.class));
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
}
