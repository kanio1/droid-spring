package com.droid.bss.application.dto.search;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for advanced search operations
 */
public class AdvancedSearchRequest {

    private String query;
    private SearchEntity entityType;
    private List<String> statusFilters;
    private List<String> tags;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String sortBy;
    private String sortOrder; // "asc" or "desc"
    private int page = 0;
    private int size = 20;
    private Map<String, Object> additionalFilters;

    public AdvancedSearchRequest() {}

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public SearchEntity getEntityType() {
        return entityType;
    }

    public void setEntityType(SearchEntity entityType) {
        this.entityType = entityType;
    }

    public List<String> getStatusFilters() {
        return statusFilters;
    }

    public void setStatusFilters(List<String> statusFilters) {
        this.statusFilters = statusFilters;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Map<String, Object> getAdditionalFilters() {
        return additionalFilters;
    }

    public void setAdditionalFilters(Map<String, Object> additionalFilters) {
        this.additionalFilters = additionalFilters;
    }

    public enum SearchEntity {
        CUSTOMER,
        INVOICE,
        PRODUCT,
        ORDER,
        PAYMENT,
        ALL
    }
}
