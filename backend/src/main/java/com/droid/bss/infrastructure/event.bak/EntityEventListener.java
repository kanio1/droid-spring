package com.droid.bss.infrastructure.event;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.event.CustomerEventPublisher;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.event.InvoiceEventPublisher;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.event.OrderEventPublisher;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.event.PaymentEventPublisher;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.event.SubscriptionEventPublisher;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Event listener that publishes CloudEvents when entities are persisted or updated
 */
@Component
public class EntityEventListener {

    @Autowired
    private InvoiceEventPublisher invoiceEventPublisher;

    @Autowired
    private CustomerEventPublisher customerEventPublisher;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private SubscriptionEventPublisher subscriptionEventPublisher;

    /**
     * Listen for invoice persist/update
     */
    @PostPersist
    @PostUpdate
    public void handleInvoiceEvent(InvoiceEntity invoice) {
        invoiceEventPublisher.publishInvoiceCreated(invoice);
    }

    /**
     * Listen for customer persist/update
     */
    @PostPersist
    @PostUpdate
    public void handleCustomerEvent(CustomerEntity customer) {
        customerEventPublisher.publishCustomerCreated(customer);
    }

    /**
     * Listen for order persist/update
     */
    @PostPersist
    @PostUpdate
    public void handleOrderEvent(OrderEntity order) {
        orderEventPublisher.publishOrderCreated(order);
    }

    /**
     * Listen for payment persist/update
     */
    @PostPersist
    @PostUpdate
    public void handlePaymentEvent(PaymentEntity payment) {
        paymentEventPublisher.publishPaymentCreated(payment);
    }

    /**
     * Listen for subscription persist/update
     */
    @PostPersist
    @PostUpdate
    public void handleSubscriptionEvent(SubscriptionEntity subscription) {
        subscriptionEventPublisher.publishSubscriptionCreated(subscription);
    }
}
