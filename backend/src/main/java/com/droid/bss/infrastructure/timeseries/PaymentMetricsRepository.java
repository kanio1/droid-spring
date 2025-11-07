package com.droid.bss.infrastructure.timeseries;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for payment time-series metrics stored in TimescaleDB
 */
@Repository
public class PaymentMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Record a payment metric
     */
    public void recordPaymentMetric(UUID paymentId, UUID orderId, UUID customerId,
                                   BigDecimal amount, String status, String paymentMethod,
                                   Double fraudScore) {
        String sql = "SELECT record_payment_metric(?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, paymentId, orderId, customerId, amount, status, paymentMethod, fraudScore);
    }

    /**
     * Detect payment anomalies based on fraud score
     */
    public List<PaymentAnomaly> detectAnomalies(Instant startTime, Instant endTime, Double fraudThreshold) {
        String sql = "SELECT " +
                     "payment_id, " +
                     "customer_id, " +
                     "amount, " +
                     "fraud_score, " +
                     "time, " +
                     "payment_method " +
                     "FROM payment_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "AND fraud_score > ? " +
                     "ORDER BY fraud_score DESC, time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime), fraudThreshold},
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

    /**
     * Analyze fraud patterns by payment method and status
     */
    public List<FraudPatternResult> analyzeFraudPatterns(Instant startTime, Instant endTime) {
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

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                FraudPatternResult result = new FraudPatternResult();
                result.setPaymentMethod(rs.getString("payment_method"));
                result.setStatus(rs.getString("status"));
                result.setCount(rs.getLong("count"));
                result.setAvgFraudScore(rs.getDouble("avg_fraud_score"));
                result.setTotalAmount(rs.getBigDecimal("total_amount"));
                return result;
            });
    }

    /**
     * Get payment status breakdown from continuous aggregate
     */
    public List<PaymentStatusDaily> getPaymentStatusDaily(int days) {
        String sql = "SELECT " +
                     "day, " +
                     "status, " +
                     "payment_count, " +
                     "total_amount, " +
                     "avg_fraud_score " +
                     "FROM payment_status_daily " +
                     "WHERE day > NOW() - INTERVAL '? days' " +
                     "ORDER BY day DESC, payment_count DESC";

        return jdbcTemplate.query(sql,
            new Object[]{days},
            (rs, rowNum) -> {
                PaymentStatusDaily status = new PaymentStatusDaily();
                status.setDay(rs.getTimestamp("day").toInstant());
                status.setStatus(rs.getString("status"));
                status.setPaymentCount(rs.getLong("payment_count"));
                status.setTotalAmount(rs.getBigDecimal("total_amount"));
                status.setAvgFraudScore(rs.getDouble("avg_fraud_score"));
                return status;
            });
    }

    /**
     * Detect unusual payment amounts (statistical outliers)
     */
    public List<UnusualPayment> detectUnusualPaymentAmounts(Instant startTime, Instant endTime, int minCount) {
        String sql = "SELECT " +
                     "customer_id, " +
                     "AVG(amount) AS avg_amount, " +
                     "STDDEV(amount) AS stddev_amount, " +
                     "COUNT(*) AS payment_count " +
                     "FROM payment_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "GROUP BY customer_id " +
                     "HAVING COUNT(*) > ? " +
                     "ORDER BY stddev_amount DESC " +
                     "LIMIT 100";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime), minCount},
            (rs, rowNum) -> {
                UnusualPayment payment = new UnusualPayment();
                payment.setCustomerId(rs.getObject("customer_id", UUID.class));
                payment.setAvgAmount(rs.getBigDecimal("avg_amount"));
                payment.setStddevAmount(rs.getBigDecimal("stddev_amount"));
                payment.setPaymentCount(rs.getLong("payment_count"));
                return payment;
            });
    }

    /**
     * Get high-value transactions
     */
    public List<HighValueTransaction> getHighValueTransactions(Instant startTime, Instant endTime,
                                                               BigDecimal minAmount) {
        String sql = "SELECT " +
                     "payment_id, " +
                     "customer_id, " +
                     "amount, " +
                     "fraud_score, " +
                     "time, " +
                     "payment_method " +
                     "FROM payment_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "AND amount >= ? " +
                     "ORDER BY amount DESC";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime), minAmount},
            (rs, rowNum) -> {
                HighValueTransaction transaction = new HighValueTransaction();
                transaction.setPaymentId(rs.getObject("payment_id", UUID.class));
                transaction.setCustomerId(rs.getObject("customer_id", UUID.class));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setFraudScore(rs.getDouble("fraud_score"));
                transaction.setTime(rs.getTimestamp("time").toInstant());
                transaction.setPaymentMethod(rs.getString("payment_method"));
                return transaction;
            });
    }

    // Inner classes for DTOs
    public static class PaymentAnomaly {
        private UUID paymentId;
        private UUID customerId;
        private BigDecimal amount;
        private Double fraudScore;
        private Instant timestamp;
        private String paymentMethod;

        // Getters and setters
        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Double getFraudScore() { return fraudScore; }
        public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class FraudPatternResult {
        private String paymentMethod;
        private String status;
        private Long count;
        private Double avgFraudScore;
        private BigDecimal totalAmount;

        // Getters and setters
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public Double getAvgFraudScore() { return avgFraudScore; }
        public void setAvgFraudScore(Double avgFraudScore) { this.avgFraudScore = avgFraudScore; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    public static class PaymentStatusDaily {
        private Instant day;
        private String status;
        private Long paymentCount;
        private BigDecimal totalAmount;
        private Double avgFraudScore;

        // Getters and setters
        public Instant getDay() { return day; }
        public void setDay(Instant day) { this.day = day; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getPaymentCount() { return paymentCount; }
        public void setPaymentCount(Long paymentCount) { this.paymentCount = paymentCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public Double getAvgFraudScore() { return avgFraudScore; }
        public void setAvgFraudScore(Double avgFraudScore) { this.avgFraudScore = avgFraudScore; }
    }

    public static class UnusualPayment {
        private UUID customerId;
        private BigDecimal avgAmount;
        private BigDecimal stddevAmount;
        private Long paymentCount;

        // Getters and setters
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public BigDecimal getAvgAmount() { return avgAmount; }
        public void setAvgAmount(BigDecimal avgAmount) { this.avgAmount = avgAmount; }
        public BigDecimal getStddevAmount() { return stddevAmount; }
        public void setStddevAmount(BigDecimal stddevAmount) { this.stddevAmount = stddevAmount; }
        public Long getPaymentCount() { return paymentCount; }
        public void setPaymentCount(Long paymentCount) { this.paymentCount = paymentCount; }
    }

    public static class HighValueTransaction {
        private UUID paymentId;
        private UUID customerId;
        private BigDecimal amount;
        private Double fraudScore;
        private Instant time;
        private String paymentMethod;

        // Getters and setters
        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Double getFraudScore() { return fraudScore; }
        public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }
        public Instant getTime() { return time; }
        public void setTime(Instant time) { this.time = time; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }
}
