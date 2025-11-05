/*
 * BSS Customer Analytics Job - Apache Flink
 * Real-time customer behavior analysis with windowing
 */

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CustomerAnalyticsJob {

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure parallelism
        env.setParallelism(2);

        // Kafka source for customer events
        KafkaSource<String> source = KafkaSource.<String>builder()
            .setBootstrapServers("kafka-1:9092,kafka-2:9092,kafka-3:9092")
            .setTopics("bss.customer.events")
            .setGroupId("flink-customer-analytics")
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema<>(Map.class))
            .build();

        // Create data stream
        DataStream<Map<String, Object>> customerEvents = env
            .fromSource(source, WatermarkStrategy.<Map<String, Object>>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((event, timestamp) -> System.currentTimeMillis()), "Customer Events")
            .name("Customer Kafka Source");

        // Customer behavior analytics
        DataStream<Tuple4<String, Long, Double, String>> customerStats = customerEvents
            .keyBy(event -> event.get("customerId"))
            .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
            .aggregate(new CustomerAggregateFunction())
            .name("Customer Windowed Analytics");

        // Send analytics to output topic
        KafkaSink<String> sink = KafkaSink.<String>builder()
            .setBootstrapServers("kafka-1:9092,kafka-2:9092,kafka-3:9092")
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic("bss.analytics.events")
                .setValueSerializationSchema(new JsonSerializationSchema())
                .build())
            .build();

        customerStats
            .map(event -> {
                Map<String, Object> result = new HashMap<>();
                result.put("type", "customer.analytics.flink");
                result.put("source", "urn:bss:flink:customer-analytics");
                result.put("customerId", event.f0);
                result.put("eventCount", event.f1);
                result.put("avgAmount", event.f2);
                result.put("window", event.f3);
                result.put("timestamp", System.currentTimeMillis());
                return result;
            })
            .sinkTo(sink)
            .name("Analytics to Kafka");

        // Execute the job
        env.execute("BSS Customer Analytics - Flink Job");
    }

    // Aggregate customer behavior
    public static class CustomerAggregateFunction implements
        AggregateFunction<Map<String, Object>, CustomerAccumulator, Tuple4<String, Long, Double, String>> {

        @Override
        public CustomerAccumulator createAccumulator() {
            return new CustomerAccumulator();
        }

        @Override
        public CustomerAccumulator add(Map<String, Object> event, CustomerAccumulator acc) {
            acc.customerId = (String) event.get("customerId");
            acc.eventCount++;
            acc.totalAmount += ((Number) event.getOrDefault("amount", 0)).doubleValue();
            return acc;
        }

        @Override
        public Tuple4<String, Long, Double, String> getResult(CustomerAccumulator acc) {
            double avgAmount = acc.eventCount > 0 ? acc.totalAmount / acc.eventCount : 0.0;
            return Tuple4.of(acc.customerId, acc.eventCount, avgAmount, "5min-window");
        }

        @Override
        public CustomerAccumitor merge(CustomerAccumulator a, CustomerAccumulator b) {
            a.eventCount += b.eventCount;
            a.totalAmount += b.totalAmount;
            return a;
        }
    }

    public static class CustomerAccumulator {
        public String customerId;
        public long eventCount = 0;
        public double totalAmount = 0.0;
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
