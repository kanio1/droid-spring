package com.droid.bss.api.analytics;

import com.droid.bss.application.service.FraudDetectionService;
import com.droid.bss.infrastructure.timeseries.PaymentMetricsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
@Tag(name = "Fraud Detection", description = "Fraud detection and payment anomaly endpoints")
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    public FraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Detect payment anomalies", description = "Detect payment anomalies based on fraud score")
    public ResponseEntity<List<PaymentMetricsRepository.PaymentAnomaly>> detectAnomalies(
            @Parameter(description = "Fraud score threshold (0-100)", example = "80.0")
            @RequestParam(defaultValue = "80.0") Double fraudThreshold,
            @Parameter(description = "Number of hours to analyze", example = "24")
            @RequestParam(defaultValue = "24") int hours) {

        List<PaymentMetricsRepository.PaymentAnomaly> anomalies =
            fraudDetectionService.detectAnomalies(hours, fraudThreshold);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/anomalies/range")
    @Operation(summary = "Detect anomalies in time range", description = "Detect anomalies within a specific time range")
    public ResponseEntity<List<PaymentMetricsRepository.PaymentAnomaly>> detectAnomaliesInRange(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @Parameter(description = "Fraud score threshold (0-100)", example = "80.0")
            @RequestParam(defaultValue = "80.0") Double fraudThreshold) {

        List<PaymentMetricsRepository.PaymentAnomaly> anomalies =
            fraudDetectionService.detectAnomalies(startTime, endTime, fraudThreshold);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/patterns")
    @Operation(summary = "Analyze fraud patterns", description = "Analyze fraud patterns by payment method and status")
    public ResponseEntity<FraudDetectionService.FraudPattern> analyzeFraudPatterns(
            @Parameter(description = "Number of days to analyze", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        FraudDetectionService.FraudPattern pattern = fraudDetectionService.analyzeFraudPatterns(days);
        return ResponseEntity.ok(pattern);
    }

    @GetMapping("/patterns/range")
    @Operation(summary = "Analyze fraud patterns in time range", description = "Analyze fraud patterns within a specific time range")
    public ResponseEntity<FraudDetectionService.FraudPattern> analyzeFraudPatternsInRange(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        FraudDetectionService.FraudPattern pattern = fraudDetectionService.analyzeFraudPatterns(startTime, endTime);
        return ResponseEntity.ok(pattern);
    }

    @GetMapping("/payments/status")
    @Operation(summary = "Get payment status breakdown", description = "Get daily payment status breakdown from continuous aggregate")
    public ResponseEntity<List<PaymentMetricsRepository.PaymentStatusDaily>> getPaymentStatusDaily(
            @Parameter(description = "Number of days", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        List<PaymentMetricsRepository.PaymentStatusDaily> status =
            fraudDetectionService.getPaymentStatusDaily(days);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/payments/unusual-amounts")
    @Operation(summary = "Detect unusual payment amounts", description = "Detect customers with unusual payment amount patterns")
    public ResponseEntity<List<PaymentMetricsRepository.UnusualPayment>> detectUnusualPaymentAmounts(
            @Parameter(description = "Number of days to analyze", example = "7")
            @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "Minimum number of payments per customer", example = "5")
            @RequestParam(defaultValue = "5") int minCount) {

        List<PaymentMetricsRepository.UnusualPayment> unusualPayments =
            fraudDetectionService.detectUnusualPaymentAmounts(days, minCount);
        return ResponseEntity.ok(unusualPayments);
    }

    @GetMapping("/payments/high-value")
    @Operation(summary = "Get high-value transactions", description = "Get high-value transactions for review")
    public ResponseEntity<List<PaymentMetricsRepository.HighValueTransaction>> getHighValueTransactions(
            @Parameter(description = "Number of days", example = "1")
            @RequestParam(defaultValue = "1") int days,
            @Parameter(description = "Minimum transaction amount", example = "1000.00")
            @RequestParam(defaultValue = "1000.00") Double minAmount) {

        List<PaymentMetricsRepository.HighValueTransaction> transactions =
            fraudDetectionService.getHighValueTransactions(days, minAmount);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/score/average")
    @Operation(summary = "Get average fraud score", description = "Get average fraud score for the period")
    public ResponseEntity<Double> getAverageFraudScore(
            @Parameter(description = "Number of days", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        Double avgScore = fraudDetectionService.calculateAverageFraudScore(days);
        return ResponseEntity.ok(avgScore);
    }
}
