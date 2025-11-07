package com.droid.bss.application.dto.address;

import com.droid.bss.domain.address.Address;
import com.droid.bss.domain.address.AddressEntity;
import com.droid.bss.domain.address.AddressStatus;
import com.droid.bss.domain.address.AddressType;
import com.droid.bss.domain.address.Country;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for address data
 */
public record AddressResponse(
        String id,
        UUID customerId,
        String customerName,
        String type,
        String typeDisplayName,
        String status,
        String statusDisplayName,
        String street,
        String houseNumber,
        String apartmentNumber,
        String postalCode,
        String city,
        String region,
        String country,
        String countryDisplayName,
        Double latitude,
        Double longitude,
        boolean isPrimary,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version
) {

    /**
     * Create AddressResponse from AddressEntity
     */
    public static AddressResponse from(AddressEntity address) {
        return new AddressResponse(
                address.getId().toString(),
                address.getCustomer().getId(),
                getCustomerName(address.getCustomer()),
                address.getType().name(),
                address.getType().getDescription(),
                address.getStatus().name(),
                address.getStatus().getDescription(),
                address.getStreet(),
                address.getHouseNumber(),
                address.getApartmentNumber(),
                address.getPostalCode(),
                address.getCity(),
                address.getRegion(),
                address.getCountry().name(),
                address.getCountry().getName(),
                address.getLatitude(),
                address.getLongitude(),
                address.isPrimary(),
                address.getNotes(),
                address.getCreatedAt(),
                address.getUpdatedAt(),
                address.getVersion()
        );
    }

    /**
     * Create AddressResponse from Address aggregate
     */
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId().toString(),
                address.getCustomerId().value(),
                null, // Customer name would need to be fetched separately if needed
                address.getType().name(),
                address.getType().getDescription(),
                address.getStatus().name(),
                address.getStatus().getDescription(),
                address.getStreet(),
                address.getHouseNumber(),
                address.getApartmentNumber(),
                address.getPostalCode(),
                address.getCity(),
                address.getRegion(),
                address.getCountry().name(),
                address.getCountry().getName(),
                address.getLatitude(),
                address.getLongitude(),
                address.isPrimary(),
                address.getNotes(),
                address.getCreatedAt(),
                address.getUpdatedAt(),
                (long) address.getVersion()
        );
    }

    private static String getCustomerName(com.droid.bss.domain.customer.CustomerEntity customer) {
        if (customer == null) {
            return null;
        }
        // Assuming CustomerEntity has getFirstName() and getLastName()
        String firstName = customer.getFirstName();
        String lastName = customer.getLastName();

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return "Unknown Customer";
        }
    }

    /**
     * Get full formatted address
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            sb.append(street);
            if (houseNumber != null && !houseNumber.isEmpty()) {
                sb.append(" ").append(houseNumber);
            }
            if (apartmentNumber != null && !apartmentNumber.isEmpty()) {
                sb.append("/").append(apartmentNumber);
            }
        }

        if (postalCode != null && !postalCode.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(postalCode);
        }

        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(city);
        }

        if (countryDisplayName != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(countryDisplayName);
        }

        return sb.toString();
    }

    /**
     * Get short formatted address
     */
    public String getShortAddress() {
        StringBuilder sb = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            sb.append(street);
            if (houseNumber != null && !houseNumber.isEmpty()) {
                sb.append(" ").append(houseNumber);
            }
        }

        if (postalCode != null && !postalCode.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(postalCode);
        }

        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(city);
        }

        return sb.toString();
    }

    /**
     * Check if address is active
     */
    public boolean isActive() {
        return status.equals(AddressStatus.ACTIVE.name());
    }
}
