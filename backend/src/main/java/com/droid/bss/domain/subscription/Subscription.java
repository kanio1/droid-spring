package com.droid.bss.domain.subscription;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.product.ProductId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Subscription aggregate root
 * Manages customer subscription lifecycle and billing
 */
public class Subscription {

    private final SubscriptionId id;
    private final String subscriptionNumber;
    private final CustomerId customerId;
    private final ProductId productId;
    private final OrderId orderId;
    private final SubscriptionStatus status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate billingStart;
    private final LocalDate nextBillingDate;
    private final String billingPeriod;
    private final BigDecimal price;
    private final String currency;
    private final BigDecimal discountAmount;
    private final BigDecimal netAmount;
    private final Map<String, Object> configuration;
    private final boolean autoRenew;
    private final boolean renewalNoticeSent;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    Subscription(
            SubscriptionId id,
            String subscriptionNumber,
            CustomerId customerId,
            ProductId productId,
            OrderId orderId,
            SubscriptionStatus status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate billingStart,
            LocalDate nextBillingDate,
            String billingPeriod,
            BigDecimal price,
            String currency,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            Map<String, Object> configuration,
            boolean autoRenew,
            boolean renewalNoticeSent,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        this.id = Objects.requireNonNull(id, "Subscription ID cannot be null");
        this.subscriptionNumber = Objects.requireNonNull(subscriptionNumber, "Subscription number cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "Product ID cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "Start date cannot be null");
        this.billingStart = Objects.requireNonNull(billingStart, "Billing start cannot be null");
        this.billingPeriod = Objects.requireNonNull(billingPeriod, "Billing period cannot be null");
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.currency = currency != null ? currency : "PLN";
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        if (this.discountAmount.compareTo(price) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed price");
        }
        this.netAmount = netAmount != null ? netAmount : calculateNetAmount(price, this.discountAmount);
        this.orderId = orderId;
        this.endDate = endDate;
        this.nextBillingDate = nextBillingDate;
        this.configuration = configuration;
        this.autoRenew = autoRenew;
        this.renewalNoticeSent = renewalNoticeSent;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        this.version = version;
    }

    /**
     * Creates a new Subscription
     */
    public static Subscription create(
            String subscriptionNumber,
            CustomerId customerId,
            ProductId productId,
            LocalDate startDate,
            String billingPeriod,
            BigDecimal price,
            String currency
    ) {
        return create(
            subscriptionNumber,
            customerId,
            productId,
            null,
            startDate,
            billingPeriod,
            price,
            currency,
            BigDecimal.ZERO,
            null,
            true
        );
    }

    /**
     * Creates a new Subscription with all parameters
     */
    public static Subscription create(
            String subscriptionNumber,
            CustomerId customerId,
            ProductId productId,
            OrderId orderId,
            LocalDate startDate,
            String billingPeriod,
            BigDecimal price,
            String currency,
            BigDecimal discountAmount,
            Map<String, Object> configuration,
            boolean autoRenew
    ) {
        Objects.requireNonNull(subscriptionNumber, "Subscription number cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(productId, "Product ID cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(billingPeriod, "Billing period cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (discountAmount != null && discountAmount.compareTo(price) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed price");
        }

        SubscriptionId subscriptionId = SubscriptionId.generate();
        LocalDateTime now = LocalDateTime.now();
        LocalDate billingStart = startDate;
        LocalDate nextBillingDate = calculateNextBillingDate(billingStart, billingPeriod);

        return new Subscription(
            subscriptionId,
            subscriptionNumber,
            customerId,
            productId,
            orderId,
            SubscriptionStatus.ACTIVE,
            startDate,
            null,
            billingStart,
            nextBillingDate,
            billingPeriod.toUpperCase(),
            price,
            currency,
            discountAmount != null ? discountAmount : BigDecimal.ZERO,
            null,
            configuration,
            autoRenew,
            false,
            now,
            now,
            1
        );
    }

    /**
     * Changes subscription status (immutable operation)
     */
    public Subscription changeStatus(SubscriptionStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");

        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                "Cannot change status from %s to %s".formatted(this.status, newStatus)
            );
        }

        LocalDate endDate = (newStatus == SubscriptionStatus.CANCELLED || newStatus == SubscriptionStatus.EXPIRED)
            ? LocalDate.now()
            : this.endDate;

        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            newStatus,
            this.startDate,
            endDate,
            this.billingStart,
            this.nextBillingDate,
            this.billingPeriod,
            this.price,
            this.currency,
            this.discountAmount,
            this.netAmount,
            this.configuration,
            this.autoRenew,
            this.renewalNoticeSent,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Suspends subscription (immutable operation)
     */
    public Subscription suspend() {
        return changeStatus(SubscriptionStatus.SUSPENDED);
    }

    /**
     * Resumes suspended subscription (immutable operation)
     */
    public Subscription resume() {
        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            SubscriptionStatus.ACTIVE,
            this.startDate,
            null,
            this.billingStart,
            this.nextBillingDate,
            this.billingPeriod,
            this.price,
            this.currency,
            this.discountAmount,
            this.netAmount,
            this.configuration,
            this.autoRenew,
            this.renewalNoticeSent,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Cancels subscription (immutable operation)
     */
    public Subscription cancel() {
        return changeStatus(SubscriptionStatus.CANCELLED);
    }

    /**
     * Expires subscription (immutable operation)
     */
    public Subscription expire() {
        return changeStatus(SubscriptionStatus.EXPIRED);
    }

    /**
     * Updates price (immutable operation)
     */
    public Subscription updatePrice(BigDecimal newPrice) {
        Objects.requireNonNull(newPrice, "Price cannot be null");
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        BigDecimal newNetAmount = calculateNetAmount(newPrice, this.discountAmount);
        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            this.status,
            this.startDate,
            this.endDate,
            this.billingStart,
            this.nextBillingDate,
            this.billingPeriod,
            newPrice,
            this.currency,
            this.discountAmount,
            newNetAmount,
            this.configuration,
            this.autoRenew,
            this.renewalNoticeSent,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Updates discount (immutable operation)
     */
    public Subscription updateDiscount(BigDecimal newDiscountAmount) {
        if (newDiscountAmount != null && newDiscountAmount.compareTo(this.price) > 0) {
            throw new IllegalArgumentException("Discount cannot exceed price");
        }

        BigDecimal discount = newDiscountAmount != null ? newDiscountAmount : BigDecimal.ZERO;
        BigDecimal newNetAmount = calculateNetAmount(this.price, discount);
        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            this.status,
            this.startDate,
            this.endDate,
            this.billingStart,
            this.nextBillingDate,
            this.billingPeriod,
            this.price,
            this.currency,
            discount,
            newNetAmount,
            this.configuration,
            this.autoRenew,
            this.renewalNoticeSent,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Updates auto-renew setting (immutable operation)
     */
    public Subscription updateAutoRenew(boolean autoRenew) {
        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            this.status,
            this.startDate,
            this.endDate,
            this.billingStart,
            this.nextBillingDate,
            this.billingPeriod,
            this.price,
            this.currency,
            this.discountAmount,
            this.netAmount,
            this.configuration,
            autoRenew,
            this.renewalNoticeSent,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Marks renewal notice as sent (immutable operation)
     */
    public Subscription markRenewalNoticeSent() {
        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            this.status,
            this.startDate,
            this.endDate,
            this.billingStart,
            this.nextBillingDate,
            this.billingPeriod,
            this.price,
            this.currency,
            this.discountAmount,
            this.netAmount,
            this.configuration,
            this.autoRenew,
            true,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Renews subscription (immutable operation)
     */
    public Subscription renew() {
        if (!this.autoRenew || this.nextBillingDate == null) {
            throw new IllegalArgumentException("Subscription cannot be automatically renewed");
        }

        if (LocalDate.now().isBefore(this.nextBillingDate)) {
            throw new IllegalArgumentException("Cannot renew before next billing date");
        }

        LocalDate newBillingStart = this.nextBillingDate;
        LocalDate newNextBillingDate = calculateNextBillingDate(newBillingStart, this.billingPeriod);
        LocalDateTime now = LocalDateTime.now();

        return new Subscription(
            this.id,
            this.subscriptionNumber,
            this.customerId,
            this.productId,
            this.orderId,
            this.status,
            this.startDate,
            this.endDate,
            newBillingStart,
            newNextBillingDate,
            this.billingPeriod,
            this.price,
            this.currency,
            this.discountAmount,
            this.netAmount,
            this.configuration,
            this.autoRenew,
            this.renewalNoticeSent,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    // Business methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return status == SubscriptionStatus.SUSPENDED;
    }

    public boolean isCancelled() {
        return status == SubscriptionStatus.CANCELLED;
    }

    public boolean isExpired() {
        return status == SubscriptionStatus.EXPIRED || (endDate != null && LocalDate.now().isAfter(endDate));
    }

    public boolean canBeSuspended() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean canBeResumed() {
        return status == SubscriptionStatus.SUSPENDED;
    }

    public boolean canBeCancelled() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.SUSPENDED;
    }

    public boolean canBeModified() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.SUSPENDED;
    }

    public boolean isAutoRenewEnabled() {
        return autoRenew;
    }

    public boolean isRenewalNoticeSent() {
        return renewalNoticeSent;
    }

    public boolean hasConfiguration() {
        return configuration != null && !configuration.isEmpty();
    }

    public boolean isForOrder() {
        return orderId != null;
    }

    public boolean isDueForRenewal() {
        return autoRenew && nextBillingDate != null && LocalDate.now().isEqual(nextBillingDate);
    }

    public boolean isPastBillingDate() {
        return nextBillingDate != null && LocalDate.now().isAfter(nextBillingDate);
    }

    private static BigDecimal calculateNetAmount(BigDecimal price, BigDecimal discount) {
        return price.subtract(discount != null ? discount : BigDecimal.ZERO);
    }

    private static LocalDate calculateNextBillingDate(LocalDate billingStart, String billingPeriod) {
        return switch (billingPeriod.toUpperCase()) {
            case "MONTHLY" -> billingStart.plusMonths(1);
            case "QUARTERLY" -> billingStart.plusMonths(3);
            case "YEARLY" -> billingStart.plusYears(1);
            default -> billingStart.plusMonths(1);
        };
    }

    // Getters
    public SubscriptionId getId() {
        return id;
    }

    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getBillingStart() {
        return billingStart;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public boolean isRenewalNoticeSentFlag() {
        return renewalNoticeSent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    // Property-style accessors (for mapping convenience)
    public SubscriptionId id() { return id; }
    public String subscriptionNumber() { return subscriptionNumber; }
    public CustomerId customerId() { return customerId; }
    public ProductId productId() { return productId; }
    public OrderId orderId() { return orderId; }
    public SubscriptionStatus status() { return status; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public LocalDate billingStart() { return billingStart; }
    public LocalDate nextBillingDate() { return nextBillingDate; }
    public String billingPeriod() { return billingPeriod; }
    public BigDecimal price() { return price; }
    public String currency() { return currency; }
    public BigDecimal discountAmount() { return discountAmount; }
    public BigDecimal netAmount() { return netAmount; }
    public Map<String, Object> configuration() { return configuration; }
    public boolean autoRenew() { return autoRenew; }
    public boolean renewalNoticeSent() { return renewalNoticeSent; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public int version() { return version; }

    /**
     * Restores Subscription from persistence state (for infrastructure layer)
     * Package-private - use by repository implementations only
     */
    static Subscription restore(
            UUID id,
            String subscriptionNumber,
            UUID customerId,
            UUID productId,
            SubscriptionStatus status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate billingStart,
            LocalDate nextBillingDate,
            String billingPeriod,
            BigDecimal price,
            String currency,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            Map<String, Object> configuration,
            boolean autoRenew,
            boolean renewalNoticeSent,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        return new Subscription(
            new SubscriptionId(id),
            subscriptionNumber,
            new CustomerId(customerId),
            new ProductId(productId),
            null, // orderId - not in entity
            status,
            startDate,
            endDate,
            billingStart,
            nextBillingDate,
            billingPeriod,
            price,
            currency,
            discountAmount,
            netAmount,
            configuration,
            autoRenew,
            renewalNoticeSent,
            createdAt,
            updatedAt,
            version
        );
    }
}
