# Load Testing Framework

This directory contains load testing scripts for the BSS Backend API using [k6](https://k6.io/).

## Overview

The load testing framework is designed to validate the performance and scalability of the BSS backend under various load conditions.

## Prerequisites

1. **Install k6**
   ```bash
   # On macOS
   brew install k6

   # On Ubuntu/Debian
   sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update
   sudo apt-get install k6

   # On Windows (using Chocolatey)
   choco install k6
   ```

2. **Start the BSS Backend**
   ```bash
   # Using Maven
   mvn spring-boot:run

   # Or build and run
   mvn clean package
   java -jar target/bss-backend-0.0.1-SNAPSHOT.jar
   ```

## Available Test Scenarios

### 1. Customer API Tests (`customers-api.js`)

Tests the customer management endpoints:
- Create Customer
- Get Customer by ID
- List Customers (paginated)
- Search Customers
- Update Customer
- Delete Customer

**Run:**
```bash
k6 run customers-api.js
```

**Expected Results:**
- 95% of requests < 500ms
- Error rate < 5%
- All CRUD operations successful

### 2. Invoice API Tests (`invoices-api.js`)

Tests the invoice management endpoints:
- Create Invoice
- Get Invoice by ID
- List Invoices (paginated)
- Search Invoices
- Change Invoice Status

**Run:**
```bash
k6 run invoices-api.js
```

**Expected Results:**
- 95% of requests < 500ms
- Error rate < 5%
- Invoice operations working correctly

## Test Configuration

Each test uses a three-stage load profile:

1. **Ramp Up** (2 minutes): Gradually increases load from 1 to target users
2. **Sustained Load** (5 minutes): Maintains target load to test stability
3. **Ramp Down** (2 minutes): Gradually decreases load to 0

### Custom Load Profiles

You can customize the load profile by modifying the `options.stages` section:

```javascript
export const options = {
    stages: [
        { duration: '1m', target: 10 },   // Ramp up to 10 users in 1 minute
        { duration: '3m', target: 10 },   // Stay at 10 users for 3 minutes
        { duration: '1m', target: 50 },   // Ramp up to 50 users in 1 minute
        { duration: '5m', target: 50 },   // Stay at 50 users for 5 minutes
        { duration: '2m', target: 0 },    // Ramp down to 0 users
    ],
};
```

## Running Tests with Custom Configuration

### Test with Different Base URL
```bash
BASE_URL=http://localhost:8080 k6 run customers-api.js
```

### Test with Different Duration
```bash
k6 run --duration 10m customers-api.js
```

### Run All Tests Sequentially
```bash
#!/bin/bash
echo "Running Customer API Load Tests..."
k6 run customers-api.js

echo "Running Invoice API Load Tests..."
k6 run invoices-api.js

echo "All load tests completed!"
```

## Performance Benchmarks

### Target Metrics

| Operation | Target Response Time (P95) | Max Error Rate |
|-----------|---------------------------|----------------|
| Create Customer | < 500ms | < 5% |
| Get Customer | < 200ms | < 1% |
| List Customers | < 300ms | < 2% |
| Search Customers | < 400ms | < 3% |
| Create Invoice | < 500ms | < 5% |
| Get Invoice | < 200ms | < 1% |
| List Invoices | < 300ms | < 2% |
| Search Invoices | < 400ms | < 3% |
| Change Status | < 300ms | < 2% |

### Success Criteria

A test is considered successful if:
1. All threshold checks pass
2. No error rates exceed 5%
3. Response times meet the target SLAs
4. No system crashes or timeouts

## Test Results Interpretation

k6 provides detailed metrics after each test run:

```
     ✓ http_req_duration..............: avg=245ms min=120ms med=230ms max=890ms p(95)=480ms p(99)=650ms
     ✗ http_req_failed................: 3.45%  ✓ matches desired 5% rate
     checks.........................: 95.23% ✓ 1048/1100 checks passed
     data_received.................: 1.2 MB 1038 kB/s
     data_sent.....................: 890 kB 784 B/s
     http_req_blocked..............: avg=15ms  min=0ms     med=5ms   max=450ms p(95)=50ms
     http_req_connecting...........: avg=12ms  min=0ms     med=8ms   max=400ms p(95)=45ms
```

Key metrics to monitor:
- `http_req_duration`: Request duration (95th percentile should be < 500ms)
- `http_req_failed`: Error rate (should be < 5%)
- `checks`: Percentage of successful checks (should be > 95%)

## Best Practices

1. **Warm Up**: Always run a short test first to warm up the JVM and cache
2. **Baseline**: Establish performance baseline before making changes
3. **Monitor Resources**: Monitor CPU, memory, and database performance during tests
4. **Isolate Tests**: Run tests in isolated environment to avoid interference
5. **Iterate**: Use test results to identify and fix performance bottlenecks

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure backend is running on the correct port
   - Check BASE_URL environment variable

2. **High Error Rate**
   - Check backend logs for errors
   - Verify database connectivity
   - Check system resources (CPU, memory)

3. **Slow Response Times**
   - Check database query performance
   - Verify cache is working (Redis)
   - Monitor system resources

### Debug Mode

Run tests in debug mode for verbose output:

```bash
k6 run --debug customers-api.js
```

## Adding New Test Scenarios

To add a new test scenario:

1. Create a new `.js` file in this directory
2. Import necessary k6 modules
3. Define test data and options
4. Implement test logic with `check()` assertions
5. Add documentation to this README

Example template:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 0 },
    ],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    // Your test logic here
}
```

## Integration with CI/CD

You can integrate k6 tests into your CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run Load Tests
  run: |
    k6 run customers-api.js
    k6 run invoices-api.js
```

## References

- [k6 Documentation](https://k6.io/docs/)
- [k6 API Reference](https://k6.io/docs/javascript-api/)
- [Load Testing Best Practices](https://k6.io/docs/testing-guides/load-testing/)

---

**Note**: These tests are designed for non-production environments. Always test in a staging environment before production deployments.
