# 🚀 Playwright Advanced Testing Guide

**Date:** 2025-11-05
**Version:** 1.0
**Focus:** Advanced Playwright Features for BSS Project

---

## 📚 TABLE OF CONTENTS

1. [Getting Started](#-getting-started)
2. [Accessibility Testing](#-accessibility-testing)
3. [Mobile & Cross-Browser Testing](#-mobile--cross-browser-testing)
4. [Performance Testing](#-performance-testing)
5. [Network Throttling](#-network-throttling)
6. [API Testing](#-api-testing)
7. [Visual Regression](#-visual-regression)
8. [Best Practices](#-best-practices)

---

## 🎯 GETTING STARTED

### **Setup**

```bash
# Install Playwright (already installed)
pnpm add -D @playwright/test

# Install additional dependencies
pnpm add -D axe-core
pnpm add -D lighthouse
pnpm add -D pixelmatch
```

### **Configuration Update**

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 4,
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
    // Desktop
    { name: 'chromium', use: { ...devices['Desktop Chrome'] }},
    { name: 'firefox', use: { ...devices['Desktop Firefox'] }},
    { name: 'webkit', use: { ...devices['Desktop Safari'] }},

    // Mobile
    { name: 'mobile-chrome', use: { ...devices['Pixel 5'] }},
    { name: 'mobile-safari', use: { ...devices['iPhone 12'] }},
    { name: 'tablet', use: { ...devices['iPad Pro'] }},

    // Mobile Portrait/Landscape
    { name: 'mobile-chrome-portrait', use: { ...devices['Pixel 5'], orientation: 'portrait' }},
    { name: 'mobile-chrome-landscape', use: { ...devices['Pixel 5'], orientation: 'landscape' }},
  ],
  webServer: {
    command: 'pnpm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
  },
})
```

---

## ♿ ACCESSIBILITY TESTING

### **Why It Matters**
- **Legal Compliance:** WCAG 2.1 Level AA required
- **User Inclusion:** 1 in 7 people have accessibility needs
- **SEO Benefits:** Better search engine ranking
- **Quality Assurance:** Better UX for everyone

### **Implementation**

#### 1. Install axe-core
```bash
pnpm add -D @axe-core/playwright
```

#### 2. Create Accessibility Test
```typescript
// tests/framework/accessibility/axe-testing.ts
import { type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

export async function checkAccessibility(page: Page) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .analyze()

  expect(results.violations).toEqual([])
}
```

#### 3. Use in Tests
```typescript
// tests/e2e/customers.spec.ts
import { checkAccessibility } from '../framework/accessibility/axe-testing'

test('customers page is accessible', async ({ page }) => {
  await page.goto('/customers')
  await checkAccessibility(page)
})
```

#### 4. Advanced Accessibility Checks

```typescript
// Custom accessibility matchers
test('form has proper labels', async ({ page }) => {
  await page.goto('/customers/create')

  const inputs = await page.locator('input').all()
  for (const input of inputs) {
    const id = await input.getAttribute('id')
    if (id) {
      const label = page.locator(`label[for="${id}"]`)
      await expect(label).toBeVisible()
    }
  }
})

// Keyboard navigation
test('keyboard navigation works', async ({ page }) => {
  await page.goto('/customers')

  // Tab through elements
  await page.keyboard.press('Tab')
  let focused = await page.evaluate(() => document.activeElement?.tagName)
  expect(focused).toBe('INPUT')

  // Enter key should work
  await page.keyboard.press('Enter')
  // Verify action happened
})
```

### **Key Accessibility Tests to Add**
1. ✅ **Page-level checks** - All pages pass axe-core
2. ✅ **Form validation** - All inputs have labels
3. ✅ **Keyboard navigation** - Tab order is logical
4. ✅ **Color contrast** - Text meets 4.5:1 ratio
5. ✅ **Screen reader support** - ARIA labels present
6. ✅ **Focus management** - Visible focus indicators

---

## 📱 MOBILE & CROSS-BROWSER TESTING

### **Mobile Testing**

#### Device Profiles
```typescript
// playwright.config.ts
projects: [
  { name: 'iPhone 12', use: { ...devices['iPhone 12'] }},
  { name: 'Pixel 5', use: { ...devices['Pixel 5'] }},
  { name: 'iPad Pro', use: { ...devices['iPad Pro'] }},
]
```

#### Mobile-Specific Tests
```typescript
// tests/e2e/mobile.spec.ts
test.use({ ...devices['iPhone 12'] })

test('mobile menu works', async ({ page }) => {
  await page.goto('/customers')

  // Open hamburger menu
  await page.tap('[data-testid="mobile-menu-toggle"]')
  await expect(page.locator('.mobile-menu')).toBeVisible()

  // Navigate to page
  await page.tap('[data-testid="nav-orders"]')
  await expect(page).toHaveURL('/orders')
})

// Touch gestures
test('swipe to delete', async ({ page }) => {
  await page.goto('/customers')

  // Swipe left on card
  await page.swipe('[data-testid="customer-card"]', { x: 300, y: 0 })

  // Delete button appears
  await expect(page.locator('.delete-button')).toBeVisible()
})
```

### **Cross-Browser Testing**

#### Browser Coverage
```typescript
projects: [
  { name: 'chromium', use: { ...devices['Desktop Chrome'] }},
  { name: 'firefox', use: { ...devices['Desktop Firefox'] }},
  { name: 'webkit', use: { ...devices['Desktop Safari'] }},
  { name: 'edge', use: { ...devices['Desktop Edge'] }},
]
```

#### Browser-Specific Tests
```typescript
test('works in all browsers', async ({ page, browserName }) => {
  await page.goto('/customers')

  // Browser-specific validations
  if (browserName === 'webkit') {
    await expect(page.locator('.safari-specific')).toBeVisible()
  } else if (browserName === 'firefox') {
    await expect(page.locator('.firefox-specific')).toBeVisible()
  }
})
```

### **Key Tests to Add**
1. ✅ **Responsive design** - Layout adapts to screen size
2. ✅ **Touch interactions** - Swipe, pinch, tap work
3. ✅ **Mobile navigation** - Hamburger menu, tab bar
4. ✅ **Keyboard types** - Virtual keyboard behavior
5. ✅ **Orientation change** - Portrait/landscape support

---

## ⚡ PERFORMANCE TESTING

### **Core Web Vitals**

```typescript
// tests/e2e/performance.spec.ts
import { test, expect } from '@playwright/test'

test('page meets Core Web Vitals', async ({ page }) => {
  await page.goto('/dashboard')

  // LCP (Largest Contentful Paint)
  const lcp = await page.evaluate(() => {
    return new Promise<number>((resolve) => {
      new PerformanceObserver((list) => {
        const entries = list.getEntries()
        resolve(entries[entries.length - 1].startTime)
      }).observe({ entryTypes: ['largest-contentful-paint'] })
      setTimeout(() => resolve(0), 5000)
    })
  })
  expect(lcp).toBeLessThan(2500) // < 2.5s

  // CLS (Cumulative Layout Shift)
  const cls = await page.evaluate(() => {
    let cls = 0
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (!entry.hadRecentInput) {
          cls += entry.value
        }
      }
    }).observe({ entryTypes: ['layout-shift'] })
    setTimeout(() => {}, 1000)
    return cls
  })
  expect(cls).toBeLessThan(0.1)
})
```

### **Resource Loading**

```typescript
test('images are optimized', async ({ page }) => {
  await page.goto('/customers')

  const images = await page.locator('img').all()
  for (const img of images) {
    const naturalWidth = await img.evaluate(el => el.naturalWidth)
    expect(naturalWidth).toBeGreaterThan(0)
    expect(naturalWidth).toBeLessThanOrEqual(1920)
  }
})

test('bundle sizes are acceptable', async ({ page }) => {
  const [response] = await Promise.all([
    page.waitForResponse('**/*.js'),
    page.goto('/customers')
  ])

  const contentLength = response.headers()['content-length']
  expect(Number(contentLength) / 1024).toBeLessThan(500) // < 500KB
})
```

### **Key Performance Tests**
1. ✅ **LCP** - Largest Contentful Paint < 2.5s
2. ✅ **FID** - First Input Delay < 100ms
3. ✅ **CLS** - Cumulative Layout Shift < 0.1
4. ✅ **Image optimization** - Proper sizes, lazy loading
5. ✅ **Bundle size** - JavaScript/CSS < 500KB
6. ✅ **API response time** - < 500ms

---

## 🌐 NETWORK THROTTLING

### **Simulate Slow Network**

```typescript
test('works on slow 3G', async ({ page }) => {
  // Throttle CPU (simulate slow device)
  await page.context().setOffline(false)
  await page.route('**/*', route => setTimeout(() => route.continue(), 100))

  const start = Date.now()
  await page.goto('/customers')
  const loadTime = Date.now() - start

  expect(loadTime).toBeLessThan(10000) // < 10s on 3G
})

test('handles offline mode', async ({ page }) => {
  await page.goto('/customers')

  // Go offline
  await page.context().setOffline(true)

  // Should show offline message
  await expect(page.locator('[data-testid="offline-message"]')).toBeVisible()

  // Go back online
  await page.context().setOffline(false)
  await page.reload()

  // Should work normally
  await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
})
```

### **Custom Network Conditions**

```typescript
// tests/utils/network.ts
export async function simulateNetwork(page: Page, speed: 'fast' | 'slow' | 'offline') {
  if (speed === 'offline') {
    await page.context().setOffline(true)
  } else {
    await page.context().setOffline(false)
    await page.route('**/*', route => {
      const delay = speed === 'slow' ? 500 : 50
      setTimeout(() => route.continue(), delay)
    })
  }
}

// Use in tests
test('customer list on slow network', async ({ page }) => {
  await simulateNetwork(page, 'slow')
  await page.goto('/customers')
  // Test behavior on slow network
})
```

### **Key Network Tests**
1. ✅ **Slow 3G** - App usable on 3G
2. ✅ **Offline mode** - Graceful degradation
3. ✅ **Network errors** - Retry logic
4. ✅ **Large responses** - Pagination works
5. ✅ **Intermittent connection** - Reconnection handling

---

## 🔌 API TESTING

### **Typed API Client**

```typescript
// tests/framework/api/api-client.ts
import { APIRequestContext } from '@playwright/test'

export class ApiClient {
  private request: APIRequestContext

  constructor(private baseURL: string, private authToken?: string) {
    this.request = {} as APIRequestContext
  }

  async get<T>(endpoint: string): Promise<T> {
    const response = await this.request.get(`${this.baseURL}${endpoint}`)
    if (!response.ok()) {
      throw new Error(`API error: ${response.status()}`)
    }
    return response.json()
  }

  async post<T>(endpoint: string, data: any): Promise<T> {
    const response = await this.request.post(`${this.baseURL}${endpoint}`, {
      data
    })
    if (!response.ok()) {
      throw new Error(`API error: ${response.status()}`)
    }
    return response.json()
  }
}
```

### **API Mocking with MSW**

```typescript
// tests/utils/msw.ts
import { setupServer } from 'msw/node'
import { rest } from 'msw'

export const server = setupServer(
  rest.get('/api/customers', (req, res, ctx) => {
    return res(ctx.json([
      { id: 1, name: 'John', email: 'john@example.com' }
    ]))
  }),

  rest.post('/api/customers', (req, res, ctx) => {
    return res(ctx.json({ id: 2, ...req.body }))
  })
)

// Setup in tests
test.beforeAll(() => server.listen())
test.afterEach(() => server.resetHandlers())
test.afterAll(() => server.close())
```

### **GraphQL Testing**

```typescript
test('GraphQL query works', async ({ page }) => {
  const response = await page.evaluate(async () => {
    const result = await fetch('/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query: `
          query GetCustomer($id: ID!) {
            customer(id: $id) {
              id
              name
              email
            }
          }
        `,
        variables: { id: 1 }
      })
    })
    return result.json()
  })

  expect(response.data.customer.name).toBe('John')
})
```

### **Key API Tests**
1. ✅ **CRUD operations** - Create, Read, Update, Delete
2. ✅ **Error handling** - 4xx, 5xx responses
3. ✅ **Authentication** - Token validation
4. ✅ **Pagination** - Page through results
5. ✅ **Filtering** - Query parameters
6. ✅ **Real-time** - WebSocket, Server-Sent Events

---

## 👁️ VISUAL REGRESSION

### **Screenshot Testing**

```typescript
import { test, expect } from '@playwright/test'

test('dashboard looks correct', async ({ page }) => {
  await page.goto('/dashboard')

  // Full page screenshot
  await expect(page).toHaveScreenshot('dashboard.png', {
    fullPage: true,
    animations: 'disabled'
  })

  // Element screenshot
  await expect(page.locator('.customer-chart')).toHaveScreenshot('chart.png')
})

// Visual diff with tolerance
test('customer card visual', async ({ page }) => {
  await page.goto('/customers')

  await expect(page.locator('.customer-card')).toHaveScreenshot('customer-card.png', {
    maxDiffPixels: 100 // Allow small differences
  })
})
```

### **CI Integration**

```yaml
# .github/workflows/visual-tests.yml
name: Visual Tests
on: [push, pull_request]

jobs:
  visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm install
      - run: npx playwright install
      - run: npx playwright test
        env:
          CI: true
      - uses: actions/upload-artifact@v4
        with:
          name: visual-test-results
          path: test-results
```

### **Key Visual Tests**
1. ✅ **Page screenshots** - Overall layout
2. ✅ **Component screenshots** - Individual components
3. ✅ **Responsive layouts** - Different screen sizes
4. ✅ **State changes** - Before/after interactions
5. ✅ **Dark mode** - Theme comparison

---

## ✅ BEST PRACTICES

### **1. Test Organization**

```typescript
// Use describe blocks
test.describe('Customer Management', () => {
  test.describe('List View', () => {
    test('displays all customers', async ({ page }) => {
      // Test implementation
    })
  })

  test.describe('Create Customer', () => {
    test('creates customer with valid data', async ({ page }) => {
      // Test implementation
    })
  })
})
```

### **2. Page Object Model**

```typescript
// tests/pages/CustomersPage.ts
import { Page, Locator } from '@playwright/test'

export class CustomersPage {
  readonly page: Page
  readonly searchInput: Locator
  readonly createButton: Locator
  readonly customerList: Locator

  constructor(page: Page) {
    this.page = page
    this.searchInput = page.locator('[data-testid="search-input"]')
    this.createButton = page.locator('[data-testid="create-btn"]')
    this.customerList = page.locator('[data-testid="customer-list"]')
  }

  async navigate() {
    await this.page.goto('/customers')
  }

  async search(term: string) {
    await this.searchInput.fill(term)
    await this.page.keyboard.press('Enter')
  }

  async createCustomer(data: any) {
    await this.createButton.click()
    await this.page.fill('[name="name"]', data.name)
    await this.page.click('[data-testid="save-btn"]')
  }
}

// Use in tests
test('search customer', async ({ page }) => {
  const customersPage = new CustomersPage(page)
  await customersPage.navigate()
  await customersPage.search('John')
  // Assertions
})
```

### **3. Custom Matchers**

```typescript
// tests/matchers/matchers.ts
import { Locator } from '@playwright/test'

export async function toBeVisible(this: any, locator: Locator) {
  const isVisible = await locator.isVisible()
  return {
    pass: isVisible,
    message: () => `Expected element to be ${isVisible ? 'not ' : ''}visible`
  }
}

export async function toHaveText(this: any, locator: Locator, text: string) {
  const elementText = await locator.textContent()
  return {
    pass: elementText?.includes(text),
    message: () => `Expected text to include "${text}", got "${elementText}"`
  }
}

// Extend expect
expect.extend({
  async toBeVisible(locator: Locator) {
    return await toBeVisible.call(this, locator)
  },
  async toHaveText(locator: Locator, text: string) {
    return await toHaveText.call(this, locator, text)
  }
})
```

### **4. Data Factories**

```typescript
// tests/factories/customerFactory.ts
import { faker } from '@faker-js/faker'

interface CustomerData {
  firstName: string
  lastName: string
  email: string
  status: 'active' | 'inactive'
}

export function createCustomer(overrides: Partial<CustomerData> = {}): CustomerData {
  return {
    firstName: faker.person.firstName(),
    lastName: faker.person.lastName(),
    email: faker.internet.email(),
    status: 'active',
    ...overrides
  }
}

// Use in tests
test('create customer', async ({ page }) => {
  const customer = createCustomer()
  // Use customer data in test
})
```

### **5. Test Data Cleanup**

```typescript
test('customer CRUD', async ({ page }) => {
  // Create customer
  const customer = await createCustomer()
  await createCustomerAPI(customer)

  // Test operations
  // ...

  // Cleanup
  await deleteCustomerAPI(customer.id)
})
```

### **6. Parallel Execution**

```typescript
// playwright.config.ts
export default defineConfig({
  fullyParallel: true, // Run tests in parallel
  workers: 4, // Number of workers
})

// Test level parallelization
test.describe.configure({ mode: 'parallel' })
```

### **7. Flaky Test Management**

```typescript
// Retry flaky tests
test('sometimes flaky test', async ({ page }) => {
  // Add retry logic
  for (let i = 0; i < 3; i++) {
    try {
      await page.click('[data-testid="flaky-button"]')
      break
    } catch (error) {
      if (i === 2) throw error
      await page.waitForTimeout(1000)
    }
  }
})
```

---

## 📊 COVERAGE GOALS

### **Current Coverage**
- E2E Tests: 10/13 modules (77%)
- Components: 15/19 (79%)
- Pages: 8/48 (17%)
- Stores: 6/11 (55%)

### **Target Coverage (6 weeks)**
- E2E Tests: 13/13 (100%) ➜ **+3 tests**
- Components: 19/19 (100%) ➜ **+4 tests**
- Pages: 30/48 (63%) ➜ **+22 tests**
- Stores: 11/11 (100%) ➜ **+5 tests**

### **Total New Tests: 34**
**Estimated Effort:** 80 hours (2.5 hours/test)

---

## 🎯 IMPLEMENTATION PLAN

### **Week 1-2: Core Coverage**
1. Add missing E2E tests (addresses, coverage-nodes, settings)
2. Add chart component tests
3. Add missing store tests

### **Week 3-4: Accessibility & Mobile**
1. Implement accessibility testing (axe-core)
2. Add mobile device testing (iPhone, Android)
3. Add cross-browser testing (Firefox, Safari)

### **Week 5-6: Advanced Features**
1. Performance testing (Core Web Vitals)
2. Network throttling (3G, offline)
3. Visual regression testing

---

## 📚 LEARNING RESOURCES

1. **Playwright Docs:** https://playwright.dev/
2. **WCAG Guidelines:** https://www.w3.org/WAI/WCAG21/quickref/
3. **Core Web Vitals:** https://web.dev/vitals/
4. **Mobile Testing:** https://playwright.dev/docs/emulation
5. **Accessibility Testing:** https://github.com/abhinaba-ghosh/playwright-axe

---

## 🏆 SUCCESS METRICS

- [ ] 100% E2E test coverage
- [ ] 100% component test coverage
- [ ] 63% page test coverage
- [ ] 100% store test coverage
- [ ] All pages pass accessibility audit
- [ ] All pages pass mobile testing
- [ ] All pages meet Core Web Vitals
- [ ] Zero critical visual regressions

---

**Document Version:** 1.0
**Last Updated:** 2025-11-05
**Status:** ✅ Ready for Implementation
