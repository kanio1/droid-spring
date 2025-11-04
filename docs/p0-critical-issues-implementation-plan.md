# Sprint 1 - Critical P0 Issues
## Detailed Implementation Blueprint

**Document Owner:** Scrum Master
**Date:** 2025-10-29
**Purpose:** Technical implementation guide for 4 MUST-FIX issues
**Status:** Ready for Development Team
**Version:** 1.0

---

## EXECUTIVE SUMMARY

**MUST FIX Definition:** Issues that will cause business failure, data corruption, or regulatory violations if not addressed before sprint completion.

**Implementation Approach:** All 4 issues are **foundational infrastructure** that must be completed **before or during** API development.

**Total Effort:** 5 days
**Critical Path:** YES - All issues on critical path
**Impact if Deferred:** CRITICAL - Sprint 1 failure likely

---

## P0 ISSUE #1: TRANSACTION BOUNDARIES

### Risk Assessment
**Business Risk:** CRITICAL
- Data corruption possible
- Inconsistent state across entities
- Failed operations leave partial data
- Hard to debug and recover

**Technical Debt Impact:** CRITICAL
- Transactions issues are hard to retrofit
- Data corruption requires manual intervention
- Performance impact of long transactions
- Concurrency issues increase

### Implementation Specifications

#### 1.1 Transaction Strategy Design

**Local Transactions (Single Database):**
```java
@Transactional(propagation = Propagation.REQUIRED)
public Order createOrder(CreateOrderCommand command) {
    Order order = orderRepository.save(command.getOrder());
    List<OrderItem> items = createOrderItems(command);
    orderItemRepository.saveAll(items);
    return order;
}
```

**Distributed Transactions (Saga Pattern):**
```java
@Transactional
public void startOrderFulfillment(UUID orderId) {
    // Step 1: Create subscription
    Subscription subscription = subscriptionService.createFromOrder(orderId);
    // Step 2: Generate invoice
    Invoice invoice = invoiceService.generateForSubscription(subscription.getId());
    // Step 3: Publish event
    eventPublisher.publish(new OrderFulfillmentCompletedEvent(orderId));
}
```

#### 1.2 Transaction Boundaries Definition

**Transaction #1: Order Creation**
```java
@Service
public class OrderService {

    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        // Atomic operations:
        // 1. Validate customer
        Customer customer = customerRepository.findById(command.getCustomerId());
        validateCustomer(customer);

        // 2. Create order
        Order order = Order.builder()
            .customerId(customer.getId())
            .orderType(command.getOrderType())
            .status(OrderStatus.PENDING)
            .build();

        Order savedOrder = orderRepository.save(order);

        // 3. Create order items
        List<OrderItem> items = command.getItems().stream()
            .map(itemCommand -> OrderItem.builder()
                .orderId(savedOrder.getId())
                .productId(itemCommand.getProductId())
                .quantity(itemCommand.getQuantity())
                .unitPrice(itemCommand.getUnitPrice())
                .build())
            .collect(Collectors.toList());

        orderItemRepository.saveAll(items);

        // 4. Publish event (OUTBOX pattern)
        outboxService.save(OrderCreatedEvent.builder()
            .orderId(savedOrder.getId())
            .customerId(customer.getId())
            .build());

        return savedOrder;
    }
}
```

**Transaction #2: Subscription Activation**
```java
@Transactional
public Subscription activateSubscription(UUID subscriptionId, UUID orderId) {
    // Validate order completed
    Order order = orderRepository.findById(orderId);
    if (order.getStatus() != OrderStatus.COMPLETED) {
        throw new IllegalStateException("Order must be completed");
    }

    // Create subscription
    Subscription subscription = Subscription.builder()
        .customerId(order.getCustomerId())
        .orderId(order.getId())
        .status(SubscriptionStatus.ACTIVE)
        .startDate(LocalDate.now())
        .build();

    Subscription savedSubscription = subscriptionRepository.save(subscription);

    // Update order status
    order.setStatus(OrderStatus.FULFILLED);
    orderRepository.save(order);

    // Publish event
    outboxService.save(SubscriptionActivatedEvent.builder()
        .subscriptionId(savedSubscription.getId())
        .orderId(orderId)
        .build());

    return savedSubscription;
}
```

**Transaction #3: Invoice Generation**
```java
@Transactional
public Invoice generateInvoice(UUID subscriptionId) {
    Subscription subscription = subscriptionRepository.findById(subscriptionId);

    // Create invoice
    Invoice invoice = Invoice.builder()
        .customerId(subscription.getCustomerId())
        .subscriptionId(subscriptionId)
        .issueDate(LocalDate.now())
        .status(InvoiceStatus.DRAFT)
        .build();

    Invoice savedInvoice = invoiceRepository.save(invoice);

    // Create invoice items
    InvoiceItem item = InvoiceItem.builder()
        .invoiceId(savedInvoice.getId())
        .subscriptionId(subscriptionId)
        .description("Monthly subscription fee")
        .amount(subscription.getPrice())
        .build();

    invoiceItemRepository.save(item);

    // Calculate totals
    BigDecimal subtotal = calculateSubtotal(savedInvoice);
    BigDecimal tax = calculateTax(subtotal);
    BigDecimal total = subtotal.add(tax);

    savedInvoice.setSubtotal(subtotal);
    savedInvoice.setTaxAmount(tax);
    savedInvoice.setTotalAmount(total);

    invoiceRepository.save(savedInvoice);

    // Publish event
    outboxService.save(InvoiceGeneratedEvent.builder()
        .invoiceId(savedInvoice.getId())
        .subscriptionId(subscriptionId)
        .build());

    return savedInvoice;
}
```

#### 1.3 Saga Orchestrator (Basic)

```java
@Component
public class OrderFulfillmentSaga {

    private final OutboxEventRepository outboxEventRepository;

    @EventListener
    @Async
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            // Start subscription creation
            Subscription subscription = subscriptionService.createFromOrder(event.getOrderId());

            // Update saga state
            SagaState sagaState = SagaState.builder()
                .orderId(event.getOrderId())
                .subscriptionId(subscription.getId())
                .currentStep("SUBSCRIPTION_CREATED")
                .status(SagaStatus.IN_PROGRESS)
                .build();

            sagaStateRepository.save(sagaState);

            // Continue to invoice generation
            invoiceService.generateInvoice(subscription.getId());

        } catch (Exception e) {
            // Compensation logic
            compensate(event.getOrderId());
        }
    }

    private void compensate(UUID orderId) {
        // Rollback subscription
        // Cancel invoice
        // Publish OrderCancelledEvent
        // Update saga status to FAILED
    }
}
```

#### 1.4 Transaction Isolation Levels

**Default Isolation Level:** READ_COMMITTED
```sql
-- For order creation (concurrent reads OK)
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- For invoice generation (needs consistent totals)
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

#### 1.5 Optimistic Locking

```java
@Entity
public class Order {
    @Version
    private Long version;

    private OrderStatus status;

    // Updates will fail if version changed
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }
}
```

### Implementation Timeline

**Day 1 (Sprint Week 1):**
- [ ] Document all transaction boundaries (4 hours)
- [ ] Create transaction diagram (2 hours)
- [ ] Add @Transactional to repository layer (2 hours)

**Day 2-3 (During API development):**
- [ ] Add @Transactional to OrderService methods
- [ ] Add @Transactional to SubscriptionService methods
- [ ] Add @Transactional to InvoiceService methods
- [ ] Add @Transactional to SagaOrchestrator

**Testing:**
- [ ] Unit tests for transaction propagation
- [ ] Integration tests for rollback scenarios
- [ ] Concurrency tests for optimistic locking
- [ ] Performance tests for transaction overhead

### Definition of Done

- [ ] All repository methods have @Transactional or explicit boundaries
- [ ] Order creation is atomic (order + items)
- [ ] Invoice generation is atomic (invoice + items)
- [ ] Saga orchestrator handles failures with compensation
- [ ] All transactions have rollback strategy documented
- [ ] Optimistic locking implemented for all entities
- [ ] Transaction isolation levels configured
- [ ] Integration tests pass (TestContainers)
- [ ] Performance impact <5% overhead
- [ ] Documentation complete

---

## P0 ISSUE #2: AUDIT COLUMNS

### Risk Assessment
**Business Risk:** HIGH
- No audit trail for regulatory compliance
- Cannot track who made changes
- Hard to debug issues
- GDPR compliance risk

**Technical Debt Impact:** MEDIUM
- Retrofit audit columns is harder
- Requires data migration
- Application-level tracking needed

### Implementation Specifications

#### 2.1 Audit Entity Base Class

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    // Getters and setters
}
```

#### 2.2 Audit Configuration

```java
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SecurityAuditorAware();
    }
}

public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }
        return Optional.of(authentication.getName());
    }
}
```

#### 2.3 Product Entity with Audit

```java
@Entity
@Table(name = "products")
public class ProductEntity extends BaseEntity {

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    // Relationships
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductFeatureEntity> features = new ArrayList<>();
}
```

#### 2.4 Audit Migration Script

```sql
-- Add audit columns to products table
ALTER TABLE products
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add audit columns to orders table
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add audit columns to subscriptions table
ALTER TABLE subscriptions
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add audit columns to invoices table
ALTER TABLE invoices
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add audit columns to payments table
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
```

#### 2.5 Audit Query Examples

```java
// Find all products created by specific user
@Query("SELECT p FROM ProductEntity p WHERE p.createdBy = :userId")
List<ProductEntity> findByCreatedBy(@Param("userId") String userId);

// Find products modified in date range
@Query("SELECT p FROM ProductEntity p WHERE p.updatedAt BETWEEN :startDate AND :endDate")
List<ProductEntity> findByUpdatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

// Find orders by version (optimistic locking check)
@Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.version = :version")
Optional<OrderEntity> findByIdAndVersion(@Param("id") UUID id, @Param("version") Long version);

// Audit log query - all changes to entity
@Query("SELECT e FROM ProductEntity e WHERE e.id = :id ORDER BY e.version DESC")
List<ProductEntity> findAuditTrail(@Param("id") UUID id);
```

### Implementation Timeline

**Day 1 (Sprint Week 1):**
- [ ] Create BaseEntity audit superclass (2 hours)
- [ ] Create AuditConfig configuration (1 hour)
- [ ] Create SecurityAuditorAware (1 hour)
- [ ] Add audit columns to DB-1 migrations (3 hours)

**Day 2-3 (During DB-2):**
- [ ] Update ProductEntity to extend BaseEntity
- [ ] Update OrderEntity to extend BaseEntity
- [ ] Update SubscriptionEntity to extend BaseEntity
- [ ] Update InvoiceEntity to extend BaseEntity
- [ ] Update PaymentEntity to extend BaseEntity

**Testing:**
- [ ] Unit test for auditor provider
- [ ] Integration test for audit column population
- [ ] Test audit trail query
- [ ] Verify version control (optimistic locking)

### Definition of Done

- [ ] All entities extend BaseEntity
- [ ] All tables have audit columns
- [ ] AuditorAware returns authenticated user
- [ ] createdAt/updatedAt auto-populated
- [ ] createdBy/updatedBy populated with user ID
- [ ] version column for optimistic locking
- [ ] Audit trail query works
- [ ] Integration tests pass
- [ ] Documentation of audit strategy complete

---

## P0 ISSUE #3: ROW-LEVEL SECURITY

### Risk Assessment
**Business Risk:** CRITICAL
- Data breach risk (customer sees other customers' data)
- GDPR violation (personal data exposure)
- Legal liability
- Reputational damage

**Technical Debt Impact:** CRITICAL
- Security issues are hardest to retrofit
- Data already exposed before fix
- Requires extensive testing

### Implementation Specifications

#### 3.1 Security Service

```java
@Service
public class SecurityService {

    @Autowired
    private AuthenticationService authenticationService;

    public boolean isOwner(UUID resourceCustomerId, String authenticatedUserId) {
        return resourceCustomerId.toString().equals(authenticatedUserId);
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    public String getCurrentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // Assuming username is customer ID
    }
}
```

#### 3.2 Security Configuration

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    @Bean
    public SecurityService securityService() {
        return new SecurityService();
    }
}
```

#### 3.3 Product Security (Read-Only for All Authenticated)

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Public read access for all authenticated users
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('AGENT') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        List<ProductDto> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // Public read access for all authenticated users
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('AGENT') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable UUID id) {
        ProductDto product = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    // Admin only for write operations
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductDto product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
```

#### 3.4 Order Security (Customer Scoped)

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Customer can only see their own orders
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getCustomerOrders(
            @RequestParam UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        List<OrderDto> orders = orderService.getOrdersByCustomerId(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    // Customer can only see their own orders
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            @RequestParam UUID customerId,
            @PathVariable UUID id) {
        OrderDto order = orderService.getOrderByIdAndCustomerId(id, customerId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // Customer can create orders for themselves
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#request.customerId, authentication.name)")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    // Agent can manage any customer's orders
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam UUID customerId,
            @RequestBody UpdateOrderStatusRequest request) {
        OrderDto order = orderService.updateOrderStatus(id, customerId, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
```

#### 3.5 Subscription Security

```java
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    // Customer can only see their own subscriptions
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionDto>>> getCustomerSubscriptions(
            @RequestParam UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        List<SubscriptionDto> subscriptions = subscriptionService
            .getSubscriptionsByCustomerId(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(subscriptions));
    }

    // Customer can only see their own subscription
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionDto>> getSubscription(
            @RequestParam UUID customerId,
            @PathVariable UUID id) {
        SubscriptionDto subscription = subscriptionService
            .getSubscriptionByIdAndCustomerId(id, customerId);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    // Customer can suspend their own subscription
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<SubscriptionDto>> suspendSubscription(
            @RequestParam UUID customerId,
            @PathVariable UUID id,
            @RequestParam String reason) {
        SubscriptionDto subscription = subscriptionService.suspendSubscription(id, customerId, reason);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }

    // Agent/Admin can manage any subscription
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionDto>> cancelSubscription(
            @PathVariable UUID id,
            @RequestParam UUID customerId,
            @RequestParam String reason) {
        SubscriptionDto subscription = subscriptionService.cancelSubscription(id, customerId, reason);
        return ResponseEntity.ok(ApiResponse.success(subscription));
    }
}
```

#### 3.6 Invoice Security

```java
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    // Customer can only see their own invoices
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getCustomerInvoices(
            @RequestParam UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByCustomerId(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    // Customer can only see their own invoice
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoice(
            @RequestParam UUID customerId,
            @PathVariable UUID id) {
        InvoiceDto invoice = invoiceService.getInvoiceByIdAndCustomerId(id, customerId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    // Billing role only for invoice generation
    @PreAuthorize("hasRole('BILLING') or hasRole('ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<InvoiceDto>> generateInvoice(
            @RequestParam UUID customerId,
            @RequestParam UUID subscriptionId) {
        InvoiceDto invoice = invoiceService.generateInvoice(customerId, subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
```

#### 3.7 Repository-Level Security (Additional Layer)

```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    // Security enforced at query level
    @Query("SELECT o FROM OrderEntity o WHERE o.customerId = :customerId")
    List<OrderEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.customerId = :customerId")
    Optional<OrderEntity> findByIdAndCustomerId(@Param("id") UUID id,
                                               @Param("customerId") UUID customerId);
}
```

### Implementation Timeline

**Day 1 (Sprint Week 1):**
- [ ] Create SecurityService bean (2 hours)
- [ ] Configure MethodSecurityConfig (1 hour)
- [ ] Review API endpoints for security requirements (2 hours)
- [ ] Document security model (3 hours)

**Day 2-5 (During API development):**
- [ ] Add @PreAuthorize to ProductController (1 hour)
- [ ] Add @PreAuthorize to OrderController (2 hours)
- [ ] Add @PreAuthorize to SubscriptionController (2 hours)
- [ ] Add @PreAuthorize to InvoiceController (2 hours)
- [ ] Update repositories with customer scoping (1 hour)

**Testing:**
- [ ] Unit tests for SecurityService
- [ ] Integration tests for @PreAuthorize
- [ ] Security tests: Customer can only access own data
- [ ] Security tests: Admin can access all data
- [ ] Security tests: Unauthorized access denied

### Definition of Done

- [ ] SecurityService bean created and tested
- [ ] @PreAuthorize on all controller methods
- [ ] Customer-scoped queries (customerId in WHERE)
- [ ] Admin/Agent role checks for management operations
- [ ] Billing role checks for sensitive operations
- [ ] All endpoints tested for authorization
- [ ] Unauthorized access returns 403
- [ ] Integration tests pass for all security scenarios
- [ ] Security documentation complete

---

## P0 ISSUE #4: PCI COMPLIANCE (BASICS)

### Risk Assessment
**Business Risk:** CRITICAL
- Legal liability for data breach
- Fines from payment processors
- Loss of payment processing ability
- Reputational damage

**Technical Debt Impact:** CRITICAL
- Compliance retrofits are expensive
- Need to re-validate entire payment flow
- May need third-party audit

### Implementation Specifications

#### 4.1 Data Encryption

**Payment Entity Encryption:**
```java
@Entity
@Table(name = "payments")
public class PaymentEntity extends BaseEntity {

    @Column(name = "payment_number", unique = true)
    private String paymentNumber;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    // Sensitive data - encrypt at rest
    @Convert(converter = EncryptionConverter.class)
    @Column(name = "transaction_id", length = 500)
    private String transactionId; // From payment gateway

    @Convert(converter = EncryptionConverter.class)
    @Column(name = "reference_number", length = 500)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;
}
```

**Encryption Converter:**
```java
@Component
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String ENCRYPTION_KEY = System.getenv("PAYMENT_DATA_KEY");

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            // Use AES encryption
            Key key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Key key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

#### 4.2 Data Masking in Logs

**Logback Configuration:**
```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>/var/log/bss/payment.log</file>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <!-- Mask sensitive fields -->
    <turboFilter class="ch.qos.logback.classic.turbo.MarkerFilter">
        <marker>SENSITIVE</marker>
        <onMatch>DENY</onMatch>
    </turboFilter>
</configuration>
```

**Masking Utility:**
```java
@Component
public class DataMaskingUtil {

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("([Tt]ransaction[_-]?[Ii]d[\\s:]*)(\\w+)");

    public String maskCardNumber(String logMessage) {
        return CARD_NUMBER_PATTERN.matcher(logMessage)
            .replaceAll("****-****-****-****");
    }

    public String maskTransactionId(String logMessage) {
        return TRANSACTION_ID_PATTERN.matcher(logMessage)
            .replaceAll("$1****");
    }

    public String maskPaymentData(String message) {
        if (message == null) {
            return null;
        }
        message = maskCardNumber(message);
        message = maskTransactionId(message);
        return message;
    }
}
```

**Payment Service with Masking:**
```java
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private DataMaskingUtil maskingUtil;

    @EventListener
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        String transactionId = event.getTransactionId();
        logger.info("Payment created with transaction ID: {}", maskingUtil.maskTransactionId(transactionId));
    }

    public PaymentDto recordPayment(RecordPaymentRequest request) {
        try {
            PaymentEntity payment = createPayment(request);
            logger.info("Payment recorded: amount={}, method={}, status={}",
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus());
            return convertToDto(payment);
        } catch (Exception e) {
            logger.error("Payment recording failed for customer: {}", request.getCustomerId(), e);
            throw e;
        }
    }
}
```

#### 4.3 Audit Logging

**Audit Event:**
```java
@AuditEvent(eventType = "PAYMENT_RECORDED")
public PaymentDto recordPayment(RecordPaymentRequest request) {
    // Implementation
}

public @interface AuditEvent {
    String eventType() default "";
    String description() default "";
}
```

**Audit Service:**
```java
@Service
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    public void logPaymentEvent(String eventType, UUID paymentId, UUID customerId,
                               String paymentMethod, BigDecimal amount,
                               String status, String userId) {
        Map<String, Object> auditData = Map.of(
            "eventType", eventType,
            "paymentId", paymentId,
            "customerId", customerId,
            "paymentMethod", paymentMethod,
            "amount", amount,
            "status", status,
            "userId", userId,
            "timestamp", Instant.now().toString(),
            "traceId", MDC.get("traceId")
        );
        auditLogger.info("PAYMENT_AUDIT: {}", auditData);
    }
}
```

#### 4.4 Access Control

**Payment Access Roles:**
```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // Customer can only see their own payments
    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getCustomerPayments(
            @RequestParam UUID customerId) {
        List<PaymentDto> payments = paymentService.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    // Billing role only for recording payments
    @PreAuthorize("hasRole('BILLING') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDto>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {
        PaymentDto payment = paymentService.recordPayment(request);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    // Admin can view all payment data
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getAllPayments() {
        List<PaymentDto> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
```

#### 4.5 TLS Configuration

**Application Properties:**
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: bss-payment
  http2:
    enabled: true
  tomcat:
    protocol-header: X-Forwarded-Proto
    remote-ip-header: X-Forwarded-For

# Force HTTPS
spring:
  security:
    require-ssl: true
```

#### 4.6 PCI Compliance Checklist

**Technical Requirements:**
- [x] Data encryption at rest (AES-256)
- [x] Data encryption in transit (TLS 1.3)
- [x] Data masking in logs
- [x] Audit logging for all payment operations
- [x] Access control (role-based)
- [x] Secure key management (environment variables)
- [x] Secure password storage (Keycloak)
- [ ] Vulnerability scanning (Sprint 2)
- [ ] Penetration testing (Sprint 2)
- [ ] SAQ completion (Sprint 2)

### Implementation Timeline

**Day 1 (Sprint Week 1):**
- [ ] Create EncryptionConverter (2 hours)
- [ ] Add @Convert to PaymentEntity (1 hour)
- [ ] Create DataMaskingUtil (2 hours)
- [ ] Configure log masking (2 hours)
- [ ] Create AuditService (1 hour)

**Day 2-3 (During API development):**
- [ ] Add @AuditEvent to PaymentService methods
- [ ] Add @PreAuthorize to PaymentController
- [ ] Add encryption to invoice.pdf_url (1 hour)
- [ ] Configure TLS in application properties (1 hour)

**Testing:**
- [ ] Unit tests for EncryptionConverter
- [ ] Test data masking in logs
- [ ] Integration tests for payment authorization
- [ ] Verify encrypted data at rest
- [ ] Verify audit log contains required fields

### Definition of Done

- [ ] Payment data encrypted at rest (AES-256)
- [ ] Payment data masked in logs
- [ ] TLS 1.3 configured for all endpoints
- [ ] Audit logging for all payment operations
- [ ] Access control enforced (role-based)
- [ ] No sensitive data in application logs
- [ ] Encryption keys stored in environment variables
- [ ] Integration tests pass
- [ ] PCI compliance checklist documented
- [ ] Security documentation complete

---

## INTEGRATION PLAN: ALL 4 P0 ISSUES

### Sequential Dependencies

**Day 1 (Week 1):**
- Morning: Audit Columns (DB migration)
- Afternoon: Transaction Boundaries (documentation)

**Day 2-3 (Week 1-2):**
- Morning: Row-Level Security (architecture)
- Afternoon: PCI Compliance (encryption setup)

**Day 4-15 (Week 2-3):**
- Incremental implementation during API development

### Cross-Cutting Concerns

**All 4 issues affect:**
- Order creation flow
- Subscription activation flow
- Invoice generation flow
- Payment recording flow

**Integration Points:**
- Audit columns track all changes
- Transaction boundaries ensure consistency
- Row-level security restricts access
- PCI compliance secures payment data

### Testing Integration

**Combined Test Scenarios:**
1. Order creation with audit logging, transaction, and security
2. Payment recording with encryption, audit, and access control
3. Invoice generation with all 4 P0 issues combined

---

## DEFINITION OF DONE - SPRINT 1

**For Sprint 1 to be complete, ALL 4 P0 issues must be:**

- [ ] Transaction boundaries defined and implemented
- [ ] Audit columns in all entities
- [ ] Row-level security on all endpoints
- [ ] PCI compliance basics implemented
- [ ] All integration tests pass
- [ ] Performance tests show <5% overhead
- [ ] Security tests show no unauthorized access
- [ ] Audit logs contain complete trail
- [ ] Payment data encrypted and masked
- [ ] Documentation complete for all 4 issues

---

**Document Status:** ✅ COMPLETE
**Ready for Implementation:** YES
**Next Step:** Development Team Review
**Approval Required:** Tech Lead
