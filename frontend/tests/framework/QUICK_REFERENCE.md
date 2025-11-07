# Playwright Testing - Quick Reference Card

## 🚀 Essential Commands

```bash
# Start UI Mode
pnpm run test:e2e:ui

# Debug tests
pnpm run test:e2e:debug

# Generate test code
pnpm run test:e2e:codegen

# View traces
pnpm run test:e2e:trace

# View report
pnpm run test:e2e:report

# Run specific project
pnpm run test:e2e:api        # API tests
pnpm run test:e2e:network    # Network tests
pnpm run test:e2e:security-advanced  # Security tests
pnpm run test:e2e:ai-ml      # AI/ML tests

# Parallel execution
pnpm run test:smoke          # Fast tests
pnpm run test:regression     # Full suite

# Cross-browser
pnpm run test:all-browsers
pnpm run test:e2e:chrome
pnpm run test:e2e:firefox
pnpm run test:e2e:safari
pnpm run test:e2e:mobile
```

## 📦 Import Paths

```typescript
// Test fixtures
import { test as base } from '@playwright/test'
import { testWithObservability } from '../framework/utils/test-observability'

// Utilities
import { slowNetwork, blockRequests, networkSimulator } from '../framework/utils/network-testing'
import { contractTester, customerSchema } from '../framework/utils/contract-testing'
import { testGenerator, testData } from '../framework/utils/test-generator'
import { measurePagePerformance, checkAccessibility } from '../framework/utils/test-observability'

// Example test data
import { faker } from '@faker-js/faker'
```

## 🧪 Test Patterns

### Basic Test
```typescript
test('should load page', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveTitle('My App')
  await expect(page.locator('h1')).toBeVisible()
})
```

### With Observability
```typescript
testWithObservability('should track performance', async ({ page, observability }) => {
  await page.goto('/dashboard')
  const metrics = await measurePagePerformance(page)
  console.log('Load time:', metrics.loadTime)
})
```

### With Network Simulation
```typescript
test('should work offline', async ({ page }) => {
  await networkSimulator.simulateOffline(page)
  await page.goto('/dashboard')
  await expect(page.locator('[data-testid="offline"]')).toBeVisible()
  await networkSimulator.simulateOnline(page)
})
```

### With Contract Testing
```typescript
test('should validate API', async ({ page }) => {
  const result = await contractTester.validateResponse(page, {
    url: 'http://localhost:3000/api/customers/123',
    expectedStatus: 200,
    expectedSchema: customerSchema,
    expectedFields: ['id', 'name', 'email']
  })
  expect(result.body).toHaveProperty('id')
})
```

### With Accessibility
```typescript
test('should be accessible', async ({ page }) => {
  await page.goto('/')
  const results = await checkAccessibility(page)
  expect(results.violations).toHaveLength(0)
})
```

## 🔧 Utility Functions

### Network Testing
```typescript
await slowNetwork(page)                           // Simulate 3G
await fastNetwork(page)                           // Simulate 4G
await networkSimulator.simulateOffline(page)     // Offline mode
await networkSimulator.mockRequests(page, [      // Mock API
  { url: '**/api/**', status: 200, response: {} }
])
await blockRequests(page, ['analytics', 'ads'])   // Block resources
await simulateAPIError(page, '**/api/**', 500)   // Simulate error
```

### Performance Testing
```typescript
const metrics = await measurePagePerformance(page)
console.log({
  loadTime: metrics.loadTime,
  domContentLoaded: metrics.domContentLoaded,
  firstPaint: metrics.firstPaint,
  memory: metrics.memory
})
```

### Accessibility Testing
```typescript
const results = await checkAccessibility(page)
console.log({
  violations: results.violations.length,
  passes: results.passes.length,
  incomplete: results.incomplete.length
})
```

### Test Data Generation
```typescript
const customer = testData.customer()
const order = testData.order(customer.id)
const user = testData.user()
const product = testData.product()
```

## 📊 Test Projects

| Project | Purpose | Command |
|---------|---------|---------|
| smoke | Fast critical tests | `pnpm run test:smoke` |
| chromium | Chrome browser | `pnpm run test:e2e:chrome` |
| firefox | Firefox browser | `pnpm run test:e2e:firefox` |
| webkit | Safari browser | `pnpm run test:e2e:safari` |
| mobile | Mobile devices | `pnpm run test:e2e:mobile` |
| regression | Full test suite | `pnpm run test:regression` |
| api | API testing | `pnpm run test:e2e:api` |
| network | Network conditions | `pnpm run test:e2e:network` |
| security-advanced | Security tests | `pnpm run test:e2e:security-advanced` |
| ai-ml | AI/ML features | `pnpm run test:e2e:ai-ml` |
| visual | Visual regression | `pnpm run test:visual` |

## 🎯 Test Structure

```
tests/
├── e2e/                    # End-to-end tests
│   ├── smoke/             # Smoke tests
│   ├── regression/        # Regression tests
│   ├── visual/            # Visual tests
│   ├── security/          # Security tests
│   └── resilience/        # Resilience tests
├── examples/              # Example implementations
│   ├── api-advanced.spec.ts
│   ├── network-advanced.spec.ts
│   ├── security-advanced.spec.ts
│   └── ai-ml.spec.ts
├── framework/             # Framework utilities
│   ├── utils/
│   │   ├── test-observability.ts
│   │   ├── network-testing.ts
│   │   ├── contract-testing.ts
│   │   └── test-generator.ts
│   ├── ADVANCED_TESTING_GUIDE.md
│   └── QUICK_REFERENCE.md
└── unit/                  # Unit tests
```

## 🔍 Common Selectors

```typescript
// Recommended: data-testid
page.locator('[data-testid="submit-button"]')

// Button
page.locator('button[type="submit"]')
page.locator('[data-testid="save"]')

// Input
page.locator('input[name="email"]')
page.locator('[data-testid="firstName"]')

// Link
page.locator('a[href="/dashboard"]')

// Wait for element
await expect(page.locator('[data-testid="loading"]')).toBeVisible()
await expect(page.locator('[data-testid="data"]')).toHaveText('Expected')
```

## ⚡ Quick Tips

1. **Use UI Mode for Development**
   ```bash
   pnpm run test:e2e:ui
   ```

2. **Debug Failing Tests**
   ```bash
   pnpm run test:e2e:debug
   ```

3. **Generate Test Code**
   ```bash
   pnpm run test:e2e:codegen
   ```

4. **View Test Traces**
   ```bash
   pnpm run test:e2e:trace
   ```

5. **Run Tests in Parallel**
   ```bash
   pnpm run test:smoke && pnpm run test:regression
   ```

6. **Check Accessibility**
   ```typescript
   const results = await checkAccessibility(page)
   expect(results.violations).toHaveLength(0)
   ```

7. **Track Performance**
   ```typescript
   const metrics = await measurePagePerformance(page)
   expect(metrics.loadTime).toBeLessThan(3000)
   ```

8. **Test Offline**
   ```typescript
   await networkSimulator.simulateOffline(page)
   // Test offline behavior
   await networkSimulator.simulateOnline(page)
   ```

9. **Mock API**
   ```typescript
   await page.route('**/api/customers', async route => {
     await route.fulfill({ json: { customers: [] } })
   })
   ```

10. **Use Faker Data**
    ```typescript
    const customer = testData.customer()
    // Returns: { firstName, lastName, email, phone, ... }
    ```

## 📚 Resources

- **Full Guide**: `tests/framework/ADVANCED_TESTING_GUIDE.md`
- **Playwright Docs**: https://playwright.dev
- **API Reference**: https://playwright.dev/docs/api
- **Best Practices**: https://playwright.dev/docs/best-practices

## 🐛 Troubleshooting

### Test Timeout
```typescript
// Increase timeout for slow tests
test('slow test', async ({ page }) => {
  test.setTimeout(60000)
  // ...
})
```

### Flaky Test
```typescript
// Add retry
test('flaky test', async ({ page }) => {
  test.flaky()
  // ...
})
```

### Debug Network
```typescript
// Log network activity
page.on('request', r => console.log('Request:', r.url()))
page.on('response', r => console.log('Response:', r.status()))
```

### View Error
```typescript
test('debug error', async ({ page }) => {
  page.on('console', msg => console.log('PAGE LOG:', msg.text()))
  page.on('pageerror', error => console.log('PAGE ERROR:', error.message))
  // ...
})
```

## 💡 Pro Tips

- Use `data-testid` attributes for reliable selectors
- Always test offline behavior for critical features
- Use test observability for performance monitoring
- Generate tests with codegen, then customize
- Run smoke tests before committing code
- Use sharding for faster CI/CD
- Check accessibility on every page
- Mock external APIs in tests
- Use Faker for realistic test data
- Track test metrics in CI/CD
