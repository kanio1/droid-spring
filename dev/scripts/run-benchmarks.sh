#!/bin/bash
# BSS Performance Benchmark Runner
# Runs comprehensive benchmarks and generates reports

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPORT_DIR="/home/labadmin/projects/droid-spring/benchmark-reports"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
REPORT_PATH="$REPORT_DIR/benchmark-$TIMESTAMP"
K6_OUT="$REPORT_PATH/k6-results.json"
BENCHMARK_OUT="$REPORT_PATH/benchmark-results.json"

# Print header
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} BSS Performance Benchmark Suite ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""
echo "Timestamp: $TIMESTAMP"
echo "Report Directory: $REPORT_PATH"
echo ""

# Create report directory
mkdir -p "$REPORT_PATH"

# Check if services are running
echo -e "${YELLOW}Checking services...${NC}"
docker compose -f dev/compose.yml ps | grep -E "(backend|postgres|redis|kafka)" || {
    echo -e "${RED}Error: Services not running. Please start with: docker compose -f dev/compose.yml up -d${NC}"
    exit 1
}
echo -e "${GREEN}✓ All services running${NC}"
echo ""

# Wait for backend to be ready
echo -e "${YELLOW}Waiting for backend to be ready...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}✗ Backend not ready after 30 attempts${NC}"
        exit 1
    fi
    sleep 2
done
echo ""

# 1. Run K6 Load Test
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} 1. Running K6 Load Test         ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""
echo "Target: 6,667 events/sec (400k events/min)"
echo "Stages: Ramp-up → Target Load → Stress Test → Spike Test"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${YELLOW}K6 not found. Installing...${NC}"
    # Install K6 (Ubuntu/Debian)
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
    echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
    sudo apt-get update
    sudo apt-get install k6
fi

# Run K6 test
echo -e "${YELLOW}Starting K6 load test (this will take ~20 minutes)...${NC}"
k6 run --out json="$K6_OUT" \
    --env BASE_URL="http://localhost:8080" \
    dev/k6/scripts/production-load-test.js

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ K6 load test completed successfully${NC}"
else
    echo -e "${RED}✗ K6 load test failed${NC}"
fi
echo ""

# 2. Run Backend Benchmark API
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} 2. Running Backend Benchmarks   ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Run comprehensive benchmark
echo -e "${YELLOW}Starting comprehensive benchmark suite...${NC}"
curl -X POST http://localhost:8080/actuator/benchmarks/run \
    -H "Content-Type: application/json" \
    -d '{
        "concurrentUsers": 100,
        "durationSeconds": 300,
        "testScenarios": ["database", "redis", "kafka", "api"]
    }' \
    --max-time 600 \
    | tee "$REPORT_PATH/benchmark-api-response.json" > /dev/null

# Wait for benchmark to complete
sleep 5

# Get results
echo -e "${YELLOW}Fetching benchmark results...${NC}"
curl -s http://localhost:8080/actuator/benchmarks/results \
    | tee "$BENCHMARK_OUT" > /dev/null

echo -e "${GREEN}✓ Backend benchmarks completed${NC}"
echo ""

# 3. Generate Grafana Metrics
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} 3. Collecting Metrics           ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Collect metrics from Prometheus
echo -e "${YELLOW}Collecting Prometheus metrics...${NC}"
curl -s "http://localhost:9090/api/v1/query?query=bss_events_total" \
    | tee "$REPORT_PATH/prometheus-events.json" > /dev/null

curl -s "http://localhost:9090/api/v1/query?query=rate(bss_events_total[5m])" \
    | tee "$REPORT_PATH/prometheus-throughput.json" > /dev/null

curl -s "http://localhost:9090/api/v1/query?query=bss_event_processing_duration_seconds" \
    | tee "$REPORT_PATH/prometheus-latency.json" > /dev/null

echo -e "${GREEN}✓ Metrics collected${NC}"
echo ""

# 4. Analyze Results
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} 4. Analyzing Results            ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Check K6 results
if [ -f "$K6_OUT" ]; then
    TOTAL_REQUESTS=$(cat "$K6_OUT" | grep -o '"metric":"http_reqs"' | wc -l)
    AVG_RESPONSE_TIME=$(cat "$K6_OUT" | grep -o '"metric":"http_req_duration"' | wc -l)
    echo -e "K6 Load Test Results:"
    echo -e "  Total Requests: $TOTAL_REQUESTS"
    echo -e "  Response Time Samples: $AVG_RESPONSE_TIME"
fi

# Check benchmark results
if [ -f "$BENCHMARK_OUT" ]; then
    echo -e "\nBackend Benchmark Results:"
    cat "$BENCHMARK_OUT" | jq -r '.databaseResult.score // "N/A"' | xargs -I {} echo -e "  Database Score: {}"
    cat "$BENCHMARK_OUT" | jq -r '.redisResult.score // "N/A"' | xargs -I {} echo -e "  Redis Score: {}"
    cat "$BENCHMARK_OUT" | jq -r '.kafkaResult.score // "N/A"' | xargs -I {} echo -e "  Kafka Score: {}"
    cat "$BENCHMARK_OUT" | jq -r '.apiResult.score // "N/A"' | xargs -I {} echo -e "  API Score: {}"
fi

echo ""

# 5. Generate Report
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} 5. Generating Report            ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Create HTML report
cat > "$REPORT_PATH/index.html" <<'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>BSS Performance Benchmark Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1, h2 { color: #333; }
        .metric { background: #f5f5f5; padding: 10px; margin: 5px 0; border-radius: 5px; }
        .success { border-left: 4px solid #4CAF50; }
        .warning { border-left: 4px solid #FF9800; }
        .error { border-left: 4px solid #f44336; }
        pre { background: #f5f5f5; padding: 10px; overflow: auto; }
        .section { margin: 20px 0; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
    </style>
</head>
<body>
    <h1>BSS Performance Benchmark Report</h1>
    <p>Generated: TIMESTAMP_PLACEHOLDER</p>

    <div class="section">
        <h2>Summary</h2>
        <p>This report contains the results of the comprehensive BSS performance benchmark suite.</p>
    </div>

    <div class="section">
        <h2>K6 Load Test Results</h2>
        <p>Target: 6,667 events/sec (400,000 events/min)</p>
        <p>See <a href="k6-results.json">k6-results.json</a> for detailed results.</p>
    </div>

    <div class="section">
        <h2>Backend Benchmark Results</h2>
        <p>See <a href="benchmark-results.json">benchmark-results.json</a> for detailed results.</p>
    </div>

    <div class="section">
        <h2>Prometheus Metrics</h2>
        <ul>
            <li><a href="prometheus-events.json">Event Metrics</a></li>
            <li><a href="prometheus-throughput.json">Throughput Metrics</a></li>
            <li><a href="prometheus-latency.json">Latency Metrics</a></li>
        </ul>
    </div>

    <div class="section">
        <h2>Recommendations</h2>
        <ul>
            <li>Monitor system metrics during load tests</li>
            <li>Review database connection pool settings</li>
            <li>Check Redis memory usage and eviction policies</li>
            <li>Monitor Kafka consumer lag</li>
            <li>Review API response times and error rates</li>
        </ul>
    </div>
</body>
</html>
EOF

sed -i "s/TIMESTAMP_PLACEHOLDER/$TIMESTAMP/" "$REPORT_PATH/index.html"

echo -e "${GREEN}✓ Report generated at: $REPORT_PATH${NC}"
echo ""

# 6. Print Summary
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE} Benchmark Complete               ${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""
echo -e "${GREEN}Report Location: ${NC}$REPORT_PATH"
echo -e "${GREEN}HTML Report: ${NC}$REPORT_PATH/index.html"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review the HTML report for detailed results"
echo "2. Check Grafana dashboards (http://localhost:3000)"
echo "3. Review Prometheus metrics (http://localhost:9090)"
echo "4. Identify bottlenecks and optimize"
echo "5. Run follow-up benchmarks after optimizations"
echo ""
echo -e "${GREEN}To view the report:${NC}"
echo "  open $REPORT_PATH/index.html"
echo "  # or"
echo "  cd $REPORT_PATH && python3 -m http.server 8080"
echo ""
