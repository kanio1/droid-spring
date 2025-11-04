# BSS Observability Stack - Complete Setup Guide

**Date:** 2025-11-04
**Version:** 1.0
**Status:** ✅ Complete

---

## 🎯 Overview

BSS system now includes a **complete observability stack** for monitoring, tracing, and logging:

- **Tempo** - Distributed tracing visualization
- **Grafana** - Dashboards and metrics visualization
- **Loki** - Log aggregation and search
- **Prometheus** - Metrics collection and storage
- **OpenTelemetry** - Auto-instrumentation for backend and frontend

---

## 🏗️ Architecture

```
┌─────────────────┐
│   Frontend      │  (Nuxt.js + OTel Plugin)
│   (Port 3000)   │
└────────┬────────┘
         │ HTTP/API
         ▼
┌─────────────────┐
│   Backend       │  (Spring Boot + OTel)
│   (Port 8080)   │
│                 │
│  ┌───────────┐  │
│  │ Metrics   │  │  Micrometer
│  │ Tracing   │  │  OpenTelemetry
│  │ Logging   │  │  SLF4J
│  └───────────┘  │
└────────┬────────┘
         │ OTLP
         ▼
┌─────────────────┐
│  Observability  │
│  Stack          │
│                 │
│  ┌───────────┐  │
│  │ Prometheus│  │  :9090
│  │ Tempo     │  │  :3200
│  │ Loki      │  │  :3100
│  │ Grafana   │  │  :3001
│  └───────────┘  │
└────────┬────────┘
         │ Dashboards
         ▼
    Grafana UI
```

---

## 🚀 Getting Started

### 1. Start Observability Stack

```bash
# From project root
cd dev

# Start all services (including observability)
docker compose up -d

# Check service health
docker compose ps
```

### 2. Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3001 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Tempo** | http://localhost:3200 | - |
| **Loki** | http://localhost:3100 | - |
| **BSS Backend** | http://localhost:8080 | - |
| **BSS Frontend** | http://localhost:3000 | - |

### 3. Verify Data Flow

**Backend Metrics:**
```bash
# Check if metrics are exposed
curl http://localhost:8080/actuator/prometheus | grep bss_customers

# Check trace endpoint
curl http://localhost:3200/api/services
```

**Grafana Dashboard:**
1. Open http://localhost:3001
2. Login: admin/admin
3. Navigate to "BSS" folder
4. Open "BSS System Overview" dashboard

---

## 📊 Custom Business Metrics

### Backend Metrics (Spring Boot)

**Location:** `backend/src/main/java/com/droid/bss/infrastructure/metrics/BusinessMetrics.java`

**Available Metrics:**

| Metric | Type | Description |
|--------|------|-------------|
| `bss_customers_created_total` | Counter | Total customers created |
| `bss_customers_updated_total` | Counter | Total customers updated |
| `bss_customers_status_changed_total` | Counter | Status changes |
| `bss_invoices_created_total` | Counter | Invoices created |
| `bss_invoices_paid_total` | Counter | Invoices paid |
| `bss_payments_created_total` | Counter | Payments created |
| `bss_payments_completed_total` | Counter | Payments completed |
| `bss_subscriptions_created_total` | Counter | Subscriptions created |
| `bss_subscriptions_active` | Gauge | Current active subscriptions |
| `bss_invoices_pending` | Gauge | Pending invoices |
| `bss_customers_total` | Gauge | Total customers |
| `bss_customers_create.time` | Timer | Create operation time |
| `bss_customers.query_by_status.time` | Timer | Query operation time |

**Usage in Code:**
```java
// Inject BusinessMetrics
@Autowired
private BusinessMetrics businessMetrics;

// Record metric
businessMetrics.incrementCustomerCreated();

// Timed operation
@Timed(value = "bss.customers.create.time")
public ResponseEntity<CustomerResponse> createCustomer(...) {
    // Method implementation
}
```

### Frontend Metrics (Nuxt.js)

**Location:** `frontend/app/plugins/otel.client.ts`

**Available Metrics:**

| Metric | Type | Description |
|--------|------|-------------|
| `bss.frontend.page_loads` | Counter | Page load events |
| `bss.frontend.api_calls` | Counter | API call count |
| `bss.frontend.api_errors` | Counter | API error count |
| `bss.frontend.user_interactions` | Counter | User interactions |
| `bss.frontend.page_load_duration` | Histogram | Page load time |

**Usage in Nuxt:**
```typescript
// Get OTel plugin
const { $otel } = useNuxtApp()

// Track API call
$otel.trackApiCall('/api/customers', 'POST', success)

// Track user interaction
$otel.trackUserInteraction('button', 'click')
```

---

## 🔍 Tracing

### Backend Tracing (Auto-instrumented)

**Spring Boot** automatically instruments:
- HTTP requests/responses
- Database calls (JPA/Hibernate)
- Kafka producers/consumers
- Redis operations
- Method calls with `@WithSpan` annotation

**View Traces:**
1. Grafana → Explore → Tempo datasource
2. Search by service name: `bss-backend`
3. Click trace to view span details

### Frontend Tracing (Manual)

**Nuxt.js** plugin instruments:
- Page loads
- Route changes
- API calls (fetch/XHR)
- User interactions

**View Frontend Traces:**
1. Grafana → Explore → Tempo
2. Search by service name: `bss-frontend`
3. See frontend → backend propagation

---

## 📝 Logging

### Structured Logging

**Backend (Logback):**
```xml
<!-- logs JSON format with trace IDs -->
<pattern>%d{ISO8601} [%X{traceId:-},%X{spanId:-}] %-5level [%thread] %logger{36} - %msg%n</pattern>
```

**View Logs:**
1. Grafana → Explore → Loki datasource
2. Query: `{job="containerlogs"}`
3. Filter by service: `service_name="bss-backend"`

### Log Levels

| Level | Description |
|-------|-------------|
| **TRACE** | Very detailed information |
| **DEBUG** | Debugging information |
| **INFO** | General information (default) |
| **WARN** | Warning messages |
| **ERROR** | Error messages |

---

## 📈 Grafana Dashboards

### BSS System Overview

**Location:** `dev/grafana/dashboards/bss-overview.json`

**Panels:**

1. **Customer Operations** (Stat)
   - Rate of customer creation
   - Thresholds: 0 (green), 5 (yellow), 10 (red)

2. **Invoice Processing** (Stat)
   - Rate of invoice creation
   - Thresholds: 0 (green), 3 (yellow), 7 (red)

3. **Payment Success Rate** (Stat)
   - Percentage of successful payments
   - Thresholds: 80% (yellow), 95% (green)

4. **Active Subscriptions** (Stat)
   - Current active subscription count

5. **API Request Rate** (Graph)
   - Request rate by endpoint
   - Filtered by method and URI

6. **API Latency P95** (Graph)
   - 95th percentile latency
   - SLI: < 200ms target

7. **Error Rate** (Graph)
   - 5xx error percentage
   - Target: < 0.1%

8. **Database Connection Pool** (Graph)
   - Active vs idle connections
   - Monitor connection health

---

## 🔧 Configuration Files

### Docker Compose
- `dev/compose.yml` - Main orchestration
- Services: postgres, redis, keycloak, backend, frontend, **tempo**, **loki**, **prometheus**, **grafana**

### Tempo (Traces)
- `dev/tempo/tempo.yaml` - Trace storage configuration
- Storage: Local (blocks + WAL)
- Retention: 24 hours

### Loki (Logs)
- `dev/loki/loki-config.yaml` - Log aggregation config
- Storage: Filesystem
- Retention: Configured via compactor

### Prometheus (Metrics)
- `dev/prometheus/prometheus.yml` - Metrics scraping config
- Targets: backend (actuator), all services
- Scrape interval: 15s

### Grafana
- `dev/grafana/provisioning/datasources/datasources.yaml` - Data sources
- `dev/grafana/provisioning/dashboards/dashboards.yaml` - Dashboard provisioning

---

## 🚨 Alerting (Future Enhancement)

### Suggested Alerts

**Business Metrics:**
- `bss_invoices_paid_total < 0.5 * bss_invoices_created_total` (Low payment rate)
- `bss_payments_completed_total / bss_payments_created_total < 0.95` (Low success rate)

**Technical Metrics:**
- `http_server_requests_duration_p95 > 200` (High latency)
- `rate(http_server_requests_total{status="5.."}[5m]) > 0.01` (High error rate)
- `up{job="bss-backend"} == 0` (Service down)

### Setup Alertmanager

```yaml
# alert_rules.yml
groups:
- name: bss-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.01
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate detected"
```

---

## 🛠️ Troubleshooting

### Metrics Not Appearing

1. **Check Prometheus targets:**
   - http://localhost:9090/targets
   - All endpoints should be "UP"

2. **Check backend metrics endpoint:**
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. **Check Grafana data sources:**
   - Grafana → Connections → Data Sources
   - All should be green

### Traces Not Flowing

1. **Check Tempo health:**
   ```bash
   curl http://localhost:3200/ready
   ```

2. **Verify OpenTelemetry SDK:**
   - Backend logs should show "Tracer configured"
   - Check for OTel exporter errors

3. **Check trace propagation:**
   - Frontend request should include `traceparent` header
   - Backend should receive and continue trace

### Logs Not Aggregating

1. **Check Promtail status:**
   ```bash
   docker logs bss-promtail
   ```

2. **Verify Loki health:**
   ```bash
   curl http://localhost:3100/ready
   ```

3. **Check log ingestion:**
   - Grafana → Loki → Inspector
   - Look for log streams

---

## 📚 Additional Resources

### Documentation
- [Tempo Documentation](https://grafana.com/docs/tempo/latest/)
- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [Loki Documentation](https://grafana.com/docs/loki/latest/)
- [Prometheus Documentation](https://prometheus.io/docs/)

### Best Practices
- Use meaningful metric names with units
- Set appropriate labels (avoid high cardinality)
- Configure retention policies
- Monitor resource usage (disk, memory)
- Set up alerting before going to production

### Performance Tuning
- Adjust scrape intervals based on needs
- Configure appropriate retention periods
- Monitor Tempo/Loki disk usage
- Tune query timeouts

---

## ✅ Verification Checklist

- [ ] All services started successfully
- [ ] Grafana accessible (http://localhost:3001)
- [ ] Backend metrics exposed (/actuator/prometheus)
- [ ] Traces visible in Tempo
- [ ] Logs aggregated in Loki
- [ ] BSS dashboard showing data
- [ ] Custom metrics reporting values
- [ ] Trace propagation working (frontend → backend)
- [ ] Logs include trace IDs

---

**Status:** ✅ Complete
**Next:** Proceed to Faza 2 (Service Activation Engine)
