package com.droid.bss.application.dto.subscription;

import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Subscription entity
 */
@Schema(name = "SubscriptionResponse", description = "Subscription response with full details")
public record SubscriptionResponse(
    String id,
    String subscriptionNumber,
    String customerId,
    String customerName,
    String productId,
    String productName,
    String orderId,
    String status,
    String statusDisplayName,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate billingStart,
    LocalDate nextBillingDate,
    String billingPeriod,
    BigDecimal price,
    String currency,
    boolean autoRenew,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version
) {

    public static SubscriptionResponse from(SubscriptionEntity subscription) {
        return new SubscriptionResponse(
            subscription.getId().toString(),
            subscription.getSubscriptionNumber(),
            subscription.getCustomer().getId().toString(),
            subscription.getCustomer().getFirstName() + " " + subscription.getCustomer().getLastName(),
            subscription.getProduct().getId().toString(),
            subscription.getProduct().getName(),
            subscription.getOrder() != null ? subscription.getOrder().getId().toString() : null,
            subscription.getStatus().name(),
            getStatusDisplayName(subscription.getStatus()),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getBillingStart(),
            subscription.getNextBillingDate(),
            subscription.getBillingPeriod(),
            subscription.getPrice(),
            subscription.getCurrency(),
            subscription.getAutoRenew(),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt(),
            subscription.getVersion().longValue()
        );
    }

    private static String getStatusDisplayName(SubscriptionStatus status) {
        return switch (status) {
            case ACTIVE -> "Aktywna";
            case SUSPENDED -> "Zawieszona";
            case CANCELLED -> "Anulowana";
            case EXPIRED -> "Wygasła";
        };
    }
}
