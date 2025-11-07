#!/bin/bash
# ============================================
# Kafka Consumer Lag Monitoring Script
# ============================================
# Purpose: Monitor consumer lag across all topics
# Generates alerts and reports
# Created: 2025-11-07
# ============================================

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
KAFKA_BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka-1:9092}"
KAFKA_CLIENT_CONFIG="/etc/ssl/certs/kafka-client.properties"
LOG_FILE="/tmp/kafka-consumer-lag.log"
REPORT_FILE="/tmp/kafka-consumer-lag-report.html"
THRESHOLD_WARN=10000
THRESHOLD_CRIT=100000

# Function to log messages
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

# Get all consumer groups
get_consumer_groups() {
    log "Fetching consumer groups..."

    docker exec bss-kafka-1 kafka-consumer-groups.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" \
        --list 2>/dev/null || {
        error "Failed to fetch consumer groups"
        return 1
    }
}

# Get consumer group details
get_consumer_group_details() {
    local group=$1

    log "Fetching details for consumer group: $group"

    docker exec bss-kafka-1 kafka-consumer-groups.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" \
        --describe \
        --group "$group" \
        --state 2>/dev/null || {
        warn "Failed to fetch details for group: $group"
        return 1
    }
}

# Calculate lag for a specific consumer group
calculate_group_lag() {
    local group=$1
    local topic=$2

    # Get current offset
    local current_offset=$(docker exec bss-kafka-1 kafka-consumer-groups.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" \
        --describe \
        --group "$group" \
        --offsets 2>/dev/null | grep "$topic" | awk '{print $4}' | head -1)

    # Get log end offset
    local log_end_offset=$(docker exec bss-kafka-1 kafka-run-class.sh \
        kafka.tools.GetOffsetShell \
        --broker-list "$KAFKA_BOOTSTRAP_SERVER" \
        --topic "$topic" \
        --time -1 2>/dev/null | grep "$topic" | awk -F':' '{print $3}' | head -1)

    # Calculate lag if both values exist
    if [ -n "$current_offset" ] && [ -n "$log_end_offset" ] && [ "$current_offset" != "Unknown" ]; then
        echo $((log_end_offset - current_offset))
    else
        echo 0
    fi
}

# Generate lag report in HTML format
generate_html_report() {
    local groups=$1
    local timestamp=$(date +'%Y-%m-%d %H:%M:%S')

    cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Kafka Consumer Lag Report</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background: #f5f5f5;
        }
        .container {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 2px solid #007bff;
            padding-bottom: 10px;
        }
        h2 {
            color: #666;
            margin-top: 30px;
        }
        .summary {
            background: #e7f3ff;
            padding: 15px;
            border-left: 4px solid #007bff;
            margin: 20px 0;
        }
        .alert-warning {
            background: #fff3cd;
            color: #856404;
            padding: 15px;
            border-left: 4px solid #ffc107;
            margin: 10px 0;
        }
        .alert-critical {
            background: #f8d7da;
            color: #721c24;
            padding: 15px;
            border-left: 4px solid #dc3545;
            margin: 10px 0;
        }
        .alert-ok {
            background: #d4edda;
            color: #155724;
            padding: 15px;
            border-left: 4px solid #28a745;
            margin: 10px 0;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        th {
            background: #007bff;
            color: white;
        }
        tr:nth-child(even) {
            background: #f2f2f2;
        }
        .lag-ok { background: #d4edda; }
        .lag-warning { background: #fff3cd; }
        .lag-critical { background: #f8d7da; }
        .footer {
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #ddd;
            color: #666;
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>Kafka Consumer Lag Report</h1>
        <p><strong>Generated:</strong> $timestamp</p>

        <div class="summary">
            <h2>Summary</h2>
            <p><strong>Total Consumer Groups:</strong> $(echo "$groups" | wc -l)</p>
            <p><strong>Checked At:</strong> $timestamp</p>
        </div>

        <h2>Consumer Group Details</h2>
        <table>
            <thead>
                <tr>
                    <th>Group ID</th>
                    <th>Topic</th>
                    <th>Partition</th>
                    <th>Current Offset</th>
                    <th>Log End Offset</th>
                    <th>Lag</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
EOF

    # Process each group
    local total_lag=0
    local warning_count=0
    local critical_count=0

    while IFS= read -r group; do
        info "Processing group: $group"

        # Get group description
        local group_desc=$(docker exec bss-kafka-1 kafka-consumer-groups.sh \
            --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
            --command-config "$KAFKA_CLIENT_CONFIG" \
            --describe \
            --group "$group" 2>/dev/null || echo "")

        if [ -n "$group_desc" ]; then
            # Parse group description and add to HTML
            while IFS= read -r line; do
                if [[ "$line" =~ ^[[:space:]]*${group} ]]; then
                    # Parse CSV line
                    local group_id=$(echo "$line" | cut -d',' -f1)
                    local topic=$(echo "$line" | cut -d',' -f2)
                    local partition=$(echo "$line" | cut -d',' -f3)
                    local current_offset=$(echo "$line" | cut -d',' -f4)
                    local log_end_offset=$(echo "$line" | cut -d',' -f5)
                    local lag=$(echo "$line" | cut -d',' -f6)

                    if [ "$current_offset" = "Unknown" ]; then
                        current_offset="N/A"
                        log_end_offset="N/A"
                        lag="N/A"
                    fi

                    # Determine status
                    local status_class="lag-ok"
                    local status_text="OK"
                    if [ "$lag" != "N/A" ]; then
                        if [ "$lag" -gt "$THRESHOLD_CRIT" ]; then
                            status_class="lag-critical"
                            status_text="CRITICAL"
                            critical_count=$((critical_count + 1))
                        elif [ "$lag" -gt "$THRESHOLD_WARN" ]; then
                            status_class="lag-warning"
                            status_text="WARNING"
                            warning_count=$((warning_count + 1))
                        fi
                    fi

                    # Add to HTML
                    cat >> "$REPORT_FILE" << EOF
                <tr>
                    <td>$group_id</td>
                    <td>$topic</td>
                    <td>$partition</td>
                    <td>$current_offset</td>
                    <td>$log_end_offset</td>
                    <td>$lag</td>
                    <td class="$status_class">$status_text</td>
                </tr>
EOF
                fi
            done <<< "$group_desc"
        fi
    done <<< "$groups"

    # Add summary row
    cat >> "$REPORT_FILE" << EOF
            </tbody>
        </table>

        <h2>Alerts</h2>
EOF

    if [ $critical_count -gt 0 ]; then
        cat >> "$REPORT_FILE" << EOF
        <div class="alert-critical">
            <strong>CRITICAL:</strong> $critical_count consumer groups have critical lag (> $THRESHOLD_CRIT messages)
        </div>
EOF
    fi

    if [ $warning_count -gt 0 ]; then
        cat >> "$REPORT_FILE" << EOF
        <div class="alert-warning">
            <strong>WARNING:</strong> $warning_count consumer groups have high lag (> $THRESHOLD_WARN messages)
        </div>
EOF
    fi

    if [ $critical_count -eq 0 ] && [ $warning_count -eq 0 ]; then
        cat >> "$REPORT_FILE" << EOF
        <div class="alert-ok">
            <strong>OK:</strong> All consumer groups are within acceptable lag thresholds
        </div>
EOF
    fi

    # Add footer
    cat >> "$REPORT_FILE" << EOF

        <div class="footer">
            <p>Generated by Kafka Consumer Lag Monitor</p>
            <p>For more information, visit: <a href="https://grafana.bss.local/d/kafka/consumer-lag">Grafana Dashboard</a></p>
        </div>
    </div>
</body>
</html>
EOF

    log "HTML report generated: $REPORT_FILE"
}

# Generate JSON report
generate_json_report() {
    local groups=$1
    local timestamp=$(date -Iseconds)

    local report=$(cat <<EOF
{
  "timestamp": "$timestamp",
  "threshold_warning": $THRESHOLD_WARN,
  "threshold_critical": $THRESHOLD_CRIT,
  "consumer_groups": [
EOF

    local first=true
    while IFS= read -r group; do
        if [ "$first" = false ]; then
            report+=","
        fi
        first=false

        local group_desc=$(docker exec bss-kafka-1 kafka-consumer-groups.sh \
            --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
            --command-config "$KAFKA_CLIENT_CONFIG" \
            --describe \
            --group "$group" 2>/dev/null || echo "")

        local group_data="[]"
        if [ -n "$group_desc" ]; then
            local group_entries="["
            local first_entry=true

            while IFS= read -r line; do
                if [[ "$line" =~ ^[[:space:]]*${group} ]]; then
                    if [ "$first_entry" = false ]; then
                        group_entries+=","
                    fi
                    first_entry=false

                    local group_id=$(echo "$line" | cut -d',' -f1)
                    local topic=$(echo "$line" | cut -d',' -f2)
                    local partition=$(echo "$line" | cut -d',' -f3)
                    local current_offset=$(echo "$line" | cut -d',' -f4)
                    local log_end_offset=$(echo "$line" | cut -d',' -f5)
                    local lag=$(echo "$line" | cut -d',' -f6)
                    local status=$(echo "$line" | cut -d',' -f7)

                    group_entries+=$(cat <<JSON
{
  "topic": "$topic",
  "partition": $partition,
  "current_offset": "$current_offset",
  "log_end_offset": "$log_end_offset",
  "lag": "$lag",
  "status": "$status"
}
JSON
)
                fi
            done <<< "$group_desc"

            group_entries+="]"
            group_data="$group_entries"
        fi

        report+=$(cat <<EOF
    {
      "group_id": "$group",
      "topics": $group_data
    }
EOF
)
    done <<< "$groups"

    report+="  ]\n}"
    echo "$report" > /tmp/kafka-consumer-lag.json

    log "JSON report generated: /tmp/kafka-consumer-lag.json"
}

# Send alerts to monitoring system
send_alerts() {
    local groups=$1
    local alert_count=0

    while IFS= read -r group; do
        local group_desc=$(docker exec bss-kafka-1 kafka-consumer-groups.sh \
            --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
            --command-config "$KAFKA_CLIENT_CONFIG" \
            --describe \
            --group "$group" 2>/dev/null || echo "")

        if [ -n "$group_desc" ]; then
            while IFS= read -r line; do
                if [[ "$line" =~ ^[[:space:]]*${group} ]]; then
                    local lag=$(echo "$line" | cut -d',' -f6)

                    if [ "$lag" != "Unknown" ] && [ -n "$lag" ]; then
                        if [ "$lag" -gt "$THRESHOLD_CRIT" ]; then
                            alert_count=$((alert_count + 1))
                            warn "CRITICAL: $group has critical lag: $lag"
                        elif [ "$lag" -gt "$THRESHOLD_WARN" ]; then
                            alert_count=$((alert_count + 1))
                            warn "WARNING: $group has high lag: $lag"
                        fi
                    fi
                fi
            done <<< "$group_desc"
        fi
    done <<< "$groups"

    if [ $alert_count -eq 0 ]; then
        log "No alerts: All consumer groups are healthy"
    fi
}

# Main execution
main() {
    log "==========================================="
    log "Kafka Consumer Lag Monitor"
    log "==========================================="
    log ""

    # Check if Kafka is accessible
    if ! docker exec bss-kafka-1 kafka-broker-api-versions \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVER" \
        --command-config "$KAFKA_CLIENT_CONFIG" > /dev/null 2>&1; then
        error "Cannot connect to Kafka. Please ensure Kafka is running."
        exit 1
    fi

    # Get all consumer groups
    local groups=$(get_consumer_groups)

    if [ -z "$groups" ]; then
        warn "No consumer groups found"
        return 1
    fi

    log "Found $(echo "$groups" | wc -l) consumer groups"

    # Generate reports
    log ""
    log "Generating reports..."
    generate_json_report "$groups"
    generate_html_report "$groups"

    # Send alerts
    log ""
    send_alerts "$groups"

    # Summary
    log ""
    log "==========================================="
    log "Monitoring Complete"
    log "==========================================="
    log "HTML Report: $REPORT_FILE"
    log "JSON Report: /tmp/kafka-consumer-lag.json"
    log "Log File: $LOG_FILE"
    log ""
    log "To view the HTML report:"
    log "  cat $REPORT_FILE | html2text  # If html2text is installed"
    log "  Or open $REPORT_FILE in a browser"
    log ""
    log "To view the JSON report:"
    log "  cat /tmp/kafka-consumer-lag.json"
    log ""
    log "==========================================="

    # Optional: Open report in browser
    if command -v xdg-open > /dev/null 2>&1; then
        read -p "Open report in browser? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            xdg-open "$REPORT_FILE"
        fi
    fi
}

# Run main function
main
