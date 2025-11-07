# PostgreSQL Row Level Security (RLS) Implementation

## Overview

Row Level Security (RLS) is implemented to ensure tenant isolation at the database level. This provides an additional security layer preventing cross-tenant data access even if application-level checks are bypassed.

## Architecture

### RLS Components

1. **Policy-Based Isolation**: Each table has policies that filter rows based on `tenant_id`
2. **Tenant Context**: PostgreSQL session variable `app.current_tenant_id` stores the active tenant
3. **Application Role**: Dedicated role (`application_role`) with RLS-aware permissions
4. **Admin Bypass**: Admin role (`admin_role`) can bypass RLS for maintenance operations

### Security Model

```
┌─────────────────────────────────────┐
│   Application Request               │
│   (with Tenant ID)                  │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   set_tenant_context(tenant_id)     │
│   (Session Variable)                │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   PostgreSQL RLS Policies           │
│   - Filter by tenant_id             │
│   - Enforce isolation               │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Tenant-Scoped Result Set          │
└─────────────────────────────────────┘
```

## Implementation Details

### 1. Tables with RLS Enabled

| Table | RLS Status | Purpose |
|-------|-----------|---------|
| `customers` | ✅ Enabled | Customer data isolation |
| `addresses` | ✅ Enabled | Address data isolation |
| `orders` | ✅ Enabled | Order data isolation |
| `order_items` | ✅ Enabled | Order items isolation |
| `invoices` | ✅ Enabled | Invoice data isolation |
| `invoice_items` | ✅ Enabled | Invoice items isolation |
| `payments` | ✅ Enabled | Payment data isolation |
| `subscriptions` | ✅ Enabled | Subscription data isolation |
| `products` | ✅ Enabled | Read-only for all tenants |

### 2. RLS Policies

Each table has four standard policies:

- **SELECT Policy**: Controls which rows can be read
- **INSERT Policy**: Controls which rows can be created
- **UPDATE Policy**: Controls which rows can be modified
- **DELETE Policy**: Controls which rows can be removed

#### Policy Pattern

```sql
CREATE POLICY <table>_isolation_select ON <table>
    FOR SELECT
    TO application_role
    USING (tenant_id = current_setting('app.current_tenant_id')::uuid);
```

### 3. Utility Functions

| Function | Purpose | Security |
|----------|---------|----------|
| `set_tenant_context(uuid)` | Set active tenant ID | `SECURITY DEFINER` |
| `clear_tenant_context()` | Clear tenant context | `SECURITY DEFINER` |
| `get_current_tenant_id()` | Get active tenant ID | `STABLE` |

### 4. Roles and Permissions

#### application_role
- Standard application role
- Subject to RLS policies
- Used by Spring Boot application
- Permissions: SELECT, INSERT, UPDATE, DELETE on scoped tables

#### admin_role
- Administrative role
- Bypasses RLS policies
- Used for maintenance and reporting
- Permissions: ALL privileges on all tables

## Database Migration

### Flyway Migration

Create migration file: `V1025__enable_row_level_security.sql`

```sql
-- This file should contain the contents of rls-setup.sql
-- Run via: ./mvnw flyway:migrate -Dspring.flyway.locations=db/migration
```

### Migration Execution

```bash
# Migrate database
cd backend
./mvnw flyway:migrate

# Verify migration
psql -h localhost -U bss_app -d bss -c "SELECT * FROM pg_policies WHERE schemaname = 'public';"
```

## Application Integration

### 1. Spring Boot Configuration

#### application-dev.yml

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Enable SQL logging for debugging
        show_sql: true
        format_sql: true
```

#### application-prod.yml

```yaml
spring:
  jpa:
    properties:
      hibernate:
        # Disable SQL logging in production
        show_sql: false
        format_sql: false
```

### 2. Setting Tenant Context

#### Option A: JPQL (Recommended)

```java
@Service
@Transactional
public class CustomerService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Customer> getCustomers(UUID tenantId) {
        // Set tenant context
        entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
            .setParameter("tenantId", tenantId)
            .getSingleResult();

        // Query is automatically filtered
        return entityManager.createQuery(
            "FROM Customer", Customer.class)
            .getResultList();
    }

    public Customer createCustomer(UUID tenantId, CreateCustomerCommand command) {
        // Set tenant context
        entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
            .setParameter("tenantId", tenantId)
            .getSingleResult();

        // Create with tenant_id
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setName(command.getName());
        // ...

        entityManager.persist(customer);

        // Clear context (optional)
        entityManager.createQuery("SELECT clear_tenant_context()")
            .getSingleResult();

        return customer;
    }
}
```

#### Option B: Interceptor

```java
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null) {
            entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        entityManager.createQuery("SELECT clear_tenant_context()")
            .getSingleResult();
    }
}
```

#### Option C: Spring AOP

```java
@Aspect
@Component
public class TenantContextAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("@annotation(TenantScoped)")
    public Object setTenantContext(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request =
            ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String tenantId = request.getHeader("X-Tenant-ID");

        if (tenantId != null) {
            entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        }

        try {
            return pjp.proceed();
        } finally {
            entityManager.createQuery("SELECT clear_tenant_context()")
                .getSingleResult();
        }
    }
}

// Usage
@Service
public class OrderService {

    @TenantScoped
    public List<Order> getOrders() {
        // Automatically filtered by tenant
        return orderRepository.findAll();
    }
}
```

## Security Best Practices

### 1. Always Set Tenant Context

```java
// ❌ WRONG: No tenant context
List<Customer> customers = entityManager.createQuery(
    "FROM Customer", Customer.class).getResultList();
// Returns empty or throws error

// ✅ CORRECT: Set tenant context
entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
    .setParameter("tenantId", tenantId).getSingleResult();
List<Customer> customers = entityManager.createQuery(
    "FROM Customer", Customer.class).getResultList();
```

### 2. Validate Tenant ID

```java
public Customer getCustomer(UUID tenantId, UUID customerId) {
    // Validate tenant exists
    if (!tenantService.exists(tenantId)) {
        throw new TenantNotFoundException(tenantId);
    }

    // Set context
    entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
        .setParameter("tenantId", tenantId).getSingleResult();

    // Query is safe
    return customerRepository.findById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(customerId));
}
```

### 3. Admin Operations

```java
@Service
public class AdminService {

    // Bypass RLS with admin role
    @PreAuthorize("hasRole('ADMIN')")
    public List<Customer> getAllCustomers() {
        // Run as admin_role
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET ROLE admin_role");
                // Perform admin operations
                stmt.execute("RESET ROLE");
            }
        });

        return customerRepository.findAll();
    }
}
```

### 4. Testing with RLS

```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class RlsIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @Test
    @Order(1)
    public void testTenantIsolation() {
        // Setup: Create customers for tenant A
        setTenantContext(TENANT_A);
        Customer customerA = createCustomer("Customer A");
        entityManager.flush();

        // Setup: Create customer for tenant B
        setTenantContext(TENANT_B);
        Customer customerB = createCustomer("Customer B");
        entityManager.flush();

        // Test: Tenant A can only see their customer
        setTenantContext(TENANT_A);
        List<Customer> customersA = entityManager.createQuery(
            "FROM Customer", Customer.class).getResultList();
        assertEquals(1, customersA.size());
        assertEquals("Customer A", customersA.get(0).getName());

        // Test: Tenant B can only see their customer
        setTenantContext(TENANT_B);
        List<Customer> customersB = entityManager.createQuery(
            "FROM Customer", Customer.class).getResultList();
        assertEquals(1, customersB.size());
        assertEquals("Customer B", customersB.get(0).getName());
    }

    private void setTenantContext(UUID tenantId) {
        entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
            .setParameter("tenantId", tenantId)
            .getSingleResult();
    }
}
```

## Performance Considerations

### 1. Indexing

Ensure `tenant_id` is indexed on all tables:

```sql
-- Create indexes on tenant_id
CREATE INDEX idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX idx_addresses_tenant_id ON addresses(tenant_id);
CREATE INDEX idx_orders_tenant_id ON orders(tenant_id);
-- ... repeat for all tables
```

### 2. Partitioning

For large-scale deployments, consider partitioning by tenant_id:

```sql
-- Example: Partition customers by tenant
CREATE TABLE customers_tenant_1 PARTITION OF customers
    FOR VALUES IN ('550e8400-e29b-41d4-a716-446655440001');

CREATE TABLE customers_tenant_2 PARTITION OF customers
    FOR VALUES IN ('550e8400-e29b-41d4-a716-446655440002');
```

### 3. Query Optimization

RLS adds a WHERE clause to all queries. Monitor performance:

```sql
-- Enable query logging
SET log_statement = 'all';

-- Check slow queries
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

## Monitoring

### 1. Check RLS Status

```sql
-- Verify RLS is enabled
SELECT schemaname, tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
  AND rowsecurity = true;

-- List all policies
SELECT tablename, policyname, cmd, roles
FROM pg_policies
WHERE schemaname = 'public';
```

### 2. Policy Usage Statistics

```sql
-- Check policy usage
SELECT schemaname, tablename, policyname,
       qual, with_check
FROM pg_policies
WHERE schemaname = 'public';
```

### 3. Performance Monitoring

```sql
-- Monitor queries filtered by RLS
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
WHERE query LIKE '%current_setting%'
ORDER BY mean_time DESC;
```

## Troubleshooting

### Issue 1: Empty Results

**Symptom**: Query returns no results even though data exists

**Cause**: Tenant context not set

**Solution**:
```sql
-- Check if tenant context is set
SELECT current_setting('app.current_tenant_id', true);

-- If empty, set it
SELECT set_tenant_context('550e8400-e29b-41d4-a716-446655440000');
```

### Issue 2: Access Denied

**Symptom**: `new row violates row-level security policy`

**Cause**: INSERT/UPDATE without tenant_id or wrong tenant_id

**Solution**:
```java
// Ensure tenant_id is set before insert
entityManager.createQuery("SELECT set_tenant_context(:tenantId)")
    .setParameter("tenantId", tenantId)
    .getSingleResult();

Customer customer = new Customer();
customer.setTenantId(tenantId);  // Important!
entityManager.persist(customer);
```

### Issue 3: Admin Bypass Not Working

**Symptom**: Admin user still subject to RLS

**Cause**: Not using admin_role

**Solution**:
```java
// Explicitly set role
Session session = entityManager.unwrap(Session.class);
session.doWork(connection -> {
    try (Statement stmt = connection.createStatement()) {
        stmt.execute("SET ROLE admin_role");
        // Admin operations here
        stmt.execute("RESET ROLE");
    }
});
```

## Migration Checklist

- [ ] Run `rls-setup.sql` on database
- [ ] Verify RLS enabled on all tables
- [ ] Create indexes on `tenant_id` columns
- [ ] Update Spring Boot to set tenant context
- [ ] Add tests for tenant isolation
- [ ] Document tenant context pattern
- [ ] Train developers on RLS usage
- [ ] Add monitoring for RLS performance
- [ ] Create admin procedures for RLS bypass
- [ ] Update backup strategy (include RLS metadata)

## References

- [PostgreSQL RLS Documentation](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Spring Boot JPA Best Practices](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL Security Guide](https://www.postgresql.org/docs/current/security.html)

## Support

For issues with RLS implementation:
1. Check PostgreSQL logs: `/var/log/postgresql/`
2. Enable query logging: `SET log_statement = 'all';`
3. Review policy definitions: `SELECT * FROM pg_policies;`
4. Contact: infra-team@company.com
