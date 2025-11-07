package com.droid.bss.domain.subscription;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.product.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Subscription - Aggregate Root Tests")
class SubscriptionTest {

    @Test
    @DisplayName("should create subscription with minimal required fields")
    void shouldCreateSubscriptionWithMinimalFields() {
        // Given
        CustomerId customerId = CustomerId.generate();
        ProductId productId = ProductId.generate();
        String subscriptionNumber = "SUB-001";
        LocalDate startDate = LocalDate.now();
        String billingPeriod = "MONTHLY";
        BigDecimal price = new BigDecimal("99.99");
        String currency = "PLN";

        // When
        Subscription subscription = Subscription.create(
            subscriptionNumber,
            customerId,
            productId,
            startDate,
            billingPeriod,
            price,
            currency
        );

        // Then
        assertThat(subscription).isNotNull();
        assertThat(subscription.getId()).isNotNull();
        assertThat(subscription.getSubscriptionNumber()).isEqualTo(subscriptionNumber);
        assertThat(subscription.getCustomerId()).isEqualTo(customerId);
        assertThat(subscription.getProductId()).isEqualTo(productId);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartDate()).isEqualTo(startDate);
        assertThat(subscription.getBillingPeriod()).isEqualTo("MONTHLY");
        assertThat(subscription.getPrice()).isEqualByComparingTo(price);
        assertThat(subscription.getCurrency()).isEqualTo(currency);
        assertThat(subscription.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(subscription.getNetAmount()).isEqualByComparingTo(price);
        assertThat(subscription.isAutoRenew()).isTrue();
        assertThat(subscription.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("should create subscription with all fields")
    void shouldCreateSubscriptionWithAllFields() {
        // Given
        CustomerId customerId = CustomerId.generate();
        ProductId productId = ProductId.generate();
        OrderId orderId = OrderId.generate();
        String subscriptionNumber = "SUB-002";
        LocalDate startDate = LocalDate.now();
        String billingPeriod = "YEARLY";
        BigDecimal price = new BigDecimal("999.99");
        String currency = "PLN";
        BigDecimal discountAmount = new BigDecimal("99.99");
        Map<String, Object> configuration = Map.of("feature1", true, "feature2", "value");
        boolean autoRenew = false;

        // When
        Subscription subscription = Subscription.create(
            subscriptionNumber,
            customerId,
            productId,
            orderId,
            startDate,
            billingPeriod,
            price,
            currency,
            discountAmount,
            configuration,
            autoRenew
        );

        // Then
        assertThat(subscription.getOrderId()).isEqualTo(orderId);
        assertThat(subscription.getBillingPeriod()).isEqualTo("YEARLY");
        assertThat(subscription.getDiscountAmount()).isEqualByComparingTo(discountAmount);
        assertThat(subscription.getNetAmount()).isEqualByComparingTo(price.subtract(discountAmount));
        assertThat(subscription.getConfiguration()).isEqualTo(configuration);
        assertThat(subscription.isAutoRenew()).isFalse();
        assertThat(subscription.getNextBillingDate()).isEqualTo(startDate.plusYears(1));
    }

    @Test
    @DisplayName("should create subscription with quarterly billing")
    void shouldCreateSubscriptionWithQuarterlyBilling() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        // When
        Subscription subscription = Subscription.create(
            "SUB-Q1",
            CustomerId.generate(),
            ProductId.generate(),
            startDate,
            "QUARTERLY",
            new BigDecimal("299.99"),
            "PLN"
        );

        // Then
        assertThat(subscription.getBillingPeriod()).isEqualTo("QUARTERLY");
        assertThat(subscription.getNextBillingDate()).isEqualTo(LocalDate.of(2025, 4, 1));
    }

    @Test
    @DisplayName("should create subscription with default currency PLN")
    void shouldCreateSubscriptionWithDefaultCurrencyPln() {
        // When
        Subscription subscription = Subscription.create(
            "SUB-PLN",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("50.00"),
            null
        );

        // Then
        assertThat(subscription.getCurrency()).isEqualTo("PLN");
    }

    @Test
    @DisplayName("should change status from ACTIVE to SUSPENDED")
    void shouldChangeStatusFromActiveToSuspended() {
        // Given
        Subscription subscription = createTestSubscription();

        // When
        Subscription suspended = subscription.suspend();

        // Then
        assertThat(suspended.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        assertThat(suspended.isSuspended()).isTrue();
        assertThat(suspended.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should resume suspended subscription")
    void shouldResumeSuspendedSubscription() {
        // Given
        Subscription subscription = createTestSubscription().suspend();

        // When
        Subscription resumed = subscription.resume();

        // Then
        assertThat(resumed.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(resumed.isActive()).isTrue();
        assertThat(resumed.getEndDate()).isNull();
        assertThat(resumed.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("should cancel subscription")
    void shouldCancelSubscription() {
        // Given
        Subscription subscription = createTestSubscription();

        // When
        Subscription cancelled = subscription.cancel();

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(cancelled.isCancelled()).isTrue();
        assertThat(cancelled.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(cancelled.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should expire subscription")
    void shouldExpireSubscription() {
        // Given
        Subscription subscription = createTestSubscription();

        // When
        Subscription expired = subscription.expire();

        // Then
        assertThat(expired.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(expired.isExpired()).isTrue();
        assertThat(expired.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update price")
    void shouldUpdatePrice() {
        // Given
        Subscription subscription = createTestSubscription();
        BigDecimal newPrice = new BigDecimal("149.99");

        // When
        Subscription updated = subscription.updatePrice(newPrice);

        // Then
        assertThat(updated.getPrice()).isEqualByComparingTo(newPrice);
        assertThat(updated.getNetAmount()).isEqualByComparingTo(newPrice);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update discount")
    void shouldUpdateDiscount() {
        // Given
        Subscription subscription = createTestSubscription();
        BigDecimal newDiscount = new BigDecimal("25.00");

        // When
        Subscription updated = subscription.updateDiscount(newDiscount);

        // Then
        assertThat(updated.getDiscountAmount()).isEqualByComparingTo(newDiscount);
        assertThat(updated.getNetAmount()).isEqualByComparingTo(
            subscription.getPrice().subtract(newDiscount)
        );
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update auto-renew setting")
    void shouldUpdateAutoRenewSetting() {
        // Given
        Subscription subscription = createTestSubscription();

        // When
        Subscription updated = subscription.updateAutoRenew(false);

        // Then
        assertThat(updated.isAutoRenew()).isFalse();
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should mark renewal notice as sent")
    void shouldMarkRenewalNoticeAsSent() {
        // Given
        Subscription subscription = createTestSubscription();

        // When
        Subscription updated = subscription.markRenewalNoticeSent();

        // Then
        assertThat(updated.isRenewalNoticeSent()).isTrue();
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should renew subscription")
    void shouldRenewSubscription() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        Subscription subscription = Subscription.create(
            "SUB-RENEW",
            CustomerId.generate(),
            ProductId.generate(),
            startDate,
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        );

        // When
        Subscription renewed = subscription.renew();

        // Then
        assertThat(renewed.getBillingStart()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(renewed.getNextBillingDate()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(renewed.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if subscription is active")
    void shouldCheckIfSubscriptionIsActive() {
        // Given
        Subscription activeSubscription = createTestSubscription();
        Subscription suspendedSubscription = createTestSubscription().suspend();
        Subscription cancelledSubscription = createTestSubscription().cancel();
        Subscription expiredSubscription = createTestSubscription().expire();

        // Then
        assertThat(activeSubscription.isActive()).isTrue();
        assertThat(suspendedSubscription.isActive()).isFalse();
        assertThat(cancelledSubscription.isActive()).isFalse();
        assertThat(expiredSubscription.isActive()).isFalse();
    }

    @Test
    @DisplayName("should check if subscription is suspended")
    void shouldCheckIfSubscriptionIsSuspended() {
        // Given
        Subscription suspendedSubscription = createTestSubscription().suspend();

        // Then
        assertThat(suspendedSubscription.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("should check if subscription is cancelled")
    void shouldCheckIfSubscriptionIsCancelled() {
        // Given
        Subscription cancelledSubscription = createTestSubscription().cancel();

        // Then
        assertThat(cancelledSubscription.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("should check if subscription is expired")
    void shouldCheckIfSubscriptionIsExpired() {
        // Given
        Subscription expiredSubscription = createTestSubscription().expire();

        // Then
        assertThat(expiredSubscription.isExpired()).isTrue();
    }

    @Test
    @DisplayName("should check if subscription can be suspended")
    void shouldCheckIfSubscriptionCanBeSuspended() {
        // Given
        Subscription activeSubscription = createTestSubscription();
        Subscription suspendedSubscription = createTestSubscription().suspend();

        // Then
        assertThat(activeSubscription.canBeSuspended()).isTrue();
        assertThat(suspendedSubscription.canBeSuspended()).isFalse();
    }

    @Test
    @DisplayName("should check if subscription can be resumed")
    void shouldCheckIfSubscriptionCanBeResumed() {
        // Given
        Subscription suspendedSubscription = createTestSubscription().suspend();
        Subscription activeSubscription = createTestSubscription();

        // Then
        assertThat(suspendedSubscription.canBeResumed()).isTrue();
        assertThat(activeSubscription.canBeResumed()).isFalse();
    }

    @Test
    @DisplayName("should check if subscription can be cancelled")
    void shouldCheckIfSubscriptionCanBeCancelled() {
        // Given
        Subscription activeSubscription = createTestSubscription();
        Subscription suspendedSubscription = createTestSubscription().suspend();
        Subscription cancelledSubscription = createTestSubscription().cancel();

        // Then
        assertThat(activeSubscription.canBeCancelled()).isTrue();
        assertThat(suspendedSubscription.canBeCancelled()).isTrue();
        assertThat(cancelledSubscription.canBeCancelled()).isFalse();
    }

    @Test
    @DisplayName("should check if subscription can be modified")
    void shouldCheckIfSubscriptionCanBeModified() {
        // Given
        Subscription activeSubscription = createTestSubscription();
        Subscription suspendedSubscription = createTestSubscription().suspend();
        Subscription cancelledSubscription = createTestSubscription().cancel();

        // Then
        assertThat(activeSubscription.canBeModified()).isTrue();
        assertThat(suspendedSubscription.canBeModified()).isTrue();
        assertThat(cancelledSubscription.canBeModified()).isFalse();
    }

    @Test
    @DisplayName("should check if subscription is for an order")
    void shouldCheckIfSubscriptionIsForAnOrder() {
        // Given
        Subscription subscriptionWithoutOrder = createTestSubscription();
        Subscription subscriptionWithOrder = Subscription.create(
            "SUB-ORDER",
            CustomerId.generate(),
            ProductId.generate(),
            OrderId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN",
            null,
            null,
            true
        );

        // Then
        assertThat(subscriptionWithoutOrder.isForOrder()).isFalse();
        assertThat(subscriptionWithOrder.isForOrder()).isTrue();
    }

    @Test
    @DisplayName("should check if subscription has configuration")
    void shouldCheckIfSubscriptionHasConfiguration() {
        // Given
        Subscription subscriptionWithoutConfig = createTestSubscription();
        Map<String, Object> config = Map.of("key", "value");
        Subscription subscriptionWithConfig = Subscription.create(
            "SUB-CONFIG",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN",
            null,
            config,
            true
        );

        // Then
        assertThat(subscriptionWithoutConfig.hasConfiguration()).isFalse();
        assertThat(subscriptionWithConfig.hasConfiguration()).isTrue();
    }

    @Test
    @DisplayName("should check if subscription is due for renewal")
    void shouldCheckIfSubscriptionIsDueForRenewal() {
        // Given
        LocalDate today = LocalDate.now();
        Subscription subscription = Subscription.create(
            "SUB-RENEW-DUE",
            CustomerId.generate(),
            ProductId.generate(),
            today.minusMonths(1),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        );

        // When - simulate renewal
        Subscription dueForRenewal = subscription.renew();

        // Then
        assertThat(dueForRenewal.isDueForRenewal()).isTrue();
    }

    @Test
    @DisplayName("should check if subscription is past billing date")
    void shouldCheckIfSubscriptionIsPastBillingDate() {
        // Given
        LocalDate pastDate = LocalDate.now().minusDays(10);
        Subscription subscription = Subscription.create(
            "SUB-PAST",
            CustomerId.generate(),
            ProductId.generate(),
            pastDate,
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        );

        // Then
        assertThat(subscription.isPastBillingDate()).isTrue();
    }

    @Test
    @DisplayName("should throw exception for negative price")
    void shouldThrowExceptionForNegativePrice() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-NEG",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("-100.00"),
            "PLN"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Price cannot be negative");
    }

    @Test
    @DisplayName("should throw exception when discount exceeds price")
    void shouldThrowExceptionWhenDiscountExceedsPrice() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-DISC",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN",
            new BigDecimal("150.00"),
            null,
            true
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Discount cannot exceed price");
    }

    @Test
    @DisplayName("should throw exception for null subscription number")
    void shouldThrowExceptionForNullSubscriptionNumber() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            null,
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Subscription number cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null customer ID")
    void shouldThrowExceptionForNullCustomerId() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-NULL",
            null,
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Customer ID cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null product ID")
    void shouldThrowExceptionForNullProductId() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-NULL",
            CustomerId.generate(),
            null,
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Product ID cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null start date")
    void shouldThrowExceptionForNullStartDate() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-NULL",
            CustomerId.generate(),
            ProductId.generate(),
            null,
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Start date cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null billing period")
    void shouldThrowExceptionForNullBillingPeriod() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-NULL",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            null,
            new BigDecimal("100.00"),
            "PLN"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Billing period cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null price")
    void shouldThrowExceptionForNullPrice() {
        // When & Then
        assertThatThrownBy(() -> Subscription.create(
            "SUB-NULL",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            null,
            "PLN"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Price cannot be null");
    }

    @Test
    @DisplayName("should throw exception for invalid status transition from CANCELLED")
    void shouldThrowExceptionForInvalidStatusTransitionFromCancelled() {
        // Given
        Subscription subscription = createTestSubscription().cancel();

        // When & Then
        assertThatThrownBy(() -> subscription.changeStatus(SubscriptionStatus.ACTIVE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot change status from CANCELLED to ACTIVE");
    }

    @Test
    @DisplayName("should throw exception for invalid status transition from EXPIRED")
    void shouldThrowExceptionForInvalidStatusTransitionFromExpired() {
        // Given
        Subscription subscription = createTestSubscription().expire();

        // When & Then
        assertThatThrownBy(() -> subscription.suspend())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot change status from EXPIRED to SUSPENDED");
    }

    @Test
    @DisplayName("should throw exception when trying to renew without auto-renew")
    void shouldThrowExceptionWhenTryingToRenewWithoutAutoRenew() {
        // Given
        Subscription subscription = Subscription.create(
            "SUB-NO-RENEW",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN",
            null,
            null,
            false
        );

        // When & Then
        assertThatThrownBy(() -> subscription.renew())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Subscription cannot be automatically renewed");
    }

    @Test
    @DisplayName("should throw exception when trying to update price to negative")
    void shouldThrowExceptionWhenTryingToUpdatePriceToNegative() {
        // Given
        Subscription subscription = createTestSubscription();

        // When & Then
        assertThatThrownBy(() -> subscription.updatePrice(new BigDecimal("-50.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Price cannot be negative");
    }

    @Test
    @DisplayName("should throw exception when trying to update discount above price")
    void shouldThrowExceptionWhenTryingToUpdateDiscountAbovePrice() {
        // Given
        Subscription subscription = createTestSubscription();

        // When & Then
        assertThatThrownBy(() -> subscription.updateDiscount(new BigDecimal("200.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Discount cannot exceed price");
    }

    @Test
    @DisplayName("should have correct version after multiple updates")
    void shouldHaveCorrectVersionAfterMultipleUpdates() {
        // Given
        Subscription subscription = createTestSubscription();

        // When
        Subscription updated1 = subscription.suspend();
        Subscription updated2 = updated1.resume();
        Subscription updated3 = updated2.updatePrice(new BigDecimal("150.00"));
        Subscription updated4 = updated3.updateDiscount(new BigDecimal("25.00"));
        Subscription updated5 = updated4.cancel();

        // Then
        assertThat(subscription.getVersion()).isEqualTo(1);
        assertThat(updated1.getVersion()).isEqualTo(2);
        assertThat(updated2.getVersion()).isEqualTo(3);
        assertThat(updated3.getVersion()).isEqualTo(4);
        assertThat(updated4.getVersion()).isEqualTo(5);
        assertThat(updated5.getVersion()).isEqualTo(6);
    }

    @Test
    @DisplayName("should store created and updated timestamps")
    void shouldStoreCreatedAndUpdatedTimestamps() {
        // When
        Subscription subscription = createTestSubscription();

        // Then
        assertThat(subscription.getCreatedAt()).isNotNull();
        assertThat(subscription.getUpdatedAt()).isEqualTo(subscription.getCreatedAt());
    }

    @Test
    @DisplayName("should update timestamp when modifying subscription")
    void shouldUpdateTimestampWhenModifyingSubscription() {
        // Given
        Subscription subscription = createTestSubscription();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        // When
        Subscription updated = subscription.updatePrice(new BigDecimal("150.00"));

        // Then
        assertThat(updated.getUpdatedAt()).isGreaterThan(subscription.getCreatedAt());
    }

    @Test
    @DisplayName("should calculate net amount correctly")
    void shouldCalculateNetAmountCorrectly() {
        // Given
        BigDecimal price = new BigDecimal("100.00");
        BigDecimal discount = new BigDecimal("20.00");

        // When
        Subscription subscription = Subscription.create(
            "SUB-NET",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            price,
            "PLN",
            discount,
            null,
            true
        );

        // Then
        assertThat(subscription.getNetAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("should normalize billing period to uppercase")
    void shouldNormalizeBillingPeriodToUppercase() {
        // When
        Subscription subscription = Subscription.create(
            "SUB-CASE",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "monthly",
            new BigDecimal("100.00"),
            "PLN"
        );

        // Then
        assertThat(subscription.getBillingPeriod()).isEqualTo("MONTHLY");
    }

    // Helper methods
    private Subscription createTestSubscription() {
        return Subscription.create(
            "SUB-TEST-001",
            CustomerId.generate(),
            ProductId.generate(),
            LocalDate.now(),
            "MONTHLY",
            new BigDecimal("100.00"),
            "PLN"
        );
    }
}
