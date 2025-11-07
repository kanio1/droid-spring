#!/bin/bash

# Vault Secret Initialization Script
# This script initializes HashiCorp Vault with database and application secrets

set -e

VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-dev-only-token}"

echo "=========================================="
echo "Initializing HashiCorp Vault with Secrets"
echo "=========================================="
echo ""

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
until curl -s "${VAULT_ADDR}/v1/sys/health" > /dev/null 2>&1; do
    echo "Vault is not ready yet, waiting 2 seconds..."
    sleep 2
done
echo "Vault is ready!"
echo ""

# Login to Vault
echo "Logging in to Vault..."
vault write auth/token/login token="${VAULT_TOKEN}" > /dev/null 2>&1 || {
    echo "Failed to login to Vault. Make sure VAULT_TOKEN is correct."
    exit 1
}
echo "Successfully logged in to Vault"
echo ""

# Enable KV version 2 secrets engine if not already enabled
echo "Enabling KV version 2 secrets engine..."
vault secrets enable -version=2 kv > /dev/null 2>&1 || {
    echo "KV version 2 secrets engine already enabled or failed to enable"
}
echo ""

# Write database secrets
echo "Writing database secrets to Vault..."
vault kv put secret/bss/database \
    username="${POSTGRES_USER:-bss_app}" \
    password="${POSTGRES_PASSWORD:-bss_password}" \
    url="${POSTGRES_URL:-jdbc:postgresql://postgres:5432/bss}" \
    redis-password="${REDIS_PASSWORD:-redis_password_123}" \
    jwt-secret="${JWT_SECRET:-my_super_secret_jwt_key_change_in_production}" \
    > /dev/null

if [ $? -eq 0 ]; then
    echo "✓ Database secrets written successfully"
else
    echo "✗ Failed to write database secrets"
    exit 1
fi
echo ""

# Write application secrets
echo "Writing application secrets to Vault..."
vault kv put secret/bss/app \
    keycloak-client-secret="${KEYCLOAK_CLIENT_SECRET:-client_secret_change_me}" \
    mail-password="${MAIL_PASSWORD:-mail_password}" \
    encryption-key="${ENCRYPTION_KEY:-encryption_key_32_chars_long_12345}" \
    > /dev/null

if [ $? -eq 0 ]; then
    echo "✓ Application secrets written successfully"
else
    echo "✗ Failed to write application secrets"
    exit 1
fi
echo ""

# Write TLS certificates if they exist
if [ -f "/etc/ssl/certs/backend-cert.p12" ]; then
    echo "Writing TLS certificate secrets to Vault..."
    CERT_DATA=$(base64 -w 0 /etc/ssl/certs/backend-cert.p12)
    vault kv put secret/bss/tls \
        certificate-p12="${CERT_DATA}" \
        certificate-password="${SSL_KEYSTORE_PASSWORD:-changeit}" \
        > /dev/null

    if [ $? -eq 0 ]; then
        echo "✓ TLS certificate secrets written successfully"
    else
        echo "✗ Failed to write TLS certificate secrets"
    fi
    echo ""
fi

# Verify secrets
echo "Verifying secrets in Vault..."
echo ""
echo "Database secrets:"
vault kv get -field=username secret/bss/database || echo "Could not retrieve database username"
echo ""

echo "Application secrets (first few):"
vault kv get -field=keycloak-client-secret secret/bss/app || echo "Could not retrieve application secrets"
echo ""

echo "=========================================="
echo "Vault Initialization Complete!"
echo "=========================================="
echo ""
echo "Secrets are stored at:"
echo "  - Database: secret/bss/database"
echo "  - Application: secret/bss/app"
echo "  - TLS: secret/bss/tls (if certificate exists)"
echo ""
echo "To view all secrets, use:"
echo "  vault kv list secret/bss"
echo "  vault kv get secret/bss/database"
echo ""
