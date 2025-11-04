package com.droid.bss.application.command.subscription;

import com.droid.bss.application.dto.subscription.CreateSubscriptionCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.repository.OrderRepository;
import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.repository.ProductRepository;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class CreateSubscriptionUseCase {

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerEntityRepository customerEntityRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public CreateSubscriptionUseCase(
            SubscriptionRepository subscriptionRepository,
            CustomerEntityRepository customerEntityRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerEntityRepository = customerEntityRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public UUID handle(CreateSubscriptionCommand command) {
        // Validate customer exists
        CustomerEntity customer = customerEntityRepository.findById(UUID.fromString(command.customerId()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + command.customerId()));

        // Validate product exists
        ProductEntity product = productRepository.findById(UUID.fromString(command.productId()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + command.productId()));

        // Validate order if provided
        OrderEntity order = null;
        if (command.orderId() != null) {
            order = orderRepository.findById(UUID.fromString(command.orderId()))
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.orderId()));
        }

        // Generate subscription number
        String subscriptionNumber = generateSubscriptionNumber();

        // Create subscription entity
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setSubscriptionNumber(subscriptionNumber);
        subscription.setCustomer(customer);
        subscription.setProduct(product);
        subscription.setOrder(order);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(command.startDate());
        subscription.setEndDate(command.endDate());
        subscription.setBillingStart(command.billingStart());
        subscription.setNextBillingDate(command.nextBillingDate());
        subscription.setBillingPeriod(command.billingPeriod());
        subscription.setPrice(command.price());
        subscription.setCurrency(command.currency() != null ? command.currency() : "PLN");
        subscription.setAutoRenew(command.autoRenew());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());

        // Save subscription
        SubscriptionEntity savedSubscription = subscriptionRepository.save(subscription);

        return savedSubscription.getId();
    }

    private String generateSubscriptionNumber() {
        String date = LocalDateTime.now().toString().replace("-", "").substring(0, 8);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SUB-" + date + "-" + random;
    }
}
