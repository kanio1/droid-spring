package com.droid.bss.infrastructure.graphql;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.infrastructure.read.CustomerReadRepository;
import com.droid.bss.infrastructure.read.InvoiceReadRepository;
import com.droid.bss.infrastructure.read.PaymentReadRepository;
import com.droid.bss.infrastructure.read.SubscriptionReadRepository;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Configuration for GraphQL DataLoaders
 * Prevents N+1 problem by batching database queries
 */
@Configuration
public class DataLoaderConfig {

    private final CustomerReadRepository customerRepository;
    private final InvoiceReadRepository invoiceRepository;
    private final PaymentReadRepository paymentRepository;
    private final SubscriptionReadRepository subscriptionRepository;

    public DataLoaderConfig(
            CustomerReadRepository customerRepository,
            InvoiceReadRepository invoiceRepository,
            PaymentReadRepository paymentRepository,
            SubscriptionReadRepository subscriptionRepository) {
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Configure DataLoaderRegistry with all necessary DataLoaders
     */
    @Bean
    public DataLoaderRegistry dataLoaderRegistry() {
        DataLoaderRegistry registry = new DataLoaderRegistry();

        // Options for batching and caching
        DataLoaderOptions options = DataLoaderOptions.newOptions()
            .setBatchLoaderDispatchPredicate(null) // Use default
            .setCachingEnabled(true)
            .setMaxBatchSize(100);

        // Customer DataLoader
        DataLoader<UUID, CustomerEntity> customerDataLoader = DataLoader.newDataLoader(
            (List<UUID> ids) -> {
                log.debug("Batch loading customers for IDs: {}", ids.size());
                return customerRepository.findByIdIn(ids)
                    .thenApply(customers -> {
                        // Ensure we return results in the same order as requested IDs
                        return ids.stream()
                            .map(id -> customers.stream()
                                .filter(c -> c.getId().equals(id))
                                .findFirst()
                                .orElse(null))
                            .collect(Collectors.toList());
                    });
            },
            options
        );
        registry.register("customer", customerDataLoader);

        // Invoice DataLoader
        DataLoader<UUID, List<InvoiceEntity>> invoiceDataLoader = DataLoader.newDataLoader(
            (List<UUID> customerIds) -> {
                log.debug("Batch loading invoices for {} customers", customerIds.size());
                return invoiceRepository.findByCustomerIdIn(customerIds)
                    .thenApply(invoices -> {
                        return customerIds.stream()
                            .map(customerId -> invoices.stream()
                                .filter(inv -> inv.getCustomer().getId().equals(customerId))
                                .collect(Collectors.toList()))
                            .collect(Collectors.toList());
                    });
            },
            options
        );
        registry.register("invoices", invoiceDataLoader);

        // Payment DataLoader
        DataLoader<UUID, List<PaymentEntity>> paymentDataLoader = DataLoader.newDataLoader(
            (List<UUID> customerIds) -> {
                log.debug("Batch loading payments for {} customers", customerIds.size());
                return paymentRepository.findByCustomerIdIn(customerIds)
                    .thenApply(payments -> {
                        return customerIds.stream()
                            .map(customerId -> payments.stream()
                                .filter(payment -> payment.getInvoice().getCustomer().getId().equals(customerId))
                                .collect(Collectors.toList()))
                            .collect(Collectors.toList());
                    });
            },
            options
        );
        registry.register("payments", paymentDataLoader);

        // Subscription DataLoader
        DataLoader<UUID, List<SubscriptionEntity>> subscriptionDataLoader = DataLoader.newDataLoader(
            (List<UUID> customerIds) -> {
                log.debug("Batch loading subscriptions for {} customers", customerIds.size());
                return subscriptionRepository.findByCustomerIdIn(customerIds)
                    .thenApply(subscriptions -> {
                        return customerIds.stream()
                            .map(customerId -> subscriptions.stream()
                                .filter(sub -> sub.getCustomer().getId().equals(customerId))
                                .collect(Collectors.toList()))
                            .collect(Collectors.toList());
                    });
            },
            options
        );
        registry.register("subscriptions", subscriptionDataLoader);

        return registry;
    }
}
