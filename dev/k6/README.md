# k6 Load Testing for BSS System

This directory contains k6 load testing scripts for the BSS (Business Support System).

## Prerequisites

Install k6:
- macOS: `brew install k6`
- Linux: `sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69`
  ```
  echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
  sudo apt-get update
  sudo apt-get install k6
  ```
- Windows: Download from [k6.io](https://k6.io)

## Running Tests

### 1. Smoke Test (Quick Health Check)

```bash
cd /home/labadmin/projects/droid-spring/dev/k6
k6 run scripts/smoke-test.js
```

This test verifies that all critical endpoints are responding correctly.

### 2. Load Test (Normal Load)

```bash
BASE_URL=http://localhost:8080 k6 run scripts/api-load-test.js
```

This test simulates normal usage patterns:
- 10 users for 5 minutes
- 20 users for 5 minutes
- Ramps up and down gradually

Thresholds:
- 95% of requests must complete within 500ms
- Error rate must be less than 5%

### 3. Stress Test (High Load)

```bash
BASE_URL=http://localhost:8080 k6 run scripts/stress-test.js
```

This test pushes the system to its limits:
- 50 → 100 → 200 users
- Each stage lasts 3 minutes
- Tests system behavior under stress

Thresholds:
- 95% of requests must complete within 1000ms
- Error rate must be less than 10%

### 4. Extreme Spike Test (Traffic Surges)

```bash
BASE_URL=http://localhost:8080 k6 run --vus 10000 --duration 45m scripts/extreme-spike-test.js
```

This test simulates sudden traffic spikes:
- 100 → 1,000 → 5,000 → 10,000 users
- Tests auto-scaling and circuit breakers
- Validates system resilience to surges
- Peak: 10,000 VUs

**Scale**: 100K - 1M events

### 5. Volume Test (1 Million Events)

```bash
BASE_URL=http://localhost:8080 k6 run --vus 500 --iterations 2000 scripts/volume-test-1m.js
```

High-volume throughput test:
- 500 VUs × 2,000 iterations = 1,000,000 requests
- Tests database connection pooling
- Validates circuit breaker behavior
- Checks system under sustained load

**Scale**: 1,000,000 events

### 6. Marathon Test (12+ Hours)

```bash
BASE_URL=http://localhost:8080 k6 run --vus 500 --duration 12h scripts/marathon-test.js
```

Long-running endurance test:
- 12+ hour duration
- Moderate load (50-1000 VUs)
- Detects memory leaks and resource exhaustion
- Tests stability over extended periods

**Scale**: 10M+ events

### 7. Soak Test (24+ Hours)

```bash
BASE_URL=http://localhost:8080 k6 run --vus 300 --duration 24h scripts/extreme-soak-test.js
```

Extreme endurance test:
- 24+ hour duration
- Continuous moderate load (100-500 VUs)
- Identifies slow memory leaks
- Tests log rotation and cache behavior
- Monitors JVM garbage collection

**Scale**: 20M+ events

### 8. Distributed Test (Multiple Nodes)

```bash
# Run on multiple machines/nodes
BASE_URL=http://localhost:8080 WORKER_ID=worker-1 k6 run --vus 5000 scripts/distributed-test.js
BASE_URL=http://localhost:8080 WORKER_ID=worker-2 k6 run --vus 5000 scripts/distributed-test.js
BASE_URL=http://localhost:8080 WORKER_ID=worker-3 k6 run --vus 5000 scripts/distributed-test.js
```

Distributed load test:
- Multiple K6 instances across different nodes
- Simulates 15,000+ concurrent users
- Tests cluster-wide performance
- Uses K6 Cloud for results aggregation

**Scale**: 100K - 1M+ events per run

## Test Comparison

| Test Type | Duration | VUs | Events | Purpose |
|-----------|----------|-----|--------|---------|
| Smoke | 1 min | 1-5 | ~100 | Health check |
| Load | 10 min | 10-20 | ~5K | Normal operation |
| Stress | 9 min | 50-200 | ~20K | High load |
| Spike | 45 min | 100-10K | 100K-1M | Traffic surges |
| Volume | 15-30 min | 500-1000 | 1M | Throughput |
| Marathon | 12 hours | 50-1000 | 10M+ | Endurance |
| Soak | 24 hours | 100-500 | 20M+ | Memory leaks |
| Distributed | 30-60 min | 5K-15K | 100K-1M+ | Multi-node |

## Running Tests by Scale

### Development (1K events)
```bash
# Quick validation
k6 run scripts/smoke-test.js
k6 run --vus 10 --duration 5m scripts/api-load-test.js
```

### Staging (10K events)
```bash
# Performance validation
k6 run --vus 100 --duration 10m scripts/stress-test.js
k6 run --vus 200 --duration 20m scripts/api-load-test.js
```

### Production (100K-1M+ events)
```bash
# Extreme testing
k6 run --vus 500 --iterations 2000 scripts/volume-test-1m.js
k6 run --vus 5000 --duration 30m scripts/distributed-test.js
k6 run --vus 10000 --duration 45m scripts/extreme-spike-test.js
```

## Test Results

After running a test, k6 will display:
- **Virtual Users (VUs)**: Number of concurrent users
- **Request Rate**: Requests per second
- **Response Times**: p50, p95, p99 percentiles
- **Error Rate**: Percentage of failed requests
- **Throughput**: Total requests processed

## Interpreting Results

### Good Performance
- p95 < 500ms for load test
- Error rate < 1%
- No significant increase in response time as load increases

### Warning Signs
- p95 > 1000ms
- Error rate > 5%
- Response time increases significantly with more users
- System becomes unstable

### Critical Issues
- Error rate > 20%
- p99 > 5000ms
- System crashes or becomes unresponsive

## Continuous Integration

Add to your CI/CD pipeline:

```yaml
# .github/workflows/load-test.yml
name: Load Tests

on:
  schedule:
    - cron: '0 2 * * *' # Run daily at 2 AM

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run k6 smoke test
        uses: loadimpact/k6-action@v1
        with:
          filename: scripts/smoke-test.js
          flags: --env BASE_URL=${{ secrets.TEST_ENV_URL }}
```

## Customization

To create your own test:

1. Copy one of the existing scripts
2. Modify the `options` object for your scenario
3. Add your test functions
4. Run with: `k6 run your-script.js`

## Best Practices

1. **Start Small**: Run smoke tests first
2. **Gradual Load**: Ramp up users gradually
3. **Set Realistic Thresholds**: Based on your SLA requirements
4. **Test Regularly**: Run before and after deployments
5. **Monitor Resources**: Check CPU, memory, and database during tests
6. **Clean Up**: Remove test data in teardown function

## Troubleshooting

### Connection Refused
- Check if backend is running: `curl http://localhost:8080/actuator/health`
- Verify BASE_URL environment variable

### High Error Rate
- Check backend logs: `docker logs bss-backend`
- Verify database connection
- Check PgBouncer configuration

### Slow Response Times
- Check database queries with PgHero: http://localhost:8082
- Monitor Grafana dashboards: http://localhost:3001
- Check resource usage: `docker stats`

## Additional Resources

- [k6 Documentation](https://k6.io/docs/)
- [BSS Observability Guide](../README-OBSERVABILITY.md)
- [Performance Testing Best Practices](https://k6.io/docs/test-authoring/analysis-and-results/)
