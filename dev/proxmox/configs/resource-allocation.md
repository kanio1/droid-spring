# Resource Allocation Guide for BSS Load Testing

## Overview

This document describes the resource allocation strategy for BSS (Business Support System) load testing environment across three environments: Development, Staging, and Production.

## Hardware Requirements

### Minimum Proxmox Cluster Setup

- **3x Proxmox Nodes** (for HA and resource distribution)
- **CPU**: 32 cores per node (Intel Xeon or AMD EPYC)
- **RAM**: 256GB per node
- **Storage**: 2TB NVMe SSD per node
- **Network**: 10Gbps bonding

### Resource Allocation by Environment

---

## Development Environment (1K Events)

**Purpose**: Unit testing, integration testing, development team testing

### Database Tier
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 110 | PostgreSQL Master | 16 | 64GB | 500GB | Primary database |
| 111 | PostgreSQL Replica 1 | 12 | 48GB | 500GB | Read replica |
| 113 | Citus Coordinator | 8 | 32GB | 200GB | Shard coordinator |
| 114 | Citus Worker 1 | 8 | 32GB | 200GB | Shard worker |
| 115 | Citus Worker 2 | 8 | 32GB | 200GB | Shard worker |

**Total Database**: 52 vCPU, 208GB RAM, 1.6TB Disk

### Cache Layer
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 120 | Redis Cluster Node 1 | 4 | 16GB | 100GB | Cache + Sessions |
| 121 | Redis Cluster Node 2 | 4 | 16GB | 100GB | Cache + Sessions |
| 122 | Redis Cluster Node 3 | 4 | 16GB | 100GB | Cache + Sessions |

**Total Cache**: 12 vCPU, 48GB RAM, 300GB Disk

### Application Tier
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 200 | Traefik Gateway | 4 | 8GB | 100GB | API Gateway |
| 210 | Backend Service 1 | 8 | 16GB | 200GB | Spring Boot app |
| 211 | Backend Service 2 | 8 | 16GB | 200GB | Spring Boot app |

**Total App**: 20 vCPU, 40GB RAM, 500GB Disk

### Message Queue
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 220 | Kafka Broker 1 | 6 | 16GB | 200GB | Event streaming |
| 221 | Kafka Broker 2 | 6 | 16GB | 200GB | Event streaming |
| 222 | Kafka Broker 3 | 6 | 16GB | 200GB | Event streaming |
| 223 | ZooKeeper | 4 | 8GB | 50GB | Kafka coordination |

**Total MQ**: 22 vCPU, 56GB RAM, 650GB Disk

### Monitoring
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 230 | Prometheus | 4 | 8GB | 200GB | Metrics |
| 231 | Grafana | 2 | 4GB | 50GB | Dashboards |
| 232 | Jaeger | 4 | 8GB | 100GB | Tracing |
| 233 | Tempo | 4 | 8GB | 200GB | Metrics backend |
| 234 | Loki | 4 | 8GB | 200GB | Logs |

**Total Monitoring**: 18 vCPU, 36GB RAM, 750GB Disk

### Development Total
- **Total VMs**: 17
- **Total vCPU**: 124
- **Total RAM**: 388GB
- **Total Disk**: 3.8TB

---

## Staging Environment (10K Events)

**Purpose**: Performance testing, pre-production validation, load profile testing

### Database Tier
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 110 | PostgreSQL Master | 16 | 64GB | 500GB | Primary database |
| 111 | PostgreSQL Replica 1 | 12 | 48GB | 500GB | Read replica |
| 112 | PostgreSQL Replica 2 | 12 | 48GB | 500GB | Read replica |
| 113 | Citus Coordinator | 8 | 32GB | 200GB | Shard coordinator |
| 114 | Citus Worker 1 | 8 | 32GB | 200GB | Shard worker |
| 115 | Citus Worker 2 | 8 | 32GB | 200GB | Shard worker |

**Total Database**: 64 vCPU, 256GB RAM, 2.1TB Disk

### Cache Layer
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 120 | Redis Cluster Node 1 | 4 | 16GB | 100GB | Cache + Sessions |
| 121 | Redis Cluster Node 2 | 4 | 16GB | 100GB | Cache + Sessions |
| 122 | Redis Cluster Node 3 | 4 | 16GB | 100GB | Cache + Sessions |

**Total Cache**: 12 vCPU, 48GB RAM, 300GB Disk

### Application Tier
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 300 | Traefik Gateway | 6 | 12GB | 150GB | API Gateway |
| 310 | Backend Service 1 | 12 | 24GB | 300GB | Spring Boot app |
| 311 | Backend Service 2 | 12 | 24GB | 300GB | Spring Boot app |
| 312 | Backend Service 3 | 12 | 24GB | 300GB | Spring Boot app |

**Total App**: 42 vCPU, 84GB RAM, 1TB Disk

### Message Queue
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 320 | Kafka Broker 1 | 8 | 24GB | 300GB | Event streaming |
| 321 | Kafka Broker 2 | 8 | 24GB | 300GB | Event streaming |
| 322 | Kafka Broker 3 | 8 | 24GB | 300GB | Event streaming |
| 323 | ZooKeeper | 6 | 12GB | 100GB | Kafka coordination |

**Total MQ**: 30 vCPU, 84GB RAM, 1TB Disk

### Monitoring
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 330 | Prometheus | 6 | 12GB | 300GB | Metrics |
| 331 | Grafana | 4 | 8GB | 100GB | Dashboards |
| 332 | Jaeger | 6 | 12GB | 150GB | Tracing |
| 333 | Tempo | 6 | 12GB | 300GB | Metrics backend |
| 334 | Loki | 6 | 12GB | 300GB | Logs |

**Total Monitoring**: 28 vCPU, 56GB RAM, 1.15TB Disk

### Staging Total
- **Total VMs**: 20
- **Total vCPU**: 176
- **Total RAM**: 528GB
- **Total Disk**: 5.55TB

---

## Production Environment (100K-1M Events)

**Purpose**: Maximum load testing, stress testing, production-like environment

### Database Tier
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 110 | PostgreSQL Master | 16 | 64GB | 500GB | Primary database |
| 111 | PostgreSQL Replica 1 | 12 | 48GB | 500GB | Read replica |
| 112 | PostgreSQL Replica 2 | 12 | 48GB | 500GB | Read replica |
| 113 | Citus Coordinator | 8 | 32GB | 200GB | Shard coordinator |
| 114 | Citus Worker 1 | 8 | 32GB | 200GB | Shard worker |
| 115 | Citus Worker 2 | 8 | 32GB | 200GB | Shard worker |

**Total Database**: 64 vCPU, 256GB RAM, 2.1TB Disk

### Cache Layer
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 120 | Redis Cluster Node 1 | 4 | 16GB | 100GB | Cache + Sessions |
| 121 | Redis Cluster Node 2 | 4 | 16GB | 100GB | Cache + Sessions |
| 122 | Redis Cluster Node 3 | 4 | 16GB | 100GB | Cache + Sessions |

**Total Cache**: 12 vCPU, 48GB RAM, 300GB Disk

### Application Tier
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 400 | Traefik Gateway | 12 | 16GB | 200GB | API Gateway (HA) |
| 410 | Backend Service 1 | 16 | 32GB | 500GB | Spring Boot app |
| 411 | Backend Service 2 | 16 | 32GB | 500GB | Spring Boot app |
| 412 | Backend Service 3 | 16 | 32GB | 500GB | Spring Boot app |
| 413 | Backend Service 4 | 16 | 32GB | 500GB | Spring Boot app |
| 414 | Backend Service 5 | 16 | 32GB | 500GB | Spring Boot app |
| 415 | Backend Service 6 | 16 | 32GB | 500GB | Spring Boot app |

**Total App**: 108 vCPU, 208GB RAM, 3.2TB Disk

### Message Queue
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 420 | Kafka Broker 1 | 12 | 32GB | 500GB | Event streaming |
| 421 | Kafka Broker 2 | 12 | 32GB | 500GB | Event streaming |
| 422 | Kafka Broker 3 | 12 | 32GB | 500GB | Event streaming |
| 423 | Kafka Broker 4 | 12 | 32GB | 500GB | Event streaming (expansion) |
| 424 | Kafka Broker 5 | 12 | 32GB | 500GB | Event streaming (expansion) |
| 425 | ZooKeeper | 8 | 16GB | 200GB | Kafka coordination |

**Total MQ**: 68 vCPU, 176GB RAM, 2.7TB Disk

### Monitoring
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 430 | Prometheus | 8 | 16GB | 500GB | Metrics |
| 431 | Grafana | 6 | 12GB | 200GB | Dashboards |
| 432 | Jaeger | 8 | 16GB | 300GB | Tracing |
| 433 | Tempo | 8 | 16GB | 500GB | Metrics backend |
| 434 | Loki | 8 | 16GB | 500GB | Logs |

**Total Monitoring**: 38 vCPU, 76GB RAM, 2TB Disk

### Testing
| VM | Service | vCPU | RAM | Disk | Notes |
|----|---------|------|-----|------|-------|
| 500 | K6 Runner 1 | 8 | 16GB | 100GB | Load test executor |
| 501 | K6 Runner 2 | 8 | 16GB | 100GB | Load test executor |
| 502 | K6 Runner 3 | 8 | 16GB | 100GB | Load test executor |
| 510 | Perf Monitor | 4 | 8GB | 100GB | Performance metrics |

**Total Testing**: 28 vCPU, 56GB RAM, 400GB Disk

### Production Total
- **Total VMs**: 28
- **Total vCPU**: 318
- **Total RAM**: 820GB
- **Total Disk**: 10.7TB

---

## Per-Node Resource Distribution

### Node Allocation Strategy

**pve-node1** (Dev Primary)
- 100, 110, 120, 200, 210, 220, 230, 234, 300, 310, 320, 330, 400, 410, 420, 430, 434, 500, 510, 600, 611
- ~90-100 vCPUs, ~250-300GB RAM

**pve-node2** (Dev Secondary)
- 111, 121, 211, 221, 232, 233, 301, 311, 321, 331, 411, 421, 431, 501, 601
- ~90-100 vCPUs, ~250-300GB RAM

**pve-node3** (Dev Tertiary)
- 112, 122, 212, 222, 223, 234, 312, 322, 332, 412, 422, 432, 502
- ~90-100 vCPUs, ~250-300GB RAM

---

## Resource Scaling Guide

### Vertical Scaling

| Component | Dev | Stage | Prod | Scaling Factor |
|-----------|-----|-------|------|----------------|
| Backend API | 16GB | 24GB | 32GB | 1.5x per tier |
| Database | 64GB | 64GB | 64GB | Fixed |
| Kafka | 16GB | 24GB | 32GB | 1.5x per tier |
| Redis | 16GB | 16GB | 16GB | Fixed |
| Monitoring | 36GB | 56GB | 76GB | 1.5x per tier |

### Horizontal Scaling

| Service | Dev | Stage | Prod | Notes |
|---------|-----|-------|------|-------|
| Backend | 2 | 3 | 6 | 50% increase per tier |
| Kafka | 3 | 3 | 5 | Expansion for prod |
| DB Replicas | 1 | 2 | 2 | Read scaling |
| Citus Workers | 2 | 2 | 2 | Shard distribution |

---

## Deployment Timeline

| Week | Phase | Environment | Action |
|------|-------|-------------|--------|
| 1 | Setup | All | Proxmox cluster, base VMs |
| 2 | Dev | Dev | Deploy dev environment, smoke tests |
| 3 | Stage | Stage | Deploy stage environment, integration tests |
| 4 | Prod | Prod | Deploy prod environment, performance tests |

---

## Cost Estimation (AWS equivalents)

| Environment | VMs | vCPU | RAM | Est. Monthly Cost |
|-------------|-----|------|-----|-------------------|
| Dev | 17 | 124 | 388GB | $3,500 |
| Stage | 20 | 176 | 528GB | $5,200 |
| Prod | 28 | 318 | 820GB | $8,900 |
| **Total** | **65** | **618** | **1.7TB** | **$17,600** |

*Costs based on AWS r5.2xlarge instances (8 vCPU, 64GB RAM)*

---

## Network Requirements

### Bandwidth
- **Intra-cluster**: 10Gbps
- **Internet**: 1Gbps
- **Backup**: 10Gbps

### Storage IOPS
- **Database**: 20,000 IOPS per VM
- **Application**: 5,000 IOPS per VM
- **Monitoring**: 10,000 IOPS per VM

---

## Monitoring & Alerts

### Resource Alerts
- CPU > 80% for 5 minutes
- Memory > 85% for 5 minutes
- Disk > 90% for 1 minute
- Network > 1Gbps sustained

### SLA Targets
- **Dev**: 95% uptime
- **Stage**: 99% uptime
- **Prod**: 99.9% uptime
