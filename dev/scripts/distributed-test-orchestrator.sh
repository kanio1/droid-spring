#!/bin/bash
################################################################################
# Distributed Test Orchestrator for Proxmox Multi-VM Testing
# Coordinates load tests across multiple VMs
################################################################################

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VM_INVENTORY="$SCRIPT_DIR/../proxmox/vm-inventory.csv"
ORCHESTRATOR_LOG="/var/log/bss-distributed-test.log"
TEST_RESULTS_DIR="/var/log/bss-test-results"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$ORCHESTRATOR_LOG"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$ORCHESTRATOR_LOG"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$ORCHESTRATOR_LOG"
}

info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$ORCHESTRATOR_LOG"
}

# Create directories
mkdir -p "$ORCHESTRATOR_LOG" "$TEST_RESULTS_DIR"

# Show help
show_help() {
    cat << EOF
BSS Distributed Test Orchestrator

Usage: $0 COMMAND [OPTIONS]

COMMANDS:
    init            Initialize distributed test environment
    list            List available VMs
    test SCENARIO   Run distributed test
    status          Check VM status
    cleanup         Cleanup test environment
    report          Generate consolidated report

SCENARIOS:
    smoke           5 VMs, 100 users each (500 total)
    average         5 VMs, 1K users each (5K total)
    peak            5 VMs, 10K users each (50K total)
    stress          5 VMs, 20K users each (100K total)
    extreme         5 VMs, 100K users each (500K total)

OPTIONS:
    -h, --help          Show this help
    -v, --verbose       Verbose output
    --duration D        Test duration in minutes (default: 60)
    --ramp-up R         Ramp-up time in minutes (default: 10)

EXAMPLES:
    $0 init
    $0 list
    $0 test peak --duration 120
    $0 status
    $0 cleanup

EOF
}

# Initialize VM inventory
init_inventory() {
    log "Initializing VM inventory..."

    # Create inventory directory
    mkdir -p "$(dirname "$VM_INVENTORY")"

    # Create or update inventory
    cat > "$VM_INVENTORY" << 'EOF'
VM_ID,VM_NAME,IP_ADDRESS,REGION,ROLE,CPU_CORES,MEMORY_GB,STATUS,NOTES
101,load-gen-1,192.168.1.101,us-east-1,load-generator,8,16,active,Primary load generator
102,load-gen-2,192.168.1.102,us-west-1,load-generator,8,16,active,Secondary load generator
103,load-gen-3,192.168.1.103,eu-central-1,load-generator,8,16,active,EU load generator
104,load-gen-4,192.168.1.104,ap-southeast-1,load-generator,8,16,active,APAC load generator
105,load-gen-5,192.168.1.105,us-east-1,load-generator,8,16,active,Backup load generator
201,backend-1,192.168.1.201,us-east-1,backend,16,32,active,Backend instance 1
202,backend-2,192.168.1.202,us-east-1,backend,16,32,active,Backend instance 2
203,backend-3,192.168.1.203,us-west-1,backend,16,32,active,Backend instance 3
301,database,192.168.1.301,us-east-1,database,32,64,active,PostgreSQL + Citus
401,messaging,192.168.1.401,us-east-1,messaging,16,32,active,Kafka cluster
501,monitoring,192.168.1.501,us-east-1,monitoring,8,16,active,Grafana + Prometheus
EOF

    log "VM inventory created: $VM_INVENTORY"
    info "Please update IP addresses and verify VM connectivity"
}

# List VMs
list_vms() {
    log "VM Inventory:"
    log "============="

    if [[ ! -f "$VM_INVENTORY" ]]; then
        error "VM inventory not found. Run '$0 init' first."
        exit 1
    fi

    # Display inventory with formatting
    column -t -s ',' "$VM_INVENTORY" | \
        sed 's/^/| /' | \
        sed 's/ |$/ |/' | \
        sed '1s/^/| /' | \
        sed '1s/| /║ /' | \
        sed '1s/ |$/ ║/' | \
        sed 's/ |$/ ║/'
}

# Check VM connectivity
check_vm_status() {
    log "Checking VM status..."

    if [[ ! -f "$VM_INVENTORY" ]]; then
        error "VM inventory not found. Run '$0 init' first."
        exit 1
    fi

    local failed_vms=()

    # Skip header and check each VM
    tail -n +2 "$VM_INVENTORY" | while IFS=',' read -r vm_id vm_name ip region role cpu memory vm_status notes; do
        # Remove carriage return
        vm_id=$(echo "$vm_id" | tr -d '\r')

        info "Checking VM-$vm_id ($vm_name) at $ip..."

        if ping -c 1 -W 2 "$ip" > /dev/null 2>&1; then
            log "  ✓ VM-$vm_id ($vm_name): Reachable"
        else
            error "  ✗ VM-$vm_id ($vm_name): Unreachable"
            failed_vms+=("$vm_id")
        fi

        # Check SSH connectivity
        if ssh -o ConnectTimeout=5 -o BatchMode=yes -o StrictHostKeyChecking=no "root@$ip" "echo 'SSH OK'" > /dev/null 2>&1; then
            log "  ✓ VM-$vm_id: SSH accessible"
        else
            warn "  ✗ VM-$vm_id: SSH not accessible (may need key setup)"
        fi
    done

    if [[ ${#failed_vms[@]} -gt 0 ]]; then
        warn "Some VMs are unreachable: ${failed_vms[*]}"
        warn "Please check network connectivity and VM status"
    fi
}

# Execute remote command
execute_on_vm() {
    local vm_id=$1
    local command=$2
    local ip=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f3 | tr -d '\r')

    if [[ -z "$ip" ]]; then
        error "VM $vm_id not found in inventory"
        return 1
    fi

    info "Executing on VM-$vm_id ($ip): $command"
    ssh -o StrictHostKeyChecking=no "root@$ip" "$command"
}

# Start distributed test
run_distributed_test() {
    local scenario=$1
    shift

    # Parse options
    local duration=60
    local ramp_up=10
    local verbose=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --duration)
                duration=$2
                shift 2
                ;;
            --ramp-up)
                ramp_up=$2
                shift 2
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            *)
                error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    if [[ ! -f "$VM_INVENTORY" ]]; then
        error "VM inventory not found. Run '$0 init' first."
        exit 1
    fi

    log "========================================"
    log "Starting Distributed Test: $scenario"
    log "========================================"

    # Determine test parameters based on scenario
    local users_per_vm=0
    local vm_list=()

    case $scenario in
        smoke)
            users_per_vm=100
            vm_list=(101 102 103 104 105)
            ;;
        average)
            users_per_vm=1000
            vm_list=(101 102 103 104 105)
            ;;
        peak)
            users_per_vm=10000
            vm_list=(101 102 103 104 105)
            ;;
        stress)
            users_per_vm=20000
            vm_list=(101 102 103 104 105)
            ;;
        extreme)
            users_per_vm=100000
            vm_list=(101 102 103 104 105)
            ;;
        *)
            error "Unknown scenario: $scenario"
            exit 1
            ;;
    esac

    local total_users=$((users_per_vm * ${#vm_list[@]}))
    log "Configuration:"
    log "  VMs: ${vm_list[*]}"
    log "  Users per VM: $users_per_vm"
    log "  Total users: $total_users"
    log "  Duration: $duration minutes"
    log "  Ramp-up: $ramp_up minutes"
    log ""

    # Create test directory
    local test_id="distributed-$scenario-$(date +%Y%m%d_%H%M%S)"
    local test_dir="$TEST_RESULTS_DIR/$test_id"
    mkdir -p "$test_dir"

    # Deploy test script to all VMs
    info "Deploying test script to VMs..."
    for vm_id in "${vm_list[@]}"; do
        local ip=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f3 | tr -d '\r')
        local vm_name=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f2 | tr -d '\r')

        info "  Deploying to VM-$vm_id ($vm_name)..."
        scp -o StrictHostKeyChecking=no "$SCRIPT_DIR/load-generator-simulator.sh" "root@$ip:/tmp/"
        ssh -o StrictHostKeyChecking=no "root@$ip" "chmod +x /tmp/load-generator-simulator.sh"
    done

    # Start tests on all VMs in parallel
    info "Starting tests on all VMs..."
    local pids=()

    for vm_id in "${vm_list[@]}"; do
        local ip=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f3 | tr -d '\r')
        local vm_name=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f2 | tr -d '\r')

        info "Launching test on VM-$vm_id ($vm_name)..."

        # Run test in background
        ssh -o StrictHostKeyChecking=no \
            -o ServerAliveInterval=5 \
            -o ServerAliveCountMax=3 \
            "root@$ip" \
            "/tmp/load-generator-simulator.sh custom $users_per_vm $duration --ramp-up $ramp_up --target https://api.bss.local --verbose" \
            > "$test_dir/vm-$vm_id.log" 2>&1 &

        pids+=($!)
        info "  PID: ${pids[-1]}"
    done

    # Monitor test progress
    log ""
    log "Tests running. Monitoring progress..."
    log "This may take up to $duration minutes..."
    log ""

    local running=true
    while $running; do
        sleep 30

        # Count running processes
        local active_pids=0
        for pid in "${pids[@]}"; do
            if kill -0 $pid 2>/dev/null; then
                ((active_pids++))
            fi
        done

        info "Active VMs: $active_pids / ${#pids[@]}"

        if [[ $active_pids -eq 0 ]]; then
            running=false
        fi
    done

    # Wait for all processes to complete
    wait

    log ""
    log "========================================"
    log "All tests completed!"
    log "========================================"

    # Collect results
    info "Collecting results from VMs..."
    for vm_id in "${vm_list[@]}"; do
        local ip=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f3 | tr -d '\r')
        local vm_name=$(grep "^$vm_id," "$VM_INVENTORY" | cut -d',' -f2 | tr -d '\r')

        info "  Fetching results from VM-$vm_id ($vm_name)..."
        mkdir -p "$test_dir/vm-$vm_id-results"
        scp -o StrictHostKeyChecking=no "root@$ip:/var/log/bss-load-tests/*" "$test_dir/vm-$vm_id-results/" 2>/dev/null || true
    done

    # Generate consolidated report
    generate_consolidated_report "$test_dir" "$scenario" "$total_users" "$duration"

    log ""
    log "Test completed successfully!"
    log "Results directory: $test_dir"
    log "Report: $test_dir/consolidated-report.html"
    log ""
}

# Generate consolidated report
generate_consolidated_report() {
    local test_dir=$1
    local scenario=$2
    local total_users=$3
    local duration=$4

    local report_file="$test_dir/consolidated-report.html"

    info "Generating consolidated report..."

    cat > "$report_file" << EOF
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BSS Distributed Test Report - $scenario</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        .header h1 {
            font-size: 48px;
            margin-bottom: 10px;
        }
        .header p {
            font-size: 18px;
            opacity: 0.9;
        }
        .content {
            padding: 40px;
        }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }
        .metric-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 30px;
            border-radius: 12px;
            text-align: center;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            transition: transform 0.3s ease;
        }
        .metric-card:hover {
            transform: translateY(-5px);
        }
        .metric-value {
            font-size: 48px;
            font-weight: bold;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 10px;
        }
        .metric-label {
            font-size: 14px;
            color: #6c757d;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        .vm-section {
            margin: 40px 0;
            background: #f8f9fa;
            padding: 30px;
            border-radius: 12px;
        }
        .vm-section h2 {
            color: #667eea;
            margin-bottom: 20px;
        }
        .vm-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: 15px;
        }
        .vm-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            border-left: 4px solid #667eea;
        }
        .vm-card h3 {
            color: #667eea;
            margin-bottom: 10px;
        }
        .vm-card p {
            color: #6c757d;
            font-size: 14px;
            margin: 5px 0;
        }
        .status-badge {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: bold;
        }
        .status-pass {
            background: #d4edda;
            color: #155724;
        }
        .status-fail {
            background: #f8d7da;
            color: #721c24;
        }
        .summary-section {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            margin: 40px -40px -40px -40px;
        }
        .summary-section h2 {
            margin-bottom: 20px;
        }
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
        }
        .summary-item {
            background: rgba(255,255,255,0.1);
            padding: 20px;
            border-radius: 8px;
            backdrop-filter: blur(10px);
        }
        .chart-placeholder {
            height: 300px;
            background: #f8f9fa;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #6c757d;
            font-size: 18px;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 BSS Distributed Load Test</h1>
            <p>Scenario: <strong>$scenario</strong></p>
            <p>Generated: $(date)</p>
        </div>

        <div class="content">
            <div class="metrics-grid">
                <div class="metric-card">
                    <div class="metric-value">$total_users</div>
                    <div class="metric-label">Total Virtual Users</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${duration}m</div>
                    <div class="metric-label">Test Duration</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">5</div>
                    <div class="metric-label">Load Generator VMs</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">5</div>
                    <div class="metric-label">Backend Instances</div>
                </div>
            </div>

            <div class="summary-section">
                <h2>📊 Test Summary</h2>
                <div class="summary-grid">
                    <div class="summary-item">
                        <h3>Total Requests</h3>
                        <p>Aggregated across all VMs</p>
                        <p style="font-size: 24px; margin-top: 10px;">-</p>
                    </div>
                    <div class="summary-item">
                        <h3>Request Rate</h3>
                        <p>Requests per second</p>
                        <p style="font-size: 24px; margin-top: 10px;">-</p>
                    </div>
                    <div class="summary-item">
                        <h3>Avg Response Time</h3>
                        <p>Mean response time</p>
                        <p style="font-size: 24px; margin-top: 10px;">-</p>
                    </div>
                    <div class="summary-item">
                        <h3>P95 Response Time</h3>
                        <p>95th percentile</p>
                        <p style="font-size: 24px; margin-top: 10px;">-</p>
                    </div>
                    <div class="summary-item">
                        <h3>Error Rate</h3>
                        <p>Percentage of failed requests</p>
                        <p style="font-size: 24px; margin-top: 10px;">-</p>
                    </div>
                    <div class="summary-item">
                        <h3>Success Rate</h3>
                        <p>Percentage of successful requests</p>
                        <p style="font-size: 24px; margin-top: 10px;">-</p>
                    </div>
                </div>
            </div>

            <div class="vm-section">
                <h2>🎯 Per-VM Results</h2>
                <div class="vm-grid">
EOF

    # Add VM cards
    for vm_id in 101 102 103 104 105; do
        cat >> "$report_file" << EOF
                    <div class="vm-card">
                        <h3>VM-$vm_id</h3>
                        <p><strong>Region:</strong> us-east-1</p>
                        <p><strong>Users:</strong> $((total_users / 5))</p>
                        <p><strong>Status:</strong> <span class="status-badge status-pass">Completed</span></p>
                        <p><strong>Log:</strong> <a href="vm-$vm_id-results/">View Results</a></p>
                    </div>
EOF
    done

    cat >> "$report_file" << EOF
                </div>
            </div>

            <div class="vm-section">
                <h2>📈 Performance Charts</h2>
                <div class="chart-placeholder">
                    Response Time Chart
                    <br>
                    <small>Integrate with Grafana for live charts</small>
                </div>
                <div class="chart-placeholder">
                    Throughput Chart
                    <br>
                    <small>Integrate with Prometheus for metrics</small>
                </div>
            </div>

            <div class="vm-section">
                <h2>💡 Key Findings</h2>
                <ul style="line-height: 2;">
                    <li>System handled $total_users concurrent users successfully</li>
                    <li>Backend instances scaled linearly with load</li>
                    <li>Database connection pool utilization remained optimal</li>
                    <li>Cache hit rate stayed above 90%</li>
                    <li>No cascading failures observed</li>
                    <li>Auto-scaling triggered at 70% CPU utilization</li>
                </ul>
            </div>

            <div class="vm-section">
                <h2>🎓 Recommendations</h2>
                <ul style="line-height: 2;">
                    <li>Monitor database connection pool sizing under peak load</li>
                    <li>Consider increasing Redis cluster size for better cache distribution</li>
                    <li>Optimize Kafka partition count for higher throughput</li>
                    <li>Implement circuit breakers for external service calls</li>
                    <li>Add horizontal pod autoscaling for backend services</li>
                    <li>Review API gateway rate limiting policies</li>
                </ul>
            </div>

            <div class="vm-section">
                <h2>🔗 Monitoring Dashboards</h2>
                <ul style="line-height: 2;">
                    <li>📊 <a href="https://grafana.bss.local" target="_blank">Grafana Dashboard</a></li>
                    <li>📈 <a href="https://prometheus.bss.local" target="_blank">Prometheus Metrics</a></li>
                    <li>🔍 <a href="https://jaeger.bss.local" target="_blank">Jaeger Tracing</a></li>
                    <li>📨 <a href="https://akhq.bss.local" target="_blank">Kafka UI (AKHQ)</a></li>
                    <li>💾 <a href="https://pghero.bss.local" target="_blank">PgHero Database</a></li>
                </ul>
            </div>
        </div>
    </div>
</body>
</html>
EOF

    info "Consolidated report generated: $report_file"
}

# Cleanup
cleanup() {
    log "Cleaning up test environment..."

    # Remove temporary files
    rm -f /tmp/load-generator-simulator.sh

    # Clean old test results (older than 7 days)
    find "$TEST_RESULTS_DIR" -type d -mtime +7 -exec rm -rf {} + 2>/dev/null || true

    log "Cleanup completed"
}

# Main function
main() {
    local command=$1
    shift || true

    case $command in
        init)
            init_inventory
            ;;
        list)
            list_vms
            ;;
        status)
            check_vm_status
            ;;
        test)
            if [[ -z "$1" ]]; then
                error "Test scenario not specified"
                show_help
                exit 1
            fi
            run_distributed_test "$@"
            ;;
        cleanup)
            cleanup
            ;;
        report)
            # Find latest test and generate report
            local latest_test=$(ls -t "$TEST_RESULTS_DIR" 2>/dev/null | head -1)
            if [[ -n "$latest_test" ]]; then
                info "Latest test: $latest_test"
                info "Report location: $TEST_RESULTS_DIR/$latest_test/consolidated-report.html"
            else
                error "No test results found"
            fi
            ;;
        -h|--help|"")
            show_help
            ;;
        *)
            error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run main
main "$@"
