package com.droid.bss.application.dto.customer;

import com.droid.bss.domain.customer.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCustomerStatusCommand(
    @NotNull(message = "Customer ID is required")
    String customerId,
    
    @NotNull(message = "Status is required")
    String status
) {
    
    public CustomerStatus toCustomerStatus() {
        try {
            return CustomerStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }
}
