package com.droid.bss.api.search;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.search.AdvancedSearchRequest;
import com.droid.bss.application.dto.search.SearchResult;
import com.droid.bss.application.dto.search.SavedSearchRequest;
import com.droid.bss.application.service.search.AdvancedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Advanced Search API
 * Provides full-text search, filtering, sorting, and saved searches
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Advanced Search", description = "Advanced search and filtering operations")
public class AdvancedSearchController {

    private final AdvancedSearchService searchService;

    public AdvancedSearchController(AdvancedSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Advanced search with filters
     */
    @PostMapping("/advanced")
    @Operation(summary = "Advanced search", description = "Perform advanced search with filters")
    public ResponseEntity<PageResponse<SearchResult>> advancedSearch(@RequestBody AdvancedSearchRequest request) {
        return ResponseEntity.ok(searchService.advancedSearch(request));
    }

    /**
     * Quick search (simple text search)
     */
    @GetMapping("/quick")
    @Operation(summary = "Quick search", description = "Simple text search across multiple entities")
    public ResponseEntity<List<SearchResult>> quickSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(searchService.quickSearch(query, limit));
    }

    /**
     * Search customers with advanced filters
     */
    @PostMapping("/customers")
    @Operation(summary = "Search customers", description = "Search customers with advanced filters")
    public ResponseEntity<PageResponse<SearchResult>> searchCustomers(@RequestBody AdvancedSearchRequest request) {
        return ResponseEntity.ok(searchService.searchCustomers(request));
    }

    /**
     * Search invoices
     */
    @PostMapping("/invoices")
    @Operation(summary = "Search invoices", description = "Search invoices with filters")
    public ResponseEntity<PageResponse<SearchResult>> searchInvoices(@RequestBody AdvancedSearchRequest request) {
        return ResponseEntity.ok(searchService.searchInvoices(request));
    }

    /**
     * Search products
     */
    @PostMapping("/products")
    @Operation(summary = "Search products", description = "Search products with filters")
    public ResponseEntity<PageResponse<SearchResult>> searchProducts(@RequestBody AdvancedSearchRequest request) {
        return ResponseEntity.ok(searchService.searchProducts(request));
    }

    /**
     * Get search suggestions (auto-complete)
     */
    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get auto-complete suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.getSuggestions(query, limit));
    }

    /**
     * Save search for later use
     */
    @PostMapping("/saved")
    @Operation(summary = "Save search", description = "Save current search for later use")
    public ResponseEntity<String> saveSearch(@RequestBody SavedSearchRequest request) {
        return ResponseEntity.ok(searchService.saveSearch(request));
    }

    /**
     * Get saved searches for current user
     */
    @GetMapping("/saved")
    @Operation(summary = "Get saved searches", description = "Get all saved searches for current user")
    public ResponseEntity<List<SavedSearchRequest>> getSavedSearches() {
        return ResponseEntity.ok(searchService.getSavedSearches());
    }

    /**
     * Delete saved search
     */
    @DeleteMapping("/saved/{searchId}")
    @Operation(summary = "Delete saved search", description = "Delete a saved search")
    public ResponseEntity<Void> deleteSavedSearch(@PathVariable String searchId) {
        searchService.deleteSavedSearch(searchId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get search statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get search statistics", description = "Get search performance statistics")
    public ResponseEntity<SearchStatistics> getSearchStats() {
        return ResponseEntity.ok(searchService.getSearchStatistics());
    }
}
