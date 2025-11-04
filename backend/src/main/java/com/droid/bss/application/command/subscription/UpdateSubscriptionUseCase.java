package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.UpdateSubscriptionCommand;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class UpdateSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;

    public UpdateSubscriptionUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public SubscriptionEntity handle(UpdateSubscriptionCommand command) {
        // Find existing subscription
        SubscriptionEntity subscription = subscriptionRepository.findById(UUID.fromString(command.id()))
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + command.id()));

        // Update fields if provided
        if (command.endDate() != null) {
            subscription.setEndDate(command.endDate());
        }
        if (command.nextBillingDate() != null) {
            subscription.setNextBillingDate(command.nextBillingDate());
        }
        if (command.price() != null) {
            subscription.setPrice(command.price());
        }
        if (command.currency() != null) {
            subscription.setCurrency(command.currency());
        }
        if (command.billingPeriod() != null) {
            subscription.setBillingPeriod(command.billingPeriod());
        }

        // Auto-renew cannot be changed via update - separate endpoint for that
        // or we can allow it here if needed
        // subscription.setAutoRenew(command.autoRenew());

        subscription.setUpdatedAt(LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }
}
