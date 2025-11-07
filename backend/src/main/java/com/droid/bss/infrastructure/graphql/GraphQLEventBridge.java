package com.droid.bss.infrastructure.graphql;

import com.droid.bss.api.graphql.GraphQLSubscriptionResolver;
import com.droid.bss.domain.customer.CustomerEvent;
import com.droid.bss.domain.invoice.InvoiceEvent;
import com.droid.bss.domain.payment.PaymentEvent;
import com.droid.bss.domain.subscription.SubscriptionEvent;
import com.droid.bss.domain.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridge component that listens to domain events and broadcasts them to GraphQL subscriptions
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLEventBridge {

    private final GraphQLSubscriptionResolver subscriptionResolver;

    @EventListener
    public void handleCustomerEvent(CustomerEvent event) {
        log.debug("Broadcasting customer event: {}", event.getType());
        subscriptionResolver.broadcastCustomerEvent(event);
    }

    @EventListener
    public void handleInvoiceEvent(InvoiceEvent event) {
        log.debug("Broadcasting invoice event: {}", event.getType());
        subscriptionResolver.broadcastInvoiceEvent(event);
    }

    @EventListener
    public void handlePaymentEvent(PaymentEvent event) {
        log.debug("Broadcasting payment event: {}", event.getType());
        subscriptionResolver.broadcastPaymentEvent(event);
    }

    @EventListener
    public void handleSubscriptionEvent(SubscriptionEvent event) {
        log.debug("Broadcasting subscription event: {}", event.getType());
        subscriptionResolver.broadcastSubscriptionEvent(event);
    }

    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        log.debug("Broadcasting order event: {}", event.getType());
        subscriptionResolver.broadcastOrderEvent(event);
    }
}
