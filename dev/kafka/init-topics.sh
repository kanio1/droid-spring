#!/bin/bash
# Kafka Topics Initialization Script
# This script creates the necessary topics for the BSS system

echo "=========================================="
echo "Initializing Kafka Topics for BSS System"
echo "=========================================="
echo ""

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 10

# Bootstrap servers
KAFKA_SERVERS="localhost:9092,localhost:9093,localhost:9094"
KAFKA_TOPICS_DIR="/tmp/topics"

# Create topics directory
mkdir -p "$KAFKA_TOPICS_DIR"

# 1. BSS Events Topic
echo "Creating topic: bss.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 2. Customer Events Topic
echo "Creating topic: bss.customer.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.customer.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 3. Order Events Topic
echo "Creating topic: bss.order.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.order.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 4. Invoice Events Topic
echo "Creating topic: bss.invoice.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.invoice.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 5. Payment Events Topic
echo "Creating topic: bss.payment.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.payment.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 6. Notification Events Topic
echo "Creating topic: bss.notification.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.notification.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 7. Analytics Events Topic
echo "Creating topic: bss.analytics.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.analytics.events \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=2592000000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 8. Audit Events Topic
echo "Creating topic: bss.audit.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.audit.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=31536000000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 9. Service Provisioning Topic
echo "Creating topic: bss.service.provisioning"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.service.provisioning \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# 10. Billing Events Topic
echo "Creating topic: bss.billing.events"
kafka-topics --create \
  --bootstrap-server "$KAFKA_SERVERS" \
  --topic bss.billing.events \
  --partitions 3 \
  --replication-factor 3 \
  --config retention.ms=2592000000 \
  --config cleanup.policy=delete \
  --if-not-exists

echo ""
echo "=========================================="
echo "Listing all BSS topics:"
echo "=========================================="
kafka-topics --list --bootstrap-server "$KAFKA_SERVERS" | grep bss

echo ""
echo "=========================================="
echo "Topic Details:"
echo "=========================================="
kafka-topics --describe --bootstrap-server "$KAFKA_SERVERS" --topic bss.events

echo ""
echo "=========================================="
echo "Kafka Topics Initialization Complete!"
echo "=========================================="
echo ""
echo "Topic Summary:"
echo "  - Event Topics: 10"
echo "  - Total Partitions: 30"
echo "  - Replication Factor: 3"
echo "  - Retention Period: 7 days (events), 30 days (analytics), 1 year (audit)"
echo ""
