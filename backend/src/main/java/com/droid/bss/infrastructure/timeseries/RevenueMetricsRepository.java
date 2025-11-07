package com.droid.bss.infrastructure.timeseries;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for revenue time-series metrics stored in TimescaleDB
 */
@Repository
public class RevenueMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public RevenueMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Record a revenue metric
     */
    public void recordRevenueMetric(BigDecimal revenue, BigDecimal costs,
                                   Integer ordersCount, String region, String productCategory) {
        String sql = "SELECT record_revenue_metric(?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, revenue, costs, ordersCount, region, productCategory);
    }

    /**
     * Get daily revenue summary
     */
    public List<RevenueDaily> getRevenueSummary(int days) {
        String sql = "SELECT " +
                     "day, " +
                     "total_revenue, " +
                     "total_profit, " +
                     "total_orders, " +
                     "avg_order_value " +
                     "FROM revenue_daily " +
                     "WHERE day > NOW() - INTERVAL '? days' " +
                     "ORDER BY day DESC";

        return jdbcTemplate.query(sql,
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
    }

    /**
     * Get revenue by region
     */
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

    /**
     * Get revenue by product category
     */
    public List<RevenueByCategory> getRevenueByCategory(int days) {
        String sql = "SELECT " +
                     "time_bucket('1 day', time) AS day, " +
                     "product_category, " +
                     "SUM(revenue) AS total_revenue, " +
                     "SUM(profit) AS total_profit, " +
                     "SUM(orders_count) AS total_orders " +
                     "FROM revenue_metrics " +
                     "WHERE time > NOW() - INTERVAL '? days' " +
                     "GROUP BY day, product_category " +
                     "ORDER BY day DESC, total_revenue DESC";

        return jdbcTemplate.query(sql,
            new Object[]{days},
            (rs, rowNum) -> {
                RevenueByCategory revenue = new RevenueByCategory();
                revenue.setDay(rs.getTimestamp("day").toInstant());
                revenue.setProductCategory(rs.getString("product_category"));
                revenue.setTotalRevenue(rs.getBigDecimal("total_revenue"));
                revenue.setTotalProfit(rs.getBigDecimal("total_profit"));
                revenue.setTotalOrders(rs.getLong("total_orders"));
                return revenue;
            });
    }

    /**
     * Calculate growth rate for a metric
     */
    public List<GrowthRate> calculateGrowthRate(String metricName, int days) {
        String sql = "WITH data AS (" +
                     "SELECT " +
                     "time_bucket('1 day', time) AS day, " +
                     "AVG(revenue) AS value " +
                     "FROM revenue_metrics " +
                     "WHERE time > NOW() - INTERVAL '? days' " +
                     "GROUP BY day " +
                     "ORDER BY day" +
                     ") " +
                     "SELECT " +
                     "day, " +
                     "value, " +
                     "(value - LAG(value) OVER (ORDER BY day)) / LAG(value) OVER (ORDER BY day) * 100 AS growth_rate " +
                     "FROM data";

        return jdbcTemplate.query(sql,
            new Object[]{days},
            (rs, rowNum) -> {
                GrowthRate growth = new GrowthRate();
                growth.setDay(rs.getTimestamp("day").toInstant());
                growth.setValue(rs.getDouble("value"));
                growth.setGrowthRate(rs.getDouble("growth_rate"));
                return growth;
            });
    }

    /**
     * Get total revenue for a period
     */
    public BigDecimal getTotalRevenue(Instant startTime, Instant endTime) {
        String sql = "SELECT COALESCE(SUM(revenue), 0) FROM revenue_metrics " +
                     "WHERE time BETWEEN ? AND ?";

        return jdbcTemplate.queryForObject(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            BigDecimal.class);
    }

    /**
     * Get total profit for a period
     */
    public BigDecimal getTotalProfit(Instant startTime, Instant endTime) {
        String sql = "SELECT COALESCE(SUM(profit), 0) FROM revenue_metrics " +
                     "WHERE time BETWEEN ? AND ?";

        return jdbcTemplate.queryForObject(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            BigDecimal.class);
    }

    /**
     * Get average order value
     */
    public BigDecimal getAverageOrderValue(Instant startTime, Instant endTime) {
        String sql = "SELECT COALESCE(AVG(avg_order_value), 0) FROM revenue_metrics " +
                     "WHERE time BETWEEN ? AND ?";

        return jdbcTemplate.queryForObject(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            BigDecimal.class);
    }

    // Inner classes for DTOs
    public static class RevenueDaily {
        private Instant day;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
        private Long totalOrders;
        private BigDecimal avgOrderValue;

        // Getters and setters
        public Instant getDay() { return day; }
        public void setDay(Instant day) { this.day = day; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public BigDecimal getAvgOrderValue() { return avgOrderValue; }
        public void setAvgOrderValue(BigDecimal avgOrderValue) { this.avgOrderValue = avgOrderValue; }
    }

    public static class RevenueByRegion {
        private Instant day;
        private String region;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
        private Long totalOrders;

        // Getters and setters
        public Instant getDay() { return day; }
        public void setDay(Instant day) { this.day = day; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    }

    public static class RevenueByCategory {
        private Instant day;
        private String productCategory;
        private BigDecimal totalRevenue;
        private BigDecimal totalProfit;
        private Long totalOrders;

        // Getters and setters
        public Instant getDay() { return day; }
        public void setDay(Instant day) { this.day = day; }
        public String getProductCategory() { return productCategory; }
        public void setProductCategory(String productCategory) { this.productCategory = productCategory; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    }

    public static class GrowthRate {
        private Instant day;
        private Double value;
        private Double growthRate;

        // Getters and setters
        public Instant getDay() { return day; }
        public void setDay(Instant day) { this.day = day; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public Double getGrowthRate() { return growthRate; }
        public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }
    }
}
