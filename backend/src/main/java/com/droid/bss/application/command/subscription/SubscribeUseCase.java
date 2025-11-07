package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.SubscribeCommand;
import com.droid.bss.application.dto.subscription.SubscriptionResponse;
import com.droid.bss.domain.subscription.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stub class for SubscribeUseCase
 * Minimal implementation for testing purposes
 */
@Component
@Transactional
public class SubscribeUseCase {

    private final SubscriptionRepository subscriptionRepository;

    public SubscribeUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public SubscriptionResponse handle(SubscribeCommand command) {
        // Stub implementation
        System.out.println("Subscribing customer " + command.customerId() + " to product " + command.productId());
        return null;
    }
}
