package com.droid.bss.domain.address;

import com.droid.bss.domain.customer.CustomerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Address - Aggregate Root Tests")
class AddressTest {

    @Test
    @DisplayName("should create address with minimal required fields")
    void shouldCreateAddressWithMinimalFields() {
        // Given
        CustomerId customerId = CustomerId.generate();
        AddressType type = AddressType.BILLING;
        String street = "Main Street";
        String postalCode = "00-001";
        String city = "Warsaw";
        Country country = Country.PL;

        // When
        Address address = Address.create(
            customerId,
            type,
            street,
            postalCode,
            city,
            country
        );

        // Then
        assertThat(address).isNotNull();
        assertThat(address.getId()).isNotNull();
        assertThat(address.getCustomerId()).isEqualTo(customerId);
        assertThat(address.getType()).isEqualTo(type);
        assertThat(address.getStatus()).isEqualTo(AddressStatus.ACTIVE);
        assertThat(address.getStreet()).isEqualTo(street);
        assertThat(address.getPostalCode()).isEqualTo(postalCode);
        assertThat(address.getCity()).isEqualTo(city);
        assertThat(address.getCountry()).isEqualTo(country);
        assertThat(address.isPrimary()).isFalse();
        assertThat(address.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("should create address with all fields")
    void shouldCreateAddressWithAllFields() {
        // Given
        CustomerId customerId = CustomerId.generate();
        AddressType type = AddressType.SHIPPING;
        String street = "Oak Avenue";
        String houseNumber = "123";
        String apartmentNumber = "45";
        String postalCode = "00-002";
        String city = "Krakow";
        String region = "Lesser Poland";
        Country country = Country.PL;
        Double latitude = 50.0647;
        Double longitude = 19.9450;
        boolean isPrimary = true;
        String notes = "Leave with doorman";

        // When
        Address address = Address.create(
            customerId,
            type,
            street,
            houseNumber,
            apartmentNumber,
            postalCode,
            city,
            region,
            country,
            latitude,
            longitude,
            isPrimary,
            notes
        );

        // Then
        assertThat(address).isNotNull();
        assertThat(address.getHouseNumber()).isEqualTo(houseNumber);
        assertThat(address.getApartmentNumber()).isEqualTo(apartmentNumber);
        assertThat(address.getRegion()).isEqualTo(region);
        assertThat(address.getLatitude()).isEqualTo(latitude);
        assertThat(address.getLongitude()).isEqualTo(longitude);
        assertThat(address.isPrimary()).isTrue();
        assertThat(address.getNotes()).isEqualTo(notes);
    }

    @Test
    @DisplayName("should create shipping address")
    void shouldCreateShippingAddress() {
        // Given
        CustomerId customerId = CustomerId.generate();
        String street = "Elm Street";
        String postalCode = "00-003";
        String city = "Gdansk";
        Country country = Country.PL;

        // When
        Address address = Address.create(
            customerId,
            AddressType.SHIPPING,
            street,
            postalCode,
            city,
            country
        );

        // Then
        assertThat(address.getType()).isEqualTo(AddressType.SHIPPING);
        assertThat(address.isShippingAddress()).isTrue();
        assertThat(address.isBillingAddress()).isFalse();
    }

    @Test
    @DisplayName("should create service address")
    void shouldCreateServiceAddress() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When
        Address address = Address.create(
            customerId,
            AddressType.SERVICE,
            "Service Road 5",
            "11-111",
            "Service City",
            Country.PL
        );

        // Then
        assertThat(address.getType()).isEqualTo(AddressType.SERVICE);
        assertThat(address.isServiceAddress()).isTrue();
    }

    @Test
    @DisplayName("should create correspondence address")
    void shouldCreateCorrespondenceAddress() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When
        Address address = Address.create(
            customerId,
            AddressType.CORRESPONDENCE,
            "Mail Street 1",
            "22-222",
            "Mail City",
            Country.PL
        );

        // Then
        assertThat(address.getType()).isEqualTo(AddressType.CORRESPONDENCE);
        assertThat(address.isCorrespondenceAddress()).isTrue();
    }

    @Test
    @DisplayName("should update address details")
    void shouldUpdateAddressDetails() {
        // Given
        Address address = createTestAddress();
        String newStreet = "New Street 99";
        String newHouseNumber = "456";
        String newPostalCode = "99-999";
        String newCity = "Wroclaw";
        String newNotes = "Updated address";

        // When
        Address updated = address.updateAddress(
            newStreet,
            newHouseNumber,
            null,
            newPostalCode,
            newCity,
            null,
            Country.PL,
            null,
            null,
            newNotes
        );

        // Then
        assertThat(updated.getStreet()).isEqualTo(newStreet);
        assertThat(updated.getHouseNumber()).isEqualTo(newHouseNumber);
        assertThat(updated.getPostalCode()).isEqualTo(newPostalCode);
        assertThat(updated.getCity()).isEqualTo(newCity);
        assertThat(updated.getNotes()).isEqualTo(newNotes);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should change address status from ACTIVE to INACTIVE")
    void shouldChangeStatusFromActiveToInactive() {
        // Given
        Address address = createTestAddress();

        // When
        Address inactive = address.changeStatus(AddressStatus.INACTIVE);

        // Then
        assertThat(inactive.getStatus()).isEqualTo(AddressStatus.INACTIVE);
        assertThat(inactive.isInactive()).isTrue();
        assertThat(inactive.isActive()).isFalse();
        assertThat(inactive.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should change address status from ACTIVE to PENDING")
    void shouldChangeStatusFromActiveToPending() {
        // Given
        Address address = createTestAddress();

        // When
        Address pending = address.changeStatus(AddressStatus.PENDING);

        // Then
        assertThat(pending.getStatus()).isEqualTo(AddressStatus.PENDING);
        assertThat(pending.isPending()).isTrue();
        assertThat(pending.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should change address status from INACTIVE to ACTIVE")
    void shouldChangeStatusFromInactiveToActive() {
        // Given
        Address address = createTestAddress().changeStatus(AddressStatus.INACTIVE);

        // When
        Address reactivated = address.changeStatus(AddressStatus.ACTIVE);

        // Then
        assertThat(reactivated.getStatus()).isEqualTo(AddressStatus.ACTIVE);
        assertThat(reactivated.isActive()).isTrue();
        assertThat(reactivated.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("should change address status from PENDING to ACTIVE")
    void shouldChangeStatusFromPendingToActive() {
        // Given
        Address address = createTestAddress().changeStatus(AddressStatus.PENDING);

        // When
        Address activated = address.changeStatus(AddressStatus.ACTIVE);

        // Then
        assertThat(activated.getStatus()).isEqualTo(AddressStatus.ACTIVE);
        assertThat(activated.isActive()).isTrue();
        assertThat(activated.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("should change address status from PENDING to INACTIVE")
    void shouldChangeStatusFromPendingToInactive() {
        // Given
        Address address = createTestAddress().changeStatus(AddressStatus.PENDING);

        // When
        Address deactivated = address.changeStatus(AddressStatus.INACTIVE);

        // Then
        assertThat(deactivated.getStatus()).isEqualTo(AddressStatus.INACTIVE);
        assertThat(deactivated.isInactive()).isTrue();
        assertThat(deactivated.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("should mark address as primary")
    void shouldMarkAddressAsPrimary() {
        // Given
        Address address = createTestAddress();

        // When
        Address primary = address.markAsPrimary();

        // Then
        assertThat(primary.isPrimary()).isTrue();
        assertThat(primary.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should unmark address as primary")
    void shouldUnmarkAddressAsPrimary() {
        // Given
        Address address = createPrimaryAddress();

        // When
        Address notPrimary = address.unmarkAsPrimary();

        // Then
        assertThat(notPrimary.isPrimary()).isFalse();
        assertThat(notPrimary.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update notes")
    void shouldUpdateNotes() {
        // Given
        Address address = createTestAddress();
        String newNotes = "New notes about this address";

        // When
        Address withNotes = address.updateNotes(newNotes);

        // Then
        assertThat(withNotes.getNotes()).isEqualTo(newNotes);
        assertThat(withNotes.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update coordinates")
    void shouldUpdateCoordinates() {
        // Given
        Address address = createTestAddress();
        Double newLatitude = 51.1079;
        Double newLongitude = 17.0385;

        // When
        Address withCoordinates = address.updateCoordinates(newLatitude, newLongitude);

        // Then
        assertThat(withCoordinates.getLatitude()).isEqualTo(newLatitude);
        assertThat(withCoordinates.getLongitude()).isEqualTo(newLongitude);
        assertThat(withCoordinates.hasCoordinates()).isTrue();
        assertThat(withCoordinates.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if address is active")
    void shouldCheckIfAddressIsActive() {
        // Given
        Address activeAddress = createTestAddress();
        Address inactiveAddress = createTestAddress().changeStatus(AddressStatus.INACTIVE);

        // Then
        assertThat(activeAddress.isActive()).isTrue();
        assertThat(activeAddress.isInactive()).isFalse();
        assertThat(inactiveAddress.isActive()).isFalse();
        assertThat(inactiveAddress.isInactive()).isTrue();
    }

    @Test
    @DisplayName("should check if address is pending")
    void shouldCheckIfAddressIsPending() {
        // Given
        Address pendingAddress = createTestAddress().changeStatus(AddressStatus.PENDING);

        // Then
        assertThat(pendingAddress.isPending()).isTrue();
        assertThat(pendingAddress.isActive()).isFalse();
    }

    @Test
    @DisplayName("should check if address can be modified")
    void shouldCheckIfAddressCanBeModified() {
        // Given
        Address activeAddress = createTestAddress();
        Address inactiveAddress = createTestAddress().changeStatus(AddressStatus.INACTIVE);
        Address pendingAddress = createTestAddress().changeStatus(AddressStatus.PENDING);

        // Then
        assertThat(activeAddress.canBeModified()).isTrue();
        assertThat(inactiveAddress.canBeModified()).isFalse();
        assertThat(pendingAddress.canBeModified()).isTrue();
    }

    @Test
    @DisplayName("should check if address can be deleted")
    void shouldCheckIfAddressCanBeDeleted() {
        // Given
        Address activeAddress = createTestAddress();
        Address inactiveAddress = createTestAddress().changeStatus(AddressStatus.INACTIVE);

        // Then
        assertThat(activeAddress.canBeDeleted()).isTrue();
        assertThat(inactiveAddress.canBeDeleted()).isFalse();
    }

    @Test
    @DisplayName("should format full address")
    void shouldFormatFullAddress() {
        // Given
        Address address = Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Main Street",
            "123",
            "45",
            "00-001",
            "Warsaw",
            "Masovia",
            Country.PL,
            52.2297,
            21.0122,
            false,
            "Test address"
        );

        // When
        String fullAddress = address.getFullAddress();

        // Then
        assertThat(fullAddress).contains("Main Street 123/45");
        assertThat(fullAddress).contains("00-001");
        assertThat(fullAddress).contains("Warsaw");
        assertThat(fullAddress).contains("Poland");
    }

    @Test
    @DisplayName("should format short address")
    void shouldFormatShortAddress() {
        // Given
        Address address = Address.create(
            CustomerId.generate(),
            AddressType.SHIPPING,
            "Oak Avenue",
            "456",
            null,
            "11-111",
            "Krakow",
            null,
            Country.PL,
            null,
            null,
            false,
            null
        );

        // When
        String shortAddress = address.getShortAddress();

        // Then
        assertThat(shortAddress).contains("Oak Avenue 456");
        assertThat(shortAddress).contains("11-111");
        assertThat(shortAddress).contains("Krakow");
        assertThat(shortAddress).doesNotContain("Poland");
    }

    @Test
    @DisplayName("should handle address without house number")
    void shouldHandleAddressWithoutHouseNumber() {
        // Given
        Address address = Address.create(
            CustomerId.generate(),
            AddressType.CORRESPONDENCE,
            "Park Lane",
            null,
            null,
            "33-333",
            "Lodz",
            null,
            Country.PL,
            null,
            null,
            false,
            null
        );

        // When
        String fullAddress = address.getFullAddress();
        String shortAddress = address.getShortAddress();

        // Then
        assertThat(fullAddress).contains("Park Lane");
        assertThat(shortAddress).contains("Park Lane");
    }

    @Test
    @DisplayName("should throw exception for null customer ID")
    void shouldThrowExceptionForNullCustomerId() {
        // When & Then
        assertThatThrownBy(() -> Address.create(
            null,
            AddressType.BILLING,
            "Street",
            "00-001",
            "City",
            Country.PL
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Customer ID cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null address type")
    void shouldThrowExceptionForNullAddressType() {
        // When & Then
        assertThatThrownBy(() -> Address.create(
            CustomerId.generate(),
            null,
            "Street",
            "00-001",
            "City",
            Country.PL
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Address type cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null street")
    void shouldThrowExceptionForNullStreet() {
        // When & Then
        assertThatThrownBy(() -> Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            null,
            "00-001",
            "City",
            Country.PL
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Street cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null postal code")
    void shouldThrowExceptionForNullPostalCode() {
        // When & Then
        assertThatThrownBy(() -> Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Street",
            null,
            "City",
            Country.PL
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Postal code cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null city")
    void shouldThrowExceptionForNullCity() {
        // When & Then
        assertThatThrownBy(() -> Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Street",
            "00-001",
            null,
            Country.PL
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("City cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null country")
    void shouldThrowExceptionForNullCountry() {
        // When & Then
        assertThatThrownBy(() -> Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Street",
            "00-001",
            "City",
            null
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Country cannot be null");
    }

    @Test
    @DisplayName("should throw exception for invalid status transition from INACTIVE")
    void shouldThrowExceptionForInvalidStatusTransitionFromInactive() {
        // Given
        Address address = createTestAddress().changeStatus(AddressStatus.INACTIVE);

        // When & Then
        assertThatThrownBy(() -> address.changeStatus(AddressStatus.PENDING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot change status from INACTIVE to PENDING");
    }

    @Test
    @DisplayName("should throw exception when updating inactive address")
    void shouldThrowExceptionWhenUpdatingInactiveAddress() {
        // Given
        Address address = createTestAddress().changeStatus(AddressStatus.INACTIVE);

        // When & Then
        assertThatThrownBy(() -> address.updateAddress(
            "New Street",
            "123",
            null,
            "99-999",
            "New City",
            null,
            Country.PL,
            null,
            null,
            "Notes"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Cannot modify inactive address");
    }

    @Test
    @DisplayName("should have correct version after multiple updates")
    void shouldHaveCorrectVersionAfterMultipleUpdates() {
        // Given
        Address address = createTestAddress();

        // When
        Address updated1 = address.changeStatus(AddressStatus.PENDING);
        Address updated2 = updated1.updateNotes("Note 1");
        Address updated3 = updated2.markAsPrimary();
        Address updated4 = updated3.changeStatus(AddressStatus.INACTIVE);

        // Then
        assertThat(address.getVersion()).isEqualTo(1);
        assertThat(updated1.getVersion()).isEqualTo(2);
        assertThat(updated2.getVersion()).isEqualTo(3);
        assertThat(updated3.getVersion()).isEqualTo(4);
        assertThat(updated4.getVersion()).isEqualTo(5);
    }

    @Test
    @DisplayName("should use default status ACTIVE when creating address")
    void shouldUseDefaultStatusActive() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When
        Address address = Address.create(
            customerId,
            AddressType.BILLING,
            "Street",
            "00-001",
            "City",
            Country.PL
        );

        // Then
        assertThat(address.getStatus()).isEqualTo(AddressStatus.ACTIVE);
    }

    @Test
    @DisplayName("should store created and updated timestamps")
    void shouldStoreCreatedAndUpdatedTimestamps() {
        // Given
        LocalDateTime before = LocalDateTime.now();

        // When
        Address address = Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Street",
            "00-001",
            "City",
            Country.PL
        );

        LocalDateTime after = LocalDateTime.now();

        // Then
        assertThat(address.getCreatedAt()).isBetween(before, after);
        assertThat(address.getUpdatedAt()).isEqualTo(address.getCreatedAt());
    }

    @Test
    @DisplayName("should update timestamp when modifying address")
    void shouldUpdateTimestampWhenModifyingAddress() {
        // Given
        Address address = Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Street",
            "00-001",
            "City",
            Country.PL
        );

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        // When
        Address updated = address.updateNotes("New notes");

        // Then
        assertThat(updated.getUpdatedAt()).isGreaterThan(address.getCreatedAt());
    }

    // Helper methods
    private Address createTestAddress() {
        return Address.create(
            CustomerId.generate(),
            AddressType.BILLING,
            "Test Street 123",
            "00-123",
            "Test City",
            Country.PL
        );
    }

    private Address createPrimaryAddress() {
        return Address.create(
            CustomerId.generate(),
            AddressType.SHIPPING,
            "Primary Street 1",
            "11-111",
            "Primary City",
            Country.PL
        ).markAsPrimary();
    }
}
