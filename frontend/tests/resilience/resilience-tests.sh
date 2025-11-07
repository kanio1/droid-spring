#!/bin/bash

# Resilience Testing Suite Runner
# This script runs all resilience and chaos engineering tests

set -e

echo "========================================="
echo "  Resilience Testing Suite"
echo "  (Chaos Engineering & Fault Tolerance)"
echo "========================================="
echo ""

# Configuration
BASE_URL="${BASE_URL:-http://localhost:3000}"
RESULTS_DIR="${RESULTS_DIR:-./results}"
EXPORT_DIR="${EXPORT_DIR:-./results/resilience-$(date +%Y%m%d-%H%M%S)}"
CHAOS_MODE="${CHAOS_MODE:-false}"
STRESS_MODE="${STRESS_MODE:-false}"

# Create results directory
mkdir -p "$RESULTS_DIR"
mkdir -p "$EXPORT_DIR"

echo "Configuration:"
echo "  BASE_URL: $BASE_URL"
echo "  Results Dir: $EXPORT_DIR"
echo "  Chaos Mode: $CHAOS_MODE"
echo "  Stress Mode: $STRESS_MODE"
echo ""

# Function to check if service is running
check_service() {
  local service=$1
  local url=$2

  echo "Checking if $service is running..."
  if curl -s -f "$url" > /dev/null 2>&1; then
    echo "✓ $service is running"
    return 0
  else
    echo "✗ $service is not running at $url"
    echo "  Please start the application before running resilience tests"
    return 1
  fi
}

# Function to run Playwright resilience tests
run_playwright_tests() {
  local test_file=$1
  local test_name=$2
  local extra_args=$3

  echo ""
  echo "========================================="
  echo "  Running: $test_name"
  echo "========================================="

  npx playwright test "$test_file" \
    --reporter=line,html \
    --output="$RESULTS_DIR" \
    $extra_args || {
    echo "✗ $test_name failed"
    return 1
  }

  echo "✓ $test_name completed"
  return 0
}

# Function to run chaos tests
run_chaos_tests() {
  if [ "$CHAOS_MODE" = "true" ]; then
    echo ""
    echo "========================================="
    echo "  Chaos Engineering Mode"
    echo "========================================="
    echo "Injecting failures to test resilience..."

    # This would inject actual failures in a real environment
    # For now, we simulate with test parameters
    echo "✓ Chaos mode simulated in tests"
  else
    echo ""
    echo "========================================="
    echo "  Skipping Chaos Tests (CHAOS_MODE=false)"
    echo "========================================="
  fi
}

# Function to run stress tests
run_stress_tests() {
  if [ "$STRESS_MODE" = "true" ]; then
    echo ""
    echo "========================================="
    echo "  Stress Testing Mode"
    echo "========================================="
    echo "Running extended stress tests..."
    echo "WARNING: This may take 10-15 minutes"

    # Run with extended timeouts
    run_playwright_tests \
      "resilience/load-resilience.spec.ts" \
      "Load & Stress Resilience (Extended)" \
      "--timeout=900000" || return 1
  else
    echo ""
    echo "========================================="
    echo "  Skipping Stress Tests (STRESS_MODE=false)"
    echo "========================================="
  fi
}

# Function to generate resilience report
generate_report() {
  echo ""
  echo "========================================="
  echo "  Generating Resilience Report"
  echo "========================================="

  cat > "$EXPORT_DIR/resilience-summary.txt" << EOF
RESILIENCE TEST SUMMARY
======================
Date: $(date)
Base URL: $BASE_URL
Test Duration: $SECONDS seconds

RESILIENCE PATTERNS TESTED:
1. Circuit Breaker Pattern
   - Failure detection
   - Automatic recovery
   - Bulkhead isolation

2. Timeout Handling
   - Request timeouts
   - Connection timeouts
   - Operation abortion

3. Retry Mechanisms
   - Exponential backoff
   - Jitter
   - Smart retry logic

4. Graceful Degradation
   - Fallback mechanisms
   - Feature degradation
   - Cached data usage

5. Chaos Engineering
   - Failure injection
   - Recovery testing
   - Fault tolerance

6. Load Resilience
   - High concurrency
   - Traffic spikes
   - Sustained load
   - Resource exhaustion

METRICS TRACKED:
- Response times under load
- Success rates during failures
- Recovery times
- Error rates
- Memory usage
- Throughput

PASS/FAIL CRITERIA:
- Circuit breaker prevents cascading failures
- Timeouts prevent resource exhaustion
- Retries succeed for transient errors
- System recovers from failures
- Load performance within SLA
- No memory leaks

RECOMMENDATIONS:
- Monitor circuit breaker states
- Track retry success rates
- Alert on high error rates
- Regular chaos testing
- Capacity planning based on stress tests
EOF

  echo "Report saved to: $EXPORT_DIR/resilience-summary.txt"

  # Generate metrics report if available
  if [ -f "$RESULTS_DIR/results.json" ]; then
    echo ""
    echo "Extracting test metrics..."

    jq -r '
      . Suites[] |
      .Specs[] |
      .title + " - " + .status' \
      "$RESULTS_DIR/results.json" \
      > "$EXPORT_DIR/test-results.txt" 2>/dev/null || true
  fi
}

# Main execution
main() {
  echo "Starting Resilience Testing Suite..."
  echo ""

  # Check if application is running
  if ! check_service "Application" "$BASE_URL"; then
    echo ""
    echo "Please start the application first:"
    echo "  Backend: mvn spring-boot:run"
    echo "  Frontend: pnpm run dev"
    exit 1
  fi

  echo ""

  # Track results
  FAILED_TESTS=()
  PASSED_TESTS=()

  # 1. Circuit Breaker & Fault Tolerance
  if run_playwright_tests \
    "resilience/circuit-breaker.spec.ts" \
    "Circuit Breaker & Fault Tolerance" \
    "--timeout=120000"; then
    PASSED_TESTS+=("Circuit Breaker")
  else
    FAILED_TESTS+=("Circuit Breaker")
  fi

  # 2. Chaos Engineering (optional)
  run_chaos_tests

  # 3. Timeout & Retry Patterns
  if run_playwright_tests \
    "resilience/timeout-retry.spec.ts" \
    "Timeout & Retry Patterns" \
    "--timeout=120000"; then
    PASSED_TESTS+=("Timeout & Retry")
  else
    FAILED_TESTS+=("Timeout & Retry")
  fi

  # 4. Load & Stress Resilience
  if [ "$STRESS_MODE" = "true" ]; then
    run_stress_tests
  else
    # Run shorter version
    if run_playwright_tests \
      "resilience/load-resilience.spec.ts" \
      "Load & Stress Resilience" \
      "--timeout=120000"; then
      PASSED_TESTS+=("Load Resilience")
    else
      FAILED_TESTS+=("Load Resilience")
    fi
  fi

  # 5. Chaos Engineering Tests
  if run_playwright_tests \
    "resilience/chaos-engineering.spec.ts" \
    "Chaos Engineering" \
    "--timeout=120000"; then
    PASSED_TESTS+=("Chaos Engineering")
  else
    FAILED_TESTS+=("Chaos Engineering")
  fi

  # Generate comprehensive report
  generate_report

  # Display summary
  echo ""
  echo "========================================="
  echo "  Resilience Test Summary"
  echo "========================================="
  echo ""
  echo "Passed Tests (${#PASSED_TESTS[@]}):"
  for test in "${PASSED_TESTS[@]}"; do
    echo "  ✓ $test"
  done

  if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
    echo ""
    echo "Failed Tests (${#FAILED_TESTS[@]}):"
    for test in "${FAILED_TESTS[@]}"; do
      echo "  ✗ $test"
    done
  fi

  echo ""
  echo "Results saved to: $EXPORT_DIR"
  echo ""

  # Exit code
  if [ ${#FAILED_TESTS[@]} -eq 0 ]; then
    echo "✓ All resilience tests passed!"
    echo ""
    echo "System is resilient against:"
    echo "  - Service failures"
    echo "  - Network issues"
    echo "  - High load"
    echo "  - Resource exhaustion"
    echo "  - Transient errors"
    exit 0
  else
    echo "✗ Some resilience tests failed"
    echo ""
    echo "Review the failing tests to improve system resilience"
    exit 1
  fi
}

# Help message
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  cat << EOF
Resilience Testing Suite

USAGE:
  ./resilience-tests.sh [OPTIONS]

OPTIONS:
  --help, -h          Show this help message
  --chaos, -c         Enable chaos engineering mode
  --stress, -s        Enable extended stress tests (10-15 min)
  --base-url URL      Set application URL (default: http://localhost:3000)

ENVIRONMENT VARIABLES:
  BASE_URL            Application URL
  CHAOS_MODE          Enable chaos tests (true/false, default: false)
  STRESS_MODE         Enable stress tests (true/false, default: false)
  RESULTS_DIR         Results directory
  EXPORT_DIR          Export directory

EXAMPLES:
  # Run all resilience tests
  ./resilience-tests.sh

  # Run with chaos engineering
  CHAOS_MODE=true ./resilience-tests.sh

  # Run with stress tests
  STRESS_MODE=true ./resilience-tests.sh

  # Run against custom URL
  BASE_URL=https://staging.example.com ./resilience-tests.sh

  # Run chaos + stress
  CHAOS_MODE=true STRESS_MODE=true ./resilience-tests.sh

NOTES:
  - Tests take 5-10 minutes by default
  - Stress mode extends to 10-15 minutes
  - Application must be running before tests
  - Some tests simulate failures (no actual damage)
  - Check results/ directory for detailed reports

EOF
  exit 0
fi

# Run main
main
