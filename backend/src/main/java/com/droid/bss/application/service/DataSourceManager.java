package com.droid.bss.application.service;

import com.droid.bss.domain.datasource.DataSource;
import com.droid.bss.domain.datasource.DataSourceException;
import com.droid.bss.domain.datasource.DataSourceType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all data sources and orchestrates metric collection
 */
@Service
public class DataSourceManager {

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    public void registerDataSource(String name, DataSource dataSource) {
        dataSources.put(name, dataSource);
    }

    public void unregisterDataSource(String name) {
        DataSource dataSource = dataSources.remove(name);
        if (dataSource != null && dataSource.isConnected()) {
            dataSource.disconnect();
        }
    }

    public Optional<DataSource> getDataSource(String name) {
        return Optional.ofNullable(dataSources.get(name));
    }

    public List<DataSource> getAllDataSources() {
        return new ArrayList<>(dataSources.values());
    }

    public List<DataSource> getDataSourcesByType(DataSourceType type) {
        return dataSources.values().stream()
                .filter(ds -> ds.getType() == type)
                .collect(Collectors.toList());
    }

    public Map<String, Object> collectAllMetrics() throws DataSourceException {
        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.put("collection_timestamp", System.currentTimeMillis());
        allMetrics.put("total_sources", dataSources.size());

        Map<String, Object> sourceMetrics = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            String sourceName = entry.getKey();
            DataSource dataSource = entry.getValue();

            try {
                if (!dataSource.isConnected()) {
                    dataSource.connect();
                }
                Map<String, Object> metrics = dataSource.collectMetrics();
                sourceMetrics.put(sourceName, metrics);
                successCount++;
            } catch (DataSourceException e) {
                Map<String, Object> errorMetrics = new HashMap<>();
                errorMetrics.put("error", e.getMessage());
                errorMetrics.put("source_type", dataSource.getType().name());
                sourceMetrics.put(sourceName, errorMetrics);
                failureCount++;
            }
        }

        allMetrics.put("successful_collectors", successCount);
        allMetrics.put("failed_collectors", failureCount);
        allMetrics.put("sources", sourceMetrics);

        return allMetrics;
    }

    public void connectAll() throws DataSourceException {
        List<String> failedSources = new ArrayList<>();

        for (Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().connect();
            } catch (DataSourceException e) {
                failedSources.add(entry.getKey() + ": " + e.getMessage());
            }
        }

        if (!failedSources.isEmpty()) {
            throw new DataSourceException("Failed to connect to sources: " + failedSources);
        }
    }

    public void disconnectAll() {
        for (DataSource dataSource : dataSources.values()) {
            if (dataSource.isConnected()) {
                dataSource.disconnect();
            }
        }
    }

    public void updateDataSourceConfiguration(String name, Map<String, Object> config) {
        DataSource dataSource = dataSources.get(name);
        if (dataSource != null) {
            boolean wasConnected = dataSource.isConnected();
            if (wasConnected) {
                dataSource.disconnect();
            }
            dataSource.setConfiguration(config);
        }
    }
}
