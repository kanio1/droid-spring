package com.droid.bss.application.command.subscription;

import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class DeleteSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;

    public DeleteSubscriptionUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public boolean handle(String id) {
        UUID subscriptionId = UUID.fromString(id);

        return subscriptionRepository.findById(subscriptionId)
                .map(subscription -> {
                    subscriptionRepository.delete(subscription);
                    return true;
                })
                .orElse(false);
    }
}
