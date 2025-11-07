# Performance Testing Suite

This directory contains a comprehensive **Performance Testing Suite** using [k6](https://k6.io/) to test the BSS application's performance, load handling, and stability characteristics.

## Overview

The performance testing suite includes four types of tests:

1. **Load Testing** - Normal and peak load conditions
2. **Stress Testing** - Breaking points and extreme load
3. **Spike Testing** - Sudden traffic bursts
4. **Soak Testing** - Long-term stability and memory leaks

## Prerequisites

### Install k6

```bash
# macOS (Homebrew)
brew install k6

# Windows (Chocolatey)
choco install k6

# Linux (APT)
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Or using Docker
docker pull grafana/k6
```

### Environment Setup

```bash
# Set base URL
export BASE_URL=http://localhost:3000

# Or create .env file
echo "BASE_URL=http://localhost:3000" > .env
```

## Test Types

### 1. Load Testing (`load-tests.js`)

**Purpose:** Verify system performance under normal and peak load conditions

**Configuration:**
- Duration: ~9 minutes
- Virtual Users: 0 → 25 → 50 → 0
- Ramp-up: 2 minutes
- Peak load: 5 minutes
- Ramp-down: 2 minutes

**Thresholds:**
- 95% of requests under 2 seconds
- Error rate under 5%

**What it tests:**
- Customer list loading
- Order operations
- Invoice operations
- Payment operations
- Subscription operations
- Mixed user workflows
- Concurrent user simulation

**Run:**
```bash
k6 run load-tests.js
```

**Expected output:**
- Response time metrics
- Error rates
- Throughput measurements
- Success rates

---

### 2. Stress Testing (`stress-tests.js`)

**Purpose:** Find breaking points and verify system behavior under extreme load

**Configuration:**
- Duration: ~5 minutes
- Virtual Users: 0 → 50 → 100 → 200 → 200 → 0
- Quick ramp-up to breaking point
- More relaxed thresholds

**Thresholds:**
- 95% of requests under 5 seconds
- Error rate up to 20% (stress test)

**What it tests:**
- Rapid requests
- Database operations under stress
- Concurrent API calls
- Search operations
- Authentication stress
- Error scenarios
- Long-running operations
- Mixed workflows

**Run:**
```bash
k6 run stress-tests.js
```

**Expected output:**
- Breaking point identification
- Performance degradation patterns
- Error accumulation rates
- System limits

---

### 3. Spike Testing (`spike-tests.js`)

**Purpose:** Verify system behavior with sudden bursts of traffic

**Configuration:**
- Duration: ~4 minutes
- Multiple spike patterns:
  - Normal (10 VUs) → Spike (200 VUs) → Normal → Spike again
- Quick transitions to simulate traffic spikes

**What it tests:**
- Customer operations during spikes
- Order operations during spikes
- Invoice operations during spikes
- Payment operations during spikes
- Subscription operations during spikes
- Dashboard access during spikes
- Burst operations

**Run:**
```bash
k6 run spike-tests.js
```

**Expected output:**
- Spike response patterns
- Recovery times
- System behavior during bursts
- Degradation analysis

---

### 4. Soak Testing (`soak-tests.js`)

**Purpose:** Verify long-term stability and detect memory leaks

**Configuration:**
- Duration: 30 minutes
- Virtual Users: 20 (constant load)
- Extended runtime for leak detection

**Thresholds:**
- 95% of requests under 3 seconds
- Error rate under 5%

**What it tests:**
- Continuous customer operations
- Continuous order operations
- Continuous invoice operations
- Continuous payment operations
- Continuous subscription operations
- Mixed operation cycles
- Repeated login sessions
- Data access patterns

**Run:**
```bash
k6 run soak-tests.js
```

**Expected output:**
- Long-term stability metrics
- Memory leak detection
- Performance degradation over time
- Resource consumption patterns

---

## Running Tests

### Run All Performance Tests

```bash
#!/bin/bash
# performance-tests.sh

echo "Starting Performance Testing Suite..."
echo ""

echo "1. Load Test (9 min)..."
k6 run load-tests.js
echo ""

echo "2. Stress Test (5 min)..."
k6 run stress-tests.js
echo ""

echo "3. Spike Test (4 min)..."
k6 run spike-tests.js
echo ""

echo "4. Soak Test (30 min)..."
k6 run soak-tests.js
echo ""

echo "All performance tests completed!"
```

### Run Specific Test

```bash
# Load test only
k6 run load-tests.js

# Stress test only
k6 run stress-tests.js

# Spike test only
k6 run spike-tests.js

# Soak test only
k6 run soak-tests.js
```

### Run with Custom Options

```bash
# Run with virtual users parameter
k6 run -o 100 load-tests.js

# Run with environment variables
BASE_URL=https://staging.example.com k6 run load-tests.js

# Run with output to file
k6 run -o results.json load-tests.js

# Run in development mode (shorter)
k6 run -e DEV_MODE=true load-tests.js
```

### Run with Docker

```bash
# Load test
docker run --rm -i grafana/k6 run - < load-tests.js

# With environment variables
docker run --rm -i -e BASE_URL=http://host.docker.internal:3000 grafana/k6 run - < load-tests.js

# Mount results directory
docker run --rm -i -v $(pwd)/results:/grafana/k6/results grafana/k6 run -o /grafana/k6/results/ load-tests.js
```

## Test Results

### Standard Output

Each test will output:
- Stage progression
- Request metrics
- Response times
- Error rates
- Custom metrics
- Summary statistics

### Export Results

```bash
# JSON format
k6 run -o results.json load-tests.js

# CSV format
k6 run -o results.csv load-tests.js

# Both
k6 run -o results.json -o results.csv load-tests.js
```

### HTML Report

```bash
# Install k6 report
npm install -g k6-reporter

# Generate HTML report
k6 run load-tests.js | k6-reporter --output performance-report.html
```

### Cloud Results (k6 Cloud)

```bash
# Login to k6 Cloud
k6 login cloud

# Run and upload to cloud
k6 cloud load-tests.js
```

## Customization

### Modify Virtual Users

Edit test file:
```javascript
export const options = {
  stages: [
    { duration: '2m', target: 100 }, // Change to 100 VUs
    { duration: '5m', target: 100 },
    { duration: '2m', target: 0 },
  ],
}
```

### Modify Test Duration

```javascript
export const options = {
  stages: [
    { duration: '5m', target: 50 }, // 5 minutes at peak
    { duration: '10m', target: 50 }, // Extended duration
  ],
}
```

### Add Custom Metrics

```javascript
import { Trend } from 'k6/metrics'

const customMetric = new Trend('custom_operation_time')

// In test
const start = new Date()
await doOperation()
const end = new Date()
customMetric.add(end - start)
```

### Modify Thresholds

```javascript
export const options = {
  thresholds: {
    http_req_duration: ['p(95)<1000'], // Stricter: 1 second
    http_req_failed: ['rate<0.01'],     // Stricter: 1% error rate
  },
}
```

## Performance Baselines

### Expected Performance Metrics

| Test Type | Target Response Time | Acceptable Error Rate |
|-----------|---------------------|----------------------|
| **Load** | < 2 seconds (95%) | < 5% |
| **Stress** | < 5 seconds (95%) | < 20% |
| **Spike** | < 3 seconds (95%) | < 10% |
| **Soak** | < 3 seconds (95%) | < 5% |

### Acceptable Thresholds

**Load Test:**
- ✅ 95% of requests < 2s
- ✅ Error rate < 5%
- ✅ System stable at 50 VUs

**Stress Test:**
- ✅ System doesn't crash
- ✅ Graceful degradation
- ✅ Recovery after load decreases

**Spike Test:**
- ✅ System handles sudden spikes
- ✅ Recovery within 1 minute
- ✅ No data corruption

**Soak Test:**
- ✅ No memory leaks
- ✅ Stable performance over time
- ✅ No degradation

## Test Scenarios

### Scenario 1: Normal Day Operations
- 50 concurrent users
- Mix of all operations
- 2-3 requests per user per minute
- **Test:** Load test

### Scenario 2: Peak Hour
- 200 concurrent users
- High activity
- 5-10 requests per user per minute
- **Test:** Stress test

### Scenario 3: Marketing Campaign
- Sudden traffic burst
- 10 → 200 users in 10 seconds
- **Test:** Spike test

### Scenario 4: Long-Running System
- 20 users continuously
- 30 minutes runtime
- Monitor for leaks
- **Test:** Soak test

## CI/CD Integration

### GitHub Actions

```yaml
name: Performance Tests

on:
  schedule:
    - cron: '0 3 * * *' # Nightly at 3 AM
  workflow_dispatch:

jobs:
  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install k6
        run: |
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6

      - name: Run load test
        run: k6 run tests/performance/load-tests.js
        env:
          BASE_URL: ${{ secrets.STAGING_URL }}

      - name: Run stress test
        run: k6 run tests/performance/stress-tests.js
        env:
          BASE_URL: ${{ secrets.STAGING_URL }}

      - name: Upload results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: performance-results
          path: results/
```

### Jenkins

```groovy
pipeline {
    agent any
    stages {
        stage('Install k6') {
            steps {
                sh 'curl https://github.com/grafana/k6/releases/download/v0.46.0/k6-v0.46.0-linux-amd64.tar.gz -L | tar xvz'
                sh 'sudo mv k6-v0.46.0-linux-amd64/k6 /usr/local/bin'
            }
        }
        stage('Performance Tests') {
            steps {
                sh 'k6 run tests/performance/load-tests.js'
            }
        }
    }
}
```

## Troubleshooting

### Test Hangs or Times Out

**Issue:** Test never completes

**Solution:**
1. Check BASE_URL is correct
2. Verify application is running
3. Add timeouts to requests
4. Reduce test duration

### High Error Rates

**Issue:** Error rate > threshold

**Solution:**
1. Check application health
2. Review application logs
3. Reduce virtual users
4. Check database performance

### Memory Issues

**Issue:** k6 runs out of memory

**Solution:**
1. Reduce virtual users
2. Use Docker with more memory
3. Run tests on server
4. Monitor system resources

### Inconsistent Results

**Issue:** Results vary between runs

**Solution:**
1. Run tests multiple times
2. Check for external factors
3. Use consistent environment
4. Increase test duration

## Best Practices

### 1. Test Environment
- Use isolated environment
- Consistent network conditions
- Stable database
- No other load on system

### 2. Test Data
- Use realistic test data
- Consistent user accounts
- Avoid polluting production
- Clean up after tests

### 3. Thresholds
- Set realistic thresholds
- Based on business requirements
- Monitor trends over time
- Adjust as needed

### 4. Monitoring
- Monitor system resources
- Application metrics
- Database performance
- Network latency

### 5. Documentation
- Document baseline metrics
- Track changes over time
- Note test conditions
- Report findings

## Performance Metrics Explained

### http_req_duration
Time from request start to response end (all stages)

**Measurement:** 95th percentile (p95)

### http_req_failed
Percentage of failed requests

**Failed includes:** Timeouts, network errors, 5xx responses

### Custom Metrics

**Rate:** Success/failure ratio
**Trend:** Statistical distribution
**Counter:** Total count

## Interpreting Results

### Good Performance
- ✅ Low response times
- ✅ Low error rates
- ✅ Stable metrics
- ✅ Quick recovery

### Performance Issues
- ❌ High response times
- ❌ High error rates
- ❌ Metric degradation
- ❌ Slow recovery

### Action Items
1. Identify bottlenecks
2. Optimize code
3. Scale resources
4. Improve architecture
5. Re-test

## Resources

- [k6 Documentation](https://k6.io/docs/)
- [k6 Best Practices](https://k6.io/docs/testing-guides/load-testing/)
- [Performance Testing Guide](https://k6.io/docs/testing-guides/)
- [k6 Cloud](https://k6.io/cloud/)

## Support

For issues:
- Check k6 documentation
- Review test logs
- Check system resources
- Validate environment setup

---

**Last Updated:** 2025-11-06
**Test Types:** Load, Stress, Spike, Soak
**Tool:** k6
**Duration:** 5-30 minutes per test
