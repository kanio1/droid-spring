/**
 * Sentiment Analysis Service
 *
 * Analyzes text for sentiment and emotional content
 * Supports multiple languages and provides detailed emotional insights
 */

package com.droid.bss.application.service.ai;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@Service
public class SentimentAnalysisService {

    /**
     * Analyze sentiment of customer feedback or communication
     */
    public SentimentAnalysisResult analyzeFeedback(String feedbackText, String language) {
        // Mock AI analysis - in real implementation, would use ML model like BERT, VADER, or similar
        SentimentAnalysisResult result = new SentimentAnalysisResult();
        result.setText(feedbackText);
        result.setLanguage(language);
        result.setAnalyzedAt(java.time.LocalDateTime.now());

        // Simple mock analysis based on keywords
        String lowerText = feedbackText.toLowerCase();
        double sentimentScore = calculateSentimentScore(lowerText);
        SentimentType sentimentType = determineSentimentType(sentimentScore);
        double confidence = 0.70 + Math.random() * 0.25;

        result.setSentimentType(sentimentType);
        result.setSentimentScore(sentimentScore);
        result.setConfidenceScore(new BigDecimal(confidence));

        // Extract emotions
        Map<String, Double> emotions = extractEmotions(lowerText);
        result.setEmotions(emotions);

        // Extract keywords
        List<String> keywords = extractKeywords(lowerText);
        result.setKeywords(keywords);

        // Extract topics
        List<String> topics = extractTopics(lowerText);
        result.setTopics(topics);

        // Generate summary
        result.setSummary(generateSummary(sentimentType, emotions, confidence));

        // Calculate processing time (mock)
        result.setProcessingTimeMs((long) (50 + Math.random() * 200));

        return result;
    }

    /**
     * Analyze sentiment across multiple texts (batch processing)
     */
    public SentimentAnalysisBatchResult analyzeBatch(List<String> texts, String language) {
        SentimentAnalysisBatchResult batchResult = new SentimentAnalysisBatchResult();
        batchResult.setLanguage(language);
        batchResult.setAnalyzedAt(java.time.LocalDateTime.now());
        batchResult.setTotalTexts(texts.size());

        List<SentimentAnalysisResult> results = texts.stream()
            .map(text -> analyzeFeedback(text, language))
            .toList();

        batchResult.setResults(results);

        // Calculate summary statistics
        long positiveCount = results.stream()
            .mapToLong(r -> r.getSentimentType() == SentimentType.POSITIVE ? 1 : 0)
            .sum();
        long negativeCount = results.stream()
            .mapToLong(r -> r.getSentimentType() == SentimentType.NEGATIVE ? 1 : 0)
            .sum();
        long neutralCount = results.stream()
            .mapToLong(r -> r.getSentimentType() == SentimentType.NEUTRAL ? 1 : 0)
            .sum();

        batchResult.setPositiveCount(positiveCount);
        batchResult.setNegativeCount(negativeCount);
        batchResult.setNeutralCount(neutralCount);

        double averageConfidence = results.stream()
            .mapToDouble(r -> r.getConfidenceScore().doubleValue())
            .average()
            .orElse(0.0);

        batchResult.setAverageConfidence(new BigDecimal(averageConfidence));
        batchResult.setProcessingTimeMs(results.stream()
            .mapToLong(SentimentAnalysisResult::getProcessingTimeMs)
            .sum());

        return batchResult;
    }

    /**
     * Compare sentiment across different time periods
     */
    public SentimentTrendAnalysis analyzeSentimentTrend(String customerId, String tenantId, String timeframe) {
        SentimentTrendAnalysis analysis = new SentimentTrendAnalysis();
        analysis.setCustomerId(customerId);
        analysis.setTenantId(tenantId);
        analysis.setTimeframe(timeframe);
        analysis.setAnalyzedAt(java.time.LocalDateTime.now());

        // Mock trend data - in real implementation, would query historical sentiment data
        double currentScore = -0.2 + Math.random() * 0.4; // -0.2 to 0.2
        double previousScore = -0.3 + Math.random() * 0.6; // -0.3 to 0.3

        analysis.setCurrentPeriodScore(currentScore);
        analysis.setPreviousPeriodScore(previousScore);
        analysis.setTrendDirection(currentScore > previousScore ? "IMPROVING" : "DECLINING");
        analysis.setTrendStrength(Math.abs(currentScore - previousScore));

        analysis.setConfidenceScore(new BigDecimal(0.75 + Math.random() * 0.20));
        analysis.setProcessingTimeMs((long) (100 + Math.random() * 200));

        return analysis;
    }

    // Private helper methods

    private double calculateSentimentScore(String text) {
        // Mock sentiment scoring based on positive/negative word counts
        List<String> positiveWords = Arrays.asList(
            "good", "great", "excellent", "amazing", "love", "best", "wonderful",
            "happy", "satisfied", "pleased", "fantastic", "awesome", "perfect",
            "recommend", "thanks", "thank you", "appreciate", "outstanding"
        );

        List<String> negativeWords = Arrays.asList(
            "bad", "terrible", "awful", "hate", "worst", "horrible", "disappointing",
            "angry", "frustrated", "upset", "poor", "useless", "broken",
            "complaint", "problem", "issue", "refund", "cancel", "dissatisfied"
        );

        long positiveCount = positiveWords.stream()
            .mapToLong(word -> text.contains(word) ? 1 : 0)
            .sum();

        long negativeCount = negativeWords.stream()
            .mapToLong(word -> text.contains(word) ? 1 : 0)
            .sum();

        // Simple scoring: range from -1 (very negative) to +1 (very positive)
        double score = (positiveCount - negativeCount) / 5.0;
        return Math.max(-1.0, Math.min(1.0, score));
    }

    private SentimentType determineSentimentType(double score) {
        if (score > 0.1) return SentimentType.POSITIVE;
        if (score < -0.1) return SentimentType.NEGATIVE;
        return SentimentType.NEUTRAL;
    }

    private Map<String, Double> extractEmotions(String text) {
        Map<String, Double> emotions = new HashMap<>();

        // Mock emotion detection
        double joy = Math.random() * 0.8;
        double anger = text.contains("angry") || text.contains("frustrated") ? 0.6 + Math.random() * 0.3 : Math.random() * 0.2;
        double fear = text.contains("worried") || text.contains("concerned") ? 0.5 + Math.random() * 0.4 : Math.random() * 0.2;
        double sadness = text.contains("sad") || text.contains("disappointed") ? 0.5 + Math.random() * 0.4 : Math.random() * 0.2;
        double surprise = text.contains("surprised") || text.contains("unexpected") ? 0.4 + Math.random() * 0.5 : Math.random() * 0.3;

        emotions.put("joy", joy);
        emotions.put("anger", anger);
        emotions.put("fear", fear);
        emotions.put("sadness", sadness);
        emotions.put("surprise", surprise);

        return emotions;
    }

    private List<String> extractKeywords(String text) {
        // Mock keyword extraction - would use NLP techniques in real implementation
        List<String> allKeywords = Arrays.asList(
            "product", "service", "quality", "price", "support", "delivery",
            "experience", "customer", "satisfaction", "recommendation"
        );

        return allKeywords.stream()
            .filter(text::contains)
            .limit(5)
            .toList();
    }

    private List<String> extractTopics(String text) {
        // Mock topic extraction
        List<String> topics = new java.util.ArrayList<>();

        if (text.contains("delivery") || text.contains("shipping")) {
            topics.add("Logistics");
        }
        if (text.contains("price") || text.contains("cost") || text.contains("expensive")) {
            topics.add("Pricing");
        }
        if (text.contains("support") || text.contains("help") || text.contains("service")) {
            topics.add("Customer Support");
        }
        if (text.contains("quality") || text.contains("product") || text.contains("feature")) {
            topics.add("Product Quality");
        }

        return topics.isEmpty() ? List.of("General") : topics;
    }

    private String generateSummary(SentimentType sentiment, Map<String, Double> emotions, double confidence) {
        StringBuilder summary = new StringBuilder();
        summary.append("Overall sentiment is ").append(sentiment.name().toLowerCase());

        if (sentiment == SentimentType.POSITIVE) {
            summary.append(", with high satisfaction and positive emotions.");
        } else if (sentiment == SentimentType.NEGATIVE) {
            summary.append(", indicating issues that need attention.");
        } else {
            summary.append(", with neutral feedback requiring further review.");
        }

        summary.append(String.format(" Confidence: %.0f%%", confidence * 100));

        return summary.toString();
    }

    // Result classes

    public enum SentimentType {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    public static class SentimentAnalysisResult {
        private String text;
        private String language;
        private SentimentType sentimentType;
        private double sentimentScore;
        private BigDecimal confidenceScore;
        private Map<String, Double> emotions;
        private List<String> keywords;
        private List<String> topics;
        private String summary;
        private java.time.LocalDateTime analyzedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public SentimentType getSentimentType() { return sentimentType; }
        public void setSentimentType(SentimentType sentimentType) { this.sentimentType = sentimentType; }

        public double getSentimentScore() { return sentimentScore; }
        public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }

        public BigDecimal getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

        public Map<String, Double> getEmotions() { return emotions; }
        public void setEmotions(Map<String, Double> emotions) { this.emotions = emotions; }

        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }

        public List<String> getTopics() { return topics; }
        public void setTopics(List<String> topics) { this.topics = topics; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public java.time.LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(java.time.LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class SentimentAnalysisBatchResult {
        private String language;
        private int totalTexts;
        private long positiveCount;
        private long negativeCount;
        private long neutralCount;
        private BigDecimal averageConfidence;
        private List<SentimentAnalysisResult> results;
        private java.time.LocalDateTime analyzedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public int getTotalTexts() { return totalTexts; }
        public void setTotalTexts(int totalTexts) { this.totalTexts = totalTexts; }

        public long getPositiveCount() { return positiveCount; }
        public void setPositiveCount(long positiveCount) { this.positiveCount = positiveCount; }

        public long getNegativeCount() { return negativeCount; }
        public void setNegativeCount(long negativeCount) { this.negativeCount = negativeCount; }

        public long getNeutralCount() { return neutralCount; }
        public void setNeutralCount(long neutralCount) { this.neutralCount = neutralCount; }

        public BigDecimal getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(BigDecimal averageConfidence) { this.averageConfidence = averageConfidence; }

        public List<SentimentAnalysisResult> getResults() { return results; }
        public void setResults(List<SentimentAnalysisResult> results) { this.results = results; }

        public java.time.LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(java.time.LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    public static class SentimentTrendAnalysis {
        private String customerId;
        private String tenantId;
        private String timeframe;
        private double currentPeriodScore;
        private double previousPeriodScore;
        private String trendDirection;
        private double trendStrength;
        private BigDecimal confidenceScore;
        private java.time.LocalDateTime analyzedAt;
        private long processingTimeMs;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getTimeframe() { return timeframe; }
        public void setTimeframe(String timeframe) { this.timeframe = timeframe; }

        public double getCurrentPeriodScore() { return currentPeriodScore; }
        public void setCurrentPeriodScore(double currentPeriodScore) { this.currentPeriodScore = currentPeriodScore; }

        public double getPreviousPeriodScore() { return previousPeriodScore; }
        public void setPreviousPeriodScore(double previousPeriodScore) { this.previousPeriodScore = previousPeriodScore; }

        public String getTrendDirection() { return trendDirection; }
        public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }

        public double getTrendStrength() { return trendStrength; }
        public void setTrendStrength(double trendStrength) { this.trendStrength = trendStrength; }

        public BigDecimal getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

        public java.time.LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(java.time.LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
}
