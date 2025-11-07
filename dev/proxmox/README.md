# Proxmox VM Configuration for BSS Load Testing

## Overview

This directory contains the complete Proxmox VM configuration for BSS (Business Support System) load testing environment. The setup supports testing with **1K**, **10K**, and **1M+ events** across three environments: Development, Staging, and Production.

## Directory Structure

```
dev/proxmox/
├── configs/
│   ├── vm-inventory.csv          # VM inventory with specifications
│   └── resource-allocation.md    # Detailed resource allocation guide
├── scripts/
│   ├── create-vms.sh            # Create VMs from template
│   ├── start-vms.sh             # Start VMs
│   ├── stop-vms.sh              # Stop VMs
│   ├── deploy-services.sh       # Deploy services to VMs
│   └── health-check.sh          # Health check for all services
└── README.md                    # This file
```

## Quick Start

### Prerequisites

1. **Proxmox VE 8.0+** cluster with 3 nodes
2. **Base template VM** (ID: 9000) with:
   - Ubuntu 22.04 LTS
   - Docker & Docker Compose
   - SSH access configured
   - Cloud-init enabled
3. **API Access** to Proxmox cluster
4. **Network** with VLAN support for isolation

### Environment Variables

```bash
export PROXMOX_HOST="pve.lab.local"
export PROXMOX_USER="root@pam"
export PROXMOX_PASSWORD="your_password"
```

### Step 1: Create VMs

```bash
# Create development environment VMs (1K events)
./scripts/create-vms.sh --env dev

# Create staging environment VMs (10K events)
./scripts/create-vms.sh --env stage

# Create production environment VMs (100K-1M events)
./scripts/create-vms.sh --env prod

# Dry run to see what would be created
./scripts/create-vms.sh --env dev --dry-run
```

### Step 2: Start VMs

```bash
# Start all dev VMs
./scripts/start-vms.sh dev

# Start all VMs
./scripts/start-vms.sh all
```

### Step 3: Deploy Services

```bash
# Deploy to dev environment
./scripts/deploy-services.sh dev

# With custom registry and tag
DOCKER_REGISTRY=my-registry.com IMAGE_TAG=v1.2.3 ./scripts/deploy-services.sh prod
```

### Step 4: Health Check

```bash
# Check all services
./scripts/health-check.sh dev
```

### Step 5: Run Load Tests

```bash
# K6 tests will be run from bss-k6-runner VMs
# See dev/k6/ directory for test scripts
```

## VM Inventory

### Development Environment (1K Events)

**Total Resources:**
- VMs: 17
- vCPU: 124
- RAM: 388GB
- Disk: 3.8TB

**Key Components:**
- PostgreSQL 18 + Citus (3 nodes)
- Redis Cluster (3 nodes)
- Spring Boot Backend (2 instances)
- Kafka Cluster (3 brokers)
- Monitoring Stack (5 services)

### Staging Environment (10K Events)

**Total Resources:**
- VMs: 20
- vCPU: 176
- RAM: 528GB
- Disk: 5.55TB

**Key Components:**
- PostgreSQL + 2 Replicas + Citus
- Redis Cluster (3 nodes)
- Spring Boot Backend (3 instances)
- Kafka Cluster (3 brokers)
- Monitoring Stack (5 services)

### Production Environment (100K-1M Events)

**Total Resources:**
- VMs: 28
- vCPU: 318
- RAM: 820GB
- Disk: 10.7TB

**Key Components:**
- PostgreSQL + 2 Replicas + Citus
- Redis Cluster (3 nodes)
- Spring Boot Backend (6 instances)
- Kafka Cluster (5 brokers)
- Monitoring Stack (5 services)
- K6 Load Test Runners (3 instances)

## VM ID Ranges

| Environment | VM ID Range | Purpose |
|-------------|-------------|---------|
| Infrastructure | 100-199 | LB, DB, Cache |
| Development | 200-299 | API, App, MQ, Monitor |
| Staging | 300-399 | API, App, MQ, Monitor |
| Production | 400-499 | API, App, MQ, Monitor |
| Testing | 500-599 | K6, Performance |
| Admin | 600-699 | CI/CD, Management |

## Resource Allocation

See [configs/resource-allocation.md](configs/resource-allocation.md) for detailed resource specifications per VM.

### Per-Node Distribution

VMs are distributed across 3 Proxmox nodes for high availability:

- **pve-node1**: Infrastructure + Dev + Prod services
- **pve-node2**: Infrastructure + Dev + Prod services
- **pve-node3**: Infrastructure + Dev + Prod services

Each node gets ~90-100 vCPUs and ~250-300GB RAM.

## Network Configuration

### Bridges
- **vmbr0**: Management network (192.168.1.0/24)
- **vmbr100**: Development (10.100.100.0/24)
- **vmbr200**: Staging (10.200.200.0/24)
- **vmbr300**: Production (10.300.300.0/24)

### Network Types
All VMs use **virtio** network drivers for optimal performance.

## Storage Configuration

### Disk Sizing
- **OS Disk**: 50-100GB
- **Data Disk**: 200-500GB (depends on service)
- **Total per VM**: See vm-inventory.csv

### Storage Tiering
- **NVMe SSD**: Database, Kafka, monitoring
- **SATA SSD**: Application services
- **Backup Storage**: Dedicated backup server (VM 611)

## Service Deployment

### Deployment Order

1. **Database Layer** (5-10 minutes)
   - PostgreSQL cluster
   - Redis cluster
   - Wait for healthy

2. **Message Queue** (3-5 minutes)
   - Kafka brokers
   - ZooKeeper ensemble
   - Topic creation

3. **Application Layer** (5-10 minutes)
   - Traefik gateway
   - Backend services
   - Health checks

4. **Monitoring** (5 minutes)
   - Prometheus
   - Grafana
   - Jaeger, Tempo, Loki

5. **Load Testing** (2 minutes)
   - K6 runners
   - Test data generator

### Docker Compose

Each VM runs services via Docker Compose. Example for backend VM:

```yaml
version: '3.8'
services:
  backend:
    image: bss/backend:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - POSTGRES_HOST=bss-db-master
    depends_on:
      - postgres
      - redis
```

## Monitoring & Observability

### Metrics
- **Prometheus**: 15s scrape interval
- **Grafana**: Pre-built dashboards
- **JMX**: JVM metrics enabled
- **Node Exporter**: System metrics

### Tracing
- **Jaeger**: Distributed tracing
- **Tempo**: Metrics backend
- **OpenTelemetry**: Instrumentation

### Logging
- **Loki**: Log aggregation
- **Promtail**: Log collection
- **Log rotation**: 7-day retention

## Backup Strategy

### VM Backups
- **Daily**: Full VM snapshot
- **Retention**: 7 days dev, 30 days prod
- **Location**: Backup server VM 611

### Database Backups
- **Continuous**: WAL archiving
- **Daily**: pg_dump custom format
- **Weekly**: Full backup with encryption
- **S3**: Upload for disaster recovery

See `../../scripts/backup/` for backup scripts.

## Load Testing

### K6 Configuration
- **Test Types**: Smoke, Load, Stress, Spike, Soak
- **Event Counts**: 1K, 10K, 100K, 1M
- **Duration**: Ramping, steady-state, ramping down
- **Metrics**: Response time, throughput, error rate

### Test Execution
```bash
# From K6 runner VM
k6 run --vus 100 --duration 10m /opt/tests/load-test.js

# Distributed testing
k6 run --stage 5m:100,10m:1000,5m:100 /opt/tests/stress-test.js
```

## Troubleshooting

### Common Issues

**VM Won't Start**
```bash
# Check Proxmox logs
pvesh get /nodes/{node}/qemu/{vmid}/status/current

# Verify template
qm template {template_id}
```

**Services Not Responding**
```bash
# Run health check
./scripts/health-check.sh dev

# Check logs
docker-compose logs -f
```

**Performance Issues**
```bash
# Check resource usage
htop
iotop
docker stats

# Check Prometheus metrics
curl http://prometheus:9090/metrics
```

### Useful Commands

```bash
# List all BSS VMs
qm list | grep -E "bss-|bss_"

# Get VM status
qm status {vmid}

# Start VM
qm start {vmid}

# Stop VM
qm stop {vmid}

# Console access
qm terminal {vmid}
```

## Scaling

### Vertical Scaling
Increase VM resources (vCPU, RAM, Disk) as needed.

### Horizontal Scaling
Add more backend instances or Kafka brokers:
1. Update vm-inventory.csv
2. Run create-vms.sh
3. Add to load balancer pool
4. Update monitoring config

## Maintenance

### Updates
- **OS**: Monthly security patches
- **Docker Images**: Weekly updates
- **Dependencies**: Quarterly major version updates

### Monitoring Alerts
- CPU > 80% for 5 minutes
- Memory > 85% for 5 minutes
- Disk > 90% for 1 minute
- Service health check failures

## Security

### Network Isolation
- VLANs per environment
- Firewall rules (iptables/nftables)
- No direct internet access (except via Traefik)

### Secrets Management
- **Vault**: HashiCorp Vault for production
- **Env files**: For dev/stage
- **Encryption**: At rest and in transit

### Access Control
- **Proxmox**: RBAC for user management
- **SSH**: Key-based authentication only
- **Docker**: Read-only root filesystem

## Cost Optimization

### Resource Utilization
- **Dev**: Turn off on weekends
- **Stage**: Turn off nightly
- **Prod**: 24/7 operation

### Reserved Instances
- **Production**: Use reserved instances (30-50% savings)
- **Development**: Spot instances (up to 90% savings)

## Support & Documentation

### Documentation
- **Architecture**: See main project README
- **API Docs**: `/actuator` endpoints
- **Monitoring**: Grafana dashboards at port 3000

### Getting Help
- **Slack**: #bss-support
- **Email**: bss-team@company.com
- **Jira**: BSS project for tracking

## License

Proprietary - See LICENSE file in project root

## Changelog

### v1.0 (2025-11-06)
- Initial Proxmox configuration
- Support for 1K, 10K, 1M+ event testing
- Three environment setup (dev, stage, prod)
- 65 VMs across 3 Proxmox nodes
- Complete automation scripts
- Resource allocation guide
