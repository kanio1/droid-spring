# Load Testing with k6

This directory contains k6 load testing scripts for the BSS monitoring system.

## Prerequisites

Install k6:
```bash
# macOS
brew install k6

# Windows (using Chocolatey)
choco install k6

# Docker
docker pull grafana/k6
```

## Test Scripts

### 1. load-test.js
**Purpose**: Standard load test with gradual ramp-up
**Duration**: ~16 minutes
**Target**: 100-200 virtual users
**Metrics**: Response time, error rate

Run:
```bash
k6 run load-test.js
```

### 2. stress-test.js
**Purpose**: Stress test to find breaking point
**Duration**: ~7.5 minutes
**Target**: 50-1000 virtual users
**Metrics**: Throughput, error rate, system limits

Run:
```bash
k6 run stress-test.js
```

### 3. spike-test.js
**Purpose**: Sudden traffic spike test
**Duration**: ~1.5 minutes
**Target**: 10-1000 virtual users (sudden spike)
**Metrics**: System resilience, recovery time

Run:
```bash
k6 run spike-test.js
```

## Configuration

### Environment Variables

```bash
# Set base URL
export BASE_URL=http://localhost:8080

# Run with custom URL
k6 run -e BASE_URL=https://api.example.com load-test.js
```

### Custom Options

Run with custom stages:
```bash
k6 run --stage 5m:100 load-test.js
```

Enable detailed output:
```bash
k6 run --summary-export=results.json load-test.js
```

## Performance Thresholds

### Load Test Thresholds
- 95th percentile response time < 500ms
- 99th percentile response time < 1000ms
- Error rate < 1%

### Stress Test Thresholds
- 95th percentile response time < 1000ms
- 99th percentile response time < 2000ms
- Error rate < 5%

### Spike Test Thresholds
- 95th percentile response time < 2000ms
- Error rate < 10%

## Test Results

After running a test, analyze:
1. **http_req_duration**: Response time distribution
2. **http_req_failed**: Error rate
3. **Virtual Users**: Concurrent load
4. **Requests per second**: Throughput

## Interpreting Results

### Good Performance
- Response time p95 < 500ms
- Error rate < 1%
- Stable VUs throughout test

### Warning Signs
- Response time p95 > 1000ms
- Error rate > 5%
- High standard deviation in response times

### Critical Issues
- Response time p99 > 2000ms
- Error rate > 10%
- Memory/CPU saturation

## CI/CD Integration

Add to your CI pipeline:
```bash
#!/bin/bash
k6 run --out json=test-results.json load-test.js
```

## Best Practices

1. **Start Small**: Begin with 10-50 VUs
2. **Monitor Resources**: Watch CPU/memory during tests
3. **Warm Up**: Let system stabilize before measuring
4. **Multiple Runs**: Run tests 3+ times for consistency
5. **Gradual Ramp-Up**: Avoid sudden load spikes (except spike tests)
6. **Clean Environment**: Restart services between test runs
7. **Document Changes**: Track performance regressions

## Example Test Execution

```bash
# 1. Start the application
cd backend
mvn spring-boot:run

# 2. Run load test in another terminal
cd k6
k6 run load-test.js

# 3. Review results
# Check for failed thresholds
# Analyze response time trends
# Review error rate
```

## Troubleshooting

### High Error Rates
- Check application logs
- Verify database connectivity
- Review connection pool settings
- Check Redis availability

### Slow Response Times
- Monitor CPU/memory usage
- Check database query performance
- Review caching configuration
- Analyze thread pool metrics

### Connection Issues
- Verify firewall rules
- Check application is running
- Validate API endpoints
- Review SSL/TLS configuration
