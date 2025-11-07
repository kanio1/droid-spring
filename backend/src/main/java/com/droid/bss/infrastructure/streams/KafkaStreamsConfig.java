package com.droid.bss.infrastructure.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.Properties;

/**
 * Configuration for Kafka Streams
 * Real-time data processing for analytics, recommendations, and fraud detection
 */
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Bean(name = "streamsBuilder")
    public StreamsBuilderFactoryBean streamsBuilder() {
        StreamsBuilderFactoryBean factoryBean = new StreamsBuilderFactoryBean(createStreamsConfig());
        factoryBean.setAutoStart(false); // We'll start it manually
        return factoryBean;
    }

    @Bean
    public StreamsBuilder streamsBuilder() {
        StreamsBuilder builder = new StreamsBuilder();

        // Build all streams topologies
        buildCustomerActivityStream(builder);
        buildOrderEnrichmentStream(builder);
        buildFraudDetectionStream(builder);
        buildRecommendationStream(builder);
        buildDynamicPricingStream(builder);

        return builder;
    }

    private void buildCustomerActivityStream(StreamsBuilder builder) {
        // Customer activity stream
        KStream<String, CustomerActivityEvent> customerStream = builder
            .stream("bss.customer.events", Consumed.with(Serdes.String(), getCustomerEventSerde()));

        // Aggregate customer activity by window
        customerStream
            .groupByKey(Grouped.with(Serdes.String(), getCustomerEventSerde()))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .aggregate(
                CustomerActivityAggregate::new,
                (key, value, aggregate) -> aggregate.addEvent(value),
                Materialized.with(Serdes.String(), getCustomerAggregateSerde())
            )
            .toStream()
            .mapValues(this::enrichCustomerActivity)
            .to("bss.customer.activity.aggregated", Produced.with(Serdes.String(), getCustomerActivitySerde()));

        // Customer session tracking
        customerStream
            .groupBy((key, event) -> event.getCustomerId().toString())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(30)))
            .count(Materialized.as("customer-session-count"))
            .toStream()
            .to("bss.customer.sessions", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String.class), Serdes.Long()));
    }

    private void buildOrderEnrichmentStream(StreamsBuilder builder) {
        // Orders stream
        KStream<String, OrderEvent> orderStream = builder
            .stream("bss.order.events", Consumed.with(Serdes.String(), getOrderEventSerde()));

        // Enrich orders with customer data
        KTable<String, CustomerInfo> customerTable = builder
            .table("bss.customer.info", Consumed.with(Serdes.String(), getCustomerInfoSerde()));

        KStream<String, EnrichedOrderEvent> enrichedOrderStream = orderStream
            .leftJoin(
                customerTable,
                (order, customer) -> EnrichedOrderEvent.builder()
                    .orderId(order.getOrderId())
                    .customerId(order.getCustomerId())
                    .customerTier(customer != null ? customer.getTier() : "UNKNOWN")
                    .totalAmount(order.getTotalAmount())
                    .itemsCount(order.getItemsCount())
                    .region(order.getRegion())
                    .timestamp(order.getTimestamp())
                    .build()
            );

        // Detect high-value orders
        enrichedOrderStream
            .filter((key, order) -> order.getTotalAmount().compareTo(java.math.BigDecimal.valueOf(1000)) > 0)
            .to("bss.orders.highvalue", Produced.with(Serdes.String(), getEnrichedOrderSerde()));

        // Calculate order value trend
        enrichedOrderStream
            .groupBy((key, order) -> order.getRegion())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .aggregate(
                OrderValueAggregate::new,
                (key, order, aggregate) -> aggregate.addOrder(order),
                Materialized.with(Serdes.String(), getOrderAggregateSerde())
            )
            .toStream()
            .to("bss.orders.region.trend", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String.class), getOrderAggregateSerde()));
    }

    private void buildFraudDetectionStream(StreamsBuilder builder) {
        // Payment stream
        KStream<String, PaymentEvent> paymentStream = builder
            .stream("bss.payment.events", Consumed.with(Serdes.String(), getPaymentEventSerde()));

        // Detect rapid successive payments
        paymentStream
            .groupBy((key, payment) -> payment.getCustomerId().toString())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.as("payment-count-5min"))
            .toStream()
            .filter((windowedCustomerId, count) -> count > 10)
            .map((windowedCustomerId, count) -> {
                FraudAlert alert = FraudAlert.builder()
                    .customerId(windowedCustomerId.key())
                    .alertType("RAPID_PAYMENTS")
                    .severity("HIGH")
                    .count(count)
                    .windowStart(windowedCustomerId.window().startTime())
                    .windowEnd(windowedCustomerId.window().endTime())
                    .build();
                return new KeyValue<>(windowedCustomerId.key(), alert);
            })
            .to("bss.fraud.alerts", Produced.with(Serdes.String(), getFraudAlertSerde()));

        // Detect unusual amount patterns
        KTable<String, PaymentHistory> paymentHistoryTable = builder
            .table("bss.customer.payment.history", Consumed.with(Serdes.String(), getPaymentHistorySerde()));

        paymentStream
            .leftJoin(
                paymentHistoryTable,
                (payment, history) -> {
                    if (history == null || history.getAverageAmount() == null) {
                        return FraudAlert.builder()
                            .customerId(payment.getCustomerId())
                            .alertType("NO_HISTORY")
                            .severity("MEDIUM")
                            .amount(payment.getAmount())
                            .build();
                    }

                    double average = history.getAverageAmount();
                    double current = payment.getAmount().doubleValue();

                    // Flag if payment is 5x average
                    if (current > average * 5) {
                        return FraudAlert.builder()
                            .customerId(payment.getCustomerId())
                            .alertType("UNUSUAL_AMOUNT")
                            .severity("HIGH")
                            .amount(payment.getAmount())
                            .expectedAmount(java.math.BigDecimal.valueOf(average))
                            .ratio(current / average)
                            .build();
                    }

                    return null;
                }
            )
            .filter((key, alert) -> alert != null)
            .to("bss.fraud.alerts", Produced.with(Serdes.String(), getFraudAlertSerde()));

        // Calculate fraud score
        KTable<String, FraudScore> fraudScoreTable = paymentStream
            .groupBy((key, payment) -> payment.getCustomerId().toString())
            .aggregate(
                FraudScore::new,
                (key, payment, score) -> score.addPayment(payment),
                Materialized.with(Serdes.String(), getFraudScoreSerde())
            );

        fraudScoreTable
            .toStream()
            .filter((customerId, score) -> score.getScore() > 75.0)
            .to("bss.fraud.high-risk", Produced.with(Serdes.String(), getFraudScoreSerde()));
    }

    private void buildRecommendationStream(StreamsBuilder builder) {
        // Customer activity for recommendations
        KStream<String, CustomerActivityEvent> activityStream = builder
            .stream("bss.customer.activity", Consumed.with(Serdes.String(), getCustomerActivitySerde()));

        // Product views stream
        KStream<String, ProductViewEvent> productViewStream = builder
            .stream("bss.product.views", Consumed.with(Serdes.String(), getProductViewSerde()));

        // Build customer interest profile
        KTable<String, CustomerInterestProfile> interestProfile = activityStream
            .groupBy((key, activity) -> activity.getCustomerId().toString())
            .aggregate(
                CustomerInterestProfile::new,
                (key, activity, profile) -> profile.addActivity(activity),
                Materialized.with(Serdes.String(), getInterestProfileSerde())
            );

        // Generate recommendations based on product views
        KStream<String, RecommendationEvent> recommendationStream = productViewStream
            .leftJoin(
                interestProfile,
                (view, profile) -> RecommendationEvent.builder()
                    .customerId(view.getCustomerId())
                    .productId(view.getProductId())
                    .category(view.getCategory())
                    .recommendations(profile.getTopCategories(3))
                    .confidence(0.85)
                    .timestamp(java.time.Instant.now())
                    .build()
            );

        recommendationStream
            .to("bss.recommendations", Produced.with(Serdes.String(), getRecommendationSerde()));

        // Real-time trending products
        productViewStream
            .groupBy((key, view) -> view.getProductId().toString())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .count(Materialized.as("product-view-count-1h"))
            .toStream()
            .map((windowedProductId, count) -> {
                TrendingProduct trending = TrendingProduct.builder()
                    .productId(windowedProductId.key())
                    .viewCount(count)
                    .windowStart(windowedProductId.window().startTime())
                    .windowEnd(windowedProductId.window().endTime())
                    .build();
                return new KeyValue<>(windowedProductId.key(), trending);
            })
            .to("bss.products.trending", Produced.with(Serdes.String(), getTrendingProductSerde()));
    }

    private void buildDynamicPricingStream(StreamsBuilder builder) {
        // Order stream
        KStream<String, OrderEvent> orderStream = builder
            .stream("bss.order.events", Consumed.with(Serdes.String(), getOrderEventSerde()));

        // Calculate demand-based pricing
        orderStream
            .groupBy((key, order) -> order.getProductId().toString())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(15)))
            .aggregate(
                DemandMetrics::new,
                (key, order, metrics) -> metrics.addOrder(order),
                Materialized.with(Serdes.String(), getDemandMetricsSerde())
            )
            .toStream()
            .map((windowedProductId, metrics) -> {
                double basePrice = metrics.getBasePrice();
                double demandFactor = metrics.getDemandFactor();
                double recommendedPrice = basePrice * (1 + (demandFactor - 1) * 0.1); // Max 10% increase

                PricingRecommendation recommendation = PricingRecommendation.builder()
                    .productId(windowedProductId.key())
                    .basePrice(basePrice)
                    .recommendedPrice(recommendedPrice)
                    .demandFactor(demandFactor)
                    .orderCount(metrics.getOrderCount())
                    .windowStart(windowedProductId.window().startTime())
                    .build();

                return new KeyValue<>(windowedProductId.key(), recommendation);
            })
            .to("bss.pricing.recommendations", Produced.with(Serdes.String(), getPricingRecommendationSerde()));
    }

    private CustomerActivityEvent enrichCustomerActivity(CustomerActivityAggregate aggregate) {
        return CustomerActivityEvent.builder()
            .customerId(aggregate.getCustomerId())
            .activities(aggregate.getActivities())
            .sessionCount(aggregate.getSessionCount())
            .totalDuration(aggregate.getTotalDuration())
            .timestamp(java.time.Instant.now())
            .build();
    }

    private Properties createStreamsConfig() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "bss-analytics-streams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L); // 10MB
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);
        return props;
    }

    // Custom Serdes (would be implemented with Avro or JSON SerDe)
    private org.apache.kafka.common.serialization.Serde<CustomerActivityEvent> getCustomerEventSerde() {
        return null; // Placeholder - would use KafkaAvroSerde or custom JSON serde
    }

    private org.apache.kafka.common.serialization.Serde<CustomerActivityAggregate> getCustomerAggregateSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<CustomerActivityEvent> getCustomerActivitySerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<OrderEvent> getOrderEventSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<CustomerInfo> getCustomerInfoSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<EnrichedOrderEvent> getEnrichedOrderSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<OrderValueAggregate> getOrderAggregateSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<PaymentEvent> getPaymentEventSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<FraudAlert> getFraudAlertSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<PaymentHistory> getPaymentHistorySerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<FraudScore> getFraudScoreSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<ProductViewEvent> getProductViewSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<CustomerInterestProfile> getInterestProfileSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<RecommendationEvent> getRecommendationSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<TrendingProduct> getTrendingProductSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<DemandMetrics> getDemandMetricsSerde() {
        return null; // Placeholder
    }

    private org.apache.kafka.common.serialization.Serde<PricingRecommendation> getPricingRecommendationSerde() {
        return null; // Placeholder
    }
}
