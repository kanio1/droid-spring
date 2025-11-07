package com.droid.bss.infrastructure.config;

import com.droid.bss.application.service.DataSourceManager;
import com.droid.bss.infrastructure.datasource.ApiGatewayDataSource;
import com.droid.bss.infrastructure.datasource.DatabaseDataSource;
import com.droid.bss.infrastructure.datasource.SnmpDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for data source initialization
 */
@Configuration
public class DataSourceConfig {

    private final DataSourceManager dataSourceManager;
    private final SnmpDataSource snmpDataSource;
    private final ApiGatewayDataSource apiGatewayDataSource;
    private final DatabaseDataSource databaseDataSource;

    public DataSourceConfig(DataSourceManager dataSourceManager,
                           SnmpDataSource snmpDataSource,
                           ApiGatewayDataSource apiGatewayDataSource,
                           DatabaseDataSource databaseDataSource) {
        this.dataSourceManager = dataSourceManager;
        this.snmpDataSource = snmpDataSource;
        this.apiGatewayDataSource = apiGatewayDataSource;
        this.databaseDataSource = databaseDataSource;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initializeDataSources() {
        initializeSnmpDataSource();
        initializeApiGatewayDataSource();
        initializeDatabaseDataSource();
    }

    @ConditionalOnProperty(name = "bss.datasource.snmp.enabled", havingValue = "true", matchIfMissing = true)
    private void initializeSnmpDataSource() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "SNMP-Switch-01");
        config.put("host", "192.168.1.1");
        config.put("port", 161);
        config.put("community", "public");

        snmpDataSource.setConfiguration(config);
        dataSourceManager.registerDataSource("SNMP-Switch-01", snmpDataSource);
    }

    @ConditionalOnProperty(name = "bss.datasource.api-gateway.enabled", havingValue = "true", matchIfMissing = true)
    private void initializeApiGatewayDataSource() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "API-Gateway-Primary");
        config.put("baseUrl", "https://api.example.com");
        config.put("apiKey", "${API_GATEWAY_KEY:}");
        config.put("timeout", 5000);

        apiGatewayDataSource.setConfiguration(config);
        dataSourceManager.registerDataSource("API-Gateway-Primary", apiGatewayDataSource);
    }

    @ConditionalOnProperty(name = "bss.datasource.database.enabled", havingValue = "true", matchIfMissing = true)
    private void initializeDatabaseDataSource() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "PostgreSQL-Main");
        config.put("databaseType", "PostgreSQL");
        config.put("jdbcUrl", "jdbc:postgresql://localhost:5432/bss");
        config.put("username", "${DB_USERNAME:bss_app}");
        config.put("password", "${DB_PASSWORD:bss_password}");

        databaseDataSource.setConfiguration(config);
        dataSourceManager.registerDataSource("PostgreSQL-Main", databaseDataSource);
    }
}
