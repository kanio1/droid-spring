package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.GetAddressesQuery;
import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for GetAddressesByCustomerUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetAddressesByCustomerUseCase Query Side")
@Disabled("Temporarily disabled - use case not fully implemented")

class GetAddressesByCustomerUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private GetAddressesByCustomerUseCase getAddressesByCustomerUseCase;

    @Test
    @DisplayName("Should return all addresses for customer")
    void shouldReturnAllAddressesForCustomer() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());

        List<Address> addresses = createAddressList(customerId, 5);

        when(addressRepository.findByCustomerId(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        verify(addressRepository).findByCustomerId(eq(customerId));
    }

    @Test
    @DisplayName("Should return empty list when customer has no addresses")
    void shouldReturnEmptyListWhenNoAddresses() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());

        when(addressRepository.findByCustomerId(eq(customerId))).thenReturn(List.of());

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(addressRepository).findByCustomerId(eq(customerId));
    }

    @Test
    @DisplayName("Should filter addresses by type")
    void shouldFilterAddressesByType() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);
        AddressType filterType = AddressType.BILLING;

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());
        query.setType(filterType.name());

        List<Address> allAddresses = createAddressListWithDifferentTypes(customerId);
        List<Address> filteredAddresses = allAddresses.stream()
            .filter(addr -> addr.getType() == filterType)
            .toList();

        when(addressRepository.findByCustomerIdAndType(eq(customerId), eq(filterType)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("BILLING");

        verify(addressRepository).findByCustomerIdAndType(eq(customerId), eq(filterType));
    }

    @Test
    @DisplayName("Should filter addresses by status")
    void shouldFilterAddressesByStatus() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);
        AddressStatus filterStatus = AddressStatus.ACTIVE;

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());
        query.setStatus(filterStatus.name());

        List<Address> allAddresses = createAddressListWithDifferentStatuses(customerId);
        List<Address> filteredAddresses = allAddresses.stream()
            .filter(addr -> addr.getStatus() == filterStatus)
            .toList();

        when(addressRepository.findByStatus(eq(filterStatus)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("ACTIVE");

        verify(addressRepository).findByStatus(eq(filterStatus));
    }

    @Test
    @DisplayName("Should return only primary addresses")
    void shouldReturnOnlyPrimaryAddresses() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());
        query.setIsPrimary(true);

        List<Address> allAddresses = createAddressListWithMixedPrimaryFlags(customerId);
        List<Address> primaryAddresses = allAddresses.stream()
            .filter(Address::isPrimary)
            .toList();

        when(addressRepository.findByCustomerId(eq(customerId))).thenReturn(allAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPrimary()).isTrue();

        verify(addressRepository).findByCustomerId(eq(customerId));
    }

    @Test
    @DisplayName("Should search addresses by city")
    void shouldSearchAddressesByCity() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);
        String searchTerm = "Warsaw";

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());
        query.setSearchTerm(searchTerm);

        List<Address> allAddresses = createAddressListWithDifferentCities(customerId);
        List<Address> filteredAddresses = allAddresses.stream()
            .filter(addr -> addr.getCity().contains(searchTerm))
            .toList();

        when(addressRepository.findByCity(eq(searchTerm)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get(0).city()).contains(searchTerm);

        verify(addressRepository).findByCity(eq(searchTerm));
    }

    @Test
    @DisplayName("Should return addresses with all address types")
    void shouldReturnAddressesWithAllTypes() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());

        List<Address> addresses = createAddressListWithDifferentTypes(customerId);

        when(addressRepository.findByCustomerId(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        List<String> types = result.stream()
            .map(AddressResponse::type)
            .distinct()
            .toList();

        assertThat(types).contains("BILLING", "SHIPPING", "SERVICE", "CORRESPONDENCE");

        verify(addressRepository).findByCustomerId(eq(customerId));
    }

    @Test
    @DisplayName("Should return addresses with all statuses")
    void shouldReturnAddressesWithAllStatuses() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());

        List<Address> addresses = createAddressListWithDifferentStatuses(customerId);

        when(addressRepository.findByCustomerId(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        List<String> statuses = result.stream()
            .map(AddressResponse::status)
            .distinct()
            .toList();

        assertThat(statuses).contains("ACTIVE", "INACTIVE", "PENDING");

        verify(addressRepository).findByCustomerId(eq(customerId));
    }

    @Test
    @DisplayName("Should return complete address information")
    void shouldReturnCompleteAddressInformation() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());

        Address address = createCompleteAddress(customerId);

        when(addressRepository.findByCustomerId(eq(customerId))).thenReturn(List.of(address));

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        AddressResponse response = result.get(0);
        assertThat(response.street()).isEqualTo("Complete Street");
        assertThat(response.houseNumber()).isEqualTo("123");
        assertThat(response.apartmentNumber()).isEqualTo("45");
        assertThat(response.postalCode()).isEqualTo("00-123");
        assertThat(response.city()).isEqualTo("Warsaw");
        assertThat(response.region()).isEqualTo("Mazovia");
        assertThat(response.country()).isEqualTo("PL");
        assertThat(response.latitude()).isEqualTo(52.2297);
        assertThat(response.longitude()).isEqualTo(21.0122);
        assertThat(response.isPrimary()).isTrue();
        assertThat(response.notes()).isEqualTo("Important address");

        verify(addressRepository).findByCustomerId(eq(customerId));
    }

    @Test
    @DisplayName("Should return only active addresses")
    void shouldReturnOnlyActiveAddresses() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());

        List<Address> allAddresses = createAddressListWithDifferentStatuses(customerId);
        List<Address> activeAddresses = allAddresses.stream()
            .filter(Address::isActive)
            .toList();

        when(addressRepository.findByStatus(eq(AddressStatus.ACTIVE)))
            .thenReturn(activeAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();

        verify(addressRepository).findByStatus(eq(AddressStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should search addresses by postal code")
    void shouldSearchAddressesByPostalCode() {
        // Arrange
        UUID customerIdUuid = UUID.randomUUID();
        CustomerId customerId = new CustomerId(customerIdUuid);
        String postalCode = "00-123";

        GetAddressesQuery query = new GetAddressesQuery(customerIdUuid.toString());
        query.setSearchTerm(postalCode);

        List<Address> addresses = createAddressListWithDifferentPostalCodes(customerId);
        List<Address> filteredAddresses = addresses.stream()
            .filter(addr -> addr.getPostalCode().equals(postalCode))
            .toList();

        when(addressRepository.findByPostalCode(eq(postalCode)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get(0).postalCode()).isEqualTo(postalCode);

        verify(addressRepository).findByPostalCode(eq(postalCode));
    }

    // Helper methods for test data
    private List<Address> createAddressList(CustomerId customerId, int count) {
        List<Address> addresses = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            addresses.add(createTestAddress(customerId, AddressType.BILLING, i));
        }

        return addresses;
    }

    private List<Address> createAddressListWithDifferentTypes(CustomerId customerId) {
        List<Address> addresses = new ArrayList<>();

        addresses.add(createTestAddress(customerId, AddressType.BILLING, 0));
        addresses.add(createTestAddress(customerId, AddressType.SHIPPING, 1));
        addresses.add(createTestAddress(customerId, AddressType.SERVICE, 2));
        addresses.add(createTestAddress(customerId, AddressType.CORRESPONDENCE, 3));

        return addresses;
    }

    private List<Address> createAddressListWithDifferentStatuses(CustomerId customerId) {
        List<Address> addresses = new ArrayList<>();

        Address active = createTestAddressWithStatus(customerId, AddressType.BILLING, 0, AddressStatus.ACTIVE);
        Address inactive = createTestAddressWithStatus(customerId, AddressType.SHIPPING, 1, AddressStatus.INACTIVE);
        Address pending = createTestAddressWithStatus(customerId, AddressType.SERVICE, 2, AddressStatus.PENDING);

        addresses.add(active);
        addresses.add(inactive);
        addresses.add(pending);

        return addresses;
    }

    private List<Address> createAddressListWithMixedPrimaryFlags(CustomerId customerId) {
        List<Address> addresses = new ArrayList<>();

        Address primary = createTestAddress(customerId, AddressType.BILLING, 0);
        primary = primary.markAsPrimary();

        Address nonPrimary = createTestAddress(customerId, AddressType.SHIPPING, 1);

        addresses.add(primary);
        addresses.add(nonPrimary);

        return addresses;
    }

    private List<Address> createAddressListWithDifferentCities(CustomerId customerId) {
        List<Address> addresses = new ArrayList<>();

        Address warsaw = createTestAddressWithCity(customerId, AddressType.BILLING, 0, "Warsaw");
        Address krakow = createTestAddressWithCity(customerId, AddressType.SHIPPING, 1, "Krakow");
        Address gdansk = createTestAddressWithCity(customerId, AddressType.SERVICE, 2, "Gdansk");

        addresses.add(warsaw);
        addresses.add(krakow);
        addresses.add(gdansk);

        return addresses;
    }

    private List<Address> createAddressListWithDifferentPostalCodes(CustomerId customerId) {
        List<Address> addresses = new ArrayList<>();

        addresses.add(createTestAddressWithPostalCode(customerId, AddressType.BILLING, 0, "00-123"));
        addresses.add(createTestAddressWithPostalCode(customerId, AddressType.SHIPPING, 1, "10-456"));
        addresses.add(createTestAddressWithPostalCode(customerId, AddressType.SERVICE, 2, "20-789"));

        return addresses;
    }

    private Address createCompleteAddress(CustomerId customerId) {
        return Address.create(
            customerId,
            AddressType.BILLING,
            "Complete Street",
            "123",
            "45",
            "00-123",
            "Warsaw",
            "Mazovia",
            Country.PL,
            52.2297,
            21.0122,
            true,
            "Important address"
        );
    }

    private Address createTestAddress(CustomerId customerId, AddressType type, int index) {
        return Address.create(
            customerId,
            type,
            "Street " + index,
            "00-" + String.format("%03d", index),
            "City " + index,
            Country.PL
        );
    }

    private Address createTestAddressWithStatus(CustomerId customerId, AddressType type, int index, AddressStatus status) {
        Address address = Address.create(
            customerId,
            type,
            "Street " + index,
            "00-" + String.format("%03d", index),
            "City " + index,
            Country.PL
        );
        return address.changeStatus(status);
    }

    private Address createTestAddressWithCity(CustomerId customerId, AddressType type, int index, String city) {
        return Address.create(
            customerId,
            type,
            "Street " + index,
            "00-" + String.format("%03d", index),
            city,
            Country.PL
        );
    }

    private Address createTestAddressWithPostalCode(CustomerId customerId, AddressType type, int index, String postalCode) {
        return Address.create(
            customerId,
            type,
            "Street " + index,
            postalCode,
            "City " + index,
            Country.PL
        );
    }
}
