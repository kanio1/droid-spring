package com.droid.bss.application.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Optional;

/**
 * Command for creating a new address
 */
public record CreateAddressCommand(
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

        Boolean isPrimary,

        String notes
) {
    // Helper method to get optional fields
    public Optional<String> getHouseNumber() {
        return Optional.ofNullable(houseNumber);
    }

    public Optional<String> getApartmentNumber() {
        return Optional.ofNullable(apartmentNumber);
    }

    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }

    public Optional<Double> getLatitude() {
        return Optional.ofNullable(latitude);
    }

    public Optional<Double> getLongitude() {
        return Optional.ofNullable(longitude);
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }
}
