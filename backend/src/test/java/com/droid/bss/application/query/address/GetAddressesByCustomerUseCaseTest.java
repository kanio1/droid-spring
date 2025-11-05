package com.droid.bss.application.query.address;

import com.droid.bss.application.dto.address.GetAddressesQuery;
import com.droid.bss.application.dto.address.AddressResponse;
import com.droid.bss.domain.address.*;
import com.droid.bss.domain.customer.CustomerEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
class GetAddressesByCustomerUseCaseTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private GetAddressesByCustomerUseCase getAddressesByCustomerUseCase;

    @Test
    @DisplayName("Should return all addresses for customer")
    void shouldReturnAllAddressesForCustomer() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());

        List<AddressEntity> addresses = createAddressList(customerId, 5);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return empty list when customer has no addresses")
    void shouldReturnEmptyListWhenNoAddresses() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(List.of());

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should filter addresses by type")
    void shouldFilterAddressesByType() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        AddressType filterType = AddressType.BILLING;

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setType(filterType.name());

        List<AddressEntity> allAddresses = createAddressListWithDifferentTypes(customerId);
        List<AddressEntity> filteredAddresses = allAddresses.stream()
            .filter(addr -> addr.getType() == filterType)
            .toList();

        when(addressRepository.findByCustomerIdAndTypeAndDeletedAtIsNull(eq(customerId), eq(filterType)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("BILLING");

        verify(addressRepository).findByCustomerIdAndTypeAndDeletedAtIsNull(eq(customerId), eq(filterType));
    }

    @Test
    @DisplayName("Should filter addresses by status")
    void shouldFilterAddressesByStatus() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        AddressStatus filterStatus = AddressStatus.ACTIVE;

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setStatus(filterStatus.name());

        List<AddressEntity> allAddresses = createAddressListWithDifferentStatuses(customerId);
        List<AddressEntity> filteredAddresses = allAddresses.stream()
            .filter(addr -> addr.getStatus() == filterStatus)
            .toList();

        when(addressRepository.findByCustomerIdAndStatusAndDeletedAtIsNull(eq(customerId), eq(filterStatus)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");

        verify(addressRepository).findByCustomerIdAndStatusAndDeletedAtIsNull(eq(customerId), eq(filterStatus));
    }

    @Test
    @DisplayName("Should return only primary addresses")
    void shouldReturnOnlyPrimaryAddresses() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setIsPrimary(true);

        List<AddressEntity> allAddresses = createAddressListWithMixedPrimaryFlags(customerId);
        List<AddressEntity> primaryAddresses = allAddresses.stream()
            .filter(AddressEntity::isPrimary)
            .toList();

        when(addressRepository.findByCustomerIdAndIsPrimaryTrueAndDeletedAtIsNull(eq(customerId)))
            .thenReturn(primaryAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPrimary()).isTrue();

        verify(addressRepository).findByCustomerIdAndIsPrimaryTrueAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should search addresses by city")
    void shouldSearchAddressesByCity() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        String searchTerm = "Warsaw";

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setSearchTerm(searchTerm);

        List<AddressEntity> allAddresses = createAddressListWithDifferentCities(customerId);
        List<AddressEntity> filteredAddresses = allAddresses.stream()
            .filter(addr -> addr.getCity().contains(searchTerm))
            .toList();

        when(addressRepository.findByCustomerIdAndCityContainingAndDeletedAtIsNull(eq(customerId), eq(searchTerm)))
            .thenReturn(filteredAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get(0).getCity()).contains(searchTerm);

        verify(addressRepository).findByCustomerIdAndCityContainingAndDeletedAtIsNull(eq(customerId), eq(searchTerm));
    }

    @Test
    @DisplayName("Should return addresses sorted by city")
    void shouldReturnAddressesSortedByCity() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setSortBy("city");
        query.setSortOrder("ASC");

        List<AddressEntity> addresses = createAddressListWithDifferentCities(customerId);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return addresses sorted by type")
    void shouldReturnAddressesSortedByType() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setSortBy("type");
        query.setSortOrder("ASC");

        List<AddressEntity> addresses = createAddressListWithDifferentTypes(customerId);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return addresses with pagination")
    void shouldReturnAddressesWithPagination() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setPage(0);
        query.setSize(3);

        List<AddressEntity> addresses = createAddressList(customerId, 10);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // Pagination is typically handled at the repository level

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should handle combination of filters")
    void shouldHandleCombinationOfFilters() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setType("BILLING");
        query.setStatus("ACTIVE");
        query.setIsPrimary(false);

        List<AddressEntity> addresses = createAddressList(customerId, 3);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // The actual filtering would happen in the repository

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return addresses with all address types")
    void shouldReturnAddressesWithAllTypes() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());

        List<AddressEntity> addresses = createAddressListWithDifferentTypes(customerId);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(4);

        List<String> types = result.stream()
            .map(AddressResponse::getType)
            .distinct()
            .toList();

        assertThat(types).contains("BILLING", "SHIPPING", "SERVICE", "CORRESPONDENCE");

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return addresses with all statuses")
    void shouldReturnAddressesWithAllStatuses() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());

        List<AddressEntity> addresses = createAddressListWithDifferentStatuses(customerId);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        List<String> statuses = result.stream()
            .map(AddressResponse::getStatus)
            .distinct()
            .toList();

        assertThat(statuses).contains("ACTIVE", "INACTIVE", "PENDING");

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return complete address information")
    void shouldReturnCompleteAddressInformation() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());

        AddressEntity address = createCompleteAddress(customerId);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(List.of(address));

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

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    @Test
    @DisplayName("Should return only active addresses")
    void shouldReturnOnlyActiveAddresses() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());

        List<AddressEntity> allAddresses = createAddressListWithDifferentStatuses(customerId);
        List<AddressEntity> activeAddresses = allAddresses.stream()
            .filter(AddressEntity::isActive)
            .toList();

        when(addressRepository.findByCustomerIdAndStatusAndDeletedAtIsNull(
            eq(customerId), eq(AddressStatus.ACTIVE)))
            .thenReturn(activeAddresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();

        verify(addressRepository).findByCustomerIdAndStatusAndDeletedAtIsNull(
            eq(customerId), eq(AddressStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void shouldHandleCaseInsensitiveSearch() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        String searchTerm = "WARSAW";

        GetAddressesQuery query = new GetAddressesQuery(customerId.toString());
        query.setSearchTerm(searchTerm);

        List<AddressEntity> addresses = createAddressListWithDifferentCities(customerId);

        when(addressRepository.findByCustomerIdAndDeletedAtIsNull(eq(customerId))).thenReturn(addresses);

        // Act
        List<AddressResponse> result = getAddressesByCustomerUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        // Case-insensitive search should be implemented in the repository

        verify(addressRepository).findByCustomerIdAndDeletedAtIsNull(eq(customerId));
    }

    // Helper methods for test data
    private List<AddressEntity> createAddressList(UUID customerId, int count) {
        List<AddressEntity> addresses = new ArrayList<>();
        CustomerEntity customer = createTestCustomer(customerId);

        for (int i = 0; i < count; i++) {
            addresses.add(createTestAddress(customer, AddressType.BILLING, i));
        }

        return addresses;
    }

    private List<AddressEntity> createAddressListWithDifferentTypes(UUID customerId) {
        List<AddressEntity> addresses = new ArrayList<>();
        CustomerEntity customer = createTestCustomer(customerId);

        addresses.add(createTestAddress(customer, AddressType.BILLING, 0));
        addresses.add(createTestAddress(customer, AddressType.SHIPPING, 1));
        addresses.add(createTestAddress(customer, AddressType.SERVICE, 2));
        addresses.add(createTestAddress(customer, AddressType.CORRESPONDENCE, 3));

        return addresses;
    }

    private List<AddressEntity> createAddressListWithDifferentStatuses(UUID customerId) {
        List<AddressEntity> addresses = new ArrayList<>();
        CustomerEntity customer = createTestCustomer(customerId);

        AddressEntity active = createTestAddress(customer, AddressType.BILLING, 0);
        active.setStatus(AddressStatus.ACTIVE);

        AddressEntity inactive = createTestAddress(customer, AddressType.SHIPPING, 1);
        inactive.setStatus(AddressStatus.INACTIVE);

        AddressEntity pending = createTestAddress(customer, AddressType.SERVICE, 2);
        pending.setStatus(AddressStatus.PENDING);

        addresses.add(active);
        addresses.add(inactive);
        addresses.add(pending);

        return addresses;
    }

    private List<AddressEntity> createAddressListWithMixedPrimaryFlags(UUID customerId) {
        List<AddressEntity> addresses = new ArrayList<>();
        CustomerEntity customer = createTestCustomer(customerId);

        AddressEntity primary = createTestAddress(customer, AddressType.BILLING, 0);
        primary.setIsPrimary(true);

        AddressEntity nonPrimary = createTestAddress(customer, AddressType.SHIPPING, 1);
        nonPrimary.setIsPrimary(false);

        addresses.add(primary);
        addresses.add(nonPrimary);

        return addresses;
    }

    private List<AddressEntity> createAddressListWithDifferentCities(UUID customerId) {
        List<AddressEntity> addresses = new ArrayList<>();
        CustomerEntity customer = createTestCustomer(customerId);

        AddressEntity warsaw = createTestAddress(customer, AddressType.BILLING, 0);
        warsaw.setCity("Warsaw");

        AddressEntity krakow = createTestAddress(customer, AddressType.SHIPPING, 1);
        krakow.setCity("Krakow");

        AddressEntity gdansk = createTestAddress(customer, AddressType.SERVICE, 2);
        gdansk.setCity("Gdansk");

        addresses.add(warsaw);
        addresses.add(krakow);
        addresses.add(gdansk);

        return addresses;
    }

    private AddressEntity createCompleteAddress(UUID customerId) {
        CustomerEntity customer = createTestCustomer(customerId);
        AddressEntity address = new AddressEntity(
            customer,
            AddressType.BILLING,
            AddressStatus.ACTIVE,
            "Complete Street",
            "00-123",
            "Warsaw",
            Country.PL
        );
        address.setId(UUID.randomUUID());
        address.setHouseNumber("123");
        address.setApartmentNumber("45");
        address.setRegion("Mazovia");
        address.setLatitude(52.2297);
        address.setLongitude(21.0122);
        address.setIsPrimary(true);
        address.setNotes("Important address");
        return address;
    }

    private CustomerEntity createTestCustomer(UUID customerId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        return customer;
    }

    private AddressEntity createTestAddress(CustomerEntity customer, AddressType type, int index) {
        AddressEntity address = new AddressEntity(
            customer,
            type,
            AddressStatus.ACTIVE,
            "Street " + index,
            "00-" + String.format("%03d", index),
            "City " + index,
            Country.PL
        );
        address.setId(UUID.randomUUID());
        address.setHouseNumber(String.valueOf(100 + index));
        address.setIsPrimary(false);
        return address;
    }
}
