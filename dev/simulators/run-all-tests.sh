#!/bin/bash
# Test Runner for Infrastructure Performance Tests
# Tests all components: PostgreSQL, Redis, Kafka

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TEST_DURATION=1  # minutes
TARGET_THROUGHPUT=6667  # events per second (400k/min)
NUM_TENANTS=5
BATCH_SIZE=100
WORKERS=10

echo -e "${BLUE}=================================================${NC}"
echo -e "${BLUE}  Infrastructure Performance Test Suite${NC}"
echo -e "${BLUE}  Target: $TARGET_THROUGHPUT events/sec (400k/min)${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""

# Check if Go is installed
if ! command -v go &> /dev/null; then
    echo -e "${RED}Error: Go is not installed${NC}"
    echo "Please install Go 1.21 or later:"
    echo "  wget https://go.dev/dl/go1.21.5.linux-amd64.tar.gz"
    echo "  sudo tar -C /usr/local -xzf go1.21.5.linux-amd64.tar.gz"
    echo "  export PATH=\$PATH:/usr/local/go/bin"
    exit 1
fi

GO_VERSION=$(go version | awk '{print $3}')
echo -e "${GREEN}✓${NC} Go version: $GO_VERSION"

# Check if Go modules are installed
if [ ! -f "go.mod" ]; then
    echo -e "${YELLOW}Installing Go modules...${NC}"
    go mod tidy
fi

echo ""
echo -e "${BLUE}Starting test suite...${NC}"
echo ""

# Test 1: PostgreSQL Batch Simulator
echo -e "${YELLOW}=== Test 1: PostgreSQL Batch Simulator ===${NC}"
echo "Testing batch insert performance..."
go run postgres-batch-simulator.go \
    --batch-size $BATCH_SIZE \
    --num-batches 400 \
    --workers $WORKERS \
    --duration $TEST_DURATION

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ PostgreSQL test completed${NC}"
else
    echo -e "${RED}✗ PostgreSQL test failed${NC}"
    exit 1
fi
echo ""

# Test 2: Redis Streams Simulator
echo -e "${YELLOW}=== Test 2: Redis Streams Simulator ===${NC}"
echo "Testing Redis Streams performance..."
go run redis-streams-simulator.go \
    --tenants $NUM_TENANTS \
    --events-per-tenant 50000 \
    --batch-size $BATCH_SIZE \
    --duration $TEST_DURATION

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Redis Streams test completed${NC}"
else
    echo -e "${RED}✗ Redis Streams test failed${NC}"
    exit 1
fi
echo ""

# Test 3: Kafka Event Generator
echo -e "${YELLOW}=== Test 3: Kafka Event Generator ===${NC}"
echo "Testing Kafka CloudEvents performance..."
go run kafka-event-generator.go \
    --tenants $NUM_TENANTS \
    --events-per-tenant 80000 \
    --duration $TEST_DURATION \
    --batch-size $BATCH_SIZE \
    --compression snappy \
    --throughput $TARGET_THROUGHPUT

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Kafka test completed${NC}"
else
    echo -e "${RED}✗ Kafka test failed${NC}"
    exit 1
fi
echo ""

# Test 4: Integrated Load Tester
echo -e "${YELLOW}=== Test 4: Integrated Load Tester ===${NC}"
echo "Testing all components together..."
go run load-tester.go \
    --duration-minutes $TEST_DURATION \
    --target-events-per-sec $TARGET_THROUGHPUT \
    --num-tenants $NUM_TENANTS \
    --kafka-enabled \
    --redis-enabled \
    --postgres-enabled

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Integrated load test completed${NC}"
else
    echo -e "${RED}✗ Integrated load test failed${NC}"
    exit 1
fi
echo ""

# Summary
echo -e "${BLUE}=================================================${NC}"
echo -e "${GREEN}  All tests completed successfully!${NC}"
echo -e "${BLUE}=================================================${NC}"
echo ""
echo "Performance Targets:"
echo "  • PostgreSQL:  10,000+ inserts/sec"
echo "  • Redis:       50,000+ ops/sec"
echo "  • Kafka:       1M+ messages/sec"
echo "  • Overall:     $TARGET_THROUGHPUT events/sec (400k/min)"
echo ""
echo "Next steps:"
echo "  1. Review test output above for any warnings"
echo "  2. Check service health: docker compose -f dev/compose.yml ps"
echo "  3. Monitor performance: docker stats"
echo "  4. Run specific tests again if needed"
echo ""

# Optional: Check service health
echo -e "${YELLOW}Checking service health...${NC}"
docker compose -f dev/compose.yml ps | grep -E "(postgres|redis|kafka)" | while read line; do
    if echo "$line" | grep -q "healthy"; then
        echo -e "${GREEN}✓${NC} $line"
    else
        echo -e "${YELLOW}!${NC} $line"
    fi
done

echo ""
echo -e "${GREEN}Test suite complete!${NC}"
