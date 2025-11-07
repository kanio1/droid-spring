package com.droid.bss.domain.datasource;

/**
 * Supported data source types
 */
public enum DataSourceType {
    SNMP("Simple Network Management Protocol"),
    API_GATEWAY("REST API Gateway"),
    DATABASE("Database"),
    METRICS_ENDPOINT("Metrics HTTP Endpoint"),
    CLOUD_WATCH("AWS CloudWatch"),
    PROMETHEUS("Prometheus"),
    INFLUXDB("InfluxDB");

    private final String description;

    DataSourceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
