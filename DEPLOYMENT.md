# BSS Production Deployment Guide

## Overview

This guide covers production deployment of the BSS (Business Support System) on Kubernetes or using Docker Compose.

## Architecture

- **Backend**: Spring Boot 3.4 with Java 21
- **Frontend**: Nuxt 3 with TypeScript
- **Database**: PostgreSQL 18 with read replica
- **Cache**: Redis 7
- **Identity**: Keycloak 26
- **Monitoring**: Prometheus, Grafana, AlertManager

## Prerequisites

### For Kubernetes Deployment

1. **Kubernetes cluster** (v1.28+)
2. **kubectl** configured
3. **Docker** for building images
4. **Ingress controller** (Traefik recommended)
5. **cert-manager** for TLS certificates
6. **Storage class** for persistent volumes

### For Docker Compose Deployment

1. **Docker** and **Docker Compose** v2
2. **At least 16GB RAM** allocated
3. **100GB+ disk space**

## Quick Start

### Option 1: Kubernetes Deployment

```bash
# 1. Clone repository
git clone <repository>
cd droid-spring

# 2. Configure secrets
cp k8s/secrets/secrets-template.yaml k8s/secrets/bss-secrets.yaml
# Edit secrets with your values

# 3. Make deploy script executable
chmod +x deploy/deploy.sh

# 4. Run deployment
./deploy/deploy.sh full

# 5. Check deployment
kubectl get pods -n bss
kubectl get ingress -n bss
```

### Option 2: Docker Compose Deployment

```bash
# 1. Configure environment
cp .env.example .env.production
# Edit .env.production with your values

# 2. Start services
docker compose -f production-compose.yml --env-file .env.production up -d

# 3. Check status
docker compose -f production-compose.yml ps

# 4. View logs
docker compose -f production-compose.yml logs -f backend
```

## Configuration

### Environment Variables

Create `.env.production` with the following variables:

```bash
# Database
POSTGRES_DB=bss
POSTGRES_USER=bss_app
POSTGRES_PASSWORD=<generate_strong_password>
POSTGRES_HOST=postgres

# Redis
REDIS_PASSWORD=<generate_strong_password>

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=<generate_strong_password>
KEYCLOAK_HOSTNAME=auth.bss.company.com
KEYCLOAK_ISSUER_URI=https://auth.bss.company.com/realms/bss
KEYCLOAK_JWKS_URI=https://auth.bss.company.com/realms/bss/protocol/openid-connect/certs

# Frontend
NUXT_PUBLIC_API_BASE=https://api.bss.company.com
NUXT_PUBLIC_KEYCLOAK_URL=https://auth.bss.company.com
NUXT_PUBLIC_KEYCLOAK_REALM=bss
NUXT_PUBLIC_KEYCLOAK_CLIENT_ID=bss-frontend

# Monitoring
GRAFANA_ADMIN_PASSWORD=<generate_strong_password>
OTLP_TRACING_ENDPOINT=http://tempo:4317

# SSL/TLS
ACME_EMAIL=ops@bss.company.com
```

### Secrets

For production, use proper secret management:

- **Kubernetes**: Sealed Secrets, external-secrets, or cloud provider secrets
- **Docker Compose**: Keep `.env.production` secure and never commit to git

## Deployment Steps

### 1. Pre-deployment Checklist

- [ ] Review all configuration files
- [ ] Generate strong passwords
- [ ] Configure SSL certificates
- [ ] Set up DNS records
- [ ] Test in staging environment
- [ ] Review resource limits
- [ ] Configure monitoring and alerting

### 2. Deploy Infrastructure

```bash
# Kubernetes
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/keycloak/

# Docker Compose
docker compose -f production-compose.yml up -d postgres redis keycloak
```

### 3. Run Database Migrations

```bash
# Kubernetes (handled by deploy script)
# Or manually:
kubectl run migrations --image=bss/backend:latest --restart=Never -- \
  ./mvnw flyway:migrate

# Docker Compose
docker compose -f production-compose.yml exec backend \
  ./mvnw flyway:migrate
```

### 4. Deploy Applications

```bash
# Kubernetes
kubectl apply -f k8s/backend/
kubectl apply -f k8s/frontend/

# Wait for rollout
kubectl rollout status deployment/bss-backend -n bss
kubectl rollout status deployment/bss-frontend -n bss

# Docker Compose
docker compose -f production-compose.yml up -d backend frontend
```

### 5. Configure Ingress

```bash
# Apply ingress
kubectl apply -f k8s/ingress.yaml

# Verify certificates
kubectl get certificates -n bss
```

## Post-deployment

### Health Checks

```bash
# Check all pods
kubectl get pods -n bss

# Check services
kubectl get services -n bss

# Check ingress
kubectl get ingress -n bss

# Test endpoints
curl -k https://api.bss.company.com/actuator/health
```

### Monitoring

Access monitoring dashboards:
- **Grafana**: https://grafana.bss.company.com
- **Prometheus**: https://prometheus.bss.company.com

Default credentials:
- User: admin
- Password: From GRAFANA_ADMIN_PASSWORD

### Logs

```bash
# Kubernetes
kubectl logs -f deployment/bss-backend -n bss
kubectl logs -f deployment/bss-frontend -n bss

# Docker Compose
docker compose -f production-compose.yml logs -f backend
docker compose -f production-compose.yml logs -f frontend
```

## Operations

### Scaling

**Kubernetes Horizontal Pod Autoscaler (HPA)**:
- Automatically scales based on CPU/memory
- See `k8s/backend/hpa.yaml` for configuration

**Manual scaling**:
```bash
kubectl scale deployment bss-backend --replicas=5 -n bss
kubectl scale deployment bss-frontend --replicas=3 -n bss
```

**Docker Compose**:
```bash
# Edit production-compose.yml
# Change replicas: N
# Then:
docker compose -f production-compose.yml up -d --scale backend=3 --scale frontend=2
```

### Updates

**Kubernetes**:
```bash
# Rolling update
kubectl set image deployment/bss-backend backend=bss/backend:v1.2.0 -n bss

# Using upgrade script
./deploy/upgrade.sh bss/backend:v1.2.0 bss/frontend:v1.2.0
```

**Docker Compose**:
```bash
# Pull new images
docker compose -f production-compose.yml pull

# Recreate containers
docker compose -f production-compose.yml up -d
```

### Rollback

**Kubernetes**:
```bash
# Rollback to previous version
kubectl rollout undo deployment/bss-backend -n bss
kubectl rollout undo deployment/bss-frontend -n bss

# Using rollback script
./deploy/rollback.sh
```

**Docker Compose**:
```bash
# Use specific image tag
docker compose -f production-compose.yml up -d --pull never
```

### Backup

**Database**:
```bash
# PostgreSQL backup
kubectl exec -n bss deployment/postgres -- pg_dump -U bss_app bss > backup.sql

# Restore
kubectl exec -i -n bss deployment/postgres -- psql -U bss_app bss < backup.sql
```

**Docker Compose**:
```bash
# Backup
docker compose -f production-compose.yml exec postgres pg_dump -U bss_app bss > backup.sql

# Restore
docker compose -f production-compose.yml exec -T postgres psql -U bss_app bss < backup.sql
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n bss

# Check events
kubectl get events -n bss --sort-by='.lastTimestamp'

# Check logs
kubectl logs <pod-name> -n bss --previous
```

### High Memory/CPU Usage

```bash
# Check resource usage
kubectl top pods -n bss
kubectl top nodes

# Check HPA status
kubectl get hpa -n bss

# Scale up manually
kubectl scale deployment bss-backend --replicas=10 -n bss
```

### Database Connection Issues

```bash
# Test database connectivity
kubectl exec -n bss deployment/bss-backend -- \
  curl -v http://postgres:5432

# Check connection pool
kubectl exec -n bss deployment/bss-backend -- \
  curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### Performance Issues

```bash
# Check slow queries
kubectl exec -n bss deployment/bss-backend -- \
  curl http://localhost:8080/actuator/metrics/hibernate.statements

# Check cache hit ratio
kubectl exec -n bss deployment/bss-backend -- \
  curl http://localhost:8080/actuator/metrics/redis.cache.hit.ratio
```

## Security Checklist

- [ ] All passwords are strong (16+ chars, mixed case, numbers, symbols)
- [ ] TLS certificates are valid and trusted
- [ ] Secrets are not stored in plain text
- [ ] Network policies are configured
- [ ] RBAC is properly configured
- [ ] Security contexts are set (runAsNonRoot, etc.)
- [ ] Ingress has rate limiting
- [ ] WAF is configured (if available)
- [ ] Regular security updates scheduled
- [ ] Audit logging is enabled
- [ ] Vulnerability scanning is in place

## Maintenance

### Regular Tasks

1. **Weekly**:
   - Check backup integrity
   - Review error rates and latency
   - Update dependencies

2. **Monthly**:
   - Security updates
   - Performance review
   - Capacity planning
   - Disaster recovery test

3. **Quarterly**:
   - Penetration testing
   - Security audit
   - Documentation update
   - Cost optimization

### Upgrade Schedule

- **Security patches**: Within 24 hours
- **Minor updates**: Monthly
- **Major updates**: Quarterly
- **Critical fixes**: Immediate

## Support

- **Documentation**: See `/docs` directory
- **Monitoring**: Grafana dashboards
- **Logs**: Centralized in Loki
- **Alerts**: AlertManager and PagerDuty

## Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Production Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Nuxt Deployment](https://nuxt.com/docs/getting-started/deployment)
- [PostgreSQL Tuning](https://pgtune.leopard.in.ua/)
- [Redis Configuration](https://redis.io/topics/config)
