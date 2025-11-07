#!/bin/bash
# ============================================
# Kafka ACLs Setup Script
# ============================================
# Purpose: Configure Access Control Lists for Kafka
# Ensures principle-of-least-privilege access
# Created: 2025-11-07
# ============================================

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka-1:9092}"
KAFKA_CLIENT_CONFIG="/etc/ssl/certs/kafka-client.properties"
LOG_FILE="/tmp/kafka-acls-setup.log"

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

# Function to execute Kafka ACL commands
execute_kafka_acls() {
    local principal=$1
    local operation=$2
    local resource_type=$3
    local resource_name=$4
    local permission=$5

    docker exec bss-kafka-1 kafka-acls.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" \
        --add \
        --principal "$principal" \
        --operation "$operation" \
        --resource-type "$resource_type" \
        --resource-name "$resource_name" \
        --$permission
}

# Function to list existing ACLs
list_acls() {
    log "Listing existing ACLs..."
    docker exec bss-kafka-1 kafka-acls.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" \
        --list
}

# ============================================
# Main ACL Configuration
# ============================================

log "Starting Kafka ACLs setup..."

# Wait for Kafka to be ready
log "Waiting for Kafka to be ready..."
for i in {1..30}; do
    if docker exec bss-kafka-1 kafka-broker-api-versions \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" \
        > /dev/null 2>&1; then
        log "Kafka is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        error "Kafka did not become ready in time"
    fi
    sleep 2
done

# ============================================
# 1. Application Service Accounts
# ============================================

log "Setting up application service accounts..."

# Backend Service Account
log "Creating ACL for backend-service..."
execute_kafka_acls "User:backend-service" "Write" "Topic" "bss.*" "allow"
execute_kafka_acls "User:backend-service" "Read" "Topic" "bss.*" "allow"
execute_kafka_acls "User:backend-service" "Describe" "Topic" "bss.*" "allow"
execute_kafka_acls "User:backend-service" "DescribeConfigs" "Broker" "*" "allow"
execute_kafka_acls "User:backend-service" "AlterConfigs" "Broker" "bss-config" "allow"
execute_kafka_acls "User:backend-service" "Create" "Topic" "bss.user.created" "allow"
execute_kafka_acls "User:backend-service" "Write" "Group" "bss-backend-group" "allow"
execute_kafka_acls "User:backend-service" "Read" "Group" "bss-backend-group" "allow"

# Frontend Service Account
log "Creating ACL for frontend-service..."
execute_kafka_acls "User:frontend-service" "Write" "Topic" "bss.user.commands" "allow"
execute_kafka_acls "User:frontend-service" "Read" "Topic" "bss.customer.events" "allow"
execute_kafka_acls "User:frontend-service" "Describe" "Topic" "bss.*" "allow"

# Customer Analytics Service
log "Creating ACL for customer-analytics-service..."
execute_kafka_acls "User:customer-analytics-service" "Read" "Topic" "bss.customer.events" "allow"
execute_kafka_acls "User:customer-analytics-service" "Write" "Topic" "bss.analytics.events" "allow"
execute_kafka_acls "User:customer-analytics-service" "Describe" "Topic" "bss.*" "allow"

# Order Processing Service
log "Creating ACL for order-processor-service..."
execute_kafka_acls "User:order-processor-service" "Read" "Topic" "bss.order.events" "allow"
execute_kafka_acls "User:order-processor-service" "Write" "Topic" "bss.analytics.events" "allow"
execute_kafka_acls "User:order-processor-service" "Describe" "Topic" "bss.*" "allow"

# ============================================
# 2. Topic-Specific ACLs
# ============================================

log "Setting up topic-specific ACLs..."

# Customer Events
log "Configuring ACLs for customer events..."
execute_kafka_acls "User:backend-service" "Write" "Topic" "bss.customer.events" "allow"
execute_kafka_acls "User:customer-analytics-service" "Read" "Topic" "bss.customer.events" "allow"
execute_kafka_acls "User:frontend-service" "Read" "Topic" "bss.customer.events" "allow"

# Order Events
log "Configuring ACLs for order events..."
execute_kafka_acls "User:backend-service" "Write" "Topic" "bss.order.events" "allow"
execute_kafka_acls "User:order-processor-service" "Read" "Topic" "bss.order.events" "allow"

# Payment Events
log "Configuring ACLs for payment events..."
execute_kafka_acls "User:backend-service" "Write" "Topic" "bss.payment.events" "allow"
execute_kafka_acls "User:payment-processor-service" "Read" "Topic" "bss.payment.events" "allow"

# Invoice Events
log "Configuring ACLs for invoice events..."
execute_kafka_acls "User:backend-service" "Write" "Topic" "bss.invoice.events" "allow"
execute_kafka_acls "User:invoice-service" "Read" "Topic" "bss.invoice.events" "allow"

# DLQ Topics (read-only for monitoring)
log "Configuring ACLs for DLQ topics..."
execute_kafka_acls "User:backend-service" "Write" "Topic" "bss.*.DLQ" "allow"
execute_kafka_acls "User:monitoring-service" "Read" "Topic" "bss.*.DLQ" "allow"
execute_kafka_acls "User:monitoring-service" "Describe" "Topic" "bss.*.DLQ" "allow"

# ============================================
# 3. Consumer Group ACLs
# ============================================

log "Setting up consumer group ACLs..."

# Backend Consumer Group
log "Configuring ACLs for backend consumer group..."
execute_kafka_acls "User:backend-service" "Read" "Group" "bss-backend-group" "allow"
execute_kafka_acls "User:backend-service" "Describe" "Group" "bss-backend-group" "allow"

# Analytics Consumer Groups
log "Configuring ACLs for analytics consumer groups..."
execute_kafka_acls "User:customer-analytics-service" "Read" "Group" "bss-customer-analytics-group" "allow"
execute_kafka_acls "User:order-processor-service" "Read" "Group" "bss-order-processor-group" "allow"

# Frontend Consumer Group
log "Configuring ACLs for frontend consumer group..."
execute_kafka_acls "User:frontend-service" "Read" "Group" "bss-frontend-group" "allow"

# ============================================
# 4. Schema Registry ACLs
# ============================================

log "Setting up Schema Registry ACLs..."
execute_kafka_acls "User:schema-registry" "Read" "Topic" "_schemas" "allow"
execute_kafka_acls "User:schema-registry" "Write" "Topic" "_schemas" "allow"
execute_kafka_acls "User:schema-registry" "Describe" "Topic" "*" "allow"

# ============================================
# 5. Cluster-Level ACLs
# ============================================

log "Setting up cluster-level ACLs..."

# Idempotent writes for all services
log "Configuring idempotent write ACLs..."
execute_kafka_acls "User:backend-service" "IdempotentWrite" "Cluster" "kafka-cluster" "allow"
execute_kafka_acls "User:frontend-service" "IdempotentWrite" "Cluster" "kafka-cluster" "allow"

# Describe cluster access
log "Configuring cluster describe ACLs..."
execute_kafka_acls "User:backend-service" "Describe" "Cluster" "kafka-cluster" "allow"
execute_kafka_acls "User:customer-analytics-service" "Describe" "Cluster" "kafka-cluster" "allow"
execute_kafka_acls "User:order-processor-service" "Describe" "Cluster" "kafka-cluster" "allow"

# ============================================
# 6. Deny Rules (for additional security)
# ============================================

log "Setting up deny rules..."

# Deny internal topics to non-admin users
log "Configuring deny rules for internal topics..."
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
    --command-config "$KAFKA_CLIENT_CONFIG" \
    --add \
    --principal "User:*" \
    --operation "All" \
    --resource-type "Topic" \
    --resource-name "__.*" \
    --deny

# Deny access to other services' consumer groups
log "Configuring deny rules for consumer groups..."
docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
    --command-config "$KAFKA_CLIENT_CONFIG" \
    --add \
    --principal "User:frontend-service" \
    --operation "All" \
    --resource-type "Group" \
    --resource-name "bss-backend-group" \
    --deny

docker exec bss-kafka-1 kafka-acls.sh \
    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
    --command-config "$KAFKA_CLIENT_CONFIG" \
    --add \
    --principal "User:backend-service" \
    --operation "All" \
    --resource-type "Group" \
    --resource-name "bss-customer-analytics-group" \
    --deny

# ============================================
# 7. Monitoring and Metrics
# ============================================

log "Setting up monitoring service ACLs..."
execute_kafka_acls "User:monitoring-service" "Describe" "Cluster" "kafka-cluster" "allow"
execute_kafka_acls "User:monitoring-service" "Read" "Topic" "*" "allow"
execute_kafka_acls "User:monitoring-service" "Describe" "Group" "*" "allow"

# JMX access
execute_kafka_acls "User:monitoring-service" "Describe" "Broker" "*" "allow"

# ============================================
# 8. List Final ACLs
# ============================================

log "Listing all configured ACLs..."
list_acls

# ============================================
# Summary
# ============================================

log ""
log "=========================================="
log "Kafka ACLs Configuration Complete!"
log "=========================================="
log ""
log "Summary of configured ACLs:"
log "  ✓ Backend Service: Full access to bss.* topics"
log "  ✓ Frontend Service: Limited read/write access"
log "  ✓ Customer Analytics: Read customer events, write analytics"
log "  ✓ Order Processor: Read order events, write analytics"
log "  ✓ Payment Processor: Read payment events"
log "  ✓ Invoice Service: Read invoice events"
log "  ✓ Schema Registry: Full access to _schemas"
log "  ✓ Monitoring: Read-only access to all topics and groups"
log "  ✓ Deny rules: Protect internal and cross-service resources"
log ""
log "Service Accounts Created:"
log "  - User:backend-service"
log "  - User:frontend-service"
log "  - User:customer-analytics-service"
log "  - User:order-processor-service"
log "  - User:payment-processor-service"
log "  - User:invoice-service"
log "  - User:monitoring-service"
log "  - User:schema-registry"
log ""
log "Log file: $LOG_FILE"
log ""
log "Next steps:"
log "  1. Update client configurations to use service accounts"
log "  2. Distribute SSL certificates to services"
log "  3. Test ACL enforcement"
log "  4. Monitor for unauthorized access attempts"
log "=========================================="

# Generate client configuration templates
log "Generating client configuration templates..."

mkdir -p /home/labadmin/projects/droid-spring/dev/kafka/client-configs

# Backend service config
cat > /home/labadmin/projects/droid-spring/dev/kafka/client-configs/backend-service.properties << 'EOF'
# Kafka Client Configuration for Backend Service
# Generated: 2025-11-07

# Connection
bootstrap.servers=kafka-1:9092,kafka-2:9092,kafka-3:9092
security.protocol=SSL

# SSL Configuration
ssl.truststore.location=/etc/ssl/certs/truststore.jks
ssl.truststore.password=${KAFKA_TRUSTSTORE_PASSWORD}
ssl.keystore.location=/etc/ssl/certs/kafka.p12
ssl.keystore.password=${KAFKA_KEYSTORE_PASSWORD}
ssl.keystore.type=PKCS12
ssl.enabled.protocols=TLSv1.2,TLSv1.3

# SASL Authentication (if using SASL/PLAIN)
# sasl.mechanism=PLAIN
# sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
#   username="backend-service" \
#   password="CHANGE_ME_PASSWORD";

# Client Configuration
client.id=backend-service
group.id=bss-backend-group
session.timeout.ms=30000
heartbeat.interval.ms=3000
max.poll.records=100
enable.auto.commit=false

# Security
security.inter.broker.protocol=SSL
security.protocol=SSL
allow.auto.create.topics=false

# Producer Configuration
acks=all
retries=3
batch.size=16384
linger.ms=5
compression.type=snappy

# Consumer Configuration
auto.offset.reset=earliest
enable.auto.commit=false
EOF

# Frontend service config
cat > /home/labadmin/projects/droid-spring/dev/kafka/client-configs/frontend-service.properties << 'EOF'
# Kafka Client Configuration for Frontend Service
# Generated: 2025-11-07

# Connection
bootstrap.servers=kafka-1:9092,kafka-2:9092,kafka-3:9092
security.protocol=SSL

# SSL Configuration
ssl.truststore.location=/etc/ssl/certs/truststore.jks
ssl.truststore.password=${KAFKA_TRUSTSTORE_PASSWORD}
ssl.keystore.location=/etc/ssl/certs/kafka.p12
ssl.keystore.password=${KAFKA_KEYSTORE_PASSWORD}
ssl.keystore.type=PKCS12
ssl.enabled.protocols=TLSv1.2,TLSv1.3

# Client Configuration
client.id=frontend-service
group.id=bss-frontend-group

# Consumer Configuration
auto.offset.reset=earliest
enable.auto.commit=false
max.poll.records=50

# Security
security.inter.broker.protocol=SSL
security.protocol=SSL
allow.auto.create.topics=false
EOF

log "Client configuration templates generated in /home/labadmin/projects/droid-spring/dev/kafka/client-configs/"
log "Kafka ACLs setup completed successfully!"

exit 0
