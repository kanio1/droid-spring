package com.droid.bss.application.dto.search;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saved Search DTO
 */
public class SavedSearchRequest {

    private String id;
    private String name;
    private String description;
    private AdvancedSearchRequest searchCriteria;
    private String userId;
    private boolean isPublic;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;

    public SavedSearchRequest() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AdvancedSearchRequest getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(AdvancedSearchRequest searchCriteria) {
        this.searchCriteria = searchCriteria;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
}
