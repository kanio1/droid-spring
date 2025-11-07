package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.CreateAddressCommand;
import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
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
 * Test for CreateAddressUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAddressUseCase Application Layer")
class CreateAddressUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CreateAddressUseCase createAddressUseCase;

    @Test
    @DisplayName("Should create address successfully with all required fields")
    void shouldCreateAddressSuccessfully() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "BILLING",
            "Main Street 123",
            "123",
            "45",
            "00-123",
            "Warsaw",
            "Mazovia",
            "PL",
            52.2297,
            21.0122,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.BILLING);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.street()).isEqualTo("Main Street 123");
        assertThat(result.city()).isEqualTo("Warsaw");
        assertThat(result.postalCode()).isEqualTo("00-123");
        assertThat(result.country()).isEqualTo("PL");
        assertThat(result.type()).isEqualTo("BILLING");
        assertThat(result.isPrimary()).isFalse();

        verify(customerRepository).existsById(eq(customerIdUuid));
        verify(addressRepository).save(argThat(addr ->
            addr.getCustomerId().equals(customerId) &&
            addr.getType() == AddressType.BILLING &&
            addr.getStreet().equals("Main Street 123")
        ));
    }

    @Test
    @DisplayName("Should create address without optional fields")
    void shouldCreateAddressWithoutOptionalFields() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "SERVICE",
            "Oak Street",
            null,
            null,
            "10-001",
            "Krakow",
            null,
            "PL",
            null,
            null,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.SERVICE);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.street()).isEqualTo("Oak Street");
        assertThat(result.houseNumber()).isNull();
        assertThat(result.apartmentNumber()).isNull();
        assertThat(result.region()).isNull();
        assertThat(result.latitude()).isNull();
        assertThat(result.longitude()).isNull();

        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("Should create address with BILLING type")
    void shouldCreateAddressWithBillingType() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "BILLING",
            "Billing Street",
            "100",
            null,
            "20-001",
            "Lublin",
            "Lublin Voivodeship",
            "PL",
            51.2465,
            22.5684,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.BILLING);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("BILLING");
        assertThat(result.isBillingAddress()).isTrue();

        verify(addressRepository).save(argThat(addr -> addr.getType() == AddressType.BILLING));
    }

    @Test
    @DisplayName("Should create address with SHIPPING type")
    void shouldCreateAddressWithShippingType() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "SHIPPING",
            "Shipping Avenue",
            "50",
            "10",
            "30-001",
            "Krakow",
            "Lesser Poland",
            "PL",
            50.0647,
            19.9450,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.SHIPPING)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.SHIPPING);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("SHIPPING");
        assertThat(result.isShippingAddress()).isTrue();

        verify(addressRepository).save(argThat(addr -> addr.getType() == AddressType.SHIPPING));
    }

    @Test
    @DisplayName("Should create address with SERVICE type")
    void shouldCreateAddressWithServiceType() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "SERVICE",
            "Service Road",
            "200",
            null,
            "50-001",
            "Wroclaw",
            "Lower Silesia",
            "PL",
            51.1079,
            17.0385,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.SERVICE);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("SERVICE");
        assertThat(result.isServiceAddress()).isTrue();

        verify(addressRepository).save(argThat(addr -> addr.getType() == AddressType.SERVICE));
    }

    @Test
    @DisplayName("Should create address with CORRESPONDENCE type")
    void shouldCreateAddressWithCorrespondenceType() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "CORRESPONDENCE",
            "Mail Street",
            "75",
            "5",
            "60-001",
            "Poznan",
            "Greater Poland",
            "PL",
            52.4064,
            16.9252,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.CORRESPONDENCE)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.CORRESPONDENCE);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("CORRESPONDENCE");
        assertThat(result.isCorrespondenceAddress()).isTrue();

        verify(addressRepository).save(argThat(addr -> addr.getType() == AddressType.CORRESPONDENCE));
    }

    @Test
    @DisplayName("Should create primary address")
    void shouldCreatePrimaryAddress() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "BILLING",
            "Primary Street",
            "10",
            null,
            "00-123",
            "Warsaw",
            null,
            "PL",
            null,
            null,
            true
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());

        Address savedAddress = createPrimaryAddress(customerId, AddressType.BILLING);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isTrue();

        verify(addressRepository).save(argThat(Address::isPrimary));
    }

    @Test
    @DisplayName("Should create address with Poland country")
    void shouldCreateAddressWithPolandCountry() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "BILLING",
            "Warsaw Street",
            "1",
            null,
            "00-001",
            "Warsaw",
            "Mazovia",
            "PL",
            52.2297,
            21.0122,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());

        Address savedAddress = createTestAddress(customerId, AddressType.BILLING);
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("PL");
        assertThat(result.country()).isEqualTo("PL");

        verify(addressRepository).save(argThat(addr -> addr.getCountry() == Country.PL));
    }

    @Test
    @DisplayName("Should create address with Germany country")
    void shouldCreateAddressWithGermanyCountry() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "SHIPPING",
            "Berlin Avenue",
            "100",
            null,
            "10115",
            "Berlin",
            null,
            "DE",
            52.5200,
            13.4050,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.SHIPPING)))
            .thenReturn(Optional.empty());

        Address savedAddress = Address.create(
            customerId,
            AddressType.SHIPPING,
            "Berlin Avenue",
            "100",
            null,
            "10115",
            "Berlin",
            null,
            Country.DE,
            52.5200,
            13.4050,
            false,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("DE");

        verify(addressRepository).save(argThat(addr -> addr.getCountry() == Country.DE));
    }

    @Test
    @DisplayName("Should create address with coordinates")
    void shouldCreateAddressWithCoordinates() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);
        Double latitude = 48.8566;
        Double longitude = 2.3522;

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "SERVICE",
            "Paris Street",
            "1",
            null,
            "75001",
            "Paris",
            null,
            "FR",
            latitude,
            longitude,
            false
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());

        Address savedAddress = Address.create(
            customerId,
            AddressType.SERVICE,
            "Paris Street",
            "1",
            null,
            "75001",
            "Paris",
            null,
            Country.FR,
            latitude,
            longitude,
            false,
            null
        );
        when(addressRepository.save(any(Address.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.latitude()).isEqualTo(latitude);
        assertThat(result.longitude()).isEqualTo(longitude);

        verify(addressRepository).save(argThat(addr ->
            addr.getLatitude() != null && addr.getLongitude() != null));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UUID customerIdUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        CreateAddressCommand command = new CreateAddressCommand(
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

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> createAddressUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer not found: " + customerIdUuid);

        verify(customerRepository).existsById(eq(customerIdUuid));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("Should throw exception when primary address already exists")
    void shouldThrowExceptionWhenPrimaryAddressAlreadyExists() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        CreateAddressCommand command = new CreateAddressCommand(
            customerIdUuid.toString(),
            "BILLING",
            "New Street",
            "10",
            null,
            "00-123",
            "Warsaw",
            null,
            "PL",
            null,
            null,
            true
        );

        when(customerRepository.existsById(eq(customerIdUuid))).thenReturn(true);

        Address existingPrimaryAddress = createPrimaryAddress(customerId, AddressType.BILLING);
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerIdUuid), eq(AddressType.BILLING)))
            .thenReturn(Optional.of(existingPrimaryAddress));

        // Act & Assert
        assertThatThrownBy(() -> createAddressUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Customer already has a primary BILLING address");

        verify(addressRepository, never()).save(any(Address.class));
    }

    // Helper methods for test data
    private Address createTestAddress(CustomerId customerId, AddressType type) {
        return Address.create(
            customerId,
            type,
            "Test Street",
            "00-123",
            "Test City",
            Country.PL
        );
    }

    private Address createPrimaryAddress(CustomerId customerId, AddressType type) {
        return Address.create(
            customerId,
            type,
            "Primary Street",
            "00-123",
            "Primary City",
            Country.PL
        ).markAsPrimary();
    }
}
