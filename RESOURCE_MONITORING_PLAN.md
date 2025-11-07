# Resource Usage Monitoring & Cost Optimization
**Projekt Plan - BSS System**

---

## 📋 EXECUTIVE SUMMARY

**Vision:** Real-time monitoring of all customer resources (bandwidth, storage, services, API calls) with intelligent alerts, cost optimization, and predictive analytics.

**Business Impact:**
- Prevent overages before they happen
- Optimize resource allocation automatically
- Increase transparency for customers
- Reduce support tickets by 40%
- Enable usage-based billing

**Tech Stack:**
- **PostgreSQL**: Historical usage data, resource catalogs, cost models
- **Redis**: Real-time metrics cache, active alerts, thresholds
- **Kafka**: Metrics streaming, alert events, usage data
- **CloudEvents**: `usage.threshold.v1`, `cost.optimization.v1`, `resource.alert.v1`
- **Nuxt.js**: Real-time monitoring dashboard, alert management
- **Spring Boot**: Metrics collection, threshold engine, alert dispatcher

---

## 🏗️ ARCHITECTURE OVERVIEW

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Data Sources                              │
├─────────────────────────────────────────────────────────────┤
│  • Network Equipment (SNMP)  • Service Gateways             │
│  • API Gateways              • Billing Systems              │
│  • Database Metrics          • Application Logs             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Kafka Message Broker                           │
│  Topic: resource.usage.raw                                  │
│  Format: CloudEvents v1.0                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Services                           │
├─────────────────────────────────────────────────────────────┤
│  • Metrics Collector Service                                │
│  • Threshold Engine                                         │
│  • Alert Dispatcher                                         │
│  • Cost Calculator                                          │
│  • Optimization Recommender                                 │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Data Storage                                   │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL:  Historical + Structured Data                  │
│  Redis:       Real-time + Cache + Sessions                  │
│  Kafka:       Event Stream + Processing                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Frontend (Nuxt.js)                             │
├─────────────────────────────────────────────────────────────┤
│  • Real-time Dashboard                                      │
│  • Alert Management                                         │
│  • Cost Reports                                             │
│  • Configuration UI                                         │
│  • Customer Portal                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 DATABASE SCHEMA

### New Tables

#### 1. Resource Catalogs
```sql
-- Define all trackable resources
CREATE TABLE resource_catalogs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  resource_type VARCHAR(50) NOT NULL, -- bandwidth, storage, api_calls, cpu, memory
  resource_name VARCHAR(100) NOT NULL, -- "Internet 100Mbps", "Cloud Storage 1TB"
  unit VARCHAR(20) NOT NULL, -- GB, MB, hours, requests
  cost_per_unit DECIMAL(10,4),
  is_billable BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_resource_catalogs_tenant ON resource_catalogs(tenant_id);
CREATE INDEX idx_resource_catalogs_type ON resource_catalogs(resource_type);
```

#### 2. Customer Resources
```sql
-- Track resources assigned to customers
CREATE TABLE customer_resources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL,
  resource_catalog_id UUID NOT NULL REFERENCES resource_catalogs(id),
  subscription_id UUID, -- Optional link to subscription
  current_usage DECIMAL(15,4) DEFAULT 0,
  limit_value DECIMAL(15,4), -- NULL = unlimited
  warning_threshold DECIMAL(5,2) DEFAULT 80, -- % of limit
  critical_threshold DECIMAL(5,2) DEFAULT 95, -- % of limit
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_customer_resources_customer ON customer_resources(customer_id);
CREATE INDEX idx_customer_resources_status ON customer_resources(status);
```

#### 3. Usage Metrics
```sql
-- Raw usage data
CREATE TABLE usage_metrics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_resource_id UUID NOT NULL REFERENCES customer_resources(id),
  metric_timestamp TIMESTAMP NOT NULL,
  metric_value DECIMAL(15,4) NOT NULL,
  metric_unit VARCHAR(20) NOT NULL,
  source VARCHAR(50), -- "api_gateway", "snmp", "application"
  metadata JSONB, -- Additional context
  created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for time-series queries
CREATE INDEX idx_usage_metrics_customer_time ON usage_metrics(customer_resource_id, metric_timestamp);
CREATE INDEX idx_usage_metrics_time_range ON usage_metrics(metric_timestamp) WHERE metric_timestamp > NOW() - INTERVAL '1 year';
```

#### 4. Aggregated Usage
```sql
-- Hourly/daily/monthly aggregates
CREATE TABLE usage_aggregates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_resource_id UUID NOT NULL REFERENCES customer_resources(id),
  period_type VARCHAR(10) NOT NULL, -- hour, day, week, month
  period_start TIMESTAMP NOT NULL,
  period_end TIMESTAMP NOT NULL,
  avg_usage DECIMAL(15,4),
  min_usage DECIMAL(15,4),
  max_usage DECIMAL(15,4),
  total_usage DECIMAL(15,4),
  cost DECIMAL(10,2),
  created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_usage_aggregates_customer_period ON usage_aggregates(customer_resource_id, period_type, period_start);
```

#### 5. Alerts
```sql
-- Alert system
CREATE TABLE alerts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_resource_id UUID NOT NULL REFERENCES customer_resources(id),
  alert_type VARCHAR(50) NOT NULL, -- threshold_warning, threshold_critical, anomaly
  severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
  message TEXT NOT NULL,
  current_value DECIMAL(15,4),
  threshold_value DECIMAL(15,4),
  status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, ACKNOWLEDGED, RESOLVED
  acknowledged_by UUID,
  acknowledged_at TIMESTAMP,
  resolved_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_customer ON alerts(customer_resource_id);
```

#### 6. Cost Models
```sql
-- Cost tracking and optimization
CREATE TABLE cost_models (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  model_name VARCHAR(100) NOT NULL,
  billing_period VARCHAR(20) NOT NULL, -- monthly, yearly
  base_cost DECIMAL(10,2),
  overage_rate DECIMAL(10,4),
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW()
);

-- Cost forecasts
CREATE TABLE cost_forecasts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_resource_id UUID NOT NULL REFERENCES customer_resources(id),
  forecast_period_start DATE NOT NULL,
  forecast_period_end DATE NOT NULL,
  predicted_usage DECIMAL(15,4),
  predicted_cost DECIMAL(10,2),
  confidence_level DECIMAL(5,2), -- 0-100
  created_at TIMESTAMP DEFAULT NOW()
);
```

### Data Migration

```sql
-- Add tenant_id to existing tables
ALTER TABLE customers ADD COLUMN tenant_id UUID DEFAULT 'default-tenant';

-- Create default resource catalog
INSERT INTO resource_catalogs (tenant_id, resource_type, resource_name, unit, cost_per_unit, is_billable)
VALUES
  ('default-tenant', 'bandwidth', 'Internet 100Mbps', 'GB', 0.10, true),
  ('default-tenant', 'bandwidth', 'Internet 500Mbps', 'GB', 0.08, true),
  ('default-tenant', 'storage', 'Cloud Storage 1TB', 'GB', 0.05, true),
  ('default-tenant', 'api_calls', 'API Requests', 'request', 0.001, true),
  ('default-tenant', 'cpu', 'CPU Hours', 'hour', 0.05, true);

-- Create customer resources from existing subscriptions
INSERT INTO customer_resources (customer_id, resource_catalog_id, limit_value, current_usage)
SELECT
  c.id as customer_id,
  rc.id as resource_catalog_id,
  CASE
    WHEN rc.resource_type = 'bandwidth' THEN 1000 -- 1TB
    WHEN rc.resource_type = 'storage' THEN 1024 -- 1TB
    WHEN rc.resource_type = 'api_calls' THEN 100000 -- 100K
  END as limit_value,
  0 as current_usage
FROM customers c
CROSS JOIN resource_catalogs rc
WHERE rc.tenant_id = 'default-tenant';
```

---

## 🔧 BACKEND SERVICES

### 1. Metrics Collector Service

**Endpoint:** `/api/metrics/collector`

```java
@RestController
@RequestMapping("/api/metrics")
public class MetricsCollectorController {

    @Autowired
    private MetricsCollectorService metricsService;

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingestUsage(@RequestBody UsageEvent event) {
        // Process incoming usage event
        metricsService.processUsageEvent(event);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/customer/{customerId}/current")
    public ResponseEntity<List<CurrentUsage>> getCurrentUsage(@PathVariable String customerId) {
        return ResponseEntity.ok(metricsService.getCurrentUsage(customerId));
    }
}

@Service
public class MetricsCollectorService {

    @Autowired
    private KafkaTemplate<String, CloudEvent> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void processUsageEvent(UsageEvent event) {
        // 1. Validate event
        validateEvent(event);

        // 2. Store in Redis (real-time cache)
        String key = "usage:" + event.getCustomerId() + ":" + event.getResourceType();
        redisTemplate.opsForValue().set(key, event.getValue(), Duration.ofHours(1));

        // 3. Publish to Kafka
        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource("urn:droid:bss:metrics")
            .withType("resource.usage.raw.v1")
            .withData(JSON.toJSONString(event))
            .build();

        kafkaTemplate.send("resource.usage.raw", cloudEvent);

        // 4. Update threshold engine
        checkThresholds(event);
    }

    private void checkThresholds(UsageEvent event) {
        CustomerResource resource = customerResourceRepository
            .findByCustomerIdAndResourceType(event.getCustomerId(), event.getResourceType());

        if (resource != null) {
            double usagePercent = (event.getValue() / resource.getLimitValue()) * 100;

            if (usagePercent >= resource.getCriticalThreshold()) {
                sendAlert(resource, "CRITICAL", event.getValue(), usagePercent);
            } else if (usagePercent >= resource.getWarningThreshold()) {
                sendAlert(resource, "WARNING", event.getValue(), usagePercent);
            }
        }
    }
}
```

### 2. Threshold Engine

```java
@Service
public class ThresholdEngine {

    @Autowired
    private AlertService alertService;

    @Autowired
    private NotificationService notificationService;

    @EventListener
    public void handleUsageEvent(UsageEvent event) {
        CustomerResource resource = getResource(event);

        if (resource == null) return;

        double usagePercent = (event.getValue() / resource.getLimitValue()) * 100;

        // Check thresholds
        if (usagePercent >= resource.getCriticalThreshold()) {
            createAlert(resource, "THRESHOLD_CRITICAL", event.getValue(), usagePercent);
            notificationService.sendCriticalAlert(resource, event);
        } else if (usagePercent >= resource.getWarningThreshold()) {
            createAlert(resource, "THRESHOLD_WARNING", event.getValue(), usagePercent);
            notificationService.sendWarningAlert(resource, event);
        }

        // Check for anomalies (simple statistical check)
        checkAnomalies(resource, event);
    }

    private void checkAnomalies(CustomerResource resource, UsageEvent event) {
        // Get last 24 hours of data
        List<UsageMetric> recent = usageRepository
            .findLast24Hours(resource.getId());

        if (recent.size() > 10) {
            double avg = recent.stream().mapToDouble(UsageMetric::getValue).average().orElse(0);
            double stdDev = calculateStdDev(recent, avg);
            double zScore = (event.getValue() - avg) / stdDev;

            // Alert if z-score > 3 (99.7% confidence)
            if (Math.abs(zScore) > 3) {
                createAlert(resource, "ANOMALY_DETECTED", event.getValue(), zScore);
            }
        }
    }
}
```

### 3. Alert Dispatcher

```java
@Component
public class AlertDispatcher {

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private SlackService slackService;

    @EventListener
    public void dispatchAlert(Alert alert) {
        // Get notification preferences for customer
        NotificationPreferences prefs = getNotificationPreferences(alert.getCustomerResource().getCustomerId());

        // Email notification
        if (prefs.isEmailEnabled()) {
            emailService.sendAlertEmail(alert, prefs.getEmailRecipients());
        }

        // SMS for critical alerts
        if (alert.getSeverity() == AlertSeverity.CRITICAL && prefs.isSmsEnabled()) {
            smsService.sendSmsAlert(alert, prefs.getPhoneNumbers());
        }

        // Slack webhook
        if (prefs.isSlackEnabled() && alert.getSeverity() != AlertSeverity.LOW) {
            slackService.sendSlackAlert(alert, prefs.getSlackWebhook());
        }

        // Store in database
        alertRepository.save(alert);

        // Publish CloudEvent
        publishCloudEvent(alert);
    }
}
```

### 4. Cost Calculator

```java
@Service
public class CostCalculationService {

    public CostBreakdown calculateCost(CustomerResource resource, LocalDateTime start, LocalDateTime end) {
        // Get usage data for period
        List<UsageMetric> usage = usageRepository
            .findByResourceAndPeriod(resource.getId(), start, end);

        double totalUsage = usage.stream()
            .mapToDouble(UsageMetric::getValue)
            .sum();

        // Get cost model
        CostModel costModel = getCostModel(resource.getCustomerId());

        // Calculate cost
        double baseCost = costModel.getBaseCost();
        double overageUsage = Math.max(0, totalUsage - resource.getLimitValue());
        double overageCost = overageUsage * costModel.getOverageRate();
        double totalCost = baseCost + overageCost;

        return CostBreakdown.builder()
            .baseCost(baseCost)
            .overageCost(overageCost)
            .totalCost(totalCost)
            .totalUsage(totalUsage)
            .limit(resource.getLimitValue())
            .overagePercentage((overageUsage / resource.getLimitValue()) * 100)
            .build();
    }

    public CostForecast forecastCost(CustomerResource resource, int daysAhead) {
        // Simple linear regression forecast
        List<UsageMetric> recent = usageRepository
            .findLast30Days(resource.getId());

        double trend = calculateTrend(recent);
        double predictedUsage = (recent.isEmpty() ? 0 : recent.get(recent.size()-1).getValue()) + (trend * daysAhead);

        double cost = calculateCost(resource,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(daysAhead))
            .getTotalCost();

        return CostForecast.builder()
            .predictedUsage(predictedUsage)
            .predictedCost(cost)
            .confidenceLevel(calculateConfidence(recent))
            .daysAhead(daysAhead)
            .build();
    }
}
```

### 5. CloudEvent Publishers

```java
@Component
public class CloudEventPublisher {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUsageThresholdEvent(CustomerResource resource, Alert alert) {
        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource("urn:droid:bss:monitoring")
            .withType("usage.threshold.v1")
            .withTime(Instant.now())
            .withData(JSON.toJSONString(Map.of(
                "customerId", resource.getCustomerId(),
                "resourceType", resource.getResourceType(),
                "currentUsage", alert.getCurrentValue(),
                "threshold", alert.getThresholdValue(),
                "percentage", (alert.getCurrentValue() / alert.getThresholdValue()) * 100,
                "severity", alert.getSeverity()
            )))
            .build();

        kafkaTemplate.send("resource.alerts", event);
    }

    public void publishCostOptimizationEvent(CustomerResource resource, OptimizationRecommendation recommendation) {
        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource("urn:droid:bss:optimization")
            .withType("cost.optimization.v1")
            .withTime(Instant.now())
            .withData(JSON.toJSONString(Map.of(
                "customerId", resource.getCustomerId(),
                "recommendationType", recommendation.getType(),
                "potentialSavings", recommendation.getPotentialSavings(),
                "description", recommendation.getDescription()
            )))
            .build();

        kafkaTemplate.send("optimization.events", event);
    }
}
```

---

## 🎨 FRONTEND (NUXT.JS)

### 1. Real-time Monitoring Dashboard

**Page:** `pages/monitoring/dashboard.vue`

```vue
<template>
  <div class="monitoring-dashboard">
    <h1>Resource Monitoring</h1>

    <!-- Real-time metrics cards -->
    <div class="metrics-grid">
      <MetricCard
        v-for="resource in resources"
        :key="resource.id"
        :resource="resource"
        :current-usage="resource.currentUsage"
        :limit="resource.limit"
        :trend="resource.trend"
        :is-critical="isCritical(resource)"
      />
    </div>

    <!-- Real-time usage chart -->
    <div class="usage-chart">
      <h2>Real-time Usage</h2>
      <RealtimeChart
        :data="chartData"
        :time-range="timeRange"
        @time-range-changed="handleTimeRangeChange"
      />
    </div>

    <!-- Active alerts -->
    <div class="alerts-section">
      <h2>Active Alerts ({{ activeAlerts.length }})</h2>
      <AlertList
        :alerts="activeAlerts"
        @acknowledge="acknowledgeAlert"
        @resolve="resolveAlert"
      />
    </div>

    <!-- Cost optimization -->
    <div class="optimization-section">
      <h2>Cost Optimization</h2>
      <CostOptimizationCard
        :recommendations="optimizationRecommendations"
        @apply="applyOptimization"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
const { resources, activeAlerts, optimizationRecommendations } = useMonitoring()
const { chartData, timeRange, loadRealtimeData } = useUsageChart()

// Real-time updates via WebSocket
onMounted(() => {
  const ws = new WebSocket('ws://localhost:3001/monitoring')
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.type === 'USAGE_UPDATE') {
      updateResource(data.resource)
    } else if (data.type === 'ALERT') {
      addAlert(data.alert)
    }
  }
})

const handleTimeRangeChange = (range: TimeRange) => {
  loadRealtimeData(range)
}
</script>
```

### 2. Metric Card Component

**Component:** `components/MetricCard.vue`

```vue
<template>
  <div class="metric-card" :class="{ critical: isCritical }">
    <div class="card-header">
      <h3>{{ resource.name }}</h3>
      <span class="resource-type">{{ resource.type }}</span>
    </div>

    <div class="usage-display">
      <div class="usage-value">
        <span class="current">{{ currentUsage }}</span>
        <span class="separator">/</span>
        <span class="limit">{{ limit }}</span>
        <span class="unit">{{ resource.unit }}</span>
      </div>

      <div class="usage-bar">
        <div
          class="usage-fill"
          :style="{ width: usagePercentage + '%' }"
          :class="{
            'warning': usagePercentage >= warningThreshold && usagePercentage < criticalThreshold,
            'critical': usagePercentage >= criticalThreshold
          }"
        />
      </div>
    </div>

    <div class="card-footer">
      <div class="trend">
        <i :class="trendIcon" />
        <span>{{ trendPercentage }}%</span>
      </div>

      <div class="last-updated">
        Updated: {{ formatTime(lastUpdated) }}
      </div>
    </div>

    <!-- Alert indicator -->
    <div v-if="hasAlert" class="alert-indicator" :class="alertSeverity">
      <i class="pi pi-exclamation-triangle" />
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  resource: any
  currentUsage: number
  limit: number
  trend: number
}>()

const usagePercentage = computed(() => {
  if (!props.limit) return 0
  return (props.currentUsage / props.limit) * 100
})

const isCritical = computed(() => usagePercentage.value >= 95)
const hasAlert = computed(() => isCritical.value)

const trendIcon = computed(() => {
  if (props.trend > 0) return 'pi pi-arrow-up'
  if (props.trend < 0) return 'pi pi-arrow-down'
  return 'pi pi-minus'
})
</script>
```

### 3. Realtime Chart Component

**Component:** `components/RealtimeChart.vue`

```vue
<template>
  <div class="realtime-chart">
    <canvas ref="chartCanvas"></canvas>

    <div class="chart-controls">
      <ButtonGroup>
        <Button
          v-for="range in timeRanges"
          :key="range.value"
          :label="range.label"
          :active="timeRange === range.value"
          @click="setTimeRange(range.value)"
        />
      </ButtonGroup>

      <Button
        icon="pi pi-refresh"
        label="Refresh"
        @click="refresh"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Chart, LineController, LineElement, PointElement, LinearScale, TimeScale } from 'chart.js'
import 'chartjs-adapter-date-fns'

Chart.register(LineController, LineElement, PointElement, LinearScale, TimeScale)

const props = defineProps<{
  data: any[]
  timeRange: string
}>()

const emit = defineEmits(['time-range-changed'])

const chartCanvas = ref<HTMLCanvasElement>()
let chart: Chart

onMounted(() => {
  if (chartCanvas.value) {
    chart = new Chart(chartCanvas.value, {
      type: 'line',
      data: {
        datasets: [{
          label: 'Usage',
          data: props.data,
          borderColor: '#3B82F6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          tension: 0.4
        }]
      },
      options: {
        responsive: true,
        scales: {
          x: {
            type: 'time',
            time: {
              unit: getTimeUnit(props.timeRange)
            }
          },
          y: {
            beginAtZero: true
          }
        },
        animation: {
          duration: 0
        }
      }
    })
  }
})

watch(() => props.data, (newData) => {
  if (chart) {
    chart.data.datasets[0].data = newData
    chart.update('none')
  }
})

const timeRanges = [
  { label: '1H', value: '1h' },
  { label: '24H', value: '24h' },
  { label: '7D', value: '7d' },
  { label: '30D', value: '30d' }
]

const setTimeRange = (range: string) => {
  emit('time-range-changed', range)
}
</script>
```

### 4. Alert Management

**Component:** `components/AlertList.vue`

```vue
<template>
  <div class="alert-list">
    <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.severity">
      <div class="alert-icon">
        <i :class="getAlertIcon(alert.type)" />
      </div>

      <div class="alert-content">
        <div class="alert-header">
          <span class="alert-type">{{ alert.type }}</span>
          <span class="alert-time">{{ formatTime(alert.createdAt) }}</span>
        </div>
        <p class="alert-message">{{ alert.message }}</p>
        <div class="alert-details">
          <span>Current: {{ alert.currentValue }} {{ alert.unit }}</span>
          <span>Threshold: {{ alert.thresholdValue }} {{ alert.unit }}</span>
        </div>
      </div>

      <div class="alert-actions">
        <Button
          v-if="alert.status === 'ACTIVE'"
          label="Acknowledge"
          icon="pi pi-check"
          severity="secondary"
          size="small"
          @click="$emit('acknowledge', alert)"
        />
        <Button
          v-if="alert.status === 'ACKNOWLEDGED'"
          label="Resolve"
          icon="pi pi-times"
          severity="success"
          size="small"
          @click="$emit('resolve', alert)"
        />
        <Tag :value="alert.status" :severity="getStatusSeverity(alert.status)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  alerts: Alert[]
}>()

const emit = defineEmits(['acknowledge', 'resolve'])

const getAlertIcon = (type: string) => {
  const icons: Record<string, string> = {
    THRESHOLD_WARNING: 'pi pi-exclamation-circle',
    THRESHOLD_CRITICAL: 'pi pi-exclamation-triangle',
    ANOMALY_DETECTED: 'pi pi-chart-line'
  }
  return icons[type] || 'pi pi-info-circle'
}
</script>
```

### 5. Cost Optimization

**Component:** `components/CostOptimizationCard.vue`

```vue
<template>
  <div class="cost-optimization">
    <div v-for="rec in recommendations" :key="rec.id" class="optimization-item">
      <div class="optimization-header">
        <i :class="getRecommendationIcon(rec.type)" />
        <h4>{{ rec.title }}</h4>
        <Tag :value="rec.potentialSavings + ' savings'" severity="success" />
      </div>

      <p class="optimization-description">{{ rec.description }}</p>

      <div class="optimization-impact">
        <div class="impact-item">
          <span class="label">Cost Impact:</span>
          <span class="value">-${{ rec.potentialSavings }}/month</span>
        </div>
        <div class="impact-item">
          <span class="label">Implementation:</span>
          <span class="value">{{ rec.effort }}</span>
        </div>
      </div>

      <div class="optimization-actions">
        <Button
          label="Apply"
          icon="pi pi-check"
          @click="$emit('apply', rec)"
        />
        <Button
          label="Details"
          icon="pi pi-info-circle"
          severity="secondary"
          @click="showDetails(rec)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  recommendations: OptimizationRecommendation[]
}>()

const emit = defineEmits(['apply'])

const getRecommendationIcon = (type: string) => {
  const icons: Record<string, string> = {
    RESIZE_RESOURCE: 'pi pi-server',
    SCHEDULE_OFF_PEAK: 'pi pi-clock',
    COMPRESS_DATA: 'pi pi-compress',
    ELIMINATE_WASTE: 'pi pi-trash'
  }
  return icons[type] || 'pi pi-lightbulb'
}
</script>
```

---

## 🔌 INTEGRATION POINTS

### 1. Data Sources

#### Network Equipment (SNMP)
```bash
# snmpwalk to collect bandwidth usage
snmpwalk -v2c -c public router1 1.3.6.1.2.1.2.2.1.10

# Parse and send to metrics collector
curl -X POST http://localhost:8080/api/metrics/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-123",
    "resourceType": "bandwidth",
    "value": 1024.5,
    "unit": "GB",
    "timestamp": "2024-01-15T10:30:00Z",
    "source": "snmp"
  }'
```

#### API Gateway
```java
// Intercept API calls
@Component
public class ApiCallInterceptor {

    @EventListener
    public void handleApiCall(ApiCallEvent event) {
        // Record API usage
        UsageEvent usageEvent = UsageEvent.builder()
            .customerId(event.getCustomerId())
            .resourceType("api_calls")
            .value(1)
            .unit("request")
            .timestamp(Instant.now())
            .build();

        metricsCollectorService.processUsageEvent(usageEvent);
    }
}
```

#### Database Monitoring
```sql
-- Query to check database size per customer
SELECT
    customer_id,
    pg_size_pretty(pg_database_size(customer_db)) as db_size
FROM customer_databases;
```

### 2. Notification Channels

#### Email Notifications
```java
@Service
public class EmailService {

    public void sendThresholdAlert(CustomerResource resource, Alert alert) {
        String subject = String.format(
            "Alert: %s usage at %.1f%% of limit",
            resource.getResourceType(),
            alert.getPercentage()
        );

        String body = buildAlertEmailBody(resource, alert);

        mailService.sendEmail(
            getCustomerEmails(resource.getCustomerId()),
            subject,
            body
        );
    }

    private String buildAlertEmailBody(CustomerResource resource, Alert alert) {
        return String.format("""
            Dear Customer,

            This is an automated alert regarding your %s usage.

            Current Usage: %.2f %s
            Limit: %.2f %s
            Percentage: %.1f%%

            Please take action to avoid service interruption.

            Best regards,
            BSS Team
            """,
            resource.getResourceType(),
            alert.getCurrentValue(),
            resource.getUnit(),
            resource.getLimitValue(),
            resource.getUnit(),
            alert.getPercentage()
        );
    }
}
```

#### SMS Notifications
```java
@Service
public class SmsService {

    public void sendCriticalAlert(CustomerResource resource, UsageEvent event) {
        String message = String.format(
            "ALERT: %s usage critical (%.1f%%). Current: %.2f %s. Login to portal for details.",
            resource.getResourceType(),
            (event.getValue() / resource.getLimitValue()) * 100,
            event.getValue(),
            resource.getUnit()
        );

        List<String> phones = getCustomerPhones(resource.getCustomerId());
        for (String phone : phones) {
            twilioClient.sendSms(phone, message);
        }
    }
}
```

---

## ⚙️ CONFIGURATION

### Application Configuration

**File:** `src/main/resources/application.yml`

```yaml
# Resource Monitoring
monitoring:
  metrics:
    collection-interval: 60s  # Collect every minute
    retention-period: 90d     # Keep raw data for 90 days
    aggregation:
      enabled: true
      intervals: [hour, day, week, month]
  alerting:
    default-threshold-warning: 80%
    default-threshold-critical: 95%
    channels:
      email: true
      sms: false
      slack: true
  cost:
    default-currency: USD
    rounding: 0.01  # Round to cents
  optimization:
    enabled: true
    check-interval: 1d  # Check for optimizations daily
    min-savings-threshold: 10  # Only recommend if savings > $10

# Redis
redis:
  host: localhost
  port: 6379
  timeout: 2000ms
  resource-monitoring:
    keyspace: monitoring
    ttl: 1h

# Kafka
kafka:
  bootstrap-servers: localhost:9092
  topics:
    resource-usage-raw: resource.usage.raw
    resource-alerts: resource.alerts
    optimization-events: optimization.events
  consumer:
    group-id: monitoring-service
    auto-offset-reset: earliest
```

### Environment Variables

```bash
# Required
export DATABASE_URL=jdbc:postgresql://localhost:5432/bss
export REDIS_URL=redis://localhost:6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Optional
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=alerts@company.com
export SMTP_PASSWORD=password
export TWILIO_ACCOUNT_SID=ACxxxx
export TWILIO_AUTH_TOKEN=xxxx
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx
```

---

## 📅 IMPLEMENTATION TIMELINE

### Phase 1: Foundation (Weeks 1-2)
- [ ] Database schema design and migration
- [ ] Basic backend services (Metrics Collector, Threshold Engine)
- [ ] Redis setup for real-time caching
- [ ] Basic frontend dashboard skeleton

### Phase 2: Core Features (Weeks 3-4)
- [ ] Real-time metrics ingestion
- [ ] Threshold-based alerting
- [ ] Notification system (email, SMS, Slack)
- [ ] Frontend monitoring dashboard
- [ ] Real-time chart component

### Phase 3: Advanced Features (Weeks 5-6)
- [ ] Cost calculation engine
- [ ] Cost forecasting
- [ ] Optimization recommendations
- [ ] Alert management UI
- [ ] Customer resource configuration

### Phase 4: Polish & Testing (Week 7)
- [ ] Performance optimization
- [ ] Load testing
- [ ] UI/UX improvements
- [ ] Documentation
- [ ] Production deployment

---

## 📊 SUCCESS METRICS

### Technical Metrics
- **Latency**: Alert generation < 5 seconds from threshold breach
- **Accuracy**: 99.9% data accuracy for usage metrics
- **Uptime**: 99.95% availability of monitoring system
- **Scalability**: Support 10,000+ concurrent resources

### Business Metrics
- **Alert Response Time**: 50% reduction in response time to issues
- **Support Tickets**: 40% reduction in usage-related support tickets
- **Cost Optimization**: Average 15% cost reduction for customers
- **Proactive Alerts**: 90% of issues identified before customer impact

---

## 🔍 TESTING STRATEGY

### Unit Tests
```java
// Metrics Collector
@Test
public void testUsageIngestion() {
    UsageEvent event = createTestEvent();
    metricsService.processUsageEvent(event);

    verify(redisTemplate).opsForValue().set(anyString(), eq(event.getValue()), any());
    verify(kafkaTemplate).send(anyString(), any(CloudEvent.class));
}

// Threshold Engine
@Test
public void testThresholdWarning() {
    UsageEvent event = createUsageEvent(85); // 85% of limit
    CustomerResource resource = createResource(100, 80, 95);

    thresholdEngine.handleUsageEvent(event);

    Alert alert = alertRepository.findFirstByOrderByCreatedAtDesc();
    assertEquals(AlertType.THRESHOLD_WARNING, alert.getType());
    assertEquals(AlertSeverity.MEDIUM, alert.getSeverity());
}
```

### Integration Tests
```java
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MonitoringIntegrationTest {

    @Test
    public void testEndToEndAlertFlow() {
        // 1. Send usage event
        UsageEvent event = createTestEvent(96); // 96% of limit
        metricsCollectorService.processUsageEvent(event);

        // 2. Wait for alert generation
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<Alert> alerts = alertRepository.findByStatus("ACTIVE");
                assertEquals(1, alerts.size());
                assertEquals(AlertType.THRESHOLD_CRITICAL, alerts.get(0).getType());
            });

        // 3. Verify notification sent
        verify(notificationService).sendCriticalAlert(any(), any());
    }
}
```

### Load Testing
```yaml
# k6 load test
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 100 },  // Ramp up
    { duration: '5m', target: 100 },  // Stay at 100
    { duration: '2m', target: 200 },  // Ramp up
    { duration: '5m', target: 200 },  // Stay at 200
    { duration: '2m', target: 0 },    // Ramp down
  ],
};

export default function() {
  const payload = JSON.stringify({
    customerId: 'test-customer',
    resourceType: 'bandwidth',
    value: Math.random() * 1000,
    unit: 'GB'
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const response = http.post('http://localhost:8080/api/metrics/ingest', payload, params);
  check(response, { 'status is 200': (r) => r.status === 200 });
}
```

---

## 🐳 DOCKER DEPLOYMENT

### Docker Compose
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: bss
      POSTGRES_USER: bss
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  backend:
    build: ./backend
    depends_on:
      - postgres
      - redis
      - kafka
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/bss
      REDIS_URL: redis://redis:6379
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  frontend:
    build: ./frontend
    depends_on:
      - backend
    ports:
      - "3000:3000"
    environment:
      NUXT_PUBLIC_API_BASE: http://backend:8080/api

volumes:
  postgres_data:
  redis_data:
```

---

## 📚 DOCUMENTATION

### API Documentation

**OpenAPI Spec:** `docs/openapi-monitoring.yaml`

```yaml
openapi: 3.0.0
info:
  title: Resource Monitoring API
  version: 1.0.0
paths:
  /api/metrics/ingest:
    post:
      summary: Ingest usage metric
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UsageEvent'
      responses:
        200:
          description: Metric ingested successfully

  /api/monitoring/customers/{customerId}/resources:
    get:
      summary: Get customer resources
      parameters:
        - name: customerId
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: List of customer resources
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/CustomerResource'

components:
  schemas:
    UsageEvent:
      type: object
      required:
        - customerId
        - resourceType
        - value
        - unit
      properties:
        customerId:
          type: string
        resourceType:
          type: string
          enum: [bandwidth, storage, api_calls, cpu, memory]
        value:
          type: number
        unit:
          type: string
        timestamp:
          type: string
          format: date-time
```

---

## 🎯 NEXT STEPS

1. **Review this plan** with the team
2. **Set up development environment** (PostgreSQL, Redis, Kafka)
3. **Create database schema** and run migrations
4. **Implement backend services** (start with Metrics Collector)
5. **Build frontend dashboard** (basic version)
6. **Test with real data** (simulate usage events)
7. **Iterate and improve** based on feedback

**Ready to start implementing? Let's make it happen! 🚀**
