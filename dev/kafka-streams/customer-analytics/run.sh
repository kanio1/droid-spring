#!/bin/bash
# Customer Analytics Kafka Streams Application
# Simulates real-time customer behavior analysis

echo "========================================"
echo "BSS Customer Analytics Stream"
echo "========================================"
echo ""

# Dependencies check
command -v jq >/dev/null 2>&1 || { echo "jq is required but not installed. Aborting." >&2; exit 1; }

# Configuration
BOOTSTRAP_SERVERS=${BOOTSTRAP_SERVERS:-"kafka-1:9092,kafka-2:9092,kafka-3:9092"}
APPLICATION_ID=${APPLICATION_ID:-"bss-customer-analytics"}
INPUT_TOPIC=${INPUT_TOPIC:-"bss.customer.events"}
OUTPUT_TOPIC=${OUTPUT_TOPIC:-"bss.analytics.events"}

echo "Configuration:"
echo "  Bootstrap Servers: $BOOTSTRAP_SERVERS"
echo "  Application ID: $APPLICATION_ID"
echo "  Input Topic: $INPUT_TOPIC"
echo "  Output Topic: $OUTPUT_TOPIC"
echo ""

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 5

# Create input topic if not exists
echo "Creating input topic: $INPUT_TOPIC"
kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic $INPUT_TOPIC \
  --partitions 3 \
  --replication-factor 3 \
  --if-not-exists 2>/dev/null || true

# Create output topic if not exists
echo "Creating output topic: $OUTPUT_TOPIC"
kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic $OUTPUT_TOPIC \
  --partitions 3 \
  --replication-factor 3 \
  --if-not-exists 2>/dev/null || true

echo ""
echo "Starting Customer Analytics Stream Processor..."
echo "Listening on: $INPUT_TOPIC"
echo "Publishing to: $OUTPUT_TOPIC"
echo ""

# Start consumer
kafka-console-consumer \
  --bootstrap-server $BOOTSTRAP_SERVERS \
  --topic $INPUT_TOPIC \
  --group $APPLICATION_ID \
  --from-beginning \
  --max-messages 1000000 \
  --timeout-ms 1000 \
  2>/dev/null | while IFS= read -r line; do
    # Process customer event
    if echo "$line" | jq . >/dev/null 2>&1; then
      # Extract event type
      EVENT_TYPE=$(echo "$line" | jq -r '.type // "unknown"' 2>/dev/null)
      CUSTOMER_ID=$(echo "$line" | jq -r '.data.customerId // "unknown"' 2>/dev/null)
      CUSTOMER_NAME=$(echo "$line" | jq -r '.data.name // "unknown"' 2>/dev/null)
      TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
      
      # Generate analytics event
      ANALYTICS_EVENT=$(jq -n \
        --arg type "customer.analytics" \
        --arg ts "$TIMESTAMP" \
        --arg cid "$CUSTOMER_ID" \
        --arg cname "$CUSTOMER_NAME" \
        --arg etype "$EVENT_TYPE" \
        '{
          specversion: "1.0",
          type: $type,
          source: "urn:bss:stream:customer-analytics",
          id: ($cid + "-" + $ts),
          time: $ts,
          data: {
            customerId: $cid,
            customerName: $cname,
            eventType: $etype,
            analyticsType: "customer_behavior",
            timestamp: $ts,
            processed: true
          }
        }')
      
      # Publish analytics event
      echo "$ANALYTICS_EVENT" | kafka-console-producer \
        --bootstrap-server $BOOTSTRAP_SERVERS \
        --topic $OUTPUT_TOPIC \
        2>/dev/null || true
    fi
  done

echo "Stream processor stopped."
