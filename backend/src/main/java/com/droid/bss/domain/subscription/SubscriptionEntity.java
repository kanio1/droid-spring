package com.droid.bss.domain.subscription;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.product.ProductEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Subscription entity for active customer subscriptions
 */
@Entity
@Table(name = "subscriptions")
@SQLRestriction("deleted_at IS NULL")
public class SubscriptionEntity extends BaseEntity {

    @Column(name = "subscription_number", nullable = false, unique = true, length = 50)
    private String subscriptionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "billing_start", nullable = false)
    private LocalDate billingStart;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "billing_period", nullable = false, length = 20)
    private String billingPeriod;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    @Column(name = "auto_renew")
    private Boolean autoRenew = true;

    @Column(name = "renewal_notice_sent")
    private Boolean renewalNoticeSent = false;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    // Constructors
    public SubscriptionEntity() {}

    public SubscriptionEntity(
            String subscriptionNumber,
            CustomerEntity customer,
            ProductEntity product,
            OrderEntity order,
            SubscriptionStatus status,
            LocalDate startDate,
            LocalDate billingStart,
            String billingPeriod,
            BigDecimal price,
            String currency,
            BigDecimal discountAmount,
            Boolean autoRenew
    ) {
        this.subscriptionNumber = subscriptionNumber;
        this.customer = customer;
        this.product = product;
        this.order = order;
        this.status = status;
        this.startDate = startDate;
        this.billingStart = billingStart;
        this.billingPeriod = billingPeriod;
        this.price = price;
        this.currency = currency;
        this.discountAmount = discountAmount;
        this.autoRenew = autoRenew;
        this.calculateNetAmount();
    }

    // Business methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return status == SubscriptionStatus.SUSPENDED;
    }

    public boolean isExpired() {
        return status == SubscriptionStatus.EXPIRED ||
               (endDate != null && LocalDate.now().isAfter(endDate));
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

    public void calculateNetAmount() {
        if (price != null) {
            this.netAmount = price.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        }
    }

    public void markAsSuspended(String reason) {
        this.status = SubscriptionStatus.SUSPENDED;
        this.endDate = LocalDate.now();
    }

    public void resume() {
        this.status = SubscriptionStatus.ACTIVE;
        this.endDate = null;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.endDate = LocalDate.now();
    }

    public void renew() {
        if (autoRenew && nextBillingDate != null && LocalDate.now().isEqual(nextBillingDate)) {
            // Extend subscription by billing period
            this.billingStart = nextBillingDate;
            this.startDate = nextBillingDate;
            this.nextBillingDate = calculateNextBillingDate();
        }
    }

    private LocalDate calculateNextBillingDate() {
        return switch (billingPeriod.toUpperCase()) {
            case "MONTHLY" -> billingStart.plusMonths(1);
            case "QUARTERLY" -> billingStart.plusMonths(3);
            case "YEARLY" -> billingStart.plusYears(1);
            default -> billingStart.plusMonths(1);
        };
    }

    // Getters and setters
    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public void setSubscriptionNumber(String subscriptionNumber) {
        this.subscriptionNumber = subscriptionNumber;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getBillingStart() {
        return billingStart;
    }

    public void setBillingStart(LocalDate billingStart) {
        this.billingStart = billingStart;
    }

    public LocalDate getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(LocalDate nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        calculateNetAmount();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        calculateNetAmount();
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public Boolean getRenewalNoticeSent() {
        return renewalNoticeSent;
    }

    public void setRenewalNoticeSent(Boolean renewalNoticeSent) {
        this.renewalNoticeSent = renewalNoticeSent;
    }

    public LocalDate getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDate deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Converts JPA entity to DDD aggregate
     */
    public Subscription toDomain() {
        return Subscription.restore(
            this.id,
            this.subscriptionNumber,
            this.customer != null ? this.customer.getId() : null,
            this.product != null ? this.product.getId() : null,
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
            this.renewalNoticeSent,
            this.createdAt,
            this.updatedAt,
            this.version != null ? this.version.intValue() : 0
        );
    }

    /**
     * Creates JPA entity from DDD aggregate
     */
    public static SubscriptionEntity from(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.id = subscription.id().value();
        entity.subscriptionNumber = subscription.subscriptionNumber();
        // Note: Customer and Product need to be set by repository
        entity.status = subscription.status();
        entity.startDate = subscription.startDate();
        entity.endDate = subscription.endDate();
        entity.billingStart = subscription.billingStart();
        entity.nextBillingDate = subscription.nextBillingDate();
        entity.billingPeriod = subscription.billingPeriod();
        entity.price = subscription.price();
        entity.currency = subscription.currency();
        entity.discountAmount = subscription.discountAmount();
        entity.netAmount = subscription.netAmount();
        entity.configuration = subscription.configuration();
        entity.autoRenew = subscription.autoRenew();
        entity.renewalNoticeSent = subscription.renewalNoticeSent();
        entity.createdAt = subscription.createdAt();
        entity.updatedAt = subscription.updatedAt();
        // Note: deletedAt should be set separately for soft delete
        return entity;
    }
}
