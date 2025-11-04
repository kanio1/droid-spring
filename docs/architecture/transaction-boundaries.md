# Transaction Boundaries Documentation
## BSS System - Sprint 1

**Document Owner:** Backend Developer
**Date:** 2025-10-29
**Version:** 1.0
**Status:** DRAFT

---

## EXECUTIVE SUMMARY

This document defines the critical transaction boundaries for the BSS system to ensure data consistency and integrity across complex operations involving multiple entities and external systems.

**Key Principle:** All transactions must be atomic, consistent, isolated, and durable (ACID).

---

## CRITICAL TRANSACTION BOUNDARIES

### Transaction #1: Order Creation
**Purpose:** Create a new customer order with multiple items atomically

**Scope:**
- Create Order entity
- Create OrderItems entities (multiple)
- Update product availability (if applicable)
- Publish OrderCreatedEvent

**Boundary Level:** REQUIRES_NEW (new transaction)

**Implementation:**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Order createOrder(CreateOrderCommand command) {
    // Validate customer
    Customer customer = customerRepository.findById(command.getCustomerId());
    validateCustomer(customer);

    // Create order
    Order order = Order.builder()
        .customerId(customer.getId())
        .orderType(command.getOrderType())
        .status(OrderStatus.PENDING)
        .orderNumber(generateOrderNumber())
        .build();

    Order savedOrder = orderRepository.save(order);

    // Create order items
    List<OrderItem> items = command.getItems().stream()
        .map(itemCommand -> OrderItem.builder()
            .orderId(savedOrder.getId())
            .productId(itemCommand.getProductId())
            .quantity(itemCommand.getQuantity())
            .unitPrice(itemCommand.getUnitPrice())
            .totalPrice(itemCommand.getUnitPrice() * itemCommand.getQuantity())
            .build())
        .collect(Collectors.toList());

    orderItemRepository.saveAll(items);

    // Publish event (Outbox pattern - will be sent separately)
    outboxService.save(OrderCreatedEvent.builder()
        .orderId(savedOrder.getId())
        .customerId(customer.getId())
        .orderNumber(savedOrder.getOrderNumber())
        .totalAmount(savedOrder.getTotalAmount())
        .build());

    return savedOrder;
}
```

**Rollback Strategy:**
- If any step fails, entire transaction rolls back
- No partial order creation allowed
- Customer sees no order created

**Isolation Level:** READ_COMMITTED

---

### Transaction #2: Order Approval
**Purpose:** Approve an order and transition to IN_PROGRESS

**Scope:**
- Update Order status to APPROVED
- Validate business rules (credit check, availability)
- Update order timeline

**Boundary Level:** REQUIRED (join existing or create new)

**Implementation:**
```java
@Transactional
public Order approveOrder(UUID orderId, UUID customerId, String approvedBy) {
    Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    validateOrderCanBeApproved(order);

    order.setStatus(OrderStatus.APPROVED);
    order.setUpdatedBy(approvedBy);
    order.setPromisedDate(calculatePromisedDate());

    Order approvedOrder = orderRepository.save(order);

    // Add timeline entry
    orderTimelineService.addTimelineEntry(orderId, "ORDER_APPROVED", approvedBy);

    return approvedOrder;
}
```

**Rollback Strategy:**
- If validation fails, transaction rolls back
- Order remains in previous status

---

### Transaction #3: Order Fulfillment (Saga Orchestration)
**Purpose:** Execute the order fulfillment saga with compensation on failure

**Saga Flow:**
1. OrderCompletedEvent received
2. Create Subscription from Order
3. Generate Initial Invoice
4. Publish SubscriptionActivatedEvent

**Boundary Level:** REQUIRED with compensation

**Implementation:**
```java
@Component
@Transactional
public class OrderFulfillmentSaga {

    @EventListener
    @Async
    public void handleOrderCompleted(OrderCompletedEvent event) {
        UUID orderId = event.getOrderId();
        UUID customerId = event.getCustomerId();

        SagaState sagaState = SagaState.builder()
            .orderId(orderId)
            .currentStep("SUBSCRIPTION_CREATION")
            .status(SagaStatus.IN_PROGRESS)
            .build();

        sagaStateRepository.save(sagaState);

        try {
            // Step 1: Create subscription
            Subscription subscription = subscriptionService.createFromOrder(orderId);
            updateSagaStep(sagaState, "INVOICE_GENERATION");

            // Step 2: Generate invoice
            Invoice invoice = invoiceService.generateForSubscription(subscription.getId());
            updateSagaStep(sagaState, "COMPLETED");

            // Mark saga as completed
            sagaState.setStatus(SagaStatus.COMPLETED);
            sagaStateRepository.save(sagaState);

        } catch (Exception e) {
            // Compensation logic
            compensate(sagaState, e);
        }
    }

    private void compensate(SagaState sagaState, Exception error) {
        // Rollback subscription
        if (sagaState.getSubscriptionId() != null) {
            subscriptionService.cancel(sagaState.getSubscriptionId(), "SAGA_COMPENSATION");
        }

        // Rollback invoice
        if (sagaState.getInvoiceId() != null) {
            invoiceService.cancel(sagaState.getInvoiceId(), "SAGA_COMPENSATION");
        }

        // Update saga status
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setErrorMessage(error.getMessage());
        sagaStateRepository.save(sagaState);

        // Publish compensation event
        eventPublisher.publish(new OrderCompensationEvent(
            sagaState.getOrderId(),
            error.getMessage()
        ));
    }
}
```

**Rollback Strategy:**
- Use Saga pattern with explicit compensation
- Each step has inverse operation
- Failed saga marks customer service for manual intervention

---

### Transaction #4: Subscription Activation
**Purpose:** Activate a subscription from a completed order

**Scope:**
- Create Subscription entity
- Link to customer and product
- Set billing cycle
- Update subscription status to ACTIVE
- Publish SubscriptionActivatedEvent

**Boundary Level:** REQUIRES_NEW

**Implementation:**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Subscription activateSubscription(UUID orderId, UUID productId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    if (order.getStatus() != OrderStatus.COMPLETED) {
        throw new IllegalStateException("Order must be COMPLETED to activate subscription");
    }

    // Check if subscription already exists
    Optional<Subscription> existing = subscriptionRepository
        .findByOrderId(orderId);
    if (existing.isPresent()) {
        return existing.get(); // Idempotent
    }

    // Create subscription
    Subscription subscription = Subscription.builder()
        .customerId(order.getCustomerId())
        .productId(productId)
        .orderId(orderId)
        .status(SubscriptionStatus.ACTIVE)
        .startDate(LocalDate.now())
        .billingStartDate(LocalDate.now())
        .billingPeriod("MONTHLY")
        .price(calculateSubscriptionPrice(productId))
        .autoRenew(true)
        .build();

    Subscription savedSubscription = subscriptionRepository.save(subscription);

    // Publish event
    outboxService.save(SubscriptionActivatedEvent.builder()
        .subscriptionId(savedSubscription.getId())
        .customerId(savedSubscription.getCustomerId())
        .productId(savedSubscription.getProductId())
        .build());

    return savedSubscription;
}
```

**Rollback Strategy:**
- If subscription creation fails, transaction rolls back
- Order remains COMPLETED but subscription is not activated
- Manual intervention required to retry

---

### Transaction #5: Invoice Generation
**Purpose:** Generate invoice from subscription billing cycle

**Scope:**
- Create Invoice entity
- Create InvoiceItems from subscription
- Calculate totals (subtotal, tax, total)
- Set due date
- Publish InvoiceGeneratedEvent

**Boundary Level:** REQUIRES_NEW

**Implementation:**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Invoice generateInvoice(UUID subscriptionId) {
    Subscription subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

    // Check if invoice already exists for this period
    LocalDate billingPeriodStart = subscription.getBillingStartDate();
    LocalDate billingPeriodEnd = billingPeriodStart.plusMonths(1);

    Optional<Invoice> existing = invoiceRepository
        .findBySubscriptionIdAndPeriod(subscriptionId, billingPeriodStart, billingPeriodEnd);

    if (existing.isPresent()) {
        return existing.get(); // Idempotent
    }

    // Create invoice
    Invoice invoice = Invoice.builder()
        .customerId(subscription.getCustomerId())
        .subscriptionId(subscriptionId)
        .invoiceType(InvoiceType.RECURRING)
        .status(InvoiceStatus.DRAFT)
        .issueDate(LocalDate.now())
        .dueDate(LocalDate.now().plusDays(14))
        .billingPeriodStart(billingPeriodStart)
        .billingPeriodEnd(billingPeriodEnd)
        .currency(subscription.getCurrency())
        .build();

    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Create invoice item
    InvoiceItem item = InvoiceItem.builder()
        .invoiceId(savedInvoice.getId())
        .subscriptionId(subscriptionId)
        .itemType(InvoiceItemType.SUBSCRIPTION)
        .description("Monthly subscription fee")
        .quantity(1)
        .unitPrice(subscription.getPrice())
        .netAmount(subscription.getPrice())
        .taxRate(BigDecimal.valueOf(23.00)) // VAT
        .build();

    // Calculate tax
    BigDecimal taxAmount = subscription.getPrice()
        .multiply(BigDecimal.valueOf(0.23));
    item.setTaxAmount(taxAmount);
    item.setTotalAmount(subscription.getPrice().add(taxAmount));

    invoiceItemRepository.save(item);

    // Update invoice totals
    savedInvoice.setSubtotal(subscription.getPrice());
    savedInvoice.setTaxAmount(taxAmount);
    savedInvoice.setTotalAmount(subscription.getPrice().add(taxAmount));

    Invoice finalizedInvoice = invoiceRepository.save(savedInvoice);

    // Update next billing date
    subscription.setNextBillingDate(billingPeriodEnd.plusMonths(1));
    subscriptionRepository.save(subscription);

    // Publish event
    outboxService.save(InvoiceGeneratedEvent.builder()
        .invoiceId(finalizedInvoice.getId())
        .subscriptionId(subscriptionId)
        .totalAmount(finalizedInvoice.getTotalAmount())
        .build());

    return finalizedInvoice;
}
```

**Rollback Strategy:**
- If any step fails, entire transaction rolls back
- No partial invoices allowed
- Next billing date remains unchanged

---

### Transaction #6: Payment Processing
**Purpose:** Record payment against invoice

**Scope:**
- Create Payment entity
- Link to invoice
- Update invoice status to PAID (if fully paid)
- Record transaction details
- Publish PaymentReceivedEvent

**Boundary Level:** REQUIRES_NEW

**Implementation:**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public Payment processPayment(RecordPaymentRequest request) {
    Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
        .orElseThrow(() -> new InvoiceNotFoundException(request.getInvoiceId()));

    // Validate payment amount
    BigDecimal remainingAmount = invoice.getTotalAmount()
        .subtract(getTotalPaid(invoice.getId()));

    if (request.getAmount().compareTo(remainingAmount) > 0) {
        throw new PaymentAmountExceededException("Payment exceeds remaining amount");
    }

    // Create payment record
    Payment payment = Payment.builder()
        .customerId(invoice.getCustomerId())
        .invoiceId(invoice.getId())
        .amount(request.getAmount())
        .currency(request.getCurrency())
        .paymentMethod(request.getPaymentMethod())
        .paymentStatus(PaymentStatus.COMPLETED)
        .paymentDate(LocalDate.now())
        .transactionId(request.getTransactionId())
        .gateway(request.getGateway())
        .referenceNumber(request.getReferenceNumber())
        .build();

    Payment savedPayment = paymentRepository.save(payment);

    // Check if invoice is fully paid
    BigDecimal newTotalPaid = getTotalPaid(invoice.getId());
    if (newTotalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
        // Invoice fully paid
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(LocalDate.now());
        invoiceRepository.save(invoice);
    }

    // Publish event
    outboxService.save(PaymentReceivedEvent.builder()
        .paymentId(savedPayment.getId())
        .invoiceId(invoice.getId())
        .amount(savedPayment.getAmount())
        .paymentMethod(savedPayment.getPaymentMethod())
        .build());

    return savedPayment;
}

private BigDecimal getTotalPaid(UUID invoiceId) {
    return paymentRepository.findByInvoiceId(invoiceId).stream()
        .map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**Rollback Strategy:**
- If payment processing fails, transaction rolls back
- Payment status remains unchanged
- Invoice status remains unchanged

---

## TRANSACTION ISOLATION LEVELS

### Default Isolation: READ_COMMITTED
- Prevents dirty reads
- Allows non-repeatable reads
- Acceptable for most operations

### Higher Isolation: REPEATABLE_READ
- Used for: Invoice generation
- Prevents non-repeatable reads
- Ensures consistent totals during calculation

---

## OPTIMISTIC LOCKING

**Implementation:** All entities have `version` field

```java
@Entity
public abstract class BaseEntity {
    @Version
    @Column(name = "version")
    private Long version;
}
```

**Purpose:**
- Detect concurrent modifications
- Prevent lost updates
- Fail fast on conflicts

---

## OUTBOX PATTERN

**Purpose:** Reliable event publishing

**Implementation:**
1. Business transaction commits to database
2. Event saved to outbox table (same transaction)
3. Separate process publishes events to Kafka
4. Events marked as published
5. Failed events retried

**Benefits:**
- Guaranteed delivery
- No event loss
- Transactional consistency

---

## SAGMA PATTERN SUMMARY

### Saga Orchestrator Pattern Used For:
- Order → Subscription → Invoice flow
- Long-running transactions
- Distributed operations

### Saga State Tracking:
```java
@Entity
public class SagaState {
    private UUID orderId;
    private String currentStep;
    private SagaStatus status;
    private String errorMessage;
    private Map<String, Object> data;
}
```

### Compensation Strategies:
- Subscription activation failed → Cancel subscription
- Invoice generation failed → Mark subscription for manual review
- Payment failed → Keep invoice as outstanding

---

## ERROR HANDLING

### Propagation Levels
- **REQUIRED:** Default, join existing or create new
- **REQUIRES_NEW:** Always create new transaction, suspend existing
- **NESTED:** Create savepoint within existing transaction

### Retry Logic
- Transient failures (network, database connection): Retry 3 times
- Business rule failures: No retry, return error
- Deadlocks: Exponential backoff, retry 5 times

### Monitoring
- Track transaction duration
- Alert on slow transactions (>5 seconds)
- Log all rollbacks
- Monitor saga failure rate

---

## PERFORMANCE CONSIDERATIONS

### Transaction Duration
- Keep transactions as short as possible
- Avoid long-running operations
- No user interaction within transactions

### Batch Operations
- Process in chunks (1000 records)
- Use batch inserts
- Commit frequently for large imports

### Connection Management
- Use connection pooling
- Monitor pool usage
- Release connections promptly

---

## TESTING STRATEGY

### Unit Tests
- Test transaction boundaries
- Test rollback scenarios
- Test optimistic locking
- Test concurrency

### Integration Tests
- Test with real database (Testcontainers)
- Test transaction propagation
- Test outbox pattern
- Test saga orchestration

### Load Tests
- Concurrent order creation
- High-volume invoice generation
- Payment processing under load

---

## DEFINITION OF DONE

### All transactions must:
- [ ] Have clear boundaries defined
- [ ] Include rollback strategy
- [ ] Have optimistic locking
- [ ] Publish events via outbox
- [ ] Include comprehensive tests
- [ ] Document performance characteristics
- [ ] Include monitoring/alerting
- [ ] Pass integration tests

---

**Document Status:** ✅ Ready for Review
**Next Steps:**
1. Review with Tech Lead
2. Implement @Transactional annotations
3. Create unit tests for transaction boundaries
4. Verify with integration tests

---

**Author:** Backend Developer
**Reviewer:** Tech Lead
**Last Updated:** 2025-10-29
