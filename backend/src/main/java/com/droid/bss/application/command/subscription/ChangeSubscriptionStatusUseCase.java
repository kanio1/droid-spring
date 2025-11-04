package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.ChangeSubscriptionStatusCommand;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class ChangeSubscriptionStatusUseCase {

    private final SubscriptionRepository subscriptionRepository;

    public ChangeSubscriptionStatusUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public SubscriptionEntity handle(ChangeSubscriptionStatusCommand command) {
        // Find existing subscription
        SubscriptionEntity subscription = subscriptionRepository.findById(UUID.fromString(command.id()))
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + command.id()));

        SubscriptionStatus oldStatus = subscription.getStatus();
        SubscriptionStatus newStatus = command.status();

        // Update status
        subscription.setStatus(newStatus);
        subscription.setUpdatedAt(LocalDateTime.now());

        // Set additional fields based on status
        if (newStatus == SubscriptionStatus.CANCELLED && oldStatus != SubscriptionStatus.CANCELLED) {
            if (command.reason() != null) {
                // Store cancellation reason in notes or configuration
                // This would require a field in the entity
            }
        }

        if (newStatus == SubscriptionStatus.EXPIRED) {
            subscription.setEndDate(LocalDate.now());
        }

        if (newStatus == SubscriptionStatus.SUSPENDED) {
            // Suspension logic - might set suspension date
        }

        if (newStatus == SubscriptionStatus.ACTIVE && oldStatus != SubscriptionStatus.ACTIVE) {
            // Reactivation logic
        }

        return subscriptionRepository.save(subscription);
    }
}
