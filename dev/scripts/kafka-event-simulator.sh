#!/bin/bash
################################################################################
# Kafka Event Simulator for BSS - 1M+ Events Testing
# Simulates customer, order, payment, and subscription events
################################################################################

set -e

# Configuration
SIMULATOR_VERSION="1.0"
KAFKA_BROKERS="${KAFKA_BROKERS:-kafka-1:9092,kafka-2:9092,kafka-3:9092}"
RESULTS_DIR="/var/log/kafka-event-tests"
LOG_FILE="$RESULTS_DIR/event-simulator.log"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

# Create results directory
mkdir -p "$RESULTS_DIR"

# Show help
show_help() {
    cat << EOF
BSS Kafka Event Simulator v$SIMULATOR_VERSION

Generates up to 1,000,000+ events for testing Kafka infrastructure

USAGE:
    $0 COMMAND [OPTIONS]

COMMANDS:
    prepare             Create topics and schema registry
    generate N          Generate N events (default: 10000)
    massive N           Generate N events in batch mode (default: 1000000)
    consume TOPIC       Consume events from topic
    throughput          Run throughput test
    latency             Run latency test
    storm N             Generate event storm (N events/sec)
    dlq-test            Test Dead Letter Queue handling
    cleanup             Delete all test topics

OPTIONS:
    -h, --help              Show this help
    -v, --verbose           Verbose output
    -t, --topics T          Comma-separated topic list
    -r, --rate R            Events per second (for throughput)
    -b, --brokers B         Kafka brokers (default: $KAFKA_BROKERS)
    -p, --partitions P      Number of partitions (default: 30)
    -rf, --replication R    Replication factor (default: 3)
    -s, --size SIZE         Message size in bytes (default: 1024)
    -d, --duration D        Duration in seconds (for storm)

EXAMPLES:
    # Prepare Kafka environment
    $0 prepare

    # Generate 100K events
    $0 generate 100000

    # Generate 1M events with batching
    $0 massive 1000000

    # Generate event storm at 50K events/sec for 10 minutes
    $0 storm 50000 --duration 600

    # Test throughput at 10K events/sec
    $0 throughput --rate 10000

    # Consume from customer events topic
    $0 consume bss.customer.events

    # Cleanup test topics
    $0 cleanup

EOF
}

# Create test topics
create_topics() {
    local topics=($1)

    log "Creating Kafka topics..."

    for topic in "${topics[@]}"; do
        info "Creating topic: $topic"
        docker exec -i bss-kafka-1 kafka-topics --create \
            --bootstrap-server kafka-1:9092 \
            --topic "$topic" \
            --partitions 30 \
            --replication-factor 3 \
            --config min.insync.replicas=2 \
            --config retention.ms=604800000 \
            --config retention.bytes=1073741824 \
            --config compression.type=lz4 \
            --if-not-exists
    done

    log "Topics created successfully"
}

# Generate events
generate_events() {
    local num_events=$1
    local topic="${2:-bss.test.events}"
    local message_size=${3:-1024}

    log "Generating $num_events events to topic: $topic"
    log "Message size: $message_size bytes"

    local start_time=$(date +%s)
    local payload=$(generate_payload $message_size)

    # Generate events using Kafka console producer
    for i in $(seq 1 $num_events); do
        local event=$(generate_event $i)
        echo "$event" | docker exec -i bss-kafka-1 kafka-console-producer \
            --bootstrap-server kafka-1:9092 \
            --topic "$topic" \
            --property "parse.key=false" \
            --property "key.separator=,"

        if [[ $((i % 10000)) -eq 0 ]]; then
            local elapsed=$(($(date +%s) - start_time))
            local rate=$((i / elapsed))
            info "Generated $i events ($rate events/sec)"
        fi
    done

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local avg_rate=$((num_events / duration))

    log "Generation completed!"
    log "  Total events: $num_events"
    log "  Duration: ${duration}s"
    log "  Average rate: ${avg_rate} events/sec"
}

# Massive event generation (with batching)
massive_generate() {
    local num_events=$1
    local topic="${2:-bss.test.events}"

    log "Starting MASSIVE event generation: $num_events events"
    log "This will use optimized batching for maximum throughput"

    # Create temporary file for batch generation
    local batch_file="$RESULTS_DIR/events-batch-$(date +%s).jsonl"
    local batch_size=1000

    log "Generating events in batches of $batch_size..."
    local batch_count=$((num_events / batch_size))

    for batch in $(seq 1 $batch_count); do
        local batch_start=$(((batch - 1) * batch_size + 1))
        local batch_end=$((batch * batch_size))

        info "Generating batch $batch/$batch_count (events $batch_start-$batch_end)"

        # Generate batch in background
        {
            for i in $(seq $batch_start $batch_end); do
                generate_event $i
            done
        } > "$batch_file.tmp"

        # Send batch to Kafka
        cat "$batch_file.tmp" | docker exec -i bss-kafka-1 kafka-console-producer \
            --bootstrap-server kafka-1:9092 \
            --topic "$topic" \
            --property "parse.key=false" \
            --batch-size 1000 \
            --request-required-acks all \
            --compression-codec lz4

        rm "$batch_file.tmp"

        # Progress update
        local progress=$((batch * 100 / batch_count))
        info "Progress: ${progress}% ($((batch * batch_size)) events)"
    done

    log "Massive generation completed!"
    log "Total events sent: $num_events"
}

# Generate event storm (high-rate continuous generation)
event_storm() {
    local rate=$1
    local duration=${2:-300}
    local topic="${3:-bss.test.events}"

    log "Starting EVENT STORM"
    log "  Rate: $rate events/second"
    log "  Duration: ${duration}s"
    log "  Topic: $topic"
    log "  Total expected: $((rate * duration)) events"

    # Start event generator in background
    local pid=$BASHPID

    (
        local counter=0
        local start_time=$(date +%s)
        local batch_size=100

        while true; do
            local current_time=$(date +%s)
            local elapsed=$((current_time - start_time))

            if [[ $elapsed -ge $duration ]]; then
                break
            fi

            # Calculate how many events to send in this batch
            local target_count=$(((elapsed + 1) * rate))
            local events_to_send=$((target_count - counter))

            if [[ $events_to_send -gt 0 ]]; then
                # Generate batch
                for i in $(seq 1 $events_to_send); do
                    generate_event $((counter + i))
                done | docker exec -i bss-kafka-1 kafka-console-producer \
                    --bootstrap-server kafka-1:9092 \
                    --topic "$topic" \
                    --property "parse.key=false" \
                    --batch-size $batch_size

                counter=$((counter + events_to_send))

                # Progress update every 10 seconds
                if [[ $((elapsed % 10)) -eq 0 ]]; then
                    local actual_rate=$((counter / elapsed))
                    local progress=$((elapsed * 100 / duration))
                    info "Storm progress: ${progress}% | Events: $counter | Rate: ${actual_rate}/sec"
                fi
            fi

            sleep 0.1
        done

        log "Event storm completed! Total events: $counter"
    ) &

    local storm_pid=$!

    # Monitor storm
    local start_monitor=$(date +%s)
    while kill -0 $storm_pid 2>/dev/null; do
        sleep 5
        local elapsed=$(( $(date +%s) - start_monitor ))
        local progress=$((elapsed * 100 / duration))
        echo -ne "\rStorm in progress... ${progress}% (${elapsed}s/${duration}s)    "
    done

    echo ""
    wait $storm_pid
}

# Throughput test
throughput_test() {
    local target_rate=$1
    local duration=${2:-300}

    log "Starting Throughput Test"
    log "  Target rate: $target_rate events/sec"
    log "  Duration: ${duration}s"

    local topic="bss.throughput.test"
    create_topics "$topic"

    # Run concurrent producers
    local num_producers=5
    local rate_per_producer=$((target_rate / num_producers))

    info "Using $num_producers concurrent producers"
    info "Rate per producer: $rate_per_producer events/sec"

    local pids=()
    for i in $(seq 1 $num_producers); do
        (
            local count=0
            local start_time=$(date +%s)

            while [[ $(($(date +%s) - start_time)) -lt $duration ]]; do
                local batch_start=$count
                for j in $(seq 1 $rate_per_producer); do
                    generate_event $((batch_start + j))
                done | docker exec -i bss-kafka-1 kafka-console-producer \
                    --bootstrap-server kafka-1:9092 \
                    --topic "$topic" \
                    --property "parse.key=false"

                count=$((count + rate_per_producer))
                sleep 1
            done
        ) &

        pids+=($!)
    done

    # Wait for all producers
    for pid in "${pids[@]}"; do
        wait $pid
    done

    log "Throughput test completed"

    # Calculate metrics
    get_consumer_metrics "$topic" "$duration"
}

# Consume and analyze events
consume_events() {
    local topic=$1
    local timeout=${2:-60}

    log "Consuming events from topic: $topic"
    log "Timeout: ${timeout}s"

    docker exec -i bss-kafka-1 kafka-console-consumer \
        --bootstrap-server kafka-1:9092 \
        --topic "$topic" \
        --from-beginning \
        --timeout-ms $((timeout * 1000)) \
        --max-messages 1000 2>/dev/null | head -n 100

    log "Consumer completed"
}

# Get consumer lag and metrics
get_consumer_metrics() {
    local topic=$1
    local duration=$2

    log "Analyzing consumer metrics..."

    # Get consumer group stats
    docker exec bss-kafka-1 kafka-consumer-groups \
        --bootstrap-server kafka-1:9092 \
        --describe \
        --group test-consumer-group \
        --timeout 10000 2>/dev/null || warn "No consumer group found"

    # Get topic stats
    docker exec bss-kafka-1 kafka-topics \
        --bootstrap-server kafka-1:9092 \
        --describe \
        --topic "$topic"
}

# Generate event payload
generate_event() {
    local event_id=$1
    local timestamp=$(date +%s%3N)

    cat << EOF
{
  "specversion": "1.0",
  "type": "com.bss.test.event",
  "source": "/test/generator",
  "id": "evt-$timestamp-$event_id",
  "time": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
  "subject": "test-event-$event_id",
  "data": {
    "eventId": $event_id,
    "timestamp": $timestamp,
    "eventType": "TEST_EVENT",
    "customerId": "cust-$((event_id % 100000))",
    "orderId": "ord-$((event_id % 50000))",
    "amount": $((RANDOM % 10000)) / 100,
    "metadata": {
      "source": "simulator",
      "version": "$SIMULATOR_VERSION"
    }
  }
}
EOF
}

# Generate payload of specific size
generate_payload() {
    local size=$1
    local content="X"
    printf "%${size}s" "$content" | tr ' ' 'X'
}

# Test Dead Letter Queue
test_dlq() {
    log "Testing Dead Letter Queue handling..."

    local topic="bss.test.dlq"
    create_topics "$topic"

    log "Sending valid events..."
    for i in $(seq 1 100); do
        generate_event $i | docker exec -i bss-kafka-1 kafka-console-producer \
            --bootstrap-server kafka-1:9092 \
            --topic "$topic"
    done

    log "Sending invalid events (should go to DLQ)..."
    for i in $(seq 101 200); do
        echo "INVALID_EVENT_DATA_$i" | docker exec -i bss-kafka-1 kafka-console-producer \
            --bootstrap-server kafka-1:9092 \
            --topic "$topic"
    done

    log "DLQ test completed"
    log "Check DLQ topic for invalid events"
}

# Cleanup test topics
cleanup() {
    log "Cleaning up test topics..."

    # List of test topics to delete
    local topics=(
        "bss.test.events"
        "bss.customer.events"
        "bss.order.events"
        "bss.payment.events"
        "bss.subscription.events"
        "bss.test.dlq"
        "bss.throughput.test"
    )

    for topic in "${topics[@]}"; do
        info "Deleting topic: $topic"
        docker exec -i bss-kafka-1 kafka-topics \
            --bootstrap-server kafka-1:9092 \
            --delete \
            --topic "$topic" \
            2>/dev/null || warn "Topic $topic may not exist"
    done

    log "Cleanup completed"
}

# Prepare Kafka environment
prepare() {
    log "Preparing Kafka test environment..."

    local topics=(
        "bss.customer.events"
        "bss.order.events"
        "bss.payment.events"
        "bss.subscription.events"
        "bss.invoice.events"
        "bss.billing.events"
        "bss.test.events"
        "bss.analytics.events"
        "bss.dlq.events"
    )

    create_topics "${topics[*]}"

    # Configure topic settings
    for topic in "${topics[@]}"; do
        info "Configuring topic: $topic"
        docker exec bss-kafka-1 kafka-configs \
            --bootstrap-server kafka-1:9092 \
            --alter \
            --entity-type topics \
            --entity-name "$topic" \
            --add-config "min.insync.replicas=2,retention.ms=604800000,compression.type=lz4" \
            2>/dev/null || true
    done

    log "Kafka environment prepared successfully"
    log "Available topics: ${topics[*]}"
}

# Main function
main() {
    local command=$1
    shift || true

    # Default values
    local verbose=false
    local topics="bss.test.events"
    local partitions=30
    local replication=3
    local message_size=1024
    local brokers="$KAFKA_BROKERS"

    # Parse global options
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            -t|--topics)
                topics="$2"
                shift 2
                ;;
            -p|--partitions)
                partitions="$2"
                shift 2
                ;;
            -rf|--replication)
                replication="$2"
                shift 2
                ;;
            -s|--size)
                message_size="$2"
                shift 2
                ;;
            -b|--brokers)
                brokers="$2"
                shift 2
                ;;
            *)
                break
                ;;
        esac
    done

    # Process command
    case $command in
        prepare)
            prepare
            ;;
        generate)
            local num_events=${1:-10000}
            generate_events "$num_events" "$topics" "$message_size"
            ;;
        massive)
            local num_events=${1:-1000000}
            massive_generate "$num_events" "$topics"
            ;;
        storm)
            local rate=${1:-10000}
            local duration=$(parse_option --duration "$@")
            event_storm "$rate" "$duration" "$topics"
            ;;
        throughput)
            local rate=$(parse_option --rate "$@")
            local duration=$(parse_option --duration "$@")
            throughput_test "$rate" "$duration"
            ;;
        consume)
            local topic=${1:-$topics}
            local timeout=$(parse_option --timeout "$@")
            consume_events "$topic" "$timeout"
            ;;
        dlq-test)
            test_dlq
            ;;
        cleanup)
            cleanup
            ;;
        "")
            show_help
            ;;
        *)
            error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Parse option value from arguments
parse_option() {
    local option=$1
    shift || true

    while [[ $# -gt 0 ]]; do
        if [[ "$1" == "$option" ]]; then
            echo "$2"
            return 0
        fi
        shift
    done

    # Return default
    case $option in
        --duration) echo "300" ;;
        --rate) echo "10000" ;;
        --timeout) echo "60" ;;
        *) echo "" ;;
    esac
}

# Run main
main "$@"
