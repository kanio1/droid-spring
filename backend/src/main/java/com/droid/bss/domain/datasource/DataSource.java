package com.droid.bss.domain.datasource;

import java.util.Map;

/**
 * Interface for data source connectors
 */
public interface DataSource {
    String getName();
    DataSourceType getType();
    boolean isConnected();
    void connect() throws DataSourceException;
    void disconnect();
    Map<String, Object> collectMetrics() throws DataSourceException;
    Map<String, Object> getConfiguration();
    void setConfiguration(Map<String, Object> config);
}
