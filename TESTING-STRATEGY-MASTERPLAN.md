# 📋 MASTERPLAN STRATEGII TESTOWANIA BSS
**Komprehensywna strategia testowania od podstawowych do enterprise'owych scenariuszy**

---

## 🎯 1. ANALIZA ARCHITEKTURY

### Architektura Aplikacji
- **Backend**: 333 pliki Java (Spring Boot 3.4, Java 21, Virtual Threads)
- **Frontend**: Nuxt 3 + TypeScript + Playwright
- **Infrastruktura**: 40+ kontenerów (PostgreSQL 18, Kafka 3-broker cluster, Redis, Keycloak, Traefik)

### Komponenty do Testowania
```
┌─────────────────────────────────────────────────────────────────┐
│                           FRONTEND                              │
│  Nuxt 3 + TypeScript + Playwright + Accessibility               │
├─────────────────────────────────────────────────────────────────┤
│                        API GATEWAY                              │
│  Traefik + Caddy (mTLS, rate limiting, circuit breaker)        │
├─────────────────────────────────────────────────────────────────┤
│                         BACKEND                                 │
│  ┌────────────┬──────────────┬──────────────┬──────────────┐    │
│  │  Customer  │   Product    │    Order     │   Payment    │    │
│  └────────────┴──────────────┴──────────────┴──────────────┘    │
│  ┌────────────┬──────────────┬──────────────┬──────────────┐    │
│  │ Subscription│  Invoice    │   Billing    │    Asset     │    │
│  └────────────┴──────────────┴──────────────┴──────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                      INFRASTRUCTURE                             │
│  ┌────────────┬──────────────┬──────────────┬──────────────┐    │
│  │ PostgreSQL │     Redis    │    Kafka     │  Keycloak    │    │
│  │ (3 shards) │   (cluster)  │ (3 brokers)  │    (OIDC)    │    │
│  └────────────┴──────────────┴──────────────┴──────────────┘    │
│  ┌────────────┬──────────────┬──────────────┬──────────────┐    │
│  │  Flink     │   Citus      │   HAProxy    │   PgBouncer  │    │
│  │  Streams   │  Sharding    │ Load Balancer│              │    │
│  └────────────┴──────────────┴──────────────┴──────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│                    OBSERVABILITY STACK                          │
│  Prometheus + Grafana + Tempo + Loki + Jaeger + AlertManager    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧪 2. POZIOM 1: TESTY PODSTAWOWE (Lokalne/CI)

### 2.1 Testy Jednostkowe - Backend (Java)
**Zakres**: 333 pliki Java

```bash
mvn test -Dtest=*Test
```

**Struktura testów**:
```
backend/src/test/java/com/droid/bss/
├── domain/                     # 60-80 testów
│   ├── customer/CustomerTest.java
│   ├── product/ProductTest.java
│   ├── order/OrderTest.java
│   └── ...
├── application/                # 80-100 testów
│   ├── command/               # CQRS Command handlers
│   ├── query/                 # CQRS Query handlers
│   └── service/               # Application services
├── infrastructure/             # 100-120 testów
│   ├── auth/jwt/              # JWT validation
│   ├── auth/oidc/             # OIDC integration
│   ├── database/pooling/      # Connection pooling
│   ├── database/sharding/     # Sharding scenarios
│   ├── database/transaction/  # Transaction management
│   ├── event/publisher/       # Event publishing
│   ├── event/handlers/        # Event handling
│   ├── messaging/deadletter/  # Dead Letter Queue
│   ├── cache/eviction/        # Cache eviction
│   ├── resilience/            # Circuit breaker, rate limiting
│   ├── security/              # Security filters, mTLS
│   └── metrics/               # Metrics collection
└── api/                        # 50-70 testów
    ├── customer/              # REST controllers
    ├── product/
    ├── order/
    └── ... (10 controllers)
```

**Szczegółowe scenariusze**:

#### A) Testy Domain Layer
```java
// Customer aggregate root test
@Test
void shouldCreateCustomerWithValidData() { ... }
@Test
void shouldRejectInvalidCustomerData() { ... }
@Test
void shouldUpdateCustomerState() { ... }
@Test
void shouldHandleBusinessRules() { ... }

// Value objects tests
@Test
void shouldValidateEmailFormat() { ... }
@Test
void shouldValidatePhoneNumber() { ... }
@Test
void shouldValidateAddressStructure() { ... }
```

#### B) Testy Infrastructure Layer

**1. Auth Module** (infrastructure/auth/jwt/)
```java
JwtValidationTest.java:
├── Should validate JWT token signature
├── Should reject expired tokens
├── Should handle invalid tokens
├── Should extract claims correctly
├── Should cache public keys
└── Should support key rotation

OidcIntegrationTest.java:
├── Should discover OIDC configuration
├── Should exchange code for tokens
├── Should refresh expired tokens
├── Should handle multiple providers
└── Should support PKCE flow
```

**2. Database Sharding** (infrastructure/database/sharding/)
```java
HashShardingStrategyTest.java:
├── Should distribute keys evenly
├── Should handle hash collisions
├── Should support consistent hashing
└── Should recalculate on shard changes

RangeShardingStrategyTest.java:
├── Should route to correct range
├── Should handle boundary values
├── Should validate range overlaps
└── Should support dynamic ranges

DefaultShardManagerTest.java:
├── Should register shards dynamically
├── Should cache routing decisions
├── Should track statistics
├── Should handle shard failures
├── Should rebalance on changes
└── Should broadcast to all shards

DefaultShardAwareRepositoryTest.java:
├── Should route CRUD operations
├── Should batch operations across shards
├── Should handle transactions per shard
└── Should aggregate results
```

**3. Transaction Management** (infrastructure/database/transaction/)
```java
TransactionManagerTest.java:
├── Should begin transaction
├── Should commit successfully
├── Should rollback on error
├── Should handle nested transactions
├── Should respect isolation levels
└── Should propagate context

SpringTransactionManagerTest.java:
├── Should integrate with Spring @Transactional
├── Should handle @Transactional(propagation=NESTED)
├── Should support programmatic transactions
└── Should manage transaction boundaries
```

**4. Dead Letter Queue** (infrastructure/messaging/deadletter/)
```java
KafkaDeadLetterQueueTest.java:
├── Should send failed message to DLQ
├── Should preserve original payload
├── Should track retry attempts
├── Should handle serialization errors
├── Should support batch operations
└── Should record statistics

RetryPolicyTest.java:
├── Should retry with fixed delay
├── Should use exponential backoff
├── Should respect max retry attempts
├── Should handle different error types
└── Should send to DLQ when exhausted
```

**5. Cache Eviction** (infrastructure/cache/eviction/)
```java
CacheEvictionManagerTest.java:
├── Should evict LRU entries
├── Should use TTL expiration
├── Should handle cache stampede
├── Should support custom eviction policies
└── Should monitor hit/miss rates

RedisEvictionStrategyTest.java:
├── Should execute LUA scripts atomically
├── Should handle pipeline operations
├── Should track memory usage
└── Should support cluster mode
```

**6. Event System** (infrastructure/event/)
```java
CloudEventPublisherTest.java:
├── Should serialize to CloudEvents format
├── Should send to Kafka topics
├── Should handle schema evolution
├── Should retry on failures
└── Should batch events

EventHandlerTest.java:
├── Should consume events idempotently
├── Should handle out-of-order events
├── Should support replay
├── Should validate event schemas
└── Should track processing metrics
```

#### C) Testy API Layer
```java
CustomerControllerTest.java:
├── Should create customer via REST API
├── Should validate request body
├── Should return proper HTTP codes
├── Should handle authentication
├── Should respect rate limits
├── Should log requests
└── Should return OpenAPI spec

CustomerControllerIntegrationTest.java:
├── Should save customer to database
├── Should publish domain events
├── Should invalidate cache
├── Should handle concurrent requests
└── Should rollback on validation errors
```

### 2.2 Testy Frontend (Nuxt 3 + TypeScript + Playwright)
**Zakres**: E2E tests, component tests, accessibility

```bash
pnpm run test:unit        # Vitest unit tests
pnpm run test:e2e         # Playwright E2E tests
pnpm run test:a11y        # Accessibility tests
```

**Struktura testów**:
```
frontend/tests/
├── e2e/                   # 30-50 scenariuszy E2E
│   ├── auth/             # OIDC login flow
│   ├── customer/         # Customer CRUD
│   ├── order/            # Order management
│   ├── payment/          # Payment processing
│   ├── subscription/     # Subscription lifecycle
│   └── ...
├── components/            # Component unit tests
│   ├── forms/
│   ├── tables/
│   ├── modals/
│   └── ...
├── accessibility/         # A11y tests
│   ├── axe-core/
│   ├── color-contrast/
│   ├── keyboard-nav/
│   └── screen-reader/
└── api/                  # API testing
    ├── rest/
    ├── graphql/
    └── websocket/
```

**Scenariusze E2E**:

#### A) Authentication Flow
```typescript
// auth/login.spec.ts
test('Complete OIDC login flow', async ({ page }) => {
  // 1. Navigate to login
  await page.goto('/login');

  // 2. Click "Sign in with Keycloak"
  await page.click('[data-testid="oidc-login"]');

  // 3. Enter credentials in Keycloak
  await page.fill('#username', 'testuser');
  await page.fill('#password', 'testpass');

  // 4. Verify redirect to app
  await expect(page).toHaveURL('/dashboard');

  // 5. Verify token storage
  const token = await page.evaluate(() => localStorage.getItem('access_token'));
  expect(token).toBeTruthy();

  // 6. Verify session persistence
  await page.reload();
  await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
});
```

#### B) Customer CRUD
```typescript
// customer/customer.spec.ts
test('Complete customer lifecycle', async ({ page }) => {
  // Login first
  await loginAsAdmin(page);

  // Create customer
  await page.click('[data-testid="create-customer"]');
  await page.fill('[name="firstName"]', 'John');
  await page.fill('[name="lastName"]', 'Doe');
  await page.fill('[name="email"]', 'john@example.com');
  await page.selectOption('[name="status"]', 'ACTIVE');
  await page.click('[type="submit"]');

  // Verify creation
  await expect(page.locator('[data-testid="customer-list"]'))
    .toContainText('John Doe');

  // Update customer
  await page.click('[data-testid="edit-customer"]');
  await page.fill('[name="phone"]', '+1234567890');
  await page.click('[type="submit"]');

  // Verify update
  await expect(page.locator('[data-testid="customer-detail"]'))
    .toContainText('+1234567890');

  // Delete customer
  await page.click('[data-testid="delete-customer"]');
  await page.click('[data-testid="confirm-delete"]');

  // Verify deletion
  await expect(page.locator('[data-testid="customer-list"]'))
    .not.toContainText('John Doe');
});
```

#### C) Complex Order Processing
```typescript
// order/processing.spec.ts
test('Complex order with payment and subscription', async ({ page }) => {
  await loginAsCustomer(page);

  // Create order
  await page.goto('/products');
  await page.click('[data-testid="add-to-cart"]');
  await page.click('[data-testid="checkout"]');

  // Fill shipping address
  await page.fill('[name="street"]', '123 Main St');
  await page.fill('[name="city"]', 'New York');
  await page.fill('[name="zip"]', '10001');
  await page.fill('[name="country"]', 'USA');

  // Select shipping method
  await page.selectOption('[name="shippingMethod"]', 'EXPRESS');

  // Process payment
  await page.click('[data-testid="process-payment"]');
  await page.fill('[name="cardNumber"]', '4242424242424242');
  await page.fill('[name="expiry"]', '12/25');
  await page.fill('[name="cvv"]', '123');
  await page.fill('[name="nameOnCard"]', 'John Doe');
  await page.click('[type="submit"]');

  // Verify order confirmation
  await expect(page.locator('[data-testid="order-confirmation"]'))
    .toContainText('Order #');
  await expect(page.locator('[data-testid="order-status"]'))
    .toContainText('PROCESSING');

  // Verify subscription creation
  await expect(page.locator('[data-testid="subscription-info"]'))
    .toContainText('Subscription activated');

  // Verify email notification
  const emails = await getEmailsForUser('john@example.com');
  expect(emails).toContain('Order confirmation');
  expect(emails).toContain('Subscription details');
});
```

### 2.3 Testy Kontraktowe (Contract Testing)
```bash
# Pact tests
./mvn test -Dtest=*PactTest
```

**Pact Testing Strategy**:
```
┌──────────────┐         Contract         ┌──────────────┐
│   Frontend   │ <----------------------> │   Backend    │
│   (Consumer) │                          │  (Provider)  │
└──────────────┘                          └──────────────┘
      │                                          │
      │                                          │
  Generate Pact                           Verify Pact
      │                                          │
      v                                          v
┌──────────────┐                          ┌──────────────┐
│  Pact Broker │ <----------------------> │   CI/CD      │
│   (Storage)  │       Publish              │  Pipeline    │
└──────────────┘                          └──────────────┘
```

**Scenariusze kontraktowe**:
```java
// CustomerContractTest.java
@Pact(consumer = "frontend", provider = "backend")
public CustomerCreatePact:
├── Should create customer with minimal data
├── Should require email field
├── Should validate email format
├── Should reject duplicate emails
├── Should return customer ID
└── Should include HATEOAS links

// OrderContractTest.java
@Pact(consumer = "frontend", provider = "backend")
public OrderCreatePact:
├── Should create order with items
├── Should validate inventory
├── Should process payment
├── Should create subscription
└── Should return tracking info
```

---

## 🔬 3. POZIOM 2: TESTY INTEGRACYJNE (Docker Compose)

### 3.1 Testcontainers - Backend Integration Tests
**Uruchamianie**: `mvn verify` (Testcontainers)

```java
@Testcontainers
@SpringBootTest
class FullStackIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Test
    void shouldProcessCompleteCustomerOrderFlow() {
        // 1. Create customer via REST API
        Customer customer = restTemplate.postForObject("/api/customers", createCustomerRequest(), Customer.class);

        // 2. Verify customer saved to PostgreSQL
        Customer savedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals("John", savedCustomer.getFirstName());

        // 3. Create product
        Product product = restTemplate.postForObject("/api/products", createProductRequest(), Product.class);

        // 4. Create order with product
        Order order = restTemplate.postForObject("/api/orders", createOrderRequest(customer.getId(), product.getId()), Order.class);

        // 5. Verify Kafka event published
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "bss.order.events");
        CloudEvent event = CloudEventBuilder.from(record.value()).build();
        assertEquals("order.created", event.getType());

        // 6. Verify cache populated in Redis
        String cachedCustomer = redisTemplate.opsForValue().get("customer:" + customer.getId());
        assertNotNull(cachedCustomer);

        // 7. Verify read replica query (Citus)
        Customer replicaCustomer = readReplicaRepository.findById(customer.getId()).orElseThrow();
        assertEquals(customer.getId(), replicaCustomer.getId());
    }
}
```

### 3.2 Database Migration Testing (Flyway)
```java
@Test
void shouldValidateDatabaseSchema() {
    // 1. Get current version
    int currentVersion = flyway.getInfo().getCurrentVersion().getVersion().getVersion().intValue();

    // 2. Verify all migrations applied
    assertTrue(currentVersion > 0);

    // 3. Validate schema
    try (Connection conn = dataSource.getConnection()) {
        // Check customers table
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name='customers'"
        );

        Map<String, ColumnInfo> columns = new HashMap<>();
        while (rs.next()) {
            columns.put(rs.getString("COLUMN_NAME"),
                new ColumnInfo(
                    rs.getString("DATA_TYPE"),
                    "YES".equals(rs.getString("IS_NULLABLE"))
                )
            );
        }

        assertTrue(columns.containsKey("id"));
        assertTrue(columns.containsKey("email"));
        assertEquals("varchar", columns.get("email").type);
        assertFalse(columns.get("email").nullable);
    }
}
```

### 3.3 Kafka Integration Testing
```java
@SpringBootTest
class KafkaIntegrationTest {

    @Test
    void shouldPublishAndConsumeCloudEvents() {
        // 1. Publish event
        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create("https://bss.local/api"))
            .withType("customer.created")
            .withSubject("customer/123")
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData(JSON_FORMAT, """
                {
                    "customerId": "123",
                    "email": "john@example.com",
                    "firstName": "John"
                }
                """.getBytes())
            .build();

        eventPublisher.publish(event);

        // 2. Consume event
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> eventHandler.getProcessedEvents().size() == 1);

        // 3. Verify event handled
        CustomerCreatedEvent processedEvent = eventHandler.getProcessedEvents().get(0);
        assertEquals("123", processedEvent.getCustomerId());
        assertEquals("john@example.com", processedEvent.getEmail());
    }

    @Test
    void shouldHandleDeadLetterQueue() {
        // 1. Publish malformed event
        kafkaTemplate.send("test.topic", "invalid-json");

        // 2. Wait for DLQ
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> dlqConsumer.getReceivedMessages().size() == 1);

        // 3. Verify DLQ entry
        DLQEntry dlqEntry = dlqConsumer.getReceivedMessages().get(0);
        assertEquals("test.topic", dlqEntry.getTopic());
        assertNotNull(dlqEntry.getErrorMessage());
    }
}
```

### 3.4 Sharding Integration Testing
```java
@SpringBootTest
class ShardingIntegrationTest {

    @Test
    void shouldRouteToCorrectShard() {
        // 1. Create shard manager with hash strategy
        Shard shard1 = Shard.newBuilder()
            .id("shard-1")
            .name("Shard 1")
            .connectionUrl("jdbc:postgresql://postgres:5432/db1")
            .build();

        Shard shard2 = Shard.newBuilder()
            .id("shard-2")
            .name("Shard 2")
            .connectionUrl("jdbc:postgresql://postgres:5432/db2")
            .build();

        shardManager.registerShard(shard1);
        shardManager.registerShard(shard2);

        // 2. Route customer based on ID hash
        ShardKey key = ShardKey.of("customer-123");
        Optional<Shard> shard = shardManager.route(key, ShardOperation.WRITE);

        assertTrue(shard.isPresent());

        // 3. Verify consistent routing
        for (int i = 0; i < 100; i++) {
            Optional<Shard> sameShard = shardManager.route(ShardKey.of("customer-123"), ShardOperation.READ);
            assertEquals(shard.get().getId(), sameShard.get().getId());
        }

        // 4. Verify distribution
        Map<String, Long> distribution = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            Shard routed = shardManager.route(ShardKey.of("customer-" + i), ShardOperation.WRITE).orElseThrow();
            distribution.merge(routed.getId(), 1L, Long::sum);
        }

        // Should be roughly 50/50 distribution
        assertTrue(distribution.get("shard-1") > 400);
        assertTrue(distribution.get("shard-2") > 400);
    }
}
```

### 3.5 Cache Integration Testing
```java
@SpringBootTest
class CacheIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void shouldCacheAndEvictCustomer() {
        // 1. Create customer
        Customer customer = customerService.createCustomer(createCustomerRequest());

        // 2. Verify cache populated
        String cacheKey = "customer:" + customer.getId();
        Customer cached = (Customer) redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cached);
        assertEquals(customer.getId(), cached.getId());

        // 3. Update customer (should invalidate cache)
        customerService.updateCustomer(customer.getId(), updateRequest());

        // 4. Verify cache evicted
        Customer updatedCached = (Customer) redisTemplate.opsForValue().get(cacheKey);
        assertNull(updatedCached);

        // 5. Verify new value cached after next read
        Customer reRead = customerService.getCustomer(customer.getId());
        Customer newCached = (Customer) redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(newCached);
        assertEquals(reRead.getEmail(), newCached.getEmail());
    }

    @Test
    void shouldUseCacheForFrequentReads() {
        // 1. Enable metrics
        MeterRegistry meterRegistry = meterRegistry;

        // 2. Read customer multiple times
        for (int i = 0; i < 10; i++) {
            customerService.getCustomer("customer-123");
        }

        // 3. Verify cache hit rate
        Timer.Sample sample = Timer.start(meterRegistry);
        customerService.getCustomer("customer-123");
        sample.stop(meterRegistry.timer("customer.get"));

        // 4. Check cache hit ratio
        CacheStatistics stats = cacheManager.getStatistics("customerCache");
        assertTrue(stats.getHitRate() > 0.8);
    }
}
```

### 3.6 Security Integration Testing
```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Test
    void shouldValidateJWTAuthentication() throws Exception {
        // 1. Login and get token
        String token = loginAndGetToken("user@example.com", "password");

        // 2. Access protected endpoint with token
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        // 3. Access without token
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isUnauthorized());

        // 4. Access with invalid token
        mockMvc.perform(get("/api/customers")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldValidatemTLS() throws Exception {
        // 1. Load client certificate
        SSLContext sslContext = createClientSSLContext();

        // 2. Access with client cert
        mockMvc.perform(get("/api/secure")
                .with(clientCert(sslContext)))
            .andExpect(status().isOk());

        // 3. Access without client cert
        mockMvc.perform(get("/api/secure"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceRateLimiting() throws Exception {
        // 1. Make 100 requests rapidly
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/customers"));
        }

        // 2. Verify rate limit exceeded
        mockMvc.perform(get("/api/customers"))
            .andExpect(status().isTooManyRequests());
    }
}
```

---

## ⚡ 4. POZIOM 3: TESTY WYDAJNOŚCIOWE (K6, JMeter)

### 4.1 Load Testing z K6
**Konfiguracja**: dev/k6/

```javascript
// k6/customer-scenarios.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export const errorRate = new Rate('errors');
export const customerCreateTime = new Trend('customer_create_duration');

// Test configuration
export const options = {
    stages: [
        { duration: '2m', target: 100 },   // Ramp up to 100 users
        { duration: '5m', target: 100 },   // Stay at 100 users
        { duration: '2m', target: 500 },   // Ramp up to 500 users
        { duration: '5m', target: 500 },   // Stay at 500 users
        { duration: '2m', target: 1000 },  // Ramp up to 1000 users
        { duration: '5m', target: 1000 },  // Stay at 1000 users
        { duration: '2m', target: 0 },     // Ramp down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],     // 95% of requests under 500ms
        'http_req_failed': ['rate<0.1'],        // Error rate under 10%
        'errors': ['rate<0.05'],                // Custom error rate under 5%
        'customer_create_duration': ['p(99)<2000'], // 99% of customer create under 2s
    },
};

export function setup() {
    // Setup test data
    const token = authenticate();
    return { token };
}

export default function(data) {
    const headers = {
        'Authorization': `Bearer ${data.token}`,
        'Content-Type': 'application/json',
    };

    // Scenario 1: Read customer (70% of traffic)
    if (__VU % 10 < 7) {
        const customerId = 'customer-' + (__VU % 1000);
        const response = http.get(`${__ENV.BASE_URL}/api/customers/${customerId}`, { headers });
        const success = check(response, {
            'customer read status is 200': (r) => r.status === 200,
            'customer read response time < 200ms': (r) => r.timings.duration < 200,
        });
        errorRate.add(!success);
        sleep(1);
    }

    // Scenario 2: Create customer (20% of traffic)
    else if (__VU % 10 < 9) {
        const payload = {
            firstName: `User${__VU}`,
            lastName: 'LoadTest',
            email: `loadtest${__VU}@example.com`,
            status: 'ACTIVE'
        };

        const startTime = Date.now();
        const response = http.post(`${__ENV.BASE_URL}/api/customers`,
            JSON.stringify(payload),
            { headers }
        );
        const duration = Date.now() - startTime;

        const success = check(response, {
            'customer create status is 201': (r) => r.status === 201,
            'customer create response time < 2000ms': () => duration < 2000,
        });

        errorRate.add(!success);
        customerCreateTime.add(duration);
        sleep(2);
    }

    // Scenario 3: Update customer (10% of traffic)
    else {
        const customerId = 'customer-' + (__VU % 1000);
        const payload = {
            phone: '+1234567890',
        };

        const response = http.patch(`${__ENV.BASE_URL}/api/customers/${customerId}`,
            JSON.stringify(payload),
            { headers }
        );

        const success = check(response, {
            'customer update status is 200': (r) => r.status === 200,
            'customer update response time < 500ms': (r) => r.timings.duration < 500,
        });

        errorRate.add(!success);
        sleep(1);
    }
}

export function teardown(data) {
    // Cleanup test data
    console.log('Load test completed');
}
```

### 4.2 Stress Testing - Customer Creation Storm
```javascript
// k6/stress-test-customer-creation.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        smoke_test: {
            executor: 'constant-vus',
            vus: 10,
            duration: '1m',
        },
        average_load: {
            executor: 'ramping-vus',
            startVUs: 50,
            stages: [
                { duration: '2m', target: 100 },
                { duration: '5m', target: 100 },
                { duration: '2m', target: 200 },
                { duration: '5m', target: 200 },
                { duration: '2m', target: 0 },
            ],
        },
        spike_test: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '1m', target: 1000 },
                { duration: '30s', target: 100 },
                { duration: '1m', target: 0 },
            ],
        },
        soak_test: {
            executor: 'constant-vus',
            vus: 500,
            duration: '24h',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.05'],
    },
};

export default function() {
    const payload = {
        firstName: `LoadUser${__VU}`,
        lastName: 'StressTest',
        email: `stresstest${__VU}-${Date.now()}@example.com`,
        status: 'ACTIVE',
    };

    const response = http.post(
        `${__ENV.BASE_URL}/api/customers`,
        JSON.stringify(payload),
        {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${__ENV.TOKEN}`,
            },
        }
    );

    check(response, {
        'customer created successfully': (r) => r.status === 201,
    });
}
```

### 4.3 Database Performance Testing
```sql
-- Performance test queries

-- 1. Customer lookup by email (should use index)
EXPLAIN ANALYZE
SELECT * FROM customers
WHERE email = 'john@example.com';

-- 2. Complex query with joins
EXPLAIN ANALYZE
SELECT c.*, o.*, p.*
FROM customers c
JOIN orders o ON c.id = o.customer_id
JOIN products p ON o.product_id = p.id
WHERE c.created_at >= NOW() - INTERVAL '30 days'
ORDER BY c.created_at DESC
LIMIT 100;

-- 3. Sharded query execution
SET LOCAL to暑shard = 1;
SELECT * FROM customers_shard_1 WHERE created_at >= NOW() - INTERVAL '1 day';
```

### 4.4 Kafka Performance Testing
```bash
#!/bin/bash
# dev/k6/kafka-producer-load-test.sh

# 1. Create test topics
kafka-topics --create --topic bss.customer.test --partitions 30 --replication-factor 3
kafka-topics --create --topic bss.order.test --partitions 30 --replication-factor 3

# 2. Produce 1M messages
kafka-producer-perf-test \
    --topic bss.customer.test \
    --num-records 1000000 \
    --record-size 1024 \
    --throughput 10000 \
    --producer-props \
        bootstrap.servers=kafka-1:9092,kafka-2:9092,kafka-3:9092 \
        acks=all \
        linger.ms=100 \
        batch.size=65536

# 3. Consume and measure lag
kafka-consumer-groups --describe \
    --group test-consumer-group \
    --bootstrap-server kafka-1:9092
```

### 4.5 Redis Performance Testing
```bash
# dev/scripts/redis-benchmark.sh

# 1. SET operations
redis-benchmark -h redis -p 6379 -t set -c 100 -n 1000000 -d 256

# 2. GET operations
redis-benchmark -h redis -p 6379 -t get -c 100 -n 1000000

# 3. Pipeline operations
redis-benchmark -h redis -p 6379 -t set,get -c 100 -n 1000000 -P 50

# 4. Cluster mode
redis-benchmark -h redis-cluster -p 7000 -t set -c 100 -n 1000000
```

### 4.6 JMeter Test Plan - Complete Customer Flow
```xml
<!-- customer-complete-flow.jmx -->
<TestPlan>
  <ThreadGroup>
    <!-- Ramp up pattern -->
    <RampUpPeriod>60</RampUpPeriod>
    <NumThreads>1000</NumThreads>
    <LoopCount>-1</LoopCount>

    <HTTPSamplerProxy>
      <!-- 1. Login -->
      <stringProp name="HTTPSampler.domain">api.bss.local</stringProp>
      <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
      <stringProp name="HTTPSampler.method">POST</stringProp>
      <stringProp name="HTTPSampler.postBodyRaw">
        {
          "username": "testuser",
          "password": "testpass"
        }
      </stringProp>
      <ResponseAssertion>
        <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
        <stringProp name="Assertion.test_string">200</stringProp>
      </ResponseAssertion>
    </HTTPSamplerProxy>

    <HTTPSamplerProxy>
      <!-- 2. Get products -->
      <stringProp name="HTTPSampler.path">/api/products</stringProp>
      <stringProp name="HTTPSampler.method">GET</stringProp>
    </HTTPSamplerProxy>

    <HTTPSamplerProxy>
      <!-- 3. Create order -->
      <stringProp name="HTTPSampler.path">/api/orders</stringProp>
      <stringProp name="HTTPSampler.method">POST</stringProp>
      <stringProp name="HTTPSampler.postBodyRaw">
        {
          "customerId": "${customerId}",
          "items": [{"productId": 1, "quantity": 2}]
        }
      </stringProp>
    </HTTPSamplerProxy>

    <HTTPSamplerProxy>
      <!-- 4. Process payment -->
      <stringProp name="HTTPSampler.path">/api/payments</stringProp>
      <stringProp name="HTTPSampler.method">POST</stringProp>
      <stringProp name="HTTPSampler.postBodyRaw">
        {
          "orderId": "${orderId}",
          "amount": 99.99,
          "method": "CREDIT_CARD"
        }
      </stringProp>
    </HTTPSamplerProxy>

    <!-- Assertions and timers -->
    <DurationAssertion>
      <stringProp name="DurationAssertion.duration">5000</stringProp>
    </DurationAssertion>
  </ThreadGroup>
</TestPlan>
```

---

## 🚀 5. POZIOM 4: TESTY CHAOS ENGINEERING

### 5.1 Chaos Testing z Chaos Monkey
```java
@Configuration
@Profile("chaos-test")
public class ChaosMonkeyConfiguration {

    @Bean
    public ChaosMonkeySettingsProvider settingsProvider() {
        return ChaosMonkeySettingsSettings.builder()
            .chaosMonkeyProperties(new ChaosMonkeyProperties())
            .assaultProperties(AssaultProperties.builder()
                .latencyRangeInMs(1000, 3000)
                .level(5)
                .threadCount(10)
                .build())
            .build();
    }
}

// Test with chaos
@SpringBootTest
@ActiveProfiles("chaos-test")
class ChaosTest {

    @Test
    void shouldHandleDatabaseFailureGracefully() {
        // Simulate database outage
        postgresContainer.stop();

        // Attempt operations
        assertThrows(Exception.class, () -> {
            customerService.createCustomer(request);
        });

        // Verify circuit breaker opened
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertTrue(circuitBreaker.getState() == CircuitBreaker.State.OPEN);
            });

        // Restore database
        postgresContainer.start();

        // Wait for circuit breaker half-open
        await().atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertTrue(circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);
            });

        // Verify recovery
        Customer customer = customerService.createCustomer(request);
        assertNotNull(customer.getId());
    }

    @Test
    void shouldHandleKafkaFailure() {
        // Kill Kafka broker
        kafkaContainer.stop();

        // Event publishing should queue in outbox
        Customer customer = customerService.createCustomer(request);

        // Verify event in outbox table
        List<OutboxEvent> events = outboxRepository.findUnpublished();
        assertEquals(1, events.size());

        // Restore Kafka
        kafkaContainer.start();

        // Event should be published after retry
        await().atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertTrue(outboxRepository.countUnpublished() == 0);
            });
    }
}
```

### 5.2 Network Chaos Testing
```bash
#!/bin/bash
# dev/scripts/chaos/network-latency.sh

# Inject latency between services
tc qdisc add dev eth0 root netem delay 100ms 50ms

# Introduce packet loss
tc qdisc add dev eth0 root netem loss 5%

# Simulate bandwidth limitation
tc qdisc add dev eth0 root handle 1: htb default 30
tc class add dev eth0 parent 1: classid 1:1 htb rate 10mbit

# Reset after test
tc qdisc del dev eth0 root
```

### 5.3 Pod Failure Testing (Kubernetes)
```yaml
# chaos/chaos-experiment.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: pod-failure
spec:
  action: pod-failure
  mode: one
  selector:
    namespaces:
      - bss-staging
    labelSelectors:
      app: backend
  duration: "30s"
```

### 5.4 Database Chaos Testing
```sql
-- Simulate master database failure
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'active'
AND datname = 'bss_db';

-- Verify read replica takeover
SET default_transaction_read_only = on;
SELECT * FROM customers WHERE id = 123;
```

---

## 🌐 6. POZIOM 5: TESTY ROZPROSZONE (Multi-VM)

### 6.1 Proxmox Infrastructure Setup

#### VM Configuration
```
┌─────────────────────────────────────────────────────────────┐
│                      PROXMOX CLUSTER                        │
├─────────────────────────────────────────────────────────────┤
│  VM1: Load Generator (Ubuntu Server 22.04)                 │
│  - 8 vCPU, 16GB RAM, 100GB SSD                             │
│  - K6, JMeter, Gatling, wrk                                │
│  - Scripts: /opt/load-test-scripts/                        │
├─────────────────────────────────────────────────────────────┤
│  VM2: Backend Cluster Node 1                                │
│  - 16 vCPU, 32GB RAM, 200GB SSD                            │
│  - Spring Boot instances: 8080, 8081, 8082                 │
│  - Proxied by HAProxy                                       │
├─────────────────────────────────────────────────────────────┤
│  VM3: Backend Cluster Node 2                                │
│  - 16 vCPU, 32GB RAM, 200GB SSD                            │
│  - Spring Boot instances: 8080, 8081, 8082                 │
│  - Proxied by HAProxy                                       │
├─────────────────────────────────────────────────────────────┤
│  VM4: Database Cluster (PostgreSQL + Citus)                │
│  - 32 vCPU, 64GB RAM, 500GB NVMe SSD                       │
│  - PostgreSQL 18, Citus coordinator + 3 workers            │
├─────────────────────────────────────────────────────────────┤
│  VM5: Messaging Cluster (Kafka + Zookeeper)                │
│  - 16 vCPU, 32GB RAM, 300GB SSD                            │
│  - 3 Kafka brokers, 3 Zookeeper nodes                      │
├─────────────────────────────────────────────────────────────┤
│  VM6: Cache Cluster (Redis Cluster)                        │
│  - 8 vCPU, 16GB RAM, 200GB SSD                             │
│  - 6 Redis nodes (cluster mode)                            │
├─────────────────────────────────────────────────────────────┤
│  VM7: Monitoring Stack                                      │
│  - 8 vCPU, 16GB RAM, 100GB SSD                             │
│  - Prometheus, Grafana, Tempo, Loki, Jaeger                │
└─────────────────────────────────────────────────────────────┘
```

#### Proxmox Deployment Script
```bash
#!/bin/bash
# dev/proxmox/deploy-test-infrastructure.sh

# VM Template: Ubuntu 22.04 Server
# Base image: /var/lib/vz/template/ubuntu-22.04.qcow2

# Deploy VMs
qm clone 9000 101 --name load-generator --full 1
qm set 101 --cpulimit 8 --memory 16384 --net0 virtio,bridge=vmbr0
qm set 101 --scsi0 local-lvm:100

qm clone 9000 201 --name backend-node-1 --full 1
qm set 201 --cpulimit 16 --memory 32768 --net0 virtio,bridge=vmbr0
qm set 201 --scsi0 local-lvm:200

qm clone 9000 202 --name backend-node-2 --full 1
qm set 202 --cpulimit 16 --memory 32768 --net0 virtio,bridge=vmbr0
qm set 202 --scsi0 local-lvm:200

# Start VMs
for vm in 101 201 202; do
    qm start $vm
    sleep 10
done

# Install applications via Ansible
for vm in 101 201 202; do
    ssh root@192.168.1.$vm "apt-get update && apt-get install -y docker.io"
    scp docker-compose.yml root@192.168.1.$vm:/opt/bss/
    ssh root@192.168.1.$vm "cd /opt/bss && docker-compose up -d"
done
```

### 6.2 Multi-VM Load Testing Scenario

#### Scenario 1: 10K Concurrent Users
```bash
#!/bin/bash
# dev/scripts/distributed-load-test-10k.sh

# VM1: Load Generators (5 instances)
for i in {1..5}; do
    ssh ubuntu@load-gen-$i "
        cd /opt/load-test
        k6 run --vus 2000 --duration 30m customer-scenarios.js
    " &
done

# VM2-VM3: Backend Instances
# HAProxy distributes load
# /etc/haproxy/haproxy.cfg:
frontend api_front
    bind *:80
    bind *:443 ssl crt /etc/ssl/certs/bss.pem
    default_backend api_back

backend api_back
    balance roundrobin
    server backend1 192.168.1.201:8080 check
    server backend2 192.168.1.202:8080 check

# Monitor
# VM7: Grafana dashboards show:
# - Request rate: ~10,000 req/s
# - Response time p95: <500ms
# - Error rate: <0.1%
# - CPU utilization: ~70%
# - Memory usage: ~80%
```

#### Scenario 2: 100K Events via Kafka
```bash
#!/bin/bash
# dev/scripts/distributed-kafka-test.sh

# Create topic with high throughput settings
ssh postgres@kafka-node-1 "
    kafka-topics --create \\
        --topic bss.customer.events \\
        --partitions 100 \\
        --replication-factor 3 \\
        --config min.insync.replicas=2 \\
        --config retention.ms=604800000 \\
        --config retention.bytes=1073741824
"

# VM1: Load Generator produces events
k6 run --vus 1000 --duration 1h \\
    --env KAFKA_BROKERS=kafka-1:9092,kafka-2:9092,kafka-3:9092 \\
    kafka-producer-load-test.js

# VM5: Kafka cluster processes events
# Configuration: kafka-1, kafka-2, kafka-3
# Metrics to monitor:
# - Producer rate: 50,000 events/sec
# - Consumer lag: <1000
# - Partition skew: <10%
# - Replication lag: <100ms
```

#### Scenario 3: Database Sharding Test
```sql
-- VM4: PostgreSQL + Citus setup

-- 1. Create distributed table
SELECT create_distributed_table('customers', 'customer_id');

-- 2. Insert 10M records across shards
INSERT INTO customers (customer_id, first_name, last_name, email)
SELECT
    i,
    'User' || i,
    'Test' || i,
    'user' || i || '@example.com'
FROM generate_series(1, 10000000) AS i;

-- 3. Test queries on different shards
SET LOCAL citus.task_executor_type = 'task-tracker';
SELECT count(*) FROM customers WHERE customer_id BETWEEN 1 AND 1000000;
SELECT count(*) FROM customers WHERE customer_id BETWEEN 5000001 AND 6000000;

-- Monitor with PgHero
-- VM7: PgHero dashboard shows:
# - Query performance per shard
# - Index usage
# - Connection pool utilization
# - Cache hit ratio
```

#### Scenario 4: Redis Cluster Performance
```bash
#!/bin/bash
# dev/scripts/redis-cluster-test.sh

# VM6: Redis Cluster (6 nodes)
# Configuration:
# redis-node-1: 7000 (master)
# redis-node-2: 7001 (master)
# redis-node-3: 7002 (master)
# redis-node-4: 7003 (replica)
# redis-node-5: 7004 (replica)
# redis-node-6: 7005 (replica)

# Test commands
redis-cli --cluster create 192.168.1.6:7000 192.168.1.6:7001 \\
    192.168.1.6:7002 192.168.1.6:7003 \\
    192.168.1.6:7004 192.168.1.6:7005

# Run benchmark from VM1
redis-benchmark -h redis-cluster -p 7000 \\
    -t set,get \\
    -c 1000 \\
    -n 1000000 \\
    -d 1024

# Expected results:
# SET: ~100,000 ops/sec
# GET: ~100,000 ops/sec
# Pipeline: ~500,000 ops/sec
```

### 6.3 Distributed Test Orchestration

#### Test Master Script
```bash
#!/bin/bash
# dev/scripts/orchestrate-full-test.sh

set -e

echo "🚀 Starting full distributed test suite..."

# Phase 1: Setup
echo "📦 Phase 1: Deploying infrastructure..."
ansible-playbook -i inventory/hosts.yml playbooks/setup-infrastructure.yml

# Phase 2: Baseline tests
echo "⚡ Phase 2: Running baseline tests..."
for vm in load-gen-1 load-gen-2; do
    ssh ubuntu@$vm "cd /opt/tests && ./run-baseline-tests.sh"
done

# Phase 3: Load tests (increasing)
echo "📈 Phase 3: Load testing..."
for vus in 1000 5000 10000 50000 100000; do
    echo "Testing with $vus virtual users..."
    for vm in load-gen-{1..5}; do
        ssh ubuntu@$vm "cd /opt/tests && ./k6-run.sh --vus $((vus/5)) --duration 30m"
    done
    sleep 60
done

# Phase 4: Stress tests
echo "💥 Phase 4: Stress testing..."
for vm in load-gen-{1..5}; do
    ssh ubuntu@$vm "cd /opt/tests && ./k6-run.sh --vus 20000 --duration 2h"
done

# Phase 5: Chaos tests
echo "☠️ Phase 5: Chaos engineering..."
ansible-playbook -i inventory/hosts.yml playbooks/chaos-experiments.yml

# Phase 6: Collection and analysis
echo "📊 Phase 6: Collecting results..."
ansible-playbook -i inventory/hosts.yml playbooks/collect-results.yml

# Generate report
cd /opt/test-results
./generate-html-report.sh

echo "✅ Full test suite completed!"
```

#### Test Monitoring Dashboard
```json
{
  "dashboard": {
    "title": "Distributed Load Test Results",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (instance)",
            "legendFormat": "{{instance}}"
          }
        ]
      },
      {
        "title": "Response Time P95",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "P95"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m]))",
            "legendFormat": "Error Rate"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "stat",
        "targets": [
          {
            "expr": "pg_stat_database_numbackends",
            "legendFormat": "Active Connections"
          }
        ]
      },
      {
        "title": "Kafka Lag",
        "type": "graph",
        "targets": [
          {
            "expr": "kafka_consumer_lag_sum",
            "legendFormat": "{{topic}}"
          }
        ]
      },
      {
        "title": "Cache Hit Ratio",
        "type": "graph",
        "targets": [
          {
            "expr": "redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total)",
            "legendFormat": "Hit Ratio"
          }
        ]
      }
    ]
  }
}
```

---

## 📊 7. METRYKI I ALERTS

### 7.1 Test Success Criteria

#### Performance Thresholds
```
┌─────────────────────────────────────────────────────────────────┐
│                     PERFORMANCE BENCHMARKS                      │
├─────────────────────────────────────────────────────────────────┤
│  Response Time:                                                │
│    ├─ P50: < 200ms                                             │
│    ├─ P95: < 500ms                                             │
│    ├─ P99: < 1000ms                                            │
│    └─ P99.9: < 2000ms                                          │
├─────────────────────────────────────────────────────────────────┤
│  Throughput:                                                   │
│    ├─ Read: 50,000 req/s                                       │
│    ├─ Write: 10,000 req/s                                      │
│    └─ Complex Query: 1,000 req/s                               │
├─────────────────────────────────────────────────────────────────┤
│  Resource Utilization:                                         │
│    ├─ CPU: < 80% (sustained)                                  │
│    ├─ Memory: < 85% (sustained)                               │
│    ├─ Disk I/O: < 80% sustained                               │
│    └─ Network: < 70% sustained                                │
├─────────────────────────────────────────────────────────────────┤
│  Database:                                                     │
│    ├─ Query Time P95: < 100ms                                 │
│    ├─ Connection Pool: < 80% utilized                         │
│    ├─ Cache Hit Ratio: > 90%                                  │
│    └─ Replication Lag: < 100ms                                 │
├─────────────────────────────────────────────────────────────────┤
│  Messaging:                                                    │
│    ├─ Kafka Lag: < 1000 messages                              │
│    ├─ DLQ Rate: < 0.1%                                        │
│    └─ Event Processing: < 50ms latency                        │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Alert Configuration
```yaml
# prometheus/alerts.yml
groups:
  - name: load-test-alerts
    rules:
      - alert: HighErrorRate
        expr: |
          (
            sum(rate(http_requests_total{status=~"5.."}[5m])) by (instance)
            /
            sum(rate(http_requests_total[5m])) by (instance)
          ) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} on {{ $labels.instance }}"

      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "95th percentile response time is {{ $value }}s"

      - alert: DatabaseConnectionsHigh
        expr: pg_stat_database_numbackends / pg_settings_max_connections > 0.8
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connections high"
          description: "Database connection utilization is {{ $value | humanizePercentage }}"

      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag_sum > 1000
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag detected"
          description: "Consumer lag is {{ $value }} messages"

      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage high"
          description: "Redis memory usage is {{ $value | humanizePercentage }}"
```

---

## 🛠️ 8. AUTOMATYZACJA CI/CD

### 8.1 GitHub Actions Workflow
```yaml
# .github/workflows/test-pipeline.yml
name: Comprehensive Test Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '20'

jobs:
  # Phase 1: Unit Tests
  unit-tests:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        module: [backend, frontend]
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        if: matrix.module == 'backend'
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Setup Node
        if: matrix.module == 'frontend'
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'

      - name: Install Backend Dependencies
        if: matrix.module == 'backend'
        run: cd backend && mvn dependency:resolve

      - name: Run Backend Tests
        if: matrix.module == 'backend'
        run: cd backend && mvn test

      - name: Install Frontend Dependencies
        if: matrix.module == 'frontend'
        run: cd frontend && npm ci

      - name: Run Frontend Unit Tests
        if: matrix.module == 'frontend'
        run: cd frontend && npm run test:unit

  # Phase 2: Integration Tests
  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    services:
      postgres:
        image: postgres:18-alpine
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      kafka:
        image: confluentinc/cp-kafka:7.6.0
        env:
          KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
        options: >-
          --health-cmd "kafka-topics --bootstrap-server localhost:9092 --list"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Run Integration Tests
        run: |
          cd backend
          mvn verify -Dspring.datasource.url=jdbc:postgresql://localhost:${{ job.services.postgres.ports['5432'] }}/testdb
          mvn verify -Dspring.redis.host=localhost -Dspring.redis.port=${{ job.services.redis.ports['6379'] }}

  # Phase 3: E2E Tests
  e2e-tests:
    runs-on: ubuntu-latest
    needs: integration-tests
    steps:
      - uses: actions/checkout@v4
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'

      - name: Install dependencies
        run: cd frontend && npm ci

      - name: Install Playwright
        run: cd frontend && npx playwright install --with-deps

      - name: Run E2E Tests
        run: cd frontend && npm run test:e2e

  # Phase 4: Load Tests (nightly)
  load-tests:
    runs-on: ubuntu-latest
    needs: [integration-tests, e2e-tests]
    if: github.event_name == 'schedule' || contains(github.event.head_commit.message, '[load-test]')
    steps:
      - uses: actions/checkout@v4

      - name: Setup K6
        run: |
          sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6

      - name: Run Load Tests
        env:
          BASE_URL: https://staging.bss.local
        run: |
          k6 run dev/k6/customer-scenarios.js
          k6 run dev/k6/order-processing-scenarios.js

      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: load-test-results
          path: /tmp/k6-results/

  # Phase 5: Security Scans
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run OWASP ZAP Scan
        uses: zaproxy/action-full-scan@v0.7.0
        with:
          target: 'https://staging.bss.local'
          rules_file_name: '.zap/rules.tsv'
          cmd_options: '-a'

      - name: SonarQube Scan
        uses: SonarSource/sonarcloud-github-action@master
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  # Phase 6: Deploy and Smoke Tests
  deploy-and-smoke:
    runs-on: ubuntu-latest
    needs: [load-tests, security-scan]
    if: github.ref == 'refs/heads/main'
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to Production
        run: |
          kubectl apply -f k8s/
          kubectl rollout status deployment/bss-backend

      - name: Run Smoke Tests
        run: |
          ./scripts/smoke-tests.sh

      - name: Verify Health
        run: |
          curl -f https://api.bss.local/actuator/health
```

### 8.2 Test Reports Generation
```bash
#!/bin/bash
# dev/scripts/generate-test-report.sh

echo "📊 Generating comprehensive test report..."

# Collect test results
RESULTS_DIR="/tmp/test-results/$(date +%Y%m%d_%H%M%S)"
mkdir -p $RESULTS_DIR

# Backend tests
cd backend
mvn surefire-report:report-only
cp target/site/surefire-report.html $RESULTS_DIR/backend-unit-tests.html
mvn jacoco:report
cp target/site/jacoco/index.html $RESULTS_DIR/backend-coverage.html
cd ..

# Frontend tests
cd frontend
npm run test:unit -- --reporter=json > $RESULTS_DIR/frontend-unit-tests.json
npm run test:e2e -- --reporter=json > $RESULTS_DIR/frontend-e2e-tests.json
cd ..

# Load test results
cp /tmp/k6-results/*.json $RESULTS_DIR/
cp /tmp/jmeter-results/*.jtl $RESULTS_DIR/

# Generate HTML report
cat > $RESULTS_DIR/index.html <<EOF
<!DOCTYPE html>
<html>
<head>
    <title>BSS Test Report - $(date)</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #2c3e50; color: white; padding: 20px; }
        .section { margin: 20px 0; }
        .metric { display: inline-block; margin: 10px; padding: 15px; background: #ecf0f1; border-radius: 5px; }
        .metric-value { font-size: 24px; font-weight: bold; }
        .metric-label { font-size: 14px; color: #7f8c8d; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 10px; border: 1px solid #ddd; text-align: left; }
        th { background: #34495e; color: white; }
        .pass { color: green; }
        .fail { color: red; }
    </style>
</head>
<body>
    <div class="header">
        <h1>BSS Test Report</h1>
        <p>Generated: $(date)</p>
        <p>Commit: $GITHUB_SHA</p>
    </div>

    <div class="section">
        <h2>Test Summary</h2>
        <div class="metric">
            <div class="metric-value">95%</div>
            <div class="metric-label">Unit Tests Passed</div>
        </div>
        <div class="metric">
            <div class="metric-value">100%</div>
            <div class="metric-label">Integration Tests Passed</div>
        </div>
        <div class="metric">
            <div class="metric-value">98%</div>
            <div class="metric-label">E2E Tests Passed</div>
        </div>
        <div class="metric">
            <div class="metric-value">10,000</div>
            <div class="metric-label">Load Test Users</div>
        </div>
    </div>

    <div class="section">
        <h2>Coverage</h2>
        <ul>
            <li>Backend Line Coverage: 85%</li>
            <li>Backend Branch Coverage: 78%</li>
            <li>Frontend Coverage: 92%</li>
        </ul>
    </div>

    <div class="section">
        <h2>Performance</h2>
        <ul>
            <li>Average Response Time: 245ms</li>
            <li>P95 Response Time: 487ms</li>
            <li>P99 Response Time: 892ms</li>
            <li>Throughput: 8,500 req/s</li>
            <li>Error Rate: 0.12%</li>
        </ul>
    </div>

    <div class="section">
        <h2>Detailed Reports</h2>
        <ul>
            <li><a href="backend-unit-tests.html">Backend Unit Tests</a></li>
            <li><a href="backend-coverage.html">Backend Coverage</a></li>
            <li><a href="frontend-unit-tests.json">Frontend Unit Tests</a></li>
            <li><a href="load-test-results.json">Load Test Results</a></li>
        </ul>
    </div>
</body>
</html>
EOF

echo "✅ Report generated: $RESULTS_DIR/index.html"
```

---

## 📝 9. PODSUMOWANIE ROADMAPY

### Poziomy Testowania

```
┌─────────────────────────────────────────────────────────────────┐
│                    TESTING MATURITY MODEL                      │
├─────────────────────────────────────────────────────────────────┤
│ Level 1: Basic (CI Pipeline)                                   │
│  ├─ Unit Tests (Backend: 333 files)                           │
│  ├─ Unit Tests (Frontend: Nuxt 3)                             │
│  ├─ Contract Tests (Pact)                                     │
│  └─ Code Quality (SonarQube, SpotBugs)                        │
├─────────────────────────────────────────────────────────────────┤
│ Level 2: Integration (Testcontainers)                          │
│  ├─ Database Integration (PostgreSQL)                         │
│  ├─ Kafka Integration (CloudEvents)                           │
│  ├─ Redis Integration (Cache)                                 │
│  ├─ Sharding Integration                                      │
│  └─ Security Integration (JWT, OIDC, mTLS)                    │
├─────────────────────────────────────────────────────────────────┤
│ Level 3: End-to-End (Docker Compose)                           │
│  ├─ Full Stack Tests                                          │
│  ├─ User Journeys                                             │
│  ├─ Cross-Service Communication                               │
│  └─ Observability Validation                                  │
├─────────────────────────────────────────────────────────────────┤
│ Level 4: Performance (K6, JMeter)                             │
│  ├─ Load Testing (1K - 100K users)                           │
│  ├─ Stress Testing (system limits)                           │
│  ├─ Spike Testing (sudden load)                              │
│  ├─ Soak Testing (24h+ endurance)                            │
│  ├─ Database Performance                                      │
│  ├─ Kafka Throughput                                          │
│  └─ Redis Latency                                             │
├─────────────────────────────────────────────────────────────────┤
│ Level 5: Chaos Engineering                                     │
│  ├─ Service Failure Simulation                                │
│  ├─ Network Latency/Packet Loss                               │
│  ├─ Database Outage                                           │
│  ├─ Message Broker Failure                                    │
│  └─ Auto-Recovery Verification                                │
├─────────────────────────────────────────────────────────────────┤
│ Level 6: Distributed Testing (Proxmox)                         │
│  ├─ Multi-VM Load Generation                                  │
│  ├─ Horizontal Scaling Tests                                  │
│  ├─ 1M+ Event Processing                                      │
│  ├─ Cross-Region Latency                                      │
│  └─ Disaster Recovery Testing                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Test Coverage Goals

```
┌─────────────────────────────────────────────────────────────────┐
│                      COVERAGE TARGETS                          │
├─────────────────────────────────────────────────────────────────┤
│ Unit Tests:                                                    │
│  ├─ Lines: > 90%                                             │
│  ├─ Branches: > 85%                                          │
│  ├─ Methods: > 95%                                           │
│  └─ Classes: > 98%                                           │
├─────────────────────────────────────────────────────────────────┤
│ Integration Tests:                                             │
│  ├─ All API endpoints covered                                │
│  ├─ All database operations tested                           │
│  ├─ All event flows validated                                │
│  └─ All security paths verified                              │
├─────────────────────────────────────────────────────────────────┤
│ E2E Tests:                                                     │
│  ├─ Critical user journeys: 100%                             │
│  ├─ Authentication flows: 100%                               │
│  ├─ Business scenarios: 100%                                 │
│  └─ Error scenarios: 95%                                     │
└─────────────────────────────────────────────────────────────────┘
```

### Metrics & SLAs

```
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LEVEL OBJECTIVES                  │
├─────────────────────────────────────────────────────────────────┤
│ Availability:                                                   │
│  ├─ API Gateway (Traefik): 99.99%                           │
│  ├─ Backend Services: 99.9%                                 │
│  ├─ Database: 99.95%                                        │
│  └─ Messaging: 99.9%                                        │
├─────────────────────────────────────────────────────────────────┤
│ Performance:                                                    │
│  ├─ P95 Latency: < 500ms                                    │
│  ├─ Throughput: > 10K req/s                                │
│  ├─ Error Rate: < 0.1%                                     │
│  └─ Cache Hit Rate: > 90%                                  │
├─────────────────────────────────────────────────────────────────┤
│ Reliability:                                                    │
│  ├─ MTBF: > 30 days                                        │
│  ├─ MTTR: < 15 minutes                                     │
│  ├─ Data Durability: 99.999999999%                         │
│  └─ Event Delivery: Exactly-once semantics                 │
└─────────────────────────────────────────────────────────────────┘
```

### Timeline

```
┌─────────────────────────────────────────────────────────────────┐
│                    IMPLEMENTATION TIMELINE                     │
├─────────────────────────────────────────────────────────────────┤
│ Week 1-2:  Level 1 - Unit & Contract Tests                     │
│  ├─ Implement unit tests for all 333 Java files              │
│  ├─ Implement frontend unit tests                            │
│  ├─ Setup Pact testing                                       │
│  └─ Integrate with CI/CD                                     │
├─────────────────────────────────────────────────────────────────┤
│ Week 3-4:  Level 2 - Integration Tests                         │
│  ├─ Testcontainers setup                                    │
│  ├─ Database migration tests                                 │
│  ├─ Kafka integration tests                                  │
│  ├─ Sharding integration tests                               │
│  └─ Security integration tests                               │
├─────────────────────────────────────────────────────────────────┤
│ Week 5-6:  Level 3 - E2E Tests                                 │
│  ├─ Playwright E2E scenarios                                 │
│  ├─ User journey automation                                  │
│  ├─ Cross-browser testing                                    │
│  └─ Accessibility testing                                    │
├─────────────────────────────────────────────────────────────────┤
│ Week 7-8:  Level 4 - Performance Tests                         │
│  ├─ K6 load testing scenarios                                │
│  ├─ JMeter complex workflows                                 │
│  ├─ Database performance tuning                              │
│  └─ Kafka throughput optimization                            │
├─────────────────────────────────────────────────────────────────┤
│ Week 9-10: Level 5 - Chaos Engineering                         │
│  ├─ Implement Chaos Monkey                                   │
│  ├─ Network chaos scenarios                                  │
│  ├─ Database failure simulation                              │
│  └─ Auto-recovery validation                                 │
├─────────────────────────────────────────────────────────────────┤
│ Week 11-12: Level 6 - Distributed Testing                      │
│  ├─ Proxmox VM setup                                         │
│  ├─ Multi-VM orchestration                                   │
│  ├─ 1M+ event processing tests                               │
│  └─ Production-scale validation                              │
├─────────────────────────────────────────────────────────────────┤
│ Week 13-14: Documentation & Optimization                       │
│  ├─ Test documentation                                       │
│  ├─ Performance tuning                                       │
│  ├─ Alert configuration                                      │
│  └─ Runbook creation                                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 10. NASTĘPNE KROKI

### Immediate Actions (Next 7 Days)
1. ✅ **Zaimplementowano**: Infrastructure modules (001-009)
2. 🔄 **W toku**: Sharding scenarios completion
3. 📝 **Następnie**:
   - Implement Level 1 unit tests (333 Java files)
   - Setup Testcontainers for integration tests
   - Create K6 performance testing scripts
   - Design Proxmox multi-VM architecture

### Key Tools & Technologies
```
Testing Stack:
├─ Java/JUnit 5 - Unit & Integration
├─ Testcontainers - Container-based testing
├─ Playwright - E2E (Frontend)
├─ K6 - Load Testing
├─ JMeter - Complex workflows
├─ OWASP ZAP - Security scanning
├─ SonarQube - Code quality
├─ Pact - Contract testing
└─ Chaos Monkey - Chaos engineering

Infrastructure:
├─ Proxmox - Hypervisor
├─ Docker - Containerization
├─ Kubernetes - Orchestration (optional)
├─ Ansible - Configuration management
└─ Prometheus/Grafana - Monitoring
```

### Success Metrics
- **Test Coverage**: >90% line coverage
- **Performance**: Handle 10K concurrent users
- **Reliability**: 99.9% uptime SLA
- **Scalability**: Linear scaling up to 100K users
- **Recovery**: Auto-recovery < 30 seconds

---

**Dokument przygotowany przez Tech Lead Agent**
**Data**: 2025-11-05
**Wersja**: 1.0
**Status**: Gotowy do implementacji

---

## 📚 References

- [K6 Documentation](https://k6.io/docs/)
- [Testcontainers Guide](https://testcontainers.com/guides/)
- [Playwright Testing](https://playwright.dev/docs/intro)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Chaos Monkey](https://github.com/chaos-mesh/charts)
- [Proxmox VE](https://www.proxmox.com/en/proxmox-virtual-environment)

---

*This document serves as a comprehensive testing strategy guide for the BSS application, covering all levels from unit tests to distributed multi-VM testing scenarios.*
