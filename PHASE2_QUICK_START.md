# Phase 2 Quick Start Guide
## Implementation Examples for Key Components

---

## 🚀 GETTING STARTED

This guide provides **working code examples** for the most critical Phase 2 components. You can copy-paste these into your test suite immediately.

---

## 1. ENHANCED DATA FACTORY

### 1.1 Unique Data Pool Generator

**File: `tests/framework/data-factories/generators/unique-data.pool.ts`**

```typescript
/**
 * Prevents test data collisions by managing unique values
 */
export class UniqueDataPool {
  private usedEmails: Set<string> = new Set()
  private usedIds: Set<string> = new Set()
  private counter: number = 0

  static create(): UniqueDataPool {
    return new UniqueDataPool()
  }

  nextEmail(): string {
    this.counter++
    const email = `test+${Date.now()}_${this.counter}@example.com`
    this.usedEmails.add(email)
    return email
  }

  nextId(prefix: string = 'test'): string {
    this.counter++
    const id = `${prefix}_${Date.now()}_${this.counter}`
    this.usedIds.add(id)
    return id
  }

  reset(): void {
    this.usedEmails.clear()
    this.usedIds.clear()
    this.counter = 0
  }
}

export const dataPool = UniqueDataPool.create()
```

### 1.2 Enhanced Customer Factory

**File: `tests/framework/data-factories/advanced/entity-factories/customer.factory.ts`**

```typescript
/**
 * Enhanced Customer Factory with advanced scenarios
 */
import { faker } from '@faker-js/faker'
import { dataPool } from '../../generators/unique-data.pool'

export type CustomerStatus = 'active' | 'inactive' | 'pending' | 'suspended'

export interface Customer {
  id: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  status: CustomerStatus
  createdAt: Date
  metadata?: Record<string, any>
}

export class CustomerFactory {
  private options: Partial<Customer> = {}

  static create(): CustomerFactory {
    return new CustomerFactory()
  }

  withRandomEmail(): CustomerFactory {
    this.options.email = dataPool.nextEmail()
    return this
  }

  withFirstName(name: string): CustomerFactory {
    this.options.firstName = name
    return this
  }

  withLastName(name: string): CustomerFactory {
    this.options.lastName = name
    return this
  }

  active(): CustomerFactory {
    this.options.status = 'active'
    return this
  }

  inactive(): CustomerFactory {
    this.options.status = 'inactive'
    return this
  }

  // NEW: Scenario-specific methods
  asVipCustomer(): CustomerFactory {
    this.options.status = 'active'
    this.options.metadata = {
      ...this.options.metadata,
      tier: 'VIP',
      discount: 0.2,
      paymentTerms: 'NET_60'
    }
    return this
  }

  asNewCustomer(): CustomerFactory {
    this.options.status = 'pending'
    this.options.createdAt = new Date()
    return this
  }

  withSpecialCharacters(): CustomerFactory {
    this.options.firstName = 'José María'
    this.options.lastName = "O'Connor-Smith"
    return this
  }

  withMetadata(metadata: Record<string, any>): CustomerFactory {
    this.options.metadata = { ...this.options.metadata, ...metadata }
    return this
  }

  // Build customer
  build(): Customer {
    return {
      id: dataPool.nextId('customer'),
      firstName: this.options.firstName || faker.person.firstName(),
      lastName: this.options.lastName || faker.person.lastName(),
      email: this.options.email || dataPool.nextEmail(),
      phone: this.options.phone || faker.phone.number(),
      status: this.options.status || 'active',
      createdAt: this.options.createdAt || new Date(),
      metadata: this.options.metadata
    }
  }
}
```

### 1.3 Data Correlator

**File: `tests/framework/data-factories/advanced/base/data-correlator.ts`**

```typescript
/**
 * Correlates related entities for realistic test data
 */
import { CustomerFactory } from '../../entity-factories/customer.factory'
import { OrderFactory } from '../../entity-factories/order.factory'
import { InvoiceFactory } from '../../entity-factories/invoice.factory'
import { PaymentFactory } from '../../entity-factories/payment.factory'

export interface EntityCorrelation {
  customers: string[]
  orders: string[]
  invoices: string[]
  payments: string[]
}

export class DataCorrelator {
  private customers: any[] = []
  private orders: any[] = []
  private invoices: any[] = []
  private payments: any[] = []

  static create(): DataCorrelator {
    return new DataCorrelator()
  }

  // Add customers
  withCustomers(count: number): DataCorrelator {
    for (let i = 0; i < count; i++) {
      const customer = CustomerFactory.create()
        .withRandomEmail()
        .active()
        .build()
      this.customers.push(customer)
    }
    return this
  }

  // Add orders to customer
  withOrders(customerId: string, count: number): DataCorrelator {
    for (let i = 0; i < count; i++) {
      const order = OrderFactory.create()
        .withCustomerId(customerId)
        .build()
      this.orders.push(order)
    }
    return this
  }

  // Build correlation
  build(): EntityCorrelation {
    return {
      customers: this.customers.map(c => c.id),
      orders: this.orders.map(o => o.id),
      invoices: this.invoices.map(i => i.id),
      payments: this.payments.map(p => p.id)
    }
  }
}
```

---

## 2. CONTRACT TESTING (PACT)

### 2.1 Consumer Contract Test

**File: `tests/framework/contract-testing/consumers/customer-consumer.spec.ts`**

```typescript
/**
 * Pact contract test for Customer API
 */
import { Pact, somethingLike, eachLike } from '@pact-foundation/pact'
import { CustomerFactory } from '../../data-factories/customer.factory'

const provider = new Pact({
  port: 1234,
  log: 'tests/framework/contract-testing/logs/pact.log',
  dir: 'tests/framework/contract-testing/pacts',
  consumer: 'frontend',
  provider: 'backend'
})

describe('Customer API Contract', () => {
  beforeAll(async () => {
    await provider.setup()
  })

  afterAll(async () => {
    await provider.finalize()
  })

  describe('GET /api/customers', () => {
    it('returns list of customers', async () => {
      const expectedCustomer = {
        id: somethingLike('cust_123'),
        firstName: somethingLike('John'),
        lastName: somethingLike('Doe'),
        email: somethingLike('john@example.com'),
        status: somethingLike('active')
      }

      await provider
        .given('customers exist')
        .uponReceiving('a request for all customers')
        .withRequest({
          method: 'GET',
          path: '/api/customers'
        })
        .willRespondWith({
          status: 200,
          body: {
            data: eachLike(expectedCustomer),
            total: somethingLike(1)
          }
        })

      // Simulate API call
      const response = await fetch('http://localhost:1234/api/customers')
      const data = await response.json()

      expect(data.data).toHaveLength(1)
    })
  })

  describe('POST /api/customers', () => {
    it('creates a new customer', async () => {
      const customer = CustomerFactory.create()
        .withRandomEmail()
        .build()

      await provider
        .given('no customer with email exists')
        .uponReceiving('a request to create a customer')
        .withRequest({
          method: 'POST',
          path: '/api/customers',
          body: customer
        })
        .willRespondWith({
          status: 201,
          body: {
            success: true,
            data: {
              id: somethingLike('cust_new'),
              ...customer
            }
          }
        })

      // Simulate API call
      const response = await fetch('http://localhost:1234/api/customers', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(customer)
      })

      const data = await response.json()
      expect(data.success).toBe(true)
    })
  })
})
```

---

## 3. SMOKE TEST SUITE

### 3.1 Authentication Smoke Test

**File: `tests/smoke/critical-paths/authentication.spec.ts`**

```typescript
import { test, expect } from '@playwright/test'
import { AuthHelper } from '../../helpers/auth.helper'

test.describe('Authentication Smoke', () => {
  test('01 - User can login with valid credentials', async ({ page }) => {
    await test.step('Navigate to login', async () => {
      await page.goto('/login')
      await expect(page.locator('h1')).toContainText('Login')
    })

    await test.step('Login', async () => {
      await AuthHelper.login(page, {
        username: process.env.TEST_USER || 'admin',
        password: process.env.TEST_PASSWORD || 'admin123'
      })
    })

    await test.step('Verify redirect', async () => {
      await expect(page).toHaveURL('/dashboard')
      await expect(page.locator('[data-testid="user-menu"]')).toBeVisible()
    })
  })

  test('02 - User cannot login with invalid credentials', async ({ page }) => {
    await page.goto('/login')
    
    await AuthHelper.login(page, {
      username: 'invalid',
      password: 'invalid'
    })

    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/invalid/i)
    await expect(page).toHaveURL(/\/login/)
  })

  test('03 - User can logout', async ({ page }) => {
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'admin',
      password: process.env.TEST_PASSWORD || 'admin123'
    })

    await AuthHelper.logout(page)
    await expect(page).toHaveURL(/\/login/)
  })
})
```

### 3.2 Customer CRUD Smoke Test

**File: `tests/smoke/critical-paths/customer-crud.spec.ts`**

```typescript
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../framework/utils/page-object-model'
import { CustomerFactory } from '../../framework/data-factories/customer.factory'
import { AuthHelper } from '../../helpers/auth.helper'

test.describe('Customer CRUD Smoke', () => {
  let customerPage: CustomerPage
  let createdCustomerId: string

  test.beforeEach(async ({ page }) => {
    await AuthHelper.login(page)
    customerPage = new CustomerPage(page)
    await customerPage.navigateTo()
  })

  test('01 - Create new customer', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withRandomEmail()
      .active()
      .build()

    await customerPage.navigateToCreate()

    await page.fill('[data-testid="firstName-input"]', customer.firstName)
    await page.fill('[data-testid="lastName-input"]', customer.lastName)
    await page.fill('[data-testid="email-input"]', customer.email)
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText(/created/i)

    createdCustomerId = await customerPage.getLastCreatedId()
    expect(createdCustomerId).toBeDefined()
  })

  test('02 - Read customer details', async ({ page }) => {
    if (!createdCustomerId) {
      test.skip(true, 'Customer not created')
    }

    await customerPage.navigateToDetail(createdCustomerId)
    await expect(page.locator('h1')).toContainText(/customer/i)
  })

  test('03 - Update customer', async ({ page }) => {
    if (!createdCustomerId) {
      test.skip(true, 'Customer not created')
    }

    await customerPage.navigateToDetail(createdCustomerId)
    await page.click('[data-testid="edit-button"]')

    const newLastName = 'Updated'
    await page.fill('[data-testid="lastName-input"]', newLastName)
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText(/updated/i)
  })

  test('04 - Delete customer', async ({ page }) => {
    if (!createdCustomerId) {
      test.skip(true, 'Customer not created')
    }

    await customerPage.navigateToDetail(createdCustomerId)
    await page.click('[data-testid="delete-button"]')
    await page.click('[data-testid="confirm-delete"]')

    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText(/deleted/i)
  })
})
```

### 3.3 API Health Check

**File: `tests/smoke/sanity-checks/api-health.spec.ts`**

```typescript
import { test, expect } from '@playwright/test'

test.describe('API Health Checks', () => {
  const baseUrl = process.env.BACKEND_URL || 'http://localhost:8080'

  test('01 - Backend API is healthy', async ({ request }) => {
    const response = await request.get(`${baseUrl}/actuator/health`)
    expect(response.status()).toBe(200)

    const health = await response.json()
    expect(health.status).toBe('UP')
  })

  test('02 - Customer API is responsive', async ({ request }) => {
    const response = await request.get(`${baseUrl}/api/customers`)
    expect(response.status()).toBe(200)
  })

  test('03 - Order API is responsive', async ({ request }) => {
    const response = await request.get(`${baseUrl}/api/orders`)
    expect(response.status()).toBe(200)
  })
})
```

---

## 4. REGRESSION TEST SUITE

### 4.1 Customer Management Regression

**File: `tests/regression/business-flows/customer-management/create.spec.ts`**

```typescript
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../../framework/utils/page-object-model'
import { CustomerFactory } from '../../../framework/data-factories/customer.factory'
import { AuthHelper } from '../../../helpers/auth.helper'

test.describe('Customer Creation Regression', () => {
  let customerPage: CustomerPage

  test.beforeEach(async ({ page }) => {
    await AuthHelper.login(page)
    customerPage = new CustomerPage(page)
    await customerPage.navigateTo()
  })

  test.describe('Valid Input Scenarios', () => {
    test('01 - Create customer with minimum required fields', async ({ page }) => {
      const customer = CustomerFactory.create()
        .withRandomEmail()
        .build()

      await customerPage.create(customer)
      const customerId = await customerPage.getLastCreatedId()
      expect(customerId).toBeDefined()
    })

    test('02 - Create customer with special characters', async ({ page }) => {
      const customer = CustomerFactory.create()
        .withSpecialCharacters()
        .withRandomEmail()
        .build()

      await customerPage.create(customer)
      const customerId = await customerPage.getLastCreatedId()

      await customerPage.navigateToDetail(customerId)
      await expect(page.locator('[data-testid="firstName"]'))
        .toContainText('José María')
    })

    test('03 - Create customer with metadata', async ({ page }) => {
      const customer = CustomerFactory.create()
        .withRandomEmail()
        .withMetadata({
          source: 'marketing_campaign',
          campaign_id: 'CAMP-2025-001'
        })
        .build()

      await customerPage.create(customer)
      const customerId = await customerPage.getLastCreatedId()

      await customerPage.navigateToDetail(customerId)
      await expect(page.locator('[data-testid="metadata-source"]'))
        .toContainText('marketing_campaign')
    })
  })

  test.describe('Validation Scenarios', () => {
    test('04 - Reject empty required fields', async ({ page }) => {
      await customerPage.navigateToCreate()
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-firstName"]'))
        .toContainText(/required/i)
      await expect(page.locator('[data-testid="error-email"]'))
        .toContainText(/required/i)
    })

    test('05 - Reject invalid email format', async ({ page }) => {
      await customerPage.navigateToCreate()
      await page.fill('[data-testid="email-input"]', 'invalid-email')
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-email"]'))
        .toContainText(/invalid.*email/i)
    })

    test('06 - Reject duplicate email', async ({ page }) => {
      const customer1 = CustomerFactory.create()
        .withRandomEmail()
        .build()

      await customerPage.create(customer1)

      const customer2 = CustomerFactory.create()
        .withEmail(customer1.email)
        .build()

      await customerPage.create(customer2)

      await expect(page.locator('[data-testid="error-email"]'))
        .toContainText(/already.*exists/i)
    })
  })
})
```

### 4.2 Cross-Browser Compatibility

**File: `tests/regression/cross-cutting/compatibility/browser-matrix.spec.ts`**

```typescript
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../../framework/utils/page-object-model'
import { CustomerFactory } from '../../../framework/data-factories/customer.factory'
import { AuthHelper } from '../../../helpers/auth.helper'

test.describe('Browser Compatibility', () => {
  test('01 - Customer creation works in all browsers', async ({ browserName, page }) => {
    const customer = CustomerFactory.create()
      .withRandomEmail()
      .build()

    await AuthHelper.login(page)
    const customerPage = new CustomerPage(page)

    await customerPage.create(customer)
    const customerId = await customerPage.getLastCreatedId()
    expect(customerId).toBeDefined()
  })

  test('02 - Keyboard navigation works', async ({ page }) => {
    await AuthHelper.login(page)
    await page.goto('/customers')

    await page.keyboard.press('Tab')
    await expect(page.locator('[data-testid="search-input"]')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('[data-testid="status-filter"]')).toBeFocused()
  })
})
```

---

## 5. NPM SCRIPTS

### Update `frontend/package.json`

```json
{
  "scripts": {
    "test:phase2:smoke": "playwright test --config=tests/smoke/smoke.config.ts",
    "test:phase2:contract": "playwright test --config=tests/framework/contract-testing/consumer.config.ts",
    "test:phase2:regression": "playwright test --config=tests/regression/regression.config.ts",
    "test:phase2:all": "npm run test:phase2:smoke && npm run test:phase2:contract",
    "test:phase2:data:seed": "ts-node tests/framework/data-factories/database/seed.ts",
    "test:phase2:data:clean": "ts-node tests/framework/data-factories/database/clean.ts"
  }
}
```

---

## 6. TEST CONFIGURATIONS

### Smoke Test Config

**File: `tests/smoke/smoke.config.ts`**

```typescript
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/smoke',
  fullyParallel: true,
  timeout: 60000,
  retries: process.env.CI ? 2 : 0,
  maxFailures: 5,
  reporter: [
    ['html', { outputFolder: 'test-results/smoke-report' }],
    ['list']
  ],
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ]
})
```

---

## 7. CI/CD INTEGRATION

### GitHub Actions Workflow

**File: `.github/workflows/playwright-smoke.yml`**

```yaml
name: Playwright Smoke Tests

on:
  push:
    branches: [ main, develop ]

jobs:
  smoke:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 21
          
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
          npx playwright install --with-deps
          
      - name: Start services
        run: docker compose -f dev/compose.yml up -d
        
      - name: Run smoke tests
        run: |
          cd frontend
          npx playwright test --config=tests/smoke/smoke.config.ts
          
      - name: Upload report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: smoke-test-report
          path: frontend/test-results/
```

---

## 8. RUNNING THE TESTS

### Execute Individual Suites

```bash
# Run smoke tests only
npm run test:phase2:smoke

# Run contract tests
npm run test:phase2:contract

# Run regression tests
npm run test:phase2:regression

# Run all Phase 2 tests
npm run test:phase2:all
```

### With Specific Browser

```bash
# Chrome only
npx playwright test --project=chromium

# Firefox only
npx playwright test --project=firefox

# Mobile
npx playwright test --project='mobile-*'
```

### Debug Mode

```bash
# UI mode
npm run test:phase2:smoke -- --ui

# Debug mode
npm run test:phase2:smoke -- --debug
```

---

## 9. QUICK CHECKLIST

### Setup Phase 2
- [ ] Install dependencies: `npm install @pact-foundation/pact`
- [ ] Copy data factory files
- [ ] Copy smoke test files
- [ ] Copy regression test files
- [ ] Update npm scripts
- [ ] Configure test environments
- [ ] Run first smoke test

### First Test Run
- [ ] Run smoke tests: `npm run test:phase2:smoke`
- [ ] Check test results in `test-results/`
- [ ] Fix any configuration issues
- [ ] Verify Page Objects are working
- [ ] Verify factories are generating unique data

### CI/CD Integration
- [ ] Create GitHub Actions workflow
- [ ] Configure secret variables
- [ ] Test workflow on a branch
- [ ] Verify artifact upload
- [ ] Set up branch protection

---

## 💡 TIPS

1. **Start Small**: Begin with smoke tests, then add regression
2. **Use Data Factories**: Never hardcode test data
3. **Clean Up**: Always delete test data after tests
4. **Parallel Execution**: Use `fullyParallel: true` for speed
5. **Trace Debugging**: Enable `trace: 'on-first-retry'` for debugging
6. **Screenshots**: Set `screenshot: 'only-on-failure'` for debugging
7. **Readability**: Use `test.step()` for clear test steps

---

## 🎯 NEXT STEPS

1. **Install Dependencies**
   ```bash
   npm install @pact-foundation/pact
   ```

2. **Create Directory Structure**
   ```bash
   mkdir -p tests/smoke/{critical-paths,sanity-checks}
   mkdir -p tests/regression/{business-flows,cross-cutting}
   mkdir -p tests/framework/contract-testing/{consumers,providers,pacts}
   ```

3. **Copy Code Examples**
   - Copy data factory examples
   - Copy smoke test examples
   - Copy regression test examples

4. **Run First Test**
   ```bash
   npm run test:phase2:smoke
   ```

5. **Iterate and Expand**
   - Add more scenarios
   - Add more edge cases
   - Add more validations

---

## 📚 MORE RESOURCES

- [Full Phase 2 Plan](./PHASE2_TEST_FRAMEWORK_PLAN.md)
- [Implementation Summary](./PHASE2_IMPLEMENTATION_SUMMARY.md)
- [Playwright Documentation](https://playwright.dev/)
- [Pact Documentation](https://docs.pact.io/)

---

**Version**: 1.0  
**Date**: 2025-11-06
