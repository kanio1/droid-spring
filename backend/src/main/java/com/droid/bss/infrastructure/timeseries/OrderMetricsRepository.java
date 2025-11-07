package com.droid.bss.infrastructure.timeseries;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for order time-series metrics stored in TimescaleDB
 */
@Repository
public class OrderMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Record an order metric
     */
    public void recordOrderMetric(UUID orderId, UUID customerId, String status,
                                 BigDecimal totalAmount, Integer itemsCount, String region) {
        String sql = "SELECT record_order_metric(?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, orderId, customerId, status, totalAmount, itemsCount, region);
    }

    /**
     * Get order status distribution for a time range
     */
    public List<OrderStatusDistribution> getOrderStatusDistribution(Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "time_bucket('1 day', time) AS day, " +
                     "status, " +
                     "COUNT(*) AS order_count, " +
                     "SUM(total_amount) AS total_amount " +
                     "FROM order_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "GROUP BY day, status " +
                     "ORDER BY day DESC, order_count DESC";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                OrderStatusDistribution dist = new OrderStatusDistribution();
                dist.setDay(rs.getTimestamp("day").toInstant());
                dist.setStatus(rs.getString("status"));
                dist.setOrderCount(rs.getLong("order_count"));
                dist.setTotalAmount(rs.getBigDecimal("total_amount"));
                return dist;
            });
    }

    /**
     * Get orders by region
     */
    public List<OrderByRegion> getOrdersByRegion(Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "region, " +
                     "COUNT(*) AS order_count, " +
                     "SUM(total_amount) AS total_amount, " +
                     "AVG(total_amount) AS avg_amount, " +
                     "SUM(items_count) AS total_items " +
                     "FROM order_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "GROUP BY region " +
                     "ORDER BY total_amount DESC";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                OrderByRegion order = new OrderByRegion();
                order.setRegion(rs.getString("region"));
                order.setOrderCount(rs.getLong("order_count"));
                order.setTotalAmount(rs.getBigDecimal("total_amount"));
                order.setAvgAmount(rs.getBigDecimal("avg_amount"));
                order.setTotalItems(rs.getLong("total_items"));
                return order;
            });
    }

    /**
     * Get customer order history
     */
    public List<CustomerOrderHistory> getCustomerOrderHistory(UUID customerId, Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "time, " +
                     "order_id, " +
                     "status, " +
                     "total_amount, " +
                     "items_count, " +
                     "region " +
                     "FROM order_metrics " +
                     "WHERE customer_id = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "ORDER BY time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{customerId, java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                CustomerOrderHistory history = new CustomerOrderHistory();
                history.setTime(rs.getTimestamp("time").toInstant());
                history.setOrderId(rs.getObject("order_id", UUID.class));
                history.setStatus(rs.getString("status"));
                history.setTotalAmount(rs.getBigDecimal("total_amount"));
                history.setItemsCount(rs.getInt("items_count"));
                history.setRegion(rs.getString("region"));
                return history;
            });
    }

    /**
     * Calculate order fulfillment time (if completed)
     */
    public List<OrderFulfillmentTime> getOrderFulfillmentTime(Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "o.order_id, " +
                     "o.customer_id, " +
                     "o.time AS order_time, " +
                     "c.time AS completed_time, " +
                     "EXTRACT(EPOCH FROM (c.time - o.time)) / 3600 AS fulfillment_hours " +
                     "FROM order_metrics o " +
                     "JOIN order_metrics c ON o.order_id = c.order_id " +
                     "WHERE o.status = 'PENDING' " +
                     "AND c.status = 'DELIVERED' " +
                     "AND o.time BETWEEN ? AND ? " +
                     "ORDER BY fulfillment_hours DESC";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                OrderFulfillmentTime time = new OrderFulfillmentTime();
                time.setOrderId(rs.getObject("order_id", UUID.class));
                time.setCustomerId(rs.getObject("customer_id", UUID.class));
                time.setOrderTime(rs.getTimestamp("order_time").toInstant());
                time.setCompletedTime(rs.getTimestamp("completed_time").toInstant());
                time.setFulfillmentHours(rs.getDouble("fulfillment_hours"));
                return time;
            });
    }

    // Inner classes for DTOs
    public static class OrderStatusDistribution {
        private Instant day;
        private String status;
        private Long orderCount;
        private BigDecimal totalAmount;

        // Getters and setters
        public Instant getDay() { return day; }
        public void setDay(Instant day) { this.day = day; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    public static class OrderByRegion {
        private String region;
        private Long orderCount;
        private BigDecimal totalAmount;
        private BigDecimal avgAmount;
        private Long totalItems;

        // Getters and setters
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getAvgAmount() { return avgAmount; }
        public void setAvgAmount(BigDecimal avgAmount) { this.avgAmount = avgAmount; }
        public Long getTotalItems() { return totalItems; }
        public void setTotalItems(Long totalItems) { this.totalItems = totalItems; }
    }

    public static class CustomerOrderHistory {
        private Instant time;
        private UUID orderId;
        private String status;
        private BigDecimal totalAmount;
        private Integer itemsCount;
        private String region;

        // Getters and setters
        public Instant getTime() { return time; }
        public void setTime(Instant time) { this.time = time; }
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public Integer getItemsCount() { return itemsCount; }
        public void setItemsCount(Integer itemsCount) { this.itemsCount = itemsCount; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    public static class OrderFulfillmentTime {
        private UUID orderId;
        private UUID customerId;
        private Instant orderTime;
        private Instant completedTime;
        private Double fulfillmentHours;

        // Getters and setters
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public Instant getOrderTime() { return orderTime; }
        public void setOrderTime(Instant orderTime) { this.orderTime = orderTime; }
        public Instant getCompletedTime() { return completedTime; }
        public void setCompletedTime(Instant completedTime) { this.completedTime = completedTime; }
        public Double getFulfillmentHours() { return fulfillmentHours; }
        public void setFulfillmentHours(Double fulfillmentHours) { this.fulfillmentHours = fulfillmentHours; }
    }
}
