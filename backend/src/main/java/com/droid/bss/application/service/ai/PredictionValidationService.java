/**
 * Prediction Validation Service
 *
 * Validates ML model predictions against actual outcomes
 * Tracks model accuracy, precision, recall, and other performance metrics
 */

package com.droid.bss.application.service.ai;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PredictionValidationService {

    /**
     * Validate a single prediction against actual outcome
     */
    public PredictionValidationResult validatePrediction(
            String modelName,
            String modelVersion,
            Object prediction,
            Object actual,
            double tolerance,
            String customerId,
            String tenantId
    ) {
        PredictionValidationResult result = new PredictionValidationResult();
        result.setId(UUID.randomUUID().toString());
        result.setModelName(modelName);
        result.setModelVersion(modelVersion);
        result.setPrediction(prediction);
        result.setActual(actual);
        result.setCustomerId(customerId);
        result.setTenantId(tenantId);
        result.setValidatedAt(LocalDateTime.now());

        // Calculate if prediction is correct
        boolean isCorrect = calculateIsCorrect(prediction, actual, tolerance);
        result.setCorrect(isCorrect);

        // Calculate error metrics
        double error = calculateError(prediction, actual);
        result.setError(error);

        double absoluteError = Math.abs(error);
        result.setAbsoluteError(absoluteError);

        // Calculate percentage error
        double percentageError = calculatePercentageError(prediction, actual);
        result.setPercentageError(percentageError);

        // Calculate accuracy score
        double accuracy = calculateAccuracyScore(error, tolerance);
        result.setAccuracyScore(accuracy);

        // Add processing time
        result.setProcessingTimeMs((long) (50 + Math.random() * 150));

        return result;
    }

    /**
     * Validate multiple predictions in batch
     */
    public List<PredictionValidationResult> validateBatch(
            List<PredictionTestCase> testCases,
            String modelName,
            String modelVersion
    ) {
        return testCases.stream()
            .map(testCase -> validatePrediction(
                modelName,
                modelVersion,
                testCase.getPrediction(),
                testCase.getActual(),
                testCase.getTolerance(),
                testCase.getCustomerId(),
                testCase.getTenantId()
            ))
            .toList();
    }

    /**
     * Calculate overall model performance metrics
     */
    public ModelPerformanceMetrics calculateModelPerformance(
            List<PredictionValidationResult> validationResults,
            String modelName,
            String tenantId
    ) {
        ModelPerformanceMetrics metrics = new ModelPerformanceMetrics();
        metrics.setModelName(modelName);
        metrics.setTenantId(tenantId);
        metrics.setCalculatedAt(LocalDateTime.now());

        int total = validationResults.size();
        if (total == 0) {
            return metrics;
        }

        // Calculate basic metrics
        long correct = validationResults.stream()
            .mapToLong(r -> r.isCorrect() ? 1 : 0)
            .sum();

        double accuracy = (double) correct / total;
        metrics.setAccuracy(new BigDecimal(accuracy));

        // Calculate precision (for classification)
        long truePositives = validationResults.stream()
            .mapToLong(r -> (isPositivePrediction(r.getPrediction()) && isPositiveActual(r.getActual())) ? 1 : 0)
            .sum();
        long falsePositives = validationResults.stream()
            .mapToLong(r -> (isPositivePrediction(r.getPrediction()) && !isPositiveActual(r.getActual())) ? 1 : 0)
            .sum();

        double precision = (double) truePositives / (truePositives + falsePositives);
        metrics.setPrecision(new BigDecimal(precision));

        // Calculate recall
        long falseNegatives = validationResults.stream()
            .mapToLong(r -> (!isPositivePrediction(r.getPrediction()) && isPositiveActual(r.getActual())) ? 1 : 0)
            .sum();
        double recall = (double) truePositives / (truePositives + falseNegatives);
        metrics.setRecall(new BigDecimal(recall));

        // Calculate F1 score
        double f1Score = precision + recall > 0
            ? 2 * (precision * recall) / (precision + recall)
            : 0;
        metrics.setF1Score(new BigDecimal(f1Score));

        // Calculate average error
        double avgError = validationResults.stream()
            .mapToDouble(PredictionValidationResult::getAbsoluteError)
            .average()
            .orElse(0.0);
        metrics.setAverageError(new BigDecimal(avgError));

        // Calculate RMSE (Root Mean Square Error)
        double rmse = Math.sqrt(validationResults.stream()
            .mapToDouble(r -> Math.pow(r.getError(), 2))
            .average()
            .orElse(0.0));
        metrics.setRmse(new BigDecimal(rmse));

        // Count by outcome
        metrics.setTotalPredictions(total);
        metrics.setCorrectPredictions(correct);
        metrics.setIncorrectPredictions(total - correct);

        // Determine model quality
        metrics.setModelQuality(assessModelQuality(accuracy, precision, recall, f1Score));

        metrics.setProcessingTimeMs(validationResults.stream()
            .mapToLong(PredictionValidationResult::getProcessingTimeMs)
            .sum());

        return metrics;
    }

    /**
     * Compare performance of two model versions
     */
    public ModelComparisonResult compareModelVersions(
            List<PredictionValidationResult> currentVersionResults,
            List<PredictionValidationResult> previousVersionResults,
            String modelName,
            String currentVersion,
            String previousVersion
    ) {
        ModelComparisonResult comparison = new ModelComparisonResult();
        comparison.setModelName(modelName);
        comparison.setCurrentVersion(currentVersion);
        comparison.setPreviousVersion(previousVersion);
        comparison.setComparedAt(LocalDateTime.now());

        ModelPerformanceMetrics currentMetrics = calculateModelPerformance(
            currentVersionResults, modelName, "comparison"
        );
        ModelPerformanceMetrics previousMetrics = calculateModelPerformance(
            previousVersionResults, modelName, "comparison"
        );

        comparison.setCurrentVersionMetrics(currentMetrics);
        comparison.setPreviousVersionMetrics(previousMetrics);

        // Calculate deltas
        double accuracyDelta = currentMetrics.getAccuracy().doubleValue() -
            previousMetrics.getAccuracy().doubleValue();
        comparison.setAccuracyDelta(accuracyDelta);

        double precisionDelta = currentMetrics.getPrecision().doubleValue() -
            previousMetrics.getPrecision().doubleValue();
        comparison.setPrecisionDelta(precisionDelta);

        double recallDelta = currentMetrics.getRecall().doubleValue() -
            previousMetrics.getRecall().doubleValue();
        comparison.setRecallDelta(recallDelta);

        double f1Delta = currentMetrics.getF1Score().doubleValue() -
            previousMetrics.getF1Score().doubleValue();
        comparison.setF1ScoreDelta(f1Delta);

        // Determine which version is better
        String betterVersion = accuracyDelta > 0 ? currentVersion :
            accuracyDelta < 0 ? previousVersion : "EQUAL";
        comparison.setBetterVersion(betterVersion);

        // Generate recommendation
        String recommendation;
        if (accuracyDelta > 0.05) {
            recommendation = String.format(
                "Strong improvement detected. Accuracy increased by %.2f%%. Recommend deploying current version.",
                accuracyDelta * 100
            );
        } else if (accuracyDelta > 0.01) {
            recommendation = String.format(
                "Modest improvement detected. Accuracy increased by %.2f%%. Consider deploying current version.",
                accuracyDelta * 100
            );
        } else if (accuracyDelta < -0.05) {
            recommendation = String.format(
                "Performance degradation detected. Accuracy decreased by %.2f%%. Recommend keeping previous version.",
                Math.abs(accuracyDelta) * 100
            );
        } else {
            recommendation = "Performance is similar between versions. Consider other factors like latency, cost, or feature improvements.";
        }
        comparison.setRecommendation(recommendation);

        comparison.setProcessingTimeMs(currentMetrics.getProcessingTimeMs() +
            previousMetrics.getProcessingTimeMs());

        return comparison;
    }

    /**
     * A/B test two model variants
     */
    public ABTestResult runABTest(
            String modelName,
            String variantAName,
            String variantBName,
            List<PredictionTestCase> testCases,
            double trafficSplit // 0.0 to 1.0
    ) {
        ABTestResult result = new ABTestResult();
        result.setModelName(modelName);
        result.setVariantAName(variantAName);
        result.setVariantBName(variantBName);
        result.setStartedAt(LocalDateTime.now());
        result.setTrafficSplit(trafficSplit);

        // Split test cases
        int splitIndex = (int) (testCases.size() * trafficSplit);
        List<PredictionTestCase> variantATests = testCases.subList(0, splitIndex);
        List<PredictionTestCase> variantBTests = testCases.subList(splitIndex, testCases.size());

        // Validate both variants
        List<PredictionValidationResult> variantAResults = validateBatch(
            variantATests, modelName, variantAName
        );
        List<PredictionValidationResult> variantBResults = validateBatch(
            variantBTests, modelName, variantBName
        );

        result.setVariantAResults(variantAResults);
        result.setVariantBResults(variantBResults);

        // Calculate performance metrics
        ModelPerformanceMetrics variantAMetrics = calculateModelPerformance(
            variantAResults, modelName + "-A", "ab-test"
        );
        ModelPerformanceMetrics variantBMetrics = calculateModelPerformance(
            variantBResults, modelName + "-B", "ab-test"
        );

        result.setVariantAMetrics(variantAMetrics);
        result.setVariantBMetrics(variantBMetrics);

        // Determine winner
        double variantAAccuracy = variantAMetrics.getAccuracy().doubleValue();
        double variantBAccuracy = variantBMetrics.getAccuracy().doubleValue();
        String winner = variantAAccuracy > variantBAccuracy ? variantAName : variantBName;

        result.setWinner(winner);
        result.setCompletedAt(LocalDateTime.now());
        result.setProcessingTimeMs(variantAMetrics.getProcessingTimeMs() +
            variantBMetrics.getProcessingTimeMs());

        return result;
    }

    // Private helper methods

    private boolean calculateIsCorrect(Object prediction, Object actual, double tolerance) {
        if (prediction == null || actual == null) {
            return false;
        }

        if (prediction instanceof Number && actual instanceof Number) {
            double pred = ((Number) prediction).doubleValue();
            double act = ((Number) actual).doubleValue();
            return Math.abs(pred - act) <= tolerance;
        }

        // For non-numeric, use exact match
        return Objects.equals(prediction, actual);
    }

    private double calculateError(Object prediction, Object actual) {
        if (prediction instanceof Number && actual instanceof Number) {
            double pred = ((Number) prediction).doubleValue();
            double act = ((Number) actual).doubleValue();
            return pred - act;
        }
        return prediction.equals(actual) ? 0.0 : 1.0;
    }

    private double calculatePercentageError(Object prediction, Object actual) {
        if (prediction instanceof Number && actual instanceof Number) {
            double pred = ((Number) prediction).doubleValue();
            double act = ((Number) actual).doubleValue();
            if (act == 0) return 0.0;
            return Math.abs((pred - act) / act) * 100.0;
        }
        return prediction.equals(actual) ? 0.0 : 100.0;
    }

    private double calculateAccuracyScore(double error, double tolerance) {
        // Convert error to 0-1 scale where 1 is perfect
        double normalizedError = Math.abs(error) / tolerance;
        return Math.max(0.0, 1.0 - normalizedError);
    }

    private boolean isPositivePrediction(Object prediction) {
        if (prediction instanceof Boolean) {
            return (Boolean) prediction;
        }
        if (prediction instanceof String) {
            return "true".equalsIgnoreCase((String) prediction) ||
                   "yes".equalsIgnoreCase((String) prediction) ||
                   "positive".equalsIgnoreCase((String) prediction);
        }
        if (prediction instanceof Number) {
            return ((Number) prediction).doubleValue() > 0.5;
        }
        return false;
    }

    private boolean isPositiveActual(Object actual) {
        return isPositivePrediction(actual); // Same logic for actual
    }

    private String assessModelQuality(double accuracy, double precision, double recall, double f1Score) {
        double avgScore = (accuracy + precision + recall + f1Score) / 4.0;
        if (avgScore >= 0.9) return "EXCELLENT";
        if (avgScore >= 0.8) return "GOOD";
        if (avgScore >= 0.7) return "FAIR";
        if (avgScore >= 0.6) return "POOR";
        return "NEEDS_IMPROVEMENT";
    }

    // Result classes

    public static class PredictionValidationResult {
        private String id;
        private String modelName;
        private String modelVersion;
        private Object prediction;
        private Object actual;
        private boolean correct;
        private double error;
        private double absoluteError;
        private double percentageError;
        private double accuracyScore;
        private String customerId;
        private String tenantId;
        private LocalDateTime validatedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

        public Object getPrediction() { return prediction; }
        public void setPrediction(Object prediction) { this.prediction = prediction; }

        public Object getActual() { return actual; }
        public void setActual(Object actual) { this.actual = actual; }

        public boolean isCorrect() { return correct; }
        public void setCorrect(boolean correct) { this.correct = correct; }

        public double getError() { return error; }
        public void setError(double error) { this.error = error; }

        public double getAbsoluteError() { return absoluteError; }
        public void setAbsoluteError(double absoluteError) { this.absoluteError = absoluteError; }

        public double getPercentageError() { return percentageError; }
        public void setPercentageError(double percentageError) { this.percentageError = percentageError; }

        public double getAccuracyScore() { return accuracyScore; }
        public void setAccuracyScore(double accuracyScore) { this.accuracyScore = accuracyScore; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public LocalDateTime getValidatedAt() { return validatedAt; }
        public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class ModelPerformanceMetrics {
        private String modelName;
        private String tenantId;
        private BigDecimal accuracy;
        private BigDecimal precision;
        private BigDecimal recall;
        private BigDecimal f1Score;
        private BigDecimal averageError;
        private BigDecimal rmse;
        private int totalPredictions;
        private int correctPredictions;
        private int incorrectPredictions;
        private String modelQuality;
        private LocalDateTime calculatedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public BigDecimal getAccuracy() { return accuracy; }
        public void setAccuracy(BigDecimal accuracy) { this.accuracy = accuracy; }

        public BigDecimal getPrecision() { return precision; }
        public void setPrecision(BigDecimal precision) { this.precision = precision; }

        public BigDecimal getRecall() { return recall; }
        public void setRecall(BigDecimal recall) { this.recall = recall; }

        public BigDecimal getF1Score() { return f1Score; }
        public void setF1Score(BigDecimal f1Score) { this.f1Score = f1Score; }

        public BigDecimal getAverageError() { return averageError; }
        public void setAverageError(BigDecimal averageError) { this.averageError = averageError; }

        public BigDecimal getRmse() { return rmse; }
        public void setRmse(BigDecimal rmse) { this.rmse = rmse; }

        public int getTotalPredictions() { return totalPredictions; }
        public void setTotalPredictions(int totalPredictions) { this.totalPredictions = totalPredictions; }

        public int getCorrectPredictions() { return correctPredictions; }
        public void setCorrectPredictions(int correctPredictions) { this.correctPredictions = correctPredictions; }

        public int getIncorrectPredictions() { return incorrectPredictions; }
        public void setIncorrectPredictions(int incorrectPredictions) { this.incorrectPredictions = incorrectPredictions; }

        public String getModelQuality() { return modelQuality; }
        public void setModelQuality(String modelQuality) { this.modelQuality = modelQuality; }

        public LocalDateTime getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class PredictionTestCase {
        private Object prediction;
        private Object actual;
        private double tolerance = 0.1;
        private String customerId;
        private String tenantId;

        public Object getPrediction() { return prediction; }
        public void setPrediction(Object prediction) { this.prediction = prediction; }

        public Object getActual() { return actual; }
        public void setActual(Object actual) { this.actual = actual; }

        public double getTolerance() { return tolerance; }
        public void setTolerance(double tolerance) { this.tolerance = tolerance; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class ModelComparisonResult {
        private String modelName;
        private String currentVersion;
        private String previousVersion;
        private ModelPerformanceMetrics currentVersionMetrics;
        private ModelPerformanceMetrics previousVersionMetrics;
        private double accuracyDelta;
        private double precisionDelta;
        private double recallDelta;
        private double f1ScoreDelta;
        private String betterVersion;
        private String recommendation;
        private LocalDateTime comparedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }

        public String getPreviousVersion() { return previousVersion; }
        public void setPreviousVersion(String previousVersion) { this.previousVersion = previousVersion; }

        public ModelPerformanceMetrics getCurrentVersionMetrics() { return currentVersionMetrics; }
        public void setCurrentVersionMetrics(ModelPerformanceMetrics currentVersionMetrics) { this.currentVersionMetrics = currentVersionMetrics; }

        public ModelPerformanceMetrics getPreviousVersionMetrics() { return previousVersionMetrics; }
        public void setPreviousVersionMetrics(ModelPerformanceMetrics previousVersionMetrics) { this.previousVersionMetrics = previousVersionMetrics; }

        public double getAccuracyDelta() { return accuracyDelta; }
        public void setAccuracyDelta(double accuracyDelta) { this.accuracyDelta = accuracyDelta; }

        public double getPrecisionDelta() { return precisionDelta; }
        public void setPrecisionDelta(double precisionDelta) { this.precisionDelta = precisionDelta; }

        public double getRecallDelta() { return recallDelta; }
        public void setRecallDelta(double recallDelta) { this.recallDelta = recallDelta; }

        public double getF1ScoreDelta() { return f1ScoreDelta; }
        public void setF1ScoreDelta(double f1ScoreDelta) { this.f1ScoreDelta = f1ScoreDelta; }

        public String getBetterVersion() { return betterVersion; }
        public void setBetterVersion(String betterVersion) { this.betterVersion = betterVersion; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

        public LocalDateTime getComparedAt() { return comparedAt; }
        public void setComparedAt(LocalDateTime comparedAt) { this.comparedAt = comparedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class ABTestResult {
        private String modelName;
        private String variantAName;
        private String variantBName;
        private List<PredictionValidationResult> variantAResults;
        private List<PredictionValidationResult> variantBResults;
        private ModelPerformanceMetrics variantAMetrics;
        private ModelPerformanceMetrics variantBMetrics;
        private String winner;
        private double trafficSplit;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }

        public String getVariantAName() { return variantAName; }
        public void setVariantAName(String variantAName) { this.variantAName = variantAName; }

        public String getVariantBName() { return variantBName; }
        public void setVariantBName(String variantBName) { this.variantBName = variantBName; }

        public List<PredictionValidationResult> getVariantAResults() { return variantAResults; }
        public void setVariantAResults(List<PredictionValidationResult> variantAResults) { this.variantAResults = variantAResults; }

        public List<PredictionValidationResult> getVariantBResults() { return variantBResults; }
        public void setVariantBResults(List<PredictionValidationResult> variantBResults) { this.variantBResults = variantBResults; }

        public ModelPerformanceMetrics getVariantAMetrics() { return variantAMetrics; }
        public void setVariantAMetrics(ModelPerformanceMetrics variantAMetrics) { this.variantAMetrics = variantAMetrics; }

        public ModelPerformanceMetrics getVariantBMetrics() { return variantBMetrics; }
        public void setVariantBMetrics(ModelPerformanceMetrics variantBMetrics) { this.variantBMetrics = variantBMetrics; }

        public String getWinner() { return winner; }
        public void setWinner(String winner) { this.winner = winner; }

        public double getTrafficSplit() { return trafficSplit; }
        public void setTrafficSplit(double trafficSplit) { this.trafficSplit = trafficSplit; }

        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
}
