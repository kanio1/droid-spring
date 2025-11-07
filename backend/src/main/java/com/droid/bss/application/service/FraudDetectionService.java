package com.droid.bss.application.service;

import com.droid.bss.infrastructure.timeseries.PaymentMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final PaymentMetricsRepository repository;

    public FraudDetectionService(PaymentMetricsRepository repository) {
        this.repository = repository;
    }

    public void recordPaymentMetric(UUID paymentId, UUID orderId, UUID customerId,
                                   java.math.BigDecimal amount, String status, String paymentMethod,
                                   Double fraudScore) {
        repository.recordPaymentMetric(paymentId, orderId, customerId, amount, status, paymentMethod, fraudScore);
    }

    public List<PaymentMetricsRepository.PaymentAnomaly> detectAnomalies(Instant startTime, Instant endTime) {
        return detectAnomalies(startTime, endTime, 80.0);
    }

    public List<PaymentMetricsRepository.PaymentAnomaly> detectAnomalies(Instant startTime, Instant endTime,
                                                                         Double fraudThreshold) {
        log.info("Detecting anomalies with fraud threshold: {}", fraudThreshold);
        return repository.detectAnomalies(startTime, endTime, fraudThreshold);
    }

    public List<PaymentMetricsRepository.PaymentAnomaly> detectAnomalies(int hours, Double fraudThreshold) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(hours * 60L * 60);
        return repository.detectAnomalies(startTime, endTime, fraudThreshold);
    }

    public FraudPattern analyzeFraudPatterns(Instant startTime, Instant endTime) {
        List<PaymentMetricsRepository.FraudPatternResult> results =
            repository.analyzeFraudPatterns(startTime, endTime);

        FraudPattern pattern = new FraudPattern();
        pattern.setPatterns(results);
        pattern.setTotalPayments(results.stream()
            .mapToLong(PaymentMetricsRepository.FraudPatternResult::getCount).sum());
        pattern.setAvgFraudScore(results.stream()
            .mapToDouble(PaymentMetricsRepository.FraudPatternResult::getAvgFraudScore)
            .average().orElse(0.0));
        pattern.setTotalAmount(results.stream()
            .map(PaymentMetricsRepository.FraudPatternResult::getTotalAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        return pattern;
    }

    public FraudPattern analyzeFraudPatterns(int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return analyzeFraudPatterns(startTime, endTime);
    }

    public List<PaymentMetricsRepository.PaymentStatusDaily> getPaymentStatusDaily(int days) {
        return repository.getPaymentStatusDaily(days);
    }

    public List<PaymentMetricsRepository.UnusualPayment> detectUnusualPaymentAmounts(int days, int minCount) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.detectUnusualPaymentAmounts(startTime, endTime, minCount);
    }

    public List<PaymentMetricsRepository.HighValueTransaction> getHighValueTransactions(
            int days, double minAmount) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.getHighValueTransactions(startTime, endTime,
            java.math.BigDecimal.valueOf(minAmount));
    }

    public Double calculateAverageFraudScore(int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);

        // Get all payments in the period
        List<PaymentMetricsRepository.PaymentStatusDaily> payments = repository.getPaymentStatusDaily(days);

        if (payments.isEmpty()) {
            return 0.0;
        }

        // Calculate weighted average
        double totalWeightedScore = 0.0;
        long totalPayments = 0;

        for (PaymentMetricsRepository.PaymentStatusDaily payment : payments) {
            if (payment.getAvgFraudScore() != null) {
                totalWeightedScore += payment.getAvgFraudScore() * payment.getPaymentCount();
                totalPayments += payment.getPaymentCount();
            }
        }

        return totalPayments > 0 ? totalWeightedScore / totalPayments : 0.0;
    }

    // Inner classes for DTOs
    public static class FraudPattern {
        private List<PaymentMetricsRepository.FraudPatternResult> patterns;
        private Long totalPayments;
        private Double avgFraudScore;
        private java.math.BigDecimal totalAmount;

        // Getters and setters
        public List<PaymentMetricsRepository.FraudPatternResult> getPatterns() { return patterns; }
        public void setPatterns(List<PaymentMetricsRepository.FraudPatternResult> patterns) { this.patterns = patterns; }
        public Long getTotalPayments() { return totalPayments; }
        public void setTotalPayments(Long totalPayments) { this.totalPayments = totalPayments; }
        public Double getAvgFraudScore() { return avgFraudScore; }
        public void setAvgFraudScore(Double avgFraudScore) { this.avgFraudScore = avgFraudScore; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }
}
