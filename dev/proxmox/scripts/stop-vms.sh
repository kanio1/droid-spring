#!/bin/bash
################################################################################
# Stop VMs Script for BSS Load Testing Environment
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

# Stop VM
stop_vm() {
    local vm_id="$1"
    local vm_name="$2"
    local node="$3"

    log_info "Stopping VM: $vm_name (ID: $vm_id) on node: $node"

    # Stop VM
    curl -s -k -X "POST" \
        -H "Cookie: PVEAuthCookie=$TICKET" \
        -H "CSRFPreventionToken: $CSRF" \
        "https://$PROXMOX_HOST:8006/api2/json/nodes/$node/qemu/$vm_id/status/stop"

    log_info "VM $vm_name stopped successfully"
}

# Process VMs
process_vms() {
    log_info "Stopping VMs for environment: $ENV_FILTER"

    # Read CSV and stop VMs
    local count=0
    while IFS=',' read -r vm_id vm_name node template vcpu ram_gb disk_gb network bridge notes role; do
        [ -z "$vm_id" ] || [[ "$vm_id" =~ ^# ]] && continue

        # Filter by environment
        if [ "$ENV_FILTER" != "all" ]; then
            if [[ "$vm_id" =~ ^$ENV_FILTER ]]; then
                stop_vm "$vm_id" "$vm_name" "$node"
                ((count++))
            fi
        else
            stop_vm "$vm_id" "$vm_name" "$node"
            ((count++))
        fi
    done < <(grep -v '^#' "$INVENTORY_FILE" | tail -n +2)

    log_info "Stopped $count VMs"
}

# Main
if [ -z "$PROXMOX_PASSWORD" ]; then
    echo "Error: PROXMOX_PASSWORD environment variable is required!"
    exit 1
fi

get_api_token
process_vms

log_info "All VMs stopped!"
