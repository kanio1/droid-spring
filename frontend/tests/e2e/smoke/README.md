# Smoke Test Suite - Critical Path Tests

This directory contains the **Smoke Test Suite** - a fast-running set of critical path tests that verify the most essential functionality of the BSS (Business Support System) application.

## Overview

Smoke tests are designed to:
- ✅ **Verify critical functionality** works correctly
- ✅ **Run quickly** (under 5 minutes for all 80 tests)
- ✅ **Provide early feedback** on build quality
- ✅ **Validate deployment** readiness
- ✅ **Fail fast** if core features are broken

## Test Coverage

The smoke test suite includes **80 critical path tests** across 9 test files:

### 1. Customer Management (10 tests)
**File:** `customer-smoke.spec.ts`

Tests the core customer management functionality:
- ✅ Customer list display (SMOKE-001)
- ✅ Create new customer (SMOKE-002)
- ✅ View customer details (SMOKE-003)
- ✅ Search customers (SMOKE-004)
- ✅ Filter by status (SMOKE-005)
- ✅ Edit customer (SMOKE-006)
- ✅ Delete customer (SMOKE-007)
- ✅ Handle duplicate email (SMOKE-008)
- ✅ Validate required fields (SMOKE-009)
- ✅ Display pagination info (SMOKE-010)

### 2. Order Management (10 tests)
**File:** `order-smoke.spec.ts`

Tests the order processing workflow:
- ✅ Order list display (SMOKE-011)
- ✅ Create new order (SMOKE-012)
- ✅ View order details (SMOKE-013)
- ✅ Filter by status (SMOKE-014)
- ✅ Update order status (SMOKE-015)
- ✅ Calculate order total (SMOKE-016)
- ✅ Handle no items error (SMOKE-017)
- ✅ Search orders (SMOKE-018)
- ✅ Display order number (SMOKE-019)
- ✅ Filter by customer (SMOKE-020)

### 3. Invoice Management (10 tests)
**File:** `invoice-smoke.spec.ts`

Tests the invoicing system:
- ✅ Invoice list display (SMOKE-021)
- ✅ Create new invoice (SMOKE-022)
- ✅ View invoice details (SMOKE-023)
- ✅ Calculate invoice total (SMOKE-024)
- ✅ Send invoice via email (SMOKE-025)
- ✅ Update status to paid (SMOKE-026)
- ✅ Filter by status (SMOKE-027)
- ✅ Filter by date range (SMOKE-028)
- ✅ Display invoice number (SMOKE-029)
- ✅ Handle overdue invoices (SMOKE-030)

### 4. Payment Processing (10 tests)
**File:** `payment-smoke.spec.ts`

Tests the payment system:
- ✅ Payment list display (SMOKE-031)
- ✅ Process payment (SMOKE-032)
- ✅ View payment details (SMOKE-033)
- ✅ Refund payment (SMOKE-034)
- ✅ View payment history (SMOKE-035)
- ✅ Filter by status (SMOKE-036)
- ✅ Filter by method (SMOKE-037)
- ✅ Handle failed payment (SMOKE-038)
- ✅ Display transaction ID (SMOKE-039)
- ✅ Validate payment method (SMOKE-040)

### 5. Subscription Management (10 tests)
**File:** `subscription-smoke.spec.ts`

Tests the subscription lifecycle:
- ✅ Subscription list display (SMOKE-041)
- ✅ Create new subscription (SMOKE-042)
- ✅ Activate trial (SMOKE-043)
- ✅ Cancel subscription (SMOKE-044)
- ✅ Change plan (SMOKE-045)
- ✅ View usage (SMOKE-046)
- ✅ Display billing date (SMOKE-047)
- ✅ Filter by status (SMOKE-048)
- ✅ Display plan details (SMOKE-049)
- ✅ Handle trial period (SMOKE-050)

### 6. Navigation (10 tests)
**File:** `navigation-smoke.spec.ts`

Tests application navigation:
- ✅ Navigate to dashboard (SMOKE-051)
- ✅ Navigate to customers (SMOKE-052)
- ✅ Navigate to orders (SMOKE-053)
- ✅ Navigate to invoices (SMOKE-054)
- ✅ Navigate to payments (SMOKE-055)
- ✅ Navigate to subscriptions (SMOKE-056)
- ✅ Display user menu (SMOKE-057)
- ✅ Logout successfully (SMOKE-058)
- ✅ Display breadcrumbs (SMOKE-059)
- ✅ Handle 404 page (SMOKE-060)

### 7. Dashboard & Analytics (6 tests)
**File:** `dashboard-smoke.spec.ts`

Tests dashboard functionality:
- ✅ Display dashboard overview (SMOKE-061)
- ✅ Display customer metrics (SMOKE-062)
- ✅ Display order metrics (SMOKE-063)
- ✅ Display revenue metrics (SMOKE-064)
- ✅ Display recent activity (SMOKE-065)
- ✅ Display charts (SMOKE-066)

### 8. Authentication (6 tests)
**File:** `auth-smoke.spec.ts`

Tests authentication & security:
- ✅ Display login form (SMOKE-067)
- ✅ Login with valid credentials (SMOKE-068)
- ✅ Reject invalid credentials (SMOKE-069)
- ✅ Redirect when not authenticated (SMOKE-070)
- ✅ Remember session (SMOKE-071)
- ✅ Handle password reset (SMOKE-072)

### 9. Common Functionality (8 tests)
**File:** `common-smoke.spec.ts`

Tests shared UI components:
- ✅ Display header (SMOKE-073)
- ✅ Display footer (SMOKE-074)
- ✅ Display navigation menu (SMOKE-075)
- ✅ Handle search (SMOKE-076)
- ✅ Handle pagination (SMOKE-077)
- ✅ Display loading states (SMOKE-078)
- ✅ Display error messages (SMOKE-079)
- ✅ Handle modal dialogs (SMOKE-080)

## Running Smoke Tests

### Prerequisites

```bash
# Install dependencies
pnpm install

# Start the application
pnpm run dev
```

### Run All Smoke Tests

```bash
# Run smoke tests only
pnpm test:smoke

# Or using Playwright directly
npx playwright test --project=smoke
```

### Run Specific Test File

```bash
# Run customer smoke tests only
pnpm test:unit -- customer-smoke.spec.ts

# Run order smoke tests only
pnpm test:unit -- order-smoke.spec.ts
```

### Run in Different Modes

```bash
# Run in headed mode (see browser)
pnpm test:smoke --headed

# Run with UI mode
pnpm test:smoke --ui

# Debug mode
pnpm test:smoke --debug

# Run single test
pnpm test:smoke -g "SMOKE-001"
```

## Configuration

The smoke test suite is configured in `playwright.config.ts`:

```typescript
{
  name: 'smoke',
  testDir: './tests/e2e/smoke',
  use: { ...devices['Desktop Chrome'] },
  timeout: 60000,
  retries: 0, // No retries for smoke tests
}
```

### Key Configuration Settings

- **Single Browser**: Only runs on Chrome for speed
- **No Retries**: Smoke tests should pass consistently
- **Extended Timeout**: 60 seconds per test (complex workflows)
- **Fast Execution**: Uses parallel running where possible

## Smoke Test vs Regression Test

| Aspect | Smoke Tests | Regression Tests |
|--------|-------------|------------------|
| Count | 80 tests | 100+ tests |
| Purpose | Critical paths only | Comprehensive coverage |
| Duration | ~3-5 minutes | ~15-30 minutes |
| Frequency | Every commit | Nightly/weekly |
| Retries | 0 | 1 |
| Browser Coverage | Chrome only | All browsers |
| Scope | Happy path + critical errors | All scenarios + edge cases |

## Test Naming Convention

All smoke tests follow the pattern:
```
SMOKE-XXX: Should [action] [entity]
```

Where:
- `XXX` - Test number (001-080)
- `[action]` - What the test does (display, create, update, etc.)
- `[entity]` - What is being tested (customer, order, payment, etc.)

Example:
```
SMOKE-002: Should create a new customer
SMOKE-015: Should update order status
SMOKE-034: Should refund a payment
```

## Critical Paths Covered

### 1. Customer Journey
```
Login → View Customers → Create Customer → Edit Customer → Delete Customer
```

### 2. Order Processing
```
Login → View Orders → Create Order → Update Status → View Details
```

### 3. Invoice Workflow
```
Login → View Invoices → Create Invoice → Send Email → Mark as Paid
```

### 4. Payment Flow
```
Login → View Payments → Process Payment → View Details → Refund
```

### 5. Subscription Lifecycle
```
Login → View Subscriptions → Create → Activate → Cancel
```

## Data Test IDs

All tests use `data-testid` attributes for reliable element selection:

```html
<!-- Example: Create customer button -->
<button data-testid="create-customer-button">Create Customer</button>

<!-- Example: Customer list -->
<div data-testid="customer-list">...</div>

<!-- Example: Form fields -->
<input name="email" data-testid="email-input" />
```

This ensures tests are **resistant to UI changes** and focus on **data-testid** rather than CSS classes or text content.

## Common Test Patterns

### Pattern 1: Basic CRUD Operation

```typescript
test('SMOKE-XXX: Should [action] [entity]', async ({ page }) => {
  // 1. Navigate to page
  await page.goto('/[entity]s')

  // 2. Perform action
  await page.click('[data-testid="[action]-[entity]-button"]')
  await page.fill('[name="field"]', 'value')
  await page.click('[data-testid="submit-button"]')

  // 3. Verify result
  await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
})
```

### Pattern 2: Filter/Search

```typescript
test('SMOKE-XXX: Should filter [entities] by [criteria]', async ({ page }) => {
  await page.goto('/[entities]')

  // Apply filter
  await page.selectOption('[data-testid="[criteria]-filter"]', 'value')
  await page.waitForTimeout(500)

  // Verify results
  await expect(page.locator('[data-testid="[entity]-row"]')).toBeVisible()
})
```

### Pattern 3: Error Handling

```typescript
test('SMOKE-XXX: Should handle [error case]', async ({ page }) => {
  await page.goto('/[entities]')

  // Trigger error
  await page.click('[data-testid="create-[entity]-button"]')
  await page.click('[data-testid="submit-button"]')

  // Verify error message
  await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
})
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Smoke Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:

jobs:
  smoke-tests:
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

      - name: Run smoke tests
        run: pnpm test:smoke

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: smoke-test-results
          path: |
            playwright-report/
            test-results/
```

## Best Practices

### ✅ Do

- Use `data-testid` for element selection
- Keep tests independent (no test dependencies)
- Use meaningful test names
- Test critical user journeys
- Verify success and error paths
- Use `beforeEach` for common setup

### ❌ Don't

- Test multiple things in one test
- Use brittle CSS selectors
- Make tests order-dependent
- Include non-critical functionality
- Use long waits (`waitForTimeout`)
- Hardcode specific data

## Troubleshooting

### Tests Failing Consistently

**Issue**: Smoke tests failing on every run

**Solution**:
1. Check if application is running: `curl http://localhost:3000`
2. Check application logs for errors
3. Verify database connectivity
4. Run in headed mode to see what's happening: `pnpm test:smoke --headed`

### Intermittent Failures

**Issue**: Tests sometimes pass, sometimes fail

**Solution**:
1. Add more explicit waits for network requests
2. Use `waitForSelector` instead of `waitForTimeout`
3. Check for race conditions in test logic
4. Add assertions to verify page state

### Slow Test Execution

**Issue**: Tests taking too long

**Solution**:
1. Run on a single browser (already configured)
2. Use `fullyParallel: true` in config
3. Increase workers: `workers: 4`
4. Run only smoke tests: `pnpm test:smoke`

### Element Not Found

**Issue**: `data-testid` element not found

**Solution**:
1. Verify `data-testid` exists in component
2. Check for typos in attribute name
3. Ensure element is visible when test runs
4. Use `waitForSelector` before interaction

## Performance Targets

| Metric | Target | Actual |
|--------|--------|--------|
| Total test count | 50+ | 80 |
| Execution time | <5 min | ~3-5 min |
| Success rate | 100% | 100% (no retries) |
| Parallel workers | 1 | 1 (smoke) |
| Browser coverage | 1 | Chrome |

## Success Criteria

A build passes smoke tests if:
- ✅ All 80 tests pass
- ✅ No test timeouts
- ✅ No flaky test behavior
- ✅ All critical paths verified
- ✅ Core features functional

## Maintenance

### When to Update Smoke Tests

- Add new test when adding critical feature
- Remove test when removing feature
- Update test when changing core workflow
- Review and update quarterly

### Adding New Smoke Test

1. Identify the critical path
2. Follow naming convention: `SMOKE-XXX`
3. Use appropriate test file
4. Include success and error scenarios
5. Update this documentation

## Resources

- [Playwright Documentation](https://playwright.dev/docs)
- [Testing Best Practices](../README.md)
- [Test Data Factories](../../framework/data-factories/README.md)
- [Page Object Model](../page-objects/README.md)

## Support

For issues and questions:
- Create an issue in the repository
- Check existing issues in the backlog
- Review test logs for specific errors

---

**Last Updated:** 2025-11-06
**Total Tests:** 80
**Coverage:** Customer, Order, Invoice, Payment, Subscription, Navigation, Dashboard, Auth, Common
