#!/bin/bash
# Visual Regression Baseline Update Script
#
# This script helps update visual regression baselines when legitimate
# visual changes are introduced

set -e

BASELINE_DIR="test-results/baseline"
UPDATE_MODE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -u|--update)
      UPDATE_MODE=true
      shift
      ;;
    -h|--help)
      echo "Usage: $0 [options]"
      echo ""
      echo "Options:"
      echo "  -u, --update    Update baseline images"
      echo "  -h, --help      Show this help message"
      echo ""
      echo "Examples:"
      echo "  $0              # Run tests without updating baselines"
      echo "  $0 -u           # Run tests and update baselines"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo "========================================="
echo "Visual Regression Test Runner"
echo "========================================="
echo ""

# Create baseline directory if it doesn't exist
if [ ! -d "$BASELINE_DIR" ]; then
  echo "Creating baseline directory: $BASELINE_DIR"
  mkdir -p "$BASELINE_DIR"
fi

# Run tests
echo "Running visual regression tests..."
echo ""

if [ "$UPDATE_MODE" = true ]; then
  echo "UPDATE MODE: Baselines will be updated with new screenshots"
  echo ""

  # Update baselines with new screenshots
  pnpm exec playwright test --update-snapshots

  echo ""
  echo "✓ Baseline images updated successfully!"
  echo ""
  echo "Review the changes with:"
  echo "  git diff --name-only"
  echo ""
  echo "Updated baselines:"
  find test-results -name "*-snapshots" -type d
else
  echo "Running tests with existing baselines..."
  echo ""

  pnpm exec playwright test

  echo ""
  echo "✓ Visual regression tests completed!"
  echo ""
  echo "To update baselines for legitimate changes, run:"
  echo "  ./tests/visual/update-baselines.sh -u"
  echo ""
  echo "New baselines will be saved to: $BASELINE_DIR"
fi

# Show summary
echo "========================================="
echo "Summary"
echo "========================================="
echo ""

if [ -d "test-results" ]; then
  echo "Test Results:"
  echo "  Total: $(find test-results -type f \( -name "*.png" -o -name "*.jpg" \) | wc -l) images"
  echo "  Passed: $(find test-results -type f -name "*-expected.png" | wc -l)"
  echo "  Failed: $(find test-results -type f -name "*-actual.png" | wc -l)"
  echo "  Diff: $(find test-results -type f -name "*-diff.png" | wc -l)"
  echo ""
fi

if [ "$UPDATE_MODE" = true ]; then
  echo "✓ Baselines have been updated"
  echo ""
  echo "Next steps:"
  echo "  1. Review the updated baseline images"
  echo "  2. Commit the changes: git add ."
  echo "  3. Push to repository: git push"
else
  echo "Test Results Location: test-results/"
  echo "HTML Report: test-results/index.html"
  echo ""
fi

echo "========================================="
