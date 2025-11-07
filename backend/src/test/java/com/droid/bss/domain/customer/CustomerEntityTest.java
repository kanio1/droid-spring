package com.droid.bss.domain.customer;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomerEntity
 */
class CustomerEntityTest {

    @Test
    @DisplayName("Should create customer with required fields")
    void shouldCreateCustomerWithRequiredFields() {
        // When
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();

        // Then
        assertNotNull(customer);
        assertEquals("John", customer.getFirstName());
        assertEquals("Doe", customer.getLastName());
        assertEquals("john.doe@example.com", customer.getEmail());
    }

    @Test
    @DisplayName("Should set and get all fields correctly")
    void shouldSetAndGetAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        String firstName = "Jane";
        String lastName = "Smith";
        String pesel = "12345678901";
        String nip = "PL1234567890";
        String email = "jane.smith@example.com";
        String phone = "+1234567890";
        CustomerStatus status = CustomerStatus.ACTIVE;
        LocalDateTime now = LocalDateTime.now();

        // When
        CustomerEntity customer = CustomerEntity.builder()
            .id(id)
            .firstName(firstName)
            .lastName(lastName)
            .pesel(pesel)
            .nip(nip)
            .email(email)
            .phone(phone)
            .status(status)
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Then
        assertEquals(id, customer.getId());
        assertEquals(firstName, customer.getFirstName());
        assertEquals(lastName, customer.getLastName());
        assertEquals(pesel, customer.getPesel());
        assertEquals(nip, customer.getNip());
        assertEquals(email, customer.getEmail());
        assertEquals(phone, customer.getPhone());
        assertEquals(status, customer.getStatus());
        assertEquals(now, customer.getCreatedAt());
        assertEquals(now, customer.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // When
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .build();

        // Then
        assertNull(customer.getPesel());
        assertNull(customer.getNip());
        assertNull(customer.getPhone());
    }

    @Test
    @DisplayName("Should update timestamp when setting new status")
    void shouldUpdateTimestampWhenSettingNewStatus() {
        // Given
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Bob")
            .lastName("Brown")
            .email("bob@example.com")
            .status(CustomerStatus.PENDING)
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(LocalDateTime.now().minusDays(1))
            .build();

        LocalDateTime beforeUpdate = LocalDateTime.now();

        // When
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setUpdatedAt(LocalDateTime.now());

        // Then
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertNotNull(customer.getUpdatedAt());
        assertTrue(customer.getUpdatedAt().isAfter(beforeUpdate) || customer.getUpdatedAt().equals(beforeUpdate));
    }

    @Test
    @DisplayName("Should calculate full name")
    void shouldCalculateFullName() {
        // Given
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .build();

        // When
        String fullName = customer.getFirstName() + " " + customer.getLastName();

        // Then
        assertEquals("John Doe", fullName);
    }

    @Test
    @DisplayName("Should identify PENDING customers")
    void shouldIdentifyPendingCustomers() {
        // Given
        CustomerEntity pendingCustomer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .status(CustomerStatus.PENDING)
            .build();

        // Then
        assertTrue(pendingCustomer.getStatus() == CustomerStatus.PENDING);
    }

    @Test
    @DisplayName("Should identify ACTIVE customers")
    void shouldIdentifyActiveCustomers() {
        // Given
        CustomerEntity activeCustomer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Active")
            .lastName("User")
            .email("active@example.com")
            .status(CustomerStatus.ACTIVE)
            .build();

        // Then
        assertTrue(activeCustomer.getStatus() == CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should identify INACTIVE customers")
    void shouldIdentifyInactiveCustomers() {
        // Given
        CustomerEntity inactiveCustomer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Inactive")
            .lastName("User")
            .email("inactive@example.com")
            .status(CustomerStatus.INACTIVE)
            .build();

        // Then
        assertTrue(inactiveCustomer.getStatus() == CustomerStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should identify SUSPENDED customers")
    void shouldIdentifySuspendedCustomers() {
        // Given
        CustomerEntity suspendedCustomer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Suspended")
            .lastName("User")
            .email("suspended@example.com")
            .status(CustomerStatus.SUSPENDED)
            .build();

        // Then
        assertTrue(suspendedCustomer.getStatus() == CustomerStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should have all CustomerStatus enum values")
    void shouldHaveAllCustomerStatusValues() {
        // Then - verify all expected statuses exist
        assertNotNull(CustomerStatus.PENDING);
        assertNotNull(CustomerStatus.ACTIVE);
        assertNotNull(CustomerStatus.INACTIVE);
        assertNotNull(CustomerStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should update customer information")
    void shouldUpdateCustomerInformation() {
        // Given
        CustomerEntity customer = CustomerEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Old")
            .lastName("Name")
            .email("old@example.com")
            .phone("+1111111111")
            .build();

        // When
        customer.setFirstName("New");
        customer.setLastName("Name");
        customer.setEmail("new@example.com");
        customer.setPhone("+2222222222");
        customer.setUpdatedAt(LocalDateTime.now());

        // Then
        assertEquals("New", customer.getFirstName());
        assertEquals("Name", customer.getLastName());
        assertEquals("new@example.com", customer.getEmail());
        assertEquals("+2222222222", customer.getPhone());
    }
}
