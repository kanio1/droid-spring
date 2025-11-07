# Resilience Testing Suite

This directory contains a comprehensive **Resilience Testing Suite** for chaos engineering, fault tolerance validation, and system resilience verification.

## Overview

The resilience testing suite validates that the BSS application can withstand and recover from various failure scenarios, maintaining functionality even under adverse conditions.

## Resilience Patterns Tested

1. **Circuit Breaker Pattern** - Fault detection and automatic recovery
2. **Timeout Handling** - Request timeout and connection abort
3. **Retry Mechanisms** - Exponential backoff and smart retries
4. **Graceful Degradation** - Fallback mechanisms and feature degradation
5. **Chaos Engineering** - Failure injection and recovery testing
6. **Load Resilience** - Performance under high load and stress
7. **Resource Management** - Connection pools and memory handling

## Prerequisites

```bash
# Application must be running
export BASE_URL=http://localhost:3000

# Install Playwright browsers
npx playwright install
```

## Test Files

### 1. Circuit Breaker Testing (`circuit-breaker.spec.ts`)

**Purpose:** Validate circuit breaker implementation and fault tolerance

**What it tests:**
- Database connection failure handling
- Fallback UI when service is unavailable
- Retry with exponential backoff
- Service recovery after outages
- Bulkhead pattern (isolate failures)
- Request timeouts
- Rate limiting
- Partial system failures
- Graceful degradation
- Health checks and monitoring

**Test Suites:**
1. **Circuit Breaker Resilience** - Core breaker functionality
2. **Graceful Degradation** - Fallback mechanisms
3. **Health Check & Monitoring** - System health endpoints

**Run:**
```bash
pnpm test:resilience:circuit
npx playwright test resilience/circuit-breaker.spec.ts
```

**Expected Behaviors:**
- Circuit opens after consecutive failures
- Requests fail fast when circuit is open
- Circuit closes after recovery timeout
- Fallback UI displayed on failures
- Health checks report system status
- Bulkhead pattern prevents cascade failures

**Pass Criteria:**
- ✅ Circuit prevents cascading failures
- ✅ Fallback mechanisms activated
- ✅ Service recovers automatically
- ✅ Bulkhead isolation working
- ✅ Timeouts prevent resource exhaustion
- ✅ Rate limiting prevents abuse

**Duration:** 2-3 minutes

---

### 2. Chaos Engineering Testing (`chaos-engineering.spec.ts`)

**Purpose:** Test system resilience through intentional failure injection

**What it tests:**
- Random pod/container kills
- Network latency injection
- Database connection pool exhaustion
- Memory pressure handling
- Disk space issues
- CPU throttle/injection
- Fault tolerance patterns
- Recovery testing
- Resilience metrics

**Test Suites:**
1. **Chaos Engineering - Failure Injection** - Chaos scenarios
2. **Fault Tolerance Patterns** - Resilience patterns
3. **Recovery Testing** - Auto-recovery validation
4. **Resilience Metrics** - Performance tracking

**Run:**
```bash
# Standard chaos tests
pnpm test:resilience:chaos

# With chaos mode enabled
CHAOS_MODE=true pnpm test:resilience
```

**Chaos Scenarios:**
- **Pod Kill Simulation** - Random service failures
- **Network Latency** - 3-second delays injected
- **Connection Pool Exhaustion** - 100 concurrent requests
- **Memory Pressure** - Large object allocation
- **Disk Space Issues** - Simulated storage problems
- **CPU Throttle** - High CPU load simulation

**Pass Criteria:**
- ✅ System survives random failures
- ✅ Recovery rate > 70%
- ✅ Latency handled gracefully
- ✅ Memory pressure managed
- ✅ Retry patterns working
- ✅ Auto-recovery functioning

**Duration:** 3-4 minutes

---

### 3. Load & Stress Resilience (`load-resilience.spec.ts`)

**Purpose:** Validate system behavior under high load and stress

**What it tests:**
- High concurrency handling (up to 200 users)
- Memory leak detection under load
- Traffic spike response
- Concurrent write consistency
- Breaking point identification
- Post-stress recovery
- Availability SLA validation
- Resource exhaustion handling
- Long-term stability (5-minute test)

**Test Suites:**
1. **High Load Resilience** - Concurrent request handling
2. **Stress Testing Resilience** - Breaking point discovery
3. **Chaos + Load Testing** - Combined failure and load
4. **Resource Exhaustion** - Pool and thread exhaustion
5. **Resilience Under Load - Long Running** - Extended stability

**Run:**
```bash
# Standard load tests
pnpm test:resilience:load

# Extended stress tests
STRESS_MODE=true pnpm test:resilience
```

**Load Scenarios:**
- **Concurrency Test** - 10-100 concurrent users
- **Traffic Spikes** - 5→10→20→50→100 user spikes
- **Breaking Point** - Find system limits
- **5-Minute Stability** - Sustained load test
- **Resource Exhaustion** - 200 connection requests

**Pass Criteria:**
- ✅ Handles 10 concurrent users with <5s avg response
- ✅ Breaking point > 50 users
- ✅ 99% availability under stress
- ✅ No memory leaks (growth < 50%)
- ✅ Successful recovery after stress
- ✅ Throughput maintained under load

**Duration:**
- Standard: 3-4 minutes
- Stress mode: 10-15 minutes

---

### 4. Timeout & Retry Testing (`timeout-retry.spec.ts`)

**Purpose:** Validate timeout handling and retry mechanisms

**What it tests:**
- Request timeouts (5s, 10s, 30s)
- Connection timeouts
- Operation abortion
- Retry on failures
- Exponential backoff
- Retry limits
- Smart retry logic (no retry on 4xx)
- Jitter implementation
- Circuit breaker timeouts
- Recovery from transient errors
- Fast-fail on permanent errors
- Retry metrics tracking

**Test Suites:**
1. **Timeout Handling** - Various timeout scenarios
2. **Retry Pattern** - Retry mechanism validation
3. **Circuit Breaker Timeout** - Breaker timing
4. **Timeout & Retry Configuration** - Config validation
5. **Error Recovery** - Transient vs permanent errors
6. **Resilience Metrics** - Retry tracking

**Run:**
```bash
pnpm test:resilience:timeout
npx playwright test resilience/timeout-retry.spec.ts
```

**Timeout Scenarios:**
- **Slow Response** - 15-second delayed response
- **Connection Timeout** - Never responding endpoint
- **Operation Abortion** - Cancel long-running operations
- **Exponential Backoff** - 3 retry attempts with delay
- **Retry Limits** - Maximum retry attempts
- **Smart Retry** - No retry on client errors (4xx)

**Pass Criteria:**
- ✅ Timeouts prevent resource exhaustion
- ✅ Retries succeed for transient errors
- ✅ Exponential backoff implemented
- ✅ Retry limits respected
- ✅ Jitter prevents thundering herd
- ✅ Circuit opens after failures
- ✅ Permanent errors fail fast

**Duration:** 3-4 minutes

---

## Running Tests

### Run All Resilience Tests

```bash
# Standard suite (5-10 minutes)
pnpm test:resilience

# With chaos engineering
CHAOS_MODE=true pnpm test:resilience

# With extended stress tests
STRESS_MODE=true pnpm test:resilience

# Both chaos and stress
CHAOS_MODE=true STRESS_MODE=true pnpm test:resilience
```

### Run Specific Test Category

```bash
# Circuit breaker only
pnpm test:resilience:circuit

# Chaos engineering only
pnpm test:resilience:chaos

# Load resilience only
pnpm test:resilience:load

# Timeout and retry only
pnpm test:resilience:timeout
```

### Run with Playwright

```bash
# All resilience tests
npx playwright test --project=resilience

# Specific test file
npx playwright test resilience/circuit-breaker.spec.ts

# With custom timeout
npx playwright test resilience/load-resilience.spec.ts --timeout=900000
```

### Using Test Runner Script

```bash
# Standard run
./tests/resilience/resilience-tests.sh

# With help
./tests/resilience/resilience-tests.sh --help

# Chaos mode
./tests/resilience/resilience-tests.sh --chaos

# Stress mode
./tests/resilience/resilience-tests.sh --stress

# Custom URL
BASE_URL=https://staging.example.com ./tests/resilience/resilience-tests.sh
```

## Environment Configuration

### Environment Variables

```bash
# Application URL (required)
BASE_URL=http://localhost:3000

# Enable chaos engineering mode
CHAOS_MODE=true

# Enable extended stress tests
STRESS_MODE=true

# Results directory
RESULTS_DIR=./results/resilience

# Export directory
EXPORT_DIR=./results/resilience-$(date +%Y%m%d-%H%M%S)
```

### Playwright Config

```typescript
{
  name: 'resilience',
  testDir: './tests/resilience',
  testMatch: /.*\.spec\.ts/,
  timeout: 120000, // 2 minutes
  retries: 0, // No retries
}
```

---

## Resilience Patterns Explained

### 1. Circuit Breaker Pattern

**Purpose:** Prevent cascading failures

**States:**
- **Closed** - Normal operation
- **Open** - Failing fast, no requests
- **Half-Open** - Testing if service recovered

**Benefits:**
- Prevents resource exhaustion
- Protects downstream services
- Enables fast failure detection
- Automatic recovery

### 2. Timeout Pattern

**Purpose:** Prevent indefinite waiting

**Types:**
- **Request Timeout** - Max time for response
- **Connection Timeout** - Max time to establish connection
- **Idle Timeout** - Max time without activity

**Benefits:**
- Prevents resource leaks
- Enables fast failure
- Improves user experience
- Allows retry

### 3. Retry Pattern

**Purpose:** Handle transient failures

**Strategies:**
- **Fixed Delay** - Wait same time between retries
- **Exponential Backoff** - Increase delay exponentially
- **Jitter** - Add randomness to prevent thundering herd

**Benefits:**
- Handles temporary failures
- Improves success rate
- Distributes load
- Reduces noise

### 4. Bulkhead Pattern

**Purpose:** Isolate failures

**Implementation:**
- Separate thread pools
- Separate connection pools
- Separate service instances
- Resource quotas per service

**Benefits:**
- Prevents cascade failures
- Limits blast radius
- Improves fault isolation
- Enables partial degradation

### 5. Fallback Pattern

**Purpose:** Provide alternative response

**Types:**
- **Static Response** - Default data
- **Cached Response** - Last known good data
- **Alternative Service** - Secondary provider
- **Degraded Mode** - Limited functionality

**Benefits:**
- Maintains availability
- Provides graceful degradation
- Improves user experience
- Reduces error impact

---

## Interpreting Results

### Circuit Breaker Results

**Good (Pass):**
- ✅ Circuit opens after 3-5 failures
- ✅ Requests fail fast (<1s) when open
- ✅ Circuit closes after 30s recovery timeout
- ✅ Bulkhead prevents service cross-contamination

**Issues (Fail):**
- ❌ No circuit breaker (cascading failures)
- ❌ Circuit never opens (resource exhaustion)
- ❌ Circuit never closes (stuck open)
- ❌ No bulkhead (failures spread)

### Timeout Results

**Good (Pass):**
- ✅ Requests timeout within configured limit
- ✅ Timeouts prevent resource exhaustion
- ✅ Different timeouts for different operations
- ✅ Operations can be aborted

**Issues (Fail):**
- ❌ Requests never timeout
- ❌ Timeouts too short (failures)
- ❌ Timeouts too long (waste resources)
- ❌ No operation abortion

### Retry Results

**Good (Pass):**
- ✅ Retries succeed for transient errors
- ✅ Exponential backoff implemented
- ✅ Jitter prevents thundering herd
- ✅ No retry on 4xx errors
- ✅ Retry limits respected

**Issues (Fail):**
- ❌ Retries amplify failures
- ❌ No exponential backoff
- ❌ Retry on permanent errors
- ❌ No retry limits
- ❌ Synchronized retries

### Load Resilience Results

**Good (Pass):**
- ✅ 10+ concurrent users handled
- ✅ Breaking point > 50 users
- ✅ 99% availability maintained
- ✅ No memory leaks
- ✅ Successful recovery after stress

**Issues (Fail):**
- ❌ Low breaking point (< 20 users)
- ❌ Memory leaks under load
- ❌ Availability < 95%
- ❌ No recovery after stress
- ❌ Response time > 10s under load

### Chaos Engineering Results

**Good (Pass):**
- ✅ Recovery rate > 70%
- ✅ Failures don't cascade
- ✅ Services recover automatically
- ✅ Metrics tracked

**Issues (Fail):**
- ❌ Recovery rate < 50%
- ❌ Failures cascade
- ❌ No automatic recovery
- ❌ Metrics not available

---

## Resilience Metrics

### Key Metrics to Track

**Circuit Breaker:**
- Open/Close transitions
- Failure threshold
- Recovery timeout
- Request success rate

**Timeouts:**
- Average response time
- Timeout rate
- Aborted operations
- Resource utilization

**Retries:**
- Retry count
- Success rate
- Backoff delay
- Jitter variance

**Load:**
- Concurrent users
- Response time (p50, p95, p99)
- Error rate
- Throughput (req/s)
- Availability (%)

**Memory:**
- Heap usage
- GC frequency
- Memory growth rate
- Leak detection

**Recovery:**
- Recovery time
- Recovery rate
- MTTR (Mean Time To Recovery)
- Availability SLA

### Metrics Collection

**Actuator Endpoints:**
```bash
# Health check
curl http://localhost:3000/health

# Metrics
curl http://localhost:3000/actuator/metrics

# Circuit breakers
curl http://localhost:3000/actuator/circuitbreakers
```

**Application Metrics:**
```javascript
// Track in application
resilienceMetrics.recordRetry('customers', true)
resilienceMetrics.recordTimeout('orders', 5000)
resilienceMetrics.recordCircuitBreaker('payments', 'OPEN')
```

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Resilience Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 4 * * *' # Daily at 4 AM

jobs:
  resilience-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm install

      - name: Install Playwright
        run: npx playwright install

      - name: Start application
        run: docker-compose up -d && sleep 30

      - name: Run resilience tests
        run: pnpm test:resilience
        env:
          BASE_URL: http://localhost:3000

      - name: Upload results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: resilience-results
          path: results/resilience/
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any

    environment {
        BASE_URL = 'http://staging.example.com'
        CHAOS_MODE = 'true'
    }

    stages {
        stage('Resilience Tests') {
            steps {
                script {
                    sh '''
                        pnpm test:resilience
                    '''
                }
            }

            post {
                always {
                    archiveArtifacts artifacts: 'results/resilience/**', allowEmptyArchive: true
                }
            }
        }
    }
}
```

---

## Troubleshooting

### Common Issues

**1. Tests timeout**
```bash
# Increase timeout
npx playwright test resilience/circuit-breaker.spec.ts --timeout=300000
```

**2. Application not responding**
```bash
# Check if running
curl http://localhost:3000/health

# Start application
pnpm run dev
# or
mvn spring-boot:run
```

**3. High memory usage**
```bash
# Run with more memory
node --max-old-space-size=4096 ./tests/resilience/resilience-tests.sh
```

**4. Slow performance**
```bash
# Run in headless mode
npx playwright test resilience --headed=false
```

**5. Flaky tests**
```bash
# Run multiple times
for i in {1..5}; do
  pnpm test:resilience:circuit
done
```

### Debug Mode

```bash
# Run with debug
npx playwright test resilience/circuit-breaker.spec.ts --debug

# With tracing
npx playwright test resilience --trace=on
```

---

## Best Practices

### 1. Test Design
- Isolate test cases
- Clean up after tests
- Use realistic failure scenarios
- Test recovery paths
- Validate metrics

### 2. Resilience Implementation
- Use proven patterns (Circuit Breaker, Retry, Timeout)
- Configure appropriate thresholds
- Monitor and alert on patterns
- Test in production-like environment
- Document failure modes

### 3. Monitoring
- Track all resilience metrics
- Alert on threshold breaches
- Dashboard for visibility
- Regular review of patterns
- Capacity planning based on tests

### 4. Continuous Testing
- Run resilience tests regularly
- Test after infrastructure changes
- Include in pre-production checks
- Monitor in production
- Regular chaos experiments

---

## Chaos Engineering Principles

### 1. Assume Failure Will Happen
- Design for failure
- Build redundancy
- Plan for recovery
- Test failure scenarios

### 2. Create Realistic Failures
- Use production-like scenarios
- Test actual failure modes
- Consider human factors
- Include cascading failures

### 3. Run Experiments in Production
- Test with real traffic
- Monitor closely
- Have rollback plan
- Start small

### 4. Minimize Blast Radius
- Isolate failures
- Use feature flags
- Gradual rollout
- Quick recovery

### 5. Learn and Improve
- Document findings
- Update architecture
- Improve monitoring
- Share knowledge

---

## Resources

### Documentation
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Retry Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/retry)
- [Timeout Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/timeout)
- [Chaos Engineering](http://principlesofchaos.org/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

### Tools
- **Resilience4j** - Java resilience library
- **Polly** - .NET resilience library
- **Hystrix** - Latency and fault tolerance
- **Chaos Monkey** - Failure injection
- **Gremlin** - Chaos engineering platform

### Metrics
- Prometheus + Grafana
- Micrometer
- Actuator
- Application Insights
- New Relic

---

## Additional Test Scenarios

### Database Resilience
- Connection pool exhaustion
- Transaction timeouts
- Deadlock handling
- Primary/secondary failover

### Message Queue Resilience
- Broker downtime
- Message retry
- Poison message handling
- Consumer lag

### Cache Resilience
- Cache eviction
- Cache stampede
- Invalidation delays
- Cache poisoning

### External Service Resilience
- API rate limiting
- Service unavailability
- Slow responses
- Data format changes

---

## File Structure

```
tests/resilience/
├── README.md                           # This file (600+ lines)
├── resilience-tests.sh                 # Main test runner
├── circuit-breaker.spec.ts             # Circuit breaker tests (40+ tests)
├── chaos-engineering.spec.ts           # Chaos tests (30+ tests)
├── load-resilience.spec.ts             # Load/stress tests (25+ tests)
└── timeout-retry.spec.ts               # Timeout/retry tests (35+ tests)
```

---

## Summary Statistics

**Test Files:** 4
**Total Tests:** 130+
**Test Suites:** 18
**Documentation:** 600+ lines
**Coverage:** All major resilience patterns
**Execution Time:**
- Standard: 5-10 minutes
- Stress mode: 10-15 minutes

---

## Support

For issues:
1. Check troubleshooting section
2. Review test logs
3. Verify environment setup
4. Check application health
5. Contact resilience team

---

**Last Updated:** 2025-11-06
**Test Types:** Circuit Breaker, Chaos, Load, Timeout/Retry
**Patterns:** 7 resilience patterns covered
**Duration:** 5-15 minutes per run
**Coverage:** Fault tolerance, Recovery, Performance
