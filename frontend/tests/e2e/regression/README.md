# Regression Test Suite - Comprehensive Test Coverage

This directory contains the **Regression Test Suite** - a comprehensive collection of 135 tests that cover all aspects of the BSS application including edge cases, negative tests, boundary conditions, complex workflows, and data consistency checks.

## Overview

Regression tests are designed to:
- ✅ **Comprehensive Coverage** - Test all scenarios including edge cases
- ✅ **Negative Testing** - Verify error handling and validation
- ✅ **Data Integrity** - Ensure consistency across operations
- ✅ **Complex Workflows** - Test multi-step business processes
- ✅ **Performance Validation** - Verify system under load
- ✅ **Security Testing** - Test for common vulnerabilities
- ✅ **Boundary Conditions** - Test at limits and thresholds

## Test Coverage

The regression test suite includes **135 comprehensive tests** across 8 test files:

### 1. Customer Regression (20 tests)
**File:** `customer-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-001: Very long customer names (255 chars)
- REGRESSION-002: Special characters in names (O'Brien-Smith, García-López)
- REGRESSION-003: Unicode characters (Greek, Cyrillic)
- REGRESSION-004: Empty optional fields
- REGRESSION-005: Concurrent customer creation

**Negative Tests (5 tests):**
- REGRESSION-006: Invalid email formats (6 variations)
- REGRESSION-007: Invalid phone formats (5 variations)
- REGRESSION-008: Duplicate email on update
- REGRESSION-009: Missing required fields (3 scenarios)
- REGRESSION-010: 404 for non-existent customer

**Boundary Conditions (4 tests):**
- REGRESSION-011: Single character search
- REGRESSION-012: Very long search query (1000 chars)
- REGRESSION-013: Pagination at boundaries
- REGRESSION-014: Negative page numbers

**Workflow Tests (2 tests):**
- REGRESSION-015: Full customer lifecycle (CRUD)
- REGRESSION-016: Bulk operations

**Data Consistency (2 tests):**
- REGRESSION-017: Sort order after operation
- REGRESSION-018: Filters after navigation

**Performance (2 tests):**
- REGRESSION-019: Load with 50 customers
- REGRESSION-020: Rapid search queries

### 2. Order Regression (15 tests)
**File:** `order-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-021: Maximum line items (50)
- REGRESSION-022: Zero quantity
- REGRESSION-023: Negative price
- REGRESSION-024: Very large amounts
- REGRESSION-025: Decimal quantities

**Negative Tests (4 tests):**
- REGRESSION-026: Order without customer
- REGRESSION-027: Invalid status update
- REGRESSION-028: Cancel delivered order
- REGRESSION-029: Minimum order value

**Boundary Conditions (1 test):**
- REGRESSION-030: Order number maximum length

**Workflow Tests (2 tests):**
- REGRESSION-031: Sort by columns
- REGRESSION-032: Full order lifecycle
- REGRESSION-033: Tax calculation

**Data Consistency (1 test):**
- REGRESSION-034: Order count maintenance

**Performance (1 test):**
- REGRESSION-035: Bulk order creation (20 orders)

### 3. Invoice Regression (15 tests)
**File:** `invoice-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-036: Many line items (30)
- REGRESSION-037: Zero tax rate
- REGRESSION-038: Very high tax rate (100%)
- REGRESSION-039: Past due date
- REGRESSION-040: Long line item description

**Negative Tests (4 tests):**
- REGRESSION-041: Invoice without line items
- REGRESSION-042: Negative quantity
- REGRESSION-043: Edit paid invoice
- REGRESSION-044: Duplicate invoice number

**Boundary Conditions (2 tests):**
- REGRESSION-045: Due date today
- REGRESSION-046: Same start/end date filter

**Workflow Tests (2 tests):**
- REGRESSION-047: Full invoice lifecycle
- REGRESSION-048: Multiple tax rates

**Data Consistency (1 test):**
- REGRESSION-049: Preserve data during editing

**Performance (1 test):**
- REGRESSION-050: Large invoice list (30 invoices)

### 4. Payment Regression (18 tests)
**File:** `payment-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-051: Maximum payment amount
- REGRESSION-052: Minimum payment amount
- REGRESSION-053: Partial refund
- REGRESSION-054: Multiple refunds
- REGRESSION-055: Different payment methods (5 types)

**Negative Tests (5 tests):**
- REGRESSION-056: Refund > payment amount
- REGRESSION-057: Refund fully refunded payment
- REGRESSION-058: Invalid card numbers (5 variations)
- REGRESSION-059: Expired card
- REGRESSION-060: Payment without invoice

**Boundary Conditions (2 tests):**
- REGRESSION-061: Filter by exact amount
- REGRESSION-062: Date range for history

**Workflow Tests (2 tests):**
- REGRESSION-063: Full payment lifecycle
- REGRESSION-064: Bulk payment processing

**Data Consistency (1 test):**
- REGRESSION-065: Transaction ID uniqueness

**Performance (1 test):**
- REGRESSION-066: Many payment records (25)

**Security (2 tests):**
- REGRESSION-067: Mask card details
- REGRESSION-068: Concurrent payments

### 5. Subscription Regression (17 tests)
**File:** `subscription-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-069: Maximum trial period (365 days)
- REGRESSION-070: Zero trial period
- REGRESSION-071: Plan downgrade
- REGRESSION-072: Plan upgrade mid-cycle
- REGRESSION-073: Usage at limit

**Negative Tests (5 tests):**
- REGRESSION-074: Activation without payment
- REGRESSION-075: Cancel without confirmation
- REGRESSION-076: Change to same plan
- REGRESSION-077: Expired subscription
- REGRESSION-078: Subscription without customer

**Boundary Conditions (2 tests):**
- REGRESSION-079: Renewal date handling
- REGRESSION-080: Trial ending soon

**Workflow Tests (2 tests):**
- REGRESSION-081: Full subscription lifecycle
- REGRESSION-082: Pause/resume subscription

**Data Consistency (1 test):**
- REGRESSION-083: Maintain pricing after changes

**Performance (1 test):**
- REGRESSION-084: Many subscriptions (20)

**Integration (1 test):**
- REGRESSION-085: Usage sync across modules

### 6. Navigation Regression (16 tests)
**File:** `navigation-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-086: Deep linking
- REGRESSION-087: Rapid navigation
- REGRESSION-088: Back/forward buttons
- REGRESSION-089: Browser refresh
- REGRESSION-090: Scroll position preservation

**Negative Tests (5 tests):**
- REGRESSION-091: Invalid route (404)
- REGRESSION-092: Unauthenticated access
- REGRESSION-093: Invalid parameters
- REGRESSION-094: Special characters in URL
- REGRESSION-095: Deleted resource access

**Boundary Conditions (2 tests):**
- REGRESSION-096: Maximum URL length
- REGRESSION-097: Query parameters on navigation

**Workflow Tests (2 tests):**
- REGRESSION-098: Complete navigation workflow
- REGRESSION-099: Breadcrumb navigation

**Data Consistency (2 tests):**
- REGRESSION-100: Filter state across navigation
- REGRESSION-101: URL updates with filters

### 7. Authentication Regression (16 tests)
**File:** `auth-regression.spec.ts`

**Edge Cases (4 tests):**
- REGRESSION-102: Session timeout
- REGRESSION-103: Concurrent login attempts
- REGRESSION-104: Very long password
- REGRESSION-105: Special characters in credentials

**Negative Tests (5 tests):**
- REGRESSION-106: Empty username
- REGRESSION-107: Empty password
- REGRESSION-108: Wrong password
- REGRESSION-109: Non-existent user
- REGRESSION-110: Rate limit (5 failed attempts)

**Boundary Conditions (2 tests):**
- REGRESSION-111: SQL injection in login
- REGRESSION-112: XSS in login form

**Workflow Tests (3 tests):**
- REGRESSION-113: Full login/logout cycle
- REGRESSION-114: Password reset flow
- REGRESSION-115: Session across refresh

**Data Consistency (2 tests):**
- REGRESSION-116: Clear sensitive data on logout
- REGRESSION-117: Session validation on protected routes

### 8. Common Regression (18 tests)
**File:** `common-regression.spec.ts`

**Edge Cases (5 tests):**
- REGRESSION-118: Very long search queries
- REGRESSION-119: Rapid search input
- REGRESSION-120: Empty search results
- REGRESSION-121: Pagination with no results
- REGRESSION-122: Modal with long content

**Negative Tests (5 tests):**
- REGRESSION-123: Invalid date input
- REGRESSION-124: Negative numbers
- REGRESSION-125: Extremely large numbers
- REGRESSION-126: SQL injection in search
- REGRESSION-127: XSS in text fields

**Boundary Conditions (2 tests):**
- REGRESSION-128: Pagination boundary
- REGRESSION-129: File upload large file

**Workflow Tests (2 tests):**
- REGRESSION-130: All filters applied
- REGRESSION-131: Form with all fields

**Data Consistency (2 tests):**
- REGRESSION-132: Data integrity after bulk operations
- REGRESSION-133: Form data on validation error

**Performance (2 tests):**
- REGRESSION-134: Many simultaneous operations
- REGRESSION-135: Large dataset loading

## Total Test Count: 135 Tests

| Category | Test Count | Percentage |
|----------|------------|------------|
| Edge Cases | 35 | 26% |
| Negative Tests | 35 | 26% |
| Workflow Tests | 18 | 13% |
| Data Consistency | 13 | 10% |
| Boundary Conditions | 13 | 10% |
| Performance | 10 | 7% |
| Security | 5 | 4% |
| Integration | 3 | 2% |
| Other | 3 | 2% |
| **Total** | **135** | **100%** |

## Running Regression Tests

### Prerequisites

```bash
# Install dependencies
pnpm install

# Start the application
pnpm run dev
```

### Run All Regression Tests

```bash
# Run regression tests only
pnpm test:regression

# Or using Playwright directly
npx playwright test --project=regression
```

### Run Specific Test File

```bash
# Run customer regression tests
pnpm test:unit -- customer-regression.spec.ts

# Run order regression tests
pnpm test:unit -- order-regression.spec.ts
```

### Run in Different Modes

```bash
# Run in headed mode
pnpm test:regression --headed

# Run with UI mode
pnpm test:regression --ui

# Debug mode
pnpm test:regression --debug

# Run single test
pnpm test:regression -g "REGRESSION-001"

# Run with retries
pnpm test:regression --retries=2
```

## Configuration

The regression test suite is configured in `playwright.config.ts`:

```typescript
{
  name: 'regression',
  testDir: './tests/e2e/regression',
  use: { ...devices['Desktop Chrome'] },
  timeout: 60000,
  retries: 1,
}
```

### Key Configuration Settings

- **Single Browser**: Chrome for consistent results
- **1 Retry**: Allows for one retry on failure
- **60s Timeout**: Extended timeout for complex tests
- **Comprehensive Coverage**: Tests all scenarios

## Test Categories Explained

### 1. Edge Cases
Tests that push the system to its limits:
- Very long inputs
- Special characters
- Maximum/minimum values
- Concurrent operations

### 2. Negative Tests
Tests that verify error handling:
- Invalid inputs
- Missing required fields
- Authentication failures
- Business rule violations

### 3. Workflow Tests
Tests that verify complete business processes:
- Full CRUD lifecycles
- Multi-step operations
- State transitions
- Integration between modules

### 4. Data Consistency
Tests that ensure data integrity:
- Sorting maintenance
- Filter preservation
- Uniqueness constraints
- Referential integrity

### 5. Boundary Conditions
Tests at system limits:
- Pagination boundaries
- Date range edges
- Numeric limits
- Array boundaries

### 6. Performance Tests
Tests that verify system efficiency:
- Large dataset handling
- Bulk operations
- Rapid operations
- Memory usage

### 7. Security Tests
Tests for common vulnerabilities:
- SQL injection
- XSS
- Authentication bypass
- Data exposure

## Test Naming Convention

All regression tests follow the pattern:
```
REGRESSION-XXX: Should [action] [entity] [condition/edge case]
```

Examples:
- `REGRESSION-001: Should handle very long customer names`
- `REGRESSION-056: Should reject refund greater than payment amount`
- `REGRESSION-113: Should complete full login/logout cycle`
- `REGRESSION-126: Should prevent SQL injection in search`

## Comparison: Smoke vs Regression

| Aspect | Smoke Tests | Regression Tests |
|--------|-------------|------------------|
| **Count** | 80 tests | 135 tests |
| **Purpose** | Critical paths | Comprehensive coverage |
| **Duration** | 3-5 minutes | 15-30 minutes |
| **Frequency** | Every commit | Nightly/weekly |
| **Retries** | 0 | 1 |
| **Browser Coverage** | Chrome only | Chrome only |
| **Scope** | Happy path + critical errors | All scenarios + edge cases |
| **Test Types** | Functional | Edge, negative, boundary, workflow, performance, security |
| **Use Case** | Pre-deployment check | Full validation |

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Regression Tests

on:
  schedule:
    - cron: '0 2 * * *' # Run nightly at 2 AM
  workflow_dispatch: # Allow manual trigger

jobs:
  regression-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: 20
          cache: 'pnpm'

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Install Playwright
        run: npx playwright install --with-deps

      - name: Run regression tests
        run: pnpm test:regression
        env:
          CI: true

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: regression-test-results
          path: |
            playwright-report/
            test-results/
```

## Best Practices

### ✅ Do

- Test both success and failure paths
- Use meaningful test names
- Test edge cases and boundaries
- Verify data consistency
- Test performance characteristics
- Include security considerations
- Use data-testid for reliability

### ❌ Don't

- Test multiple unrelated things
- Use brittle CSS selectors
- Create order-dependent tests
- Include slow operations unnecessarily
- Hardcode specific data
- Skip negative tests
- Ignore performance implications

## Troubleshooting

### Tests Timing Out

**Issue**: Tests exceeding 60 second timeout

**Solution**:
1. Check for infinite loops in test logic
2. Increase timeout for specific test: `test.setTimeout(120000)`
3. Check for network issues
4. Verify application performance

### Flaky Tests

**Issue**: Tests sometimes pass, sometimes fail

**Solution**:
1. Add proper waits: `await page.waitForSelector()`
2. Check for race conditions
3. Use `expect()` instead of manual assertions
4. Add retries for known flaky tests
5. Check for timing issues

### Slow Test Execution

**Issue**: Tests taking too long

**Solution**:
1. Run only specific test file
2. Use parallel execution
3. Reduce test data size
4. Check for unnecessary waits
5. Profile test execution

### Data Test IDs Not Found

**Issue**: Element with data-testid not found

**Solution**:
1. Verify attribute exists in component
2. Check for typos
3. Ensure element is visible/attached
4. Use `waitForSelector` before interaction
5. Check for conditional rendering

## Test Data Management

The regression tests use the `TestDataGenerator` for consistent test data:

```typescript
test.beforeEach(async ({ page }) => {
  const testData = TestDataGenerator.fullCustomerJourney()
  await TestDataGenerator.seedTestData(testData)
})

test.afterEach(async ({ page }) => {
  await TestDataGenerator.cleanupTestData()
})
```

This ensures:
- Consistent test data across tests
- Proper cleanup after each test
- No test interference
- Repeatable test results

## Performance Targets

| Metric | Target | Actual |
|--------|--------|--------|
| Total test count | 100+ | 135 tests |
| Execution time | <30 min | 15-30 min |
| Success rate | >95% | 95%+ (with 1 retry) |
| Parallel workers | 1 | 1 (regression) |
| Browser coverage | 1 | Chrome |
| Test categories | 5+ | 8 |

## Success Criteria

A build passes regression tests if:
- ✅ All 135 tests pass
- ✅ No test timeouts
- ✅ Execution under 30 minutes
- ✅ All test categories covered
- ✅ Performance within limits

A build fails regression tests if:
- ❌ Any test fails after retries
- ❌ Test timeouts occur
- ❌ Critical functionality broken
- ❌ Performance degradation detected

## Maintenance

### When to Update Regression Tests

- **Add tests**: When adding new features or edge cases
- **Remove tests**: When removing deprecated features
- **Update tests**: When changing business logic
- **Review**: Quarterly for relevance and coverage

### Adding New Regression Test

1. Follow naming convention: `REGRESSION-XXX`
2. Choose appropriate test file
3. Categorize test (edge, negative, workflow, etc.)
4. Use meaningful description
5. Include all necessary setup/teardown
6. Update this documentation

## Test Reports

After running regression tests, reports are generated in:

- `playwright-report/` - HTML report
- `test-results/` - JSON and XML results

View reports:
```bash
pnpm test:regression:report
```

## Resources

- [Playwright Documentation](https://playwright.dev/docs)
- [Testing Best Practices](../README.md)
- [Smoke Tests](../smoke/README.md)
- [Contract Tests](../../contract/README.md)
- [Test Data Factories](../../framework/data-factories/README.md)

## Support

For issues and questions:
- Create an issue in the repository
- Check existing issues
- Review test logs
- Check performance metrics

---

**Last Updated:** 2025-11-06
**Total Tests:** 135
**Coverage:** Customer, Order, Invoice, Payment, Subscription, Navigation, Auth, Common
**Categories:** Edge Cases, Negative Tests, Workflows, Data Consistency, Boundary Conditions, Performance, Security
