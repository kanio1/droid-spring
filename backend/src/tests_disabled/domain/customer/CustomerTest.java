package com.droid.bss.domain.customer;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerTest {

    @Test
    @DisplayName("should create customer with valid data")
    void shouldCreateCustomerWithValidData() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");

        // When
        Customer customer = Customer.create(personalInfo, contactInfo);

        // Then
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getPersonalInfo()).isEqualTo(personalInfo);
        assertThat(customer.getContactInfo()).isEqualTo(contactInfo);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.canBeModified()).isTrue();
        assertThat(customer.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("should update personal info and increment version")
    void shouldUpdatePersonalInfoAndIncrementVersion() {
        // Given
        CustomerInfo originalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(originalInfo, contactInfo);

        CustomerInfo newInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");

        // When
        Customer updatedCustomer = customer.updatePersonalInfo(newInfo);

        // Then
        assertThat(updatedCustomer.getPersonalInfo()).isEqualTo(newInfo);
        assertThat(updatedCustomer.getContactInfo()).isEqualTo(contactInfo);
        assertThat(updatedCustomer.getVersion()).isEqualTo(2);
        assertThat(updatedCustomer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("should update contact info and increment version")
    void shouldUpdateContactInfoAndIncrementVersion() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo originalContactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, originalContactInfo);

        ContactInfo newContactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");

        // When
        Customer updatedCustomer = customer.updateContactInfo(newContactInfo);

        // Then
        assertThat(updatedCustomer.getContactInfo()).isEqualTo(newContactInfo);
        assertThat(updatedCustomer.getPersonalInfo()).isEqualTo(personalInfo);
        assertThat(updatedCustomer.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should change status and increment version")
    void shouldChangeStatusAndIncrementVersion() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        // When
        Customer inactiveCustomer = customer.deactivate();

        // Then
        assertThat(inactiveCustomer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(inactiveCustomer.getVersion()).isEqualTo(2);
        assertThat(inactiveCustomer.isActive()).isFalse();
    }

    @Test
    @DisplayName("should not allow invalid status transition")
    void shouldNotAllowInvalidStatusTransition() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        // When & Then
        assertThatThrownBy(() -> customer.changeStatus(CustomerStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot change status from ACTIVE to ACTIVE");
    }

    @Test
    @DisplayName("should not allow modification of terminated customer")
    void shouldNotAllowModificationOfTerminatedCustomer() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);
        Customer terminatedCustomer = customer.terminate();

        // When & Then
        assertThat(terminatedCustomer.canBeModified()).isFalse();
        assertThatThrownBy(() -> terminatedCustomer.updatePersonalInfo(new CustomerInfo("Jane", "Doe", "12345678901", "1234567890")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot modify terminated customer");
    }

    @Test
    @DisplayName("should create customer without optional fields")
    void shouldCreateCustomerWithoutOptionalFields() {
        // Given
        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", null, null);
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", null);

        // When
        Customer customer = Customer.create(personalInfo, contactInfo);

        // Then
        assertThat(customer.getPersonalInfo().pesel()).isNull();
        assertThat(customer.getPersonalInfo().nip()).isNull();
        assertThat(customer.getContactInfo().phone()).isNull();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
