# Row-Level Security Architecture
## BSS System - Sprint 1

**Document Owner:** Backend Developer
**Date:** 2025-10-29
**Version:** 1.0
**Status:** DRAFT

---

## EXECUTIVE SUMMARY

This document defines the row-level security model for the BSS system to ensure customers can only access their own data, while agents and administrators have appropriate access levels.

**Security Principle:** Zero Trust - Verify every access request

---

## SECURITY MODEL

### Core Principles
1. **Data Isolation:** Customers can only access their own data
2. **Role-Based Access:** Different roles have different permissions
3. **Principle of Least Privilege:** Grant minimum necessary access
4. **Defense in Depth:** Security at multiple layers
5. **Audit Trail:** Log all access for compliance

---

## AUTHENTICATION & AUTHORIZATION

### Authentication
**Technology:** Keycloak OIDC (OpenID Connect)

**Flow:**
1. User authenticates with Keycloak
2. Keycloak returns JWT token
3. Backend validates JWT signature
4. Backend extracts user ID and roles from token
5. User context established for request

**Token Claims:**
```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "email": "customer@example.com",
  "preferred_username": "customer123",
  "realm_access": {
    "roles": ["CUSTOMER"]
  },
  "resource_access": {
    "bss-frontend": {
      "roles": ["CUSTOMER"]
    }
  }
}
```

### Authorization
**Technology:** Spring Security + Method Security

**Implementation:**
```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    @Bean
    public SecurityExpressionHandler<MethodInvocation> expressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(new CustomPermissionEvaluator());
        return handler;
    }
}
```

---

## ROLE DEFINITION

### Role Hierarchy
```
ADMIN (Full System Access)
  ├── BILLING (Billing Operations)
  └── SUPPORT (View Data, Create Tickets)
      └── AGENT (Manage Customer Orders/Subscriptions)
          └── CUSTOMER (Own Data Only)
```

### Role Permissions

#### CUSTOMER
**Description:** End customer accessing their own data

**Permissions:**
- ✅ View own customer profile
- ✅ View own orders
- ✅ Create new orders
- ✅ View own subscriptions
- ✅ Suspend/Resume own subscriptions
- ✅ View own invoices
- ✅ View own payments
- ✅ View own usage records

**Restrictions:**
- ❌ Cannot access other customers' data
- ❌ Cannot access system-wide data
- ❌ Cannot access admin functions
- ❌ Cannot modify product catalog

---

#### AGENT
**Description:** Customer service agent managing customer accounts

**Permissions:**
- ✅ All CUSTOMER permissions for any customer
- ✅ View all customer profiles
- ✅ Create/modify orders for any customer
- ✅ Manage subscriptions for any customer
- ✅ View all orders and subscriptions
- ✅ Approve/reject orders
- ✅ Generate invoices

**Restrictions:**
- ❌ Cannot access billing/administrative functions
- ❌ Cannot modify product catalog
- ❌ Cannot access system configuration

---

#### BILLING
**Description:** Billing department operations

**Permissions:**
- ✅ View all invoices
- ✅ Generate invoices
- ✅ Process payments
- ✅ View payment history
- ✅ Generate billing reports
- ✅ Adjust invoice amounts

**Restrictions:**
- ❌ Cannot modify customer profiles
- ❌ Cannot modify orders/subscriptions
- ❌ Cannot access system configuration

---

#### SUPPORT
**Description:** Technical support (read-only for troubleshooting)

**Permissions:**
- ✅ View customer profiles (read-only)
- ✅ View orders (read-only)
- ✅ View subscriptions (read-only)
- ✅ View usage records (read-only)
- ✅ Create support tickets

**Restrictions:**
- ❌ Cannot modify any data
- ❌ Cannot access financial data
- ❌ Cannot access product catalog

---

#### ADMIN
**Description:** System administrator

**Permissions:**
- ✅ Full access to all data
- ✅ Manage product catalog
- ✅ Manage system configuration
- ✅ View all reports
- ✅ Manage users and roles
- ✅ Access system logs

---

## SECURITY SERVICE

### SecurityService Implementation

```java
@Service
public class SecurityService {

    private final AuthenticationService authenticationService;

    public SecurityService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Check if authenticated user owns the resource
     */
    public boolean isOwner(UUID resourceCustomerId, String authenticatedUserId) {
        return resourceCustomerId.toString().equals(authenticatedUserId);
    }

    /**
     * Check if authenticated user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if authenticated user is agent or admin
     */
    public boolean isAgentOrAdmin() {
        return hasRole("AGENT") || hasRole("ADMIN");
    }

    /**
     * Check if authenticated user has specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Get current authenticated customer ID
     */
    public String getCurrentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return authentication.getName();
    }

    /**
     * Get current authenticated user roles
     */
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replace("ROLE_", ""))
            .collect(Collectors.toSet());
    }
}
```

---

## API SECURITY PATTERNS

### Pattern 1: Customer Scoped Endpoints

**Use Case:** Customer accessing their own data

**Implementation:**
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto>>> getCustomerOrders(
            @RequestParam UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {

        List<OrderDto> orders = orderService.getOrdersByCustomerId(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#request.customerId, authentication.name)")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        OrderDto order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
```

**Security Flow:**
1. Request arrives with customerId parameter
2. @PreAuthorize validates:
   - User has CUSTOMER role
   - customerId matches authenticated user's ID
3. If validation fails → 403 Forbidden
4. If validation passes → Business logic executes

---

### Pattern 2: Agent Endpoints

**Use Case:** Agent accessing any customer's data

**Implementation:**
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders(
            @PageableDefault(size = 50) Pageable pageable) {

        List<OrderDto> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OrderDto>> approveOrder(
            @PathVariable UUID id,
            @RequestParam UUID customerId,
            @RequestBody ApproveOrderRequest request) {

        OrderDto order = orderService.approveOrder(id, customerId, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
```

**Security Flow:**
1. Request arrives
2. @PreAuthorize validates:
   - User has AGENT or ADMIN role
3. If validation fails → 403 Forbidden
4. If validation passes → Business logic executes

---

### Pattern 3: Administrative Endpoints

**Use Case:** Admin managing product catalog

**Implementation:**
```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @PreAuthorize("hasRole('CUSTOMER') or hasRole('AGENT') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        List<ProductDto> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductDto product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {

        ProductDto product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
```

**Security Flow:**
1. GET: Any authenticated user (CUSTOMER, AGENT, ADMIN) can view
2. POST/PUT: Only ADMIN can modify
3. Validation based on HTTP method

---

### Pattern 4: Billing Endpoints

**Use Case:** Billing operations

**Implementation:**
```java
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @PreAuthorize("hasRole('CUSTOMER') and @securityService.isOwner(#customerId, authentication.name)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getCustomerInvoices(
            @RequestParam UUID customerId) {

        List<InvoiceDto> invoices = invoiceService.getInvoicesByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @PreAuthorize("hasRole('BILLING') or hasRole('ADMIN')")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<InvoiceDto>> generateInvoice(
            @RequestParam UUID subscriptionId) {

        InvoiceDto invoice = invoiceService.generateInvoice(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }
}
```

---

## REPOSITORY-LEVEL SECURITY

### Customer Scoped Queries

**Implementation:**
```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    // Security enforced at query level
    @Query("SELECT o FROM OrderEntity o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id AND o.customerId = :customerId")
    Optional<OrderEntity> findByIdAndCustomerId(@Param("id") UUID id,
                                               @Param("customerId") UUID customerId);

    // Admin can query all (no WHERE clause)
    @Query("SELECT o FROM OrderEntity o ORDER BY o.createdAt DESC")
    List<OrderEntity> findAllOrders(Pageable pageable);
}
```

**Purpose:**
- Double security layer (controller + repository)
- Prevent SQL injection (parameterized queries)
- Ensure customer data isolation

---

### Best Practices

1. **Always Include Customer ID in WHERE Clause**
```java
// ✅ GOOD
@Query("SELECT o FROM OrderEntity o WHERE o.customerId = :customerId")

// ❌ BAD - No customer filter
@Query("SELECT o FROM OrderEntity o")
```

2. **Validate Customer ID Match**
```java
// ✅ GOOD
public OrderDto getOrder(UUID orderId, UUID customerId) {
    OrderDto order = orderService.getOrderById(orderId);
    if (!order.getCustomerId().equals(customerId)) {
        throw new AccessDeniedException("Cannot access another customer's order");
    }
    return order;
}
```

3. **Use Parameterized Queries**
```java
// ✅ GOOD
@Query("SELECT o FROM OrderEntity o WHERE o.customerId = :customerId")

// ❌ BAD - String concatenation
@Query("SELECT o FROM OrderEntity o WHERE o.customerId = '" + customerId + "'")
```

---

## SERVICE LAYER SECURITY

### Input Validation

```java
@Service
public class OrderService {

    public OrderDto getOrderById(UUID orderId, UUID customerId) {
        // Get authenticated user
        String authenticatedCustomerId = securityService.getCurrentCustomerId();

        // Get order
        OrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Verify ownership
        if (!order.getCustomerId().toString().equals(authenticatedCustomerId)) {
            throw new AccessDeniedException("Cannot access another customer's order");
        }

        return convertToDto(order);
    }
}
```

**Validation Steps:**
1. Get authenticated user from security context
2. Fetch requested resource
3. Compare customer IDs
4. Throw AccessDeniedException if mismatch

---

## ERROR HANDLING

### Security Exceptions

```java
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Access Denied: " + ex.getMessage()
        );
        problem.setTitle("Access Denied");
        problem.setType(URI.create("about:blank"));

        // Don't expose sensitive information
        logger.warn("Access denied: {} - User: {}",
            ex.getMessage(),
            SecurityContextHolder.getContext().getAuthentication().getName());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication required"
            ));
    }
}
```

**Error Codes:**
- 401 Unauthorized: Authentication required or token invalid
- 403 Forbidden: Authenticated but not authorized
- 404 Not Found: Resource doesn't exist (don't reveal if exists but denied)

---

## AUDIT LOGGING

### Security Audit Events

```java
@Component
public class SecurityAuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logAccess(String action, UUID resourceId, String customerId, boolean granted) {
        Map<String, Object> auditData = Map.of(
            "action", action,
            "resourceId", resourceId,
            "customerId", customerId,
            "granted", granted,
            "timestamp", Instant.now().toString(),
            "traceId", MDC.get("traceId"),
            "userAgent", getCurrentUserAgent(),
            "ipAddress", getCurrentIpAddress()
        );

        if (granted) {
            auditLogger.info("SECURITY_GRANTED: {}", auditData);
        } else {
            auditLogger.warn("SECURITY_DENIED: {}", auditData);
        }
    }
}
```

### Audit Events to Log
1. All successful data access
2. All denied access attempts
3. Role changes
4. Failed authentication attempts
5. Suspicious activity patterns

---

## TESTING

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class OrderControllerSecurityTest {

    @Mock
    private OrderService orderService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private OrderController orderController;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldAllowCustomerToAccessOwnOrders() {
        // Given
        UUID customerId = UUID.randomUUID();
        when(securityService.getCurrentCustomerId()).thenReturn(customerId.toString());

        // When
        ResponseEntity<ApiResponse<List<OrderDto>>> response =
            orderController.getCustomerOrders(customerId, Pageable.unpaged());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToAnotherCustomersOrders() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID anotherCustomerId = UUID.randomUUID();
        when(securityService.getCurrentCustomerId()).thenReturn(customerId.toString());
        doThrow(new AccessDeniedException("Access denied"))
            .when(securityService).isOwner(eq(anotherCustomerId), anyString());

        // When & Then
        assertThatThrownBy(() ->
            orderController.getCustomerOrders(anotherCustomerId, Pageable.unpaged())
        ).isInstanceOf(AccessDeniedException.class);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class OrderSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("bss_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldDenyAccessWithoutAuthentication() {
        // When
        ResponseEntity<OrderDto[]> response = restTemplate.getForEntity(
            "/api/orders",
            OrderDto[].class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToAnotherCustomersData() {
        // Given
        UUID anotherCustomerId = UUID.randomUUID();

        // When
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/orders?customerId={customerId}",
            HttpMethod.GET,
            null,
            String.class,
            anotherCustomerId
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

---

## COMPLIANCE & GDPR

### Data Access Rights
1. **Right to Access:** Customers can view their own data
2. **Right to Rectification:** Customers can update their own data
3. **Right to Erasure:** Customers can request deletion
4. **Right to Portability:** Customers can export their data

### Audit Requirements
- Log all data access
- Retain logs for 7 years
- Enable compliance reporting
- Track data processing purposes

---

## PERFORMANCE CONSIDERATIONS

### Indexing Strategy
```sql
-- Index on customer_id for all customer-scoped queries
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_subscriptions_customer_id ON subscriptions(customer_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);

-- Composite indexes for common queries
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX idx_subscriptions_customer_status ON subscriptions(customer_id, status);
```

### Caching
```java
@Cacheable(value = "customer-orders", key = "#customerId")
public List<OrderDto> getOrdersByCustomerId(UUID customerId, Pageable pageable) {
    // Implementation
}
```

**Caching Strategy:**
- Cache customer data (5 minutes TTL)
- Cache product catalog (1 hour TTL)
- Don't cache sensitive financial data
- Invalidate cache on data changes

---

## MONITORING & ALERTING

### Security Metrics
1. Failed authentication attempts
2. Authorization failures (403)
3. Access denied events
4. Suspicious patterns (rapid failures)
5. Privilege escalation attempts

### Alerts
```yaml
# Prometheus alerts
- alert: HighAuthenticationFailureRate
  expr: rate(http_requests_total{status="401"}[5m]) > 0.1
  for: 2m
  labels:
    severity: warning

- alert: HighAuthorizationFailureRate
  expr: rate(http_requests_total{status="403"}[5m]) > 0.05
  for: 1m
  labels:
    severity: warning
```

---

## DEFINITION OF DONE

### Security Implementation Complete When:
- [ ] SecurityService bean created and tested
- [ ] @PreAuthorize on all controller methods
- [ ] Customer-scoped queries (customerId in WHERE)
- [ ] Admin/Agent role checks for management operations
- [ ] All endpoints tested for authorization
- [ ] Unauthorized access returns 403
- [ ] Integration tests pass for all security scenarios
- [ ] Security documentation complete
- [ ] Audit logging implemented
- [ ] Performance tests show acceptable overhead (<5%)

---

**Document Status:** ✅ Ready for Implementation
**Next Steps:**
1. Implement SecurityService bean
2. Add @PreAuthorize annotations to controllers
3. Add customer scoping to repositories
4. Create security tests
5. Implement audit logging

---

**Author:** Backend Developer
**Reviewer:** Tech Lead, Security Team
**Last Updated:** 2025-10-29
