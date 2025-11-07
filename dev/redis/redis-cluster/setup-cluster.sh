#!/bin/bash
# ============================================
# Redis Cluster Setup Script
# ============================================
# Purpose: Initialize and configure Redis cluster
# Creates a 6-node cluster (3 masters + 3 replicas)
# Created: 2025-11-07
# ============================================

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
REDIS_CLUSTER_NODES=6
REDIS_CLUSTER_PORTS=(7000 7001 7002 7003 7004 7005)
REDIS_CLUSTER_BUS_PORTS=(17000 17001 17002 17003 17004 17005)
LOG_FILE="/tmp/redis-cluster-setup.log"

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
    exit 1
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

# Check if services are running
check_services() {
    log "Checking if Redis services are running..."

    for port in "${REDIS_CLUSTER_PORTS[@]}"; do
        if ! docker ps | grep -q "bss-redis-cluster-$port"; then
            warn "Redis cluster node on port $port is not running"
            return 1
        fi
    done

    log "All Redis cluster nodes are running"
    return 0
}

# Wait for Redis to be ready
wait_for_redis() {
    local port=$1
    log "Waiting for Redis on port $port to be ready..."

    for i in {1..30}; do
        if docker exec bss-redis-cluster-$port redis-cli --tls \
            --cacert /etc/ssl/certs/ca-cert.pem \
            --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
            ping > /dev/null 2>&1; then
            log "Redis on port $port is ready!"
            return 0
        fi
        if [ $i -eq 30 ]; then
            error "Redis on port $port did not become ready in time"
        fi
        sleep 2
    done
}

# Create cluster nodes configuration
create_cluster_config() {
    log "Creating cluster node configuration..."

    local nodes_array=()

    for port in "${REDIS_CLUSTER_PORTS[@]}"; do
        nodes_array+=("127.0.0.1:$port")
    done

    local nodes_string=$(IFS=','; echo "${nodes_array[*]}")

    echo "$nodes_string"
}

# Create Redis cluster
create_cluster() {
    log "Creating Redis cluster..."

    local port=${REDIS_CLUSTER_PORTS[0]}
    local nodes=$(create_cluster_config)

    # Create cluster with --cluster-replicas 1 (1 replica per master)
    docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        --cluster create $nodes \
        --cluster-replicas 1 \
        --cluster-yes

    if [ $? -eq 0 ]; then
        log "Redis cluster created successfully!"
    else
        error "Failed to create Redis cluster"
    fi
}

# Check cluster status
check_cluster_status() {
    log "Checking cluster status..."

    local port=${REDIS_CLUSTER_PINTS[0]}

    docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        --cluster check 127.0.0.1:$port

    # Get cluster info
    log "Cluster info:"
    docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        --cluster info

    # List nodes
    log "Cluster nodes:"
    docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        cluster nodes
}

# Test cluster operations
test_cluster_operations() {
    log "Testing cluster operations..."

    local port=${REDIS_CLUSTER_PORTS[0]}

    # Test SET/GET
    log "Testing SET/GET operations..."
    docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        set test:key "test:value" > /dev/null

    local value=$(docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        get test:key)

    if [ "$value" = "test:value" ]; then
        log "✓ SET/GET test passed"
    else
        warn "✗ SET/GET test failed (got: $value)"
    fi

    # Test cluster slots
    log "Testing cluster slots..."
    docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        cluster slots

    # Test cluster nodes
    log "Testing cluster nodes..."
    local nodes_count=$(docker exec bss-redis-cluster-$port redis-cli --tls \
        --cacert /etc/ssl/certs/ca-cert.pem \
        --pass "${REDIS_CLUSTER_PASSWORD:-redis_cluster_password_123}" \
        cluster nodes | wc -l)

    if [ $nodes_count -eq 6 ]; then
        log "✓ All 6 nodes are in the cluster"
    else
        warn "✗ Expected 6 nodes, found $nodes_count"
    fi
}

# Show connection information
show_connection_info() {
    log ""
    log "==========================================="
    log "Redis Cluster Connection Information"
    log "==========================================="
    log ""
    log "Cluster Nodes:"
    for i in "${!REDIS_CLUSTER_PORTS[@]}"; do
        local port=${REDIS_CLUSTER_PORTS[$i]}
        local bus_port=${REDIS_CLUSTER_BUS_PORTS[$i]}
        log "  Node $((i+1)): localhost:$port (TLS: $bus_port)"
    done
    log ""
    log "TLS Ports (Client Connections):"
    for port in "${REDIS_CLUSTER_PORTS[@]}"; do
        log "  redis-cli --tls -h localhost -p $port"
    done
    log ""
    log "Configuration File: /home/labadmin/projects/droid-spring/dev/redis/redis-cluster/"
    log "Log File: $LOG_FILE"
    log ""
    log "==========================================="
    log "Useful Commands"
    log "==========================================="
    log ""
    log "Connect to cluster:"
    log "  docker exec -it bss-redis-cluster-7000 redis-cli --tls \\"
    log "    --cacert /etc/ssl/certs/ca-cert.pem \\"
    log "    --pass redis_cluster_password_123"
    log ""
    log "Check cluster status:"
    log "  docker exec bss-redis-cluster-7000 redis-cli --tls \\"
    log "    --cacert /etc/ssl/certs/ca-cert.pem \\"
    log "    --pass redis_cluster_password_123 \\"
    log "    cluster info"
    log ""
    log "List all nodes:"
    log "  docker exec bss-redis-cluster-7000 redis-cli --tls \\"
    log "    --cacert /etc/ssl/certs/ca-cert.pem \\"
    log "    --pass redis_cluster_password_123 \\"
    log "    cluster nodes"
    log ""
    log "Add new node:"
    log "  docker exec bss-redis-cluster-7000 redis-cli --tls \\"
    log "    --cacert /etc/ssl/certs/ca-cert.pem \\"
    log "    --pass redis_cluster_password_123 \\"
    log "    --cluster add-node <new_node_ip:port> <existing_node_ip:port>"
    log ""
    log "Reshard slots:"
    log "  docker exec bss-redis-cluster-7000 redis-cli --tls \\"
    log "    --cacert /etc/ssl/certs/ca-cert.pem \\"
    log "    --pass redis_cluster_password_123 \\"
    log "    --cluster reshard <node_id>"
    log ""
    log "Monitor cluster:"
    log "  watch -n 1 'docker exec bss-redis-cluster-7000 redis-cli --tls \\"
    log "    --cacert /etc/ssl/certs/ca-cert.pem \\"
    log "    --pass redis_cluster_password_123 \\"
    log "    cluster info'"
    log "==========================================="
}

# Main execution
main() {
    log "==========================================="
    log "Redis Cluster Setup"
    log "==========================================="
    log ""

    # Check if services are running
    if ! check_services; then
        error "Please start Redis cluster services first:\n  docker compose -f dev/compose.yml up -d redis-cluster"
    fi

    # Wait for all Redis instances to be ready
    for port in "${REDIS_CLUSTER_PORTS[@]}"; do
        wait_for_redis $port
    done

    log ""
    log "Creating Redis cluster..."
    log "This will create a $REDIS_CLUSTER_NODES-node cluster"
    log "  - 3 master nodes (will hold data)"
    log "  - 3 replica nodes (backup/failover)"
    log ""

    read -p "Do you want to continue? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log "Aborted by user"
        exit 0
    fi

    # Create cluster
    create_cluster

    # Check status
    log ""
    check_cluster_status

    # Test operations
    log ""
    test_cluster_operations

    # Show connection info
    log ""
    show_connection_info

    log ""
    log "==========================================="
    log "Redis Cluster Setup Complete!"
    log "==========================================="
    log ""
    log "Next steps:"
    log "  1. Update application configuration to use cluster nodes"
    log "  2. Test application connection to cluster"
    log "  3. Monitor cluster health"
    log "  4. Set up monitoring and alerting"
    log ""
    log "For help: /home/labadmin/projects/droid-spring/dev/redis/redis-cluster/REDIS-CLUSTER.md"
    log "==========================================="
}

# Run main function
main
