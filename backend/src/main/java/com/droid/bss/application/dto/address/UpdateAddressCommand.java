package com.droid.bss.application.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Command for updating an existing address
 */
public record UpdateAddressCommand(
        @NotBlank(message = "Address ID is required")
        String id,

        @NotNull(message = "Version is required")
        Long version,

        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotNull(message = "Address type is required")
        String type,

        @NotBlank(message = "Street is required")
        @Size(max = 255, message = "Street must not exceed 255 characters")
        String street,

        @Size(max = 20, message = "House number must not exceed 20 characters")
        String houseNumber,

        @Size(max = 20, message = "Apartment number must not exceed 20 characters")
        String apartmentNumber,

        @NotBlank(message = "Postal code is required")
        @Pattern(regexp = "^\\d{2}-\\d{3}$", message = "Postal code must be in format XX-XXX")
        String postalCode,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "Region must not exceed 100 characters")
        String region,

        @NotNull(message = "Country is required")
        String country,

        Double latitude,

        Double longitude,

        Boolean isPrimary
) {
    // Helper method to get optional fields
    public java.util.Optional<String> getHouseNumber() {
        return java.util.Optional.ofNullable(houseNumber);
    }

    public java.util.Optional<String> getApartmentNumber() {
        return java.util.Optional.ofNullable(apartmentNumber);
    }

    public java.util.Optional<String> getRegion() {
        return java.util.Optional.ofNullable(region);
    }

    public java.util.Optional<Double> getLatitude() {
        return java.util.Optional.ofNullable(latitude);
    }

    public java.util.Optional<Double> getLongitude() {
        return java.util.Optional.ofNullable(longitude);
    }
}
