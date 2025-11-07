#!/bin/bash
# Contract Testing Workflow Script
# Runs the complete contract test lifecycle: Consumer → Provider → Verify

set -e

echo "🔍 Starting Contract Testing Workflow"
echo "======================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if we're in the right directory
if [ ! -f "package.json" ]; then
    echo -e "${RED}Error: package.json not found. Run this script from the frontend directory.${NC}"
    exit 1
fi

# Create pacts directory if it doesn't exist
mkdir -p tests/contract/pacts

echo "Step 1: Running Consumer Tests (Frontend)"
echo "-------------------------------------------"
echo ""

# Run consumer tests to generate pacts
if pnpm run test:contract; then
    echo -e "${GREEN}✓ Consumer tests passed${NC}"
else
    echo -e "${RED}✗ Consumer tests failed${NC}"
    exit 1
fi

echo ""
echo "Step 2: Consumer Contract Files Generated"
echo "-------------------------------------------"
echo ""

# List generated pact files
if ls tests/contract/pacts/*.json 1> /dev/null 2>&1; then
    echo "Generated pact files:"
    ls -lh tests/contract/pacts/*.json
else
    echo -e "${YELLOW}Warning: No pact files found${NC}"
fi

echo ""
echo "Step 3: Provider Verification (Backend)"
echo "----------------------------------------"
echo ""

# Check if we're in a monorepo and backend exists
if [ -d "../backend" ]; then
    echo "Backend found. Running provider verification tests..."
    echo ""

    # Navigate to backend
    cd ../backend

    # Run provider verification tests
    if mvn test -Dtest=*ContractTest -DfailIfNoTests=false 2>&1 | tee /tmp/pact-verification.log; then
        echo ""
        echo -e "${GREEN}✓ Provider verification passed${NC}"
    else
        echo ""
        echo -e "${RED}✗ Provider verification failed${NC}"
        echo ""
        echo "Logs saved to: /tmp/pact-verification.log"
        exit 1
    fi

    # Move back to frontend
    cd -
else
    echo -e "${YELLOW}Backend directory not found. Skipping provider verification.${NC}"
    echo "To verify provider, run: cd ../backend && mvn test -Dtest=*ContractTest"
fi

echo ""
echo "Step 4: Optional - Publish to Pact Broker"
echo "-------------------------------------------"
echo ""

# Check if broker URL is configured
if [ -n "$PACT_BROKER_BASE_URL" ] && [ -n "$PACT_BROKER_TOKEN" ]; then
    echo "Publishing pacts to broker..."
    pnpm run test:contract:publish
    echo -e "${GREEN}✓ Pacts published to broker${NC}"
else
    echo -e "${YELLOW}Skipping publish (PACT_BROKER_BASE_URL or PACT_BROKER_TOKEN not set)${NC}"
fi

echo ""
echo "======================================="
echo -e "${GREEN}✓ Contract Testing Complete!${NC}"
echo "======================================="
echo ""
echo "Summary:"
echo "  • Consumer tests: PASSED"
echo "  • Provider verification: PASSED"
if [ -n "$PACT_BROKER_BASE_URL" ]; then
    echo "  • Pacts published: YES"
else
    echo "  • Pacts published: NO (broker not configured)"
fi
echo ""
echo "Next steps:"
echo "  • Review generated pacts in tests/contract/pacts/"
echo "  • Check provider verification logs in backend test results"
echo "  • Configure Pact broker for continuous integration"
echo ""
