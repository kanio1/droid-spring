# Testcontainers Integration Test Framework Setup

**Date:** 2025-11-05
**Status:** CONFIGURED ✅

## Overview

This document describes the Testcontainers integration test framework for the BSS (Business Support System) project. Testcontainers enables lightweight, disposable instances of dependencies (PostgreSQL, Kafka, Redis, etc.) for integration testing without requiring external services.

## Architecture

### Provided Containers

The `AbstractIntegrationTest` base class provides the following containers:

1. **PostgreSQL 18**
   - Version: `postgres:18-alpine`
   - Database: `testdb`
   - User/Pass: `test/test`
   - Used for: Database layer testing

2. **Apache Kafka**
   - Version: `confluentinc/cp-kafka:7.4.0`
   - Port: 9093 (exposed)
   - Used for: Event streaming, CloudEvents testing

3. **Redis 7**
   - Version: `redis:7-alpine`
   - Port: 6379 (exposed)
   - Database: 15 (test isolation)
   - Used for: Caching, session storage

## Usage Examples

### 1. Repository Layer Tests (@DataJpaTest)

Extend `AbstractIntegrationTest` or create your own container setup:

```java
@DataJpaTest
@Testcontainers
@EnableJpaAuditing
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CustomerRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldSaveCustomer() {
        // Test implementation
    }
}
```

### 2. Full Integration Tests (@SpringBootTest)

Extend `AbstractIntegrationTest` for complete integration testing:

```java
@SpringBootTest
class BillingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private IngestUsageRecordUseCase ingestUsageRecordUseCase;

    @Test
    void shouldProcessBillingCycle() {
        // Test implementation
        // All containers automatically configured:
        // - PostgreSQL database
        // - Kafka message broker
        // - Redis cache
    }
}
```

### 3. Kafka Integration Tests

Test Kafka producers and consumers:

```java
@SpringBootTest
class KafkaEventIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Test
    void shouldSendAndReceiveEvent() {
        // Given
        String topic = "test.topic";
        TestEvent event = new TestEvent("test-id", "test-data");

        // When
        kafkaTemplate.send(topic, event);

        // Then - verify event received
        // Kafka container is automatically configured
    }
}
```

### 4. Redis Integration Tests

Test caching and session management:

```java
@SpringBootTest
class RedisIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldStoreAndRetrieveValue() {
        // Given
        String key = "test:key";
        String value = "test-value";

        // When
        redisTemplate.opsForValue().set(key, value);

        // Then
        String retrieved = redisTemplate.opsForValue().get(key);
        assertThat(retrieved).isEqualTo(value);
    }
}
```

## Configuration

### Test Application Properties

**Location:** `src/test/resources/application-test.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: test
    password: test
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration

  kafka:
    bootstrap-servers: localhost:9093
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

  data:
    redis:
      host: localhost
      port: 6379
      database: 15

# Disable security for tests
spring security:
  oauth2:
    resourceserver:
      jwt:
        issuer-uri: http://localhost:8081/realms/bss
```

### Maven Dependencies

**Location:** `pom.xml`

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redis</artifactId>
    <scope>test</scope>
</dependency>
```

## Best Practices

### 1. Container Lifecycle

- Containers are started once per test class (static fields)
- Automatically stopped after all tests complete
- Use `@DirtiesContext` to reset Spring context if needed

### 2. Test Isolation

```java
// Good: Clean database before each test
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MyTest {
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
}
```

### 3. Test Database Configuration

```java
// Use Flyway for schema migration in tests
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
```

### 4. Avoid Network Dependencies

Tests run without external services:
- No need for local PostgreSQL
- No need for local Kafka cluster
- No need for local Redis
- All dependencies provided by Testcontainers

### 5. Performance Optimization

```java
// Reuse containers across tests
@ClassRule
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withStartupTimeout(Duration.ofSeconds(30));
```

## Running Tests

### Maven

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerRepositoryTest

# Run with coverage
mvn verify -Djacoco.skip=false

# Run integration tests only
mvn test -Dtest.groups=integration
```

### IDE

Tests can be run directly from IDE:
1. Right-click on test class
2. Select "Run" or "Run with Coverage"
3. Containers automatically start

### CI/CD

```yaml
# Example GitHub Actions
- name: Run tests
  run: mvn clean verify -DskipITs=false

# Tests automatically use Testcontainers
# No additional CI configuration needed
```

## Container Customization

### PostgreSQL Customization

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/test-data.sql")  // Load test data
        .withExposedPorts(5432);  // Expose port for debugging
```

### Kafka Customization

```java
@Container
static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
        .withExposedPorts(9093)
        .withEmbeddedZookeeper();  // Include Zookeeper
```

### Redis Customization

```java
@Container
static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withCommand("redis-server", "--save", "");  // Disable persistence for faster tests
```

## Debugging Tests

### 1. Container Logs

```java
@AfterEach
void tearDown() {
    // Print container logs for debugging
    System.out.println("PostgreSQL logs:");
    System.out.println(postgres.getLogs());

    System.out.println("Kafka logs:");
    System.out.println(kafka.getLogs());
}
```

### 2. Connect to Test Database

```java
// Use container.getJdbcUrl() to connect from IDE
String jdbcUrl = postgres.getJdbcUrl();
// Connect with: psql $jdbcUrl
```

### 3. Connect to Test Kafka

```java
// Use container.getBootstrapServers()
String bootstrapServers = kafka.getBootstrapServers();
// Use with kafka-console-producer.sh for testing
```

### 4. Inspect Redis Data

```java
// Connect to Redis for debugging
RedisConnection connection = redis.getConnection();
RedisCommands<String, String> commands = connection.sync();
String value = commands.get("test:key");
```

## Troubleshooting

### 1. Container Fails to Start

**Error:** `Container startup failed`

**Solution:**
- Check Docker is running
- Verify Docker has enough memory
- Check network connectivity
- Increase startup timeout

```java
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withStartupTimeout(Duration.ofSeconds(60));
```

### 2. Port Already in Use

**Error:** `Port 5432 is already allocated`

**Solution:**
- Check no local PostgreSQL running on port 5432
- Testcontainers will automatically use different port if needed
- Verify no other tests using same static container

### 3. Tests Slow

**Symptoms:** Tests taking too long to run

**Solutions:**
- Use `@DirtiesContext` only when necessary
- Consider using `@TestInstance(PER_CLASS)` to share context
- Disable Flyway if not needed: `spring.flyway.enabled=false`
- Use in-memory database for simple tests

### 4. OutOfMemoryError

**Error:** Java heap space or container memory error

**Solution:**
- Increase Docker memory limit
- Add JVM options: `-Xmx512m`
- Use smaller test datasets
- Clean up test data between tests

## Testing Patterns

### 1. Repository Pattern Testing

```java
@DataJpaTest
@Testcontainers
class CustomerRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldSaveCustomer() {
        // Given
        Customer customer = createTestCustomer();

        // When
        Customer saved = customerRepository.save(customer);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
    }
}
```

### 2. Use Case Testing

```java
@SpringBootTest
class CreateCustomerUseCaseTest extends AbstractIntegrationTest {

    @Autowired
    private CreateCustomerUseCase createCustomerUseCase;

    @Test
    void shouldCreateCustomer() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John", "Doe", "john@example.com", "123456789"
        );

        // When
        CustomerResponse response = createCustomerUseCase.handle(command);

        // Then
        assertThat(response.id()).isNotNull();
        assertThat(response.email()).isEqualTo("john@example.com");
    }
}
```

### 3. Event Publishing Testing

```java
@SpringBootTest
class CustomerEventPublisherTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerEventPublisher eventPublisher;

    @Autowired
    private KafkaTestUtils kafkaTestUtils;

    @Test
    void shouldPublishCustomerCreatedEvent() {
        // Given
        Customer customer = createTestCustomer();

        // When
        eventPublisher.publishCustomerCreated(customer);

        // Then
        ConsumerRecord<String, Object> record = kafkaTestUtils
                .waitForNextMessage("customer.events");
        assertThat(record.key()).isEqualTo(customer.getId().toString());
    }
}
```

### 4. Caching Testing

```java
@SpringBootTest
class CustomerServiceCacheTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldCacheCustomerData() {
        // Given
        String customerId = "customer-123";

        // When - first call
        Customer customer1 = customerService.getCustomer(customerId);

        // When - second call (should hit cache)
        Customer customer2 = customerService.getCustomer(customerId);

        // Then
        assertThat(customer2).isEqualTo(customer1);
        // Verify cached in Redis
        String cached = redisTemplate.opsForValue().get("customer:" + customerId);
        assertThat(cached).isNotNull();
    }
}
```

## Advantages of Testcontainers

### 1. Real Dependencies

- Test against real PostgreSQL (not H2 in-memory)
- Test against real Kafka (not embedded)
- Test against real Redis
- Catch integration issues early

### 2. No External Setup

- No need to install PostgreSQL locally
- No need to install Kafka locally
- No need to install Redis locally
- Tests run anywhere

### 3. Isolation

- Each test class gets fresh containers
- No test data pollution
- Consistent test environment
- Reproducible tests

### 4. CI/CD Friendly

- Tests work in CI pipelines
- No additional CI configuration
- Fast container startup
- Parallel test execution supported

## Comparison with Alternatives

### Testcontainers vs H2

| Feature | Testcontainers | H2 |
|---------|----------------|-----|
| Real database | ✅ PostgreSQL | ❌ H2 (different SQL dialect) |
| Transaction isolation | ✅ | ✅ |
| Performance | Slower | Faster |
| Realism | ✅ | ❌ |

### Testcontainers vs Embedded Kafka

| Feature | Testcontainers | Embedded Kafka |
|---------|----------------|----------------|
| Real Kafka | ✅ | ✅ (embeddable) |
| Configuration | Realistic | Simplified |
| Zookeeper | Optional | Included |
| Performance | Good | Good |

### Testcontainers vs Local Services

| Feature | Testcontainers | Local Services |
|---------|----------------|----------------|
| Setup | Automatic | Manual |
| CI/CD | Easy | Complex |
| Isolation | ✅ | ❌ |
| Cleanup | Automatic | Manual |

## Performance Tips

### 1. Container Reuse

```java
// Static containers are shared across all test methods in a class
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("testdb");

// Reused for all tests - faster startup
```

### 2. Parallel Execution

Enable parallel tests in `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <parallel>classes</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

### 3. Test Data Optimization

```java
// Use small test datasets
@Test
void shouldFindCustomer() {
    // Create 1-2 customers for test
    // Not 1000 customers
}
```

## Example Test Suite

### Complete Integration Test

```java
@SpringBootTest
class CompleteBillingFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CreateCustomerUseCase createCustomerUseCase;

    @Autowired
    private IngestUsageRecordUseCase ingestUsageRecordUseCase;

    @Autowired
    private StartBillingCycleUseCase startBillingCycleUseCase;

    @Autowired
    private ProcessBillingCycleUseCase processBillingCycleUseCase;

    @Test
    void shouldCompleteFullBillingFlow() {
        // Step 1: Create customer
        CreateCustomerCommand customerCmd = new CreateCustomerCommand(
                "John", "Doe", "john@example.com", "123456789"
        );
        CustomerResponse customer = createCustomerUseCase.handle(customerCmd);

        // Step 2: Ingest usage records
        IngestUsageRecordCommand usageCmd = new IngestUsageRecordCommand(
                customer.id(),
                "VOICE_CALL",
                "MINUTES",
                BigDecimal.valueOf(10),
                LocalDate.now(),
                LocalTime.now(),
                "MOBILE",
                "+1234567890",
                "US",
                "PEAK",
                "CDR_FILE",
                "CDR"
        );
        UsageRecordResponse usage = ingestUsageRecordUseCase.handle(usageCmd);

        // Step 3: Start billing cycle
        StartBillingCycleCommand cycleCmd = new StartBillingCycleCommand(
                customer.id(),
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "MONTHLY"
        );
        BillingCycleEntity cycle = startBillingCycleUseCase.handle(cycleCmd);

        // Step 4: Process billing cycle
        BillingCycleEntity processed = processBillingCycleUseCase.handle(cycle.getId());

        // Assertions
        assertThat(processed.getStatus()).isEqualTo(BillingCycleStatus.PROCESSED);
        assertThat(processed.getInvoices()).hasSize(1);
        assertThat(processed.getInvoices().get(0).getTotalAmount())
                .isGreaterThan(BigDecimal.ZERO);
    }
}
```

## Summary

The Testcontainers framework provides:

- ✅ **Real Dependencies**: PostgreSQL, Kafka, Redis
- ✅ **Easy Setup**: No external services required
- ✅ **Isolation**: Each test gets fresh environment
- ✅ **CI/CD Ready**: Works in any pipeline
- ✅ **Realistic Testing**: Test against production-like services
- ✅ **Best Practices**: Built-in patterns and examples
- ✅ **Documentation**: Comprehensive guides

**Total Containers Provided:** 3 (PostgreSQL, Kafka, Redis)
**Base Test Class:** `AbstractIntegrationTest`
**Integration Tests Available:** 6+ using `@SpringBootTest`

Tests now run with real dependencies, catching integration issues early, and working consistently across all environments.
