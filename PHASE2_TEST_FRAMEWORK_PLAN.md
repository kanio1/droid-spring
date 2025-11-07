# Phase 2: Advanced Playwright Testing Framework
## Comprehensive Development Plan for BSS E2E Testing

### Executive Summary

This document outlines Phase 2 of the Playwright testing framework development for the BSS (Business Support System). Building on Phase 1's foundation of 150 E2E tests and 6 Page Objects, Phase 2 will implement advanced testing capabilities including contract testing, specialized test suites, and enhanced data management.

**Phase 2 Goals:**
- ✅ Phase 1: 150 E2E tests, 6 Page Objects, Factories, Helpers
- 🎯 Phase 2: Contract Testing, Smoke Suite (50 tests), Regression Suite (100+ tests)
- 🎯 Enhanced Data Factories with Advanced Scenarios
- 🎯 Complete CI/CD Integration with Reporting

---

## 1. ENHANCED DATA FACTORIES

### 1.1 Architecture Overview

**Current State:**
- Basic factories: Customer, Order, Invoice, Payment, Subscription
- Simple fluent interface for data generation
- Static data with minimal customization

**Phase 2 Enhancements:**
- Dynamic correlation between entities (Customer → Orders → Invoices → Payments)
- Edge case and invalid data scenarios
- Large dataset generation for performance testing
- Database seeding integration
- Data retention and cleanup policies

### 1.2 New File Structure

```
tests/framework/data-factories/
├── advanced/
│   ├── base/
│   │   ├── factory.interface.ts
│   │   ├── data-correlator.ts
│   │   └── scenario-builder.ts
│   ├── entity-factories/
│   │   ├── customer.factory.ts (enhanced)
│   │   ├── order.factory.ts (enhanced)
│   │   ├── invoice.factory.ts (enhanced)
│   │   ├── payment.factory.ts (enhanced)
│   │   ├── subscription.factory.ts (enhanced)
│   │   └── product.factory.ts (new)
│   ├── correlation/
│   │   ├── customer-order.correlator.ts
│   │   ├── order-invoice.correlator.ts
│   │   ├── invoice-payment.correlator.ts
│   │   └── subscription-customer.correlator.ts
│   └── scenarios/
│       ├── edge-cases/
│       │   ├── invalid-email.factory.ts
│       │   ├── overdue-invoice.factory.ts
│       │   ├── failed-payment.factory.ts
│       │   └── cancelled-subscription.factory.ts
│       ├── bulk-data/
│       │   ├── bulk-customer.factory.ts
│       │   └── bulk-order.factory.ts
│       └── performance/
│           ├── large-dataset.factory.ts
│           └── stress-test-data.factory.ts
├── generators/
│   ├── faker-extensions.ts
│   ├── id-generator.ts
│   ├── timestamp-generator.ts
│   └── unique-data.pool.ts
├── database/
│   ├── seed-manager.ts
│   ├── data-cleanup.ts
│   └── test-db.config.ts
└── index.ts (enhanced)
```

### 1.3 Key Implementations

#### 1.3.1 Data Correlator Engine

**File: `tests/framework/data-factories/advanced/base/data-correlator.ts`**

```typescript
/**
 * Correlates related entities to create realistic data hierarchies
 * - Customer → Orders → Invoices → Payments
 * - Maintains referential integrity
 * - Supports batch correlation
 */
export class DataCorrelator {
  private entityGraph: Map<string, Set<string>> = new Map()
  private correlations: Map<string, string[]> = new Map()

  static create(): DataCorrelator {
    return new DataCorrelator()
  }

  // Correlate customer with orders
  withOrders(customerId: string, orderCount: number = 1): DataCorrelator {
    const orderIds = Array.from({ length: orderCount }, () => 
      IdGenerator.generate('order')
    )
    this.correlations.set(`customer:${customerId}:orders`, orderIds)
    return this
  }

  // Correlate order with invoices
  withInvoices(orderId: string, invoiceCount: number = 1): DataCorrelator {
    const invoiceIds = Array.from({ length: invoiceCount }, () => 
      IdGenerator.generate('invoice')
    )
    this.correlations.set(`order:${orderId}:invoices`, invoiceIds)
    return this
  }

  // Build complete customer ecosystem
  buildCustomerEcosystem(): CustomerEcosystem {
    const ecosystem: CustomerEcosystem = {
      customer: null,
      orders: [],
      invoices: [],
      payments: []
    }

    // Build full hierarchy with realistic data
    return ecosystem
  }
}
```

#### 1.3.2 Scenario Builder

**File: `tests/framework/data-factories/advanced/base/scenario-builder.ts`**

```typescript
/**
 * Predefined test scenarios for common business cases
 */
export interface TestScenario {
  name: string
  description: string
  setup: () => Promise<EntityCorrelation>
  validate: (entities: EntityCorrelation) => Promise<ValidationResult>
}

export class ScenarioBuilder {
  static happyPath(): TestScenario {
    return {
      name: 'Happy Path',
      description: 'Standard customer with one order and successful payment',
      setup: async () => {
        return await DataCorrelator.create()
          .withOrders('customer-1', 1)
          .withInvoices('order-1', 1)
          .withPayments('invoice-1', 1)
          .buildCustomerEcosystem()
      },
      validate: async (entities) => {
        // Validation logic
        return { valid: true, errors: [] }
      }
    }
  }

  static overdueInvoice(): TestScenario {
    return {
      name: 'Overdue Invoice',
      description: 'Customer with overdue invoice and collection attempts',
      setup: async () => {
        const invoice = InvoiceFactory.create()
          .withOverdueStatus()
          .withAgeInDays(30)
          .withLateFees()
          .build()

        return { invoice }
      },
      validate: async (entities) => {
        // Check overdue status
        return { valid: true, errors: [] }
      }
    }
  }

  static bulkOperations(): TestScenario {
    return {
      name: 'Bulk Operations',
      description: 'Customer with 100+ orders for performance testing',
      setup: async () => {
        return await BulkDataFactory.create()
          .withCustomers(10)
          .withOrdersPerCustomer(50)
          .withRandomInvoices(0.8)
          .withRandomPayments(0.7)
          .build()
      },
      validate: async (entities) => {
        // Performance validation
        return { valid: true, errors: [] }
      }
    }
  }
}
```

#### 1.3.3 Enhanced Customer Factory

**File: `tests/framework/data-factories/advanced/entity-factories/customer.factory.ts`**

```typescript
/**
 * Enhanced Customer Factory with advanced scenarios
 */
export class CustomerFactory {
  private options: CustomerFactoryOptions = {}
  private relatedEntities: RelatedEntities = {}
  private scenarioContext: ScenarioContext | null = null

  // Existing basic methods...

  // NEW: Scenario-specific methods
  asNewCustomer(): CustomerFactory {
    this.options.status = 'pending'
    this.options.createdAt = new Date()
    return this
  }

  asVipCustomer(): CustomerFactory {
    this.options.tier = 'VIP'
    this.options.discount = 0.2
    this.options.paymentTerms = 'NET_60'
    return this
  }

  withRelatedOrders(count: number): CustomerFactory {
    this.relatedEntities.orders = OrderFactory.createBulk(count).build()
    return this
  }

  withRelatedSubscriptions(count: number): CustomerFactory {
    this.relatedEntities.subscriptions = SubscriptionFactory.createBulk(count).build()
    return this
  }

  // NEW: Edge case methods
  withInvalidEmail(): CustomerFactory {
    this.options.email = 'invalid-email'
    this.options.shouldFailValidation = true
    return this
  }

  withDuplicateEmail(existingEmail: string): CustomerFactory {
    this.options.email = existingEmail
    this.options.shouldFailDuplicate = true
    return this
  }

  withSpecialCharacters(): CustomerFactory {
    this.options.firstName = 'José María'
    this.options.lastName = "O'Connor-Smith"
    this.options.companyName = 'Acme Corp. & Associates'
    return this
  }

  // NEW: Performance scenarios
  withLargeDataset(): CustomerFactory {
    this.options.metadata = {
      ...Array.from({ length: 100 }, (_, i) => ({
        [`key-${i}`]: `value-${i}`.repeat(100)
      }))
    }
    return this
  }

  // Build with correlation
  buildWithCorrelation(): CustomerBuildResult {
    const customer = this.build()
    
    const correlation = DataCorrelator.create()
      .withCustomer(customer.id)

    if (this.relatedEntities.orders) {
      correlation.withOrders(customer.id, this.relatedEntities.orders.length)
    }

    return {
      customer,
      correlation,
      relatedEntities: this.relatedEntities
    }
  }
}
```

#### 1.3.4 Bulk Data Factory

**File: `tests/framework/data-factories/advanced/bulk-data/bulk-customer.factory.ts`**

```typescript
/**
 * Generates large datasets for performance and stress testing
 */
export class BulkDataFactory {
  private customerCount: number = 0
  private ordersPerCustomer: number = 0
  private invoiceProbability: number = 0.8
  private paymentProbability: number = 0.7

  static create(): BulkDataFactory {
    return new BulkDataFactory()
  }

  withCustomers(count: number): BulkDataFactory {
    this.customerCount = count
    return this
  }

  withOrdersPerCustomer(count: number): BulkDataFactory {
    this.ordersPerCustomer = count
    return this
  }

  withInvoiceProbability(prob: number): BulkDataFactory {
    this.invoiceProbability = prob
    return this
  }

  async build(): Promise<BulkDataset> {
    const customers: Customer[] = []
    const orders: Order[] = []
    const invoices: Invoice[] = []
    const payments: Payment[] = []

    // Generate customers
    for (let i = 0; i < this.customerCount; i++) {
      const customer = CustomerFactory.create()
        .withRandomData()
        .withIndex(i)
        .build()

      customers.push(customer)

      // Generate orders for this customer
      for (let j = 0; j < this.ordersPerCustomer; j++) {
        const order = OrderFactory.create()
          .withCustomerId(customer.id)
          .withIndex(j)
          .build()

        orders.push(order)

        // Generate invoice with probability
        if (Math.random() < this.invoiceProbability) {
          const invoice = InvoiceFactory.create()
            .withOrderId(order.id)
            .withCustomerId(customer.id)
            .build()

          invoices.push(invoice)

          // Generate payment with probability
          if (Math.random() < this.paymentProbability) {
            const payment = PaymentFactory.create()
              .withInvoiceId(invoice.id)
              .withCustomerId(customer.id)
              .build()

            payments.push(payment)
          }
        }
      }
    }

    return {
      customers,
      orders,
      invoices,
      payments,
      statistics: {
        totalCustomers: customers.length,
        totalOrders: orders.length,
        totalInvoices: invoices.length,
        totalPayments: payments.length,
        invoiceRate: invoices.length / orders.length,
        paymentRate: payments.length / invoices.length
      }
    }
  }
}
```

### 1.4 Database Integration

**File: `tests/framework/data-factories/database/seed-manager.ts`**

```typescript
/**
 * Manages database seeding and cleanup for tests
 */
export class SeedManager {
  private activeSeeds: Set<string> = new Set()

  static async seedTestDatabase(dataset: BulkDataset): Promise<SeedResult> {
    // Clean existing test data
    await this.cleanTestData()

    // Seed in correct order (respect foreign keys)
    const customerIds = await this.seedCustomers(dataset.customers)
    const orderIds = await this.seedOrders(dataset.orders, customerIds)
    const invoiceIds = await this.seedInvoices(dataset.invoices, orderIds, customerIds)
    const paymentIds = await this.seedPayments(dataset.payments, invoiceIds, customerIds)

    return {
      success: true,
      entityCounts: {
        customers: customerIds.length,
        orders: orderIds.length,
        invoices: invoiceIds.length,
        payments: paymentIds.length
      }
    }
  }

  static async cleanTestData(): Promise<void> {
    // Delete in reverse order
    await Payment.deleteMany({ testData: true })
    await Invoice.deleteMany({ testData: true })
    await Order.deleteMany({ testData: true })
    await Customer.deleteMany({ testData: true })
  }

  static async createDataRetentionPolicy(): Promise<void> {
    // Mark all seeded data with testData: true
    // Set TTL for automatic cleanup
  }
}
```

---

## 2. CONTRACT TESTING (PACT)

### 2.1 Architecture Overview

**Goal:** Prevent API integration issues by testing consumer-provider contracts

**Approach:** Consumer-Driven Contract Testing using Pact

**Benefits:**
- Catch breaking API changes before deployment
- Ensure backward compatibility
- Reduce integration testing time
- Document API contracts

### 2.2 File Structure

```
tests/framework/contract-testing/
├── consumers/
│   ├── customer-consumer.spec.ts
│   ├── order-consumer.spec.ts
│   ├── invoice-consumer.spec.ts
│   ├── payment-consumer.spec.ts
│   └── subscription-consumer.spec.ts
├── providers/
│   ├── customer-provider.pact.ts
│   ├── order-provider.pact.ts
│   ├── invoice-provider.pact.ts
│   ├── payment-provider.pact.ts
│   └── subscription-provider.pact.ts
├── pacts/
│   ├── customer-consumer-customer-provider.json
│   ├── order-consumer-order-provider.json
│   └── ...
├── matchers/
│   ├── common.matchers.ts
│   ├── customer.matchers.ts
│   ├── order.matchers.ts
│   └── ...
├── pact-config/
│   ├── broker.ts
│   ├── verification.config.ts
│   └── ci-integration.ts
└── index.ts
```

### 2.3 Key Implementations

#### 2.3.1 Consumer Contract Tests

**File: `tests/framework/contract-testing/consumers/customer-consumer.spec.ts`**

```typescript
/**
 * Pact contract test for Customer API consumer
 * Verifies frontend expectations match backend API
 */
import { Pact, somethingLike, eachLike } from '@pact-foundation/pact'
import { CustomerAPIClient } from '../api-client'
import { CustomerFactory } from '../../data-factories'

describe('Customer API Contract', () => {
  const provider = new Pact({
    port: 1234,
    log: 'tests/framework/contract-testing/logs/customer-provider.log',
    dir: 'tests/framework/contract-testing/pacts',
    consumer: 'frontend',
    provider: 'backend',
    logLevel: 'INFO'
  })

  beforeAll(async () => {
    await provider.setup()
  })

  afterEach(async () => {
    await provider.verify()
  })

  afterAll(async () => {
    await provider.finalize()
  })

  describe('GET /api/customers', () => {
    it('returns a list of customers', async () => {
      const expectedCustomers = eachLike({
        id: somethingLike('cust_123'),
        firstName: somethingLike('John'),
        lastName: somethingLike('Doe'),
        email: somethingLike('john@example.com'),
        status: somethingLike('active'),
        createdAt: somethingLike('2025-01-01T00:00:00Z')
      })

      await provider
        .given('customers exist')
        .uponReceiving('a request for all customers')
        .withRequest({
          method: 'GET',
          path: '/api/customers',
          headers: {
            Authorization: somethingLike('Bearer token')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            data: expectedCustomers,
            total: somethingLike(1),
            page: somethingLike(1),
            limit: somethingLike(10)
          }
        })

      const apiClient = new CustomerAPIClient('http://localhost:1234')
      const response = await apiClient.getCustomers()

      expect(response.data).toHaveLength(1)
      expect(response.data[0]).toMatchObject({
        id: expect.any(String),
        firstName: expect.any(String),
        lastName: expect.any(String)
      })
    })
  })

  describe('POST /api/customers', () => {
    it('creates a new customer', async () => {
      const customerRequest = CustomerFactory.create()
        .withRandomEmail()
        .build()

      const expectedResponse = {
        id: somethingLike('cust_new'),
        ...customerRequest,
        createdAt: somethingLike('2025-01-01T00:00:00Z')
      }

      await provider
        .given('no customer with email exists')
        .uponReceiving('a request to create a customer')
        .withRequest({
          method: 'POST',
          path: '/api/customers',
          headers: {
            'Content-Type': 'application/json',
            Authorization: somethingLike('Bearer token')
          },
          body: customerRequest
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            success: somethingLike(true),
            data: expectedResponse
          }
        })

      const apiClient = new CustomerAPIClient('http://localhost:1234')
      const response = await apiClient.createCustomer(customerRequest)

      expect(response.success).toBe(true)
      expect(response.data.id).toBeDefined()
    })
  })

  describe('Error scenarios', () => {
    it('returns 400 for invalid customer data', async () => {
      const invalidCustomer = {
        email: 'invalid-email',
        firstName: ''
      }

      await provider
        .given('invalid customer data')
        .uponReceiving('a request with invalid data')
        .withRequest({
          method: 'POST',
          path: '/api/customers',
          body: invalidCustomer
        })
        .willRespondWith({
          status: 400,
          body: {
            success: false,
            errors: eachLike({
              field: somethingLike('email'),
              message: somethingLike('Invalid email format')
            })
          }
        })

      const apiClient = new CustomerAPIClient('http://localhost:1234')
      
      await expect(
        apiClient.createCustomer(invalidCustomer)
      ).rejects.toThrow('400')
    })
  })
})
```

#### 2.3.2 Provider Verification

**File: `tests/framework/contract-testing/providers/customer-provider.pact.ts`**

```typescript
/**
 * Provider-side verification of consumer contracts
 * Run against actual backend API
 */
import { Verifier } from '@pact-foundation/pact'
import { config } from 'dotenv'

describe('Customer API Provider Verification', () => {
  it('validates the expectations of consumer', async () => {
    const token = process.env.PACT_BROKER_TOKEN
    const brokerUrl = process.env.PACT_BROKER_URL || 'http://localhost:9292'

    const verifier = new Verifier({
      provider: 'backend',
      providerBaseUrl: 'http://localhost:8080',
      brokerUrl: brokerUrl,
      token: token,
      publishVerificationResult: process.env.CI === 'true',
      providerVersion: process.env.GIT_COMMIT || 'local'
    })

    const result = await verifier.verifyProvider()
    expect(result).toContain('Pact verification successful')
  })
})
```

#### 2.3.3 Pact Matchers Library

**File: `tests/framework/contract-testing/matchers/common.matchers.ts`**

```typescript
/**
 * Reusable matchers for common API patterns
 */
import { somethingLike, eachLike, term, regex } from '@pact-foundation/pact'

// ID patterns
export const uuidMatcher = () => regex(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)
export const idMatcher = () => regex(/^[a-z]+_[a-z0-9]+$/)
export const numericIdMatcher = () => somethingLike(expect.any(Number))

// Date patterns
export const isoDateMatcher = () => term({
  matcher: /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/,
  generate: '2025-01-01T00:00:00Z'
})

// Status patterns
export const statusMatcher = (allowed: string[]) => term({
  matcher: new RegExp(`^(${allowed.join('|')})$`),
  generate: allowed[0]
})

// Pagination patterns
export const paginationMatcher = () => ({
  page: numericIdMatcher(),
  limit: numericIdMatcher(),
  total: numericIdMatcher(),
  totalPages: numericIdMatcher()
})

// Money patterns
export const moneyMatcher = () => ({
  amount: somethingLike(expect.any(Number)),
  currency: somethingLike('USD')
})

// Address patterns
export const addressMatcher = () => ({
  street: somethingLike(expect.any(String)),
  city: somethingLike(expect.any(String)),
  state: somethingLike(expect.any(String)),
  postalCode: somethingLike(expect.any(String)),
  country: somethingLike('US')
})
```

#### 2.3.4 Pact Broker Integration

**File: `tests/framework/contract-testing/pact-config/broker.ts`**

```typescript
/**
 * Pact Broker integration for contract management
 */
export class PactBroker {
  private brokerUrl: string
  private token: string

  constructor() {
    this.brokerUrl = process.env.PACT_BROKER_URL || 'http://localhost:9292'
    this.token = process.env.PACT_BRONER_TOKEN || ''
  }

  async publishContract(pactFilePath: string, version: string): Promise<void> {
    // Publish to broker for sharing with team
  }

  async checkVersionCompatibility(consumer: string, version: string): Promise<boolean> {
    // Check if consumer version is compatible with latest provider
  }

  async getContract(consumer: string, provider: string): Promise<Contract> {
    // Download contract from broker
  }

  async verifyWebhooks(): Promise<void> {
    // Setup webhooks for automatic verification
  }
}
```

---

## 3. SMOKE TEST SUITE

### 3.1 Overview

**Purpose:** Rapid validation of critical user paths (< 5 minutes)

**Scope:** 50 essential tests covering core business flows

**Execution Strategy:**
- Run on every commit
- Parallel execution
- Fast feedback loop
- Resource optimization

### 3.2 File Structure

```
tests/smoke/
├── critical-paths/
│   ├── authentication.spec.ts
│   ├── customer-crud.spec.ts
│   ├── order-creation.spec.ts
│   ├── payment-processing.spec.ts
│   ├── invoice-generation.spec.ts
│   └── subscription-activation.spec.ts
├── sanity-checks/
│   ├── homepage-load.spec.ts
│   ├── api-health.spec.ts
│   ├── database-connectivity.spec.ts
│   └── third-party-services.spec.ts
├── navigation/
│   ├── main-navigation.spec.ts
│   ├── breadcrumbs.spec.ts
│   └── footer-links.spec.ts
├── regression-guard/
│   ├── critical-bugs.spec.ts
│   └── known-issues.spec.ts
└── smoke.config.ts
```

### 3.3 Key Implementations

#### 3.3.1 Authentication Smoke Test

**File: `tests/smoke/critical-paths/authentication.spec.ts`**

```typescript
/**
 * Smoke test for authentication flow
 * Validates login, logout, and session management
 */
import { test, expect } from '@playwright/test'
import { AuthHelper } from '../../helpers'
import { CustomerFactory } from '../../framework/data-factories'

test.describe('Authentication Smoke', () => {
  test('01 - User can login with valid credentials', async ({ page }) => {
    await test.step('Navigate to login page', async () => {
      await page.goto('/login')
      await expect(page.locator('h1')).toContainText('Login')
    })

    await test.step('Login with valid credentials', async () => {
      const customer = CustomerFactory.create().build()
      
      await AuthHelper.login(page, {
        username: process.env.TEST_USER || 'admin',
        password: process.env.TEST_PASSWORD || 'admin123'
      })
    })

    await test.step('Verify redirect to dashboard', async () => {
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

    // Should stay on login page with error
    await expect(page.locator('[data-testid="error-message"]')).toContainText(/invalid/i)
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

  test('04 - Session expires after timeout', async ({ page }) => {
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'admin',
      password: process.env.TEST_PASSWORD || 'admin123'
    })

    // Simulate session timeout (2 minutes)
    await page.waitForTimeout(120000)

    // Should redirect to login
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })
})
```

#### 3.3.2 Customer CRUD Smoke Test

**File: `tests/smoke/critical-paths/customer-crud.spec.ts`**

```typescript
/**
 * Smoke test for customer CRUD operations
 */
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../framework/utils/page-object-model'
import { CustomerFactory } from '../../framework/data-factories'
import { AuthHelper } from '../../helpers'

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

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/created/i)

    // Extract ID from URL or success message
    createdCustomerId = await customerPage.getLastCreatedId()
    expect(createdCustomerId).toBeDefined()
  })

  test('02 - Read customer details', async ({ page }) => {
    if (!createdCustomerId) {
      test.skip(true, 'Customer not created')
    }

    await customerPage.navigateToDetail(createdCustomerId)

    await expect(page.locator('h1')).toContainText(/customer/i)
    await expect(page.locator('[data-testid="customer-name"]'))
      .toContainText(/john doe/i)
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

    // Verify update
    await expect(page.locator('[data-testid="customer-name"]'))
      .toContainText(newLastName)
  })

  test('04 - Delete customer', async ({ page }) => {
    if (!createdCustomerId) {
      test.skip(true, 'Customer not created')
    }

    await customerPage.navigateToDetail(createdCustomerId)
    await page.click('[data-testid="delete-button"]')

    // Confirm deletion
    await page.click('[data-testid="confirm-delete"]')

    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText(/deleted/i)

    // Verify not in list
    await customerPage.navigateTo()
    await expect(page.locator(`[data-testid="customer-row-${createdCustomerId}"]`))
      .toHaveCount(0)
  })
})
```

#### 3.3.3 API Health Smoke Test

**File: `tests/smoke/sanity-checks/api-health.spec.ts`**

```typescript
/**
 * Smoke test for API health checks
 * Validates all critical backend services are responding
 */
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

  test('04 - Invoice API is responsive', async ({ request }) => {
    const response = await request.get(`${baseUrl}/api/invoices`)

    expect(response.status()).toBe(200)
  })

  test('05 - Payment API is responsive', async ({ request }) => {
    const response = await request.get(`${baseUrl}/api/payments`)

    expect(response.status()).toBe(200)
  })
})
```

#### 3.3.4 Smoke Test Configuration

**File: `tests/smoke/smoke.config.ts`**

```typescript
/**
 * Smoke test suite configuration
 */
export const smokeConfig = {
  // Parallel execution
  fullyParallel: true,
  
  // Test timeout (1 minute)
  timeout: 60000,
  
  // Retries
  retries: process.env.CI ? 2 : 0,
  
  // Max failures
  maxFailures: 5,
  
  // Reporter
  reporter: [
    ['html', { outputFolder: 'test-results/smoke-report' }],
    ['allure-playwright'],
    ['list']
  ],
  
  // Projects (browser matrix)
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ],
  
  // Global setup/teardown
  globalSetup: require.resolve('./global-smoke-setup.ts'),
  globalTeardown: require.resolve('./global-smoke-teardown.ts')
}
```

---

## 4. REGRESSION TEST SUITE

### 4.1 Overview

**Purpose:** Comprehensive validation of all features (100+ tests)

**Scope:** Full feature coverage across all business flows

**Execution Strategy:**
- Nightly execution
- Parallel across browsers
- Full data seeding
- Complete test isolation

### 4.2 File Structure

```
tests/regression/
├── business-flows/
│   ├── customer-management/
│   │   ├── create.spec.ts
│   │   ├── read.spec.ts
│   │   ├── update.spec.ts
│   │   ├── delete.spec.ts
│   │   ├── search.spec.ts
│   │   ├── filter.spec.ts
│   │   ├── sort.spec.ts
│   │   ├── pagination.spec.ts
│   │   ├── bulk-operations.spec.ts
│   │   └── export.spec.ts
│   ├── order-management/
│   │   ├── create.spec.ts
│   │   ├── items.spec.ts
│   │   ├── status.spec.ts
│   │   ├── cancellation.spec.ts
│   │   └── fulfillment.spec.ts
│   ├── invoice-management/
│   │   ├── generation.spec.ts
│   │   ├── status.spec.ts
│   │   ├── overdue.spec.ts
│   │   ├── adjustments.spec.ts
│   │   └── export.spec.ts
│   ├── payment-processing/
│   │   ├── credit-card.spec.ts
│   │   ├── bank-transfer.spec.ts
│   │   ├── refunds.spec.ts
│   │   ├── chargebacks.spec.ts
│   │   └── reconciliation.spec.ts
│   └── subscription-management/
│       ├── activation.spec.ts
│       ├── cancellation.spec.ts
│       ├── renewal.spec.ts
│       ├── upgrades.spec.ts
│       └── downgrades.spec.ts
├── cross-cutting/
│   ├── accessibility/
│   │   ├── keyboard-navigation.spec.ts
│   │   ├── screen-reader.spec.ts
│   │   └── color-contrast.spec.ts
│   ├── performance/
│   │   ├── page-load.spec.ts
│   │   ├── api-response.spec.ts
│   │   └── large-datasets.spec.ts
│   ├── security/
│   │   ├── auth.spec.ts
│   │   ├── authorization.spec.ts
│   │   ├── data-sanitization.spec.ts
│   │   └── csrf.spec.ts
│   └── compatibility/
│       ├── browser-matrix.spec.ts
│       ├── device-matrix.spec.ts
│       └── viewport-matrix.spec.ts
├── edge-cases/
│   ├── network-errors.spec.ts
│   ├── server-errors.spec.ts
│   ├── validation-errors.spec.ts
│   ├── concurrent-operations.spec.ts
│   └── data-race-conditions.spec.ts
└── regression.config.ts
```

### 4.3 Key Implementations

#### 4.3.1 Customer Management Regression Suite

**File: `tests/regression/business-flows/customer-management/create.spec.ts`**

```typescript
/**
 * Comprehensive regression test for customer creation
 */
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../../framework/utils/page-object-model'
import { CustomerFactory } from '../../../framework/data-factories'
import { AuthHelper } from '../../../helpers'

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
        .withRequiredOnly()
        .build()

      await customerPage.create(customer)

      const customerId = await customerPage.getLastCreatedId()
      expect(customerId).toBeDefined()

      // Verify in list
      await expect(page.locator(`[data-testid="customer-row-${customerId}"]`))
        .toBeVisible()
    })

    test('02 - Create customer with all fields', async ({ page }) => {
      const customer = CustomerFactory.create()
        .withAllFields()
        .build()

      await customerPage.create(customer)

      const customerId = await customerPage.getLastCreatedId()
      
      // Verify all fields in detail view
      await customerPage.navigateToDetail(customerId)
      
      await expect(page.locator('[data-testid="firstName"]')).toContainText(customer.firstName)
      await expect(page.locator('[data-testid="lastName"]')).toContainText(customer.lastName)
      await expect(page.locator('[data-testid="email"]')).toContainText(customer.email)
      await expect(page.locator('[data-testid="phone"]')).toContainText(customer.phone)
    })

    test('03 - Create customer with special characters', async ({ page }) => {
      const customer = CustomerFactory.create()
        .withSpecialCharacters()
        .build()

      await customerPage.create(customer)

      const customerId = await customerPage.getLastCreatedId()
      
      await customerPage.navigateToDetail(customerId)
      
      await expect(page.locator('[data-testid="firstName"]'))
        .toContainText('José María')
      await expect(page.locator('[data-testid="lastName"]'))
        .toContainText("O'Connor-Smith")
    })

    test('04 - Create customer with metadata', async ({ page }) => {
      const customer = CustomerFactory.create()
        .withMetadata({
          source: 'marketing_campaign',
          campaign_id: 'CAMP-2025-001',
          referral_code: 'REF123456'
        })
        .build()

      await customerPage.create(customer)

      const customerId = await customerPage.getLastCreatedId()
      
      // Verify metadata
      await customerPage.navigateToDetail(customerId)
      await expect(page.locator('[data-testid="metadata-source"]'))
        .toContainText('marketing_campaign')
    })
  })

  test.describe('Validation Scenarios', () => {
    test('05 - Reject empty required fields', async ({ page }) => {
      await customerPage.navigateToCreate()

      await page.click('[data-testid="submit-button"]')

      // Should show validation errors
      await expect(page.locator('[data-testid="error-firstName"]'))
        .toContainText(/required/i)
      await expect(page.locator('[data-testid="error-lastName"]'))
        .toContainText(/required/i)
      await expect(page.locator('[data-testid="error-email"]'))
        .toContainText(/required/i)
    })

    test('06 - Reject invalid email format', async ({ page }) => {
      await customerPage.navigateToCreate()

      await page.fill('[data-testid="email-input"]', 'invalid-email')
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-email"]'))
        .toContainText(/invalid.*email/i)
    })

    test('07 - Reject duplicate email', async ({ page }) => {
      const existingCustomer = CustomerFactory.create()
        .withRandomEmail()
        .build()

      await customerPage.create(existingCustomer)

      // Try to create another with same email
      const duplicateCustomer = CustomerFactory.create()
        .withEmail(existingCustomer.email)
        .build()

      await customerPage.create(duplicateCustomer)

      await expect(page.locator('[data-testid="error-email"]'))
        .toContainText(/already.*exists/i)
    })

    test('08 - Reject overly long fields', async ({ page }) => {
      await customerPage.navigateToCreate()

      const tooLong = 'x'.repeat(256)
      await page.fill('[data-testid="firstName-input"]', tooLong)
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-firstName"]'))
        .toContainText(/too.*long/i)
    })
  })

  test.describe('Concurrent Creation', () => {
    test('09 - Handle concurrent customer creation', async ({ browser }) => {
      // Create two contexts
      const context1 = await browser.newContext()
      const context2 = await browser.newContext()

      const page1 = await context1.newPage()
      const page2 = await context2.newPage()

      await AuthHelper.ensureLoggedIn(page1)
      await AuthHelper.ensureLoggedIn(page2)

      const customerPage1 = new CustomerPage(page1)
      const customerPage2 = new CustomerPage(page2)

      const customer1 = CustomerFactory.create().withRandomEmail().build()
      const customer2 = CustomerFactory.create().withRandomEmail().build()

      // Create customers concurrently
      await Promise.all([
        customerPage1.create(customer1),
        customerPage2.create(customer2)
      ])

      // Both should succeed
      const id1 = await customerPage1.getLastCreatedId()
      const id2 = await customerPage2.getLastCreatedId()

      expect(id1).not.toBe(id2)

      await context1.close()
      await context2.close()
    })
  })
})
```

#### 4.3.2 Cross-Browser Compatibility Tests

**File: `tests/regression/cross-cutting/compatibility/browser-matrix.spec.ts`**

```typescript
/**
 * Cross-browser compatibility regression tests
 */
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../../framework/utils/page-object-model'
import { CustomerFactory } from '../../../framework/data-factories'
import { AuthHelper } from '../../../helpers'

test.describe('Browser Compatibility', () => {
  test('01 - Customer creation works in all browsers', async ({ browserName, page }) => {
    // Skip Safari for features not supported
    if (browserName === 'webkit') {
      test.skip(true, 'Not supported in Safari')
    }

    const customer = CustomerFactory.create()
      .withRandomEmail()
      .build()

    await AuthHelper.login(page)
    const customerPage = new CustomerPage(page)

    await customerPage.create(customer)

    const customerId = await customerPage.getLastCreatedId()
    expect(customerId).toBeDefined()
  })

  test('02 - Keyboard navigation works in all browsers', async ({ browserName, page }) => {
    await AuthHelper.login(page)
    await page.goto('/customers')

    // Tab through elements
    await page.keyboard.press('Tab')
    await expect(page.locator('[data-testid="search-input"]')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('[data-testid="status-filter"]')).toBeFocused()
  })

  test('03 - Visual regression checks', async ({ browserName, page }) => {
    await AuthHelper.login(page)
    const customerPage = new CustomerPage(page)
    await customerPage.navigateTo()

    // Take screenshot and compare
    await expect(page).toHaveScreenshot(`customers-list-${browserName}.png`, {
      fullPage: true,
      threshold: 0.3
    })
  })
})
```

#### 4.3.3 Performance Regression Tests

**File: `tests/regression/cross-cutting/performance/large-datasets.spec.ts`**

```typescript
/**
 * Performance regression tests with large datasets
 */
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../../framework/utils/page-object-model'
import { BulkDataFactory } from '../../../framework/data-factories'
import { AuthHelper } from '../../../helpers'
import { SeedManager } from '../../../framework/data-factories/database/seed-manager'

test.describe('Performance Regression', () => {
  test.beforeAll(async () => {
    // Seed large dataset for performance testing
    const dataset = await BulkDataFactory.create()
      .withCustomers(1000)
      .withOrdersPerCustomer(5)
      .build()

    await SeedManager.seedTestDatabase(dataset)
  })

  test('01 - Customer list loads with 1000+ customers', async ({ page }) => {
    await AuthHelper.login(page)
    const customerPage = new CustomerPage(page)

    const startTime = Date.now()
    await customerPage.navigateTo()
    const loadTime = Date.now() - startTime

    // Should load within 3 seconds
    expect(loadTime).toBeLessThan(3000)

    // Verify data is displayed
    await expect(page.locator('[data-testid="customers-table"]')).toBeVisible()
  })

  test('02 - Search performs well with large dataset', async ({ page }) => {
    await AuthHelper.login(page)
    await page.goto('/customers')

    const startTime = Date.now()
    await page.fill('[data-testid="search-input"]', 'John')
    await page.keyboard.press('Enter')
    await page.waitForLoadState('networkidle')
    const searchTime = Date.now() - startTime

    // Should search within 1 second
    expect(searchTime).toBeLessThan(1000)

    // Verify results
    const resultCount = await page.locator('[data-testid^="customer-row-"]').count()
    expect(resultCount).toBeGreaterThan(0)
  })

  test('03 - Pagination performance', async ({ page }) => {
    await AuthHelper.login(page)
    await page.goto('/customers')

    // Navigate to last page
    const startTime = Date.now()
    await page.click('[data-testid="pagination-last"]')
    await page.waitForLoadState('networkidle')
    const paginationTime = Date.now() - startTime

    // Should paginate within 500ms
    expect(paginationTime).toBeLessThan(500)
  })
})
```

#### 4.3.4 Edge Case Tests

**File: `tests/regression/edge-cases/network-errors.spec.ts`**

```typescript
/**
 * Network error handling regression tests
 */
import { test, expect } from '@playwright/test'
import { CustomerPage } from '../../../framework/utils/page-object-model'
import { CustomerFactory } from '../../../framework/data-factories'
import { AuthHelper } from '../../../helpers'
import { NetworkHelper } from '../../../helpers'

test.describe('Network Error Handling', () => {
  test('01 - Handle 500 server error on create', async ({ page }) => {
    await AuthHelper.login(page)

    // Mock 500 error
    await NetworkHelper.mockError(page, '/api/customers', 500)

    const customer = CustomerFactory.create().withRandomEmail().build()
    const customerPage = new CustomerPage(page)
    await customerPage.navigateToCreate()

    await customerPage.fillForm(customer)
    await page.click('[data-testid="submit-button"]')

    // Should show error message
    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/error|500/i)

    // Should not navigate away
    await expect(page).toHaveURL(/\/customers\/new/)
  })

  test('02 - Handle network timeout', async ({ page }) => {
    await AuthHelper.login(page)

    // Mock slow response (5 seconds)
    await NetworkHelper.mockSlowResponse(page, '/api/customers', 5000)

    const customer = CustomerFactory.create().withRandomEmail().build()
    const customerPage = new CustomerPage(page)
    await customerPage.navigateToCreate()

    await customerPage.fillForm(customer)
    await page.click('[data-testid="submit-button"]')

    // Should show loading state
    await expect(page.locator('[data-testid="loading"]')).toBeVisible()

    // Should handle timeout gracefully
    await expect(page.locator('[data-testid="timeout-message"]'))
      .toBeVisible()
  })

  test('03 - Handle connection lost', async ({ page }) => {
    await AuthHelper.login(page)

    const customer = CustomerFactory.create().withRandomEmail().build()
    const customerPage = new CustomerPage(page)
    await customerPage.navigateToCreate()

    // Simulate connection loss
    await NetworkHelper.simulateConnectionLoss(page)

    // Check offline indicator
    await expect(page.locator('[data-testid="offline-indicator"]'))
      .toBeVisible()

    // Restore connection
    await NetworkHelper.restoreConnection(page)

    // Retry operation
    await customerPage.fillForm(customer)
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]'))
      .toBeVisible()
  })
})
```

#### 4.3.5 Regression Test Configuration

**File: `tests/regression/regression.config.ts`**

```typescript
/**
 * Comprehensive regression test configuration
 */
export const regressionConfig = {
  // Full parallelization
  fullyParallel: true,
  
  // Longer timeout for complex tests
  timeout: 300000, // 5 minutes
  
  // More retries
  retries: process.env.CI ? 3 : 1,
  
  // Fail fast
  maxFailures: 10,
  
  // HTML + Allure reporting
  reporter: [
    ['html', { outputFolder: 'test-results/regression-report' }],
    ['allure-playwright'],
    ['list'],
    ['json', { outputFile: 'test-results/regression-results.json' }]
  ],
  
  // Full browser matrix
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 5'] } },
    { name: 'mobile-safari', use: { ...devices['iPhone 12'] } }
  ],
  
  // Trace on for debugging
  trace: 'on-first-retry',
  
  // Screenshots on failure
  screenshot: 'only-on-failure',
  
  // Video on retry
  video: 'retain-on-failure',
  
  // Global test database
  globalSetup: require.resolve('./global-regression-setup.ts'),
  globalTeardown: require.resolve('./global-regression-teardown.ts')
}
```

---

## 5. TEST DATA MANAGEMENT

### 5.1 Data Pool Strategy

**File: `tests/framework/data-factories/generators/unique-data.pool.ts`**

```typescript
/**
 * Manages pool of unique test data to prevent collisions
 */
export class UniqueDataPool {
  private usedEmails: Set<string> = new Set()
  private usedIds: Set<string> = new Set()
  private emailPrefix = 'test+'
  private emailDomain = 'example.com'

  static create(): UniqueDataPool {
    return new UniqueDataPool()
  }

  nextEmail(): string {
    let attempts = 0
    while (attempts < 1000) {
      const email = `${this.emailPrefix}${Date.now()}_${attempts}@${this.emailDomain}`
      if (!this.usedEmails.has(email)) {
        this.usedEmails.add(email)
        return email
      }
      attempts++
    }
    throw new Error('Failed to generate unique email')
  }

  nextId(prefix: string = 'test'): string {
    let attempts = 0
    while (attempts < 1000) {
      const id = `${prefix}_${Date.now()}_${attempts}`
      if (!this.usedIds.has(id)) {
        this.usedIds.add(id)
        return id
      }
      attempts++
    }
    throw new Error('Failed to generate unique ID')
  }

  reset(): void {
    this.usedEmails.clear()
    this.usedIds.clear()
  }
}
```

### 5.2 Test Database Management

**File: `tests/framework/data-factories/database/test-db.config.ts`**

```typescript
/**
 * Test database configuration and management
 */
export class TestDatabase {
  private static instance: TestDatabase
  private connection: any

  private constructor() {}

  static getInstance(): TestDatabase {
    if (!TestDatabase.instance) {
      TestDatabase.instance = new TestDatabase()
    }
    return TestDatabase.instance
  }

  async connect(): Promise<void> {
    // Connect to test database
    this.connection = await createConnection({
      host: process.env.TEST_DB_HOST || 'localhost',
      port: parseInt(process.env.TEST_DB_PORT || '5432'),
      database: process.env.TEST_DB_NAME || 'bss_test',
      username: process.env.TEST_DB_USER || 'test',
      password: process.env.TEST_DB_PASSWORD || 'test'
    })
  }

  async clean(): Promise<void> {
    // Clean all test data
    await this.connection.query('TRUNCATE TABLE test_data CASCADE')
  }

  async disconnect(): Promise<void> {
    if (this.connection) {
      await this.connection.close()
    }
  }
}
```

---

## 6. CI/CD INTEGRATION

### 6.1 GitHub Actions Workflow

**File: `.github/workflows/playwright-phase2.yml`**

```yaml
name: Playwright Phase 2 Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *' # Nightly regression

jobs:
  smoke-tests:
    name: Smoke Tests
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 21
          
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
          npx playwright install --with-deps
          
      - name: Start services
        run: |
          docker compose -f dev/compose.yml up -d
          sleep 30
          
      - name: Run smoke tests
        run: |
          cd frontend
          npx playwright test --config=tests/smoke/smoke.config.ts
          
      - name: Upload smoke test report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: smoke-test-report
          path: frontend/test-results/

  contract-tests:
    name: Contract Tests
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 21
          
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
          npm install @pact-foundation/pact
          
      - name: Run contract tests
        run: |
          cd frontend
          npx pact-broker publish tests/framework/contract-testing/pacts \
            --broker-url=${{ secrets.PACT_BROKER_URL }} \
            --broker-token=${{ secrets.PACT_BROKER_TOKEN }} \
            --consumer-app-version=${{ github.sha }}

  regression-tests:
    name: Regression Tests
    runs-on: ubuntu-latest
    timeout-minutes: 60
    strategy:
      matrix:
        browser: [chromium, firefox, webkit]
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 21
          
      - name: Install dependencies
        run: |
          cd frontend
          npm ci
          npx playwright install --with-deps ${{ matrix.browser }}
          
      - name: Start services
        run: |
          docker compose -f dev/compose.yml up -d
          sleep 30
          
      - name: Run regression tests
        run: |
          cd frontend
          npx playwright test --config=tests/regression/regression.config.ts \
            --project=${{ matrix.browser }}
            
      - name: Upload regression test report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: regression-test-report-${{ matrix.browser }}
          path: frontend/test-results/

  quality-gate:
    name: Quality Gate
    runs-on: ubuntu-latest
    needs: [smoke-tests, contract-tests, regression-tests]
    if: always()
    steps:
      - name: Check test results
        run: |
          if [[ "${{ needs.smoke-tests.result }}" == "failure" ]]; then
            echo "Smoke tests failed"
            exit 1
          fi
          if [[ "${{ needs.regression-tests.result }}" == "failure" ]]; then
            echo "Regression tests failed"
            exit 1
          fi
          echo "All tests passed"
```

### 6.2 NPM Script Enhancements

**File: `frontend/package.json` additions**

```json
{
  "scripts": {
    "test:phase2:smoke": "playwright test --config=tests/smoke/smoke.config.ts",
    "test:phase2:contract": "playwright test --config=tests/framework/contract-testing/consumer.config.ts",
    "test:phase2:regression": "playwright test --config=tests/regression/regression.config.ts",
    "test:phase2:all": "npm run test:phase2:smoke && npm run test:phase2:contract",
    "test:phase2:data:seed": "ts-node tests/framework/data-factories/database/seed.ts",
    "test:phase2:data:clean": "ts-node tests/framework/data-factories/database/clean.ts",
    "test:phase2:report": "allure generate test-results -o test-results/allure-report",
    "test:phase2:serve-report": "allure serve test-results"
  }
}
```

---

## 7. REPORTING & ANALYTICS

### 7.1 Allure Reports

**File: `tests/framework/reporting/allure.config.ts`**

```typescript
/**
 * Allure reporting configuration for Phase 2
 */
export const allureConfig = {
  historySize: 50,
  environmentInfo: {
    Browser: process.env.BROWSER || 'chromium',
    Platform: process.platform,
    NodeJS: process.version
  },
  categories: [
    {
      name: 'Product defects',
      matchedStatuses: ['failed']
    },
    {
      name: 'Test defects',
      matchedStatuses: ['broken']
    },
    {
      name: 'Authentication failures',
      messagePatterns: [/auth|login|session/i]
    },
    {
      name: 'Network errors',
      messagePatterns: [/network|timeout|500/i]
    },
    {
      name: 'Flaky tests',
      messagePatterns: [/flaky|intermittent/i]
    }
  ],
  epic: 'BSS Phase 2',
  feature: 'E2E Testing',
  story: 'Smoke & Regression'
}
```

### 7.2 Test Analytics

**File: `tests/framework/reporting/test-analytics.ts`**

```typescript
/**
 * Test execution analytics and metrics
 */
export class TestAnalytics {
  static trackTest(testName: string, status: 'passed' | 'failed' | 'skipped', duration: number) {
    // Send to analytics service
  }

  static trackFlakyTest(testName: string) {
    // Track for investigation
  }

  static generateTestReport(results: TestResult[]) {
    const report = {
      summary: {
        total: results.length,
        passed: results.filter(r => r.status === 'passed').length,
        failed: results.filter(r => r.status === 'failed').length,
        skipped: results.filter(r => r.status === 'skipped').length,
        flaky: results.filter(r => r.flaky).length
      },
      coverage: {
        businessFlows: this.calculateBusinessFlowCoverage(results),
        browsers: this.calculateBrowserCoverage(results),
        testTypes: this.calculateTestTypeCoverage(results)
      },
      trends: this.calculateTrends(results)
    }

    return report
  }
}
```

---

## 8. DOCUMENTATION

### 8.1 Test Pattern Documentation

**File: `tests/docs/TESTING_GUIDE.md`**

```markdown
# Playwright Phase 2 Testing Guide

## Quick Start

### Running Test Suites

```bash
# Smoke tests (fast feedback)
npm run test:phase2:smoke

# Contract tests
npm run test:phase2:contract

# Full regression (nightly)
npm run test:phase2:regression

# All tests
npm run test:phase2:all
```

### Writing New Tests

1. **Use Page Objects**
   ```typescript
   const customerPage = new CustomerPage(page)
   await customerPage.navigateTo()
   ```

2. **Use Data Factories**
   ```typescript
   const customer = CustomerFactory.create()
     .withRandomEmail()
     .active()
     .build()
   ```

3. **Use Scenarios**
   ```typescript
   const scenario = ScenarioBuilder.happyPath()
   const data = await scenario.setup()
   ```

### Best Practices

- Always clean up test data
- Use unique data (don't reuse IDs)
- Follow the AAA pattern (Arrange, Act, Assert)
- Tag tests appropriately (@smoke @regression @contract)
- Use test.step() for readability
- Avoid hard-coded waits (use expectations)
```

---

## 9. IMPLEMENTATION ROADMAP

### Phase 2A: Enhanced Data Factories (Week 1-2)
1. ✅ Data correlator engine
2. ✅ Scenario builder
3. ✅ Enhanced entity factories
4. ✅ Bulk data generation
5. ✅ Database integration

### Phase 2B: Contract Testing (Week 3-4)
1. ✅ Consumer contract tests
2. ✅ Provider verification
3. ✅ Pact broker setup
4. ✅ CI/CD integration
5. ✅ Contract documentation

### Phase 2C: Smoke Test Suite (Week 5)
1. ✅ Critical path tests (50)
2. ✅ API health checks
3. ✅ Quick validation suite
4. ✅ Performance optimization
5. ✅ CI integration

### Phase 2D: Regression Test Suite (Week 6-8)
1. ✅ Full business flow tests (100+)
2. ✅ Cross-browser testing
3. ✅ Performance tests
4. ✅ Edge case coverage
5. ✅ Visual regression

### Phase 2E: Infrastructure (Ongoing)
1. ✅ CI/CD pipelines
2. ✅ Reporting & analytics
3. ✅ Documentation
4. ✅ Test data management
5. ✅ Monitoring & alerting

---

## 10. BENEFITS & COVERAGE

### Benefits

1. **Prevent Integration Issues**
   - Contract testing catches API changes early
   - Pact broker provides centralized contract management
   - Version compatibility checking

2. **Fast Feedback**
   - Smoke suite runs in < 5 minutes
   - Parallel execution across browsers
   - Quick validation of critical paths

3. **Comprehensive Coverage**
   - 100+ regression tests cover all features
   - Edge cases and error scenarios
   - Cross-browser and device testing

4. **Maintainability**
   - Page Objects reduce duplication
   - Data factories provide reusable test data
   - Clear test patterns and documentation

5. **Data Integrity**
   - Correlation engine ensures realistic data
   - Unique data pool prevents collisions
   - Automated cleanup

### Coverage Improvements

| Test Type | Phase 1 | Phase 2 | Improvement |
|-----------|---------|---------|-------------|
| E2E Tests | 150 | 250+ | 67% increase |
| API Tests | 0 | 50+ | New capability |
| Contract Tests | 0 | 5 APIs | New capability |
| Smoke Tests | 0 | 50 | New capability |
| Regression Tests | 0 | 100+ | New capability |
| Cross-Browser | 3 browsers | 5 browsers | 67% increase |
| Performance Tests | 0 | 20+ | New capability |

### Test Execution Time

| Suite | Phase 1 | Phase 2 | Target |
|-------|---------|---------|---------|
| Smoke | N/A | 50 tests | < 5 min |
| Contract | N/A | 25 tests | < 3 min |
| Regression | 150 tests | 250+ tests | < 60 min |
| Full Suite | 150 tests | 325+ tests | < 70 min |

---

## CONCLUSION

Phase 2 transforms the Playwright testing framework from a basic E2E suite into a comprehensive, enterprise-grade testing solution. The combination of contract testing, specialized test suites, and enhanced data management provides:

- **Early detection** of integration issues through contract testing
- **Rapid feedback** through smoke tests
- **Complete validation** through regression tests
- **Maintainable** and scalable test infrastructure

This approach follows industry best practices and provides the foundation for continuous quality assurance in the BSS system.

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-06  
**Owner:** QA Engineering Team
