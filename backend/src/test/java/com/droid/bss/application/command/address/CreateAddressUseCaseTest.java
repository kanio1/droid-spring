package com.droid.bss.application.command.address;

import com.droid.bss.application.dto.address.CreateAddressCommand;
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
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

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

        verify(customerRepository).findById(eq(customerId));
        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address without optional fields")
    void shouldCreateAddressWithoutOptionalFields() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.SERVICE);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

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

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with BILLING type")
    void shouldCreateAddressWithBillingType() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("BILLING");
        assertThat(result.typeDisplayName()).isEqualTo("Billing Address");

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with SHIPPING type")
    void shouldCreateAddressWithShippingType() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.SHIPPING);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.SHIPPING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("SHIPPING");
        assertThat(result.typeDisplayName()).isEqualTo("Shipping Address");

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with SERVICE type")
    void shouldCreateAddressWithServiceType() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.SERVICE);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("SERVICE");
        assertThat(result.typeDisplayName()).isEqualTo("Service Address");

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with CORRESPONDENCE type")
    void shouldCreateAddressWithCorrespondenceType() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.CORRESPONDENCE);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.CORRESPONDENCE)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("CORRESPONDENCE");
        assertThat(result.typeDisplayName()).isEqualTo("Correspondence Address");

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create primary address")
    void shouldCreatePrimaryAddress() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);
        savedAddress.setIsPrimary(true);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isTrue();

        verify(addressRepository).save(argThat(addr -> addr.isPrimary()));
    }

    @Test
    @DisplayName("Should create address with Poland country")
    void shouldCreateAddressWithPolandCountry() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("PL");
        assertThat(result.countryDisplayName()).isEqualTo("Poland");

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with Germany country")
    void shouldCreateAddressWithGermanyCountry() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.SHIPPING);
        savedAddress.setCountry(Country.DE);
        savedAddress.setPostalCode("10115");
        savedAddress.setCity("Berlin");

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.SHIPPING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.country()).isEqualTo("DE");
        assertThat(result.countryDisplayName()).isEqualTo("Germany");

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with coordinates")
    void shouldCreateAddressWithCoordinates() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Double latitude = 48.8566;
        Double longitude = 2.3522;

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.SERVICE);
        savedAddress.setCountry(Country.FR);
        savedAddress.setPostalCode("75001");
        savedAddress.setCity("Paris");
        savedAddress.setLatitude(latitude);
        savedAddress.setLongitude(longitude);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

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
    @DisplayName("Should create address with notes")
    void shouldCreateAddressWithNotes() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
            "BILLING",
            "Notes Street",
            "5",
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
        AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);
        savedAddress.setNotes("Please deliver to reception desk");

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.notes()).isEqualTo("Please deliver to reception desk");

        verify(addressRepository).save(argThat(addr ->
            addr.getNotes() != null && !addr.getNotes().isEmpty()));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Arrange
        UUID customerId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        CreateAddressCommand command = new CreateAddressCommand(
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

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> createAddressUseCase.handle(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer not found: " + customerId);

        verify(customerRepository).findById(eq(customerId));
        verify(addressRepository, never()).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when primary address already exists")
    void shouldThrowExceptionWhenPrimaryAddressAlreadyExists() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
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

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity existingPrimaryAddress = createTestAddress(customer, AddressType.BILLING);
        existingPrimaryAddress.setIsPrimary(true);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.of(existingPrimaryAddress));

        // Act & Assert
        assertThatThrownBy(() -> createAddressUseCase.handle(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Customer already has a primary BILLING address");

        verify(addressRepository, never()).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should create address with all European countries")
    void shouldCreateAddressWithAllEuropeanCountries() {
        // Arrange
        String[] countries = {"FR", "DE", "ES", "IT", "UK", "NL", "SE", "NO", "DK", "FI"};

        for (String countryCode : countries) {
            UUID customerId = UUID.randomUUID();

            CreateAddressCommand command = new CreateAddressCommand(
                customerId.toString(),
                "SERVICE",
                "Street",
                "1",
                null,
                "00000",
                "City",
                null,
                countryCode,
                null,
                null,
                false
            );

            CustomerEntity customer = createTestCustomer(customerId);
            AddressEntity savedAddress = createTestAddress(customer, AddressType.SERVICE);

            Country expectedCountry = Country.valueOf(countryCode);
            savedAddress.setCountry(expectedCountry);

            when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
            when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
                eq(customerId), eq(AddressType.SERVICE)))
                .thenReturn(Optional.empty());
            when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

            // Act
            AddressResponse result = createAddressUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.country()).isEqualTo(countryCode);

            verify(customerRepository).findById(eq(customerId));
            verify(addressRepository).save(any(AddressEntity.class));
            reset(customerRepository);
            reset(addressRepository);
        }
    }

    @Test
    @DisplayName("Should create address with long street name")
    void shouldCreateAddressWithLongStreetName() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        String longStreetName = "Very Long Street Name That Exceeds Normal Length Limits";

        CreateAddressCommand command = new CreateAddressCommand(
            customerId.toString(),
            "SERVICE",
            longStreetName,
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
        AddressEntity savedAddress = createTestAddress(customer, AddressType.SERVICE);
        savedAddress.setStreet(longStreetName);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.SERVICE)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.street()).isEqualTo(longStreetName);

        verify(addressRepository).save(any(AddressEntity.class));
    }

    @Test
    @DisplayName("Should handle null isPrimary parameter")
    void shouldHandleNullIsPrimaryParameter() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        CreateAddressCommand command = new CreateAddressCommand(
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
            null
        );

        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);

        when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            eq(customerId), eq(AddressType.BILLING)))
            .thenReturn(Optional.empty());
        when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

        // Act
        AddressResponse result = createAddressUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isFalse();

        verify(addressRepository).save(argThat(addr -> !addr.isPrimary()));
    }

    @Test
    @DisplayName("Should create address with different postal codes")
    void shouldCreateAddressWithDifferentPostalCodes() {
        // Arrange
        String[] postalCodes = {"00-123", "10-456", "20-789", "30-012", "40-345"};

        for (String postalCode : postalCodes) {
            UUID customerId = UUID.randomUUID();

            CreateAddressCommand command = new CreateAddressCommand(
                customerId.toString(),
                "BILLING",
                "Street",
                "1",
                null,
                postalCode,
                "City",
                null,
                "PL",
                null,
                null,
                false
            );

            CustomerEntity customer = createTestCustomer(customerId);
            AddressEntity savedAddress = createTestAddress(customer, AddressType.BILLING);
            savedAddress.setPostalCode(postalCode);

            when(customerRepository.findById(eq(customerId))).thenReturn(Optional.of(customer));
            when(addressRepository.findByCustomerIdAndTypeAndIsPrimaryTrueAndDeletedAtIsNull(
                eq(customerId), eq(AddressType.BILLING)))
                .thenReturn(Optional.empty());
            when(addressRepository.save(any(AddressEntity.class))).thenReturn(savedAddress);

            // Act
            AddressResponse result = createAddressUseCase.handle(command);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.postalCode()).isEqualTo(postalCode);

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

    private AddressEntity createTestAddress(CustomerEntity customer, AddressType type) {
        AddressEntity address = new AddressEntity(
            customer,
            type,
            AddressStatus.ACTIVE,
            "Test Street",
            "00-123",
            "Test City",
            Country.PL
        );
        address.setId(UUID.randomUUID());
        address.setHouseNumber("1");
        address.setIsPrimary(false);
        return address;
    }
}
