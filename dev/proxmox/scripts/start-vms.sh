#!/bin/bash
################################################################################
# Start VMs Script for BSS Load Testing Environment
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INVENTORY_FILE="${SCRIPT_DIR}/../configs/vm-inventory.csv"
PROXMOX_HOST="${PROXMOX_HOST:-pve.lab.local}"
PROXMOX_USER="${PROXMOX_USER:-root@pam}"
PROXMOX_PASSWORD="${PROXMOX_PASSWORD:-}"
ENV_FILTER="${1:-all}"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Get API token
get_api_token() {
    TICKET_RESPONSE=$(curl -s -k \
        -H "Authorization: Basic $(echo -n "$PROXMOX_USER:$PROXMOX_PASSWORD" | base64)" \
        "https://$PROXMOX_HOST:8006/api2/json/access/ticket")

    TICKET=$(echo "$TICKET_RESPONSE" | jq -r '.data.ticket' 2>/dev/null)
    CSRF=$(echo "$TICKET_RESPONSE" | jq -r '.data.CSRFPreventionToken' 2>/dev/null)
}

# Start VM
start_vm() {
    local vm_id="$1"
    local vm_name="$2"
    local node="$3"

    log_info "Starting VM: $vm_name (ID: $vm_id) on node: $node"

    # Check VM status
    local status=$(curl -s -k \
        -H "Cookie: PVEAuthCookie=$TICKET" \
        -H "CSRFPreventionToken: $CSRF" \
        "https://$PROXMOX_HOST:8006/api2/json/nodes/$node/qemu/$vm_id/status/current" | \
        jq -r '.data.status' 2>/dev/null)

    if [ "$status" = "running" ]; then
        log_warn "VM $vm_name is already running"
        return 0
    fi

    # Start VM
    curl -s -k -X "POST" \
        -H "Cookie: PVEAuthCookie=$TICKET" \
        -H "CSRFPreventionToken: $CSRF" \
        "https://$PROXMOX_HOST:8006/api2/json/nodes/$node/qemu/$vm_id/status/start"

    log_info "VM $vm_name started successfully"
}

# Process VMs
process_vms() {
    log_info "Starting VMs for environment: $ENV_FILTER"

    # Read CSV and start VMs
    local count=0
    while IFS=',' read -r vm_id vm_name node template vcpu ram_gb disk_gb network bridge notes role; do
        [ -z "$vm_id" ] || [[ "$vm_id" =~ ^# ]] && continue

        # Filter by environment
        if [ "$ENV_FILTER" != "all" ]; then
            if [[ "$vm_id" =~ ^$ENV_FILTER ]]; then
                start_vm "$vm_id" "$vm_name" "$node"
                ((count++))
            fi
        else
            start_vm "$vm_id" "$vm_name" "$node"
            ((count++))
        fi
    done < <(grep -v '^#' "$INVENTORY_FILE" | tail -n +2)

    log_info "Started $count VMs"
}

# Main
if [ -z "$PROXMOX_PASSWORD" ]; then
    echo "Error: PROXMOX_PASSWORD environment variable is required!"
    exit 1
fi

get_api_token
process_vms

log_info "All VMs started!"
