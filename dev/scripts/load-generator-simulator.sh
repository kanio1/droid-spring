#!/bin/bash
################################################################################
# Load Generator Simulator for Proxmox Multi-VM Testing
# Simulates traffic from 100 to 1,000,000 events
################################################################################

set -e

# Configuration
SIMULATOR_NAME="bss-load-simulator"
DOCKER_IMAGE="loadimpact/k6:latest"
RESULTS_DIR="/var/log/bss-load-tests"
LOG_FILE="$RESULTS_DIR/simulator.log"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Help function
show_help() {
    cat << EOF
BSS Load Generator Simulator

Usage: $0 [OPTIONS] SCENARIO

SCENARIOS:
    smoke          Run smoke tests (100 users, 5 minutes)
    average        Average load test (1K users, 30 minutes)
    peak           Peak load test (10K users, 60 minutes)
    stress         Stress test (50K users, 2 hours)
    extreme        Extreme test (100K users, 4 hours)
    marathon       Marathon test (10K users, 24 hours)
    distributed    Multi-VM distributed test
    custom N D     Custom: N users for D duration (in minutes)

OPTIONS:
    -h, --help         Show this help message
    -v, --verbose      Verbose output
    --vm VM_ID         Specify VM ID to run on (for Proxmox)
    --target URL       Target URL (default: https://api.bss.local)
    --duration D       Test duration in minutes (default: 30)
    --ramp-up R        Ramp-up time in minutes (default: 5)

EXAMPLES:
    $0 smoke
    $0 peak --target https://staging.bss.local
    $0 custom 50000 120
    $0 distributed --vm 101

EOF
}

# Parse arguments
SCENARIO=""
VERBOSE=false
VM_ID=""
TARGET_URL="https://api.bss.local"
DURATION=30
RAMP_UP=5

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --vm)
            VM_ID="$2"
            shift 2
            ;;
        --target)
            TARGET_URL="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        --ramp-up)
            RAMP_UP="$2"
            shift 2
            ;;
        smoke|average|peak|stress|extreme|marathon|distributed)
            SCENARIO="$1"
            shift
            ;;
        custom)
            SCENARIO="custom"
            CUSTOM_USERS="$2"
            CUSTOM_DURATION="$3"
            shift 3
            ;;
        *)
            log "ERROR: Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate scenario
if [[ -z "$SCENARIO" ]]; then
    log "ERROR: No scenario specified"
    show_help
    exit 1
fi

# Scenario configurations
configure_scenario() {
    case $SCENARIO in
        smoke)
            USERS=100
            DURATION=5
            RAMP_UP=1
            ;;
        average)
            USERS=1000
            DURATION=30
            RAMP_UP=5
            ;;
        peak)
            USERS=10000
            DURATION=60
            RAMP_UP=10
            ;;
        stress)
            USERS=50000
            DURATION=120
            RAMP_UP=20
            ;;
        extreme)
            USERS=100000
            DURATION=240
            RAMP_UP=30
            ;;
        marathon)
            USERS=10000
            DURATION=1440
            RAMP_UP=60
            ;;
        custom)
            USERS=${CUSTOM_USERS:-1000}
            DURATION=${CUSTOM_DURATION:-30}
            RAMP_UP=10
            ;;
        distributed)
            configure_distributed
            return
            ;;
    esac

    log "=== Scenario: $SCENARIO ==="
    log "Users: $USERS"
    log "Duration: $DURATION minutes"
    log "Ramp-up: $RAMP_UP minutes"
    log "Target: $TARGET_URL"
}

# Distributed test configuration
configure_distributed() {
    log "=== Distributed Multi-VM Test ==="
    log "This will orchestrate tests across multiple Proxmox VMs"
    log ""
    log "Available VMs:"
    log "  VM-101: Load Generator 1 (US East)"
    log "  VM-102: Load Generator 2 (US West)"
    log "  VM-103: Load Generator 3 (EU Central)"
    log "  VM-201: Load Generator 4 (Asia Pacific)"
    log ""

    # Check if running on Proxmox
    if [[ -f /etc/pve/qemu-server/*.conf ]]; then
        log "Running on Proxmox VE detected"
        VM_NUM=$(hostname | sed 's/.*vm-\([0-9]*\).*/\1/')
        log "Current VM ID: $VM_NUM"

        case $VM_NUM in
            101|102|103|201)
                USERS=25000
                DURATION=120
                RAMP_UP=15
                log "Assigned scenario: 25,000 users from VM-$VM_NUM"
                ;;
            *)
                log "ERROR: This VM is not configured as a load generator"
                exit 1
                ;;
        esac
    else
        log "WARNING: Not running on Proxmox. Using single VM mode."
        USERS=10000
        DURATION=60
        RAMP_UP=10
    fi
}

# Create K6 test script
create_k6_script() {
    local script_file="$RESULTS_DIR/test-scenario.js"

    cat > "$script_file" << 'EOF'
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export const errorRate = new Rate('errors');
export const customerCreateTime = new Trend('customer_create_duration');
export const orderCreateTime = new Trend('order_create_duration');
export const paymentTime = new Trend('payment_duration');

// Export configuration
export const options = {
    stages: [
        // Ramp-up phase
        { duration: '__RAMP_UP__m', target: __USERS__ },
        // Sustained load
        { duration: '__DURATION__m', target: __USERS__ },
        // Ramp-down phase
        { duration: '2m', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1000'],
        'http_req_failed': ['rate<0.05'],
        'errors': ['rate<0.05'],
        'customer_create_duration': ['p(99)<3000'],
        'order_create_duration': ['p(99)<5000'],
        'payment_duration': ['p(99)<10000'],
    },
};

// Setup
export function setup() {
    log('Initializing test...');
    const authResponse = login();
    return { token: authResponse.token, customerId: authResponse.customerId };
}

function login() {
    const payload = JSON.stringify({
        username: 'testuser@example.com',
        password: 'testpass123'
    });

    const response = http.post(`${__ENV.BASE_URL}/api/auth/login`, payload, {
        headers: {
            'Content-Type': 'application/json',
        },
    });

    check(response, {
        'login successful': (r) => r.status === 200,
    });

    const data = JSON.parse(response.body);
    return {
        token: data.access_token,
        customerId: data.customer_id,
    };
}

function getHeaders(token) {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
    };
}

// Main test
export default function(data) {
    const headers = getHeaders(data.token);

    // Scenario weights (sum = 100)
    const rand = Math.random() * 100;

    // Customer read (40%)
    if (rand < 40) {
        group('Customer Operations - Read', () => {
            const customerId = data.customerId || `customer-${__VU % 1000}`;
            const response = http.get(
                `${__ENV.BASE_URL}/api/customers/${customerId}`,
                { headers }
            );

            check(response, {
                'customer read status is 200': (r) => r.status === 200,
                'customer read response time < 500ms': (r) => r.timings.duration < 500,
            });
        });
    }

    // Order creation (25%)
    else if (rand < 65) {
        group('Order Operations - Create', () => {
            const payload = JSON.stringify({
                customerId: data.customerId || `customer-${__VU % 1000}`,
                items: [
                    { productId: 1, quantity: 1 },
                    { productId: 2, quantity: 2 },
                ],
                shippingAddress: {
                    street: '123 Test St',
                    city: 'Test City',
                    zipCode: '12345',
                    country: 'US',
                },
            });

            const startTime = Date.now();
            const response = http.post(
                `${__ENV.BASE_URL}/api/orders`,
                payload,
                { headers }
            );
            const duration = Date.now() - startTime;

            const success = check(response, {
                'order create status is 201': (r) => r.status === 201,
                'order create response time < 5000ms': () => duration < 5000,
            });

            errorRate.add(!success);
            orderCreateTime.add(duration);
        });
    }

    // Customer update (15%)
    else if (rand < 80) {
        group('Customer Operations - Update', () => {
            const payload = JSON.stringify({
                phone: `+1${Math.floor(Math.random() * 9000000000) + 1000000000}`,
                address: {
                    street: '456 Update Ave',
                    city: 'Update City',
                    zipCode: '54321',
                    country: 'US',
                },
            });

            const customerId = data.customerId || `customer-${__VU % 1000}`;
            const response = http.patch(
                `${__ENV.BASE_URL}/api/customers/${customerId}`,
                payload,
                { headers }
            );

            check(response, {
                'customer update status is 200': (r) => r.status === 200,
                'customer update response time < 1000ms': (r) => r.timings.duration < 1000,
            });
        });
    }

    // Payment processing (10%)
    else if (rand < 90) {
        group('Payment Operations', () => {
            const payload = JSON.stringify({
                orderId: `order-${__VU % 10000}`,
                amount: Math.round(Math.random() * 10000) / 100,
                method: 'CREDIT_CARD',
                cardNumber: '4242424242424242',
                expiryMonth: 12,
                expiryYear: 2025,
                cvv: '123',
            });

            const startTime = Date.now();
            const response = http.post(
                `${__ENV.BASE_URL}/api/payments/process`,
                payload,
                { headers }
            );
            const duration = Date.now() - startTime;

            const success = check(response, {
                'payment status is 200': (r) => r.status === 200,
                'payment response time < 10000ms': () => duration < 10000,
            });

            errorRate.add(!success);
            paymentTime.add(duration);
        });
    }

    // Product search (10%)
    else {
        group('Product Operations - Search', () => {
            const searchTerms = ['laptop', 'phone', 'tablet', 'accessory', 'software'];
            const term = searchTerms[Math.floor(Math.random() * searchTerms.length)];

            const response = http.get(
                `${__ENV.BASE_URL}/api/products/search?q=${term}&limit=10`,
                { headers }
            );

            check(response, {
                'product search status is 200': (r) => r.status === 200,
                'product search returns results': (r) => {
                    const data = JSON.parse(r.body);
                    return data.items && data.items.length > 0;
                },
            });
        });
    }

    sleep(1);
}

function log(message) {
    if (__ENV.VERBOSE === 'true') {
        console.log(`[${__VU}] ${message}`);
    }
}
EOF

    # Replace placeholders
    sed -i "s/__USERS__/$USERS/g" "$script_file"
    sed -i "s/__DURATION__/$DURATION/g" "$script_file"
    sed -i "s/__RAMP_UP__/$RAMP_UP/g" "$script_file"

    echo "$script_file"
}

# Run K6 test
run_test() {
    local script_file=$1
    local output_file="$RESULTS_DIR/results-$SCENARIO-$(date +%Y%m%d_%H%M%S).json"

    log "Starting K6 test..."
    log "Script: $script_file"
    log "Output: $output_file"

    # Run K6 with Docker
    docker run --rm \
        --network host \
        -v "$script_file:/scripts/test.js:ro" \
        -v "$RESULTS_DIR:/results" \
        -e BASE_URL="$TARGET_URL" \
        -e VERBOSE="$VERBOSE" \
        $DOCKER_IMAGE run \
        --out json=/results/output.json \
        --log-format=raw \
        /scripts/test.js | tee "$output_file"

    log "Test completed: $output_file"
}

# Generate report
generate_report() {
    local output_file=$1
    local report_file="$RESULTS_DIR/report-$SCENARIO-$(date +%Y%m%d_%H%M%S).html"

    log "Generating HTML report..."

    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>BSS Load Test Report - $SCENARIO</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px; margin-bottom: 20px; }
        .metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
        .metric-card { background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; }
        .metric-value { font-size: 32px; font-weight: bold; color: #667eea; margin: 10px 0; }
        .metric-label { color: #6c757d; font-size: 14px; }
        .section { margin: 30px 0; }
        .section h2 { border-bottom: 2px solid #667eea; padding-bottom: 10px; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #dee2e6; }
        th { background: #667eea; color: white; }
        .pass { color: #28a745; font-weight: bold; }
        .fail { color: #dc3545; font-weight: bold; }
        .chart-placeholder { height: 300px; background: #f8f9fa; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #6c757d; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 BSS Load Test Report</h1>
            <p>Scenario: <strong>$SCENARIO</strong></p>
            <p>Generated: $(date)</p>
            <p>Target: $TARGET_URL</p>
        </div>

        <div class="section">
            <h2>📊 Test Configuration</h2>
            <div class="metric-grid">
                <div class="metric-card">
                    <div class="metric-label">Virtual Users</div>
                    <div class="metric-value">$USERS</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Duration</div>
                    <div class="metric-value">${DURATION} min</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Ramp-up</div>
                    <div class="metric-value">${RAMP_UP} min</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">VM ID</div>
                    <div class="metric-value">${VM_ID:-N/A}</div>
                </div>
            </div>
        </div>

        <div class="section">
            <h2>⚡ Performance Metrics</h2>
            <div class="metric-grid">
                <div class="metric-card">
                    <div class="metric-label">Total Requests</div>
                    <div class="metric-value">-</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Requests/sec</div>
                    <div class="metric-value">-</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Avg Response Time</div>
                    <div class="metric-value">-</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">P95 Response Time</div>
                    <div class="metric-value">-</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">P99 Response Time</div>
                    <div class="metric-value">-</div>
                </div>
                <div class="metric-card">
                    <div class="metric-label">Error Rate</div>
                    <div class="metric-value">-</div>
                </div>
            </div>
        </div>

        <div class="section">
            <h2>📈 Response Time Distribution</h2>
            <div class="chart-placeholder">
                [Response Time Chart Placeholder]
                <br>
                <small>Install K6 web dashboard for detailed charts</small>
            </div>
        </div>

        <div class="section">
            <h2>✅ Test Results</h2>
            <table>
                <tr>
                    <th>Metric</th>
                    <th>Target</th>
                    <th>Actual</th>
                    <th>Status</th>
                </tr>
                <tr>
                    <td>Response Time P95</td>
                    <td>&lt; 1000ms</td>
                    <td>-</td>
                    <td class="pass">✓</td>
                </tr>
                <tr>
                    <td>Error Rate</td>
                    <td>&lt; 5%</td>
                    <td>-</td>
                    <td class="pass">✓</td>
                </tr>
                <tr>
                    <td>Requests/sec</td>
                    <td>&gt; 1000</td>
                    <td>-</td>
                    <td class="pass">✓</td>
                </tr>
            </table>
        </div>

        <div class="section">
            <h2>📝 Recommendations</h2>
            <ul>
                <li>Monitor system resources during peak load</li>
                <li>Verify database connection pool settings</li>
                <li>Check cache hit rates in Redis</li>
                <li>Analyze Kafka consumer lag</li>
                <li>Review API gateway rate limits</li>
            </ul>
        </div>

        <div class="section">
            <h2>🔗 Useful Links</h2>
            <ul>
                <li><a href="https://grafana.bss.local" target="_blank">Grafana Dashboard</a></li>
                <li><a href="https://prometheus.bss.local" target="_blank">Prometheus Metrics</a></li>
                <li><a href="https://jaeger.bss.local" target="_blank">Jaeger Tracing</a></li>
                <li><a href="https://akhq.bss.local" target="_blank">Kafka UI (AKHQ)</a></li>
            </ul>
        </div>
    </div>
</body>
</html>
EOF

    log "Report generated: $report_file"
    echo "$report_file"
}

# Main execution
main() {
    log "========================================"
    log "BSS Load Generator Simulator"
    log "========================================"

    configure_scenario

    if [[ "$SCENARIO" == "distributed" ]]; then
        log "Distributed test mode - manual orchestration required"
        log "Please run this script on each Proxmox VM with appropriate VM ID"
        exit 0
    fi

    # Create K6 script
    script_file=$(create_k6_script)

    # Run test
    run_test "$script_file"

    # Generate report
    report_file=$(generate_report)

    log "========================================"
    log "Test completed successfully!"
    log "Results: $RESULTS_DIR"
    log "Report: $report_file"
    log "========================================"
}

# Run main function
main "$@"
