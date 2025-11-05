# 🚀 BSS Testing Quick Start Guide

## Overview

This guide provides step-by-step instructions for implementing and running comprehensive tests for the BSS (Business Support System) application, covering scenarios from 100 to 1,000,000+ events across multiple Proxmox VMs.

---

## 📚 Table of Contents

1. [Quick Setup](#quick-setup)
2. [Testing Levels](#testing-levels)
3. [Load Testing Scripts](#load-testing-scripts)
4. [Distributed Testing](#distributed-testing)
5. [Kafka Event Simulation](#kafka-event-simulation)
6. [Performance Benchmarks](#performance-benchmarks)
7. [Monitoring & Observability](#monitoring--observability)

---

## 🎯 Quick Setup

### Prerequisites

```bash
# 1. Clone and setup project
git clone <repository>
cd droid-spring

# 2. Setup Proxmox VMs (recommended configuration)
# VM-101 to VM-105: Load Generators (8 vCPU, 16GB RAM each)
# VM-201 to VM-203: Backend (16 vCPU, 32GB RAM each)
# VM-301: Database (32 vCPU, 64GB RAM)
# VM-401: Kafka Cluster (16 vCPU, 32GB RAM)

# 3. Install dependencies
chmod +x dev/scripts/*.sh
```

### Initialize Test Environment

```bash
# Initialize distributed test infrastructure
./dev/scripts/distributed-test-orchestrator.sh init

# List configured VMs
./dev/scripts/distributed-test-orchestrator.sh list

# Check VM connectivity
./dev/scripts/distributed-test-orchestrator.sh status
```

---

## 📊 Testing Levels

### Level 1: Unit Tests (Local/CI)

```bash
# Backend unit tests (333 Java files)
cd backend
mvn test

# Frontend unit tests
cd frontend
npm run test:unit

# Contract tests (Pact)
mvn test -Dtest=*PactTest
```

### Level 2: Integration Tests (Testcontainers)

```bash
# Full integration test suite
mvn verify

# With specific profiles
mvn verify -Dspring.profiles.active=test
```

### Level 3: E2E Tests (Playwright)

```bash
# Frontend E2E tests
cd frontend
npm run test:e2e

# With specific browser
npx playwright test --project=chromium

# Accessibility tests
npm run test:a11y
```

### Level 4: Performance Tests (K6, JMeter)

```bash
# Smoke test (100 users, 5 minutes)
./dev/scripts/load-generator-simulator.sh smoke

# Average load (1K users, 30 minutes)
./dev/scripts/load-generator-simulator.sh average

# Peak load (10K users, 60 minutes)
./dev/scripts/load-generator-simulator.sh peak

# Stress test (50K users, 2 hours)
./dev/scripts/load-generator-simulator.sh stress

# Extreme test (100K users, 4 hours)
./dev/scripts/load-generator-simulator.sh extreme

# Marathon test (10K users, 24 hours)
./dev/scripts/load-generator-simulator.sh marathon

# Custom scenario
./dev/scripts/load-generator-simulator.sh custom 50000 120
```

### Level 5: Chaos Engineering

```bash
# Run chaos tests
mvn test -Dtest=ChaosTest -Dspring.profiles.active=chaos-test

# Network chaos
./dev/scripts/chaos/network-latency.sh

# Database failure simulation
# (See TESTING-STRATEGY-MASTERPLAN.md for details)
```

### Level 6: Distributed Testing (Multi-VM)

```bash
# Distributed smoke test (500 users total across 5 VMs)
./dev/scripts/distributed-test-orchestrator.sh test smoke

# Distributed average load (5K users total)
./dev/scripts/distributed-test-orchestrator.sh test average

# Distributed peak load (50K users total)
./dev/scripts/distributed-test-orchestrator.sh test peak

# Distributed stress test (100K users total)
./dev/scripts/distributed-test-orchestrator.sh test stress

# Distributed extreme test (500K users total)
./dev/scripts/distributed-test-orchestrator.sh test extreme

# Custom distributed test
./dev/scripts/distributed-test-orchestrator.sh test stress --duration 120
```

---

## ⚡ Load Testing Scripts

### Load Generator Simulator

**Location**: `dev/scripts/load-generator-simulator.sh`

**Features**:
- Multiple test scenarios (smoke, average, peak, stress, extreme, marathon)
- Custom user load and duration
- Configurable ramp-up patterns
- Real-time progress monitoring
- Automatic HTML report generation

**Usage Examples**:

```bash
# Basic scenarios
./load-generator-simulator.sh smoke
./load-generator-simulator.sh average
./load-generator-simulator.sh peak

# With custom parameters
./load-generator-simulator.sh peak --target https://staging.bss.local --duration 120

# Custom load
./load-generator-simulator.sh custom 75000 180 --ramp-up 30

# Run on specific Proxmox VM
./load-generator-simulator.sh extreme --vm 101

# Verbose output
./load-generator-simulator.sh stress --verbose
```

**Generated Outputs**:
- JSON results: `/var/log/bss-load-tests/results-*.json`
- HTML reports: `/var/log/bss-load-tests/report-*.html`
- K6 script: `/var/log/bss-load-tests/test-scenario.js`

### Distributed Test Orchestrator

**Location**: `dev/scripts/distributed-test-orchestrator.sh`

**Features**:
- Coordinates tests across multiple Proxmox VMs
- Automatic load distribution
- Consolidated reporting
- Parallel execution
- Real-time monitoring

**Usage Examples**:

```bash
# Initialize
./distributed-test-orchestrator.sh init

# List VMs
./distributed-test-orchestrator.sh list

# Check VM status
./distributed-test-orchestrator.sh status

# Run distributed tests
./distributed-test-orchestrator.sh test average
./distributed-test-orchestrator.sh test peak --duration 120
./distributed-test-orchestrator.sh test stress

# Generate consolidated report
./distributed-test-orchestrator.sh report

# Cleanup
./distributed-test-orchestrator.sh cleanup
```

**Output Structure**:
```
/var/log/bss-test-results/
└── distributed-peak-20251105_143022/
    ├── consolidated-report.html
    ├── vm-101/
    │   └── vm-101.log
    ├── vm-102/
    │   └── vm-102.log
    └── ...
```

---

## 📨 Kafka Event Simulation

### Kafka Event Simulator

**Location**: `dev/scripts/kafka-event-simulator.sh`

**Features**:
- Generate up to 1M+ events
- CloudEvents 1.0 compliant format
- Batch optimization for maximum throughput
- Event storm generation
- DLQ testing
- Latency and throughput measurements

**Usage Examples**:

```bash
# Prepare Kafka environment (create topics)
./kafka-event-simulator.sh prepare

# Generate 100K events
./kafka-event-simulator.sh generate 100000

# Generate 1M events with batching
./kafka-event-simulator.sh massive 1000000

# Event storm at 50K events/sec for 10 minutes
./kafka-event-simulator.sh storm 50000 --duration 600

# Throughput test at 10K events/sec
./kafka-event-simulator.sh throughput --rate 10000 --duration 300

# Consume events from topic
./kafka-event-simulator.sh consume bss.customer.events

# Test Dead Letter Queue
./kafka-event-simulator.sh dlq-test

# Cleanup test topics
./kafka-event-simulator.sh cleanup
```

**Event Formats**:

```json
{
  "specversion": "1.0",
  "type": "com.bss.customer.created",
  "source": "/api/customers",
  "id": "evt-1701859200000-12345",
  "time": "2024-11-05T14:30:00.000Z",
  "subject": "customer/12345",
  "data": {
    "customerId": "12345",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

---

## 📊 Performance Benchmarks

### Target Metrics

| Metric | Target | Excellent |
|--------|--------|-----------|
| **Response Time P50** | < 200ms | < 100ms |
| **Response Time P95** | < 500ms | < 300ms |
| **Response Time P99** | < 1000ms | < 500ms |
| **Throughput (Read)** | 50K req/s | 100K req/s |
| **Throughput (Write)** | 10K req/s | 25K req/s |
| **Error Rate** | < 0.1% | < 0.01% |
| **Cache Hit Rate** | > 90% | > 95% |
| **Kafka Lag** | < 1000 msgs | < 100 msgs |
| **DB Query Time** | < 100ms | < 50ms |

### Load Test Scenarios

```
Scenario 1: Smoke Test
├─ Users: 100
├─ Duration: 5 minutes
├─ Ramp-up: 1 minute
└─ Purpose: Verify basic functionality

Scenario 2: Average Load
├─ Users: 1,000
├─ Duration: 30 minutes
├─ Ramp-up: 5 minutes
└─ Purpose: Baseline performance

Scenario 3: Peak Load
├─ Users: 10,000
├─ Duration: 60 minutes
├─ Ramp-up: 10 minutes
└─ Purpose: Production-like traffic

Scenario 4: Stress Test
├─ Users: 50,000
├─ Duration: 2 hours
├─ Ramp-up: 20 minutes
└─ Purpose: Find breaking point

Scenario 5: Extreme Test
├─ Users: 100,000
├─ Duration: 4 hours
├─ Ramp-up: 30 minutes
└─ Purpose: System limits

Scenario 6: Marathon Test
├─ Users: 10,000
├─ Duration: 24 hours
├─ Ramp-up: 60 minutes
└─ Purpose: Stability & memory leaks
```

### Distributed Test Scenarios

```
Scenario 1: Distributed Smoke
├─ VMs: 5 (101-105)
├─ Users per VM: 100
├─ Total: 500 users
└─ Regions: US-East, US-West, EU, APAC

Scenario 2: Distributed Average
├─ VMs: 5
├─ Users per VM: 1,000
├─ Total: 5,000 users
└─ Load balanced across regions

Scenario 3: Distributed Peak
├─ VMs: 5
├─ Users per VM: 10,000
├─ Total: 50,000 users
└─ Real-world traffic simulation

Scenario 4: Distributed Stress
├─ VMs: 5
├─ Users per VM: 20,000
├─ Total: 100,000 users
└─ Multi-region stress test

Scenario 5: Distributed Extreme
├─ VMs: 5
├─ Users per VM: 100,000
├─ Total: 500,000 users
└─ Maximum capacity test
```

---

## 📈 Monitoring & Observability

### Grafana Dashboards

**Access**: https://grafana.bss.local

**Key Dashboards**:
- Load Test Overview
- Application Performance
- Database Metrics
- Kafka Monitoring
- Redis Cache Stats

### Prometheus Metrics

**Access**: https://prometheus.bss.local

**Key Metrics**:
```
http_requests_total
http_request_duration_seconds
jvm_memory_used_bytes
kafka_consumer_lag_sum
redis_keyspace_hits_total
pg_stat_database_numbackends
```

### Jaeger Tracing

**Access**: https://jaeger.bss.local

**Usage**:
- Trace customer creation flow
- Analyze request propagation
- Identify bottlenecks
- Debug latency issues

### AKHQ (Kafka UI)

**Access**: https://akhq.bss.local

**Features**:
- Topic browser
- Consumer group monitoring
- Message inspection
- Partition analysis

---

## 🎓 Best Practices

### 1. Test Environment

```bash
# Always use production-like data volumes
docker volume create bss-test-data

# Use dedicated test databases
export POSTGRES_DB=bss_test

# Enable verbose logging
export LOG_LEVEL=DEBUG
```

### 2. Test Data

```bash
# Generate realistic test data
./scripts/generate-test-data.sh --customers 100000 --orders 500000

# Use factories for consistent data
npm run test:generate-data
```

### 3. Performance Testing

```bash
# Warm up JVM before measurements
./scripts/warmup-jvm.sh

# Run multiple iterations for consistency
for i in {1..5}; do
    ./load-generator-simulator.sh peak
    sleep 300
done

# Monitor system resources
htop
iotop
nethogs
```

### 4. CI/CD Integration

```yaml
# .github/workflows/load-test.yml
name: Load Tests
on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Load Tests
        run: |
          chmod +x dev/scripts/*.sh
          ./dev/scripts/load-generator-simulator.sh average
```

### 5. Results Analysis

```bash
# Compare multiple test runs
./scripts/compare-results.sh results-1.json results-2.json

# Generate trend report
./scripts/trend-report.sh --days 30

# Export to CSV
jq -r '.metrics | to_entries[] | [.key, .value] | @csv' results.json > metrics.csv
```

---

## 📝 Test Reports

### Automated Report Generation

All test scripts generate HTML reports automatically:

```
/var/log/bss-load-tests/
├── results-average-20251105_143022.json
├── report-average-20251105_143022.html
└── test-scenario.js

/var/log/bss-test-results/
└── distributed-peak-20251105_143022/
    ├── consolidated-report.html
    ├── vm-101-results/
    ├── vm-102-results/
    └── ...
```

### Report Sections

1. **Test Configuration**
   - Virtual users
   - Duration
   - Ramp-up pattern
   - Target URL

2. **Performance Metrics**
   - Total requests
   - Request rate
   - Response time (P50, P95, P99)
   - Error rate

3. **Resource Utilization**
   - CPU usage
   - Memory usage
   - Network I/O
   - Disk I/O

4. **Infrastructure Metrics**
   - Database connections
   - Kafka lag
   - Cache hit rate
   - JVM heap

5. **Recommendations**
   - Performance tuning suggestions
   - Infrastructure scaling advice
   - Code optimization opportunities

---

## 🔧 Troubleshooting

### Common Issues

**Issue**: High response times during load test
```
Solution:
1. Check database connection pool settings
2. Verify Redis cache is working
3. Analyze slow queries with PgHero
4. Review JVM heap size
```

**Issue**: Kafka consumer lag
```
Solution:
1. Increase number of consumer instances
2. Optimize event processing logic
3. Check partition distribution
4. Verify network throughput
```

**Issue**: Load generator VM saturation
```
Solution:
1. Reduce users per VM
2. Add more load generator VMs
3. Optimize K6 script
4. Increase VM resources
```

**Issue**: Test failures in CI
```
Solution:
1. Increase test timeouts
2. Use @Disabled for flaky tests
3. Increase Testcontainers startup timeout
4. Check external service availability
```

### Debugging Commands

```bash
# Check VM resource usage
ssh root@vm-101 "htop"
ssh root@vm-201 "docker stats"

# Verify network connectivity
ping -c 100 api.bss.local
traceroute api.bss.local

# Check service health
curl -f https://api.bss.local/actuator/health
curl -f https://grafana.bss.local/api/health

# Monitor Kafka lag
docker exec bss-kafka-1 kafka-consumer-groups \
  --describe --group test-consumer-group

# Check Redis
docker exec bss-redis redis-cli info stats
```

---

## 📞 Support & Documentation

### Key Documentation

- **TESTING-STRATEGY-MASTERPLAN.md** - Comprehensive testing strategy
- **AGENTS.md** - Development guidelines
- **PHASE-*.md** - Infrastructure documentation (dev/)

### Getting Help

- **GitHub Issues**: <repository>/issues
- **Slack**: #bss-testing
- **Email**: bss-dev@example.com

### Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/new-test`)
3. Commit changes (`git commit -am 'Add new test'`)
4. Push to branch (`git push origin feature/new-test`)
5. Create Pull Request

---

## 🎯 Next Steps

1. ✅ Review TESTING-STRATEGY-MASTERPLAN.md
2. ✅ Setup Proxmox infrastructure
3. ✅ Run initial smoke tests
4. ✅ Setup CI/CD pipeline
5. ✅ Schedule regular performance tests
6. ✅ Configure alerts and monitoring
7. ✅ Document findings and optimize

---

**Happy Testing! 🚀**

For questions or issues, please refer to the documentation or contact the development team.
