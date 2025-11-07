package com.droid.bss.infrastructure.event.sourcing;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for projected read models
 */
public class ProjectedReadModel {

    private String id;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
    private final Map<String, Object> data = new HashMap<>();

    public ProjectedReadModel() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public <T> T getData(String key, Class<T> type) {
        return type.cast(data.get(key));
    }

    public void updateVersion() {
        this.version++;
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
