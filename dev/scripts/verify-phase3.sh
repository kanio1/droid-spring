#!/bin/bash
# Phase 3 Verification Script
# Tests all Phase 3 components: Service Mesh, Kafka Streams, Database Scaling, and Monitoring

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counter for tests
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to print test results
print_test_result() {
    local test_name=$1
    local result=$2
    local message=$3

    TESTS_RUN=$((TESTS_RUN + 1))

    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        [ -n "$message" ] && echo -e "  ${BLUE}→${NC} $message"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name"
        [ -n "$message" ] && echo -e "  ${BLUE}→${NC} $message"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# Header
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}BSS System - Phase 3 Verification${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 1. SERVICE MESH (ENVOY) TESTS
echo -e "${YELLOW}1. SERVICE MESH (ENVOY)${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test Envoy container
if docker ps --format '{{.Names}}' | grep -q "^bss-envoy$"; then
    print_test_result "Envoy container running" "PASS" "bss-envoy container is running"
else
    print_test_result "Envoy container running" "FAIL" "bss-envoy container not found"
fi

# Test Envoy admin endpoint
if curl -s -f http://localhost:15000/ready > /dev/null 2>&1; then
    print_test_result "Envoy admin endpoint" "PASS" "Admin interface available at http://localhost:15000"
else
    print_test_result "Envoy admin endpoint" "FAIL" "Admin interface not accessible"
fi

# Test Envoy clusters
if curl -s http://localhost:15000/clusters | grep -q "bss_backend_cluster"; then
    print_test_result "Envoy cluster configuration" "PASS" "Backend cluster configured"
else
    print_test_result "Envoy cluster configuration" "FAIL" "Backend cluster not found"
fi

# Test Envoy stats
if curl -s http://localhost:15000/stats | grep -q "envoy"; then
    print_test_result "Envoy stats endpoint" "PASS" "Stats available at http://localhost:15000/stats"
else
    print_test_result "Envoy stats endpoint" "FAIL" "Stats endpoint not accessible"
fi

echo ""

# 2. KAFKA STREAMS TESTS
echo -e "${YELLOW}2. KAFKA STREAMS${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test customer analytics stream container
if docker ps --format '{{.Names}}' | grep -q "^bss-kafka-streams-customer$"; then
    print_test_result "Customer analytics stream" "PASS" "bss-kafka-streams-customer container is running"
else
    print_test_result "Customer analytics stream" "FAIL" "bss-kafka-streams-customer container not found"
fi

# Test order processor stream container
if docker ps --format '{{.Names}}' | grep -q "^bss-kafka-streams-order$"; then
    print_test_result "Order processor stream" "PASS" "bss-kafka-streams-order container is running"
else
    print_test_result "Order processor stream" "FAIL" "bss-kafka-streams-order container not found"
fi

# Test customer events topic
if docker exec bss-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | grep -q "bss.customer.events"; then
    print_test_result "Customer events topic" "PASS" "bss.customer.events topic exists"
else
    print_test_result "Customer events topic" "FAIL" "bss.customer.events topic not found"
fi

# Test order events topic
if docker exec bss-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | grep -q "bss.order.events"; then
    print_test_result "Order events topic" "PASS" "bss.order.events topic exists"
else
    print_test_result "Order events topic" "FAIL" "bss.order.events topic not found"
fi

# Test analytics events topic
if docker exec bss-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | grep -q "bss.analytics.events"; then
    print_test_result "Analytics events topic" "PASS" "bss.analytics.events topic exists"
else
    print_test_result "Analytics events topic" "FAIL" "bss.analytics.events topic not found"
fi

# Test stream processing
echo "Test customer event" | docker exec -i bss-kafka-1 kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic bss.customer.events 2>/dev/null || true

sleep 2

if docker exec bss-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic bss.analytics.events \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 2000 2>/dev/null | grep -q "customer.analytics"; then
    print_test_result "Stream processing" "PASS" "Customer events processed to analytics"
else
    print_test_result "Stream processing" "FAIL" "Stream processing issue detected"
fi

echo ""

# 3. DATABASE SCALING TESTS
echo -e "${YELLOW}3. DATABASE SCALING${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test PostgreSQL primary
if docker ps --format '{{.Names}}' | grep -q "^bss-postgres$"; then
    print_test_result "PostgreSQL primary" "PASS" "bss-postgres container is running"
else
    print_test_result "PostgreSQL primary" "FAIL" "bss-postgres container not found"
fi

# Test PostgreSQL replica 1
if docker ps --format '{{.Names}}' | grep -q "^bss-postgres-replica-1$"; then
    print_test_result "PostgreSQL replica 1" "PASS" "bss-postgres-replica-1 container is running"
else
    print_test_result "PostgreSQL replica 1" "FAIL" "bss-postgres-replica-1 container not found"
fi

# Test PostgreSQL replica 2
if docker ps --format '{{.Names}}' | grep -q "^bss-postgres-replica-2$"; then
    print_test_result "PostgreSQL replica 2" "PASS" "bss-postgres-replica-2 container is running"
else
    print_test_result "PostgreSQL replica 2" "FAIL" "bss-postgres-replica-2 container not found"
fi

# Test replica replication status
if docker exec bss-postgres-replica-1 pg_isready -U postgres 2>/dev/null | grep -q "accepting connections"; then
    print_test_result "Replica 1 health" "PASS" "Replica 1 is accepting connections"
else
    print_test_result "Replica 1 health" "FAIL" "Replica 1 not ready"
fi

if docker exec bss-postgres-replica-2 pg_isready -U postgres 2>/dev/null | grep -q "accepting connections"; then
    print_test_result "Replica 2 health" "PASS" "Replica 2 is accepting connections"
else
    print_test_result "Replica 2 health" "FAIL" "Replica 2 not ready"
fi

# Test HAProxy
if docker ps --format '{{.Names}}' | grep -q "^bss-haproxy$"; then
    print_test_result "HAProxy container" "PASS" "bss-haproxy container is running"
else
    print_test_result "HAProxy container" "FAIL" "bss-haproxy container not found"
fi

# Test HAProxy stats
if curl -s http://localhost:8084/stats;csv 2>/dev/null | grep -q "postgres"; then
    print_test_result "HAProxy stats" "PASS" "HAProxy statistics available at http://localhost:8084/stats"
else
    print_test_result "HAProxy stats" "FAIL" "HAProxy statistics not accessible"
fi

# Test HAProxy configuration
if docker exec bss-haproxy haproxy -c -f /usr/local/etc/haproxy/haproxy.cfg 2>/dev/null; then
    print_test_result "HAProxy configuration" "PASS" "HAProxy configuration is valid"
else
    print_test_result "HAProxy configuration" "FAIL" "HAProxy configuration error"
fi

echo ""

# 4. ADVANCED MONITORING TESTS
echo -e "${YELLOW}4. ADVANCED MONITORING${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test AlertManager
if docker ps --format '{{.Names}}' | grep -q "^bss-alertmanager$"; then
    print_test_result "AlertManager container" "PASS" "bss-alertmanager container is running"
else
    print_test_result "AlertManager container" "FAIL" "bss-alertmanager container not found"
fi

# Test AlertManager API
if curl -s -f http://localhost:9093/-/healthy > /dev/null 2>&1; then
    print_test_result "AlertManager API" "PASS" "AlertManager API available at http://localhost:9093"
else
    print_test_result "AlertManager API" "FAIL" "AlertManager API not accessible"
fi

# Test AlertManager alerts endpoint
if curl -s http://localhost:9093/api/v1/alerts 2>/dev/null | grep -q "\[\]"; then
    print_test_result "AlertManager alerts" "PASS" "AlertManager alerts endpoint working"
else
    print_test_result "AlertManager alerts" "FAIL" "AlertManager alerts endpoint error"
fi

# Test Node Exporter
if docker ps --format '{{.Names}}' | grep -q "^bss-node-exporter$"; then
    print_test_result "Node Exporter container" "PASS" "bss-node-exporter container is running"
else
    print_test_result "Node Exporter container" "FAIL" "bss-node-exporter container not found"
fi

# Test Node Exporter metrics
if curl -s -f http://localhost:9100/metrics > /dev/null 2>&1; then
    print_test_result "Node Exporter metrics" "PASS" "Node Exporter metrics available at http://localhost:9100/metrics"
else
    print_test_result "Node Exporter metrics" "FAIL" "Node Exporter metrics not accessible"
fi

# Test pgMonitor
if docker ps --format '{{.Names}}' | grep -q "^bss-pgmonitor$"; then
    print_test_result "pgMonitor container" "PASS" "bss-pgmonitor container is running"
else
    print_test_result "pgMonitor container" "FAIL" "bss-pgmonitor container not found"
fi

# Test pgMonitor metrics
if curl -s -f http://localhost:9187/metrics > /dev/null 2>&1; then
    print_test_result "pgMonitor metrics" "PASS" "pgMonitor metrics available at http://localhost:9187/metrics"
else
    print_test_result "pgMonitor metrics" "FAIL" "pgMonitor metrics not accessible"
fi

echo ""

# 5. PROMETHEUS TARGETS VERIFICATION
echo -e "${YELLOW}5. PROMETHEUS TARGETS${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test Prometheus targets
PROMETHEUS_TARGETS=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"url":"[^"]*"' | wc -l)

if [ "$PROMETHEUS_TARGETS" -ge 20 ]; then
    print_test_result "Prometheus targets" "PASS" "Found $PROMETHEUS_TARGETS scrape targets (expected 20+)"
else
    print_test_result "Prometheus targets" "FAIL" "Only found $PROMETHEUS_TARGETS scrape targets (expected 20+)"
fi

# Check specific Phase 3 targets
PHASE3_TARGETS=("envoy:15000" "haproxy:8084" "alertmanager:9093" "node-exporter:9100" "pgmonitor:9187")
for target in "${PHASE3_TARGETS[@]}"; do
    if curl -s http://localhost:9090/api/v1/targets 2>/dev/null | grep -q "$target"; then
        print_test_result "Prometheus target: $target" "PASS" "Target is being scraped"
    else
        print_test_result "Prometheus target: $target" "FAIL" "Target not found in scrape list"
    fi
done

echo ""

# SUMMARY
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}VERIFICATION SUMMARY${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Total tests run: $TESTS_RUN"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed! Phase 3 is operational.${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠ Some tests failed. Check the output above for details.${NC}"
    exit 1
fi
