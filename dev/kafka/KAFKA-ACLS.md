# Kafka ACLs (Access Control Lists) Implementation

## Overview

Kafka ACLs provide fine-grained access control to Kafka resources, ensuring that each service has only the minimum permissions required to perform its functions. This implements the **principle of least privilege** and prevents unauthorized access to topics, consumer groups, and cluster operations.

## Security Model

### Principle of Least Privilege

Each service account is granted only the specific permissions it needs:

```
┌──────────────────────────────────────────┐
│  Service: Backend Service                │
│  Permissions:                            │
│    - Write: bss.customer.events          │
│    - Read:  bss.customer.events          │
│    - Write: bss.order.events             │
│    - Read:  bss.payment.events           │
│    - Group: bss-backend-group            │
│    ❌ Cannot read other services' topics │
│    ❌ Cannot write to analytics topics   │
│    ❌ Cannot access other groups         │
└──────────────────────────────────────────┘
```

### Service Accounts

| Service | Principal | Purpose | Permissions |
|---------|-----------|---------|-------------|
| `backend-service` | `User:backend-service` | Main application | Read/Write bss.* topics, manage own group |
| `frontend-service` | `User:frontend-service` | Frontend commands | Limited read/write to specific topics |
| `customer-analytics-service` | `User:customer-analytics-service` | Analytics | Read customer events, write analytics |
| `order-processor-service` | `User:order-processor-service` | Order processing | Read order events, write analytics |
| `payment-processor-service` | `User:payment-processor-service` | Payment processing | Read payment events only |
| `invoice-service` | `User:invoice-service` | Invoice processing | Read invoice events only |
| `monitoring-service` | `User:monitoring-service` | Monitoring | Read-only access to all resources |
| `schema-registry` | `User:schema-registry` | Schema management | Full access to _schemas topic |

## Architecture

### ACL Structure

```
┌──────────────────────────────────────────┐
│  Resource Type: Topic                     │
│  Resource Name: bss.customer.events       │
│                                           │
│  Permissions:                             │
│    User:backend-service         → Write   │
│    User:customer-analytics-service → Read │
│    User:frontend-service        → Read    │
│                                           │
│  Deny Rules:                              │
│    User:payment-processor-service → Denied│
│    User:invoice-service         → Denied  │
└──────────────────────────────────────────┘
```

### Resource Types

1. **Topic**: Control access to Kafka topics
2. **Group**: Control access to consumer groups
3. **Cluster**: Control cluster-level operations
4. **Broker**: Control broker configuration access
5. **TransactionalID**: Control transaction access

### Operations

| Operation | Description | Use Case |
|-----------|-------------|----------|
| `Read` | Consume messages from topic | Consumer applications |
| `Write` | Produce messages to topic | Producer applications |
| `Describe` | View topic metadata | All services |
| `Create` | Create new topics | Admin only |
| `Delete` | Delete topics | Admin only |
| `Alter` | Modify topic configuration | Admin only |
| `AlterConfigs` | Modify broker configs | Admin only |
| `DescribeConfigs` | View broker configs | All services |
| `Read` (Group) | Consume from group | Consumer groups |
| `Describe` (Group) | View group metadata | All services |
| `IdempotentWrite` | Enable idempotent producer | Producers |
| `Describe` (Cluster) | View cluster metadata | All services |

## Implementation

### 1. Running the ACL Setup

```bash
# Run the ACL setup script
./dev/kafka/kafka-acls-setup.sh

# Check the log file
cat /tmp/kafka-acls-setup.log

# List all ACLs
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list
```

### 2. Client Configuration

#### Spring Boot Application (backend-service)

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
    security:
      protocol: SSL
    ssl:
      trust-store-location: classpath:ssl/truststore.jks
      trust-store-password: ${KAFKA_TRUSTSTORE_PASSWORD}
      key-store-location: classpath:ssl/kafka.p12
      key-store-password: ${KAFKA_KEYSTORE_PASSWORD}
      key-store-type: PKCS12
      enabled-protocols: TLSv1.2,TLSv1.3
    producer:
      client-id: backend-service
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 5
    consumer:
      group-id: bss-backend-group
      client-id: backend-service
      auto-offset-reset: earliest
      enable-auto-commit: false
```

#### Java Producer Example

```java
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${KAFKA_KEYSTORE_PASSWORD}")
    private String keystorePassword;

    @Value("${KAFKA_TRUSTSTORE_PASSWORD}")
    private String truststorePassword;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            "kafka-1:9092,kafka-2:9092,kafka-3:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class);

        // SSL Configuration
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
            "SSL");
        configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            "/etc/ssl/certs/truststore.jks");
        configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
            truststorePassword);
        configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
            "/etc/ssl/certs/kafka.p12");
        configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
            keystorePassword);
        configProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        configProps.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG,
            "TLSv1.2,TLSv1.3");

        // Client ID
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "backend-service");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

#### Java Consumer Example

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${KAFKA_KEYSTORE_PASSWORD}")
    private String keystorePassword;

    @Value("${KAFKA_TRUSTSTORE_PASSWORD}")
    private String truststorePassword;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            "kafka-1:9092,kafka-2:9092,kafka-3:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.droid.bss.*");

        // SSL Configuration
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
            "/etc/ssl/certs/truststore.jks");
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
            truststorePassword);
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
            "/etc/ssl/certs/kafka.p12");
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
            keystorePassword);
        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
        props.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG,
            "TLSv1.2,TLSv1.3");

        // Client ID and Group ID
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "backend-service");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "bss-backend-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAuthorizationExceptionRetryInterval(
            Duration.ofSeconds(10));
        return factory;
    }
}
```

#### Consumer Listener Example

```java
@Component
public class CustomerEventListener {

    private static final Logger log = LoggerFactory
        .getLogger(CustomerEventListener.class);

    @KafkaListener(
        topics = "bss.customer.events",
        groupId = "bss-backend-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(ConsumerRecord<String, CloudEvent> record) {
        log.info("Received event: key={}, topic={}, partition={}, offset={}",
            record.key(), record.topic(),
            record.partition(), record.offset());

        CloudEvent event = record.value();

        try {
            // Process the event
            processCustomerEvent(event);
            log.info("Successfully processed event: {}", event.getId());
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getId(), e);
            // Event will be sent to DLQ by error handler
        }
    }

    private void processCustomerEvent(CloudEvent event) {
        // Implementation
    }
}
```

### 3. Testing ACL Enforcement

#### Test 1: Unauthorized Write Attempt

```java
@Test
public void testUnauthorizedWrite() {
    // Configure producer with wrong service account
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1:9092");
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "unauthorized-service");
    // ... other config

    try (KafkaProducer<String, String> producer =
            new KafkaProducer<>(props)) {
        producer.send(new ProducerRecord<>("bss.customer.events", "test"));

        // Should fail with AuthorizationException
        fail("Expected AuthorizationException");
    } catch (KafkaException e) {
        assertTrue(e.getCause() instanceof AuthorizationException);
        log.info("ACL correctly prevented unauthorized write");
    }
}
```

#### Test 2: Authorized Read

```java
@Test
public void testAuthorizedRead() {
    // Configure consumer with correct service account
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1:9092");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "bss-backend-group");
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "backend-service");
    // ... other config

    try (KafkaConsumer<String, String> consumer =
            new KafkaConsumer<>(props)) {
        consumer.subscribe(Collections.singletonList("bss.customer.events"));

        // Should succeed
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
        assertNotNull(records);
        log.info("ACL correctly allowed authorized read");
    }
}
```

## Deny Rules

### Internal Topics Protection

Internal Kafka topics (starting with `__`) are protected from access by non-admin users:

```
Principal: User:*
Operation: All
Resource: Topic (__.*)
Action: Deny
```

**Impact:**
- Users cannot access `__consumer_offsets`, `__transaction_state`, etc.
- Protects Kafka internal operations
- Only admin services can access internal topics

### Cross-Service Protection

Consumer groups are protected from cross-service access:

```
User:frontend-service
Operation: All
Resource: Group (bss-backend-group)
Action: Deny
```

**Impact:**
- Frontend service cannot consume from backend's consumer group
- Each service's consumer group is isolated
- Prevents cross-service message interference

## Monitoring and Auditing

### 1. List All ACLs

```bash
# List all ACLs
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list

# List ACLs for specific user
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list \
    --principal User:backend-service

# List ACLs for specific topic
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list \
    --resource-type Topic \
    --resource-name bss.customer.events
```

### 2. Monitor Authorization Failures

```bash
# Check Kafka logs for authorization failures
docker logs bss-kafka-1 | grep -i "authorization"

# Monitor in real-time
docker exec bss-kafka-1 tail -f /opt/kafka/logs/server.log | grep -i "authorization"
```

### 3. Check Consumer Group Access

```bash
# List consumer groups (requires Describe permission)
docker exec bss-kafka-1 kafka-consumer-groups.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list

# Describe specific group
docker exec bss-kafka-1 kafka-consumer-groups.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --describe \
    --group bss-backend-group
```

### 4. Check Topic Access

```bash
# List topics (requires Describe permission)
docker exec bss-kafka-1 kafka-topics.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list

# Describe specific topic
docker exec bss-kafka-1 kafka-topics.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --describe \
    --topic bss.customer.events
```

## Troubleshooting

### Issue 1: AuthorizationException on Write

**Symptom:**
```
org.apache.kafka.common.errors.AuthorizationException:
    Not authorized to access resources: Topic授权 failed
```

**Cause:**
- Service account doesn't have Write permission on the topic
- Using wrong service account credentials
- Topic name pattern doesn't match (e.g., trying to write to `bss.analytics.events` but only have access to `bss.*`)

**Solution:**
```bash
# Check current user's ACLs
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list \
    --principal User:backend-service

# Add missing Write permission
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --add \
    --principal User:backend-service \
    --operation Write \
    --resource-type Topic \
    --resource-name bss.new.topic \
    --allow
```

### Issue 2: AuthorizationException on Read

**Symptom:**
```
org.apache.kafka.common.errors.AuthorizationException:
    Not authorized to access resources: Group授权 failed
```

**Cause:**
- Service account doesn't have Read permission on the topic
- Service account doesn't have Describe permission on the group
- Group ID is wrong

**Solution:**
```bash
# Verify group access
docker exec bss-kafka-1 kafka-consumer-groups.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --describe \
    --group bss-backend-group

# Add missing Read permission
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --add \
    --principal User:backend-service \
    --operation Read \
    --resource-type Topic \
    --resource-name bss.customer.events \
    --allow

# Add group access
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --add \
    --principal User:backend-service \
    --operation Read \
    --resource-type Group \
    --resource-name bss-backend-group \
    --allow
```

### Issue 3: Topic Not Found

**Symptom:**
```
org.apache.kafka.common.errors.TopicAuthorizationException:
    Not authorized to access topic: bss.customer.events
```

**Cause:**
- Topic doesn't exist
- Service account doesn't have Describe permission on the topic
- Auto-create topics is disabled and service doesn't have Create permission

**Solution:**
```bash
# Create topic (requires Create permission)
docker exec bss-kafka-1 kafka-topics.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --create \
    --topic bss.customer.events \
    --partitions 10 \
    --replication-factor 3

# Add Describe permission
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --add \
    --principal User:backend-service \
    --operation Describe \
    --resource-type Topic \
    --resource-name bss.customer.events \
    --allow
```

### Issue 4: Group Coordinator Error

**Symptom:**
```
org.apache.kafka.common.errors.GroupAuthorizationException:
    Not authorized to access group: bss-backend-group
```

**Cause:**
- Service account doesn't have access to the consumer group
- Using wrong group ID

**Solution:**
```bash
# Check which groups service can access
docker exec bss-kafka-1 kafka-consumer-groups.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list

# Add group access
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --add \
    --principal User:backend-service \
    --operation Read \
    --resource-type Group \
    --resource-name bss-backend-group \
    --allow

# Add group describe access
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --add \
    --principal User:backend-service \
    --operation Describe \
    --resource-type Group \
    --resource-name bss-backend-group \
    --allow
```

## Best Practices

### 1. Principle of Least Privilege

**Do:**
- Grant only the specific permissions each service needs
- Use pattern-based ACLs (`bss.*`) for related topics
- Separate read and write permissions
- Create dedicated service accounts for each application

**Don't:**
- Grant `All` permissions to production services
- Use wildcard principals (`User:*`)
- Share service accounts between applications
- Grant cluster-wide permissions to application services

### 2. Service Account Management

**Naming Convention:**
```
User:<service-name>-service
```

**Examples:**
- `User:backend-service` - Main application
- `User:customer-analytics-service` - Analytics service
- `User:payment-processor-service` - Payment processing

### 3. Topic Naming Patterns

Use hierarchical naming to match ACL patterns:

```
bss.customer.events      - Customer domain events
bss.order.events         - Order domain events
bss.payment.events       - Payment domain events
bss.invoice.events       - Invoice domain events
bss.analytics.events     - Analytics output topics
bss.customer.events.DLQ  - Dead letter queue
bss.*.DLQ               - All DLQ topics (pattern)
```

### 4. Consumer Group Isolation

Each service should have its own consumer group:

```
bss-backend-group              - Backend service
bss-customer-analytics-group   - Customer analytics
bss-order-processor-group      - Order processor
bss-frontend-group             - Frontend
```

### 5. Regular Audits

```bash
# Weekly: List all ACLs
kafka-acls.sh --list > /tmp/kafka-acls-audit-$(date +%Y%W).txt

# Monthly: Review and clean up unused ACLs
# Check for:
# - Services no longer in use
# - Unnecessary wildcard permissions
# - Unused consumer groups
# - Unauthorized access attempts
```

### 6. Certificate Management

**Do:**
- Store certificates securely
- Rotate certificates regularly
- Use separate certificates for each service
- Monitor certificate expiration

**Don't:**
- Commit certificates to version control
- Use self-signed certificates in production
- Share certificates between services
- Store certificates in code

## Migration Checklist

- [ ] Run `kafka-acls-setup.sh` script
- [ ] Verify ACLs are applied correctly
- [ ] Update client configurations to use service accounts
- [ ] Distribute SSL certificates to services
- [ ] Test ACL enforcement with unauthorized requests
- [ ] Update application code with correct client IDs
- [ ] Add monitoring for authorization failures
- [ ] Document service account credentials
- [ ] Train developers on ACL usage
- [ ] Schedule regular ACL audits

## References

- [Kafka ACL Documentation](https://kafka.apache.org/documentation/#security_authz)
- [Kafka Security Guide](https://kafka.apache.org/documentation/#security)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/html/)
- [Kafka Producer Configuration](https://kafka.apache.org/documentation/#producerconfigs)
- [Kafka Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)

## Support

For issues with Kafka ACLs:
1. Check logs: `docker logs bss-kafka-1 | grep -i "authorization"`
2. Verify ACLs: `kafka-acls.sh --list --principal <principal>`
3. Test connection: `kafka-broker-api-versions --bootstrap-server <server>`
4. Contact: kafka-admin@company.com
