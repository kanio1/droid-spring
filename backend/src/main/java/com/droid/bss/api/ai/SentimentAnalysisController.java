/**
 * Sentiment Analysis REST API Controller
 *
 * Exposes sentiment analysis functionality via REST endpoints
 */

package com.droid.bss.api.ai;

import com.droid.bss.application.service.ai.SentimentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sentiment")
@Tag(name = "Sentiment Analysis", description = "AI-powered sentiment analysis API")
public class SentimentAnalysisController {

    private final SentimentAnalysisService sentimentService;

    @Autowired
    public SentimentAnalysisController(SentimentAnalysisService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @PostMapping("/analyze")
    @Operation(
        summary = "Analyze sentiment of text",
        description = "Analyze the sentiment of provided text including emotions and topics"
    )
    @ApiResponse(responseCode = "200", description = "Successfully analyzed sentiment")
    public ResponseEntity<SentimentAnalysisService.SentimentAnalysisResult> analyzeSentiment(
            @Parameter(description = "Text to analyze", required = true)
            @RequestBody SentimentAnalysisRequest request
    ) {
        SentimentAnalysisService.SentimentAnalysisResult result = sentimentService.analyzeFeedback(
            request.getText(),
            request.getLanguage() != null ? request.getLanguage() : "en"
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze-batch")
    @Operation(
        summary = "Batch analyze sentiment",
        description = "Analyze sentiment of multiple texts at once"
    )
    @ApiResponse(responseCode = "200", description = "Successfully analyzed batch sentiment")
    public ResponseEntity<SentimentAnalysisService.SentimentAnalysisBatchResult> analyzeBatchSentiment(
            @Parameter(description = "Batch analysis request", required = true)
            @RequestBody BatchSentimentAnalysisRequest request
    ) {
        SentimentAnalysisService.SentimentAnalysisBatchResult result = sentimentService.analyzeBatch(
            request.getTexts(),
            request.getLanguage() != null ? request.getLanguage() : "en"
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analyze-trend/{customerId}")
    @Operation(
        summary = "Analyze sentiment trend",
        description = "Analyze customer sentiment trends over time"
    )
    @ApiResponse(responseCode = "200", description = "Successfully analyzed sentiment trend")
    public ResponseEntity<SentimentAnalysisService.SentimentTrendAnalysis> analyzeSentimentTrend(
            @Parameter(description = "Customer ID", required = true)
            @PathVariable String customerId,
            @Parameter(description = "Tenant ID", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Timeframe for analysis", example = "30d")
            @RequestParam(defaultValue = "30d") String timeframe
    ) {
        SentimentAnalysisService.SentimentTrendAnalysis result = sentimentService.analyzeSentimentTrend(
            customerId,
            tenantId,
            timeframe
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the sentiment analysis service is operational"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Sentiment Analysis Service",
            "version", "1.0.0"
        ));
    }

    // Request classes

    public static class SentimentAnalysisRequest {
        private String text;
        private String language;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    public static class BatchSentimentAnalysisRequest {
        private List<String> texts;
        private String language;

        public List<String> getTexts() { return texts; }
        public void setTexts(List<String> texts) { this.texts = texts; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
}
