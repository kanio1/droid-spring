#!/bin/bash
################################################################################
# Proxmox VM Creation Script for BSS Load Testing Environment
# Creates VMs based on vm-inventory.csv configuration
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INVENTORY_FILE="${SCRIPT_DIR}/../configs/vm-inventory.csv"
PROXMOX_HOST="${PROXMOX_HOST:-pve.lab.local}"
PROXMOX_USER="${PROXMOX_USER:-root@pam}"
PROXMOX_PASSWORD="${PROXMOX_PASSWORD:-}"
PROXMOX_NODE="${PROXMOX_NODE:-pve-node1}"
VM_TEMPLATE_ID="${VM_TEMPLATE_ID:-9000}"
DRY_RUN="${DRY_RUN:-false}"

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# Show help
show_help() {
    cat << EOF
Proxmox VM Creation Script for BSS

USAGE:
    $0 [OPTIONS]

OPTIONS:
    --env ENV              Environment: dev, stage, prod (default: all)
    --node NODE            Proxmox node (default: pve-node1)
    --template-id ID       Base template VM ID (default: 9000)
    --dry-run              Show what would be created without executing
    --help                 Show this help

ENVIRONMENT VARIABLES:
    PROXMOX_HOST           Proxmox host (default: pve.lab.local)
    PROXMOX_USER           Proxmox user (default: root@pam)
    PROXMOX_PASSWORD       Proxmox password (required)

EXAMPLES:
    # Create all dev environment VMs
    $0 --env dev

    # Create only production VMs on specific node
    $0 --env prod --node pve-node2

    # Dry run to see what would be created
    $0 --dry-run

ENVIRONMENTS:
    dev      - Development environment (1K events) - VMs 200-234
    stage    - Staging environment (10K events) - VMs 300-334
    prod     - Production environment (100K-1M events) - VMs 400-434
    all      - All environments (default)

EOF
}

# Get API token
get_api_token() {
    log_info "Getting API authentication token..."

    if [ -z "$PROXMOX_PASSWORD" ]; then
        log_error "PROXMOX_PASSWORD environment variable is required!"
        exit 1
    fi

    # Get ticket and CSRF token
    TICKET_RESPONSE=$(curl -s -k \
        -H "Authorization: Basic $(echo -n "$PROXMOX_USER:$PROXMOX_PASSWORD" | base64)" \
        "https://$PROXMOX_HOST:8006/api2/json/access/ticket")

    TICKET=$(echo "$TICKET_RESPONSE" | jq -r '.data.ticket' 2>/dev/null)
    CSRF=$(echo "$TICKET_RESPONSE" | jq -r '.data.CSRFPreventionToken' 2>/dev/null)

    if [ -z "$TICKET" ] || [ "$TICKET" = "null" ]; then
        log_error "Failed to authenticate with Proxmox API"
        exit 1
    fi

    log_info "Authentication successful"
}

# Make API call
api_call() {
    local method="$1"
    local path="$2"
    local data="$3"

    local url="https://$PROXMOX_HOST:8006/api2/json$path"
    local headers=(
        -H "Cookie: PVEAuthCookie=$TICKET"
        -H "CSRFPreventionToken: $CSRF"
        -H "Content-Type: application/json"
    )

    if [ -n "$data" ]; then
        headers+=(-d "$data")
    fi

    curl -s -k -X "$method" "${headers[@]}" "$url"
}

# Create VM
create_vm() {
    local vm_id="$1"
    local vm_name="$2"
    local node="$3"
    local vcpu="$4"
    local ram_gb="$5"
    local disk_gb="$6"
    local bridge="$7"
    local role="$8"

    log_info "Creating VM: $vm_name (ID: $vm_id) on node: $node"

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "[DRY RUN] Would create VM $vm_id ($vm_name)"
        return 0
    fi

    # Clone from template
    log_info "Cloning from template $VM_TEMPLATE_ID..."
    local clone_response=$(api_call "POST" "/nodes/$node/qemu/$VM_TEMPLATE_ID/clone" "{
        \"name\": \"$vm_name\",
        \"newid\": $vm_id,
        \"full\": 1
    }")

    if echo "$clone_response" | jq -e '.data' > /dev/null 2>&1; then
        log_info "VM cloned successfully"
    else
        log_error "Failed to clone VM"
        echo "$clone_response"
        return 1
    fi

    # Wait for VM to be ready
    sleep 5

    # Update VM configuration
    log_info "Updating VM configuration..."
    local vm_config=$(cat <<EOF
{
    "cores": $vcpu,
    "memory": $(($ram_gb * 1024)),
    "net0": "virtio,bridge=$bridge",
    "onboot": 1,
    "protection": 0,
    "description": "BSS $role - Created $(date)"
}
EOF
)

    api_call "PUT" "/nodes/$node/qemu/$vm_id/config" "$vm_config" > /dev/null

    # Resize disk if needed
    if [ "$disk_gb" -gt 32 ]; then
        log_info "Resizing disk to ${disk_gb}GB..."
        api_call "PUT" "/nodes/$node/qemu/$vm_id/resize" "disk=scsi0,size=${disk_gb}G" > /dev/null
    fi

    log_info "VM $vm_name created successfully"
}

# Process inventory file
process_inventory() {
    local env_filter="$1"

    log_info "Processing VM inventory from: $INVENTORY_FILE"

    if [ ! -f "$INVENTORY_FILE" ]; then
        log_error "Inventory file not found: $INVENTORY_FILE"
        exit 1
    fi

    # Read CSV, skip comments and header
    local count=0
    while IFS=',' read -r vm_id vm_name node template vcpu ram_gb disk_gb network bridge notes role; do
        # Skip empty lines, comments, and header
        [ -z "$vm_id" ] || [[ "$vm_id" =~ ^# ]] && continue

        # Filter by environment if specified
        if [ -n "$env_filter" ] && [ "$env_filter" != "all" ]; then
            if [[ "$vm_id" =~ ^$env_filter ]]; then
                create_vm "$vm_id" "$vm_name" "$node" "$vcpu" "$ram_gb" "$disk_gb" "$bridge" "$role"
                ((count++))
            else
                log_debug "Skipping $vm_name (doesn't match filter: $env_filter)"
            fi
        else
            create_vm "$vm_id" "$vm_name" "$node" "$vcpu" "$ram_gb" "$disk_gb" "$bridge" "$role"
            ((count++))
        fi
    done < <(grep -v '^#' "$INVENTORY_FILE" | tail -n +2)

    log_info "Processed $count VMs"
}

# Main execution
main() {
    local env_filter="all"
    local node_filter="$PROXMOX_NODE"

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --env)
                env_filter="$2"
                shift 2
                ;;
            --node)
                node_filter="$2"
                shift 2
                ;;
            --template-id)
                VM_TEMPLATE_ID="$2"
                shift 2
                ;;
            --dry-run)
                DRY_RUN="true"
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done

    log_info "Starting VM creation process..."
    log_info "Environment filter: $env_filter"
    log_info "Node filter: $node_filter"
    log_info "Template ID: $VM_TEMPLATE_ID"

    if [ "$DRY_RUN" = "true" ]; then
        log_warn "DRY RUN MODE - No changes will be made"
    fi

    # Get authentication
    get_api_token

    # Process inventory
    process_inventory "$env_filter"

    log_info "VM creation process completed!"
    log_info "Next steps:"
    log_info "  1. Start VMs: ./start-vms.sh --env $env_filter"
    log_info "  2. Configure IPs: ./configure-network.sh"
    log_info "  3. Deploy services: ./deploy-services.sh"
}

# Run main function
main "$@"
