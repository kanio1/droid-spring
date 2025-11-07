package com.droid.bss.api.graphql;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.infrastructure.read.CustomerReadRepository;
import com.droid.bss.infrastructure.read.InvoiceReadRepository;
import com.droid.bss.infrastructure.read.PaymentReadRepository;
import com.droid.bss.infrastructure.read.SubscriptionReadRepository;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
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
import java.util.stream.Collectors;

/**
 * GraphQL Controller for Customer-related queries and mutations
 * Implements batch loading to prevent N+1 problem
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerGraphQLController {

    private final CustomerReadRepository customerRepository;
    private final InvoiceReadRepository invoiceRepository;
    private final PaymentReadRepository paymentRepository;
    private final SubscriptionReadRepository subscriptionRepository;

    // ========== QUERY RESOLVERS ==========

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<CustomerEntity> customer(
            @Argument UUID id,
            DataFetchingEnvironment env) {

        log.debug("Fetching customer with id: {}", id);
        return CompletableFuture.supplyAsync(() ->
            customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id))
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<CustomerEntity>> customers(
            @Argument Optional<Integer> page,
            @Argument Optional<Integer> size,
            @Argument Optional<CustomerStatus> status,
            @Argument Optional<String> search) {

        log.debug("Fetching customers with page: {}, size: {}, status: {}, search: {}",
            page.orElse(0), size.orElse(20), status.orElse(null), search.orElse(null));

        return CompletableFuture.supplyAsync(() -> {
            int pageNum = page.orElse(0);
            int sizeNum = size.orElse(20);

            // For now, return all - in production, implement proper pagination
            List<CustomerEntity> customers = customerRepository.findAll();

            // Apply status filter
            if (status.isPresent()) {
                customers = customers.stream()
                    .filter(c -> c.getStatus() == status.get())
                    .collect(Collectors.toList());
            }

            // Apply search filter
            if (search.isPresent() && !search.get().trim().isEmpty()) {
                String query = search.get().toLowerCase();
                customers = customers.stream()
                    .filter(c -> c.getFirstName().toLowerCase().contains(query) ||
                                c.getLastName().toLowerCase().contains(query) ||
                                c.getEmail().toLowerCase().contains(query))
                    .collect(Collectors.toList());
            }

            return customers;
        });
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<CustomerEntity>> searchCustomers(@Argument String query) {
        log.debug("Searching customers with query: {}", query);

        return CompletableFuture.supplyAsync(() -> {
            List<CustomerEntity> customers = customerRepository.findAll();
            String lowerQuery = query.toLowerCase();

            return customers.stream()
                .filter(c -> c.getFirstName().toLowerCase().contains(lowerQuery) ||
                            c.getLastName().toLowerCase().contains(lowerQuery) ||
                            c.getEmail().toLowerCase().contains(lowerQuery) ||
                            (c.getPesel() != null && c.getPesel().contains(query)) ||
                            (c.getNip() != null && c.getNip().contains(query)))
                .collect(Collectors.toList());
        });
    }

    // ========== MUTATION RESOLVERS ==========

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<CustomerEntity> createCustomer(
            @Argument("input") CreateCustomerInput input) {

        log.info("Creating customer with email: {}", input.getEmail());

        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = CustomerEntity.builder()
                .id(UUID.randomUUID())
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .pesel(input.getPesel())
                .nip(input.getNip())
                .email(input.getEmail())
                .phone(input.getPhone())
                .status(input.getStatus() != null ? input.getStatus() : CustomerStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return customerRepository.save(customer);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<CustomerEntity> updateCustomer(
            @Argument UUID id,
            @Argument("input") UpdateCustomerInput input) {

        log.info("Updating customer with id: {}", id);

        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

            if (input.getFirstName() != null) customer.setFirstName(input.getFirstName());
            if (input.getLastName() != null) customer.setLastName(input.getLastName());
            if (input.getPesel() != null) customer.setPesel(input.getPesel());
            if (input.getNip() != null) customer.setNip(input.getNip());
            if (input.getEmail() != null) customer.setEmail(input.getEmail());
            if (input.getPhone() != null) customer.setPhone(input.getPhone());
            if (input.getStatus() != null) customer.setStatus(input.getStatus());
            customer.setUpdatedAt(LocalDateTime.now());

            return customerRepository.save(customer);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<Boolean> deleteCustomer(@Argument UUID id) {
        log.warn("Deleting customer with id: {}", id);

        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

            customer.setDeletedAt(LocalDateTime.now());
            customer.setStatus(CustomerStatus.INACTIVE);
            customerRepository.save(customer);

            return true;
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<CustomerEntity> changeCustomerStatus(
            @Argument UUID id,
            @Argument CustomerStatus status) {

        log.info("Changing customer status to: {} for customer id: {}", status, id);

        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));

            customer.setStatus(status);
            customer.setUpdatedAt(LocalDateTime.now());

            return customerRepository.save(customer);
        });
    }

    // ========== BATCH LOADING FOR N+1 PROBLEM PREVENTION ==========

    @BatchMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<List<InvoiceEntity>>> invoices(List<CustomerEntity> customers) {
        List<UUID> customerIds = customers.stream()
            .map(CustomerEntity::getId)
            .collect(Collectors.toList());

        log.debug("Batch loading invoices for {} customers", customerIds.size());

        return CompletableFuture.supplyAsync(() -> {
            List<InvoiceEntity> allInvoices = invoiceRepository.findByCustomerIdIn(customerIds);

            return customerIds.stream()
                .map(customerId -> allInvoices.stream()
                    .filter(invoice -> invoice.getCustomer().getId().equals(customerId))
                    .collect(Collectors.toList()))
                .collect(Collectors.toList());
        });
    }

    @BatchMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<List<PaymentEntity>>> payments(List<CustomerEntity> customers) {
        List<UUID> customerIds = customers.stream()
            .map(CustomerEntity::getId)
            .collect(Collectors.toList());

        log.debug("Batch loading payments for {} customers", customerIds.size());

        return CompletableFuture.supplyAsync(() -> {
            List<PaymentEntity> allPayments = paymentRepository.findByCustomerIdIn(customerIds);

            return customerIds.stream()
                .map(customerId -> allPayments.stream()
                    .filter(payment -> payment.getInvoice().getCustomer().getId().equals(customerId))
                    .collect(Collectors.toList()))
                .collect(Collectors.toList());
        });
    }

    @BatchMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<List<SubscriptionEntity>>> subscriptions(List<CustomerEntity> customers) {
        List<UUID> customerIds = customers.stream()
            .map(CustomerEntity::getId)
            .collect(Collectors.toList());

        log.debug("Batch loading subscriptions for {} customers", customerIds.size());

        return CompletableFuture.supplyAsync(() -> {
            List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findByCustomerIdIn(customerIds);

            return customerIds.stream()
                .map(customerId -> allSubscriptions.stream()
                    .filter(sub -> sub.getCustomer().getId().equals(customerId))
                    .collect(Collectors.toList()))
                .collect(Collectors.toList());
        });
    }

    // ========== COMPUTED FIELDS ==========

    public CompletableFuture<BigDecimal> getTotalRevenue(CustomerEntity customer) {
        return CompletableFuture.supplyAsync(() -> {
            return paymentRepository.findByCustomerId(customer.getId())
                .stream()
                .filter(p -> p.getStatus() == com.droid.bss.domain.payment.PaymentStatus.COMPLETED)
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        });
    }

    public CompletableFuture<Integer> getActiveSubscriptionsCount(CustomerEntity customer) {
        return CompletableFuture.supplyAsync(() -> {
            return subscriptionRepository.findByCustomerId(customer.getId())
                .stream()
                .filter(sub -> sub.getStatus() == com.droid.bss.domain.subscription.SubscriptionStatus.ACTIVE)
                .mapToInt(sub -> 1)
                .sum();
        });
    }

    public CompletableFuture<Integer> getOverdueInvoicesCount(CustomerEntity customer) {
        return CompletableFuture.supplyAsync(() -> {
            LocalDate today = LocalDate.now();
            return invoiceRepository.findByCustomerId(customer.getId())
                .stream()
                .filter(inv -> inv.getStatus() == com.droid.bss.domain.invoice.InvoiceStatus.OVERDUE)
                .mapToInt(inv -> 1)
                .sum();
        });
    }

    public CompletableFuture<LocalDateTime> getLastPaymentDate(CustomerEntity customer) {
        return CompletableFuture.supplyAsync(() -> {
            return paymentRepository.findByCustomerId(customer.getId())
                .stream()
                .filter(p -> p.getStatus() == com.droid.bss.domain.payment.PaymentStatus.COMPLETED)
                .map(PaymentEntity::getProcessedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        });
    }

    public CompletableFuture<LocalDate> getCustomerSince(CustomerEntity customer) {
        return CompletableFuture.supplyAsync(() -> {
            return customer.getCreatedAt().toLocalDate();
        });
    }

    public CompletableFuture<BigDecimal> getLifetimeValue(CustomerEntity customer) {
        return getTotalRevenue(customer);
    }

    public CompletableFuture<Float> getRiskScore(CustomerEntity customer) {
        return CompletableFuture.supplyAsync(() -> {
            // Simple risk score calculation
            // In production, this would be a more sophisticated algorithm
            int overdueCount = getOverdueInvoicesCount(customer).join();
            int activeSubs = getActiveSubscriptionsCount(customer).join();

            float score = 0.5f; // Base score
            score -= (activeSubs * 0.1f); // More subscriptions = lower risk
            score += (overdueCount * 0.2f); // More overdue = higher risk

            return Math.max(0.0f, Math.min(1.0f, score));
        });
    }

    // ========== INPUT CLASSES ==========

    public static class CreateCustomerInput {
        private String firstName;
        private String lastName;
        private String pesel;
        private String nip;
        private String email;
        private String phone;
        private CustomerStatus status;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPesel() { return pesel; }
        public void setPesel(String pesel) { this.pesel = pesel; }

        public String getNip() { return nip; }
        public void setNip(String nip) { this.nip = nip; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public CustomerStatus getStatus() { return status; }
        public void setStatus(CustomerStatus status) { this.status = status; }
    }

    public static class UpdateCustomerInput {
        private String firstName;
        private String lastName;
        private String pesel;
        private String nip;
        private String email;
        private String phone;
        private CustomerStatus status;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPesel() { return pesel; }
        public void setPesel(String pesel) { this.pesel = pesel; }

        public String getNip() { return nip; }
        public void setNip(String nip) { this.nip = nip; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public CustomerStatus getStatus() { return status; }
        public void setStatus(CustomerStatus status) { this.status = status; }
    }
}
