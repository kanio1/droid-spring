package com.droid.bss.infrastructure.database.transaction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

/**
 * Spring configuration for transaction management.
 *
 * @since 1.0
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(TransactionProperties.class)
@ConditionalOnProperty(name = "app.tx.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionConfig {

    private final TransactionProperties transactionProperties;

    public TransactionConfig(TransactionProperties transactionProperties) {
        this.transactionProperties = transactionProperties;
    }

    /**
     * Creates the transaction manager.
     *
     * @param dataSource the data source
     * @return the transaction manager
     */
    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
        if (transactionProperties.getJtaEnabled()) {
            // Use JTA transaction manager
            JtaTransactionManager jtaManager = new JtaTransactionManager();
            return jtaManager;
        } else {
            // Use Spring transaction manager
            org.springframework.jdbc.datasource.DataSourceTransactionManager manager =
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
            manager.setDefaultTimeout(transactionProperties.getDefaultTimeout());
            return manager;
        }
    }

    /**
     * Creates the application transaction manager wrapper.
     *
     * @param platformTransactionManager the Spring transaction manager
     * @param dataSource the data source
     * @return the transaction manager wrapper
     */
    @Bean
    @ConditionalOnMissingBean
    public com.droid.bss.infrastructure.database.transaction.TransactionManager applicationTransactionManager(
            PlatformTransactionManager platformTransactionManager,
            DataSource dataSource) {
        return new SpringTransactionManager(platformTransactionManager, dataSource);
    }

    /**
     * Creates a transaction scope for managing transaction lifecycle.
     *
     * @param transactionManager the transaction manager
     * @return the transaction scope
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionScope transactionScope(
            com.droid.bss.infrastructure.database.transaction.TransactionManager transactionManager) {
        return new TransactionScope(transactionManager);
    }
}
