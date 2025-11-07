#!/bin/bash

# Visual Testing Script with Percy
# This script runs visual regression tests using Percy and Playwright

set -e

echo "========================================"
echo "Visual Regression Testing with Percy"
echo "========================================"
echo ""

# Check if PERCY_TOKEN is set
if [ -z "$PERCY_TOKEN" ]; then
    echo "⚠️  PERCY_TOKEN is not set"
    echo ""
    echo "To run visual tests, you need to:"
    echo "1. Get a Percy token from: https://percy.io/app/project-settings"
    echo "2. Set it as an environment variable:"
    echo "   export PERCY_TOKEN=your_token_here"
    echo ""
    echo "Or copy .env.percy.example to .env.percy and add your token there"
    echo ""
    echo "For CI/CD, you can set the token in your CI environment variables"
    echo ""
    read -p "Do you want to continue with a dry-run? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    export PERCY_DRY_RUN=true
    echo "Running in dry-run mode (no actual snapshots will be sent to Percy)"
    echo ""
fi

# Check if dev server is running
if ! curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo "⚠️  Dev server is not running on http://localhost:3000"
    echo "Starting dev server..."
    echo ""

    # Start dev server in background
    pnpm run dev &
    DEV_SERVER_PID=$!

    # Wait for server to be ready
    echo "Waiting for dev server to be ready..."
    for i in {1..30}; do
        if curl -s http://localhost:3000 > /dev/null 2>&1; then
            echo "✅ Dev server is ready!"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "❌ Failed to start dev server"
            exit 1
        fi
        sleep 2
    done
    echo ""
fi

# Run visual tests
echo "Running visual regression tests..."
echo "Test files: ./tests/visual/*.spec.ts"
echo ""

# Load Percy environment if .env.percy exists
if [ -f .env.percy ]; then
    echo "Loading Percy configuration from .env.percy"
    export $(cat .env.percy | grep -v '^#' | xargs)
fi

# Run Percy with Playwright
if [ "$PERCY_DRY_RUN" = "true" ]; then
    echo "DRY RUN MODE: Skipping Percy snapshots"
    pnpm playwright test visual --project=visual
else
    pnpm test:visual
fi

EXIT_CODE=$?

# Cleanup
if [ ! -z "$DEV_SERVER_PID" ]; then
    echo ""
    echo "Stopping dev server..."
    kill $DEV_SERVER_PID 2>/dev/null || true
fi

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "✅ Visual tests completed successfully!"
    echo "========================================"
    echo ""
    echo "View results:"
    echo "- Percy Dashboard: https://percy.io"
    echo "- Playwright Report: ./playwright-report/index.html"
    echo ""
else
    echo ""
    echo "========================================"
    echo "❌ Visual tests failed"
    echo "========================================"
    echo ""
    echo "Check the report for details:"
    echo "- Playwright Report: ./playwright-report/index.html"
    echo ""
fi

exit $EXIT_CODE
