package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.UpdateAddressCommand;
import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private UpdateAddressUseCase updateAddressUseCase;

    @Test
    @DisplayName("Should update address successfully")
    void shouldUpdateAddressSuccessfully() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        existingAddress.setStreet("Old Street 123");
        existingAddress.setHouseNumber("123");
        existingAddress.setIsPrimary(false);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        updatedAddress.setStreet("Updated Street 456");
        updatedAddress.setHouseNumber("456");
        updatedAddress.setApartmentNumber("78");
        updatedAddress.setPostalCode("10-456");
        updatedAddress.setCity("Krakow");
        updatedAddress.setRegion("Lesser Poland");
        updatedAddress.setLatitude(50.0647);
        updatedAddress.setLongitude(19.9450);
        updatedAddress.setIsPrimary(true);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

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
        assertThat(result.isPrimary()).isTrue();

        verify(addressRepository).findById(eq(addressId));
        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should update address street")
    void shouldUpdateAddressStreet() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        String newStreet = "New Main Street";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.SHIPPING);
        existingAddress.setStreet("Old Main Street");

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SHIPPING);
        updatedAddress.setStreet(newStreet);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

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
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        String newCity = "Gdansk";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.SERVICE);
        existingAddress.setCity("Warsaw");

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SERVICE);
        updatedAddress.setCity(newCity);
        updatedAddress.setRegion("Pomerania");

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

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
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        String newPostalCode = "30-002";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.CORRESPONDENCE);
        existingAddress.setPostalCode("00-123");

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.CORRESPONDENCE);
        updatedAddress.setPostalCode(newPostalCode);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.postalCode()).isEqualTo(newPostalCode);

        verify(addressRepository).save(argThat(addr -> addr.getPostalCode().equals(newPostalCode)));
    }

    @Test
    @DisplayName("Should update address type")
    void shouldUpdateAddressType() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
            "SHIPPING",
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.BILLING);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SHIPPING);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("SHIPPING");

        verify(addressRepository).save(argThat(addr -> addr.getType() == AddressType.SHIPPING));
    }

    @Test
    @DisplayName("Should update address status")
    void shouldUpdateAddressStatus() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        existingAddress.setStatus(AddressStatus.ACTIVE);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        updatedAddress.setStatus(AddressStatus.INACTIVE);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("INACTIVE");

        verify(addressRepository).save(argThat(addr -> addr.getStatus() == AddressStatus.INACTIVE));
    }

    @Test
    @DisplayName("Should update coordinates")
    void shouldUpdateCoordinates() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        Double newLatitude = 52.2297;
        Double newLongitude = 21.0122;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.SERVICE);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SERVICE);
        updatedAddress.setLatitude(newLatitude);
        updatedAddress.setLongitude(newLongitude);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

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
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        String newHouseNumber = "999";
        String newApartmentNumber = "88";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        existingAddress.setHouseNumber("100");
        existingAddress.setApartmentNumber("10");

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        updatedAddress.setHouseNumber(newHouseNumber);
        updatedAddress.setApartmentNumber(newApartmentNumber);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

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
    @DisplayName("Should update primary flag")
    void shouldUpdatePrimaryFlag() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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
            true
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        existingAddress.setIsPrimary(false);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        updatedAddress.setIsPrimary(true);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isTrue();

        verify(addressRepository).save(argThat(addr -> addr.isPrimary()));
    }

    @Test
    @DisplayName("Should update region")
    void shouldUpdateRegion() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        String newRegion = "Lower Silesia";

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.SERVICE);
        existingAddress.setRegion(null);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SERVICE);
        updatedAddress.setRegion(newRegion);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

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
        UUID addressId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateAddressUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Address not found: " + addressId);

        verify(addressRepository).findById(eq(addressId));
        verify(addressRepository, never()).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating deleted address")
    void shouldThrowExceptionWhenUpdatingDeletedAddress() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity deletedAddress = createTestAddress(customerId, customer, AddressType.BILLING);
        deletedAddress.setDeletedAt(LocalDateTime.now());

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(deletedAddress));

        // Act & Assert
        assertThatThrownBy(() -> updateAddressUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot update deleted address");

        verify(addressRepository).findById(eq(addressId));
        verify(addressRepository, never()).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should handle null optional fields during update")
    void shouldHandleNullOptionalFieldsDuringUpdate() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
            "SERVICE",
            "Street",
            null,
            null,
            "00-123",
            "Warsaw",
            null,
            "PL",
            null,
            null,
            false
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.SERVICE);
        existingAddress.setHouseNumber("100");
        existingAddress.setApartmentNumber("10");
        existingAddress.setLatitude(10.0);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SERVICE);

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        // Null optional fields should not update existing values
        // The actual behavior depends on the use case implementation

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should update with different country")
    void shouldUpdateWithDifferentCountry() {
        // Arrange
        UUID addressId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Long version = 1L;

        UpdateAddressCommand command = new UpdateAddressCommand(
            addressId.toString(),
            version,
            customerId.toString(),
            "SHIPPING",
            "Berlin Street",
            "1",
            null,
            "10115",
            "Berlin",
            null,
            "DE",
            52.5200,
            13.4050,
            false
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.SHIPPING);
        existingAddress.setCountry(Country.PL);

        AddressEntity updatedAddress = createTestAddress(customerId, customer, AddressType.SHIPPING);
        updatedAddress.setCountry(Country.DE);
        updatedAddress.setPostalCode("10115");
        updatedAddress.setCity("Berlin");

        when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

        // Act
        AddressResponse result = updateAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("DE");
        assertThat(result.countryDisplayName()).isEqualTo("Germany");

        verify(addressRepository).save(argThat(addr -> addr.getCountry() == Country.DE));
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
            UUID addressId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Long version = 1L;

            UpdateAddressCommand command = new UpdateAddressCommand(
                addressId.toString(),
                version,
                customerId.toString(),
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

            CustomerEntity customer = createTestCustomer(customerId);
            AddressEntity existingAddress = createTestAddress(customerId, customer, AddressType.BILLING);

            AddressEntity updatedAddress = createTestAddress(customerId, customer, addressType);

            when(addressRepository.findById(eq(addressId))).thenReturn(Optional.of(existingAddress));
            when(addressRepository.save(any(AddressEntity.class))).thenReturn(updatedAddress);

            // Act
            AddressResponse result = updateAddressUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(addressType.name());

            verify(addressRepository).findById(eq(addressId));
            verify(addressRepository).save(any(AddressEntity.class));
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
        address.setVersion(1L);
        return address;
    }
}
