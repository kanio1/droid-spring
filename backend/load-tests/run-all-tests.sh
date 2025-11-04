#!/bin/bash

# Load Testing Runner Script
# This script runs all load test scenarios sequentially

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
BASE_URL=${BASE_URL:-"http://localhost:8080"}
VERBOSE=${VERBOSE:-false}

# Print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if k6 is installed
check_k6() {
    if ! command -v k6 &> /dev/null; then
        print_error "k6 is not installed!"
        echo "Please install k6: https://k6.io/docs/getting-started/installation/"
        exit 1
    fi
    print_info "k6 version: $(k6 version)"
}

# Check if backend is running
check_backend() {
    if ! curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
        print_error "Backend is not running at $BASE_URL"
        echo "Please start the backend first:"
        echo "  mvn spring-boot:run"
        exit 1
    fi
    print_info "Backend is running at $BASE_URL"
}

# Run a single test
run_test() {
    local test_file=$1
    local test_name=$(basename "$test_file" .js)

    print_info "Running $test_name load test..."

    if [ "$VERBOSE" = true ]; then
        k6 run "$test_file"
    else
        k6 run "$test_file" 2>&1 | grep -E "(✓|✗|running|done|http_req|checks|error)"
    fi

    if [ $? -eq 0 ]; then
        print_info "$test_name test completed successfully!"
    else
        print_error "$test_name test failed!"
        return 1
    fi
}

# Main execution
main() {
    echo "====================================="
    echo "BSS Backend Load Testing Suite"
    echo "====================================="
    echo ""

    # Check prerequisites
    check_k6
    check_backend

    echo ""
    print_info "Base URL: $BASE_URL"
    print_info "Test files: $(ls -1 *.js 2>/dev/null | wc -l) scenarios"
    echo ""

    # Change to script directory
    cd "$(dirname "$0")"

    # Run all tests
    local total_tests=0
    local passed_tests=0

    for test_file in customers-api.js invoices-api.js payments-api.js; do
        if [ -f "$test_file" ]; then
            total_tests=$((total_tests + 1))

            echo ""
            echo "-------------------------------------"
            print_info "Test $total_tests: $test_file"
            echo "-------------------------------------"

            if run_test "$test_file"; then
                passed_tests=$((passed_tests + 1))
            else
                print_warn "Continuing with next test..."
                sleep 2
            fi
        fi
    done

    # Summary
    echo ""
    echo "====================================="
    print_info "Load Testing Complete!"
    echo "====================================="
    echo "Total tests: $total_tests"
    echo "Passed: $passed_tests"
    echo "Failed: $((total_tests - passed_tests))"
    echo ""

    if [ $passed_tests -eq $total_tests ]; then
        print_info "All tests passed successfully! ✓"
        exit 0
    else
        print_error "Some tests failed! ✗"
        exit 1
    fi
}

# Show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -v, --verbose  Enable verbose output"
    echo ""
    echo "Environment Variables:"
    echo "  BASE_URL       Backend URL (default: http://localhost:8080)"
    echo "  VERBOSE        Enable verbose output (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                          # Run all tests with default settings"
    echo "  BASE_URL=http://localhost:8080 $0  # Run with custom backend URL"
    echo "  $0 --verbose               # Run with verbose output"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_usage
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Run main function
main
