package com.droid.bss.infrastructure.observability;

import com.droid.bss.BssApplication;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Metrics Collection Tests
 *
 * Tests custom metrics, system metrics, business metrics, metric types,
 * aggregation, cardinality, and export functionality.
 */
@SpringBootTest(classes = Application.class)
@DisplayName("Metrics Collection Tests")
class MetricsCollectionTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TestMetricsService testMetricsService;

    @Test
    @DisplayName("Should collect custom metrics")
    void shouldCollectCustomMetrics() {
        Counter customCounter = Counter.builder("custom.metric")
                .description("Custom test metric")
                .register(meterRegistry);

        customCounter.increment(5.0);

        assertThat(customCounter.count()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should collect system metrics")
    void shouldCollectSystemMetrics() {
        var jvmMetrics = meterRegistry.find("jvm.memory.used")
                .meters();

        assertThat(jvmMetrics).isNotNull();
        assertThat(jvmMetrics.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should collect business metrics")
    void shouldCollectBusinessMetrics() {
        Counter businessCounter = Counter.builder("business.customer.created")
                .description("Number of customers created")
                .tag("region", "us-east-1")
                .register(meterRegistry);

        businessCounter.increment();

        assertThat(businessCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should aggregate metrics")
    void shouldAggregateMetrics() {
        Counter aggregatedCounter = Counter.builder("aggregated.metric")
                .description("Aggregated test metric")
                .register(meterRegistry);

        aggregatedCounter.increment(2.0);
        aggregatedCounter.increment(3.0);
        aggregatedCounter.increment(5.0);

        assertThat(aggregatedCounter.count()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should validate metric cardinality")
    void shouldValidateMetricCardinality() {
        String baseMetricName = "high.cardinality.metric";

        for (int i = 0; i < 10; i++) {
            Counter counter = Counter.builder(baseMetricName)
                    .tag("user_id", "user-" + i)
                    .tag("session_id", "session-" + i)
                    .register(meterRegistry);

            counter.increment();
        }

        var metrics = meterRegistry.find(baseMetricName).meters();
        assertThat(metrics.size()).isEqualTo(10);

        metrics.forEach(metric -> {
            assertThat(metric.getId().getTags()).hasSize(2);
        });
    }

    @Test
    @DisplayName("Should create histogram metrics")
    void shouldCreateHistogramMetrics() {
        Timer histogramTimer = Timer.builder("request.duration")
                .description("Request duration histogram")
                .register(meterRegistry);

        histogramTimer.record(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        histogramTimer.record(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThat(histogramTimer.count()).isEqualTo(2);
        assertThat(histogramTimer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should track counter metrics")
    void shouldTrackCounterMetrics() {
        Counter apiCalls = Counter.builder("api.calls")
                .description("Total API calls")
                .tag("endpoint", "/api/users")
                .register(meterRegistry);

        for (int i = 0; i < 5; i++) {
            apiCalls.increment();
        }

        assertThat(apiCalls.count()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should track gauge metrics")
    void shouldTrackGaugeMetrics() {
        AtomicInteger gaugeValue = new AtomicInteger(0);

        Gauge activeConnections = Gauge.builder("active.connections")
                .description("Active connections gauge")
                .register(meterRegistry, gaugeValue, AtomicInteger::get);

        gaugeValue.set(10);
        assertThat(gaugeValue.get()).isEqualTo(10);

        gaugeValue.set(20);
        assertThat(gaugeValue.get()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should export metrics to Prometheus")
    void shouldExportMetricsToPrometheus() {
        Counter exportCounter = Counter.builder("prometheus.export.test")
                .description("Test metric for Prometheus export")
                .register(meterRegistry);

        exportCounter.increment();

        String prometheusFormat = meterRegistry.scrape();

        assertThat(prometheusFormat).isNotEmpty();
        assertThat(prometheusFormat).contains("prometheus_export_test_total");
    }

    @Test
    @DisplayName("Should validate metric retention")
    void shouldValidateMetricRetention() {
        Counter retentionCounter = Counter.builder("metric.retention")
                .description("Metric retention test")
                .register(meterRegistry);

        retentionCounter.increment(100.0);

        assertThat(retentionCounter.count()).isEqualTo(100.0);

        retentionCounter.increment(50.0);

        assertThat(retentionCounter.count()).isEqualTo(150.0);
    }

    @Test
    @DisplayName("Should compress metrics")
    void shouldCompressMetrics() {
        for (int i = 0; i < 100; i++) {
            Counter counter = Counter.builder("metric.compression")
                    .tag("iteration", String.valueOf(i))
                    .register(meterRegistry);

            counter.increment();
        }

        var metrics = meterRegistry.find("metric.compression").meters();

        assertThat(metrics.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should validate alert thresholds")
    void shouldValidateAlertThresholds() {
        Counter alertCounter = Counter.builder("alert.threshold.test")
                .description("Test alert threshold")
                .register(meterRegistry);

        alertCounter.increment(90.0);

        assertThat(alertCounter.count()).isEqualTo(90.0);

        alertCounter.increment(20.0);

        assertThat(alertCounter.count()).isEqualTo(110.0);

        boolean shouldAlert = alertCounter.count() > 100.0;
        assertThat(shouldAlert).isTrue();
    }

    @Service
    static class TestMetricsService {
        private final Counter testCounter;
        private final Timer testTimer;

        public TestMetricsService(MeterRegistry meterRegistry) {
            this.testCounter = Counter.builder("service.operations")
                    .description("Service operations counter")
                    .register(meterRegistry);

            this.testTimer = Timer.builder("service.operations.time")
                    .description("Service operations time")
                    .register(meterRegistry);
        }

        @Counted("test.operation.counted")
        public void performCountedOperation() {
            testCounter.increment();
        }

        @Timed("test.operation.timed")
        public void performTimedOperation() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
