package com.droid.bss.domain.address;

import com.droid.bss.domain.customer.CustomerId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Address aggregate root
 * Manages customer addresses (billing, shipping, service, correspondence)
 */
public class Address {

    private final AddressId id;
    private final CustomerId customerId;
    private final AddressType type;
    private final AddressStatus status;
    private final String street;
    private final String houseNumber;
    private final String apartmentNumber;
    private final String postalCode;
    private final String city;
    private final String region;
    private final Country country;
    private final Double latitude;
    private final Double longitude;
    private final boolean isPrimary;
    private final String notes;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    Address(
            AddressId id,
            CustomerId customerId,
            AddressType type,
            AddressStatus status,
            String street,
            String houseNumber,
            String apartmentNumber,
            String postalCode,
            String city,
            String region,
            Country country,
            Double latitude,
            Double longitude,
            boolean isPrimary,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        this.id = Objects.requireNonNull(id, "Address ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.type = Objects.requireNonNull(type, "Address type cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.street = Objects.requireNonNull(street, "Street cannot be null");
        this.postalCode = Objects.requireNonNull(postalCode, "Postal code cannot be null");
        this.city = Objects.requireNonNull(city, "City cannot be null");
        this.country = Objects.requireNonNull(country, "Country cannot be null");
        this.houseNumber = houseNumber;
        this.apartmentNumber = apartmentNumber;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isPrimary = isPrimary;
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        this.version = version;
    }

    /**
     * Creates a new Address
     */
    public static Address create(
            CustomerId customerId,
            AddressType type,
            String street,
            String postalCode,
            String city,
            Country country
    ) {
        return create(
            customerId,
            type,
            street,
            null,
            null,
            postalCode,
            city,
            null,
            country,
            null,
            null,
            false,
            null
        );
    }

    /**
     * Creates a new Address with all parameters
     */
    public static Address create(
            CustomerId customerId,
            AddressType type,
            String street,
            String houseNumber,
            String apartmentNumber,
            String postalCode,
            String city,
            String region,
            Country country,
            Double latitude,
            Double longitude,
            boolean isPrimary,
            String notes
    ) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(type, "Address type cannot be null");
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        AddressId addressId = AddressId.generate();
        LocalDateTime now = LocalDateTime.now();

        return new Address(
            addressId,
            customerId,
            type,
            AddressStatus.ACTIVE,
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
            notes,
            now,
            now,
            1
        );
    }

    /**
     * Updates address details (immutable operation)
     */
    public Address updateAddress(
            String street,
            String houseNumber,
            String apartmentNumber,
            String postalCode,
            String city,
            String region,
            Country country,
            Double latitude,
            Double longitude,
            String notes
    ) {
        if (!canBeModified()) {
            throw new IllegalArgumentException("Cannot modify inactive address");
        }

        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        LocalDateTime now = LocalDateTime.now();

        return new Address(
            this.id,
            this.customerId,
            this.type,
            this.status,
            street,
            houseNumber,
            apartmentNumber,
            postalCode,
            city,
            region,
            country,
            latitude,
            longitude,
            this.isPrimary,
            notes,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Changes address status (immutable operation)
     */
    public Address changeStatus(AddressStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");

        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                "Cannot change status from %s to %s".formatted(this.status, newStatus)
            );
        }

        LocalDateTime now = LocalDateTime.now();

        return new Address(
            this.id,
            this.customerId,
            this.type,
            newStatus,
            this.street,
            this.houseNumber,
            this.apartmentNumber,
            this.postalCode,
            this.city,
            this.region,
            this.country,
            this.latitude,
            this.longitude,
            this.isPrimary,
            this.notes,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Marks address as primary (immutable operation)
     */
    public Address markAsPrimary() {
        if (this.isPrimary) {
            return this;
        }

        LocalDateTime now = LocalDateTime.now();

        return new Address(
            this.id,
            this.customerId,
            this.type,
            this.status,
            this.street,
            this.houseNumber,
            this.apartmentNumber,
            this.postalCode,
            this.city,
            this.region,
            this.country,
            this.latitude,
            this.longitude,
            true,
            this.notes,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Unmarks address as primary (immutable operation)
     */
    public Address unmarkAsPrimary() {
        if (!this.isPrimary) {
            return this;
        }

        LocalDateTime now = LocalDateTime.now();

        return new Address(
            this.id,
            this.customerId,
            this.type,
            this.status,
            this.street,
            this.houseNumber,
            this.apartmentNumber,
            this.postalCode,
            this.city,
            this.region,
            this.country,
            this.latitude,
            this.longitude,
            false,
            this.notes,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Adds or updates notes (immutable operation)
     */
    public Address updateNotes(String newNotes) {
        LocalDateTime now = LocalDateTime.now();

        return new Address(
            this.id,
            this.customerId,
            this.type,
            this.status,
            this.street,
            this.houseNumber,
            this.apartmentNumber,
            this.postalCode,
            this.city,
            this.region,
            this.country,
            this.latitude,
            this.longitude,
            this.isPrimary,
            newNotes,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Updates coordinates (immutable operation)
     */
    public Address updateCoordinates(Double latitude, Double longitude) {
        LocalDateTime now = LocalDateTime.now();

        return new Address(
            this.id,
            this.customerId,
            this.type,
            this.status,
            this.street,
            this.houseNumber,
            this.apartmentNumber,
            this.postalCode,
            this.city,
            this.region,
            this.country,
            latitude,
            longitude,
            this.isPrimary,
            this.notes,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    // Business methods
    public boolean isActive() {
        return status == AddressStatus.ACTIVE;
    }

    public boolean isInactive() {
        return status == AddressStatus.INACTIVE;
    }

    public boolean isPending() {
        return status == AddressStatus.PENDING;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public boolean isBillingAddress() {
        return type == AddressType.BILLING;
    }

    public boolean isShippingAddress() {
        return type == AddressType.SHIPPING;
    }

    public boolean isServiceAddress() {
        return type == AddressType.SERVICE;
    }

    public boolean isCorrespondenceAddress() {
        return type == AddressType.CORRESPONDENCE;
    }

    public boolean canBeModified() {
        return isActive();
    }

    public boolean canBeDeleted() {
        return isActive();
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Returns full formatted address
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

        if (country != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country.getName());
        }

        return sb.toString();
    }

    /**
     * Returns short formatted address (street, postal code, city)
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

    // Getters
    public AddressId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public AddressType getType() {
        return type;
    }

    public AddressStatus getStatus() {
        return status;
    }

    public String getStreet() {
        return street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public Country getCountry() {
        return country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean isPrimaryFlag() {
        return isPrimary;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    // Property-style accessors (for mapping convenience)
    public AddressId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public AddressType type() { return type; }
    public AddressStatus status() { return status; }
    public String street() { return street; }
    public String houseNumber() { return houseNumber; }
    public String apartmentNumber() { return apartmentNumber; }
    public String postalCode() { return postalCode; }
    public String city() { return city; }
    public String region() { return region; }
    public Country country() { return country; }
    public Double latitude() { return latitude; }
    public Double longitude() { return longitude; }
    public String notes() { return notes; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public int version() { return version; }

    /**
     * Restores Address from persistence state (for infrastructure layer)
     * Public - use by repository implementations only
     */
    public static Address restore(
            UUID id,
            UUID customerId,
            AddressType type,
            AddressStatus status,
            String street,
            String houseNumber,
            String apartmentNumber,
            String postalCode,
            String city,
            String region,
            Country country,
            Double latitude,
            Double longitude,
            boolean isPrimary,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        return new Address(
            new AddressId(id),
            new CustomerId(customerId),
            type,
            status,
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
            notes,
            createdAt,
            updatedAt,
            version
        );
    }
}
