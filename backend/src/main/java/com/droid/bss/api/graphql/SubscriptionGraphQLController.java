package com.droid.bss.api.graphql;

import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.infrastructure.read.SubscriptionReadRepository;
import com.droid.bss.infrastructure.read.CustomerReadRepository;
import com.droid.bss.infrastructure.read.ProductReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Controller for Subscription-related queries and mutations
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SubscriptionGraphQLController {

    private final SubscriptionReadRepository subscriptionRepository;
    private final CustomerReadRepository customerRepository;
    private final ProductReadRepository productRepository;

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<SubscriptionEntity> subscription(@Argument UUID id) {
        log.debug("Fetching subscription with id: {}", id);
        return CompletableFuture.supplyAsync(() ->
            subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id))
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<SubscriptionEntity>> subscriptions(
            @Argument Optional<Integer> page,
            @Argument Optional<Integer> size,
            @Argument Optional<SubscriptionStatus> status,
            @Argument Optional<UUID> customerId) {

        log.debug("Fetching subscriptions with filters");
        return CompletableFuture.supplyAsync(() -> {
            List<SubscriptionEntity> subscriptions = subscriptionRepository.findAll();

            if (status.isPresent()) {
                subscriptions = subscriptions.stream()
                    .filter(s -> s.getStatus() == status.get())
                    .collect(java.util.stream.Collectors.toList());
            }

            if (customerId.isPresent()) {
                subscriptions = subscriptions.stream()
                    .filter(s -> s.getCustomer().getId().equals(customerId.get()))
                    .collect(java.util.stream.Collectors.toList());
            }

            return subscriptions;
        });
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<SubscriptionEntity>> subscriptionsByCustomer(@Argument UUID customerId) {
        log.debug("Fetching subscriptions for customer: {}", customerId);
        return CompletableFuture.supplyAsync(() ->
            subscriptionRepository.findByCustomerId(customerId)
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<SubscriptionEntity> createSubscription(@Argument("input") CreateSubscriptionInput input) {
        log.info("Creating subscription for customer: {}, product: {}", input.getCustomerId(), input.getProductId());
        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = customerRepository.findById(input.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + input.getCustomerId()));

            ProductEntity product = productRepository.findById(input.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + input.getProductId()));

            SubscriptionEntity subscription = SubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .product(product)
                .status(SubscriptionStatus.PENDING)
                .startDate(input.getStartDate())
                .endDate(calculateEndDate(input.getStartDate(), input.getBillingCycle()))
                .billingCycle(input.getBillingCycle())
                .price(input.getPrice())
                .currency(input.getCurrency() != null ? input.getCurrency() : "PLN")
                .autoRenew(input.getAutoRenew() != null ? input.getAutoRenew() : true)
                .trialEndDate(input.getTrialEndDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return subscriptionRepository.save(subscription);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<SubscriptionEntity> updateSubscription(
            @Argument UUID id,
            @Argument("input") UpdateSubscriptionInput input) {

        log.info("Updating subscription: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

            if (input.getStatus() != null) subscription.setStatus(input.getStatus());
            if (input.getEndDate() != null) subscription.setEndDate(input.getEndDate());
            if (input.getBillingCycle() != null) subscription.setBillingCycle(input.getBillingCycle());
            if (input.getPrice() != null) subscription.setPrice(input.getPrice());
            if (input.getAutoRenew() != null) subscription.setAutoRenew(input.getAutoRenew());
            subscription.setUpdatedAt(LocalDateTime.now());

            return subscriptionRepository.save(subscription);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<SubscriptionEntity> cancelSubscription(
            @Argument UUID id,
            @Argument Optional<String> reason) {

        log.info("Cancelling subscription: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
            subscription.setCancellationReason(reason.orElse("No reason provided"));
            subscription.setUpdatedAt(LocalDateTime.now());

            return subscriptionRepository.save(subscription);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<SubscriptionEntity> renewSubscription(@Argument UUID id) {
        log.info("Renewing subscription: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

            LocalDate newStartDate = subscription.getEndDate() != null ?
                subscription.getEndDate() : LocalDate.now();
            LocalDate newEndDate = calculateEndDate(newStartDate, subscription.getBillingCycle());

            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setStartDate(newStartDate);
            subscription.setEndDate(newEndDate);
            subscription.setCancelledAt(null);
            subscription.setCancellationReason(null);
            subscription.setUpdatedAt(LocalDateTime.now());

            return subscriptionRepository.save(subscription);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<SubscriptionEntity> suspendSubscription(
            @Argument UUID id,
            @Argument Optional<String> reason) {

        log.info("Suspending subscription: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            subscription.setSuspendedAt(LocalDateTime.now());
            subscription.setSuspensionReason(reason.orElse("No reason provided"));
            subscription.setUpdatedAt(LocalDateTime.now());

            return subscriptionRepository.save(subscription);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<SubscriptionEntity> activateSubscription(@Argument UUID id) {
        log.info("Activating subscription: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            SubscriptionEntity subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));

            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setSuspendedAt(null);
            subscription.setSuspensionReason(null);
            subscription.setUpdatedAt(LocalDateTime.now());

            return subscriptionRepository.save(subscription);
        });
    }

    private LocalDate calculateEndDate(LocalDate startDate, String billingCycle) {
        switch (billingCycle.toLowerCase()) {
            case "monthly":
            case "month":
                return startDate.plusMonths(1);
            case "quarterly":
            case "quarter":
                return startDate.plusMonths(3);
            case "yearly":
            case "year":
            case "annually":
                return startDate.plusYears(1);
            default:
                return startDate.plusMonths(1);
        }
    }

    // ========== INPUT CLASSES ==========

    public static class CreateSubscriptionInput {
        private UUID customerId;
        private UUID productId;
        private LocalDate startDate;
        private String billingCycle;
        private BigDecimal price;
        private String currency;
        private Boolean autoRenew;
        private LocalDate trialEndDate;

        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public String getBillingCycle() { return billingCycle; }
        public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public Boolean getAutoRenew() { return autoRenew; }
        public void setAutoRenew(Boolean autoRenew) { this.autoRenew = autoRenew; }

        public LocalDate getTrialEndDate() { return trialEndDate; }
        public void setTrialEndDate(LocalDate trialEndDate) { this.trialEndDate = trialEndDate; }
    }

    public static class UpdateSubscriptionInput {
        private SubscriptionStatus status;
        private LocalDate endDate;
        private String billingCycle;
        private BigDecimal price;
        private Boolean autoRenew;

        public SubscriptionStatus getStatus() { return status; }
        public void setStatus(SubscriptionStatus status) { this.status = status; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public String getBillingCycle() { return billingCycle; }
        public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Boolean getAutoRenew() { return autoRenew; }
        public void setAutoRenew(Boolean autoRenew) { this.autoRenew = autoRenew; }
    }
}
