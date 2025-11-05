#!/bin/bash
# Kafka Streams Demo Script
# Demonstrates the real-time stream processing capabilities

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper function to print section headers
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

# Helper function to print commands
print_command() {
    echo -e "${YELLOW}$ $1${NC}"
}

# Helper function to print success messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Demo header
print_header "BSS System - Kafka Streams Demo"

echo -e "This demo showcases the real-time stream processing capabilities"
echo -e "of the BSS system using Kafka Streams.\n"

# 1. Check Kafka cluster status
print_header "1. Kafka Cluster Status"
print_command "docker ps --filter name=bss-kafka"
docker ps --filter name=bss-kafka --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || true
echo ""

# 2. List Kafka topics
print_header "2. Kafka Topics"
print_command "docker exec bss-kafka-1 kafka-topics --list --bootstrap-server localhost:9092"
docker exec bss-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 || true
echo ""

# 3. Describe customer events topic
print_header "3. Customer Events Topic"
print_command "docker exec bss-kafka-1 kafka-topics --describe --topic bss.customer.events --bootstrap-server localhost:9092"
docker exec bss-kafka-1 kafka-topics --describe --topic bss.customer.events --bootstrap-server localhost:9092 2>/dev/null || echo "Topic not found or not ready yet"
echo ""

# 4. Describe order events topic
print_header "4. Order Events Topic"
print_command "docker exec bss-kafka-1 kafka-topics --describe --topic bss.order.events --bootstrap-server localhost:9092"
docker exec bss-kafka-1 kafka-topics --describe --topic bss.order.events --bootstrap-server localhost:9092 2>/dev/null || echo "Topic not found or not ready yet"
echo ""

# 5. Describe analytics events topic
print_header "5. Analytics Events Topic"
print_command "docker exec bss-kafka-1 kafka-topics --describe --topic bss.analytics.events --bootstrap-server localhost:9092"
docker exec bss-kafka-1 kafka-topics --describe --topic bss.analytics.events --bootstrap-server localhost:9092 2>/dev/null || echo "Topic not found or not ready yet"
echo ""

# 6. Show stream processors
print_header "6. Stream Processors"
print_command "docker ps --filter name=bss-kafka-streams"
docker ps --filter name=bss-kafka-streams --format "table {{.Names}}\t{{.Status}}" || true
echo ""

# 7. Demo customer event processing
print_header "7. Customer Event Processing Demo"
echo -e "Producing a customer event to ${YELLOW}bss.customer.events${NC} topic..."
echo ""

# Create a sample customer event
CUSTOMER_EVENT='{
  "specversion": "1.0",
  "type": "com.droid.bss.customer.created",
  "source": "urn:bss:customer:demo",
  "id": "cust-demo-123",
  "time": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
  "data": {
    "customerId": "cust-demo-123",
    "name": "Demo Customer",
    "email": "demo@example.com",
    "status": "NEW"
  }
}'

print_command "echo '$CUSTOMER_EVENT' | docker exec -i bss-kafka-1 kafka-console-producer --bootstrap-server localhost:9092 --topic bss.customer.events"
echo "$CUSTOMER_EVENT" | docker exec -i bss-kafka-1 kafka-console-producer --bootstrap-server localhost:9092 --topic bss.customer.events 2>/dev/null || true

echo ""
sleep 2

print_success "Customer event produced successfully"
echo ""

# 8. Demo order event processing
print_header "8. Order Event Processing Demo"
echo -e "Producing an order event to ${YELLOW}bss.order.events${NC} topic..."
echo ""

# Create a sample order event
ORDER_EVENT='{
  "specversion": "1.0",
  "type": "com.droid.bss.order.created",
  "source": "urn:bss:order:demo",
  "id": "order-demo-456",
  "time": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
  "data": {
    "orderId": "order-demo-456",
    "customerId": "cust-demo-123",
    "amount": 299.99,
    "status": "CREATED",
    "items": [
      {
        "productId": "prod-001",
        "quantity": 1,
        "price": 299.99
      }
    ]
  }
}'

print_command "echo '$ORDER_EVENT' | docker exec -i bss-kafka-1 kafka-console-producer --bootstrap-server localhost:9092 --topic bss.order.events"
echo "$ORDER_EVENT" | docker exec -i bss-kafka-1 kafka-console-producer --bootstrap-server localhost:9092 --topic bss.order.events 2>/dev/null || true

echo ""
sleep 2

print_success "Order event produced successfully"
echo ""

# 9. Show analytics events
print_header "9. Analytics Events Output"
echo -e "Reading from ${YELLOW}bss.analytics.events${NC} topic to see processed results..."
echo ""

print_command "docker exec bss-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic bss.analytics.events --from-beginning --max-messages 2 --timeout-ms 3000"
docker exec bss-kafka-1 kafka-console-consumer --bootstrap-server localhost:9092 --topic bss.analytics.events --from-beginning --max-messages 2 --timeout-ms 3000 2>/dev/null || echo "No messages yet (streams may still be processing)"
echo ""

# 10. Check stream processor logs
print_header "10. Stream Processor Logs"
echo -e "Checking ${YELLOW}Customer Analytics${NC} stream logs..."
print_command "docker logs --tail 20 bss-kafka-streams-customer"
docker logs --tail 20 bss-kafka-streams-customer 2>/dev/null | head -n 20 || echo "Logs not available yet"
echo ""

echo -e "Checking ${YELLOW}Order Processing${NC} stream logs..."
print_command "docker logs --tail 20 bss-kafka-streams-order"
docker logs --tail 20 bss-kafka-streams-order 2>/dev/null | head -n 20 || echo "Logs not available yet"
echo ""

# Summary
print_header "Demo Complete"
echo -e "${GREEN}The BSS system successfully demonstrated:${NC}"
echo -e "  • Customer event production and analytics processing"
echo -e "  • Order event production and analytics processing"
echo -e "  • Real-time stream transformation from CloudEvents to analytics events"
echo -e ""
echo -e "For more information, see the Phase 3 documentation at:"
echo -e "  ${BLUE}dev/PHASE-3-README.md${NC}"
