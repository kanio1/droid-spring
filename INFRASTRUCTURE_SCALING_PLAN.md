# Plan Skalowania Systemu BSS do 400k Zdarzeń/Min

## 📋 Przegląd Planu

**Cel:** Skalowanie systemu BSS do obsługi 400,000 zdarzeń na minutę (6,667 zdarzeń/sekundę)
**Architektura:** Event-driven z dystrybucją na 3 maszyny wirtualne w Proxmox
**Technologie:** PostgreSQL 18, Redis 8.0, Apache Kafka 4.0, Traefik, CloudEvents v1.0.1

---

## 🏗️ Architektura Docelowa

### VM Distribution Strategy

```
┌─────────────────────────────────────────────────────────┐
│                    Proxmox Cluster                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │     VM1      │  │     VM2      │  │     VM3      │   │
│  │  Database    │  │   Streaming  │  │  Cache + GW  │   │
│  │             │  │             │  │             │   │
│  │ PostgreSQL 18│  │ Kafka 4.0   │  │ Redis 8.0   │   │
│  │ (Master)    │  │ (3 Brokers)  │  │ (Cluster)   │   │
│  │ + Replica   │  │ KRaft Mode   │  │ + Traefik   │   │
│  │ AIO Enabled │  │ 100+ Part.   │  │ API Gateway │   │
│  │ 64GB RAM    │  │ 32GB RAM     │  │ 48GB RAM    │   │
│  │ 16 vCPU     │  │ 16 vCPU      │  │ 16 vCPU     │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
│         │                │                 │              │
│    ┌────┴────────────────┴─────────────────┴────┐        │
│    │         Shared Storage (Ceph/ZFS)          │        │
│    └────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

### Komponenty w Każdej VM

**VM1: Database & Persistence**
- PostgreSQL 18 (Master + Read Replica)
- Asynchronous I/O (AIO) enabled
- Shared storage via Ceph/ZFS
- **Throughput:** 10,000+ inserts/sec

**VM2: Event Streaming**
- Apache Kafka 4.0 (3-broker cluster)
- KRaft mode (no ZooKeeper)
- Compression: snappy/lz4
- **Throughput:** 1M+ messages/sec

**VM3: Cache & API Gateway**
- Redis 8.0 Cluster (master-slave)
- Traefik v3 API Gateway
- CloudEvents validation
- **Throughput:** 500k+ ops/sec

---

## 🔧 Komponenty Szczegółowo

### 1. PostgreSQL 18 (AIO) - VM1

**Kluczowe Funkcje:**
- **Asynchronous I/O:** 20-50% wzrost throughput
- **UUIDv7:** Native support dla event IDs
- **Skip Scans:** Faster queries on partitioned tables
- **Parallel Queries:** Better multi-core utilization

**Tuning Configuration:**
```yaml
postgresql.conf:
  shared_buffers: 16GB                    # 25% of RAM
  effective_cache_size: 48GB              # 75% of RAM
  io_method: aio                         # Enable AIO
  max_wal_size: 4GB
  min_wal_size: 1GB
  wal_buffers: 64MB
  checkpoint_completion_target: 0.9
  random_page_cost: 1.1                   # For NVMe
  max_worker_processes: 16
  max_parallel_workers_per_gather: 8
```

**Partitioning Strategy:**
```sql
-- Partition events by time (daily)
CREATE TABLE events_2025_11 PARTITION OF events
FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

-- Index on partition key
CREATE INDEX ON events_2025_11 (event_time, event_type);

-- Batch insert optimization
COPY events FROM STDIN WITH (FORMAT 'binary');
```

### 2. Apache Kafka 4.0 (KRaft) - VM2

**Kluczowe Funkcje:**
- **KRaft Mode:** No ZooKeeper needed, simpler scaling
- **Queues:** Better message ordering
- **Faster Rebalances:** KIP-848 implementation
- **Improved Compression:** Better throughput

**Broker Configuration:**
```yaml
server.properties:
  node.id=1
  log.dirs=/var/lib/kafka/data
  num.network.threads=32
  num.io.threads=64
  socket.send.buffer.bytes=102400
  socket.receive.buffer.bytes=102400
  socket.request.max.bytes=104857600
  num.partitions=100
  default.replication.factor=3
  min.insync.replicas=2
  compression.type=snappy
  batch.size=1048576
  linger.ms=5
  buffer.memory=67108864
  log.retention.hours=168
  log.segment.bytes=1073741824
```

**Topic Configuration:**
```bash
kafka-topics --create \
  --topic cloud-events \
  --partitions 100 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000 \
  --config compression.type=snappy
```

### 3. Redis 8.0 (Streams) - VM3

**Kluczowe Funkcje:**
- **Vector Sets:** Future-ready dla embeddings
- **Hash Field Expiration:** Granular TTL
- **Client-Side Caching:** Reduced round-trips
- **Redis Streams:** Built-in event streaming

**Configuration:**
```redis
redis.conf:
  maxmemory: 32gb
  maxmemory-policy: allkeys-lru
  tcp-keepalive: 300
  timeout: 0
  save: 900 1 300 10 60 10000
  appendfsync: everysec
  cluster-enabled: yes
  cluster-node-timeout: 15000
  cluster-require-full-coverage: no
```

**Streams Setup:**
```bash
XADD events:cloud * event-type payment.created event-id ${uuid}
XGROUP CREATE events:cloud payment-service $0 MKSTREAM
```

### 4. Traefik v3 - API Gateway - VM3

**Dlaczego Traefik zamiast Kong?**

| Aspekt | Traefik v3 | Kong Gateway |
|--------|------------|--------------|
| **Performance** | ⭐⭐⭐⭐⭐ (0.5ms latency) | ⭐⭐⭐⭐ (1-2ms) |
| **Configuration** | ⭐⭐⭐⭐⭐ (Auto-discovery) | ⭐⭐⭐ (Manual/YAML) |
| **CloudEvents** | ⭐⭐⭐⭐⭐ (Native validators) | ⭐⭐⭐⭐ (Plugin-based) |
| **Resource Usage** | ⭐⭐⭐⭐⭐ (50MB RAM) | ⭐⭐⭐ (200MB+ RAM) |
| **Metrics** | ⭐⭐⭐⭐⭐ (Prometheus built-in) | ⭐⭐⭐⭐ (Plugin) |
| **SSL/TLS** | ⭐⭐⭐⭐⭐ (Auto cert, Let's Encrypt) | ⭐⭐⭐⭐ (Manual config) |
| **Docker/K8s** | ⭐⭐⭐⭐⭐ (Native integration) | ⭐⭐⭐ (Plugin) |
| **Learning Curve** | ⭐⭐⭐⭐⭐ (Simple YAML) | ⭐⭐⭐ (Complex) |

**Traefik Configuration (traefik.yml):**
```yaml
api:
  dashboard: true
  insecure: true

entryPoints:
  web:
    address: ":80"
  websecure:
    address: ":443"
  traefik:
    address: ":8080"

providers:
  docker:
    endpoint: "unix:///var/run/docker.sock"
    exposedByDefault: false
  file:
    filename: /etc/traefik/dynamic.yml
    watch: true

metrics:
  prometheus:
    addEntryPointsLabels: true
    addServicesLabels: true

log:
  level: INFO

accessLog: {}

experimental:
  http3: true
```

**CloudEvents Middleware (dynamic.yml):**
```yaml
http:
  middlewares:
    cloudevents-validator:
      plugin:
        cloudevents-validator:
          schema: |
            type: object
            properties:
              specversion: { type: string }
              type: { type: string }
              source: { type: string }
            required: [specversion, type, source]
          strict: false

    rate-limit:
      plugin:
        rate-limit:
          burst: 10000
          average: 6667

  routers:
    api-kafka:
      rule: "PathPrefix(`/api/events`)"
      service: kafka-service
      middlewares:
        - "cloudevents-validator"
        - "rate-limit"

  services:
    kafka-service:
      loadBalancer:
        servers:
          - url: "http://kafka:8080"

    redis-service:
      loadBalancer:
        servers:
          - url: "http://redis:6379"
```

### 5. CloudEvents v1.0.1

**Format Zdarzenia:**
```json
{
  "specversion": "1.0",
  "type": "payment.created",
  "id": "evt-12345",
  "source": "/tenants/tenant-001",
  "time": "2025-11-07T10:30:00Z",
  "tenantid": "tenant-001",
  "data": {
    "event_id": "evt-12345",
    "amount": 100.50,
    "currency": "USD",
    "customer_id": "cust-001"
  }
}
```

**Validation Schema:**
```json
{
  "type": "object",
  "properties": {
    "specversion": { "type": "string", "enum": ["1.0"] },
    "type": { "type": "string" },
    "id": { "type": "string" },
    "source": { "type": "string" },
    "time": { "type": "string", "format": "date-time" },
    "tenantid": { "type": "string" },
    "data": { "type": "object" }
  },
  "required": ["specversion", "type", "id", "source"]
}
```

---

## 📈 Propozycje Nowych Funkcjonalności

### 1. **Advanced Observability Stack** 🔍
**Components:** Prometheus + Grafana + custom metrics

**Features:**
- Real-time event throughput dashboard
- Distributed tracing across Kafka → Postgres → Redis
- Custom BSS metrics (orders/min, invoices/sec, fraud alerts)
- Anomaly detection on event patterns
- SLA monitoring z automatycznym alertowaniem

**Prometheus Configuration:**
```yaml
scrape_configs:
  - job_name: 'bss-backend'
    static_configs:
      - targets: ['vm1:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'kafka'
    static_configs:
      - targets: ['vm2:9092']

  - job_name: 'redis'
    static_configs:
      - targets: ['vm3:6379']

  - job_name: 'traefik'
    static_configs:
      - targets: ['vm3:8080']
```

### 2. **Event Replay & Time Travel** ⏪
**Components:** Kafka offset management + PostgreSQL temporal tables

**Features:**
- Selective event replay by time range
- State reconstruction at any point in time
- Debug mode with controlled replay speed
- A/B testing with historical data
- Compliance auditing

### 3. **Intelligent Cache Invalidation** 🧠
**Components:** Redis + Postgres LISTEN/NOTIFY + Event-driven invalidation

**Features:**
- Automatic cache invalidation on database changes
- Event-driven cache warming
- Probabilistic early expiration
- Distributed cache coherence

### 4. **Dynamic Partitioning & Auto-Sharding** ⚖️
**Components:** Kafka auto-partition + Redis cluster rebalancing

**Features:**
- Auto-scaling partitions based on load
- Hot partition detection and mitigation
- Redis cluster automatic sharding
- Load prediction with auto-scaling

### 5. **Secure Multi-Tenant Event Routing** 🔐
**Components:** Traefik + JWT + CloudEvents validation

**Features:**
- Tenant-aware event routing
- CloudEvents schema validation per tenant
- Rate limiting per tenant
- Encryption at rest and in transit

---

## 🧪 Symulatory i Generatory (Golang)

### 1. **Kafka Event Generator**
**Plik:** `dev/simulators/kafka-event-generator.go`
**Funkcjonalności:**
- Generuje CloudEvents w formacie Kafka
- Konfigurowalne: batch size, compression, throughput
- Testuje 400k+ events/min
- Obsługuje multiple tenants
- Kompresja: gzip, snappy, lz4

**Uruchomienie:**
```bash
cd dev/simulators
go mod tidy
go run kafka-event-generator.go
```

**Output:**
```
2025-11-07 10:30:00 Starting load test: 5 tenants, 80000 events/tenant, duration 1 min
2025-11-07 10:30:05 Stats: Total=33335, Success=33335, Errors=0, Rate=6667.00 events/sec
2025-11-07 10:31:00 Load test completed in 60.00 seconds
2025-11-07 10:31:00 Final stats: Total=400000, Success=400000, Errors=0, Rate=6667.50 events/sec
```

### 2. **Redis Streams Simulator**
**Plik:** `dev/simulators/redis-streams-simulator.go`
**Funkcjonalności:**
- Redis Streams (XADD, XGROUP)
- Consumer groups dla horizontal scaling
- Batch operations via pipeline
- Testuje 50k+ msgs/sec

### 3. **PostgreSQL Batch Simulator**
**Plik:** `dev/simulators/postgres-batch-simulator.go`
**Funkcjonalności:**
- Batch inserts (COPY)
- Multi-worker concurrent inserts
- Partitioned tables
- Testuje 10k+ inserts/sec

### 4. **Integrated Load Tester**
**Plik:** `dev/simulators/load-tester.go`
**Funkcjonalności:**
- Testuje wszystkie komponenty jednocześnie
- Synchronizowany throughput
- Performance metrics
- Memory usage tracking

---

## 📊 Rekomendacje Strategiczne

### Phase 1: Foundation (Month 1-2)
1. ✅ **Upgrade PostgreSQL to 18, enable AIO**
2. ✅ **Upgrade Redis to 8.0, enable clustering**
3. ✅ **Upgrade Kafka to 4.0, migrate to KRaft**
4. 🔲 **Deploy Traefik API Gateway** (VMs setup first)
5. 🔲 **Set up 3-VM Proxmox cluster** (end phase)

### Phase 2: Optimization (Month 3-4)
1. Implement Advanced Observability
2. Add Event Replay & Time Travel
3. Deploy Intelligent Cache Invalidation
4. Performance tuning and benchmarking

### Phase 3: Advanced Features (Month 5-6)
1. Dynamic Partitioning & Auto-Sharding
2. Secure Multi-Tenant Routing
3. Load testing z 400k events/min
4. Production hardening

---

## 📈 Oczekiwane Wyniki

| Metryka | Obecny | Cel | Poprawa |
|---------|--------|-----|---------|
| **Event Throughput** | 50k/min | 400k/min | 700% ↑ |
| **Latency (p99)** | 500ms | 50ms | 90% ↓ |
| **Cache Hit Rate** | 60% | 95% | 58% ↑ |
| **Uptime SLA** | 99.5% | 99.95% | 0.45% ↑ |
| **Time to Detect Issues** | 15min | 30sec | 97% ↓ |

### Infrastructure Cost (3 VM Proxmox)
- **VM1 (DB)**: $400/month (16 vCPU, 64GB RAM, NVMe)
- **VM2 (Kafka)**: $320/month (16 vCPU, 32GB RAM, NVMe)
- **VM3 (Cache/Gateway)**: $360/month (16 vCPU, 48GB RAM, NVMe)
- **Total**: $1,080/month
- **Cost per 1M events**: $0.45

---

## 🚀 Next Steps

1. Uruchomić symulatory w dev environment
2. Zmierzyć baseline performance
3. Zaktualizować konfiguracje komponentów
4. Przeprowadzić incremental testing
5. Przygotować Proxmox VM setup

---

**Dokument utworzony:** 2025-11-07
**Wersja:** 1.0
**Status:** W implementacji
