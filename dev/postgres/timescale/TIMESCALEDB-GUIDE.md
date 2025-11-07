# TimescaleDB - Time-Series Analytics Platform

## Overview

TimescaleDB is a powerful PostgreSQL extension that enables time-series data storage, real-time analytics, and forecasting. It provides automatic compression, data retention, and continuous aggregates for high-performance analytics on time-based data.

## Value Proposition

### Why TimescaleDB?

1. **Real-Time Analytics**: Continuous aggregates provide real-time dashboards
2. **Data Compression**: Automatic compression reduces storage by 90%+
3. **Query Performance**: 10-100x faster queries on time-series data
4. **Scalability**: Handles millions of data points per second
5. **SQL Interface**: No need to learn new query languages
6. **PostgreSQL Compatible**: Works with existing PostgreSQL ecosystem

### Business Benefits

- **Customer Insights**: Track user behavior, login patterns, session duration
- **Revenue Analytics**: Real-time revenue, profit, and growth metrics
- **Fraud Detection**: Identify suspicious payment patterns
- **System Monitoring**: Track performance metrics across services
- **Predictive Analytics**: Forecast demand, churn, and trends
- **Operational Dashboards**: Real-time business intelligence

## Architecture

### TimescaleDB Components

```
┌─────────────────────────────────────────┐
│         Application Layer                │
│  - Dashboards (Grafana)                 │
│  - Analytics APIs                       │
│  - Business Intelligence                │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         PostgreSQL Database              │
│  ┌─────────────────────────────────────┐ │
│  │         TimescaleDB Extension       │ │
│  │                                     │ │
│  │  ┌────────────────────────────────┐ │ │
│  │  │   Hypertables (Time-series)    │ │ │
│  │  │  - customer_metrics            │ │ │
│  │  │  - order_metrics               │ │ │
│  │  │  - payment_metrics             │ │ │
│  │  │  - revenue_metrics             │ │ │
│  │  └────────────────────────────────┘ │ │
│  │                                     │ │
│  │  ┌────────────────────────────────┐ │ │
│  │  │  Continuous Aggregates         │ │ │
│  │  │  - revenue_daily               │ │ │
│  │  │  - customer_activity_hourly    │ │ │
│  │  │  - payment_status_daily        │ │ │
│  │  └────────────────────────────────┘ │ │
│  │                                     │ │
│  │  ┌────────────────────────────────┐ │ │
│  │  │  Policies                      │ │ │
│  │  │  - Compression                 │ │ │
│  │  │  - Retention                   │ │ │
│  │  │  - Refresh                     │ │ │
│  │  └────────────────────────────────┘ │ │
│  └─────────────────────────────────────┘ │
│                                          │
│  - Connection Pooling (PgBouncer)        │
│  - Logical Replication                    │
│  - RLS (Row Level Security)               │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│        Kafka Event Stream                 │
│  - Customer events                       │
│  - Order events                          │
│  - Payment events                        │
│  - System metrics                        │
└─────────────────────────────────────────┘
```

### Hypertable Structure

A hypertable is a TimescaleDB table that automatically partitions data by time:

```
┌─────────────────────────────────────────┐
│    Hypertable: customer_metrics          │
│                                           │
│  ┌──────────────┬──────────────┐        │
│  │  Chunk 1     │  Chunk 2     │        │
│  │  Day 1-2     │  Day 3-4     │        │
│  ├──────────────┼──────────────┤        │
│  │  Chunk 3     │  Chunk 4     │        │
│  │  Day 5-6     │  Day 7-8     │        │
│  └──────────────┴──────────────┘        │
│                                           │
│  - Time-based partitioning                │
│  - Automatic chunk management            │
│  - Parallel query execution              │
└─────────────────────────────────────────┘
```

## Implementation

### 1. Setup TimescaleDB

```bash
# Run the setup script
./dev/postgres/timescale/setup-timescaledb.sh

# The script will:
# 1. Install TimescaleDB extension
# 2. Create hypertables for time-series data
# 3. Set up continuous aggregates
# 4. Configure compression and retention policies
# 5. Create sample data for testing
```

### 2. Application Integration

#### Spring Boot Configuration

```java
@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
public class TimescaleConfig {

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

#### Customer Metrics Repository

```java
@Repository
public class CustomerMetricsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Insert customer activity metric
    public void recordCustomerMetric(UUID customerId, String metricName,
                                    Double metricValue, Map<String, Object> labels) {
        String sql = "INSERT INTO customer_metrics (time, customer_id, metric_name, " +
                     "metric_value, labels) VALUES (NOW(), ?, ?, ?, ?::jsonb)";

        jdbcTemplate.update(sql, customerId, metricName, metricValue,
                           new JSONObject(labels).toString());
    }

    // Get customer activity for time range
    public List<CustomerMetric> getCustomerMetrics(UUID customerId,
                                                   Instant startTime,
                                                   Instant endTime) {
        String sql = "SELECT time, customer_id, metric_name, metric_value, labels " +
                     "FROM customer_metrics " +
                     "WHERE customer_id = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "ORDER BY time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{customerId, Timestamp.from(startTime), Timestamp.from(endTime)},
            (rs, rowNum) -> {
                CustomerMetric metric = new CustomerMetric();
                metric.setTime(rs.getTimestamp("time").toInstant());
                metric.setCustomerId(rs.getObject("customer_id", UUID.class));
                metric.setMetricName(rs.getString("metric_name"));
                metric.setMetricValue(rs.getDouble("metric_value"));
                String labelsJson = rs.getString("labels");
                if (labelsJson != null) {
                    metric.setLabels(JSON.parseObject(labelsJson, Map.class));
                }
                return metric;
            });
    }

    // Get aggregate metrics
    public List<MetricAggregate> getMetricAggregates(String metricName, String period,
                                                     Instant startTime, Instant endTime) {
        String sql = "SELECT time_bucket(?, time) AS bucket, " +
                     "AVG(metric_value) AS avg_value, " +
                     "MIN(metric_value) AS min_value, " +
                     "MAX(metric_value) AS max_value, " +
                     "COUNT(*) AS count " +
                     "FROM customer_metrics " +
                     "WHERE metric_name = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "GROUP BY bucket " +
                     "ORDER BY bucket DESC";

        String bucketInterval = getBucketInterval(period);

        return jdbcTemplate.query(sql,
            new Object[]{bucketInterval, metricName,
                        Timestamp.from(startTime), Timestamp.from(endTime)},
            (rs, rowNum) -> {
                MetricAggregate aggregate = new MetricAggregate();
                aggregate.setBucket(rs.getTimestamp("bucket").toInstant());
                aggregate.setAvgValue(rs.getDouble("avg_value"));
                aggregate.setMinValue(rs.getDouble("min_value"));
                aggregate.setMaxValue(rs.getDouble("max_value"));
                aggregate.setCount(rs.getLong("count"));
                return aggregate;
            });
    }

    private String getBucketInterval(String period) {
        return switch (period.toLowerCase()) {
            case "minute" -> "1 minute";
            case "hour" -> "1 hour";
            case "day" -> "1 day";
            case "week" -> "1 week";
            case "month" -> "1 month";
            default -> "1 day";
        };
    }
}
```

#### Revenue Analytics Service

```java
@Service
@Slf4j
public class RevenueAnalyticsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public RevenueSummary getRevenueSummary(int days) {
        String sql = "SELECT " +
                     "day, " +
                     "total_revenue, " +
                     "total_profit, " +
                     "total_orders, " +
                     "avg_order_value " +
                     "FROM revenue_daily " +
                     "WHERE day > NOW() - INTERVAL '? days' " +
                     "ORDER BY day DESC";

        List<RevenueDaily> dailyData = jdbcTemplate.query(sql,
            new Object[]{days},
            (rs, rowNum) -> {
                RevenueDaily daily = new RevenueDaily();
                daily.setDay(rs.getTimestamp("day").toInstant());
                daily.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                daily.setTotalProfit(rs.getBigDecimal("total_profit"));
                daily.setTotalOrders(rs.getLong("total_orders"));
                daily.setAvgOrderValue(rs.getBigDecimal("avg_order_value"));
                return daily;
            });

        RevenueSummary summary = new RevenueSummary();
        summary.setDailyData(dailyData);
        summary.setTotalRevenue(calculateTotalRevenue(dailyData));
        summary.setTotalProfit(calculateTotalProfit(dailyData));
        summary.setGrowthRate(calculateGrowthRate(dailyData));
        return summary;
    }

    public List<RevenueByRegion> getRevenueByRegion(int days) {
        String sql = "SELECT " +
                     "time_bucket('1 day', time) AS day, " +
                     "region, " +
                     "SUM(revenue) AS total_revenue, " +
                     "SUM(profit) AS total_profit, " +
                     "SUM(orders_count) AS total_orders " +
                     "FROM revenue_metrics " +
                     "WHERE time > NOW() - INTERVAL '? days' " +
                     "GROUP BY day, region " +
                     "ORDER BY day DESC, total_revenue DESC";

        return jdbcTemplate.query(sql,
            new Object[]{days},
            (rs, rowNum) -> {
                RevenueByRegion revenue = new RevenueByRegion();
                revenue.setDay(rs.getTimestamp("day").toInstant());
                revenue.setRegion(rs.getString("region"));
                revenue.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                revenue.setTotalProfit(rs.getBigDecimal("total_profit"));
                revenue.setTotalOrders(rs.getLong("total_orders"));
                return revenue;
            });
    }

    public List<RevenueForecast> forecastRevenue(int daysToForecast) {
        String sql = "SELECT " +
                     "time_bucket('1 day', time) AS day, " +
                     "AVG(revenue) AS avg_revenue, " +
                     "AVG(profit) AS avg_profit " +
                     "FROM revenue_metrics " +
                     "WHERE time > NOW() - INTERVAL '30 days' " +
                     "GROUP BY day " +
                     "ORDER BY day";

        List<RevenueDataPoint> historicalData = jdbcTemplate.query(sql,
            (rs, rowNum) -> {
                RevenueDataPoint point = new RevenueDataPoint();
                point.setDay(rs.getTimestamp("day").toInstant());
                point.setRevenue(rs.getBigDecimal("avg_revenue").doubleValue());
                point.setProfit(rs.getBigDecimal("avg_profit").doubleValue());
                return point;
            });

        // Simple linear regression for forecasting
        return performForecast(historicalData, daysToForecast);
    }

    private List<RevenueForecast> performForecast(List<RevenueDataPoint> historical,
                                                  int days) {
        // Implementation of forecasting algorithm
        // This is a simplified version - in production, use libraries like
        // Apache Spark MLlib or Python-based forecasting
        log.info("Forecasting revenue for {} days", days);
        return List.of(); // Return forecasted data
    }
}
```

#### Payment Fraud Detection

```java
@Service
public class FraudDetectionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<PaymentAnomaly> detectAnomalies(Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "payment_id, " +
                     "customer_id, " +
                     "amount, " +
                     "fraud_score, " +
                     "time, " +
                     "payment_method " +
                     "FROM payment_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "AND fraud_score > 80 " +
                     "ORDER BY fraud_score DESC, time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{Timestamp.from(startTime), Timestamp.from(endTime)},
            (rs, rowNum) -> {
                PaymentAnomaly anomaly = new PaymentAnomaly();
                anomaly.setPaymentId(rs.getObject("payment_id", UUID.class));
                anomaly.setCustomerId(rs.getObject("customer_id", UUID.class));
                anomaly.setAmount(rs.getBigDecimal("amount"));
                anomaly.setFraudScore(rs.getDouble("fraud_score"));
                anomaly.setTimestamp(rs.getTimestamp("time").toInstant());
                anomaly.setPaymentMethod(rs.getString("payment_method"));
                return anomaly;
            });
    }

    public FraudPattern analyzeFraudPatterns(Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "payment_method, " +
                     "status, " +
                     "COUNT(*) as count, " +
                     "AVG(fraud_score) as avg_fraud_score, " +
                     "SUM(amount) as total_amount " +
                     "FROM payment_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "GROUP BY payment_method, status " +
                     "ORDER BY avg_fraud_score DESC";

        List<FraudPatternResult> results = jdbcTemplate.query(sql,
            new Object[]{Timestamp.from(startTime), Timestamp.from(endTime)},
            (rs, rowNum) -> {
                FraudPatternResult result = new FraudPatternResult();
                result.setPaymentMethod(rs.getString("payment_method"));
                result.setStatus(rs.getString("status"));
                result.setCount(rs.getLong("count"));
                result.setAvgFraudScore(rs.getDouble("avg_fraud_score"));
                result.setTotalAmount(rs.getBigDecimal("total_amount"));
                return result;
            });

        FraudPattern pattern = new FraudPattern();
        pattern.setPatterns(results);
        pattern.setTotalPayments(results.stream()
            .mapToLong(FraudPatternResult::getCount).sum());
        pattern.setAvgFraudScore(results.stream()
            .mapToDouble(FraudPatternResult::getAvgFraudScore).average().orElse(0.0));
        return pattern;
    }
}
```

#### System Performance Monitoring

```java
@Service
public class SystemMetricsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<SystemMetricPoint> getSystemMetrics(String serviceName,
                                                    Instant startTime,
                                                    Instant endTime) {
        String sql = "SELECT " +
                     "time, " +
                     "service_name, " +
                     "cpu_usage, " +
                     "memory_usage, " +
                     "request_rate, " +
                     "error_rate, " +
                     "latency_p99 " +
                     "FROM system_metrics " +
                     "WHERE service_name = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "ORDER BY time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{serviceName, Timestamp.from(startTime), Timestamp.from(endTime)},
            (rs, rowNum) -> {
                SystemMetricPoint point = new SystemMetricPoint();
                point.setTime(rs.getTimestamp("time").toInstant());
                point.setServiceName(rs.getString("service_name"));
                point.setCpuUsage(rs.getDouble("cpu_usage"));
                point.setMemoryUsage(rs.getDouble("memory_usage"));
                point.setRequestRate(rs.getDouble("request_rate"));
                point.setErrorRate(rs.getDouble("error_rate"));
                point.setLatencyP99(rs.getDouble("latency_p99"));
                return point;
            });
    }

    public SystemHealthStatus getSystemHealthStatus() {
        String sql = "SELECT " +
                     "service_name, " +
                     "AVG(cpu_usage) as avg_cpu, " +
                     "AVG(memory_usage) as avg_memory, " +
                     "AVG(error_rate) as avg_error_rate, " +
                     "MAX(latency_p99) as max_latency " +
                     "FROM system_metrics " +
                     "WHERE time > NOW() - INTERVAL '1 hour' " +
                     "GROUP BY service_name";

        List<SystemHealthData> healthData = jdbcTemplate.query(sql,
            (rs, rowNum) -> {
                SystemHealthData data = new SystemHealthData();
                data.setServiceName(rs.getString("service_name"));
                data.setAvgCpu(rs.getDouble("avg_cpu"));
                data.setAvgMemory(rs.getDouble("avg_memory"));
                data.setAvgErrorRate(rs.getDouble("avg_error_rate"));
                data.setMaxLatency(rs.getDouble("max_latency"));
                return data;
            });

        SystemHealthStatus status = new SystemHealthStatus();
        status.setServices(healthData);
        status.setOverallHealth(calculateOverallHealth(healthData));
        return status;
    }
}
```

### 3. Kafka Integration

#### Real-Time Metrics Producer

```java
@Component
public class MetricsProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void recordCustomerMetric(UUID customerId, String metricName,
                                    Double metricValue) {
        CustomerMetricEvent event = CustomerMetricEvent.builder()
            .customerId(customerId)
            .metricName(metricName)
            .metricValue(metricValue)
            .timestamp(Instant.now())
            .labels(Map.of("source", "application", "version", "1.0"))
            .build();

        kafkaTemplate.send("bss.customer.metrics", event);
    }

    public void recordOrderMetric(UUID orderId, UUID customerId,
                                 String status, BigDecimal totalAmount) {
        OrderMetricEvent event = OrderMetricEvent.builder()
            .orderId(orderId)
            .customerId(customerId)
            .status(status)
            .totalAmount(totalAmount)
            .timestamp(Instant.now())
            .build();

        kafkaTemplate.send("bss.order.metrics", event);
    }

    public void recordPaymentMetric(UUID paymentId, UUID orderId,
                                   UUID customerId, BigDecimal amount,
                                   String status, Double fraudScore) {
        PaymentMetricEvent event = PaymentMetricEvent.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .status(status)
            .fraudScore(fraudScore)
            .timestamp(Instant.now())
            .build();

        kafkaTemplate.send("bss.payment.metrics", event);
    }
}
```

#### Kafka Consumer to TimescaleDB

```java
@Component
public class MetricsConsumer {

    @Autowired
    private CustomerMetricsRepository metricsRepository;

    @Autowired
    private OrderMetricsRepository orderRepository;

    @Autowired
    private PaymentMetricsRepository paymentRepository;

    @KafkaListener(topics = "bss.customer.metrics", groupId = "timescale-ingest")
    public void handleCustomerMetric(CustomerMetricEvent event) {
        try {
            metricsRepository.recordCustomerMetric(
                event.getCustomerId(),
                event.getMetricName(),
                event.getMetricValue(),
                event.getLabels()
            );
        } catch (Exception e) {
            log.error("Failed to record customer metric", e);
            // Send to DLQ
        }
    }

    @KafkaListener(topics = "bss.order.metrics", groupId = "timescale-ingest")
    public void handleOrderMetric(OrderMetricEvent event) {
        try {
            orderRepository.recordOrderMetric(
                event.getOrderId(),
                event.getCustomerId(),
                event.getStatus(),
                event.getTotalAmount()
            );
        } catch (Exception e) {
            log.error("Failed to record order metric", e);
        }
    }

    @KafkaListener(topics = "bss.payment.metrics", groupId = "timescale-ingest")
    public void handlePaymentMetric(PaymentMetricEvent event) {
        try {
            paymentRepository.recordPaymentMetric(
                event.getPaymentId(),
                event.getOrderId(),
                event.getCustomerId(),
                event.getAmount(),
                event.getStatus(),
                event.getFraudScore()
            );
        } catch (Exception e) {
            log.error("Failed to record payment metric", e);
        }
    }
}
```

## Query Examples

### Basic Time-Series Queries

```sql
-- Get customer activity for last 7 days
SELECT time, metric_name, metric_value
FROM customer_metrics
WHERE customer_id = '550e8400-e29b-41d4-a716-446655440000'
  AND time > NOW() - INTERVAL '7 days'
ORDER BY time DESC;

-- Get daily revenue for last 30 days
SELECT day, total_revenue, total_profit, total_orders
FROM revenue_daily
WHERE day > NOW() - INTERVAL '30 days'
ORDER BY day;

-- Get payment anomalies (high fraud score)
SELECT payment_id, customer_id, amount, fraud_score, time
FROM payment_metrics
WHERE fraud_score > 90
  AND time > NOW() - INTERVAL '24 hours'
ORDER BY fraud_score DESC;
```

### Advanced Analytics

```sql
-- Customer activity heatmap (hour of day vs day of week)
SELECT
    EXTRACT(hour FROM time) AS hour_of_day,
    EXTRACT(dow FROM time) AS day_of_week,
    COUNT(*) AS activity_count
FROM customer_metrics
WHERE time > NOW() - INTERVAL '30 days'
GROUP BY hour_of_day, day_of_week
ORDER BY activity_count DESC;

-- Top customers by activity
SELECT customer_id, COUNT(*) AS activity_count
FROM customer_metrics
WHERE time > NOW() - INTERVAL '7 days'
GROUP BY customer_id
ORDER BY activity_count DESC
LIMIT 10;

-- Revenue trend by region
SELECT
    time_bucket('1 day', time) AS day,
    region,
    SUM(revenue) AS daily_revenue,
    SUM(profit) AS daily_profit
FROM revenue_metrics
WHERE time > NOW() - INTERVAL '30 days'
GROUP BY day, region
ORDER BY day DESC, daily_revenue DESC;

-- Fraud detection: unusual payment amounts
SELECT
    customer_id,
    AVG(amount) AS avg_amount,
    STDDEV(amount) AS stddev_amount,
    COUNT(*) AS payment_count
FROM payment_metrics
WHERE time > NOW() - INTERVAL '7 days'
GROUP BY customer_id
HAVING COUNT(*) > 5
ORDER BY stddev_amount DESC
LIMIT 10;

-- System performance trends
SELECT
    service_name,
    time_bucket('1 hour', time) AS hour,
    AVG(cpu_usage) AS avg_cpu,
    MAX(cpu_usage) AS max_cpu,
    AVG(memory_usage) AS avg_memory
FROM system_metrics
WHERE time > NOW() - INTERVAL '24 hours'
GROUP BY service_name, hour
ORDER BY hour DESC, avg_cpu DESC;
```

### Continuous Aggregate Queries

```sql
-- Use continuous aggregates for fast queries
-- These are pre-computed and updated automatically

-- Revenue summary (last 30 days)
SELECT day, total_revenue, total_profit, total_orders, avg_order_value
FROM revenue_daily
WHERE day > NOW() - INTERVAL '30 days'
ORDER BY day;

-- Customer activity hourly
SELECT hour, customer_id, activity_count, avg_metric_value
FROM customer_activity_hourly
WHERE hour > NOW() - INTERVAL '24 hours'
ORDER BY hour DESC, activity_count DESC
LIMIT 100;

-- Payment status breakdown
SELECT day, status, payment_count, total_amount, avg_fraud_score
FROM payment_status_daily
WHERE day > NOW() - INTERVAL '7 days'
ORDER BY day DESC, payment_count DESC;
```

## Performance Optimization

### 1. Compression Policy

TimescaleDB automatically compresses data after a certain period:

```sql
-- Compress data after 7 days, keep for 2 years
SELECT add_compression_policy('customer_metrics', INTERVAL '7 days');
SELECT add_compression_policy('order_metrics', INTERVAL '7 days');
SELECT add_compression_policy('payment_metrics', INTERVAL '7 days');
SELECT add_compression_policy('revenue_metrics', INTERVAL '7 days');
```

### 2. Retention Policy

Automatically delete old data:

```sql
-- Keep data for 2 years
SELECT add_retention_policy('customer_metrics', INTERVAL '2 years');
SELECT add_retention_policy('order_metrics', INTERVAL '2 years');
SELECT add_retention_policy('payment_metrics', INTERVAL '2 years');
SELECT add_retention_policy('revenue_metrics', INTERVAL '2 years');
```

### 3. Index Optimization

```sql
-- Create indexes for common queries
CREATE INDEX CONCURRENTLY idx_customer_metrics_time_name
    ON customer_metrics (time DESC, metric_name);

CREATE INDEX CONCURRENTLY idx_order_metrics_time_customer
    ON order_metrics (time DESC, customer_id);

CREATE INDEX CONCURRENTLY idx_payment_metrics_time_status
    ON payment_metrics (time DESC, status);
```

### 4. Data Tiering

```sql
-- Move old chunks to slower storage
SELECT move_chunk(
    'customer_metrics_1'::regclass,
    'data/old_chunks',
    'data/new_chunks',
    1024 * 1024 * 100,  -- 100MB per chunk
    'retry'
);
```

## Monitoring & Alerting

### 1. Hypertable Health

```sql
-- Check hypertable status
SELECT
    hypertable_name,
    chunk_count,
    total_chunks,
    compression_ratio,
    total_data_size,
    total_index_size
FROM timescaledb_information.hypertables
WHERE hypertable_schema = 'public';

-- Check chunk count and size
SELECT
    hypertable_name,
    chunk_name,
    range_start,
    range_end,
    chunk_size
FROM timescaledb_information.chunks
WHERE hypertable_schema = 'public'
ORDER BY chunk_name;
```

### 2. Compression Stats

```sql
-- Check compression statistics
SELECT
    hypertable_name,
    total_chunks,
    compressed_chunks,
    compression_ratio
FROM timescaledb_information.compression_stats
WHERE hypertable_schema = 'public';

-- Check compression settings
SELECT
    hypertable_name,
    after_compression_size,
    before_compression_size
FROM timescaledb_information.compression_settings
WHERE hypertable_schema = 'public';
```

### 3. Background Jobs

```sql
-- Check background jobs status
SELECT
    job_id,
    application_name,
    schedule_interval,
    next_run,
    state
FROM timescaledb_information.job_stats
WHERE application_name LIKE '%compression%'
   OR application_name LIKE '%retention%'
   OR application_name LIKE '%continuous_aggregate%';
```

## Grafana Integration

### Dashboard Configuration

Create a Grafana dashboard for time-series analytics:

```json
{
  "dashboard": {
    "title": "BSS Time-Series Analytics",
    "panels": [
      {
        "title": "Daily Revenue",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(revenue_daily_total_revenue) by (day)",
            "legendFormat": "Revenue"
          }
        ]
      },
      {
        "title": "Customer Activity",
        "type": "heatmap",
        "targets": [
          {
            "expr": "customer_activity_hourly_activity_count",
            "legendFormat": "Activity Count"
          }
        ]
      },
      {
        "title": "Payment Status Distribution",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum(payment_status_daily_payment_count) by (status)",
            "legendFormat": "{{status}}"
          }
        ]
      }
    ]
  }
}
```

### Prometheus Metrics

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'timescaledb'
    static_configs:
      - targets: ['localhost:9187']
    metrics_path: /metrics
    scrape_interval: 30s
```

## Advanced Features

### 1. Distributed Hypertables (Multi-Node)

```sql
-- Create distributed hypertable
SELECT create_distributed_hypertable(
    'system_metrics',
    'time',
    'service_name',
    number_of_replicas => 2
);
```

### 2. Data Import/Export

```sql
-- Export to CSV
COPY (
    SELECT * FROM revenue_daily
    WHERE day > NOW() - INTERVAL '30 days'
) TO '/tmp/revenue_export.csv' WITH (FORMAT csv, HEADER true);

-- Import from CSV
COPY customer_metrics (time, customer_id, metric_name, metric_value, labels)
FROM '/tmp/customer_metrics.csv' WITH (FORMAT csv, HEADER true);
```

### 3. SQL Functions

```sql
-- Custom function to calculate growth rate
CREATE OR REPLACE FUNCTION calculate_growth_rate(
    metric_name TEXT,
    days INTEGER DEFAULT 30
) RETURNS TABLE(
    day TIMESTAMPTZ,
    value DOUBLE PRECISION,
    growth_rate DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    WITH data AS (
        SELECT
            time_bucket('1 day', time) AS day,
            AVG(metric_value) AS value
        FROM customer_metrics
        WHERE metric_name = calculate_growth_rate.metric_name
          AND time > NOW() - INTERVAL '1 day' * days
        GROUP BY day
        ORDER BY day
    )
    SELECT
        d.day,
        d.value,
        (d.value - LAG(d.value) OVER (ORDER BY day)) / LAG(d.value) OVER (ORDER BY day) * 100
    FROM data d;
END;
$$ LANGUAGE plpgsql;
```

## Best Practices

### 1. Data Modeling

**Do:**
- Use meaningful metric names
- Include labels for filtering and grouping
- Keep chunk size reasonable (1-2GB per chunk)
- Use appropriate data types (DOUBLE PRECISION for metrics, TIMESTAMPTZ for time)
- Create indexes on frequently queried columns

**Don't:**
- Use overly granular time intervals
- Store too much data in a single row
- Forget to create indexes
- Use TEXT for metric values (use appropriate numeric types)

### 2. Query Performance

**Do:**
- Use continuous aggregates for common aggregations
- Apply WHERE clauses on time range first
- Use LIMIT for large result sets
- Monitor query performance with EXPLAIN ANALYZE

**Don't:**
- SELECT * on large tables
- Query without time range filters
- Ignore chunk boundaries in queries

### 3. Compression & Retention

**Do:**
- Enable compression for old data
- Set appropriate retention policies
- Monitor compression ratios
- Test compression on sample data

**Don't:**
- Compress very recent data
- Set retention too short for compliance
- Ignore storage costs

### 4. Continuous Aggregates

**Do:**
- Create continuous aggregates for frequent queries
- Use appropriate refresh policies
- Monitor aggregate freshness
- Test aggregate performance

**Don't:**
- Create too many aggregates (each has overhead)
- Set refresh too frequent (wastes resources)
- Use aggregates for highly dynamic data

## Migration Checklist

- [ ] Install TimescaleDB extension
- [ ] Create hypertables for all time-series data
- [ ] Set up continuous aggregates
- [ ] Configure compression policies
- [ ] Set retention policies
- [ ] Create indexes
- [ ] Set up Kafka producers
- [ ] Set up Kafka consumers
- [ ] Create Grafana dashboard
- [ ] Set up monitoring and alerting
- [ ] Test query performance
- [ ] Document business use cases
- [ ] Train development team
- [ ] Create runbooks
- [ ] Set up automated backups

## Troubleshooting

### Issue 1: Slow Queries

**Symptoms:**
- Queries taking longer than expected
- High CPU usage

**Solution:**
```sql
-- Check query plan
EXPLAIN ANALYZE
SELECT * FROM customer_metrics
WHERE time > NOW() - INTERVAL '7 days'
ORDER BY time DESC;

-- Add index if missing
CREATE INDEX CONCURRENTLY idx_customer_metrics_time
    ON customer_metrics (time DESC);
```

### Issue 2: High Disk Usage

**Symptoms:**
- Database disk space growing rapidly

**Solution:**
```sql
-- Check chunk sizes
SELECT
    chunk_name,
    chunk_size
FROM timescaledb_information.chunks
WHERE hypertable_name = 'customer_metrics'
ORDER BY chunk_name DESC;

-- Manually compress old chunks
SELECT
    compress_chunk(i, if_compressed => true)
FROM show_chunks('customer_metrics') i
WHERE i < NOW() - INTERVAL '7 days';
```

### Issue 3: Continuous Aggregate Not Refreshing

**Symptoms:**
- Aggregate data is stale
- Real-time dashboard not updating

**Solution:**
```sql
-- Check refresh policy
SELECT * FROM timescaledb_information.continuous_aggregate_policies
WHERE view_name = 'revenue_daily';

-- Manually refresh aggregate
CALL refresh_continuous_aggregate('revenue_daily', NULL, NOW());

-- Check job status
SELECT * FROM timescaledb_information.job_stats
WHERE application_name LIKE '%continuous_aggregate%';
```

## References

- [TimescaleDB Documentation](https://docs.timescale.com/)
- [PostgreSQL Time-Series](https://www.postgresql.org/docs/current/sql-createtable.html)
- [Timescale Best Practices](https://docs.timescale.com/use-timescale/latest/)
- [Grafana Time-Series](https://grafana.com/docs/grafana/latest/panels-visualizations/visualizations/time-series/)

## Support

For TimescaleDB issues:
1. Check logs: `docker logs bss-postgres`
2. Monitor metrics: `https://grafana.bss.local`
3. Query hypertables: `timescaledb_information.hypertables`
4. Contact: db-team@company.com
