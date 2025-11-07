#!/bin/bash

# Security Testing Suite Runner
# This script runs all security tests in sequence

set -e

echo "========================================="
echo "  Security Testing Suite"
echo "========================================="
echo ""

# Configuration
BASE_URL="${BASE_URL:-http://localhost:3000}"
ZAP_PROXY="${ZAP_PROXY:-http://localhost:8080}"
RESULTS_DIR="${RESULTS_DIR:-./results}"
EXPORT_DIR="${EXPORT_DIR:-./results/security-$(date +%Y%m%d-%H%M%S)}"

# Create results directory
mkdir -p "$RESULTS_DIR"
mkdir -p "$EXPORT_DIR"

echo "Configuration:"
echo "  BASE_URL: $BASE_URL"
echo "  ZAP_PROXY: $ZAP_PROXY"
echo "  Results Dir: $EXPORT_DIR"
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
    echo "✗ $service is not running"
    echo "  Please start $service before running security tests"
    return 1
  fi
}

# Function to run Playwright security tests
run_playwright_tests() {
  local test_file=$1
  local test_name=$2

  echo ""
  echo "========================================="
  echo "  Running: $test_name"
  echo "========================================="

  npx playwright test "$test_file" --reporter=line --output="$RESULTS_DIR" || {
    echo "✗ $test_name failed"
    return 1
  }

  echo "✓ $test_name completed"
  return 0
}

# Function to run Nuclei scan
run_nuclei_scan() {
  echo ""
  echo "========================================="
  echo "  Running: Nuclei Vulnerability Scan"
  echo "========================================="

  if ! command -v nuclei &> /dev/null; then
    echo "Nuclei not found. Installing..."

    # Detect OS
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
      curl -L https://github.com/projectdiscovery/nuclei/releases/latest/download/nuclei_2.9.15_linux_amd64.zip -o nuclei.zip
      unzip nuclei.zip -d /tmp
      sudo mv /tmp/nuclei /usr/local/bin/
      rm nuclei.zip
    elif [[ "$OSTYPE" == "darwin"* ]]; then
      brew install nuclei
    else
      echo "Please install nuclei manually: https://github.com/projectdiscovery/nuclei"
      return 1
    fi
  fi

  node ./tests/security/nuclei-scan.js || {
    echo "✗ Nuclei scan failed"
    return 1
  }

  echo "✓ Nuclei scan completed"
  return 0
}

# Function to run ZAP scan
run_zap_scan() {
  echo ""
  echo "========================================="
  echo "  Running: OWASP ZAP Active Scan"
  echo "========================================="

  if ! check_service "ZAP" "$ZAP_PROXY"; then
    echo "Skipping ZAP scan (ZAP not running)"
    return 0
  fi

  npx playwright test zap-active-scan.spec.ts --reporter=line --output="$RESULTS_DIR" || {
    echo "✗ ZAP scan failed"
    return 1
  }

  echo "✓ ZAP scan completed"
  return 0
}

# Main execution
main() {
  echo "Starting Security Testing Suite..."
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

  # 1. Security Headers Validation
  if run_playwright_tests "security/security-headers.spec.ts" "Security Headers Validation"; then
    PASSED_TESTS+=("Security Headers")
  else
    FAILED_TESTS+=("Security Headers")
  fi

  # 2. Authentication & Authorization Security
  if run_playwright_tests "security/auth-security.spec.ts" "Authentication & Authorization Security"; then
    PASSED_TESTS+=("Auth Security")
  else
    FAILED_TESTS+=("Auth Security")
  fi

  # 3. OWASP ZAP Active Scan (if running)
  if [ "${SKIP_ZAP:-false}" != "true" ]; then
    if run_zap_scan; then
      PASSED_TESTS+=("ZAP Active Scan")
    else
      FAILED_TESTS+=("ZAP Active Scan")
    fi
  fi

  # 4. Nuclei Vulnerability Scan
  if [ "${SKIP_NUCLEI:-false}" != "true" ]; then
    if run_nuclei_scan; then
      PASSED_TESTS+=("Nuclei Scan")
    else
      FAILED_TESTS+=("Nuclei Scan")
    fi
  fi

  # Generate summary
  echo ""
  echo "========================================="
  echo "  Security Test Summary"
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

  # Save results
  cat > "$EXPORT_DIR/summary.txt" << EOF
Security Test Summary
=====================
Date: $(date)
Base URL: $BASE_URL

Passed Tests (${#PASSED_TESTS[@]}):
$(for test in "${PASSED_TESTS[@]}"; do echo "  - $test"; done)

Failed Tests (${#FAILED_TESTS[@]}):
$(for test in "${FAILED_TESTS[@]}"; do echo "  - $test"; done)

Total: $(( ${#PASSED_TESTS[@]} + ${#FAILED_TESTS[@]} ))
EOF

  echo ""
  echo "Results saved to: $EXPORT_DIR"
  echo ""

  # Exit code
  if [ ${#FAILED_TESTS[@]} -eq 0 ]; then
    echo "✓ All security tests passed!"
    exit 0
  else
    echo "✗ Some security tests failed"
    exit 1
  fi
}

# Run main
main
