package com.droid.bss.application.dto.customer;

import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateCustomerCommand(
    @NotNull(message = "Customer ID is required")
    String customerId,
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName,
    
    @Pattern(regexp = "^\\d{11}$", message = "PESEL must be exactly 11 digits")
    String pesel,
    
    @Pattern(regexp = "^\\d{10}$", message = "NIP must be exactly 10 digits")
    String nip,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @Pattern(regexp = "^\\+?[0-9\\s-]{9,15}$", message = "Phone must be between 9-15 characters")
    String phone
) {
    
    public CustomerInfo toCustomerInfo() {
        return new CustomerInfo(firstName, lastName, pesel, nip);
    }
    
    public ContactInfo toContactInfo() {
        return new ContactInfo(email, phone);
    }
}
