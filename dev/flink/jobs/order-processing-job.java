/*
 * BSS Order Processing Job - Apache Flink
 * Real-time order analysis with complex event processing
 */

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OrderProcessingJob {

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure parallelism and checkpointing
        env.setParallelism(2);
        env.enableCheckpointing(10000); // 10 seconds checkpoint

        // Kafka source for order events
        KafkaSource<String> source = KafkaSource.<String>builder()
            .setBootstrapServers("kafka-1:9092,kafka-2:9092,kafka-3:9092")
            .setTopics("bss.order.events")
            .setGroupId("flink-order-processor")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema<>(Map.class))
            .build();

        // Create data stream with watermarks
        DataStream<Map<String, Object>> orderEvents = env
            .fromSource(source, WatermarkStrategy.<Map<String, Object>>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis()), "Order Events")
            .name("Order Kafka Source");

        // Order status tracking
        DataStream<Tuple5<String, String, Double, Long, String>> orderAnalytics = orderEvents
            .filter(new OrderFilterFunction())
            .keyBy(event -> event.get("customerId"))
            .window(SlidingEventTimeWindows.of(Time.of(10, TimeUnit.MINUTES), Time.of(1, TimeUnit.MINUTES)))
            .aggregate(new OrderAggregateFunction())
            .name("Order Windowed Analytics");

        // Send to analytics topic
        KafkaSink<String> sink = KafkaSink.<String>builder()
            .setBootstrapServers("kafka-1:9092,kafka-2:9092,kafka-3:9092")
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic("bss.analytics.events")
                .setValueSerializationSchema(new JsonSerializationSchema())
                .build())
            .build();

        orderAnalytics
            .map(event -> {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "order.analytics.flink");
                result.put("source", "urn:bss:flink:order-processor");
                result.put("customerId", event.f0);
                result.put("status", event.f1);
                result.put("totalAmount", event.f2);
                result.put("orderCount", event.f3);
                result.put("window", event.f4);
                result.put("timestamp", System.currentTimeMillis());
                return result;
            })
            .sinkTo(sink)
            .name("Order Analytics to Kafka");

        // Fraud detection stream
        DataStream<Map<String, Object>> fraudAlerts = orderEvents
            .filter(new FraudDetectionFunction())
            .map(event -> {
                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "fraud.alert");
                alert.put("source", "urn:bss:flink:fraud-detection");
                alert.put("orderId", event.get("orderId"));
                alert.put("customerId", event.get("customerId"));
                alert.put("amount", event.get("amount"));
                alert.put("reason", "High value order from new customer");
                alert.put("timestamp", System.currentTimeMillis());
                return alert;
            })
            .name("Fraud Detection");

        // Send fraud alerts
        KafkaSink<String> fraudSink = KafkaSink.<String>builder()
            .setBootstrapServers("kafka-1:9092,kafka-2:9092,kafka-3:9092")
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic("bss.fraud.alerts")
                .setValueSerializationSchema(new JsonSerializationSchema())
                .build())
            .build();

        fraudAlerts.sinkTo(fraudSink).name("Fraud Alerts to Kafka");

        // Execute the job
        env.execute("BSS Order Processing - Flink Job");
    }

    // Filter valid orders
    public static class OrderFilterFunction implements FilterFunction<Map<String, Object>> {
        @Override
        public boolean filter(Map<String, Object> event) {
            String status = (String) event.get("status");
            return status != null && !status.equals("CANCELLED");
        }
    }

    // Fraud detection
    public static class FraudDetectionFunction implements FilterFunction<Map<String, Object>> {
        @Override
        public boolean filter(Map<String, Object> event) {
            double amount = ((Number) event.getOrDefault("amount", 0)).doubleValue();
            String status = (String) event.getOrDefault("status", "");
            // Flag high-value orders from new customers
            return amount > 10000.0 && status.equals("CREATED");
        }
    }

    // Aggregate order data
    public static class OrderAggregateFunction implements
        org.apache.flink.api.common.functions.AggregateFunction<Map<String, Object>, OrderAccumulator, Tuple5<String, String, Double, Long, String>> {

        @Override
        public OrderAccumulator createAccumulator() {
            return new OrderAccumulator();
        }

        @Override
        public OrderAccumulator add(Map<String, Object> event, OrderAccumulator acc) {
            acc.customerId = (String) event.get("customerId");
            acc.status = (String) event.get("status");
            acc.totalAmount += ((Number) event.getOrDefault("amount", 0)).doubleValue();
            acc.orderCount++;
            return acc;
        }

        @Override
        public Tuple5<String, String, Double, Long, String> getResult(OrderAccumulator acc) {
            return Tuple5.of(acc.customerId, acc.status, acc.totalAmount, acc.orderCount, "10min-sliding");
        }

        @Override
        public OrderAccumulator merge(OrderAccumulator a, OrderAccumulator b) {
            a.totalAmount += b.totalAmount;
            a.orderCount += b.orderCount;
            return a;
        }
    }

    public static class OrderAccumulator {
        public String customerId;
        public String status;
        public double totalAmount = 0.0;
        public long orderCount = 0;
    }

    public static class JsonSerializationSchema implements SerializationSchema<Map<String, Object>> {
        @Override
        public byte[] serialize(Map<String, Object> element) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(element).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        }
    }
}
