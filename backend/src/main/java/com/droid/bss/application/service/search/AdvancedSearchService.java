package com.droid.bss.application.service.search;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.search.*;
import com.droid.bss.application.service.PerformanceCacheService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advanced Search Service
 * Provides full-text search capabilities with PostgreSQL
 */
@Service
@Transactional(readOnly = true)
public class AdvancedSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PerformanceCacheService cacheService;
    private final SearchMetricsCollector metricsCollector;

    public AdvancedSearchService(
            PerformanceCacheService cacheService,
            SearchMetricsCollector metricsCollector) {
        this.cacheService = cacheService;
        this.metricsCollector = metricsCollector;
    }

    /**
     * Advanced search with multiple filters
     */
    @Cacheable(value = "search", key = "#request.query + '_' + #request.entityType + '_' + #request.page + '_' + #request.size")
    public PageResponse<SearchResult> advancedSearch(AdvancedSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            List<SearchResult> results;

            if (request.getEntityType() == null || request.getEntityType() == AdvancedSearchRequest.SearchEntity.ALL) {
                results = searchAllEntities(request);
            } else {
                results = searchByEntityType(request);
            }

            long endTime = System.currentTimeMillis();
            metricsCollector.recordSearch(request, results.size(), endTime - startTime);

            return PageResponse.of(results, request.getPage(), request.getSize(), (long) results.size());

        } catch (Exception e) {
            metricsCollector.recordFailedSearch(request);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Quick search across all entities
     */
    public List<SearchResult> quickSearch(String query, int limit) {
        return cacheService.getOrCompute(
            "quick_search:" + query + ":" + limit,
            List.class,
            () -> {
                List<SearchResult> results = new ArrayList<>();

                // Search customers
                results.addAll(searchCustomersQuick(query, limit / 3));

                // Search invoices
                results.addAll(searchInvoicesQuick(query, limit / 3));

                // Search products
                results.addAll(searchProductsQuick(query, limit / 3));

                return results.stream()
                    .sorted((r1, r2) -> Double.compare(r2.getRelevanceScore(), r1.getRelevanceScore()))
                    .limit(limit)
                    .collect(Collectors.toList());
            }
        );
    }

    /**
     * Search customers
     */
    public PageResponse<SearchResult> searchCustomers(AdvancedSearchRequest request) {
        String jpql = buildCustomerSearchQuery(request);
        var query = entityManager.createNativeQuery(jpql);

        // Set parameters
        setSearchParameters(query, request);

        query.setFirstResult(request.getPage() * request.getSize());
        query.setMaxResults(request.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<SearchResult> results = rows.stream()
            .map(this::mapCustomerRowToResult)
            .collect(Collectors.toList());

        return PageResponse.of(results, request.getPage(), request.getSize(), (long) results.size());
    }

    /**
     * Search invoices
     */
    public PageResponse<SearchResult> searchInvoices(AdvancedSearchRequest request) {
        String jpql = buildInvoiceSearchQuery(request);
        var query = entityManager.createNativeQuery(jpql);

        setSearchParameters(query, request);

        query.setFirstResult(request.getPage() * request.getSize());
        query.setMaxResults(request.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<SearchResult> results = rows.stream()
            .map(this::mapInvoiceRowToResult)
            .collect(Collectors.toList());

        return PageResponse.of(results, request.getPage(), request.getSize(), (long) results.size());
    }

    /**
     * Search products
     */
    public PageResponse<SearchResult> searchProducts(AdvancedSearchRequest request) {
        String jpql = buildProductSearchQuery(request);
        var query = entityManager.createNativeQuery(jpql);

        setSearchParameters(query, request);

        query.setFirstResult(request.getPage() * request.getSize());
        query.setMaxResults(request.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<SearchResult> results = rows.stream()
            .map(this::mapProductRowToResult)
            .collect(Collectors.toList());

        return PageResponse.of(results, request.getPage(), request.getSize(), (long) results.size());
    }

    /**
     * Get search suggestions
     */
    public List<String> getSuggestions(String query, int limit) {
        return cacheService.getOrCompute(
            "suggestions:" + query + ":" + limit,
            List.class,
            () -> {
                List<String> suggestions = new ArrayList<>();

                // Get suggestions from customers
                var queryObj = entityManager.createNativeQuery(
                    "SELECT DISTINCT first_name FROM customers WHERE first_name ILIKE ?1 LIMIT ?2"
                );
                queryObj.setParameter(1, query + "%");
                queryObj.setParameter(2, limit / 2);
                suggestions.addAll(queryObj.getResultList());

                // Get suggestions from last names
                queryObj = entityManager.createNativeQuery(
                    "SELECT DISTINCT last_name FROM customers WHERE last_name ILIKE ?1 LIMIT ?2"
                );
                queryObj.setParameter(1, query + "%");
                queryObj.setParameter(2, limit / 2);
                suggestions.addAll(queryObj.getResultList());

                return suggestions.stream()
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());
            }
        );
    }

    /**
     * Save search
     */
    public String saveSearch(SavedSearchRequest request) {
        // Implementation would save to database
        // For now, return a generated ID
        return "saved-search-" + System.currentTimeMillis();
    }

    /**
     * Get saved searches
     */
    public List<SavedSearchRequest> getSavedSearches() {
        // Implementation would load from database
        return List.of();
    }

    /**
     * Delete saved search
     */
    public void deleteSavedSearch(String searchId) {
        // Implementation would delete from database
    }

    /**
     * Get search statistics
     */
    public SearchStatistics getSearchStatistics() {
        return metricsCollector.getStatistics();
    }

    // Private helper methods

    private List<SearchResult> searchAllEntities(AdvancedSearchRequest request) {
        List<SearchResult> results = new ArrayList<>();

        // Search in customers
        results.addAll(searchCustomersQuick(request.getQuery(), 10));

        // Search in invoices
        results.addAll(searchInvoicesQuick(request.getQuery(), 10));

        // Search in products
        results.addAll(searchProductsQuick(request.getQuery(), 10));

        return results.stream()
            .sorted((r1, r2) -> Double.compare(r2.getRelevanceScore(), r1.getRelevanceScore()))
            .collect(Collectors.toList());
    }

    private List<SearchResult> searchByEntityType(AdvancedSearchRequest request) {
        return switch (request.getEntityType()) {
            case CUSTOMER -> searchCustomersQuick(request.getQuery(), request.getSize());
            case INVOICE -> searchInvoicesQuick(request.getQuery(), request.getSize());
            case PRODUCT -> searchProductsQuick(request.getQuery(), request.getSize());
            default -> List.of();
        };
    }

    private List<SearchResult> searchCustomersQuick(String query, int limit) {
        var queryObj = entityManager.createNativeQuery(
            "SELECT * FROM advanced_customer_search(?, NULL, ?, 0)"
        );
        queryObj.setParameter(1, query);
        queryObj.setParameter(2, limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = queryObj.getResultList();

        return rows.stream()
            .map(this::mapCustomerRowToResult)
            .collect(Collectors.toList());
    }

    private List<SearchResult> searchInvoicesQuick(String query, int limit) {
        var queryObj = entityManager.createNativeQuery(
            "SELECT id, invoice_number, description, status, created_at FROM invoices " +
            "WHERE search_vector @@ plainto_tsquery('english', ?) " +
            "LIMIT ?"
        );
        queryObj.setParameter(1, query);
        queryObj.setParameter(2, limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = queryObj.getResultList();

        return rows.stream()
            .map(this::mapInvoiceRowToResult)
            .collect(Collectors.toList());
    }

    private List<SearchResult> searchProductsQuick(String query, int limit) {
        var queryObj = entityManager.createNativeQuery(
            "SELECT id, name, description, category, created_at FROM products " +
            "WHERE search_vector @@ plainto_tsquery('english', ?) " +
            "LIMIT ?"
        );
        queryObj.setParameter(1, query);
        queryObj.setParameter(2, limit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = queryObj.getResultList();

        return rows.stream()
            .map(this::mapProductRowToResult)
            .collect(Collectors.toList());
    }

    private String buildCustomerSearchQuery(AdvancedSearchRequest request) {
        StringBuilder jpql = new StringBuilder(
            "SELECT c.id, c.first_name, c.last_name, c.email, c.phone, c.status, c.created_at, " +
            "ts_rank(c.search_vector, plainto_tsquery('english', :query)) as rank " +
            "FROM customers c WHERE 1=1 "
        );

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            jpql.append("AND (c.search_vector @@ plainto_tsquery('english', :query) OR ")
                .append("c.first_name ILIKE :queryLike OR ")
                .append("c.last_name ILIKE :queryLike OR ")
                .append("c.email ILIKE :queryLike) ");
        }

        if (request.getStatusFilters() != null && !request.getStatusFilters().isEmpty()) {
            jpql.append("AND c.status IN :statuses ");
        }

        jpql.append("ORDER BY rank DESC, c.created_at DESC");

        return jpql.toString();
    }

    private String buildInvoiceSearchQuery(AdvancedSearchRequest request) {
        StringBuilder jpql = new StringBuilder(
            "SELECT i.id, i.invoice_number, i.description, i.status, i.created_at " +
            "FROM invoices i WHERE 1=0 OR i.search_vector @@ plainto_tsquery('english', :query) "
        );

        if (request.getStatusFilters() != null && !request.getStatusFilters().isEmpty()) {
            jpql.append("AND i.status IN :statuses ");
        }

        jpql.append("ORDER BY i.created_at DESC");

        return jpql.toString();
    }

    private String buildProductSearchQuery(AdvancedSearchRequest request) {
        StringBuilder jpql = new StringBuilder(
            "SELECT p.id, p.name, p.description, p.category, p.created_at " +
            "FROM products p WHERE 1=0 OR p.search_vector @@ plainto_tsquery('english', :query) "
        );

        if (request.getAdditionalFilters() != null && request.getAdditionalFilters().containsKey("category")) {
            jpql.append("AND p.category = :category ");
        }

        jpql.append("ORDER BY p.created_at DESC");

        return jpql.toString();
    }

    private void setSearchParameters(jakarta.persistence.Query query, AdvancedSearchRequest request) {
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            query.setParameter("query", request.getQuery());
            query.setParameter("queryLike", "%" + request.getQuery() + "%");
        }

        if (request.getStatusFilters() != null && !request.getStatusFilters().isEmpty()) {
            query.setParameter("statuses", request.getStatusFilters());
        }

        if (request.getAdditionalFilters() != null && request.getAdditionalFilters().containsKey("category")) {
            query.setParameter("category", request.getAdditionalFilters().get("category"));
        }
    }

    private SearchResult mapCustomerRowToResult(Object[] row) {
        SearchResult result = new SearchResult(
            row[0].toString(),
            "customer",
            row[1] + " " + row[2],
            (String) row[3]
        );
        result.setStatus((String) row[5]);
        result.setCreatedAt((java.time.LocalDateTime) row[6]);
        result.setRelevanceScore(row[7] != null ? ((Number) row[7]).doubleValue() : 0.0);
        return result;
    }

    private SearchResult mapInvoiceRowToResult(Object[] row) {
        SearchResult result = new SearchResult(
            row[0].toString(),
            "invoice",
            (String) row[1],
            (String) row[2]
        );
        result.setStatus((String) row[3]);
        result.setCreatedAt((java.time.LocalDateTime) row[4]);
        result.setRelevanceScore(0.8);
        return result;
    }

    private SearchResult mapProductRowToResult(Object[] row) {
        SearchResult result = new SearchResult(
            row[0].toString(),
            "product",
            (String) row[1],
            (String) row[2]
        );
        result.setStatus((String) row[3]);
        result.setCreatedAt((java.time.LocalDateTime) row[4]);
        result.setRelevanceScore(0.7);
        return result;
    }
}
