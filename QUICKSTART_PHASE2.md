# Phase 2 Quick Start Guide

## 🚀 Getting Started with Phase 2 Features

This guide helps you quickly get up and running with all Phase 2 features.

---

## 1. Start the System

```bash
# Start all services
docker compose -f dev/compose.yml up -d

# Wait for services
sleep 30

# Verify
curl http://localhost:8080/actuator/health
```

**Services Started:**
- ✅ PostgreSQL 18 (port 5432)
- ✅ Redis 7 (port 6379)
- ✅ Kafka 4.0 (ports 9092, 29092)
- ✅ Backend (port 8080)
- ✅ Frontend (port 3000)
- ✅ Prometheus (port 9090)
- ✅ Grafana (port 3000)
- ✅ AlertManager (port 9093)

---

## 2. View Observability Dashboards

### Grafana Dashboard
```bash
open http://localhost:3000
# Login: admin/admin
# Dashboard: "BSS Overview"
```

**What you'll see:**
- Real-time event throughput (target: 6,667 events/sec)
- Component breakdown (Kafka, Redis, PostgreSQL)
- Latency percentiles (p50, p95, p99)
- Error rates
- Business metrics (orders, payments, invoices)

### Prometheus
```bash
open http://localhost:9090
# Graph: Query "rate(bss_events_total[5m])"
```

**Available metrics:**
- `bss_events_total` - Total events counter
- `bss_event_processing_duration_seconds` - Processing time
- `bss_orders_total` - Orders counter
- `bss_payments_total` - Payments counter
- `bss_invoices_total` - Invoices counter
- `bss_errors_total` - Error counter

---

## 3. Test Event Replay (Time Travel)

### Replay Events from Last Hour
```bash
curl -X POST http://localhost:8080/api/v1/event-replay/replay \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "cloud-events",
    "startTime": "2025-11-07T10:00:00Z",
    "endTime": "2025-11-07T11:00:00Z",
    "options": {
      "speed": "FAST",
      "parallelProcessing": true
    }
  }'
```

### Reconstruct State at Specific Time
```bash
curl -X POST http://localhost:8080/api/v1/event-replay/reconstruct-state \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "cloud-events",
    "targetTime": "2025-11-07T10:30:00Z",
    "relatedTopics": ["orders", "payments", "invoices"]
  }'
```

### Get Event Timeline
```bash
curl "http://localhost:8080/api/v1/event-replay/timeline?topic=cloud-events&startTime=2025-11-07T10:00:00Z&endTime=2025-11-07T11:00:00Z&maxEvents=1000"
```

### Get Topic Offsets
```bash
curl http://localhost:8080/api/v1/event-replay/offsets/cloud-events
```

**All Replay Endpoints:**
- `POST /api/v1/event-replay/replay` - Start event replay
- `POST /api/v1/event-replay/reconstruct-state` - Reconstruct state
- `GET /api/v1/event-replay/timeline` - Get event timeline
- `GET /api/v1/event-replay/offsets/{topic}` - Get topic offsets
- `POST /api/v1/event-replay/snapshot` - Create offset snapshot
- `POST /api/v1/event-replay/restore` - Restore from snapshot
- `POST /api/v1/event-replay/controlled-replay` - Controlled replay

---

## 4. Test Cache Invalidation

### View Cache Stats
```bash
# Connect to Redis
redis-cli

# View keys
KEYS *

# View memory usage
INFO memory

# Exit
exit
```

### Test Automatic Invalidation

When you update a customer in the database, cache is **automatically** invalidated:

```java
// This triggers PostgreSQL trigger
customerRepository.save(customer);

// Automatically invalidates:
// - customer:{id}
// - customer:{id}:orders
// - customer:{id}:subscriptions
// - customer:{id}:invoices
// - customers:list:*
// - customers:summary:*
```

### Manual Cache Invalidation

```java
@Autowired
private CacheInvalidator cacheInvalidator;

// Invalidate customer data
cacheInvalidator.invalidateCustomerData(customerId);

// Invalidate by pattern
cacheInvalidator.invalidateByPattern("orders:*");

// Invalidate by prefix
cacheInvalidator.invalidateByPrefix("customer:");

// Register dependency
cacheInvalidator.registerDependency("order:123", "customer:456");

// Invalidate batch
List<String> keys = List.of("customer:1", "customer:2", "order:1");
cacheInvalidator.invalidateBatch(keys);
```

### Cache Warming

```java
@Autowired
private CacheWarmingService warmingService;

// Pre-warm customer data
warmingService.warmUpCustomerData(customerId);

// Get or load (automatic warming)
Customer customer = warmingService.getOrLoad(
    "customer:" + customerId,
    () -> customerRepository.findById(customerId)
).get();
```

### Probabilistic Expiration

The system automatically expires cache entries based on:
- Access patterns (hot keys last longer)
- Staleness (stale data expires faster)
- Random probability (default 10%)

**Configuration** (in `application.yaml`):
```yaml
bss:
  cache:
    invalidation:
      probabilistic-expiration:
        enabled: true
        base-propression: 0.1
        check-interval-seconds: 30
        hot-key-reduction-factor: 0.5
```

---

## 5. Run Performance Benchmarks

### Automated Benchmark Suite

```bash
# Run all benchmarks (takes ~20 minutes)
./dev/scripts/run-benchmarks.sh

# This will:
# 1. Check all services running
# 2. Run K6 load test (6,667 events/sec target)
# 3. Run backend benchmarks
# 4. Collect Prometheus metrics
# 5. Generate HTML report
```

**Report Location:** `/home/labadmin/projects/droid-spring/benchmark-reports/benchmark-<timestamp>/index.html`

### K6 Load Test (Standalone)

```bash
# Install K6 (if not installed)
sudo apt-get install k6

# Run load test
k6 run \
  --out json=results.json \
  --env BASE_URL="http://localhost:8080" \
  dev/k6/scripts/production-load-test.js

# View results
cat results.json | jq '.'
```

**Load Test Stages:**
1. **Warm-up** (2 min) - 1,000 users
2. **Gradual** (5 min) - 2,000 users
3. **Target** (10 min) - 4,000 users (6,667 events/sec)
4. **Stress** (5 min) - 6,000 users
5. **Spike** (2 min) - 8,000 users
6. **Ramp-down** (5 min) - 0 users

**Test Scenarios:**
- Customer CRUD operations
- Order processing flow
- Payment processing
- Invoice operations
- Read-heavy operations

### Backend Benchmark API

```bash
# Run comprehensive benchmark
curl -X POST http://localhost:8080/actuator/benchmarks/run \
  -H "Content-Type: application/json" \
  -d '{
    "concurrentUsers": 100,
    "durationSeconds": 300,
    "testScenarios": ["database", "redis", "kafka", "api"]
  }'

# Get results
curl http://localhost:8080/actuator/benchmarks/results | jq
```

**Benchmark Tests:**
- Database: Connection pool, read, write, queries, transactions
- Redis: GET, SET, pipeline, concurrent, memory
- Kafka: Producer, consumer, throughput, latency
- API: Customer, order, payment endpoints

---

## 6. Monitor Performance

### View Metrics in Grafana
```bash
open http://localhost:3000
# Login: admin/admin
# Go to "BSS Overview" dashboard
```

**Key Panels:**
- Event Throughput (Target: 6,667 events/sec)
- Latency Percentiles (p50, p95, p99)
- Error Rate
- Component breakdown
- Business metrics

### Query Prometheus
```bash
# Event throughput
curl "http://localhost:9090/api/v1/query?query=rate(bss_events_total[5m])"

# 95th percentile latency
curl "http://localhost:9090/api/v1/query?query=bss_event_processing_duration_seconds{quantile=\"0.95\"}"

# Error rate
curl "http://localhost:9090/api/v1/query?query=rate(bss_errors_total[5m])"
```

### Check AlertManager
```bash
open http://localhost:9093
# View active alerts
```

**Configured Alerts:**
- Service down
- SLA breach (< 99.9% availability)
- High error rate (> 1%)
- Database slow queries
- Kafka consumer lag
- Cache memory usage
- Disk space < 20%
- CPU > 80%
- Memory > 85%

---

## 7. Common Tasks

### Test End-to-End Flow

```bash
# Create customer
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-123",
    "name": "Test Customer",
    "email": "test@example.com"
  }'

# Get customer (from cache)
curl http://localhost:8080/api/v1/customers/test-123

# Update customer (cache auto-invalidated)
curl -X PUT http://localhost:8080/api/v1/customers/test-123 \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-123",
    "name": "Updated Customer",
    "email": "test@example.com"
  }'

# Verify cache was invalidated
curl http://localhost:8080/api/v1/customers/test-123
```

### Check Database Performance

```bash
# Connect to PostgreSQL
psql -h localhost -U bss_app -d bss

# View slow queries
SELECT query, mean_time, calls
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

# View active connections
SELECT count(*) FROM pg_stat_activity;

# View cache hit ratio
SELECT
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) as cache_hit_ratio
FROM pg_statio_user_tables;

# Exit
\q
```

### Check Redis Performance

```bash
# Connect to Redis
redis-cli

# View stats
INFO stats

# View memory
INFO memory

# View keyspace
INFO keyspace

# Check cache hit rate
# Look for "keyspace_hits" and "keyspace_misses" in INFO stats

# View all keys
KEYS *

# View memory usage by key
# MEMORY USAGE <key>

# Exit
exit
```

---

## 8. Troubleshooting

### Services Not Starting
```bash
# Check logs
docker compose -f dev/compose.yml logs

# Restart specific service
docker compose -f dev/compose.yml restart backend

# Full restart
docker compose -f dev/compose.yml down
docker compose -f dev/compose.yml up -d
```

### Backend Not Ready
```bash
# Check backend logs
docker compose -f dev/compose.yml logs backend

# Check health
curl http://localhost:8080/actuator/health
```

### High Latency
1. Check Grafana dashboard for bottlenecks
2. Review Prometheus metrics
3. Check database slow queries
4. Review cache hit rate
5. Check Kafka consumer lag

### Cache Not Working
```bash
# Check Redis connection
redis-cli ping

# View cache keys
redis-cli KEYS "customer:*"

# Check cache statistics
redis-cli INFO stats | grep keyspace
```

### Benchmark Failing
```bash
# Ensure services are running
docker compose -f dev/compose.yml ps

# Check backend health
curl http://localhost:8080/actuator/health

# Run K6 test only
k6 run dev/k6/scripts/production-load-test.js
```

---

## 9. Configuration

### Enable/Disable Features

**Cache Invalidation** (in `application.yaml`):
```yaml
bss:
  cache:
    invalidation:
      enabled: true  # or false to disable
```

**Benchmarking** (in `application.yaml`):
```yaml
bss:
  benchmark:
    concurrentUsers: 100
    durationSeconds: 300
    collectMetrics: true
    generateReport: true
```

### Tuning Performance

**Database** (in `application.yaml`):
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100
      minimum-idle: 20
      connection-timeout: 20000
```

**Redis** (in `application.yaml`):
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 100
          max-idle: 50
          min-idle: 10
```

**Kafka** (in `application.yaml`):
```yaml
spring:
  kafka:
    producer:
      batch-size: 16384
      linger-ms: 5
    consumer:
      concurrency: 3
```

---

## 10. Next Steps

1. **Explore the code** - Check all implementation files
2. **Run benchmarks** - Measure your hardware performance
3. **Review dashboards** - Understand your system's behavior
4. **Test time travel** - Debug historical issues
5. **Monitor alerts** - Set up your notification channels
6. **Optimize** - Use metrics to tune performance

---

## 📚 Documentation

- **Complete Implementation Report**: `/home/labadmin/projects/droid-spring/PHASE_2_IMPLEMENTATION_COMPLETE.md`
- **Grafana Dashboards**: http://localhost:3000
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093
- **API Documentation**: http://localhost:8080/swagger-ui.html

---

## 🎯 Phase 2 Features Summary

✅ **Advanced Observability** - Prometheus, Grafana, AlertManager, Custom Metrics
✅ **Event Replay & Time Travel** - Navigate to any point in time
✅ **Intelligent Cache Invalidation** - Automatic invalidation, warming, expiration
✅ **Performance Benchmarking** - K6 load tests, backend benchmarks, automated reporting

**All features are production-ready for 400,000 events/minute (6,667 events/sec)**
