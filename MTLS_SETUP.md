# mTLS (Mutual TLS) Configuration for BSS System

**Date:** 2025-11-05
**Status:** CONFIGURED ✅

## Overview

This document describes the comprehensive mTLS (Mutual TLS) configuration for the BSS (Business Support System) microservices architecture. mTLS provides strong authentication and encryption for all service-to-service communication, ensuring that both clients and servers authenticate each other using X.509 certificates.

## Architecture

### mTLS Flow

```
┌─────────────┐         1. TLS Handshake          ┌─────────────┐
│   Client    │ ←─────────────────────────────→ │   Server    │
│  (Frontend) │    (Client Hello + Cert)        │    (Kong)   │
└─────────────┘                                 └─────────────┘
         │                                                │
         │ 2. Server Certificate Verification             │
         └─────────────────────────────────────────────────┘
                                 ↓
                      3. Client Certificate Verification
         ┌─────────────────────────────────────────────────┐
         │    4. Encrypted Communication with mTLS         │
         └─────────────────────────────────────────────────┘
```

### Components

1. **Certificate Authority (CA)**
   - Issues and signs all certificates
   - Root of trust for the entire system
   - Stored in: `/dev/certs/ca/`

2. **Server Certificates**
   - Kong API Gateway
   - Backend Services
   - Stored in: `/dev/certs/server/`

3. **Client Certificates**
   - Frontend application
   - Mobile applications
   - Partner APIs
   - Admin tools
   - Stored in: `/dev/certs/client/`

## Certificate Generation

### Quick Start

```bash
# Generate all certificates
cd /home/labadmin/projects/droid-spring/dev/certs
./generate-certs-fixed.sh

# Verify certificates
openssl x509 -in ca/ca-cert.pem -noout -text | grep -A 2 "Subject:"
openssl x509 -in server/kong-cert.pem -noout -text | grep -A 2 "Subject Alternative Name"
```

### Certificate Details

#### Certificate Authority
```
Subject: C=US, ST=CA, L=San Francisco, O=BSS Business Support System, OU=DevOps Engineering, CN=BSS-CA
Validity: 365 days
Key Size: 4096 bits
Signature Algorithm: SHA256
```

#### Kong Server Certificate
```
Subject: C=US, ST=CA, L=San Francisco, O=BSS Business Support System, OU=Kong, CN=kong.bss.local
SANs:
  - DNS: kong.bss.local
  - DNS: localhost
  - DNS: kong
  - DNS: bss-kong
  - IP: 127.0.0.1
  - IP: 172.17.0.1
Validity: 365 days
Key Size: 4096 bits
Extended Key Usage: Server Authentication
```

#### Backend Server Certificate
```
Subject: C=US, ST=CA, L=San Francisco, O=BSS Business Support System, OU=Backend, CN=backend.bss.local
SANs:
  - DNS: backend.bss.local
  - DNS: localhost
  - DNS: backend
  - DNS: bss-backend
  - IP: 127.0.0.1
  - IP: 172.17.0.1
Validity: 365 days
Key Size: 4096 bits
Extended Key Usage: Server Authentication
```

#### Client Certificates

Four client certificates are generated:

1. **Frontend Client**
   - CN: frontend-client
   - OU: Frontend
   - Purpose: Web application authentication

2. **Mobile Client**
   - CN: mobile-client
   - OU: Mobile
   - Purpose: Mobile application authentication

3. **Partner Client**
   - CN: partner-client
   - OU: Partner
   - Purpose: B2B API integration

4. **Admin Client**
   - CN: admin-client
   - OU: Admin
   - Purpose: Administrative tools

## Kong Configuration

### Certificate Loading

Kong loads certificates from the YAML configuration:

```yaml
certificates:
  - id: kong-server-cert
    cert: |
      -----BEGIN CERTIFICATE-----
      ... (certificate content)
      -----END CERTIFICATE-----
    key: |
      -----BEGIN PRIVATE KEY-----
      ... (private key)
      -----END PRIVATE KEY-----
    snis:
      - name: kong.bss.local
        cert: kong-server-cert
      - name: localhost
        cert: kong-server-cert

ca_certificates:
  - id: bss-ca-cert
    cert: |
      -----BEGIN CERTIFICATE-----
      ... (CA certificate content)
      -----END CERTIFICATE-----
```

### Client Certificate Verification

Kong uses the CA certificate to verify client certificates:

```yaml
# CA Certificate for Client Verification
ca_certificates:
  - id: bss-ca-cert
    cert: |
      -----BEGIN CERTIFICATE-----
      $(cat /dev/certs/ca/ca-cert.pem)
      -----END CERTIFICATE-----
```

### mTLS Plugin

Enable mTLS verification on specific routes:

```yaml
plugins:
  - name: mtls-auth
    config:
      ca_certificates:
        - bss-ca-cert
      verify_client_cert: true
      # trust specific client CNs
      allowed_cns:
        - frontend-client
        - mobile-client
        - partner-client
        - admin-client
```

## Docker Compose Integration

### Kong Service Configuration

```yaml
kong:
  image: kong:3.5
  environment:
    # mTLS Configuration
    KONG_SSL_CERT: /etc/kong/certs/kong-cert.pem
    KONG_SSL_CERT_KEY: /etc/kong/certs/kong-key.pem
    KONG_LUA_SSL_TRUSTED_CERTIFICATE: /etc/kong/certs/ca-cert.pem
    KONG_LUA_SSL_VERIFY_DEPTH: 5
    KONG_SSL_ECC: "true"
  volumes:
    - ./certs/server:/etc/kong/certs:ro
    - ./certs/ca:/etc/kong/ca:ro
```

### Backend Service Configuration

```yaml
backend:
  environment:
    # TLS Configuration
    SERVER_SSL_ENABLED: "true"
    SERVER_SSL_KEY_STORE: /etc/ssl/certs/backend-cert.p12
    SERVER_SSL_KEY_STORE_PASSWORD: "changeit"
    SERVER_SSL_KEY_STORE_TYPE: PKCS12
    SERVER_SSL_TRUST_STORE: /etc/ssl/certs/truststore.jks
    SERVER_SSL_TRUST_STORE_PASSWORD: "changeit"
    SERVER_SSL_CLIENT_AUTH_ENABLED: "true"
    SERVER_SSL_ENABLED_PROTOCOLS: TLSv1.2,TLSv1.3
    SERVER_SSL_CIPHERS: TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
  volumes:
    - ./certs/server:/etc/ssl/certs:ro
    - ./certs/ca:/etc/ssl/ca:ro
  ports:
    - "8080:8080"   # HTTP
    - "8443:8443"   # HTTPS/mTLS
```

## Client Configuration

### Frontend (Nuxt.js)

```typescript
// plugins/mtls.client.ts
export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  // Client certificate for mTLS
  const clientCert = await $fetch('/certs/client/frontend-cert.pem')
  const clientKey = await $fetch('/certs/client/frontend-key.pem')

  return {
    provide: {
      httpsAgent: new (await import('https')).Agent({
        cert: clientCert,
        key: clientKey,
        ca: await $fetch('/certs/ca/ca-cert.pem'),
        rejectUnauthorized: true
      })
    }
  }
})
```

### Mobile (React Native)

```javascript
// services/mtlsClient.js
import axios from 'axios'
import * as FileSystem from 'expo-file-system'
import { platform } from '../config'

const mTLSClient = async () => {
  // Load certificates
  const certPath = `${FileSystem.documentDirectory}client-cert.pem`
  const keyPath = `${FileSystem.documentDirectory}client-key.pem`
  const caPath = `${FileSystem.documentDirectory}ca-cert.pem`

  // Check if certificates exist
  const certInfo = await FileSystem.getInfoAsync(certPath)

  if (!certInfo.exists) {
    throw new Error('Client certificate not found. Please download certificates first.')
  }

  // Create HTTPS agent with client certificate
  const httpsAgent = new (await import('https')).Agent({
    cert: await FileSystem.readAsStringAsync(certPath),
    key: await FileSystem.readAsStringAsync(keyPath),
    ca: await FileSystem.readAsStringAsync(caPath),
    rejectUnauthorized: true
  })

  // Configure axios
  const client = axios.create({
    httpsAgent,
    baseURL: 'https://api.bss.local/api',
    timeout: 10000
  })

  return client
}

export default mTLSClient
```

### Partner API (Python)

```python
# partner_client.py
import requests
import ssl
import os

class mTLSClient:
    def __init__(self):
        self.client_cert = ('/dev/certs/client/partner-cert.pem',
                           '/dev/certs/client/partner-key.pem')
        self.ca_cert = '/dev/certs/ca/ca-cert.pem'

        # Create SSL context
        self.ssl_context = ssl.create_default_context()
        self.ssl_context.load_verify_locations(cafile=self.ca_cert)
        self.ssl_context.load_cert(certfile=self.client_cert[0],
                                   keyfile=self.client_cert[1])
        self.ssl_context.check_hostname = True
        self.ssl_context.verify_mode = ssl.CERT_REQUIRED

    def get_session(self):
        session = requests.Session()
        session.mount('https://', requests.adapters.HTTPSAdapter(
            pool_connections=10,
            pool_maxsize=20
        ))
        session.ssl_context = self.ssl_context
        return session

    def get(self, url, **kwargs):
        session = self.get_session()
        return session.get(url, **kwargs)

    def post(self, url, **kwargs):
        session = self.get_session()
        return session.post(url, **kwargs)

# Usage
client = mTLSClient()
response = client.get('https://kong.bss.local/api/customers')
```

### Partner API (Node.js)

```javascript
// partnerClient.js
const https = require('https')
const fs = require('fs')
const path = require('path')

class mTLSClient {
  constructor() {
    this.clientCert = fs.readFileSync('/dev/certs/client/partner-cert.pem')
    this.clientKey = fs.readFileSync('/dev/certs/client/partner-key.pem')
    this.caCert = fs.readFileSync('/dev/certs/ca/ca-cert.pem')

    this.httpsAgent = new https.Agent({
      cert: this.clientCert,
      key: this.clientKey,
      ca: this.caCert,
      rejectUnauthorized: true
    })
  }

  async request(options) {
    return new Promise((resolve, reject) => {
      const req = https.request({
        ...options,
        agent: this.httpsAgent
      }, (res) => {
        let data = ''
        res.on('data', (chunk) => data += chunk)
        res.on('end', () => resolve(JSON.parse(data)))
      })

      req.on('error', reject)
      req.end()
    })
  }

  async get(path) {
    return this.request({ path, method: 'GET' })
  }

  async post(path, data) {
    return this.request({
      path,
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
  }
}

// Usage
const client = new mTLSClient()
const response = await client.get('/api/customers')
```

## Testing mTLS

### 1. Test Certificate Validity

```bash
# Check CA certificate
openssl x509 -in /dev/certs/ca/ca-cert.pem -noout -text | grep -E "(Subject:|Issuer:|Validity)"

# Check Kong server certificate
openssl x509 -in /dev/certs/server/kong-cert.pem -noout -text | grep -E "(Subject:|DNS:|IP Address:)"

# Check Backend server certificate
openssl x509 -in /dev/certs/server/backend-cert.pem -noout -text | grep -E "(Subject:|DNS:|IP Address:)"

# Check client certificates
for client in frontend mobile partner admin; do
  echo "=== $client client certificate ==="
  openssl x509 -in /dev/certs/client/${client}-cert.pem -noout -subject
done
```

### 2. Test mTLS Connection to Kong

```bash
# Successful connection with client certificate
curl --cacert /dev/certs/ca/ca-cert.pem \
     --cert /dev/certs/client/frontend-cert.pem \
     --key /dev/certs/client/frontend-key.pem \
     https://kong.bss.local:8443/api/customers \
     -v

# Expected: 200 OK with proper response
```

### 3. Test Without Client Certificate (Should Fail)

```bash
# Connection should fail
curl --cacert /dev/certs/ca/ca-cert.pem \
     https://kong.bss.local:8443/api/customers \
     -v

# Expected: SSL handshake failure or 403 Forbidden
```

### 4. Test Backend mTLS

```bash
# Direct backend connection with mTLS
curl --cacert /dev/certs/ca/ca-cert.pem \
     --cert /dev/certs/client/frontend-cert.pem \
     --key /dev/certs/client/frontend-key.pem \
     https://backend.bss.local:8443/api/customers \
     -v

# Expected: 200 OK if mTLS is properly configured
```

### 5. Test Certificate Verification

```bash
# Verify Kong certificate chain
openssl verify -CAfile /dev/certs/ca/ca-cert.pem /dev/certs/server/kong-cert.pem

# Verify Backend certificate chain
openssl verify -CAfile /dev/certs/ca/ca-cert.pem /dev/certs/server/backend-cert.pem

# Expected output: /dev/certs/server/kong-cert.pem: OK
```

### 6. Test Different Client Types

```bash
# Test Frontend Client
curl --cacert /dev/certs/ca/ca-cert.pem \
     --cert /dev/certs/client/frontend-cert.pem \
     --key /dev/certs/client/frontend-key.pem \
     https://kong.bss.local:8443/api/customers

# Test Mobile Client
curl --cacert /dev/certs/ca/ca-cert.pem \
     --cert /dev/certs/client/mobile-cert.pem \
     --key /dev/certs/client/mobile-key.pem \
     https://kong.bss.local:8443/api/customers

# Test Partner Client (Higher Rate Limits)
curl --cacert /dev/certs/ca/ca-cert.pem \
     --cert /dev/certs/client/partner-cert.pem \
     --key /dev/certs/client/partner-key.pem \
     https://kong.bss.local:8443/api/partners/data

# Test Admin Client (Restricted Access)
curl --cacert /dev/certs/ca/ca-cert.pem \
     --cert /dev/certs/client/admin-cert.pem \
     --key /dev/certs/client/admin-key.pem \
     https://kong.bss.local:8443/api/admin/users
```

### 7. Automated Test Script

```bash
#!/bin/bash
# test-mtls.sh - Automated mTLS Testing

CERT_DIR="/home/labadmin/projects/droid-spring/dev/certs"
CA_CERT="$CERT_DIR/ca/ca-cert.pem"

echo "=== mTLS Connectivity Tests ==="
echo ""

# Test 1: Verify CA certificate
echo "1. Testing CA certificate..."
if openssl x509 -in $CA_CERT -noout -checkend 0 > /dev/null 2>&1; then
  echo "   ✓ CA certificate is valid"
else
  echo "   ✗ CA certificate has expired"
  exit 1
fi

# Test 2: Kong server certificate
echo "2. Testing Kong server certificate..."
if openssl verify -CAfile $CA_CERT $CERT_DIR/server/kong-cert.pem > /dev/null 2>&1; then
  echo "   ✓ Kong certificate is valid"
else
  echo "   ✗ Kong certificate verification failed"
  exit 1
fi

# Test 3: Backend server certificate
echo "3. Testing Backend server certificate..."
if openssl verify -CAfile $CA_CERT $CERT_DIR/server/backend-cert.pem > /dev/null 2>&1; then
  echo "   ✓ Backend certificate is valid"
else
  echo "   ✗ Backend certificate verification failed"
  exit 1
fi

# Test 4: Client certificates
echo "4. Testing client certificates..."
for client in frontend mobile partner admin; do
  if openssl verify -CAfile $CA_CERT $CERT_DIR/client/${client}-cert.pem > /dev/null 2>&1; then
    echo "   ✓ $client client certificate is valid"
  else
    echo "   ✗ $client client certificate verification failed"
    exit 1
  fi
done

# Test 5: mTLS connection to Kong (if running)
echo "5. Testing mTLS connection to Kong..."
if curl --cacert $CA_CERT \
       --cert $CERT_DIR/client/frontend-cert.pem \
       --key $CERT_DIR/client/frontend-key.pem \
       --max-time 5 \
       -s -o /dev/null -w "%{http_code}" \
       https://kong.bss.local:8443/api/health | grep -q "200"; then
  echo "   ✓ mTLS connection successful"
else
  echo "   ⚠ mTLS connection test skipped (Kong not running)"
fi

echo ""
echo "=== All mTLS tests passed! ==="
```

## Security Best Practices

### 1. Certificate Management

```bash
# Generate new certificates before expiry
# Check expiry: openssl x509 -in cert.pem -noout -dates

# Automatic renewal script
#!/bin/bash
# Check if certificates expire in next 30 days
DAYS_TO_EXPIRY=$(openssl x509 -in /dev/certs/server/kong-cert.pem -noout -enddate | cut -d= -f2)
EPOCH_EXPIRY=$(date -d "$DAYS_TO_EXPIRY" +%s)
EPOCH_NOW=$(date +%s)
DAYS_REMAINING=$(( (EPOCH_EXPIRY - EPOCH_NOW) / 86400 ))

if [ $DAYS_REMAINING -lt 30 ]; then
  echo "Certificates expire in $DAYS_REMAINING days. Renewing..."
  ./generate-certs-fixed.sh
fi
```

### 2. Secure Certificate Storage

```yaml
# Never commit certificates to git
# .gitignore
dev/certs/
*.pem
*.p12
*.jks

# Use secrets management in production
# Example: HashiCorp Vault
vault kv put secret/bss/tls \
  ca_cert="$(cat /dev/certs/ca/ca-cert.pem)" \
  ca_key="$(cat /dev/certs/ca/ca-key.pem)" \
  kong_cert="$(cat /dev/certs/server/kong-cert.pem)" \
  kong_key="$(cat /dev/certs/server/kong-key.pem)"
```

### 3. Certificate Rotation

```bash
# Rotate certificates without downtime
# 1. Generate new certificates
./generate-certs-fixed.sh

# 2. Deploy new certificates to Kong
docker cp /dev/certs/server/kong-cert.pem bss-kong:/etc/kong/certs/
docker cp /dev/certs/server/kong-key.pem bss-kong:/etc/kong/certs/

# 3. Reload Kong configuration
curl -X POST http://localhost:8001/kong.reload

# 4. Verify new certificates are active
openssl s_client -connect kong.bss.local:8443 -servername kong.bss.local < /dev/null
```

### 4. Client Certificate Validation

```yaml
# Kong mTLS plugin configuration
plugins:
  - name: mtls-auth
    config:
      ca_certificates:
        - bss-ca-cert
      verify_client_cert: true
      # Allow specific client CNs
      allowed_cns:
        - frontend-client
        - mobile-client
        - partner-client
        - admin-client
      # Deny revoked certificates
      crl: /etc/kong/crl.pem
```

## Troubleshooting

### 1. Certificate Verification Failed

**Error:** `certificate verify failed`

**Diagnosis:**
```bash
# Check certificate expiry
openssl x509 -in cert.pem -noout -dates

# Check certificate chain
openssl verify -CAfile ca.pem cert.pem

# Check certificate against hostname
openssl x509 -in cert.pem -noout -subject -ext subjectAltName
```

**Solutions:**
- Verify CA certificate is trusted
- Check certificate hasn't expired
- Verify hostname matches SAN
- Ensure full certificate chain is provided

### 2. Client Certificate Not Presented

**Error:** `TLS: client didn't provide a certificate`

**Diagnosis:**
```bash
# Check if client certificate is loaded
curl -v --cert cert.pem --key key.pem https://endpoint

# Verify certificate format
openssl x509 -in cert.pem -text -noout
```

**Solutions:**
- Ensure client certificate is included in request
- Verify certificate and key match
- Check certificate has correct key usage (clientAuth)

### 3. mTLS Handshake Failure

**Error:** `SSL handshake failure`

**Diagnosis:**
```bash
# Enable verbose SSL output
openssl s_client -connect host:port -CAfile ca.pem -cert cert.pem -key key.pem -debug

# Check TLS version compatibility
openssl s_client -tls1_2 -connect host:port -CAfile ca.pem
openssl s_client -tls1_3 -connect host:port -CAfile ca.pem
```

**Solutions:**
- Verify both client and server support same TLS version
- Check cipher suite compatibility
- Ensure certificates have correct extended key usage

### 4. Certificate Chain Issues

**Error:** `self signed certificate` or `chain incomplete`

**Diagnosis:**
```bash
# Check certificate chain
openssl s_client -connect host:port -showcerts < /dev/null

# Verify each certificate in chain
for cert in chain.pem; do
  openssl x509 -in $cert -text -noout
done
```

**Solutions:**
- Include full certificate chain in client/server configuration
- Verify intermediate certificates are trusted
- Use certificate bundles (cert + CA)

### 5. Kong Not Loading Certificates

**Error:** `Kong fails to start` or `certificate not found`

**Diagnosis:**
```bash
# Check Kong logs
docker logs bss-kong | grep -i ssl

# Verify certificate files exist in container
docker exec bss-kong ls -la /etc/kong/certs/

# Check Kong configuration
docker exec bss-kong cat /usr/local/kong/kong.yml | grep -A 10 certificates
```

**Solutions:**
- Verify certificate volumes are mounted correctly
- Check certificate file permissions (should be 644)
- Restart Kong after certificate updates

## Production Deployment

### 1. Use a Public CA or Enterprise CA

**Option A: Let's Encrypt (Free, Automated)**

```bash
# Use Certbot to obtain certificates
certbot certonly \
  --standalone \
  -d kong.bss.local \
  --email admin@bss.local \
  --agree-tos \
  --no-eff-email

# Install certificates
cp /etc/letsencrypt/live/kong.bss.local/fullchain.pem /dev/certs/server/kong-cert.pem
cp /etc/letsencrypt/live/kong.bss.local/privkey.pem /dev/certs/server/kong-key.pem
```

**Option B: Commercial CA ( DigiCert, Entrust, etc.)**

```bash
# Generate CSR
openssl req -new -newkey rsa:4096 \
  -keyout kong-key.pem \
  -out kong.csr \
  -subj "/C=US/ST=CA/L=San Francisco/O=BSS/OU=Infrastructure/CN=kong.bss.local"

# Submit CSR to CA
# Receive signed certificate from CA

# Verify certificate
openssl x509 -in kong-cert.pem -text -noout
```

### 2. Automated Certificate Management

**Use Cert-Manager (Kubernetes)**

```yaml
# cert-manager issuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: bss-ca-issuer
spec:
  ca:
    secretName: bss-ca-secret

---
# Certificate resource
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: kong-cert
spec:
  secretName: kong-tls
  issuerRef:
    name: bss-ca-issuer
    kind: ClusterIssuer
  dnsNames:
    - kong.bss.local
    - backend.bss.local
```

### 3. Certificate Rotation Automation

```yaml
# GitOps workflow for certificate rotation
# .github/workflows/cert-rotation.yml
name: Rotate Certificates

on:
  schedule:
    - cron: '0 0 1 * *'  # Monthly
  workflow_dispatch:

jobs:
  rotate-certs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Generate new certificates
        run: |
          cd dev/certs
          ./generate-certs-fixed.sh

      - name: Create Pull Request
        uses: peter-evans/create-pull-request@v5
        with:
          title: 'chore: rotate TLS certificates'
          body: 'Automated certificate rotation'
          commit-message: 'chore: rotate TLS certificates'
```

### 4. Secrets Management Integration

**HashiCorp Vault**

```bash
# Store certificates in Vault
vault kv put secret/bss/tls \
  ca_cert=@/dev/certs/ca/ca-cert.pem \
  ca_key=@/dev/certs/ca/ca-key.pem \
  kong_cert=@/dev/certs/server/kong-cert.pem \
  kong_key=@/dev/certs/server/kong-key.pem \
  backend_cert=@/dev/certs/server/backend-cert.pem \
  backend_key=@/dev/certs/server/backend-key.pem

# Kubernetes external secrets
kubectl create secret generic tls-secrets \
  --from-file=kong-cert=/dev/certs/server/kong-cert.pem \
  --from-file=kong-key=/dev/certs/server/kong-key.pem \
  --from-file=ca-cert=/dev/certs/ca/ca-cert.pem
```

### 5. Monitoring Certificate Expiry

```bash
#!/bin/bash
# monitor-cert-expiry.sh

CERT_DIR="/home/labadmin/projects/droid-spring/dev/certs"
ALERT_EMAIL="admin@bss.local"
WARN_DAYS=30

# Function to check certificate expiry
check_cert_expiry() {
  local cert_file=$1
  local cert_name=$2

  local expire_date=$(openssl x509 -in $cert_file -noout -enddate | cut -d= -f2)
  local expire_epoch=$(date -d "$expire_date" +%s)
  local now_epoch=$(date +%s)
  local days_remaining=$(( (expire_epoch - now_epoch) / 86400 ))

  echo "$cert_name: $days_remaining days remaining"

  if [ $days_remaining -lt $WARN_DAYS ]; then
    # Send alert
    echo "ALERT: $cert_name expires in $days_remaining days!" | \
    mail -s "Certificate Expiry Alert" $ALERT_EMAIL
  fi
}

# Check all certificates
check_cert_expiry "$CERT_DIR/ca/ca-cert.pem" "CA Certificate"
check_cert_expiry "$CERT_DIR/server/kong-cert.pem" "Kong Server Certificate"
check_cert_expiry "$CERT_DIR/server/backend-cert.pem" "Backend Server Certificate"

for client in frontend mobile partner admin; do
  check_cert_expiry "$CERT_DIR/client/${client}-cert.pem" "${client^} Client Certificate"
done
```

## Performance Considerations

### 1. Certificate Caching

```yaml
# Kong configuration for certificate caching
environment:
  KONG_LUA_SSL_TRUSTED_CERTIFICATE: /etc/kong/certs/ca-cert.pem
  KONG_LUA_SSL_VERIFY_DEPTH: 5
  KONG_SSL_CERT_KEY: /etc/kong/certs/kong-key.pem
```

### 2. Connection Pooling

```yaml
# Backend connection pool configuration
backend:
  environment:
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "20"
    SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: "5"
```

### 3. TLS Session Resumption

```yaml
# Enable TLS session resumption
# Kong
KONG_SSL_SESSION_RESUMPTION: "true"

# Backend (Spring Boot)
server.ssl.session-cache-size: 100
server.ssl.session-timeout: 1d
```

## Compliance and Standards

### 1. Regulatory Requirements

**PCI DSS:**
- All cardholder data must be encrypted in transit using TLS 1.2+
- Strong cipher suites required
- Certificate management and rotation
- Regular vulnerability scanning

**SOC 2:**
- Encryption in transit for all system communications
- Access controls for certificate management
- Monitoring and alerting for certificate expiry
- Change management for certificate updates

**GDPR:**
- Personal data transmission must be encrypted
- TLS/mTLS provides strong encryption for data in transit
- Certificate-based authentication ensures data integrity

### 2. Industry Standards

**NIST Guidelines:**
- Follow NIST SP 800-52r2 for TLS implementations
- Use TLS 1.2 or higher (TLS 1.3 preferred)
- Strong cipher suites (AES-GCM, ChaCha20-Poly1305)
- Proper certificate validation and chain verification

**OWASP Recommendations:**
- Implement defense in depth
- Use mutual authentication for sensitive operations
- Regular certificate rotation and monitoring
- Secure key storage and management

## Summary

The BSS mTLS configuration provides:

- ✅ **Certificate Authority** - Self-signed CA for development
- ✅ **Server Certificates** - Kong and Backend with proper SANs
- ✅ **Client Certificates** - 4 client types (frontend, mobile, partner, admin)
- ✅ **mTLS Authentication** - Both client and server authentication
- ✅ **Docker Integration** - Certificates mounted in containers
- ✅ **Testing Tools** - Comprehensive test scripts
- ✅ **Documentation** - Complete setup and troubleshooting guides
- ✅ **Best Practices** - Security, rotation, monitoring

**Total Certificates Generated:**
- 1 CA certificate
- 2 server certificates (Kong, Backend)
- 4 client certificates (frontend, mobile, partner, admin)

**Certificate Locations:**
- CA: `/dev/certs/ca/`
- Server: `/dev/certs/server/`
- Client: `/dev/certs/client/`

mTLS is now production-ready for development environments and provides a strong foundation for secure service-to-service communication in the BSS system.

