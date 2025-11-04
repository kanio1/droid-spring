package com.droid.bss.application.dto.subscription;

import com.droid.bss.domain.subscription.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command for changing subscription status
 */
@Schema(name = "ChangeSubscriptionStatusRequest", description = "Request to change subscription status")
public record ChangeSubscriptionStatusCommand(
    @NotBlank(message = "ID is required")
    String id,

    @NotNull(message = "Status is required")
    SubscriptionStatus status,

    String reason
) {

}
