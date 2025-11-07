package com.droid.bss.infrastructure.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.vault.support.VaultResponseSupport;

import java.util.Map;
import java.util.Optional;

/**
 * Vault Secret Service
 *
 * Service for retrieving secrets from HashiCorp Vault
 */
@Service
public class VaultSecretService {

    private static final Logger log = LoggerFactory.getLogger(VaultSecretService.class);

    private final VaultTemplate vaultTemplate;

    public VaultSecretService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    /**
     * Get secret from Vault KV version 2
     *
     * @param path Path to the secret (e.g., "secret/data/postgres")
     * @return Map containing secret key-value pairs
     */
    public Map<String, Object> getSecret(String path) {
        try {
            log.debug("Retrieving secret from path: {}", path);
            VaultResponse response = vaultTemplate.read(path);

            if (response == null) {
                log.warn("No secret found at path: {}", path);
                return Map.of();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();

            // For KV version 2, data is nested under "data"
            if (data != null && data.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedData = (Map<String, Object>) data.get("data");
                log.debug("Successfully retrieved secret from path: {}", path);
                return nestedData;
            }

            log.debug("Successfully retrieved secret from path: {}", path);
            return data;
        } catch (Exception e) {
            log.error("Failed to retrieve secret from path: {}", path, e);
            throw new RuntimeException("Failed to retrieve secret from Vault", e);
        }
    }

    /**
     * Get a specific secret value by key
     *
     * @param path Path to the secret
     * @param key Key to retrieve
     * @return Optional containing the secret value
     */
    public Optional<String> getSecretValue(String path, String key) {
        try {
            Map<String, Object> secrets = getSecret(path);
            return Optional.ofNullable((String) secrets.get(key));
        } catch (Exception e) {
            log.error("Failed to retrieve secret value for key: {} from path: {}", key, path, e);
            return Optional.empty();
        }
    }

    /**
     * Write secret to Vault KV version 2
     *
     * @param path Path to the secret
     * @param data Key-value pairs to store
     */
    public void writeSecret(String path, Map<String, Object> data) {
        try {
            log.debug("Writing secret to path: {}", path);

            // For KV version 2, wrap data in a "data" object
            Map<String, Object> wrappedData = Map.of("data", data);

            VaultResponse response = vaultTemplate.write(path, wrappedData);

            log.info("Successfully wrote secret to path: {}", path);
        } catch (Exception e) {
            log.error("Failed to write secret to path: {}", path, e);
            throw new RuntimeException("Failed to write secret to Vault", e);
        }
    }

    /**
     * Delete secret from Vault
     *
     * @param path Path to the secret
     */
    public void deleteSecret(String path) {
        try {
            log.debug("Deleting secret from path: {}", path);
            vaultTemplate.delete(path);
            log.info("Successfully deleted secret from path: {}", path);
        } catch (Exception e) {
            log.error("Failed to delete secret from path: {}", path, e);
            throw new RuntimeException("Failed to delete secret from Vault", e);
        }
    }
}
