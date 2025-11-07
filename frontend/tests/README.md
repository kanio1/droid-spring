# Testing Framework - Complete Guide

## 🎯 Overview

This directory contains a comprehensive testing framework for the BSS (Business Support System) frontend. The framework uses **Playwright** for E2E testing, **Vitest** for unit testing, and provides a complete set of utilities and helpers.

## 📁 Directory Structure

```
tests/
├── data/                      # Static test data
│   ├── customers.json
│   ├── products.json
│   ├── orders.json
│   ├── invoices.json
│   ├── payments.json
│   ├── subscriptions.json
│   ├── addresses.json
│   ├── customer-import.csv
│   ├── product-import.csv
│   └── test-scenarios.json
│
├── e2e/                       # End-to-end tests
│   ├── login-flow.spec.ts
│   ├── customer-flow.spec.ts
│   ├── orders-flow.spec.ts
│   ├── invoices-flow.spec.ts
│   ├── payments-flow.spec.ts
│   ├── subscriptions-flow.spec.ts
│   └── ...
│
├── framework/                 # Testing framework utilities
│   ├── data-factories/        # Object Mother pattern for test data
│   ├── matchers/              # Custom Playwright matchers
│   ├── api-testing/           # API client utilities
│   ├── accessibility/         # a11y testing with axe-core
│   ├── testcontainers/        # Docker container integration
│   └── utils/                 # POM, visual regression, etc.
│
├── helpers/                   # Test helper utilities
│   ├── auth.helper.ts         # Authentication helpers
│   ├── network.helper.ts      # Network mocking utilities
│   ├── date.helper.ts         # Date/time utilities
│   ├── file-upload.helper.ts  # File upload helpers
│   ├── console.helper.ts      # Console logging utilities
│   └── error.helper.ts        # Error handling utilities
│
├── examples/                  # Test examples and patterns
│   ├── accessibility-testing.spec.ts
│   ├── cross-browser-testing.spec.ts
│   ├── performance-testing.spec.ts
│   └── mobile-testing.spec.ts
│
├── unit/                      # Unit tests (Vitest)
├── components/                # Component tests
├── visual/                    # Visual regression tests
├── global-setup.ts            # Global test setup
├── global-teardown.ts         # Global test cleanup
└── README.md                  # This file
```

## 🚀 Quick Start

### 1. Install Dependencies

```bash
# From frontend directory
cd frontend

# Install all dependencies including test dependencies
pnpm install

# Install Playwright browsers
npx playwright install --with-deps
```

### 2. Run Tests

```bash
# Run all E2E tests
pnpm test:e2e

# Run tests in UI mode
pnpm test:e2e:ui

# Run tests in specific browser
pnpm test:e2e:chrome
pnpm test:e2e:firefox
pnpm test:e2e:safari

# Run tests in headed mode
pnpm test:e2e:headed

# Run visual regression tests
pnpm test:visual

# Run accessibility tests
pnpm test:accessibility

# Run performance tests
pnpm test:performance

# Run unit tests
pnpm test:unit

# Run all tests
pnpm test:e2e && pnpm test:unit
```

### 3. Debug Tests

```bash
# Run in debug mode
pnpm test:e2e:debug

# Open Playwright UI
pnpm test:e2e:ui

# View test report
pnpm test:e2e:report
```

## 🛠️ Core Components

### Data Factories

Generate realistic test data using the **Object Mother** pattern:

```typescript
import { CustomerFactory, OrderFactory } from './framework/data-factories'

// Generate single customer
const customer = CustomerFactory.create()
  .withEmail('test@example.com')
  .active()
  .build()

// Generate multiple customers
const customers = CustomerFactory.create()
  .buildMany(10)

// Generate full customer journey
const journey = TestDataGenerator.fullCustomerJourney({
  customerStatus: 'active',
  orderStatus: 'delivered',
  invoiceStatus: 'paid'
})
```

**Available Factories:**
- `CustomerFactory` - Customer entities
- `OrderFactory` - Orders with items
- `InvoiceFactory` - Invoice records
- `PaymentFactory` - Payment transactions
- `SubscriptionFactory` - Subscription plans

### Page Object Model (POM)

Structured page objects for UI interaction:

```typescript
import { CustomerPage } from './framework/utils/page-object-model'

test('create customer', async ({ page }) => {
  const customerPage = new CustomerPage(page)
  await customerPage.navigateTo()

  const customerId = await customerPage.create({
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    status: 'active'
  })

  await expect(customerPage).toHaveCustomer(customerId)
})
```

**Available Page Objects:**
- `CustomerPage` - Customer CRUD operations
- `InvoicePage` - Invoice management
- `SubscriptionPage` - Subscription operations
- `DashboardPage` - Dashboard metrics

### Custom Matchers

Domain-specific matchers for assertions:

```typescript
import { registerCustomMatchers } from './framework/matchers/playwright-matchers'

// Register in test setup
registerCustomMatchers()

// Use in tests
await expect(page).toHaveCustomerStatus('active')
await expect(invoice).toBePaidInvoice()
await expect(subscription).toHaveActiveSubscription()
await expect(page).toHaveNoValidationErrors()
```

### API Testing

Typed API client with schema validation:

```typescript
import { ApiClient } from './framework/api-testing/api-client'

const api = new ApiClient({
  baseURL: 'http://localhost:3000/api',
  authToken: 'your-token'
})

// GET request
const customer = await api.customers.getById('cust-001')

// POST request
const newCustomer = await api.customers.create({
  firstName: 'John',
  lastName: 'Doe',
  email: 'john@example.com'
})

// Verify response
await expect(api.customers.getById('cust-001')).toSucceed()
```

### Helper Utilities

#### Authentication Helper

```typescript
import { AuthHelper } from './helpers'

// Login
await AuthHelper.login(page, {
  username: 'testuser',
  password: 'testpass'
})

// Check if logged in
const isLoggedIn = await AuthHelper.isLoggedIn(page)

// Logout
await AuthHelper.logout(page)
```

#### Network Mocking

```typescript
import { NetworkHelper } from './helpers'

// Mock API response
const cleanup = await NetworkHelper.mockApiRequests(page, [{
  urlPattern: '/api/customers',
  response: {
    status: 200,
    body: { id: '1', name: 'Test' }
  }
}])

// Wait for API call
const request = await NetworkHelper.waitForApiCall(
  page,
  '/api/customers',
  { method: 'GET' }
)
```

#### File Upload

```typescript
import { FileUploadHelper } from './helpers'

// Create test CSV
const csvPath = FileUploadHelper.createCSVFile('customers', [
  { firstName: 'John', email: 'john@example.com' }
])

// Upload file
await FileUploadHelper.uploadFile(page, 'input[type="file"]', csvPath)

// Verify upload
await FileUploadHelper.verifyFileUpload(page, 'customers.csv')
```

#### Error Handling

```typescript
import { ErrorHelper } from './helpers'

// Assert validation errors
await ErrorHelper.assertValidationErrors(page, {
  firstName: 'First name is required',
  email: 'Valid email is required'
})

// Retry operation
const result = await ErrorHelper.retry(
  () => page.click('[data-testid="retry-button"]'),
  3,
  1000
)
```

### Test Data

Static test data in `tests/data/`:

```typescript
import customers from './data/customers.json'
import * as fs from 'fs'

// Load static data
const testData = JSON.parse(
  fs.readFileSync('./data/customers.json', 'utf-8')
)

// Use in test
test('test with data', async ({ page }) => {
  const customer = testData[0]
  await page.goto(`/customers/${customer.id}`)
})
```

### Testcontainers Integration

#### Keycloak (OIDC Authentication)

```typescript
import { KeycloakContainer } from './framework/testcontainers/keycloak'

let keycloak: KeycloakTestContainer

test.beforeAll(async () => {
  keycloak = await KeycloakContainer.start({
    importRealm: {
      realm: 'test',
      users: [
        {
          username: 'testuser',
          password: 'testpass',
          enabled: true
        }
      ]
    }
  })
})

test('login with Keycloak', async ({ page }) => {
  await page.goto(keycloak.getRealmUrl('test'))
  // ... test logic
})
```

#### Redis (Caching)

```typescript
import { RedisContainer } from './framework/testcontainers/redis'

test('redis operations', async () => {
  const redis = await RedisContainer.start()

  // Set value
  await redis.set('key', 'value')

  // Get value
  const value = await redis.get('key')
  expect(value).toBe('value')
})
```

## 📊 Test Types

### E2E Tests (`tests/e2e/`)

Test complete user workflows:

```typescript
test('complete customer journey', async ({ page }) => {
  // 1. Login
  await AuthHelper.login(page, { username: 'user', password: 'pass' })

  // 2. Create customer
  await page.goto('/customers/create')
  await page.fill('[name="firstName"]', 'John')
  await page.click('[data-testid="save-button"]')

  // 3. Verify customer appears in list
  await expect(page.locator('[data-testid="customer-list"]'))
    .toContainText('John')
})
```

### Visual Regression (`tests/visual/`)

Detect visual changes:

```typescript
test('dashboard looks correct', async ({ page }) => {
  await page.goto('/dashboard')
  await expect(page).toHaveScreenshot('dashboard.png')
})
```

### Accessibility (`tests/examples/accessibility-testing.spec.ts`)

WCAG 2.1 compliance:

```typescript
test('page is accessible', async ({ page }) => {
  await page.goto('/customers')
  await expect(page).toBeAccessible()
})

test('form has proper labels', async ({ page }) => {
  await page.goto('/customers/create')
  const labelCheck = await AccessibilityTest.checkFormLabels(page, 'form')
  expect(labelCheck.hasLabels).toBe(true)
})
```

### Performance (`tests/examples/performance-testing.spec.ts`)

Core Web Vitals:

```typescript
test('page loads quickly', async ({ page }) => {
  await page.goto('/dashboard')

  const lcp = await page.evaluate(() => {
    return new Promise<number>((resolve) => {
      new PerformanceObserver((list) => {
        const entries = list.getEntries()
        resolve(entries[entries.length - 1].startTime)
      }).observe({ entryTypes: ['largest-contentful-paint'] })
    })
  })

  expect(lcp).toBeLessThan(2500) // 2.5s
})
```

### Cross-Browser (`tests/examples/cross-browser-testing.spec.ts`)

Test across browsers:

```typescript
test('works in all browsers', async ({ page, browserName }) => {
  await page.goto('/customers')

  // Browser-specific checks
  if (browserName === 'webkit') {
    // Safari-specific logic
  }

  await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
})
```

## 🔧 Configuration

### Playwright Config (`playwright.config.ts`)

```typescript
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 1,
  reporter: [
    ['html'],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/results.xml' }]
  ],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] }},
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }},
    { name: 'webkit', use: { ...devices['Desktop Safari'] }},
    { name: 'mobile-chrome', use: { ...devices['Pixel 5'] }},
    { name: 'mobile-safari', use: { ...devices['iPhone 12'] }}
  ]
})
```

### Environment Variables

```bash
# .env.test
KEYCLOAK_URL=http://localhost:8080/realms/bss-test
KEYCLOAK_CLIENT_ID=bss-frontend
REDIS_URL=redis://localhost:6379
BASE_URL=http://localhost:3000
```

## 📝 Writing Tests

### Test Structure

```typescript
import { test, expect } from '@playwright/test'

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Setup before each test
    await page.goto('/')
  })

  test('should do something', async ({ page }) => {
    // Test logic
    await page.click('[data-testid="button"]')
    await expect(page.locator('[data-testid="result"]')).toBeVisible()
  })

  test('should handle error', async ({ page }) => {
    // Error handling test
  })
})
```

### Best Practices

1. **Use `data-testid` attributes** for element selection
2. **Organize tests with `test.describe`** for logical grouping
3. **Use custom matchers** for domain-specific assertions
4. **Generate test data** with factories instead of hard-coding
5. **Mock external dependencies** for isolation
6. **Add assertions** for all user actions
7. **Use page objects** for complex workflows
8. **Write readable test names** that describe the scenario
9. **Clean up test data** in `test.afterEach` or `test.afterAll`
10. **Add comments** for complex test logic

## 🎨 Data Test IDs

Use `data-testid` attributes in your application for reliable element selection:

```html
<!-- Good -->
<button data-testid="save-customer-button">Save</button>
<div data-testid="customer-list"></div>

<!-- Avoid -->
<button class="btn-primary btn-large">Save</button>
<div class="table-container">
```

## 📦 CI/CD Integration

Tests run automatically in GitHub Actions on every push and PR. See `.github/workflows/e2e-tests.yml`.

## 🐛 Debugging Tests

### Common Debugging Techniques

1. **Headed mode**: Run tests with `--headed` to see browser
2. **Step-by-step**: Use `await page.pause()` to pause execution
3. **Screenshots**: Auto-captured on failure
4. **Trace viewer**: Run with `--trace on-first-retry`
5. **Console logs**: Check browser console for errors

### Debug Commands

```bash
# Debug specific test
pnpm test:e2e --debug customer.spec.ts

# Run with tracing
pnpm test:e2e --trace on

# Run in UI mode
pnpm test:e2e:ui

# View report
pnpm test:e2e:report
```

## 📚 Resources

- [Playwright Documentation](https://playwright.dev/)
- [Vitest Documentation](https://vitest.dev/)
- [Testing Library](https://testing-library.com/)
- [axe-core Accessibility](https://github.com/dequelabs/axe-core)

## 🤝 Contributing

When adding new tests:

1. Follow the existing structure
2. Use data factories for test data
3. Add appropriate assertions
4. Update this README if adding new components
5. Run all tests before committing

## 📊 Test Coverage

Run coverage reports:

```bash
# Unit test coverage
pnpm test:unit:coverage

# Generate coverage report
pnpm test:coverage
```

## 🎯 Test Priorities

- **Critical**: Authentication, payment, core CRUD operations
- **High**: Customer management, data entry, navigation
- **Medium**: Visual regression, accessibility, performance
- **Low**: Edge cases, error scenarios, documentation

## 📞 Support

For questions or issues:
- Check existing tests for examples
- Review framework utilities
- Read Playwright documentation
- Open an issue

---

**Happy Testing! 🎉**
