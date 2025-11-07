package com.droid.bss.infrastructure.config;

import com.droid.bss.infrastructure.secrets.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Database Vault Configuration
 *
 * Loads database credentials from HashiCorp Vault and makes them available
 * as Spring environment properties
 */
@Configuration
public class DatabaseVaultConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseVaultConfig.class);

    private static final String VAULT_DATABASE_PATH = "secret/data/bss/database";

    private final VaultSecretService vaultSecretService;

    public DatabaseVaultConfig(VaultSecretService vaultSecretService) {
        this.vaultSecretService = vaultSecretService;
    }

    @Bean
    @Primary
    public DatabaseVaultInitializer databaseVaultInitializer(ConfigurableEnvironment environment) {
        DatabaseVaultInitializer initializer = new DatabaseVaultInitializer(vaultSecretService, environment);
        initializer.loadSecretsFromVault();
        return initializer;
    }

    /**
     * Initializer class that loads secrets from Vault and registers them as Spring properties
     */
    public static class DatabaseVaultInitializer {

        private final VaultSecretService vaultSecretService;
        private final ConfigurableEnvironment environment;

        public DatabaseVaultInitializer(VaultSecretService vaultSecretService,
                                       ConfigurableEnvironment environment) {
            this.vaultSecretService = vaultSecretService;
            this.environment = environment;
        }

        public void loadSecretsFromVault() {
            try {
                log.info("Loading database secrets from Vault at path: {}", VAULT_DATABASE_PATH);

                Map<String, Object> secrets = vaultSecretService.getSecret(VAULT_DATABASE_PATH);

                if (secrets.isEmpty()) {
                    log.warn("No secrets found at path: {}. Using environment variables as fallback.",
                            VAULT_DATABASE_PATH);
                    return;
                }

                Map<String, Object> properties = new HashMap<>();

                // Map Vault secrets to Spring properties
                if (secrets.containsKey("username")) {
                    properties.put("spring.datasource.username", secrets.get("username"));
                }
                if (secrets.containsKey("password")) {
                    properties.put("spring.datasource.password", secrets.get("password"));
                }
                if (secrets.containsKey("url")) {
                    properties.put("spring.datasource.url", secrets.get("url"));
                }
                if (secrets.containsKey("redis-password")) {
                    properties.put("spring.redis.password", secrets.get("redis-password"));
                }
                if (secrets.containsKey("jwt-secret")) {
                    properties.put("security.jwt.secret", secrets.get("jwt-secret"));
                }

                // Register as a PropertySource with high priority
                MapPropertySource vaultPropertySource = new MapPropertySource("vaultDatabaseSecrets", properties);
                environment.getPropertySources().addFirst(vaultPropertySource);

                log.info("Successfully loaded {} database secrets from Vault", properties.size());

            } catch (Exception e) {
                log.error("Failed to load secrets from Vault. Using environment variables as fallback.", e);
                // Don't throw exception - allow application to start with environment variables
            }
        }
    }
}
