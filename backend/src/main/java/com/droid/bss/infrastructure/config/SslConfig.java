package com.droid.bss.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;

/**
 * SSL/TLS Configuration
 *
 * Configures SSL/TLS settings for database and service communication
 */
@Configuration
@PropertySource("classpath:application-ssl.properties")
public class SslConfig {

    @Value("${app.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${app.ssl.truststore.path:/etc/ssl/certs/truststore.jks}")
    private String truststorePath;

    @Value("${app.ssl.truststore.password:changeit}")
    private String truststorePassword;

    @Value("${app.ssl.keystore.path:}")
    private String keystorePath;

    @Value("${app.ssl.keystore.password:}")
    private String keystorePassword;

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * Get SSL properties for database connection
     */
    public java.util.Properties getDatabaseSslProperties() {
        java.util.Properties properties = new java.util.Properties();

        if (sslEnabled) {
            properties.setProperty("ssl", "true");
            properties.setProperty("sslmode", "require");
            properties.setProperty("sslrootcert", truststorePath);
            properties.setProperty("sslcert", "");
            properties.setProperty("sslkey", "");
        }

        return properties;
    }
}
