#!/bin/bash
# Allure Report Generation Script
# Generates and serves Allure test reports

set -e

echo "=========================================="
echo "  BSS Backend - Allure Report Generator  "
echo "=========================================="
echo ""

# Configuration
RESULTS_DIR="target/allure-results"
REPORT_DIR="target/allure-report"
HISTORY_DIR="target/allure-history"
PORT=5050

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

print_success "Maven found: $(mvn -version | head -n 1)"

# Run tests and generate Allure results
echo ""
echo "Step 1: Running tests and generating Allure results..."
echo "------------------------------------------------------"

mvn clean test -Dallure.results.pattern='**/allure-results/*.json' -q

if [ $? -eq 0 ]; then
    print_success "Tests completed successfully"
else
    print_warning "Some tests failed, but continuing with report generation..."
fi

# Check if results directory exists
if [ ! -d "$RESULTS_DIR" ]; then
    print_error "Allure results directory not found: $RESULTS_DIR"
    exit 1
fi

# Count test results
RESULT_COUNT=$(find $RESULTS_DIR -name "*.json" | wc -l)
print_success "Found $RESULT_COUNT test result files"

# Copy previous history if exists
if [ -d "$HISTORY_DIR" ]; then
    print_success "Copying previous history..."
    cp -r $HISTORY_DIR/* $RESULTS_DIR/ 2>/dev/null || true
fi

# Generate Allure report
echo ""
echo "Step 2: Generating Allure report..."
echo "------------------------------------------------------"

mvn allure:serve -Dallure.results.pattern='**/allure-results/*.json' -q -Dport=$PORT

# Alternative manual command if plugin fails:
# allure generate $RESULTS_DIR -o $REPORT_DIR --clean
# allure open $REPORT_DIR -p $PORT

print_success "Allure report generated and served at http://localhost:$PORT"

# Save history for next run
if [ -d "$RESULTS_DIR/history" ]; then
    mkdir -p $HISTORY_DIR
    cp -r $RESULTS_DIR/history/* $HISTORY_DIR/ 2>/dev/null || true
    print_success "History saved for future reports"
fi

echo ""
echo "=========================================="
echo "  Report Generation Complete!            "
echo "=========================================="
echo ""
echo "Report URL: http://localhost:$PORT"
echo "Report Directory: $REPORT_DIR"
echo "Results Directory: $RESULTS_DIR"
echo ""

# Keep server running if invoked with --serve
if [ "$1" == "--serve" ]; then
    echo "Server is running... Press Ctrl+C to stop"
    tail -f /dev/null
fi
