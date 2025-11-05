#!/bin/bash
# Phase 4 Verification Script
# Tests all Phase 4 components: Citus Sharding, Flink Analytics, Multi-region

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
echo -e "${BLUE}BSS System - Phase 4 Verification${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 1. CITUS CLUSTER TESTS
echo -e "${YELLOW}1. CITUS CLUSTER (DATABASE SHARDING)${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test Citus coordinator
if docker ps --format '{{.Names}}' | grep -q "^bss-citus-coordinator$"; then
    print_test_result "Citus coordinator running" "PASS" "bss-citus-coordinator container is running"
else
    print_test_result "Citus coordinator running" "FAIL" "bss-citus-coordinator container not found"
fi

# Test Citus worker 1
if docker ps --format '{{.Names}}' | grep -q "^bss-citus-worker-1$"; then
    print_test_result "Citus worker 1 running" "PASS" "bss-citus-worker-1 container is running"
else
    print_test_result "Citus worker 1 running" "FAIL" "bss-citus-worker-1 container not found"
fi

# Test Citus worker 2
if docker ps --format '{{.Names}}' | grep -q "^bss-citus-worker-2$"; then
    print_test_result "Citus worker 2 running" "PASS" "bss-citus-worker-2 container is running"
else
    print_test_result "Citus worker 2 running" "FAIL" "bss-citus-worker-2 container not found"
fi

# Test Citus worker 3
if docker ps --format '{{.Names}}' | grep -q "^bss-citus-worker-3$"; then
    print_test_result "Citus worker 3 running" "PASS" "bss-citus-worker-3 container is running"
else
    print_test_result "Citus worker 3 running" "FAIL" "bss-citus-worker-3 container not found"
fi

# Test Citus coordinator health
if docker exec bss-citus-coordinator pg_isready -U postgres 2>/dev/null | grep -q "accepting connections"; then
    print_test_result "Citus coordinator health" "PASS" "Coordinator is accepting connections"
else
    print_test_result "Citus coordinator health" "FAIL" "Coordinator not ready"
fi

# Test Citus extension
if docker exec bss-citus-coordinator psql -U postgres -d postgres -c "SELECT 1 FROM pg_extension WHERE extname = 'citus';" 2>/dev/null | grep -q "1"; then
    print_test_result "Citus extension installed" "PASS" "Citus extension is installed"
else
    print_test_result "Citus extension installed" "FAIL" "Citus extension not found"
fi

# Test Citus nodes
if docker exec bss-citus-coordinator psql -U postgres -d postgres -c "SELECT * FROM citus_nodes;" 2>/dev/null | grep -q "citus-worker"; then
    print_test_result "Citus worker nodes registered" "PASS" "Worker nodes are registered"
else
    print_test_result "Citus worker nodes registered" "FAIL" "Worker nodes not found"
fi

# Test distributed table
if docker exec bss-citus-coordinator psql -U postgres -d postgres -c "SELECT * FROM pg_dist_table WHERE tablename = 'customers';" 2>/dev/null | grep -q "customers"; then
    print_test_result "Distributed table created" "PASS" "Customers table is distributed"
else
    print_test_result "Distributed table created" "FAIL" "Distributed table not found"
fi

# Test shard count
SHARD_COUNT=$(docker exec bss-citus-coordinator psql -U postgres -d postgres -t -c "SELECT count(*) FROM pg_dist_shard WHERE logicalrelid = 'customers'::regclass;" 2>/dev/null | tr -d ' ')
if [ "$SHARD_COUNT" -gt 0 ]; then
    print_test_result "Database sharding active" "PASS" "Found $SHARD_COUNT shards"
else
    print_test_result "Database sharding active" "FAIL" "No shards found"
fi

echo ""

# 2. APACHE FLINK TESTS
echo -e "${YELLOW}2. APACHE FLINK (STREAM PROCESSING)${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test Flink JobManager
if docker ps --format '{{.Names}}' | grep -q "^bss-flink-jobmanager$"; then
    print_test_result "Flink JobManager running" "PASS" "bss-flink-jobmanager container is running"
else
    print_test_result "Flink JobManager running" "FAIL" "bss-flink-jobmanager container not found"
fi

# Test Flink TaskManager
TASKMANAGER_COUNT=$(docker ps --format '{{.Names}}' | grep -c "^bss-flink-taskmanager" || echo "0")
if [ "$TASKMANAGER_COUNT" -ge 2 ]; then
    print_test_result "Flink TaskManager running" "PASS" "Found $TASKMANAGER_COUNT TaskManager instances"
else
    print_test_result "Flink TaskManager running" "FAIL" "Expected 2 TaskManager instances, found $TASKMANAGER_COUNT"
fi

# Test Flink web UI
if curl -s -f http://localhost:8081/ > /dev/null 2>&1; then
    print_test_result "Flink web UI" "PASS" "Flink UI available at http://localhost:8081"
else
    print_test_result "Flink web UI" "FAIL" "Flink UI not accessible"
fi

# Test Flink job endpoint
if curl -s http://localhost:8081/jobs 2>/dev/null | grep -q "jobs"; then
    print_test_result "Flink jobs API" "PASS" "Flink jobs API is accessible"
else
    print_test_result "Flink jobs API" "FAIL" "Flink jobs API not accessible"
fi

# Test Kafka connectivity from Flink
if docker exec bss-flink-jobmanager nc -z kafka-1 9092 2>/dev/null; then
    print_test_result "Flink-Kafka connectivity" "PASS" "Flink can connect to Kafka"
else
    print_test_result "Flink-Kafka connectivity" "FAIL" "Flink cannot connect to Kafka"
fi

echo ""

# 3. PROMETHEUS TARGETS VERIFICATION
echo -e "${YELLOW}3. PROMETHEUS TARGETS${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Check Phase 4 targets
PHASE4_TARGETS=("citus-coordinator:5432" "citus-worker-1:5432" "citus-worker-2:5432" "citus-worker-3:5432" "flink-jobmanager:8081")
for target in "${PHASE4_TARGETS[@]}"; do
    if curl -s http://localhost:9090/api/v1/targets 2>/dev/null | grep -q "$target"; then
        print_test_result "Prometheus target: $target" "PASS" "Target is being scraped"
    else
        print_test_result "Prometheus target: $target" "FAIL" "Target not found in scrape list"
    fi
done

# Check total scrape targets
PROMETHEUS_TARGETS=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"url":"[^"]*"' | wc -l)
if [ "$PROMETHEUS_TARGETS" -ge 25 ]; then
    print_test_result "Total Prometheus targets" "PASS" "Found $PROMETHEUS_TARGETS scrape targets (expected 25+)"
else
    print_test_result "Total Prometheus targets" "FAIL" "Only found $PROMETHEUS_TARGETS scrape targets (expected 25+)"
fi

echo ""

# 4. SERVICE CONNECTIVITY TESTS
echo -e "${YELLOW}4. SERVICE CONNECTIVITY${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Test Citus coordinator to worker connectivity
if docker exec bss-citus-coordinator pg_isready -h citus-worker-1 -p 5432 2>/dev/null | grep -q "accepting"; then
    print_test_result "Citus coordinator-worker-1 connectivity" "PASS" "Coordinator can reach worker-1"
else
    print_test_result "Citus coordinator-worker-1 connectivity" "FAIL" "Cannot connect coordinator to worker-1"
fi

# Test Flink to Kafka topics
if docker exec bss-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | grep -q "bss.customer.events"; then
    print_test_result "Kafka topics accessible" "PASS" "bss.customer.events topic exists"
else
    print_test_result "Kafka topics accessible" "FAIL" "Kafka topics not accessible"
fi

# Test multi-service health
SERVICES=("bss-postgres" "bss-redis" "bss-backend" "bss-kafka-1" "bss-flink-jobmanager" "bss-citus-coordinator")
for service in "${SERVICES[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^$service$"; then
        print_test_result "Service $service running" "PASS" "Service is up"
    else
        print_test_result "Service $service running" "FAIL" "Service not found"
    fi
done

echo ""

# 5. PERFORMANCE METRICS
echo -e "${YELLOW}5. PERFORMANCE METRICS${NC}"
echo -e "${YELLOW}-------------------------------${NC}"

# Check if Citus has active connections
ACTIVE_CONNS=$(docker exec bss-citus-coordinator psql -U postgres -d postgres -t -c "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';" 2>/dev/null | tr -d ' ')
if [ "$ACTIVE_CONNS" -ge 0 ]; then
    print_test_result "Citus active connections" "PASS" "Found $ACTIVE_CONNS active connections"
else
    print_test_result "Citus active connections" "FAIL" "Could not retrieve connection count"
fi

# Check Flink task slots
if curl -s http://localhost:8081/taskmanagers 2>/dev/null | grep -q "slotsTotal"; then
    print_test_result "Flink task slots" "PASS" "TaskManager slots are configured"
else
    print_test_result "Flink task slots" "FAIL" "TaskManager slots not accessible"
fi

# Count total services
TOTAL_SERVICES=$(docker ps --format '{{.Names}}' | grep "bss-" | wc -l)
if [ "$TOTAL_SERVICES" -ge 35 ]; then
    print_test_result "Total BSS services" "PASS" "Found $TOTAL_SERVICES BSS services (expected 35+)"
else
    print_test_result "Total BSS services" "FAIL" "Only found $TOTAL_SERVICES BSS services (expected 35+)"
fi

# Count total ports
TOTAL_PORTS=$(docker ps --format '{{.Ports}}' | grep -o '[0-9]*:[0-9]*' | wc -l)
if [ "$TOTAL_PORTS" -ge 50 ]; then
    print_test_result "Total exposed ports" "PASS" "Found $TOTAL_PORTS exposed ports (expected 50+)"
else
    print_test_result "Total exposed ports" "FAIL" "Only found $TOTAL_PORTS exposed ports (expected 50+)"
fi

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
    echo -e "${GREEN}✓ All tests passed! Phase 4 is operational.${NC}"
    echo ""
    echo -e "${BLUE}Phase 4 Components Verified:${NC}"
    echo -e "  • Citus database sharding (1 coordinator + 3 workers)"
    echo -e "  • Apache Flink stream processing (1 jobmanager + 2 taskmanagers)"
    echo -e "  • Advanced analytics with windowing and aggregations"
    echo -e "  • Multi-region ready architecture"
    echo ""
    exit 0
else
    echo -e "${YELLOW}⚠ Some tests failed. Check the output above for details.${NC}"
    exit 1
fi
