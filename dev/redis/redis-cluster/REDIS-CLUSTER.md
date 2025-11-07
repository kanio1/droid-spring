# Redis Clustering Implementation

## Overview

Redis Cluster provides a way to automatically shard data across multiple Redis nodes, enabling horizontal scaling and high availability. It uses hash slot partitioning to distribute keys across 16384 slots, automatically redistributing slots when nodes are added or removed.

## Architecture

### Cluster Topology

```
┌─────────────────────────────────────────┐
│         Redis Cluster (6 nodes)          │
│                                            │
│  Master 1      Master 2      Master 3     │
│  Port 7000     Port 7001     Port 7002    │
│  Slots:        Slots:        Slots:        │
│  0-5460       5461-10922    10923-16383  │
│     │            │            │           │
│     ▼            ▼            ▼           │
│  Replica 1     Replica 2     Replica 3    │
│  Port 7003     Port 7004     Port 7005    │
│                                            │
└─────────────────────────────────────────┘
       │            │            │
       └────────────┴────────────┘
                    │
                    ▼
            Client Applications
```

### Key Features

1. **Automatic Sharding**: Data is automatically distributed across 16384 hash slots
2. **High Availability**: Each master has 1-2 replicas for automatic failover
3. **Read Scalability**: Replicas can serve read operations
4. **Write Scalability**: Writes are distributed across masters
5. **TLS Encryption**: All cluster communication is encrypted
6. **Client Discovery**: Clients automatically discover cluster topology

### Node Configuration

| Node | Port | TLS Port | Role | Memory |
|------|------|----------|------|--------|
| Node 1 | 7000 | 17000 | Master | 1GB |
| Node 2 | 7001 | 17001 | Master | 1GB |
| Node 3 | 7002 | 17002 | Master | 1GB |
| Node 4 | 7003 | 17003 | Replica | 1GB |
| Node 5 | 7004 | 17004 | Replica | 1GB |
| Node 6 | 7005 | 17005 | Replica | 1GB |

## Setup Instructions

### 1. Start Redis Cluster Services

```bash
# Start all Redis cluster nodes
docker compose -f dev/compose.yml up -d redis-cluster

# Verify all nodes are running
docker ps | grep redis-cluster
```

Expected output:
```
bss-redis-cluster-7000   ...   Up      0.0.0.0:7000->7000/tcp
bss-redis-cluster-7001   ...   Up      0.0.0.0:7001->7001/tcp
bss-redis-cluster-7002   ...   Up      0.0.0.0:7002->7002/tcp
bss-redis-cluster-7003   ...   Up      0.0.0.0:7003->7003/tcp
bss-redis-cluster-7004   ...   Up      0.0.0.0:7004->7004/tcp
bss-redis-cluster-7005   ...   Up      0.0.0.0:7005->7005/tcp
```

### 2. Initialize the Cluster

```bash
# Run the cluster setup script
./dev/redis/redis-cluster/setup-cluster.sh

# The script will:
# 1. Check if all services are running
# 2. Wait for Redis instances to be ready
# 3. Create a 6-node cluster (3 masters + 3 replicas)
# 4. Test cluster operations
# 5. Display connection information
```

### 3. Verify Cluster Status

```bash
# Check cluster info
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster info

# Expected output:
# cluster_state:ok
# cluster_slots_assigned:16384
# cluster_size:3
# cluster_current_epoch:6
# cluster_my_epoch:1
# cluster_stats_messages_sent:105
# cluster_stats_messages_received:105

# List all cluster nodes
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster nodes
```

### 4. Test Cluster Operations

```bash
# Connect to cluster
docker exec -it bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123

# Test SET/GET
127.0.0.1:7000> set user:1000 "John Doe"
OK

127.0.0.1:7000> get user:1000
"John Doe"

# Test keys in different slots
127.0.0.1:7000> set key:1 "value1"
OK

127.0.0.1:7000> set key:2 "value2"
(error) MOVED 9548 127.0.0.1:7001

# Note: "MOVED" is normal - client should redirect to node 7001
```

## Application Integration

### Spring Boot Configuration

#### application.yml

```yaml
spring:
  redis:
    cluster:
      # List of cluster nodes
      nodes:
        - localhost:7000
        - localhost:7001
        - localhost:7002
        - localhost:7003
        - localhost:7004
        - localhost:7005
      # Maximum redirect attempts
      max-redirects: 3
      # Connection timeout
      timeout: 2000ms
      # Password
      password: ${REDIS_CLUSTER_PASSWORD:redis_cluster_password_123}
      # SSL/TLS
      ssl:
        enabled: true
        trust-store: classpath:ssl/redis-cluster-truststore.jks
        trust-store-password: ${REDIS_TRUSTSTORE_PASSWORD}
        key-store: classpath:ssl/redis-cluster-keystore.p12
        key-store-password: ${REDIS_KEYSTORE_PASSWORD}
        key-store-type: PKCS12
        enabled-protocols: TLSv1.2,TLSv1.3
```

#### Redis Configuration Class

```java
@Configuration
@EnableRedisRepositories
public class RedisClusterConfig {

    @Value("${REDIS_CLUSTER_PASSWORD:redis_cluster_password_123}")
    private String password;

    @Value("${REDIS_TRUSTSTORE_PASSWORD}")
    private String truststorePassword;

    @Value("${REDIS_KEYSTORE_PASSWORD}")
    private String keystorePassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration()
            .clusterNode("localhost", 7000)
            .clusterNode("localhost", 7001)
            .clusterNode("localhost", 7002)
            .clusterNode("localhost", 7003)
            .clusterNode("localhost", 7004)
            .clusterNode("localhost", 7005);

        clusterConfig.setPassword(password);
        clusterConfig.setMaxRedirects(3);

        // SSL Configuration
        SslOptions sslOptions = SslOptions.builder()
            .trustStore(ResourceUtils.getFile("classpath:ssl/redis-cluster-truststore.jks"))
            .trustStorePassword(truststorePassword.toCharArray())
            .keyStore(ResourceUtils.getFile("classpath:ssl/redis-cluster-keystore.p12"))
            .keyStorePassword(keystorePassword.toCharArray())
            .keyStoreType("PKCS12")
            .build();

        clusterConfig.setSslOptions(sslOptions);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder()
                    .responseTimeout(Duration.ofSeconds(2))
                    .build())
                .build())
            .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        // Set serializers
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
```

### Java Client Example

#### CustomerCacheService

```java
@Service
@Slf4j
public class CustomerCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CUSTOMER_CACHE_KEY = "customer:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    public void cacheCustomer(Customer customer) {
        String key = CUSTOMER_CACHE_KEY + customer.getId();
        try {
            redisTemplate.opsForValue().set(key, customer, CACHE_TTL);
            log.debug("Cached customer: {}", customer.getId());
        } catch (Exception e) {
            log.error("Failed to cache customer: {}", customer.getId(), e);
        }
    }

    public Optional<Customer> getCustomer(UUID customerId) {
        String key = CUSTOMER_CACHE_KEY + customerId;
        try {
            Customer customer = (Customer) redisTemplate.opsForValue().get(key);
            if (customer != null) {
                log.debug("Cache hit for customer: {}", customerId);
                return Optional.of(customer);
            } else {
                log.debug("Cache miss for customer: {}", customerId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Failed to get customer from cache: {}", customerId, e);
            return Optional.empty();
        }
    }

    public void invalidateCustomer(UUID customerId) {
        String key = CUSTOMER_CACHE_KEY + customerId;
        try {
            redisTemplate.delete(key);
            log.debug("Invalidated customer cache: {}", customerId);
        } catch (Exception e) {
            log.error("Failed to invalidate customer cache: {}", customerId, e);
        }
    }

    public void invalidateAllCustomers() {
        try {
            Set<String> keys = redisTemplate.keys(CUSTOMER_CACHE_KEY + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} customer cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to invalidate all customer caches", e);
        }
    }
}
```

### Spring Data Redis Repository

```java
@Repository
public interface CustomerCacheRepository {

    @Cacheable(value = "customers", key = "#id")
    default Optional<Customer> findById(UUID id) {
        // This would be implemented by the actual repository
        return Optional.empty();
    }

    @CachePut(value = "customers", key = "#customer.id")
    default Customer save(Customer customer) {
        return customer;
    }

    @CacheEvict(value = "customers", key = "#id")
    default void deleteById(UUID id) {
        // This would be implemented by the actual repository
    }
}
```

## Cluster Management

### 1. Add New Node

```bash
# Start a new Redis instance (Node 7)
# (This would be done via Docker Compose)

# Add the new node as a master
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster add-node 127.0.0.1:7006 127.0.0.1:7000

# Add the new node as a replica
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster add-node 127.0.0.1:7007 127.0.0.1:7000 \
  --cluster-slave \
  --cluster-master-id <node_id>
```

### 2. Remove Node

```bash
# Remove a replica node
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster del-node 127.0.0.1:7003 <node_id>

# Remove a master node (requires resharding first)
# 1. Migrate all slots from master to other nodes
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster reshard 127.0.0.1:7000 \
  --cluster-from <master_node_id> \
  --cluster-to <other_master_node_id> \
  --cluster-slots <number_of_slots>

# 2. Delete the now-empty master
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster del-node 127.0.0.1:7000 <master_node_id>
```

### 3. Reshard Slots

```bash
# Reshard slots between nodes
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster reshard 127.0.0.1:7000

# Interactive mode will prompt for:
# - How many slots to move
# - From which node
# - To which node
```

### 4. Rebalance Slots

```bash
# Rebalance all slots evenly
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster rebalance 127.0.0.1:7000 \
  --cluster-threshold 2 \
  --cluster-use-empty-masters

# Rebalance with weight
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster rebalance 127.0.0.1:7000 \
  --cluster-weight node1=5 \
  --cluster-weight node2=3 \
  --cluster-weight node3=1
```

### 5. Failover

```bash
# Manually trigger failover (on replica)
docker exec bss-redis-cluster-7003 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster failover

# After failover, the replica becomes master
# The old master (if reachable) becomes a replica
```

## Monitoring

### 1. Cluster Health

```bash
# Check cluster health
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster info

# Check all nodes
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster check 127.0.0.1:7000

# Monitor in real-time
watch -n 1 'docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster info'
```

### 2. Key Distribution

```bash
# Check slot distribution
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster slots

# Count keys per slot
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster countkeysinslot 0

# Get keys in a slot (limit to 10)
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster getkeysinslot 0 10
```

### 3. Latency Monitoring

```bash
# Check cluster nodes latency
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster ping

# Test latency from each node
for port in 7000 7001 7002 7003 7004 7005; do
  echo "Testing latency to port $port:"
  docker exec bss-redis-cluster-$port redis-cli --tls \
    --cacert /etc/ssl/certs/ca-cert.pem \
    --pass redis_cluster_password_123 \
    --latency
done
```

### 4. Memory Usage

```bash
# Check memory usage per node
for port in 7000 7001 7002 7003 7004 7005; do
  echo "Memory usage for port $port:"
  docker exec bss-redis-cluster-$port redis-cli --tls \
    --cacert /etc/ssl/certs/ca-cert.pem \
    --pass redis_cluster_password_123 \
    INFO memory | grep used_memory_human
done
```

## Troubleshooting

### Issue 1: Node Failure

**Symptom:**
```
(error) CLUSTERDOWN The cluster is down
```

**Cause:**
- Primary node has failed
- Network partition between nodes
- Quorum lost

**Solution:**
```bash
# Check which nodes are down
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster nodes | grep fail

# If replicas are available, cluster will auto-failover
# If no replicas available, need to:

# 1. Remove failed node
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster forget <failed_node_id>

# 2. Add replacement node
# 3. Replica will sync automatically
```

### Issue 2: Key Not Accessible

**Symptom:**
```
(error) MOVED 15495 127.0.0.1:7002
```

**Cause:**
- Key is in a slot on a different node
- Client is not following redirects

**Solution:**
```bash
# Check which node has the key's slot
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  CLUSTER KEYSLOT mykey

# Ensure client is using Redis Cluster
# Most clients automatically follow MOVED redirects
```

### Issue 3: Slow Performance

**Symptom:**
- High latency on cluster operations
- Slow responses to client

**Solution:**
```bash
# Check network latency
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --latency

# Check node load
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  INFO stats

# Check memory usage
for port in 7000 7001 7002; do
  docker exec bss-redis-cluster-$port redis-cli --tls \
    --cacert /etc/ssl/certs/ca-cert.pem \
    --pass redis_cluster_password_123 \
    INFO memory
done
```

### Issue 4: Cluster Split (Split Brain)

**Symptom:**
- Two clusters with same data
- Clients getting different results
- No automatic recovery

**Solution:**
```bash
# Identify the split
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  cluster nodes

# Manually merge the cluster
# 1. On the largest cluster
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  CLUSTER MEET <ip> <port>

# 2. Reshard to balance data
docker exec bss-redis-cluster-7000 redis-cli --tls \
  --cacert /etc/ssl/certs/ca-cert.pem \
  --pass redis_cluster_password_123 \
  --cluster rebalance 127.0.0.1:7000
```

## Best Practices

### 1. Key Design

**Do:**
- Use hash tags: `{user}:1000` (ensures all keys for user 1000 are on same node)
- Use consistent key naming: `customer:12345:profile`
- Keep key names short
- Set TTL on keys with expiration

**Don't:**
- Store large values (>1MB) in Redis
- Use keys without pattern
- Store binary data directly (use serialization)

**Example:**
```java
// Good: Hash tag ensures all user keys are on same node
redisTemplate.opsForValue().set("{user:1000}:profile", profile);
redisTemplate.opsByValue().set("{user:1000}:settings", settings);
redisTemplate.opsForValue().set("{user:1000}:session", session);

// Bad: No hash tag, keys distributed randomly
redisTemplate.opsForValue().set("user:1000:profile", profile);
redisTemplate.opsForValue().set("user:1000:settings", settings);
```

### 2. Connection Pooling

```java
@Bean
public RedisConnectionFactory redisConnectionFactory() {
    RedisClusterConfiguration config = new RedisClusterConfiguration()
        .clusterNode("localhost", 7000)
        .clusterNode("localhost", 7001)
        .clusterNode("localhost", 7002);

    ClientOptions options = ClientOptions.builder()
        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
        .autoReconnect(true)
        .build();

    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .clientOptions(options)
        .commandTimeout(Duration.ofSeconds(5))
        .build();

    return new LettuceConnectionFactory(config, clientConfig);
}
```

### 3. Error Handling

```java
public void handleRedisOperation(Runnable operation) {
    try {
        operation.run();
    } catch (RedisClusterCommandTimeoutException e) {
        log.error("Cluster command timeout", e);
        // Retry with backoff
    } catch (RedisConnectionFailureException e) {
        log.error("Cluster connection failure", e);
        // Try to reconnect
    } catch (MovedDataException e) {
        log.info("Key moved to new node: {}", e.getSlot());
        // Redirect client to new node
    } catch (AskDataException e) {
        log.info("Key moved, need to ask: {}", e.getSlot());
        // Ask the target node
    } catch (Exception e) {
        log.error("Unexpected Redis error", e);
    }
}
```

### 4. Performance Optimization

```java
// Use pipeline for batch operations
public void batchCacheCustomers(List<Customer> customers) {
    RedisConnection connection = redisTemplate.getConnectionFactory()
        .getConnection();

    try (SessionCallback<Void> session = new SessionCallback<Void>() {
        @Override
        public <K, V> Void execute(RedisOperations<K, V> operations) throws DataAccessException {
            for (Customer customer : customers) {
                String key = "customer:" + customer.getId();
                operations.opsForValue().set(key, customer, Duration.ofHours(1));
            }
            operations.getConnection().pipeline();
            return null;
        }
    });

    redisTemplate.execute(session);
}
```

### 5. Security

**Do:**
- Use TLS for all connections
- Require authentication
- Restrict network access
- Monitor failed auth attempts

**Don't:**
- Expose cluster nodes to public internet
- Use default passwords
- Disable TLS in production
- Run as root user

## Migration Checklist

- [ ] Start all Redis cluster nodes
- [ ] Run cluster setup script
- [ ] Verify cluster status
- [ ] Test cluster operations
- [ ] Update application configuration
- [ ] Test application with cluster
- [ ] Set up monitoring
- [ ] Configure alerts
- [ ] Document cluster topology
- [ ] Train operations team

## References

- [Redis Cluster Documentation](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Redis Security](https://redis.io/docs/latest/operate/oss_and_stack/management/security/)
- [Redis Performance](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/)

## Support

For Redis cluster issues:
1. Check logs: `docker logs bss-redis-cluster-7000`
2. Verify cluster: `redis-cli cluster info`
3. Check network: `docker network inspect bridge`
4. Contact: redis-admin@company.com
