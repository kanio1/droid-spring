package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.CancelSubscriptionCommand;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stub class for CancelSubscriptionUseCase
 * Minimal implementation for testing purposes
 */
@Component
@Transactional
public class CancelSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;

    public CancelSubscriptionUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public void handle(CancelSubscriptionCommand command) {
        // Stub implementation
        System.out.println("Cancelling subscription " + command.subscriptionId() + " with reason: " + command.reason());
    }
}
