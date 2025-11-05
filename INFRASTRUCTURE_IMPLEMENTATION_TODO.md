# 🏗️ IMPLEMENTATION PLAN: BACKEND INFRASTRUCTURE COMPONENTS

**Data utworzenia:** 2025-11-05
**Tech Lead:** Architecture & Infrastructure
**Deadline:** 2025-12-15 (6 tygodni)

---

## 📊 OVERVIEW

### **Target: 9 Infrastructure Components**
- **Tier 1 (Critical)**: 3 komponenty - Security & Foundation
- **Tier 2 (High)**: 4 komponenty - Reliability & Performance
- **Tier 3 (Medium)**: 2 komponenty - Advanced Features

### **Total Effort**
- **Estimated Time**: 25-30 dni roboczych (5-6 tygodni)
- **Team**: 2 Backend Developers
- **Dependencies**: Some components depend on others (see below)

---

## 🔴 TIER 1: CRITICAL SECURITY & FOUNDATION

### **TASK 001: Implement JWT Validation**
**Priority:** CRITICAL 🔴
**Estimated Effort:** 2-3 dni
**Dependencies:** None (foundation)
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/auth/jwt/` directory
- [ ] Implement `JwtValidator.java` interface (Port)
- [ ] Implement `JwtValidatorImpl.java` using jose4j (Adapter)
- [ ] Create `JwtProperties.java` for configuration
- [ ] Create `JwtConfig.java` Spring configuration
- [ ] Add JWT validation tests (7-10 test cases)
- [ ] Integration tests with Keycloak
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/auth/jwt/
interface JwtValidator {
    JwtClaims validate(String token);
    boolean isTokenValid(String token);
    UserPrincipal extractUserPrincipal(String token);
}

@Configuration
class JwtConfig {
    @Bean
    JwtValidator jwtValidator(JwtProperties props) {
        return new JwtValidatorImpl(props);
    }
}
```

---

### **TASK 002: Implement OIDC Integration**
**Priority:** CRITICAL 🔴
**Estimated Effort:** 3-4 dni
**Dependencies:** TASK 001 (JWT Validation)
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/auth/oidc/` directory
- [ ] Implement `OidcClient.java` interface
- [ ] Implement `KeycloakClient.java` (Adapter)
- [ ] Create `KeycloakConfig.java` configuration
- [ ] Create `OidcProperties.java` for Keycloak settings
- [ ] Add OIDC flow tests (login, logout, token refresh)
- [ ] Integration tests with Testcontainers Keycloak
- [ ] Update SecurityConfig to use OIDC

#### Technical Details:
```java
// Location: infrastructure/auth/oidc/
interface OidcClient {
    AuthenticationResponse authenticate(String code);
    UserInfo getUserInfo(String accessToken);
    void logout(String refreshToken);
}

@Configuration
@EnableOAuth2Client
class KeycloakConfig {
    @Bean
    OidcClient oidcClient(OidcProperties props) {
        return new KeycloakClient(props);
    }
}
```

---

### **TASK 003: Implement Event Publisher**
**Priority:** CRITICAL 🔴
**Estimated Effort:** 2-3 dni
**Dependencies:** None (foundation)
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/event/publisher/` directory
- [ ] Create `DomainEvent.java` base class
- [ ] Implement `DomainEventPublisher.java` interface
- [ ] Implement `KafkaEventPublisher.java` (Adapter using CloudEvents)
- [ ] Create `EventProperties.java` for Kafka settings
- [ ] Add event publishing tests (10+ test cases)
- [ ] Integration tests with Kafka Testcontainers
- [ ] Documentation of event schemas

#### Technical Details:
```java
// Location: infrastructure/event/publisher/
interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAsync(DomainEvent event);
}

@Configuration
class EventPublisherConfig {
    @Bean
    DomainEventPublisher eventPublisher(EventProperties props) {
        return new KafkaEventPublisher(props);
    }
}
```

---

## 🟡 TIER 2: HIGH PRIORITY - RELIABILITY & PERFORMANCE

### **TASK 004: Implement Event Handlers**
**Priority:** HIGH 🟡
**Estimated Effort:** 2-3 dni
**Dependencies:** TASK 003 (Event Publisher)
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/event/handlers/` directory
- [ ] Create `EventHandler.java` generic interface
- [ ] Create specific handlers:
  - [ ] `CustomerEventHandler.java`
  - [ ] `OrderEventHandler.java`
  - [ ] `InvoiceEventHandler.java`
- [ ] Create `EventHandlerRegistry.java` for handler management
- [ ] Add event handler tests (15+ test cases)
- [ ] Integration tests with Event Publisher
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/event/handlers/
interface EventHandler<T extends DomainEvent> {
    void handle(T event);
    String getSupportedEventType();
}

@Component
class CustomerEventHandler implements EventHandler<CustomerCreatedEvent> {
    @Override
    public void handle(CustomerCreatedEvent event) {
        // Handle event
    }
}
```

---

### **TASK 005: Implement Connection Pooling**
**Priority:** HIGH 🟡
**Estimated Effort:** 2-3 dni
**Dependencies:** None
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/database/pooling/` directory
- [ ] Create `ConnectionPool.java` interface
- [ ] Implement `HikariConnectionPool.java` (Adapter)
- [ ] Create `DatabasePoolProperties.java` configuration
- [ ] Create `DatabasePoolConfig.java` Spring configuration
- [ ] Add connection pool tests (8+ test cases)
- [ ] Load testing scenarios
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/database/pooling/
interface ConnectionPool {
    Connection getConnection();
    void returnConnection(Connection conn);
    PoolStats getStats();
}

@Configuration
class DatabasePoolConfig {
    @Bean
    ConnectionPool connectionPool(DatabasePoolProperties props) {
        return new HikariConnectionPool(props);
    }
}
```

---

### **TASK 006: Implement Transaction Management**
**Priority:** HIGH 🟡
**Estimated Effort:** 3-4 dni
**Dependencies:** TASK 005 (Connection Pooling)
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/database/transaction/` directory
- [ ] Create `TransactionManager.java` interface
- [ ] Implement `SpringTransactionManager.java` (Adapter)
- [ ] Create `DistributedTransactionManager.java` for multi-DB transactions
- [ ] Add transaction tests (10+ test cases)
- [ ] Integration tests with multiple databases
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/database/transaction/
interface TransactionManager {
    void executeInTransaction(TransactionCallback callback);
    void executeInNestedTransaction(TransactionCallback callback);
}

@Service
class OrderService {
    @Autowired
    private TransactionManager txManager;

    public void createOrder(Order order) {
        txManager.executeInTransaction(() -> {
            // Multi-database operations
        });
    }
}
```

---

### **TASK 007: Implement Cache Eviction Strategies**
**Priority:** HIGH 🟡
**Estimated Effort:** 2 dni
**Dependencies:** Redis already configured
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/cache/eviction/` directory
- [ ] Create `EvictionPolicy.java` interface
- [ ] Implement policies:
  - [ ] `LRUEvictionPolicy.java`
  - [ ] `LFUEvictionPolicy.java`
  - [ ] `TTLEvictionPolicy.java`
- [ ] Create `CacheEvictionManager.java`
- [ ] Add cache eviction tests (12+ test cases)
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/cache/eviction/
interface EvictionPolicy<T> {
    void onAccess(T item);
    List<T> evict(int count);
}

@Component
public class CacheEvictionManager {
    public void evictIfNeeded(String cacheName) {
        EvictionPolicy<Object> policy = getPolicy(cacheName);
        // Apply eviction logic
    }
}
```

---

## 🟢 TIER 3: MEDIUM PRIORITY - ADVANCED FEATURES

### **TASK 008: Implement Dead Letter Queue**
**Priority:** MEDIUM 🟢
**Estimated Effort:** 2-3 dni
**Dependencies:** Kafka already configured
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/messaging/deadletter/` directory
- [ ] Create `DeadLetterQueue.java` interface
- [ ] Implement `KafkaDeadLetterQueue.java` (Adapter)
- [ ] Create `DeadLetterListener.java` for reprocessing
- [ ] Add DLQ tests (8+ test cases)
- [ ] Integration tests with failed messages
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/messaging/deadletter/
interface DeadLetterQueue {
    void sendToDeadLetter(Message failedMessage, String reason);
    List<Message> getDeadLetters(String topic);
    void reprocessDeadLetter(Message message);
}

@Component
public class MessageProcessor {
    @Autowired
    private DeadLetterQueue dlq;

    public void process(Message message) {
        try {
            // Process message
        } catch (Exception e) {
            dlq.sendToDeadLetter(message, e.getMessage());
        }
    }
}
```

---

### **TASK 009: Implement Sharding Scenarios**
**Priority:** MEDIUM 🟢
**Estimated Effort:** 4-5 dni
**Dependencies:** TASK 005 & 006 (Connection Pooling & Transaction Management)
**Status:** PENDING

#### Actions Required:
- [ ] Create `infrastructure/database/sharding/` directory
- [ ] Create `ShardManager.java` interface
- [ ] Implement `CustomerIdShardStrategy.java`
- [ ] Implement `TimeBasedShardStrategy.java`
- [ ] Create `ShardAwareRepository.java` base class
- [ ] Add sharding tests (15+ test cases)
- [ ] Integration tests with PostgreSQL partitioning
- [ ] Documentation

#### Technical Details:
```java
// Location: infrastructure/database/sharding/
interface ShardManager {
    String getShardId(ShardKey key);
    List<String> getAllShardIds();
}

interface ShardAwareRepository<T> {
    T save(T entity, String shardId);
    Optional<T> findById(String id, String shardId);
}

@Repository
class CustomerRepository extends ShardAwareRepository<Customer> {
    // Shard-aware implementation
}
```

---

## 📅 IMPLEMENTATION SCHEDULE

### **Week 1 (Nov 11-17): Foundation**
- Day 1-3: **TASK 001** - JWT Validation
- Day 4-7: **TASK 002** - OIDC Integration

### **Week 2 (Nov 18-24): Foundation + Events**
- Day 1-3: **TASK 003** - Event Publisher
- Day 4-7: Begin **TASK 004** - Event Handlers

### **Week 3 (Nov 25-Dec 1): Database Foundation**
- Day 1-3: Complete **TASK 004** - Event Handlers
- Day 4-7: **TASK 005** - Connection Pooling

### **Week 4 (Dec 2-8): Transactions & Cache**
- Day 1-4: **TASK 006** - Transaction Management
- Day 5-7: **TASK 007** - Cache Eviction Strategies

### **Week 5 (Dec 9-12): Advanced Features**
- Day 1-3: **TASK 008** - Dead Letter Queue
- Day 4: Begin **TASK 009** - Sharding Scenarios

### **Week 6 (Dec 13-15): Advanced Features (Final)**
- Day 1-3: Complete **TASK 009** - Sharding Scenarios
- Day 4-5: Final review, testing, documentation

---

## 🎯 SUCCESS CRITERIA

For each task:
- [ ] **Code Quality**: Clean code, SOLID principles
- [ ] **Tests**: >90% coverage, including integration tests
- [ ] **Documentation**: JavaDoc + README
- [ ] **Configuration**: Properties/Configuration classes
- [ ] **Integration**: Works with existing infrastructure
- [ ] **Performance**: Meets SLA requirements

---

## 🔗 DEPENDENCIES & RELATIONSHIPS

```
TASK 001 (JWT) ──┐
                ├──> TASK 002 (OIDC)
TASK 003 (Event Publisher) ──> TASK 004 (Event Handlers)
                         └─> TASK 008 (Dead Letter Queue)

TASK 005 (Connection Pooling) ──> TASK 006 (Transaction Management)

TASK 006 (Transaction Management) ──> TASK 009 (Sharding)

TASK 007 (Cache Eviction) ──> Independent

TASK 009 (Sharding) ──> Dependent on TASK 005 & 006
```

---

## 📚 TECHNOLOGIES & LIBRARIES

- **JWT**: jose4j (for JWT validation)
- **OIDC**: spring-security-oauth2-client
- **Events**: CloudEvents v1.0 (already in use)
- **Database**: HikariCP (already in Spring Boot)
- **Transactions**: Spring Transaction Management (already available)
- **Cache**: Redis (already configured)
- **Dead Letters**: Apache Kafka (already configured)
- **Sharding**: PostgreSQL partitioning + application routing

---

## 👥 TEAM ALLOCATION

**Developer 1 (Senior):**
- TASK 001: JWT Validation
- TASK 002: OIDC Integration
- TASK 006: Transaction Management
- TASK 009: Sharding Scenarios

**Developer 2 (Mid-level):**
- TASK 003: Event Publisher
- TASK 004: Event Handlers
- TASK 005: Connection Pooling
- TASK 007: Cache Eviction
- TASK 008: Dead Letter Queue

**Tech Lead (Oversight):**
- Architecture reviews
- Code reviews
- Integration testing
- Documentation review

---

## 📝 DELIVERABLES

1. **Code**: 9 complete infrastructure components
2. **Tests**: 80+ test cases across all components
3. **Configuration**: Spring Boot configuration classes
4. **Documentation**: Technical documentation for each component
5. **Integration**: All components integrated into main codebase

---

## 🚨 RISKS & MITIGATION

**Risk 1: OIDC Integration complexity**
- Mitigation: Start with JWT validation first, use Testcontainers for Keycloak

**Risk 2: Transaction Management edge cases**
- Mitigation: Extensive integration tests, use PostgreSQL features

**Risk 3: Sharding complexity**
- Mitigation: Start simple (customer-based), add complexity gradually

**Risk 4: Timeline pressure**
- Mitigation: Critical path focus (Tier 1 first), defer advanced features if needed

---

**STATUS**: Ready to begin implementation
**NEXT STEP**: Start with TASK 001 - JWT Validation

---

*This plan will be reviewed weekly and updated based on progress and changing requirements.*
