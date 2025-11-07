# Phase 6.3 - Load Testing Implementation Report

## Overview

This document details the implementation of comprehensive load testing using K6 for the BSS application. All K6 test scripts have been updated to use the standardized `/api/v1/` endpoint paths, ensuring consistency with the backend API structure.

## Test Suite Structure

### 1. Load Tests (`frontend/tests/performance/load-tests.js`)

**Purpose:** Verify system performance under normal and peak load conditions

**Configuration:**
- **Virtual Users (VUs):** 25-50 VUs
- **Duration:** 9 minutes
  - 2 min: Ramp-up to 25 VUs
  - 5 min: Sustain at 50 VUs (peak)
  - 2 min: Ramp-down to 0 VUs

**Performance Thresholds:**
- 95% of requests must complete within 2000ms
- Error rate must be below 5%
- Custom error rate below 10%

**Test Scenarios:**
1. **Customer Operations:**
   - Login authentication
   - List customers with pagination
   - Retrieve customer details
   - Search customers
   - Create new customers

2. **Order Operations:**
   - List orders

3. **Invoice Operations:**
   - List invoices

4. **Payment Operations:**
   - List payments

5. **Subscription Operations:**
   - List subscriptions

6. **Mixed Operations:**
   - Simulate complete user workflows
   - Dashboard access
   - Navigation between modules

7. **Concurrent Users:**
   - Random operation selection
   - Parallel request simulation

**Custom Metrics:**
- `errors`: Error rate tracking
- `login_success`: Authentication success rate
- `page_load_time`: Page rendering performance
- `api_response_time`: API latency tracking
- `request_count`: Total request volume

### 2. Spike Tests (`frontend/tests/performance/spike-tests.js`)

**Purpose:** Test system behavior with sudden traffic bursts

**Configuration:**
- **Virtual Users:** 10 → 200 (sudden spike)
- **Duration:** ~5 minutes
- **Pattern:** 3 spike cycles

**Test Stages:**
1. 1 min: Baseline (10 VUs)
2. 10s: Spike to 200 VUs
3. 30s: Sustain at 200 VUs
4. 1 min: Drop to 10 VUs
5. (Repeat pattern 2 more times)

**Test Scenarios:**
1. **Customer Operations During Spike**
2. **Order Operations During Spike**
3. **Invoice Operations During Spike**
4. **Payment Operations During Spike**
5. **Subscription Operations During Spike**
6. **Dashboard Access During Spike**
7. **Burst Operations**
   - Rapid sequence: customers → orders → invoices

**Custom Metrics:**
- `spike_detected`: Rate of failures during spikes
- `recovery_rate`: Success rate post-spike
- `spike_response_time`: Response time during spikes
- `spike_request_count`: Requests during spike periods

**Expected Behavior:**
- System should handle sudden traffic increases
- Graceful degradation acceptable (status < 500)
- Quick recovery to normal operation
- Minimal data loss or corruption

### 3. Soak Tests (`frontend/tests/performance/soak-tests.js`)

**Purpose:** Detect memory leaks and stability issues over extended periods

**Configuration:**
- **Virtual Users:** 20 VUs
- **Duration:** 30 minutes
  - 5 min: Warm-up period
  - 20 min: Sustained load (soak)
  - 5 min: Cool-down

**Performance Thresholds:**
- 95% of requests must complete within 3000ms
- Error rate must be below 5%

**Test Scenarios:**
1. **Continuous Customer Operations**
   - List customers
   - Search operations
   - Pagination

2. **Continuous Order Operations**

3. **Continuous Invoice Operations**

4. **Continuous Payment Operations**

5. **Continuous Subscription Operations**

6. **Mixed Operations Cycle**
   - Complete workflow: dashboard → customers → orders → invoices → payments

7. **Repeated Login Sessions**
   - 2 logins per cycle
   - Session management testing

8. **Data Access Patterns**
   - List customers
   - Get specific customer
   - List orders

**Custom Metrics:**
- `potential_memory_leak`: Stability detection
- `system_stability`: Overall stability trend
- `request_count`: Total request tracking
- `successful_requests`: Success counter
- `failed_requests`: Failure counter
- `stability_score`: Calculated stability percentage

**Expected Behavior:**
- Consistent response times over time
- No significant memory leaks
- Stable error rates
- Resource consumption patterns should be predictable

### 4. Stress Tests (`frontend/tests/performance/stress-tests.js`)

**Purpose:** Find system breaking points and limits

**Configuration:**
- **Virtual Users:** 0 → 200 VUs
- **Duration:** ~5 minutes
- **Thresholds:** Relaxed (p95 < 5000ms, error rate < 20%)

**Test Stages:**
1. 30s: Ramp to 50 VUs
2. 1 min: Ramp to 100 VUs
3. 2 min: Ramp to 200 VUs (peak stress)
4. 1 min: Sustain at 200 VUs
5. 30s: Ramp down to 0

**Test Scenarios:**

1. **Rapid Requests**
   - Quick login attempts
   - Customer list requests
   - No sleep between operations

2. **Database Operations**
   - 5 rapid customer creations per cycle
   - No delay between creates
   - Timeout handling (5s)

3. **Concurrent API Calls**
   - Batch requests to multiple endpoints
   - Customers, orders, invoices, payments, subscriptions
   - 10s timeout

4. **Search Operations**
   - Multiple search queries: 'test', 'user', 'customer', 'order', 'invoice'
   - 5s timeout per request
   - No sleep between searches

5. **File Operations**
   - Simulated file upload
   - 10s timeout

6. **Resource Intensive**
   - Large dataset requests (1000 items)
   - 15s timeout
   - Memory/CPU stress

7. **Authentication Stress**
   - 3 rapid login attempts per cycle
   - No delay between attempts

8. **Error Scenarios**
   - Invalid payloads
   - Empty credentials
   - Malformed requests
   - Expected: 4xx/5xx responses

9. **Long Running Operations**
   - Comprehensive reports endpoint
   - 30s timeout
   - Extended operation handling

10. **Mixed Workflows**
    - Complete user journey
    - Dashboard → customers → orders
    - Rapid execution

**Custom Metrics:**
- `errors`: Error rate tracking
- `breaking_point`: Detection of system failures
- `timeout_rate`: Timeout occurrence rate
- `page_load_time`: Page performance
- `api_response_time`: API latency
- `request_count`: Total volume
- `successful_requests`: Success tracking
- `failed_requests`: Failure tracking

**Expected Behavior:**
- Identify maximum capacity
- Document degradation patterns
- Understand failure modes
- Establish safe operating limits

## Endpoint Configuration

All tests use the standardized `/api/v1/` endpoint paths:

### Customer Endpoints
- `GET /api/v1/customers?page=0&size=20` - List customers
- `GET /api/v1/customers/{id}` - Get customer details
- `GET /api/v1/customers?search={query}&page=0&size=20` - Search
- `POST /api/v1/customers` - Create customer

### Order Endpoints
- `GET /api/v1/orders?page=0&size=20` - List orders

### Invoice Endpoints
- `GET /api/v1/invoices?page=0&size=20` - List invoices

### Payment Endpoints
- `GET /api/v1/payments?page=0&size=20` - List payments

### Subscription Endpoints
- `GET /api/v1/subscriptions?page=0&size=20` - List subscriptions

### Authentication
- `POST /api/auth/login` - User login

### Frontend Routes
- `GET /dashboard` - Dashboard page
- `GET /customers` - Customer list page
- `GET /orders` - Order list page

## Running the Tests

### Prerequisites
```bash
# Install k6
brew install k6
# or
sudo apt-get install k6
# or download from https://k6.io/
```

### Environment Variables
```bash
# Set base URL (default: http://localhost:3000)
export BASE_URL="http://localhost:3000"
```

### Execute Tests

**Load Test:**
```bash
k6 run frontend/tests/performance/load-tests.js
```

**Spike Test:**
```bash
k6 run frontend/tests/performance/spike-tests.js
```

**Soak Test:**
```bash
k6 run frontend/tests/performance/soak-tests.js
```

**Stress Test:**
```bash
k6 run frontend/tests/performance/stress-tests.js
```

### With Custom Base URL
```bash
BASE_URL="https://your-domain.com" k6 run frontend/tests/performance/load-tests.js
```

## Test Execution in CI/CD

### GitHub Actions Example
```yaml
name: Load Testing

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install k6
        run: sudo apt-get install k6

      - name: Run load test
        run: |
          k6 run frontend/tests/performance/load-tests.js \
            --out json=load-test-results.json
```

## Interpreting Results

### Key Metrics to Monitor

1. **Response Time (http_req_duration)**
   - `p(50)`: Median response time
   - `p(95)`: 95th percentile (most important for SLAs)
   - `p(99)`: 99th percentile (worst-case scenarios)

2. **Error Rate (http_req_failed)**
   - Percentage of failed requests
   - Should align with thresholds

3. **Custom Metrics**
   - `login_success`: Authentication reliability
   - `stability_score`: Overall system stability
   - `error_rate`: Custom error tracking

### Success Criteria

**Load Test:**
- ✅ p95 < 2000ms
- ✅ Error rate < 5%
- ✅ No crashes or timeouts

**Spike Test:**
- ✅ System remains responsive during spike
- ✅ Recovery within 1 minute
- ✅ No data corruption

**Soak Test:**
- ✅ No memory leaks detected
- ✅ Stable performance over time
- ✅ Error rate remains consistent

**Stress Test:**
- ✅ System degrades gracefully
- ✅ Identifies breaking points
- ✅ Recovery after load reduction

## Best Practices

1. **Test Environment**
   - Use production-like environment
   - Ensure realistic data volume
   - Match production configuration

2. **Test Data**
   - Use realistic user scenarios
   - Vary data patterns
   - Include edge cases

3. **Monitoring**
   - Monitor system resources (CPU, memory, disk)
   - Track database performance
   - Watch application logs

4. **Iterations**
   - Run multiple iterations
   - Compare results over time
   - Establish baselines

5. **Reporting**
   - Document baseline metrics
   - Track regressions
   - Share results with team

## Integration with Performance Monitoring

The load tests integrate with existing monitoring infrastructure:

- **Micrometer/Prometheus:** Backend metrics via Spring Actuator
- **CloudEvents:** Track event processing during load
- **Application Logs:** Correlate with load test timing
- **Database Metrics:** Query performance under load

## Troubleshooting

### Common Issues

1. **High Error Rate**
   - Check backend logs
   - Verify database connectivity
   - Review connection pool settings

2. **Slow Response Times**
   - Monitor database queries
   - Check Redis cache hit rates
   - Review Kafka consumer lag

3. **Memory Leaks (Soak Test)**
   - Check JVM heap usage
   - Monitor garbage collection
   - Review object allocations

4. **Breaking Points (Stress Test)**
   - Document capacity limits
   - Review resource constraints
   - Plan for scaling

## Future Enhancements

1. **Distributed Load Testing**
   - Use k6 cloud for distributed tests
   - Multiple geographic regions
   - Higher VU counts

2. **Automated Performance Testing**
   - CI/CD integration
   - Regression detection
   - Performance budgets

3. **Advanced Scenarios**
   - Real user behavior simulation
   - Complex workflow patterns
   - Data-driven tests

4. **Enhanced Monitoring**
   - Real-time dashboards
   - Alerting integration
   - Automated reporting

## Conclusion

The K6 load testing suite provides comprehensive performance validation for the BSS application. By testing under various load conditions (normal, spike, soak, and stress), we ensure the system performs reliably across different scenarios.

All tests have been updated to use the standardized `/api/v1/` API endpoints, ensuring consistency with the backend implementation. The test suite is ready for integration into CI/CD pipelines and ongoing performance monitoring.

## Files Modified

1. ✅ `frontend/tests/performance/load-tests.js` - Updated to /api/v1/
2. ✅ `frontend/tests/performance/spike-tests.js` - Updated to /api/v1/
3. ✅ `frontend/tests/performance/soak-tests.js` - Updated to /api/v1/
4. ✅ `frontend/tests/performance/stress-tests.js` - Updated to /api/v1/

## Test Coverage Summary

| Test Type | Duration | VUs | Endpoints Tested | Scenarios |
|-----------|----------|-----|------------------|-----------|
| Load Test | 9 min | 25-50 | 5 main modules | 7 scenarios |
| Spike Test | 5 min | 10-200 | 5 main modules | 7 scenarios |
| Soak Test | 30 min | 20 | 5 main modules | 8 scenarios |
| Stress Test | 5 min | 0-200 | 5 main modules + auth | 10 scenarios |

**Total:** 49 minutes of testing, 29 scenarios, all major endpoints covered
