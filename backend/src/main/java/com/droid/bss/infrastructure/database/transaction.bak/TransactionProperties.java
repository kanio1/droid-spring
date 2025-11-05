package com.droid.bss.infrastructure.database.transaction;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for transaction management.
 *
 * @since 1.0
 */
@Validated
@ConfigurationProperties(prefix = "app.tx")
public class TransactionProperties {

    /**
     * Whether transaction management is enabled.
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * The default transaction timeout in seconds.
     */
    @Min(1)
    @Max(3600)
    private Integer defaultTimeout = 300; // 5 minutes

    /**
     * Whether to enable nested transactions.
     */
    @NotNull
    private Boolean nestedTransactionsEnabled = false;

    /**
     * Whether to enable transaction statistics.
     */
    @NotNull
    private Boolean statisticsEnabled = true;

    /**
     * Whether to enable JTA (Java Transaction API).
     */
    @NotNull
    private Boolean jtaEnabled = false;

    /**
     * Whether to enable transaction logging.
     */
    @NotNull
    private Boolean loggingEnabled = true;

    /**
     * The log level for transaction operations.
     */
    @NotNull
    private String logLevel = "DEBUG";

    /**
     * Whether to enable transaction health checks.
     */
    @NotNull
    private Boolean healthCheckEnabled = true;

    /**
     * The interval for transaction health checks in seconds.
     */
    @Min(10)
    @Max(3600)
    private Integer healthCheckInterval = 60;

    /**
     * Whether to validate transactions on commit.
     */
    @NotNull
    private Boolean validateOnCommit = true;

    /**
     * The maximum number of concurrent transactions.
     */
    @Min(1)
    @Max(10000)
    private Integer maxConcurrentTransactions = 1000;

    /**
     * Whether to enable transaction reaping (cleanup of stale transactions).
     */
    @NotNull
    private Boolean reapingEnabled = true;

    /**
     * The interval for transaction reaping in seconds.
     */
    @Min(10)
    @Max(3600)
    private Integer reapingInterval = 120;

    public TransactionProperties() {}

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Integer defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public Boolean getNestedTransactionsEnabled() {
        return nestedTransactionsEnabled;
    }

    public void setNestedTransactionsEnabled(Boolean nestedTransactionsEnabled) {
        this.nestedTransactionsEnabled = nestedTransactionsEnabled;
    }

    public Boolean getStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(Boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public Boolean getJtaEnabled() {
        return jtaEnabled;
    }

    public void setJtaEnabled(Boolean jtaEnabled) {
        this.jtaEnabled = jtaEnabled;
    }

    public Boolean getLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(Boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public Boolean getHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(Boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public Boolean getValidateOnCommit() {
        return validateOnCommit;
    }

    public void setValidateOnCommit(Boolean validateOnCommit) {
        this.validateOnCommit = validateOnCommit;
    }

    public Integer getMaxConcurrentTransactions() {
        return maxConcurrentTransactions;
    }

    public void setMaxConcurrentTransactions(Integer maxConcurrentTransactions) {
        this.maxConcurrentTransactions = maxConcurrentTransactions;
    }

    public Boolean getReapingEnabled() {
        return reapingEnabled;
    }

    public void setReapingEnabled(Boolean reapingEnabled) {
        this.reapingEnabled = reapingEnabled;
    }

    public Integer getReapingInterval() {
        return reapingInterval;
    }

    public void setReapingInterval(Integer reapingInterval) {
        this.reapingInterval = reapingInterval;
    }
}
