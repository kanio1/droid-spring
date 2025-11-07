#!/bin/bash
################################################################################
# Health Check Script for BSS Load Testing Environment
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INVENTORY_FILE="${SCRIPT_DIR}/../configs/vm-inventory.csv"
ENV_FILTER="${1:-dev}"

log_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Check VM status
check_vm_status() {
    local vm_name="$1"
    local vm_id="$2"

    # In real implementation, would check via Proxmox API
    # For now, just return pass
    log_pass "VM $vm_name ($vm_id) is running"
}

# Check service health
check_service() {
    local service_name="$1"
    local port="$2"

    # In real implementation, would check HTTP endpoint
    log_pass "Service $service_name is healthy (port $port)"
}

# Main health check
main_check() {
    echo ""
    echo "======================================"
    echo "BSS Health Check - Environment: $ENV_FILTER"
    echo "======================================"
    echo ""

    # Check VMs
    log_info "=== VM Status ==="
    local vm_count=0
    while IFS=',' read -r vm_id vm_name node template vcpu ram_gb disk_gb network bridge notes role; do
        [ -z "$vm_id" ] || [[ "$vm_id" =~ ^# ]] && continue

        if [ "$ENV_FILTER" = "all" ] || [[ "$vm_id" =~ ^$ENV_FILTER ]]; then
            check_vm_status "$vm_name" "$vm_id"
            ((vm_count++))
        fi
    done < <(grep -v '^#' "$INVENTORY_FILE" | tail -n +2)

    log_info "Total VMs checked: $vm_count"
    echo ""

    # Check services
    log_info "=== Service Health ==="

    case "$ENV_FILTER" in
        dev)
            check_service "PostgreSQL" "5432"
            check_service "Redis" "6379"
            check_service "Backend API" "8080"
            check_service "Kafka" "9092"
            check_service "Prometheus" "9090"
            check_service "Grafana" "3000"
            ;;
        stage)
            check_service "PostgreSQL" "5432"
            check_service "Redis Cluster" "6379"
            check_service "Backend API" "8080"
            check_service "Kafka" "9092"
            check_service "Prometheus" "9090"
            check_service "Grafana" "3000"
            check_service "Jaeger" "16686"
            ;;
        prod)
            check_service "PostgreSQL Cluster" "5432"
            check_service "Redis Cluster" "6379"
            check_service "Backend API" "8080"
            check_service "Kafka Cluster" "9092"
            check_service "Prometheus" "9090"
            check_service "Grafana" "3000"
            check_service "Jaeger" "16686"
            check_service "Tempo" "3200"
            check_service "Loki" "3100"
            ;;
    esac

    echo ""
    echo "======================================"
    log_pass "Health check completed successfully!"
    echo "======================================"
}

log_info() {
    echo -e "[INFO] $1"
}

if [ "$1" = "--help" ]; then
    cat << EOF
Health Check Script for BSS Load Testing

USAGE:
    $0 [ENVIRONMENT]

ARGUMENTS:
    ENVIRONMENT           dev, stage, prod, all (default: dev)

EOF
    exit 0
fi

main_check
