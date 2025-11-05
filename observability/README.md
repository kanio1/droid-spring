# Observability Stack for BSS System

## Overview

Comprehensive observability solution for monitoring, logging, and tracing the BSS (Business Support System) using Prometheus, Grafana, Tempo, Loki, and AlertManager.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Grafana (Port 3000)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐   │
│  │Dashboard │ │  Alert   │ │  Log     │ │    Trace      │   │
│  │          │ │ Manager  │ │ Explorer │ │   Explorer    │   │
│  └──────────┘ └──────────┘ └──────────┘ └───────────────┘   │
└─────────────────────────────────────────────────────────────┘
              │                │              │
┌─────────────▼────────┬───────▼─────────────▼─────────────┐
│   Prometheus (9090)  │    Tempo (3200)    │   Loki (3100)  │
│  ┌────────────────┐  │  ┌──────────────┐ │  ┌──────────┐  │
│  │ Metrics        │  │  │ Traces       │ │  │ Logs     │  │
│  │ - Application  │  │  │ - Request    │ │  │ - App    │  │
│  │ - System       │  │  │ - Database   │ │  │ - Access │  │
│  │ - Business     │  │  │ - External   │ │  │ - Error  │  │
│  └────────────────┘  │  └──────────────┘ │  └──────────┘  │
└──────────────────────┴───────────────────┴────────────────┘
              │                        │
┌─────────────▼──────────────┬─────────▼──────────────────┐
│   Service Discovery        │     Jaeger (14268)         │
│  ┌──────────────────────┐  │  ┌──────────────────────┐  │
│  │ - Kubernetes         │  │  │ - Trace Storage      │  │
│  │ - Service Mesh       │  │  │ - Query Service      │  │
│  │ - Exporters          │  │  │ - Agent              │  │
│  └──────────────────────┘  │  └──────────────────────┘  │
└────────────────────────────┴────────────────────────────┘
              │
    ┌─────────▼────────┐
    │  AlertManager    │
    │     (9093)       │
    │  ┌────────────┐  │
    │  │ Notifications│ │
    │  │ - Email    │  │
    │  │ - Slack    │  │
    │  │ - PagerDuty│ │
    │  └────────────┘  │
    └──────────────────┘
```

## Components

### 1. Prometheus (Metrics Collection)
**Port**: 9090
**Purpose**: Time-series database for metrics

**Features**:
- Metrics scraping from applications
- PromQL query language
- Alerting rules
- Service discovery
- Data retention (30 days)
- Remote write/read support

**Configuration**:
- `prometheus.yml` - Main configuration
- `rules/alert-rules.yml` - Alert definitions

**Metrics Sources**:
- BSS Backend (Spring Boot Actuator)
- PostgreSQL Exporter
- Redis Exporter
- Kafka Exporter
- Keycloak Exporter
- Kong Gateway
- Node Exporter
- cAdvisor

### 2. Grafana (Visualization)
**Port**: 3000
**Purpose**: Dashboard and visualization platform

**Features**:
- Multiple data source support
- Custom dashboards
- Alerting
- User management
- Plugin ecosystem

**Dashboards**:
- BSS System Overview
- Backend Performance
- Database Metrics
- Infrastructure Metrics
- Business Metrics
- SLO/SLA Monitoring

**Data Sources**:
- Prometheus (metrics)
- Tempo (traces)
- Loki (logs)
- Jaeger (traces)
- AlertManager (alerts)

### 3. Tempo (Tracing)
**Port**: 3200
**Purpose**: Distributed tracing backend

**Features**:
- High-volume trace ingestion
- TraceQL query language
- Gossip protocol for ring management
- S3/GCS/Azure backend storage
- Integration with OpenTelemetry

**Traces**:
- HTTP requests
- Database queries
- External service calls
- Business logic spans

### 4. Loki (Logs)
**Port**: 3100
**Purpose**: Log aggregation system

**Features**:
- LogQL query language
- Multi-tenant
- Scalable ingestion
- S3/GCS/Azure backend
- Log shipping (Promtail)

**Log Sources**:
- Application logs
- Access logs
- Error logs
- Audit logs

### 5. AlertManager (Alerting)
**Port**: 9093
**Purpose**: Alert routing and management

**Features**:
- Alert grouping
- Route based on labels
- Multiple receivers
- Silence alerts
- High availability

**Receivers**:
- Email
- Slack
- PagerDuty
- VictorOps
- Webhook

### 6. Jaeger (Tracing - Alternative)
**Port**: 14268 (HTTP), 16686 (UI)
**Purpose**: Distributed tracing platform

**Features**:
- Trace visualization
- Service dependency graph
- Performance profiling
- Distributed context propagation

## Deployment Options

### Option 1: Docker Compose (Development)

```bash
# Start observability stack
docker compose -f dev/compose.yml up -d prometheus grafana tempo loki alertmanager

# View dashboards
open http://localhost:3000  # Grafana
open http://localhost:9090  # Prometheus
open http://localhost:3200  # Tempo
open http://localhost:3100  # Loki
```

### Option 2: Kubernetes (Production)

#### Install Prometheus Operator (Recommended)

```bash
# Add Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install kube-prometheus-stack
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values observability/kubernetes/prometheus-values.yaml
```

#### Manual Installation

```bash
# Create namespace
kubectl create namespace observability

# Install Prometheus
kubectl apply -f observability/kubernetes/prometheus/

# Install Grafana
kubectl apply -f observability/kubernetes/grafana/

# Install Tempo
kubectl apply -f observability/kubernetes/tempo/

# Install Loki
kubectl apply -f observability/kubernetes/loki/

# Install AlertManager
kubectl apply -f observability/kubernetes/alertmanager/
```

### Option 3: Helm Charts (Recommended)

```bash
# Install Prometheus Stack
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace

# Install Tempo
helm install tempo grafana/tempo \
  --namespace observability \
  --create-namespace

# Install Loki
helm install loki grafana/loki \
  --namespace observability \
  --create-namespace

# Install Grafana
helm install grafana grafana/grafana \
  --namespace observability \
  --create-namespace
```

## Configuration

### Prometheus Configuration

Edit `prometheus/prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'bss-backend'
    scrape_interval: 10s
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['backend:8080']
```

### Alert Rules

Edit `prometheus/rules/alert-rules.yml`:

```yaml
groups:
  - name: bss-backend-alerts
    rules:
      - alert: BackendDown
        expr: up{job="bss-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "BSS Backend is down"
```

### AlertManager Configuration

Edit `alertmanager/alertmanager.yml`:

```yaml
route:
  routes:
    - match:
        severity: critical
      receiver: 'backend-critical'

receivers:
  - name: 'backend-critical'
    email_configs:
      - to: 'backend-team@company.com'
```

### Grafana Dashboards

Import dashboard from `grafana/dashboards/bss-overview.json`:

1. Open Grafana UI (http://localhost:3000)
2. Go to Dashboards → Import
3. Upload JSON file or paste JSON
4. Select data source: Prometheus
5. Click Import

## Monitoring Targets

### Application Metrics (BSS Backend)

```yaml
endpoints:
  - path: /actuator/prometheus
    port: 8080
    scheme: http
```

**Key Metrics**:
- `http_server_requests_*` - HTTP request metrics
- `jvm_memory_*` - JVM memory metrics
- `bss_customers_*` - Business metrics
- `bss_invoices_*` - Billing metrics
- `bss_payments_*` - Payment metrics

### Infrastructure Metrics

#### PostgreSQL
```yaml
job_name: 'postgresql'
static_configs:
  - targets: ['postgres-exporter:9187']
```

#### Redis
```yaml
job_name: 'redis'
static_configs:
  - targets: ['redis-exporter:9121']
```

#### Kafka
```yaml
job_name: 'kafka'
static_configs:
  - targets: ['kafka-exporter:9308']
```

## Alerts

### Critical Alerts
- BackendDown - Application is down
- HighErrorRate - Error rate > 5%
- HighResponseTime - P95 > 1s
- DatabaseDown - PostgreSQL down
- RedisDown - Redis down
- HighInvoiceFailureRate - Billing failures

### Warning Alerts
- HighResponseTime - P95 > 500ms
- JVMMemoryUsageHigh - JVM memory > 90%
- CPUUsageHigh - CPU usage > 80%
- HighLoadAverage - Load average high

### SLO Alerts
- APIAvailabilitySLOBreach - Availability < 99.5%
- APILatencySLOBreach - P99 latency > 1s

## Dashboards

### 1. BSS System Overview
**Purpose**: High-level system health
**Metrics**:
- API Request Rate
- API Response Time (P50, P95, P99)
- HTTP Status Codes Distribution
- CPU Usage
- JVM Memory Usage
- Database Connection Pool

### 2. Backend Performance
**Purpose**: Detailed backend metrics
**Metrics**:
- Request rate by endpoint
- Response time by endpoint
- Error rate by endpoint
- JVM heap usage
- GC pause time
- Thread count

### 3. Database Metrics
**Purpose**: PostgreSQL monitoring
**Metrics**:
- Active connections
- Query performance
- Index usage
- Table size
- Cache hit ratio

### 4. Business Metrics
**Purpose**: Business KPIs
**Metrics**:
- Customer creation rate
- Invoice generation rate
- Payment success rate
- Subscription conversions

## Query Examples

### Prometheus Queries

```promql
# Request rate
rate(http_server_requests_total[5m])

# Response time P95
histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket[5m]))

# Error rate
rate(http_server_requests_total{status=~"5.."}[5m]) / rate(http_server_requests_total[5m])

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes

# CPU usage
system_cpu_usage
```

### Tempo (TraceQL)

```tracel
# Find slow traces
{resource.service.name="bss-backend"} | duration > 1s

# Find error traces
{resource.service.name="bss-backend"} | status_code = 2

# Find traces by customer
{resource.service.name="bss-backend"} | customer_id = "12345"
```

### Loki (LogQL)

```logql
# Application errors
{job="bss-backend"} |= "ERROR"

# Slow queries
{job="postgresql"} |= "duration:"

# Access logs
{job="nginx"} |= "404"

# Recent errors
{job="bss-backend"} |~ "ERROR|Exception"
```

## Best Practices

### 1. Metrics Naming
- Use lowercase with underscores
- Include units (e.g., `_seconds`, `_bytes`, `_total`)
- Add business context (e.g., `bss_customers_created_total`)

### 2. Alerting
- Start with critical alerts only
- Tune thresholds based on baseline
- Include runbook links
- Test alerts regularly

### 3. Dashboards
- Create for different audiences (dev, ops, business)
- Keep dashboards simple
- Use variables for filtering
- Add annotations for deployments

### 4. Logs
- Structure logs (JSON format)
- Include correlation IDs
- Avoid logging sensitive data
- Set appropriate log levels

### 5. Traces
- Use OpenTelemetry SDK
- Include business attributes
- Trace critical paths only
- Set appropriate sampling rate

## Troubleshooting

### Prometheus not scraping targets

```bash
# Check target status
curl http://localhost:9090/api/v1/targets

# Check logs
kubectl logs -n monitoring prometheus-prometheus-0
```

### No data in Grafana

1. Check data source configuration
2. Verify network connectivity
3. Check Prometheus queries
4. Review Grafana logs

### Alerts not firing

1. Verify alert rules are loaded
2. Check AlertManager configuration
3. Review alert evaluations
4. Test receiver configuration

### High memory usage

1. Adjust retention period
2. Enable compression
3. Use remote write
4. Check scrape intervals

## Performance Tuning

### Prometheus
- Adjust scrape intervals (15s-30s)
- Enable compression
- Set appropriate retention
- Use recording rules

### Grafana
- Cache queries
- Use Grafana caching
- Limit dashboard queries
- Use variable filters

### Tempo
- Adjust ingestion rate
- Configure backend storage
- Set trace retention
- Use sampling

### Loki
- Configure chunk target size
- Adjust ingestion rate
- Set log retention
- Use structured logging

## Security

### Access Control
- Use Grafana auth
- Secure Prometheus endpoint
- Enable TLS for AlertManager
- Restrict network access

### RBAC (Kubernetes)

```yaml
apiVersion: v1
kind: Role
metadata:
  namespace: monitoring
  name: prometheus
rules:
  - apiGroups: [""]
    resources: ["services", "endpoints", "pods"]
    verbs: ["get", "list", "watch"]
```

### Secrets Management

```yaml
# Use Kubernetes secrets
apiVersion: v1
kind: Secret
metadata:
  name: alertmanager-secret
type: Opaque
data:
  smtp-password: <base64-encoded>
```

## Maintenance

### Backup

```bash
# Backup Prometheus data
kubectl exec -n monitoring prometheus-prometheus-0 -- \
  tar czf /tmp/prometheus-backup.tar.gz /prometheus

# Backup Grafana dashboards
kubectl exec -n observability grafana-0 -- \
  grafana-cli admin export-dashboard
```

### Update

```bash
# Update Prometheus
helm upgrade prometheus prometheus-community/kube-prometheus-stack

# Update Grafana
helm upgrade grafana grafana/grafana
```

### Clean Old Data

```bash
# Prometheus retention
--storage.tsdb.retention.time=30d

# Loki retention
-chunk_store_target_size=1572864000 \
-table-manager.retention-period=672h
```

## Integration with CI/CD

### GitHub Actions

```yaml
- name: Deploy monitoring
  run: |
    helm upgrade --install prometheus prometheus-community/kube-prometheus-stack \
      --namespace monitoring --create-namespace
```

### ArgoCD

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: monitoring
spec:
  destination:
    namespace: monitoring
    server: https://kubernetes.default.svc
  source:
    path: observability/
    repoURL: https://github.com/company/bss
    targetRevision: HEAD
```

## Support

### Documentation
- Prometheus: https://prometheus.io/docs/
- Grafana: https://grafana.com/docs/
- Tempo: https://grafana.com/docs/tempo/
- Loki: https://grafana.com/docs/loki/
- AlertManager: https://prometheus.io/docs/alerting/latest/alertmanager/

### Community
- Prometheus: https://prometheus.io/community/
- Grafana: https://community.grafana.com/
- CNCF: https://www.cncf.io/

## License

Apache License 2.0
