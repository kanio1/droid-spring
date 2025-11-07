#!/bin/bash
# Script to create Dead Letter Queue (DLQ) topic for Kafka
# Run this after Kafka cluster is up

echo "Creating Dead Letter Queue (DLQ) topics..."

# Create DLQ topics with appropriate configuration
kafka-topics --create \
  --topic bss.events.DLQ \
  --partitions 20 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete

# Create customer DLQ
kafka-topics --create \
  --topic bss.events.customer.DLQ \
  --partitions 10 \
  --replication-factor 3 \
  --config min.insync.replicas=2

# Create order DLQ
kafka-topics --create \
  --topic bss.events.order.DLQ \
  --partitions 10 \
  --replication-factor 3 \
  --config min.insync.replicas=2

# Create payment DLQ
kafka-topics --create \
  --topic bss.events.payment.DLQ \
  --partitions 10 \
  --replication-factor 3 \
  --config min.insync.replicas=2

# Create invoice DLQ
kafka-topics --create \
  --topic bss.events.invoice.DLQ \
  --partitions 10 \
  --replication-factor 3 \
  --config min.insync.replicas=2

echo "DLQ topics created successfully!"

# List DLQ topics
kafka-topics --list --topic "*.DLQ"
