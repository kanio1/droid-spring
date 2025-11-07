# Kafka Consumer Lag Monitoring

## Overview

Consumer lag represents the number of messages that have been produced to a Kafka topic but not yet consumed by a consumer group. Monitoring consumer lag is critical for ensuring:
- **Data processing is keeping up with production**
- **SLA compliance for event processing**
- **Early detection of consumer failures**
- **Optimization of consumer performance**

## What is Consumer Lag?

### Consumer Lag Calculation

```
Consumer Lag = (Log End Offset) - (Current Consumer Offset)
```

```
┌─────────────────────────────────────────────────────────┐
│  Topic Partition                                        │
│  ┌────────┬────────┬────────┬────────┬────────┬────┐   │
│  │Offset 0│Offset 1│Offset 2│Offset 3│Offset 4│... │   │
│  └────────┴────────┴────────┴────────┴────────┴────┘   │
│                              ▲            ▲             │
│                              │            │             │
│                    Current Offset    Log End Offset   │
│                              │            │             │
│                         Consumer has   Latest message  │
│                         read up to     in partition    │
│                         here                       │
│                                                         │
│  Consumer Lag = 4 - 2 = 2 messages                     │
└─────────────────────────────────────────────────────────┘
```

### Example Scenario

**Topic: `bss.customer.events`**

| Partition | Current Offset | Log End Offset | Lag | Status |
|-----------|---------------|----------------|-----|--------|
| 0 | 1000 | 1500 | 500 | ✅ OK |
| 1 | 2000 | 2200 | 200 | ✅ OK |
| 2 | 3000 | 3800 | 800 | ✅ OK |
| **Total** | - | - | **1500** | ✅ OK |

**Interpretation:**
- Consumer has processed 6000 messages total
- 1500 messages are pending (not yet consumed)
- If production rate is 100 msg/sec, lag will clear in 15 seconds

## Why Monitor Consumer Lag?

### 1. System Health Indicators

| Lag Status | Indications | Action Required |
|------------|-------------|-----------------|
| **0-1000** | System healthy, consumers keeping up | None |
| **1000-10000** | Minor delay, watch trend | Check for issues |
| **10000-100000** | Significant delay | Scale consumers, investigate |
| **>100000** | Critical delay, potential data loss risk | Immediate action required |

### 2. Business Impact

**Low Lag (< 1000):**
- Real-time data processing
- Customer experience: Instant
- SLA: ✅ Met

**High Lag (10000-100000):**
- Delayed data processing
- Customer experience: Delayed (minutes)
- SLA: ⚠️ At risk

**Critical Lag (> 100000):**
- Batch-like processing (not real-time)
- Customer experience: Severely delayed (hours)
- SLA: ❌ Breached

### 3. Performance Optimization

Consumer lag helps identify:
- **Insufficient consumer capacity**: Need more consumers
- **Slow consumer processing**: Optimize business logic
- **Resource constraints**: Scale up or tune JVM/memory
- **Network issues**: Check connectivity
- **Consumer failures**: One or more consumers are down

## Monitoring Architecture

### Components

```
┌──────────────────────────────────────────────────────────┐
│               Kafka Cluster                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │Broker 1  │ │Broker 2  │ │Broker 3  │                  │
│  └─────┬────┘ └─────┬────┘ └─────┬────┘                  │
│        │            │            │                       │
│        └────────────┴────────────┘                       │
│                      │                                   │
└──────────────────────┼───────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│            Consumer Groups                                │
│  ┌──────────────────┐  ┌──────────────────┐             │
│  │Group: backend    │  │Group: analytics  │             │
│  │Members: 3        │  │Members: 2        │             │
│  │Lag: 500          │  │Lag: 1200         │             │
│  └──────────────────┘  └──────────────────┘             │
└──────────────────────────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│         Prometheus Metrics (jmx-kafka-consumer)          │
│  - kafka_consumer_lag_sum                                 │
│  - kafka_consumer_offset_sum                              │
│  - kafka_consumer_group_members                           │
│  - kafka_consumer_messages_total                          │
└──────────────────────────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                Prometheus Alerting Rules                  │
│  - KafkaConsumerLagHigh (warn: > 10,000)                 │
│  - KafkaConsumerLagCritical (crit: > 100,000)            │
│  - KafkaConsumerNotConsuming (warn: 0 lag + 0 offset)    │
│  - KafkaConsumerGroupOffline (crit: 0 members)           │
└──────────────────────────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────┐
│                   Grafana Dashboard                       │
│  - Real-time lag visualization                            │
│  - Historical trend analysis                              │
│  - Per-topic and per-group breakdown                      │
│  - SLA tracking                                           │
└──────────────────────────────────────────────────────────┘
```

### Metrics Collection

**Prometheus JMX Exporter** collects:
- Current consumer offset
- Log end offset (partition high watermark)
- Consumer group membership
- Message consumption rate
- Commit/fetch errors

**jmx-kafka-consumer configuration:**

```yaml
# prometheus-jmx-config.yml
lowercaseOutputName: true
rules:
  - pattern: kafka.consumer<type=(.+), client-id=(.+)><>([a-z-]+)
    name: kafka_consumer_$3
    labels:
      client_id: "$2"
```

## Implementation

### 1. Prometheus Alert Rules

Alert thresholds are defined in:
```
/home/labadmin/projects/droid-spring/dev/prometheus/rules/kafka-consumer-lag.yml
```

**Key Alerts:**

#### Alert: KafkaConsumerLagHigh
```yaml
- alert: KafkaConsumerLagHigh
  expr: kafka_consumer_lag_sum > 10000
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High consumer lag detected"
    description: "Consumer lag is {{ $value }} messages"
```

#### Alert: KafkaConsumerLagCritical
```yaml
- alert: KafkaConsumerLagCritical
  expr: kafka_consumer_lag_sum > 100000
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Critical consumer lag detected"
    description: "Consumer lag is {{ $value }} messages"
    action: "URGENT: Investigate consumer health"
```

#### Alert: KafkaConsumerNotConsuming
```yaml
- alert: KafkaConsumerNotConsuming
  expr: kafka_consumer_lag_sum == 0 and kafka_consumer_offset_sum == 0
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Consumer not consuming messages"
```

### 2. Monitoring Script

#### Usage

```bash
# Run manual check
./dev/kafka/monitor-consumer-lag.sh

# Output files:
# - /tmp/kafka-consumer-lag.log (execution log)
# - /tmp/kafka-consumer-lag-report.html (visual report)
# - /tmp/kafka-consumer-lag.json (machine-readable)
```

#### Script Features

1. **Fetches all consumer groups**
2. **Calculates lag per partition**
3. **Generates HTML report**
4. **Generates JSON report**
5. **Sends alerts based on thresholds**
6. **Color-coded output**

#### Automated Monitoring

```bash
# Add to crontab (check every 5 minutes)
*/5 * * * * /home/labadmin/projects/droid-spring/dev/kafka/monitor-consumer-lag.sh > /dev/null 2>&1

# Or use systemd timer
# /etc/systemd/system/kafka-consumer-lag.service
# /etc/systemd/system/kafka-consumer-lag.timer
```

### 3. Grafana Dashboard

#### Dashboard URL
`https://grafana.bss.local/d/kafka/consumer-lag`

#### Key Panels

1. **Total Consumer Lag (All Topics)**
   - Current lag: `sum(kafka_consumer_lag_sum)`
   - Status: Green (< 10K), Yellow (10K-100K), Red (> 100K)

2. **Lag by Consumer Group**
   - Bar chart showing lag per group
   - Identifies which groups need attention

3. **Lag by Topic**
   - Bar chart showing lag per topic
   - Identifies which topics have delays

4. **Consumer Lag Trend**
   - Time series of total lag
   - Shows if lag is increasing or decreasing

5. **Consumer Throughput**
   - Messages consumed per second
   - `rate(kafka_consumer_messages_total[5m])`

6. **Active Consumer Groups**
   - Number of active groups
   - `count(kafka_consumer_group_members)`

7. **Consumer Group Health**
   - Composite score (0-100)
   - Based on lag, rebalances, and errors

### 4. Sample Dashboard Queries

```promql
# Total consumer lag
sum(kafka_consumer_lag_sum)

# Lag by consumer group
sum by (consumergroup) (kafka_consumer_lag_sum)

# Lag by topic
sum by (topic) (kafka_consumer_lag_sum)

# Consumer lag rate of change
rate(kafka_consumer_lag_sum[5m])

# Consumer throughput
rate(kafka_consumer_messages_total[5m])

# Number of active consumer groups
count(kafka_consumer_group_members)

# 95th percentile lag
histogram_quantile(0.95, kafka_consumer_lag_bucket)

# Business-critical topic lag
sum(kafka_consumer_lag_sum) by (topic)
```

## How to Check Consumer Lag

### Method 1: Kafka Command Line

```bash
# List all consumer groups
docker exec bss-kafka-1 kafka-consumer-groups.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --list

# Describe a specific consumer group
docker exec bss-kafka-1 kafka-consumer-groups.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --describe \
    --group bss-backend-group

# Get lag for a specific topic
docker exec bss-kafka-1 kafka-run-class.sh \
    kafka.tools.GetOffsetShell \
    --broker-list kafka-1:9092 \
    --topic bss.customer.events \
    --time -1
```

### Method 2: Using the Monitoring Script

```bash
# Run the script
./dev/kafka/monitor-consumer-lag.sh

# View JSON output
cat /tmp/kafka-consumer-lag.json | jq .

# View HTML report
cat /tmp/kafka-consumer-lag-report.html
```

### Method 3: Grafana Dashboard

1. Open `https://grafana.bss.local/d/kafka/consumer-lag`
2. Select time range (e.g., last 1 hour)
3. View total lag and per-group/topics breakdown
4. Click on a group/topic for details

### Method 4: Prometheus Query

```bash
# Query via API
curl "http://prometheus.bss.local/api/v1/query?query=sum(kafka_consumer_lag_sum)"

# Query with labels
curl "http://prometheus.bss.local/api/v1/query?query=sum(kafka_consumer_lag_sum) by (consumergroup)"
```

## Understanding the Output

### Consumer Group Description Output

```
GROUP                          TOPIC                           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                         HOST                            CLIENT-ID
bss-backend-group              bss.customer.events              0          5000            5500            500             bss-backend-1-1234abcd-efgh-5678-ijkl-9012mnopqrst  /10.0.1.5                      bss-backend-1
bss-backend-group              bss.customer.events              1          6000            6500            500             bss-backend-2-5678efgh-ijkl-9012-mnop-3456qrstuvwx  /10.0.1.6                      bss-backend-2
bss-backend-group              bss.customer.events              2          5500            5800            300             bss-backend-3-9012mnop-qrst-3456-uvwx-7890yzabcd    /10.0.1.7                      bss-backend-3
```

**Columns:**
- `GROUP`: Consumer group name
- `TOPIC`: Kafka topic name
- `PARTITION`: Partition number (0-based)
- `CURRENT-OFFSET`: Last offset committed by consumer
- `LOG-END-OFFSET`: Latest offset in partition
- `LAG`: Number of messages not yet consumed
- `CONSUMER-ID`: Consumer instance ID
- `HOST`: Consumer host
- `CLIENT-ID`: Client ID

## Troubleshooting Consumer Lag

### Issue 1: High Lag on All Topics

**Symptoms:**
- All consumer groups showing high lag
- Lag increasing over time
- Production rate exceeds consumption rate

**Causes:**
- Insufficient number of consumers
- Consumers are slow (inefficient code)
- Network issues
- Resource constraints (CPU, memory)

**Solutions:**
```java
// Increase consumer parallelism
@KafkaListener(
    topics = "bss.customer.events",
    groupId = "bss-backend-group",
    concurrency = "10"  // Increase from 3 to 10
)

// Or increase number of consumer instances
// Deploy more backend pods/instances
```

### Issue 2: High Lag on Specific Topic

**Symptoms:**
- One topic has high lag, others are healthy
- Topic-specific processing is slow

**Causes:**
- Topic has higher production rate
- Topic-specific code is inefficient
- Topic has more partitions than consumers

**Solutions:**
```java
// Increase partitions for the topic
docker exec bss-kafka-1 kafka-topics.sh \
    --bootstrap-server kafka-1:9092 \
    --command-config /etc/ssl/certs/kafka-client.properties \
    --alter \
    --topic bss.customer.events \
    --partitions 20

// Ensure consumers can handle the load
@KafkaListener(
    topics = "bss.customer.events",
    groupId = "bss-backend-group",
    concurrency = "20"  // Match partition count
)
```

### Issue 3: Consumer Not Consuming (Lag = 0, Offset = 0)

**Symptoms:**
- Consumer group shows 0 lag AND 0 offset
- No messages being consumed
- Consumer appears in group but idle

**Causes:**
- Consumer polling frequency too low
- Consumer poll() returns immediately
- Auto-commit disabled and manual commit failing
- Consumer crashed after processing messages

**Solutions:**
```java
// Check poll interval
props.put(ConsumerConfig.POLL_TIMEOUT_MS_CONFIG, 3000);

// Enable auto-commit (or implement manual commit properly)
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

// Or implement manual commit correctly
try {
    consumer.poll(Duration.ofMillis(100));
    // Process messages
    consumer.commitSync();  // Commit after successful processing
} catch (Exception e) {
    // Handle errors
}
```

### Issue 4: Consumer Group Offline

**Symptoms:**
- No active members in group
- Lag increasing rapidly
- No consumption happening

**Causes:**
- Consumer service crashed
- All consumer instances stopped
- Network partition between consumers and brokers
- Authentication/authorization failure

**Solutions:**
```bash
# Check if consumer service is running
docker ps | grep backend

# Check consumer logs
docker logs bss-backend

# Restart consumer service
docker compose -f dev/compose.yml restart backend
```

### Issue 5: Frequent Rebalances

**Symptoms:**
- Consumers joining/leaving group frequently
- Lag spiking during rebalances
- Processing interrupted

**Causes:**
- Consumers taking too long to process messages
- Session timeout too low
- Heartbeat interval too high
- Consumer crash during processing

**Solutions:**
```java
// Increase session timeout
props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

// Increase max poll interval
props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

// Optimize processing time
// Use smaller batches
// Offload to async processing
```

## Best Practices

### 1. Lag Thresholds

| Lag Range | Action | Auto-scaling |
|-----------|--------|--------------|
| 0-1,000 | Normal, no action | - |
| 1,000-10,000 | Monitor, consider optimization | - |
| 10,000-100,000 | Scale consumers up | Yes (if > 30,000) |
| > 100,000 | Critical, immediate action | Yes (mandatory) |

### 2. Consumer Design

**Do:**
- Process messages asynchronously
- Use batch processing for efficiency
- Commit offsets after successful processing
- Monitor processing time
- Use appropriate concurrency
- Implement error handling and DLQ

**Don't:**
- Process messages synchronously in poll loop
- Block the consumer thread
- Ignore errors
- Commit after every single message (too slow)
- Use too few partitions
- Let consumers crash

### 3. Code Example: Lag-Aware Consumer

```java
@Component
@Slf4j
public class LagAwareConsumer {

    private static final long LAG_THRESHOLD_WARN = 10000;
    private static final long LAG_THRESHOLD_CRIT = 100000;

    @KafkaListener(
        topics = "bss.customer.events",
        groupId = "bss-backend-group",
        concurrency = "10"
    )
    public void listen(ConsumerRecord<String, CloudEvent> record) {
        long startTime = System.currentTimeMillis();

        try {
            // Process message
            processMessage(record);

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;

            // Log if processing is slow
            if (processingTime > 1000) {
                log.warn("Slow processing detected: {}ms for message {}", processingTime, record.key());
            }

            // Commit offset
            // (auto-commit or manual commitSync)
        } catch (Exception e) {
            log.error("Failed to process message: {}", record.key(), e);
            // Send to DLQ
            sendToDeadLetterQueue(record);
        }
    }

    // Implement lag monitoring
    @EventListener
    public void handleAlert(AlertEvent event) {
        if ("KafkaConsumerLagHigh".equals(event.getAlertName())) {
            log.warn("High lag detected: {}", event.getValue());
            // Could trigger auto-scaling here
            // scaleUpConsumers();
        }
    }
}
```

### 4. Auto-Scaling Based on Lag

```yaml
# kubernetes-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: External
    external:
      metric:
        name: kafka_consumer_lag_sum
        selector:
          matchLabels:
            consumergroup: bss-backend-group
      target:
        type: AverageValue
        averageValue: "5000"  # Scale up if average lag > 5000
```

### 5. Alert Routing

```yaml
# alertmanager.yml
route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default'
  routes:
  - match:
      component: kafka
    receiver: 'kafka-team'

receivers:
- name: 'kafka-team'
  email_configs:
  - to: 'kafka-team@company.com'
    subject: '[CRITICAL] Kafka Consumer Lag Alert'
    body: |
      {{ range .Alerts }}
      Alert: {{ .Annotations.summary }}
      Description: {{ .Annotations.description }}
      Action: {{ .Annotations.action }}
      Dashboard: {{ .Annotations.dashboard_url }}
      {{ end }}
```

## Performance Optimization

### 1. Batch Processing

```java
@KafkaListener(
    topics = "bss.customer.events",
    groupId = "bss-backend-group",
    batch = "true"  // Enable batch processing
)
public void listen(List<ConsumerRecord<String, CloudEvent>> records) {
    log.info("Processing batch of {} messages", records.size());

    for (ConsumerRecord<String, CloudEvent> record : records) {
        // Process each record
        processMessage(record);
    }
}
```

### 2. Parallel Processing

```java
@KafkaListener(
    topics = "bss.customer.events",
    groupId = "bss-backend-group",
    concurrency = "10",
    containerFactory = "kafkaListenerContainerFactory"
)
public void listen(ConsumerRecord<String, CloudEvent> record) {
    // Each partition gets its own thread
    // High throughput
    processMessageAsync(record);
}
```

### 3. Async Processing with CompletableFuture

```java
@KafkaListener(
    topics = "bss.customer.events",
    groupId = "bss-backend-group"
)
public void listen(ConsumerRecord<String, CloudEvent> record) {
    CompletableFuture.supplyAsync(() -> {
        // Process in background thread
        return processMessage(record);
    }).thenAccept(result -> {
        // Handle result
    }).exceptionally(throwable -> {
        // Handle errors
        return null;
    });
}
```

### 4. Optimize Consumer Config

```java
Properties props = new Properties();
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-1:9092");
props.put(ConsumerConfig.GROUP_ID_CONFIG, "bss-backend-group");

// Increase fetch size for better throughput
props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800);  // 50MB
props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);  // 500 records per poll

// Reduce timeouts for faster failure detection
props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

// Enable auto-commit
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
```

## SLA Targets

| Metric | Target | Threshold | Action if Breached |
|--------|--------|-----------|-------------------|
| **Consumer Lag** | < 1,000 | > 10,000 | Scale consumers |
| **Processing Time** | < 1 sec | > 5 sec | Optimize code |
| **Consumer Availability** | 99.9% | < 99% | Investigate crashes |
| **Rebalances per hour** | < 1 | > 5 | Tune timeouts |

## Reference: All Kafka Consumer Lag Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `kafka_consumer_lag_sum` | Gauge | Current lag per partition |
| `kafka_consumer_offset_sum` | Gauge | Current offset per partition |
| `kafka_consumer_group_members` | Gauge | Number of active consumers in group |
| `kafka_consumer_messages_total` | Counter | Total messages consumed |
| `kafka_consumer_fetch_rate` | Rate | Messages fetched per second |
| `kafka_consumer_rebalance_total` | Counter | Number of rebalances |
| `kafka_consumer_offset_commit_failures_total` | Counter | Failed offset commits |
| `kafka_consumer_fetch_errors_total` | Counter | Fetch request failures |
| `kafka_consumer_lag_bucket` | Histogram | Lag distribution |

## Integration with CI/CD

### Automated Lag Testing

```yaml
# .github/workflows/kafka-consumer-test.yml
name: Kafka Consumer Lag Test

on: [push, pull_request]

jobs:
  test-consumer-lag:
    runs-on: ubuntu-latest
    steps:
      - name: Run consumer lag test
        run: |
          # Start Kafka and consumer
          docker compose -f dev/compose.yml up -d kafka-cluster

          # Run test workload
          ./dev/kafka/load-test.sh

          # Check lag
          ./dev/kafka/monitor-consumer-lag.sh

          # Verify lag is below threshold
          LAG=$(cat /tmp/kafka-consumer-lag.json | jq '.consumer_groups[0].topics[0].lag')
          if [ "$LAG" -gt 10000 ]; then
            echo "Consumer lag too high: $LAG"
            exit 1
          fi
```

## References

- [Kafka Consumer Documentation](https://kafka.apache.org/documentation/#consumerconfigs)
- [Monitoring Kafka](https://kafka.apache.org/documentation/#monitoring)
- [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter)
- [Grafana Kafka Dashboard](https://grafana.com/grafana/dashboards/721)

## Support

For consumer lag issues:
1. Check Grafana dashboard: https://grafana.bss.local/d/kafka/consumer-lag
2. Review alerts: https://prometheus.bss.local/alerts
3. Run monitoring script: `./dev/kafka/monitor-consumer-lag.sh`
4. Contact: kafka-team@company.com
