# Kafka Dead Letter Queue (DLQ) Configuration

## Overview
The Dead Letter Queue (DLQ) is a critical component for handling failed Kafka messages. When a message cannot be processed successfully, it's automatically routed to a DLQ topic for later analysis and reprocessing.

## DLQ Topics

The following DLQ topics are configured:

1. **bss.events.DLQ** - Generic DLQ for all events
2. **bss.events.customer.DLQ** - Customer event failures
3. **bss.events.order.DLQ** - Order event failures
4. **bss.events.payment.DLQ** - Payment event failures
5. **bss.events.invoice.DLQ** - Invoice event failures

## Configuration

### Topic Settings
- Partitions: 10-20 (depending on volume)
- Replication Factor: 3 (for high availability)
- Min In-Sync Replicas: 2
- Retention: 7 days (604800000 ms)
- Cleanup Policy: Delete

### Consumer Configuration
```java
@KafkaListener(topics = "bss.events.customer")
public void handleCustomerEvent(ConsumerRecord<String, String> record) {
    try {
        // Process event
        processCustomerEvent(record.value());
    } catch (Exception e) {
        log.error("Failed to process customer event", e);
        throw e; // Will trigger DLQ via error handler
    }
}
```

### DLQ Message Format
```json
{
  "originalTopic": "bss.events.customer",
  "originalKey": "customer-123",
  "originalValue": "{...}",
  "originalOffset": 12345,
  "originalPartition": 0,
  "failedAt": 1701234567890,
  "bootstrapServers": "kafka-1:9092,kafka-2:9092,kafka-3:9092"
}
```

## Creating DLQ Topics

Run the script to create DLQ topics:
```bash
./dev/kafka/create-dlq-topic.sh
```

## Monitoring DLQ

### Prometheus Metrics
```yaml
- alert: KafkaDLQMessages
  expr: increase(kafka_consumer_dlq_count[5m]) > 10
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High number of messages in DLQ"
    description: "{{ $value }} messages sent to DLQ in the last 5 minutes"
```

### Kafka Lag Monitoring
```bash
# Check DLQ lag
kafka-consumer-groups --describe --group dlq-consumer-group
```

## Reprocessing DLQ Messages

1. **Manual Replay**
   ```bash
   # Read from DLQ and send back to main topic
   kafka-console-consumer --topic bss.events.customer.DLQ | \
   kafka-console-producer --topic bss.events.customer
   ```

2. **Programmatic Replay**
   - Implement a reprocessing service
   - Read from DLQ topic
   - Apply fix/validation
   - Send to original topic

## Best Practices

1. **Never skip DLQ** - Always handle processing failures
2. **Add metadata** - Include original message context
3. **Monitor continuously** - Set up alerts for DLQ volume
4. **Regular cleanup** - Archive old DLQ messages
5. **Test reprocessing** - Ensure recovery procedures work

## Troubleshooting

### DLQ messages not appearing
- Check Kafka consumer error handler configuration
- Verify DLQ topic exists
- Check Kafka logs for errors

### High DLQ volume
- Review processing logic for bugs
- Check data quality
- Implement validation

### Reprocessing failures
- Ensure fix is backward compatible
- Test with small batch first
- Monitor for new failures
