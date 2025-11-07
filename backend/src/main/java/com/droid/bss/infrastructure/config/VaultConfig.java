package com.droid.bss.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

/**
 * Vault Configuration
 *
 * Configures HashiCorp Vault integration for secure secrets management
 */
@Configuration
public class VaultConfig extends AbstractVaultConfiguration {

    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = new VaultEndpoint();
        endpoint.setHost(System.getenv().getOrDefault("VAULT_HOST", "vault"));
        endpoint.setPort(Integer.parseInt(System.getenv().getOrDefault("VAULT_PORT", "8200")));
        endpoint.setScheme(System.getenv().getOrDefault("VAULT_SCHEME", "http"));
        return endpoint;
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        // For development, use token authentication
        // In production, use proper authentication methods (AppRole, Kubernetes, etc.)
        String token = System.getenv().getOrDefault("VAULT_TOKEN", "dev-only-token");
        return new TokenAuthentication(token);
    }
}
