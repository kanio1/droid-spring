package com.droid.bss.application.dto.customer;

import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.customer.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Customer entity
 */
@Schema(name = "CustomerResponse", description = "Customer response with full details")
public record CustomerResponse(
    UUID id,
    String firstName,
    String lastName,
    String pesel,
    String nip,
    String email,
    String phone,
    String status,
    String statusDisplayName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int version
) {
    
    public static CustomerResponse from(Customer customer) {
        CustomerInfo personalInfo = customer.getPersonalInfo();
        ContactInfo contactInfo = customer.getContactInfo();
        CustomerId customerId = customer.getId();
        CustomerStatus status = customer.getStatus();

        return new CustomerResponse(
            customerId.value(),
            personalInfo.firstName(),
            personalInfo.lastName(),
            personalInfo.pesel(),
            personalInfo.nip(),
            contactInfo.email(),
            contactInfo.phone(),
            status.name(),
            status.getDisplayName(),
            customer.getCreatedAt(),
            customer.getUpdatedAt(),
            customer.getVersion()
        );
    }
}
