#!/bin/bash

# Allure Report Generator
# This script generates Allure reports from test results

set -e

echo "========================================="
echo "  Allure Report Generator"
echo "========================================="
echo ""

# Configuration
ALLURE_RESULTS_DIR="${ALLURE_RESULTS_DIR:-./allure-results}"
ALLURE_REPORT_DIR="${ALLURE_REPORT_DIR:-./allure-report}"
HISTORY_DIR="${HISTORY_DIR:-./allure-history}"
ALLURE_VERSION="${ALLURE_VERSION:-2.24.1}"

# Detect OS
OS="$(uname -s)"
case "${OS}" in
  Linux*)     MACHINE=Linux;;
  Darwin*)    MACHINE=Mac;;
  CYGWIN*)    MACHINE=Cygwin;;
  MINGW*)     MACHINE=MinGw;;
  *)          MACHINE="UNKNOWN:${OS}"
esac

echo "Detected OS: ${MACHINE}"
echo ""

# Function to check if Allure is installed
check_allure() {
  if command -v allure &> /dev/null; then
    ALLURE_CMD="allure"
    ALLURE_VERSION_INSTALLED=$(allure --version 2>/dev/null || echo "unknown")
    echo "✓ Allure found: ${ALLURE_VERSION_INSTALLED}"
    return 0
  fi

  echo "✗ Allure not found"
  return 1
}

# Function to install Allure
install_allure() {
  echo ""
  echo "Installing Allure..."

  case "${MACHINE}" in
    Linux)
      sudo apt-get update
      sudo apt-get install -y default-jre
      curl -o allure-${ALLURE_VERSION}.tgz -L https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.tgz
      sudo tar -xzf allure-${ALLURE_VERSION}.tgz -C /opt
      sudo ln -s /opt/allure-${ALLURE_VERSION}/bin/allure /usr/local/bin/allure
      rm allure-${ALLURE_VERSION}.tgz
      ALLURE_CMD="allure"
      ;;
    Mac)
      brew install allure
      ALLURE_CMD="allure"
      ;;
    Cygwin|MinGw)
      echo "Please install Allure manually or use package manager"
      exit 1
      ;;
    *)
      echo "Unsupported OS: ${MACHINE}"
      exit 1
      ;;
  esac

  echo "✓ Allure installed successfully"
}

# Function to ensure results directory exists
ensure_results_dir() {
  if [ ! -d "$ALLURE_RESULTS_DIR" ]; then
    echo ""
    echo "Creating results directory: $ALLURE_RESULTS_DIR"
    mkdir -p "$ALLURE_RESULTS_DIR"
  fi

  if [ ! -d "$HISTORY_DIR" ]; then
    echo "Creating history directory: $HISTORY_DIR"
    mkdir -p "$HISTORY_DIR"
  fi
}

# Function to clean old results
clean_old_results() {
  if [ "$1" = "clean" ]; then
    echo ""
    echo "Cleaning old results..."
    rm -rf "$ALLURE_RESULTS_DIR"/*
    echo "✓ Results directory cleaned"
  fi
}

# Function to generate report
generate_report() {
  echo ""
  echo "Generating Allure report..."
  echo "Results directory: $ALLURE_RESULTS_DIR"
  echo "Report directory: $ALLURE_REPORT_DIR"
  echo "History directory: $HISTORY_DIR"
  echo ""

  # Generate report
  $ALLURE_CMD generate \
    "$ALLURE_RESULTS_DIR" \
    -o "$ALLURE_REPORT_DIR" \
    --history-dir "$HISTORY_DIR" \
    --clean

  if [ $? -eq 0 ]; then
    echo "✓ Report generated successfully"
    echo ""
    echo "Report location: $ALLURE_REPORT_DIR/index.html"
    echo ""

    # Offer to open report
    if command -v open &> /dev/null; then
      echo "Opening report in browser..."
      open "$ALLURE_REPORT_DIR/index.html"
    elif command -v xdg-open &> /dev/null; then
      echo "Opening report in browser..."
      xdg-open "$ALLURE_REPORT_DIR/index.html"
    fi
  else
    echo "✗ Failed to generate report"
    exit 1
  fi
}

# Function to serve report
serve_report() {
  echo ""
  echo "Starting Allure server on port 5050..."
  echo "Open http://localhost:5050 in your browser"
  echo "Press Ctrl+C to stop"
  echo ""

  $ALLURE_CMD serve "$ALLURE_RESULTS_DIR" -p 5050
}

# Function to run tests with Allure
run_tests() {
  local test_type=$1
  local clean_flag=$2

  echo ""
  echo "Running tests with Allure reporting..."
  echo "Test type: ${test_type:-all}"
  echo ""

  # Clean if requested
  clean_old_results "$clean_flag"

  # Run tests based on type
  case "$test_type" in
    e2e)
      npx playwright test --reporter=line,allure-file
      ;;
    smoke)
      npx playwright test --project=smoke --reporter=line,allure-file
      ;;
    regression)
      npx playwright test --project=regression --reporter=line,allure-file
      ;;
    security)
      npx playwright test --project=security --reporter=line,allure-file
      ;;
    resilience)
      npx playwright test --project=resilience --reporter=line,allure-file
      ;;
    performance)
      echo "Performance tests (k6) - Results in $ALLURE_RESULTS_DIR"
      # k6 tests would need custom reporter
      ;;
    all)
      npx playwright test --reporter=line,allure-file
      ;;
    *)
      echo "Unknown test type: $test_type"
      exit 1
      ;;
  esac

  if [ $? -eq 0 ]; then
    echo "✓ Tests completed successfully"
  else
    echo "✗ Some tests failed"
  fi
}

# Main function
main() {
  local action=$1
  local test_type=$2
  local clean_flag=$3

  case "$action" in
    install)
      install_allure
      ;;
    generate)
      ensure_results_dir
      generate_report
      ;;
    serve)
      ensure_results_dir
      serve_report
      ;;
    run)
      run_tests "$test_type" "$clean_flag"
      generate_report
      ;;
    clean)
      clean_old_results "clean"
      ;;
    check)
      if check_allure; then
        echo "✓ Allure is available"
        exit 0
      else
        echo "✗ Allure is not installed"
        echo "Run with 'install' to install Allure"
        exit 1
      fi
      ;;
    help|--help|-h)
      cat << EOF
Allure Report Generator

USAGE:
  ./generate-allure-report.sh [ACTION] [OPTIONS]

ACTIONS:
  install           Install Allure
  generate          Generate report from existing results
  serve             Serve report on port 5050
  run [TYPE]        Run tests and generate report
  clean             Clean old results
  check             Check if Allure is installed
  help              Show this help

TEST TYPES (for 'run' action):
  e2e              Run E2E tests
  smoke            Run smoke tests
  regression       Run regression tests
  security         Run security tests
  resilience       Run resilience tests
  performance      Run performance tests
  all              Run all tests (default)

OPTIONS:
  clean            Clean old results before running

EXAMPLES:
  # Install Allure
  ./generate-allure-report.sh install

  # Run E2E tests and generate report
  ./generate-allure-report.sh run e2e

  # Run all tests with clean
  ./generate-allure-report.sh run all clean

  # Generate report from existing results
  ./generate-allure-report.sh generate

  # Serve report on port 5050
  ./generate-allure-report.sh serve

ENVIRONMENT VARIABLES:
  ALLURE_RESULTS_DIR    Results directory (default: ./allure-results)
  ALLURE_REPORT_DIR     Report directory (default: ./allure-report)
  HISTORY_DIR           History directory (default: ./allure-history)
  ALLURE_VERSION        Allure version to install (default: 2.24.1)

EOF
      exit 0
      ;;
    *)
      echo "Unknown action: $action"
      echo "Use 'help' for usage information"
      exit 1
      ;;
  esac
}

# Run main function
main "$@"
