package com.droid.bss.application.dto.search;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Search Result DTO
 */
public class SearchResult {

    private String id;
    private String entityType; // "customer", "invoice", "product"
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private double relevanceScore; // 0.0 to 1.0
    private Map<String, Object> metadata;
    private String highlight; // Highlighted text from search

    public SearchResult() {}

    public SearchResult(String id, String entityType, String title, String description) {
        this.id = id;
        this.entityType = entityType;
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }
}
