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
