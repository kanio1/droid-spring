# mTLS Configuration for BSS System

This document describes the mutual TLS (mTLS) setup for the BSS system.

## Overview

The BSS system implements mTLS using Envoy Proxy as the service mesh. mTLS provides:
- **Authentication**: Both client and server authenticate each other
- **Encryption**: All service-to-service communication is encrypted
- **Authorization**: Certificate-based access control
- **Zero Trust**: No implicit trust between services

## Current Implementation

### Envoy Proxy

Envoy is configured as the central mTLS proxy with:

1. **Upstream TLS** (lines 126-130 in envoy.yaml):
```yaml
transport_socket:
  name: envoy.transport_sockets.tls
  typed_config:
    "@type": type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext
    sni: backend.bss.local
```

2. **Traffic Management**:
   - Rate limiting: 100 requests/minute
   - Circuit breakers: 100 max connections
   - Health checks: /actuator/health endpoint
   - CORS enabled for web clients

3. **Service Routing**:
   - Backend API: `/api/*` → bss_backend_cluster
   - Database: postgres:5432 → postgres_cluster
   - Cache: redis:6379 → redis_cluster

## Kubernetes Migration Path

### 1. Istio Service Mesh

When migrating to Kubernetes, Envoy will be replaced by **Istio**:

**Migration Steps**:
```bash
# Install Istio
istioctl install --set values.defaultRevision=default

# Enable automatic sidecar injection
kubectl label namespace bss istio-injection=enabled

# Deploy applications
kubectl apply -f k8s/helm/bss-backend/
kubectl apply -f k8s/helm/bss-frontend/
```

**Istio mTLS Benefits**:
- Automatic mTLS between services
- Peer authentication policies
- Destination rules for TLS configuration
- Request authentication with JWT
- Mutual certificate rotation

### 2. Certificate Management

**Current** (Docker Compose):
- Self-signed certificates
- Manual certificate distribution
- SNI configuration in Envoy

**Future** (Kubernetes + Istio):
- **cert-manager**: Automatic certificate provisioning
- **SPIRE/SPIFFE**: Identity-based networking
- **Vault PKI**: Dynamic certificate issuance

Example Istio PeerAuthentication:
```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: bss
spec:
  mtls:
    mode: STRICT
```

### 3. Service Mesh Policies

**AuthorizationPolicy** (Istio):
```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: backend-authz
  namespace: bss
spec:
  selector:
    matchLabels:
      app: bss-backend
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/bss/sa/frontend"]
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
```

## Current Services mTLS Status

| Service | TLS Enabled | Notes |
|---------|------------|-------|
| **Backend** | ✅ Yes | Envoy upstream TLS configured |
| **Frontend** | ⚠️ Planned | Will use Istio mTLS |
| **PostgreSQL** | ⚠️ Planned | Enable SSL mode |
| **Redis** | ⚠️ Planned | Enable TLS in redis.conf |
| **Kafka** | ⚠️ Planned | Configure SSL listeners |
| **Envoy Proxy** | ✅ Yes | mTLS termination point |
| **Kong Gateway** | ✅ Yes | HTTPS on port 8443 |

## Configuration Details

### Backend (Spring Boot)

Enable TLS in `application.yaml`:
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:truststore.p12
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD}
    client-auth: need
```

### Database (PostgreSQL)

Enable SSL in PostgreSQL:
```bash
# postgresql.conf
ssl = on
ssl_cert_file = 'server.crt'
ssl_key_file = 'server.key'
ssl_ca_file = 'ca.crt'
```

Update `pg_hba.conf`:
```
hostssl all all 0.0.0.0/0 cert
```

### Redis

Enable TLS in `redis.conf`:
```bash
port 0
tls-port 6380

tls-cert-file /etc/ssl/redis/redis.crt
tls-key-file /etc/ssl/redis/redis.key
tls-ca-cert-file /etc/ssl/redis/ca.crt
```

## Testing mTLS

### 1. Test Backend TLS

```bash
# Check TLS configuration
curl -k https://localhost:8080/actuator/health

# Test mTLS with client certificate
curl --cert client.crt --key client.key \
     --cacert ca.crt https://backend:8080/api/customers
```

### 2. Verify mTLS in Envoy

```bash
# Access Envoy admin interface
curl http://localhost:15000/certs

# Check cluster stats
curl http://localhost:15000/clusters
```

### 3. Kubernetes mTLS Testing

```bash
# Check if mTLS is enabled
istioctl authn tls-check bss-backend-<pod>

# View mTLS status
kubectl exec <frontend-pod> -c frontend -- curl -v http://bss-backend:8080/actuator/health
```

## Security Best Practices

### 1. Certificate Rotation
- Short-lived certificates (24 hours)
- Automatic rotation via cert-manager
- No manual intervention required

### 2. Certificate Validation
- Validate certificate chain
- Check certificate expiration
- Revoke compromised certificates

### 3. Access Control
- Certificate-based authentication
- Service identity (SPIFFE IDs)
- Principle of least privilege

### 4. Monitoring
- Track TLS handshake failures
- Monitor certificate expiration
- Alert on unauthorized access attempts

## Production Deployment

### Prerequisites

1. **CA Certificate**: Obtain from trusted CA or use Vault PKI
2. **Certificates**: Generate for each service
3. **Private Keys**: Store securely (Vault, Kubernetes Secrets)

### Steps

1. **Generate Certificates**:
```bash
# Using OpenSSL
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365

# Using Vault
vault write -field=certificate pki/issue/backend common_name=backend.bss.local
```

2. **Configure Services**:
   - Update all service configurations
   - Restart services with new certificates
   - Verify mTLS connectivity

3. **Validate**:
   - Run security scans
   - Test all service communication
   - Check monitoring dashboards

## Tools and References

### Certificate Management
- **cert-manager**: Kubernetes certificate management
- **HashiCorp Vault**: PKI and secret management
- **SPIRE**: SPIFFE identity management

### Service Mesh
- **Istio**: Feature-complete service mesh
- **Linkerd**: Lightweight alternative
- **Consul Connect**: HashiCorp solution

### Monitoring
- **Envoy Admin API**: Envoy statistics
- **Istio Telemetry**: mTLS metrics
- **Grafana Dashboards**: TLS/SSL monitoring

## Troubleshooting

### Certificate Issues

```bash
# Check certificate validity
openssl x509 -in cert.pem -text -noout

# Test TLS connection
openssl s_client -connect backend:8080 -cert cert.pem -key key.pem

# Verify certificate chain
openssl verify -CAfile ca.pem cert.pem
```

### Connection Issues

1. **Check firewall rules**:
   - Ensure port 8443 (Kong HTTPS) is open
   - Verify Envoy proxy ports (15006)

2. **Verify certificates**:
   - Check expiration dates
   - Validate certificate chain
   - Confirm SAN (Subject Alternative Names)

3. **Review logs**:
   - Backend logs: `docker logs bss-backend`
   - Envoy logs: `docker logs bss-envoy`
   - Kong logs: `docker logs bss-kong`

## Next Steps

1. ✅ Enable mTLS for all inter-service communication
2. ✅ Deploy cert-manager for certificate automation
3. ✅ Configure SPIFFE/SPIRE for service identity
4. ✅ Implement mTLS policies in Istio
5. ✅ Add mTLS monitoring and alerting
6. ✅ Regular security audits and certificate rotation

## Conclusion

mTLS provides a robust security foundation for the BSS system. The current Docker Compose implementation with Envoy Proxy prepares the system for seamless migration to Kubernetes with Istio service mesh, enabling zero-trust networking across all services.
