package com.droid.bss.domain.address;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Address entity for managing customer addresses
 * Supports multiple address types per customer
 */
@Entity
@Table(name = "addresses")
public class AddressEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AddressType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AddressStatus status;

    @Column(name = "street", nullable = false, length = 255)
    private String street;

    @Column(name = "house_number", length = 20)
    private String houseNumber;

    @Column(name = "apartment_number", length = 20)
    private String apartmentNumber;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "region", length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "country", nullable = false, length = 2)
    private Country country;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public AddressEntity() {}

    public AddressEntity(
            CustomerEntity customer,
            AddressType type,
            AddressStatus status,
            String street,
            String postalCode,
            String city,
            Country country
    ) {
        this.customer = customer;
        this.type = type;
        this.status = status != null ? status : AddressStatus.ACTIVE;
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
    }

    // Business logic methods

    public boolean isActive() {
        return this.status == AddressStatus.ACTIVE && deletedAt == null;
    }

    public boolean isPrimary() {
        return this.isPrimary != null && this.isPrimary;
    }

    public void markAsPrimary() {
        this.isPrimary = true;
    }

    public void unmarkAsPrimary() {
        this.isPrimary = false;
    }

    public void deactivate() {
        this.status = AddressStatus.INACTIVE;
    }

    public void activate() {
        this.status = AddressStatus.ACTIVE;
    }

    public void softDelete() {
        this.deletedAt = java.time.LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // Helper methods

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

    // Getters and setters

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public AddressType getType() {
        return type;
    }

    public void setType(AddressType type) {
        this.type = type;
    }

    public AddressStatus getStatus() {
        return status;
    }

    public void setStatus(AddressStatus status) {
        this.status = status;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public java.time.LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.time.LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "AddressEntity{" +
                "id=" + getId() +
                ", type=" + type +
                ", status=" + status +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", country=" + country +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
