package com.droid.bss.application.dto.customer;

import com.droid.bss.domain.customer.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Command for changing customer status
 */
@Schema(name = "ChangeCustomerStatusRequest", description = "Request to change customer status")
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
