# Implementation Examples - Testing Fixes

## 1. Mock Configuration Examples

### vitest.setup.ts (Complete Implementation)

```typescript
import '@testing-library/jest-dom'
import { vi, beforeEach, afterEach } from 'vitest'

// =============================================================================
// COMPOSABLES MOCKS
// =============================================================================

// Mock useApi
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    del: vi.fn(),
    patch: vi.fn(),
    create: vi.fn(),
    read: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    paginatedGet: vi.fn(),
    request: vi.fn(),
    loading: vi.fn(() => false),
    handleSuccess: vi.fn(),
    buildUrl: vi.fn((endpoint: string) => `http://localhost:8080/api${endpoint}`),
    baseURL: 'http://localhost:8080/api'
  })
}))

// Mock useAuth
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    token: vi.fn(() => null),
    isAuthenticated: vi.fn(() => false),
    isReady: vi.fn(() => false),
    status: vi.fn(() => 'unauthenticated'),
    profile: vi.fn(() => null),
    ready: vi.fn(() => false),
    ensureReady: vi.fn(),
    refreshToken: vi.fn(() => Promise.resolve(true)),
    login: vi.fn(() => Promise.resolve()),
    logout: vi.fn(() => Promise.resolve()),
    reloadProfile: vi.fn(() => Promise.resolve(null)),
    authHeader: vi.fn(() => undefined),
    lastError: vi.fn(() => null)
  })
}))

// Mock useToast
vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

// =============================================================================
// NUXT MOCKS
// =============================================================================

// Mock #app (Nuxt composables)
vi.mock('#app', async () => {
  const actual = await vi.importActual('#app')
  return {
    ...actual,
    useRuntimeConfig: () => ({
      public: {
        apiBaseUrl: 'http://localhost:8080/api'
      }
    }),
    useNuxtApp: () => ({
      $keycloak: null
    })
  }
})

// Mock useRouter
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn(),
    forward: vi.fn()
  })
}))

// =============================================================================
// VUE MOCKS (Optional - for advanced scenarios)
// =============================================================================

vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...actual,
    ref: vi.fn((initial = null) => ({
      value: initial
    })),
    reactive: vi.fn((obj) => obj),
    computed: vi.fn(() => ({ value: null })),
    watch: vi.fn(),
    nextTick: vi.fn(() => Promise.resolve())
  }
})

// =============================================================================
// TEST HOOKS
// =============================================================================

// Reset mocks before each test
beforeEach(() => {
  vi.clearAllMocks()
})

// Clean up after each test
afterEach(() => {
  // Any cleanup logic
})
```

### Mock Helper File (tests/unit/composables-mock.ts)

```typescript
import { vi } from 'vitest'

export const mockUseApi = () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
  patch: vi.fn(),
  create: vi.fn(),
  read: vi.fn(),
  update: vi.fn(),
  remove: vi.fn(),
  paginatedGet: vi.fn(),
  request: vi.fn(),
  loading: vi.fn(() => false),
  handleSuccess: vi.fn(),
  buildUrl: vi.fn(),
  baseURL: 'http://localhost:8080/api'
})

export const mockUseAuth = () => ({
  token: vi.fn(() => null),
  isAuthenticated: vi.fn(() => false),
  ensureReady: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshToken: vi.fn(),
  reloadProfile: vi.fn(),
  authHeader: vi.fn(),
  status: vi.fn(() => 'unauthenticated')
})

export const mockUseToast = () => ({
  showToast: vi.fn()
})
```

---

## 2. Store Test Examples

### customer.store.spec.ts (Fixed)

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCustomerStore } from '~/stores/customer'
import type { Customer, CustomerStatus } from '~/schemas/customer'

describe('Customer Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useCustomerStore()

    expect(store.customers).toEqual([])
    expect(store.currentCustomer).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch customers successfully', async () => {
    const store = useCustomerStore()
    const mockCustomers: Customer[] = [
      {
        id: '1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        status: 'ACTIVE' as CustomerStatus,
        statusDisplayName: 'Active',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = useApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockCustomers,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.fetchCustomers()

    expect(store.customers).toEqual(mockCustomers)
    expect(store.pagination.totalElements).toBe(1)
    expect(get).toHaveBeenCalledWith('/customers', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc'
      }
    })
  })

  it('should create a customer', async () => {
    const store = useCustomerStore()
    const newCustomer = {
      firstName: 'Alice',
      lastName: 'Brown',
      email: 'alice@example.com',
      status: 'ACTIVE' as CustomerStatus
    }

    const mockCustomer: Customer = {
      id: '3',
      ...newCustomer,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    const { post } = useApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockCustomer
    } as any)

    const result = await store.createCustomer(newCustomer)

    expect(store.customers).toHaveLength(1)
    expect(store.customers[0]).toEqual(mockCustomer)
    expect(post).toHaveBeenCalledWith('/customers', newCustomer)
  })

  // NOTE: Fixed - using store.customers directly, not store.customers.value
  it('should update a customer', async () => {
    const store = useCustomerStore()

    const existingCustomer: Customer = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      status: 'ACTIVE' as CustomerStatus,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    // FIX: Direct assignment, not .value
    store.customers = [existingCustomer]

    const updateData = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe Updated',
      email: 'john.updated@example.com',
      version: 1
    }

    const mockUpdatedCustomer: Customer = {
      ...existingCustomer,
      ...updateData,
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = useApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedCustomer
    } as any)

    await store.updateCustomer(updateData)

    expect(store.customers[0].lastName).toBe('Doe Updated')
    expect(store.customers[0].email).toBe('john.updated@example.com')
    expect(store.customers[0].version).toBe(2)
  })
})
```

### order.store.spec.ts (With Missing Getters)

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useOrderStore } from '~/stores/order'
import type { Order, OrderStatus, OrderType, OrderPriority } from '~/schemas/order'

describe('Order Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should get orders by priority', async () => {
    const store = useOrderStore()
    const mockOrders: Order[] = [
      {
        id: '1',
        orderNumber: 'ORD-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        orderType: 'NEW' as OrderType,
        orderTypeDisplayName: 'New',
        status: 'PENDING' as OrderStatus,
        statusDisplayName: 'Pending',
        priority: 'URGENT' as OrderPriority,
        priorityDisplayName: 'Urgent',
        totalAmount: 99.99,
        currency: 'PLN',
        requestedDate: '2024-01-15',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-1',
        salesRepName: 'Jane Smith',
        notes: '',
        isPending: true,
        isCompleted: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.orders = mockOrders

    const urgentOrders = store.getOrdersByPriority('URGENT')
    expect(urgentOrders.value).toHaveLength(1)
    expect(urgentOrders.value[0].priority).toBe('URGENT')
  })
})
```

---

## 3. Store Updates Example

### app/stores/order.ts (Add Missing Getters)

```typescript
import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Order,
  CreateOrderCommand,
  UpdateOrderStatusCommand,
  OrderSearchParams,
  OrderListResponse,
  OrderStatus,
  OrderType,
  OrderPriority
} from '~/schemas/order'

export const useOrderStore = defineStore('order', () => {
  // State (existing code...)
  const orders = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = reactive({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: false,
    numberOfElements: 0,
    empty: true
  })

  // Getters (existing code...)
  const orderCount = computed(() => orders.value.length)
  const pendingOrders = computed(() => orders.value.filter(o => o.status === 'PENDING'))
  const confirmedOrders = computed(() => orders.value.filter(o => o.status === 'CONFIRMED'))
  const processingOrders = computed(() => orders.value.filter(o => o.status === 'PROCESSING'))
  const completedOrders = computed(() => orders.value.filter(o => o.status === 'COMPLETED'))
  const cancelledOrders = computed(() => orders.value.filter(o => o.status === 'CANCELLED'))

  const newOrders = computed(() => orders.value.filter(o => o.orderType === 'NEW'))
  const upgradeOrders = computed(() => orders.value.filter(o => o.orderType === 'UPGRADE'))
  const downgradeOrders = computed(() => orders.value.filter(o => o.orderType === 'DOWNGRADE'))
  const renewOrders = computed(() => orders.value.filter(o => o.orderType === 'RENEW'))
  const cancelOrders = computed(() => orders.value.filter(o => o.orderType === 'CANCEL'))

  const urgentOrders = computed(() => orders.value.filter(o => o.priority === 'URGENT'))
  const highPriorityOrders = computed(() => orders.value.filter(o => o.priority === 'HIGH'))

  // NEW: Add missing getters that tests expect
  const getOrderById = (id: string) => computed(() =>
    orders.value.find(o => o.id === id)
  )

  const getOrdersByType = (type: OrderType) => computed(() =>
    orders.value.filter(o => o.orderType === type)
  )

  const getOrdersByStatus = (status: OrderStatus) => computed(() =>
    orders.value.filter(o => o.status === status)
  )

  const getOrdersByPriority = (priority: OrderPriority) => computed(() =>
    orders.value.filter(o => o.priority === priority)
  )

  const getOrdersByCustomer = (customerId: string) => computed(() =>
    orders.value.filter(o => o.customerId === customerId)
  )

  // Actions (existing code...)
  async function fetchOrders(params: Partial<OrderSearchParams> = {}) {
    // ... existing implementation
  }

  // ... rest of the code

  return {
    // State
    orders,
    currentOrder,
    loading,
    error,
    pagination,

    // Getters
    orderCount,
    pendingOrders,
    confirmedOrders,
    processingOrders,
    completedOrders,
    cancelledOrders,
    newOrders,
    upgradeOrders,
    downgradeOrders,
    renewOrders,
    cancelOrders,
    urgentOrders,
    highPriorityOrders,
    getOrderById,
    getOrdersByType,
    getOrdersByStatus,
    getOrdersByPriority,
    getOrdersByCustomer,

    // Actions
    fetchOrders,
    fetchOrderById,
    createOrder,
    updateOrderStatus,
    cancelOrder,
    deleteOrder,
    searchOrders,
    getOrdersByCustomerId,
    setPage,
    setSize,
    setSort,
    reset
  }
})
```

---

## 4. Coverage Configuration

### vitest.config.ts (With Coverage)

```typescript
import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    include: ['tests/unit/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
    exclude: ['tests/e2e/**'],
    
    // Coverage configuration
    coverage: {
      provider: 'v8',
      reporter: [
        'text',
        'text-summary',
        'json',
        'html',
        'lcov'
      ],
      reportsDirectory: 'coverage',
      exclude: [
        // Coverage exclusions
        'node_modules/',
        'tests/',
        'dist/',
        '.nuxt/',
        '.output/',
        '**/*.d.ts',
        '**/*.config.{js,ts,mjs,cjs}',
        '**/*.spec.{ts,js}',
        '**/*.test.{ts,js}',
        'vitest.setup.ts',
        
        // Helper files
        '**/types/**',
        '**/ mocks/**',
        '**/utils/test/**',
        
        // Main entry files
        'app/main.ts',
        'app/plugins/**',
        'app/middleware/**'
      ],
      
      // Thresholds - FAIL if below
      thresholds: {
        global: {
          branches: 80,
          functions: 80,
          lines: 80,
          statements: 80
        },
        
        // Per-file thresholds
        perFile: {
          statements: 80,
          branches: 70,
          functions: 80,
          lines: 80
        }
      }
    }
  },
  
  resolve: {
    alias: {
      '~': path.resolve(__dirname, './app'),
      '@': path.resolve(__dirname, './app')
    }
  }
})
```

---

## 5. E2E Test Example

### tests/e2e/login-flow.spec.ts (Implementation)

```typescript
import { test, expect } from '@playwright/test'

test.describe('Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear any existing session
    await page.context().clearCookies()
  })

  test('redirects unauthenticated visitor to login page', async ({ page }) => {
    // Attempt to access protected route
    await page.goto('/dashboard')
    
    // Should be redirected to login or show auth gate
    // Adjust based on your auth setup
    await expect(page).toHaveURL(/.*(login|auth|keycloak).*/)
  })

  test('completes Keycloak authentication flow', async ({ page }) => {
    // Navigate to login
    await page.goto('/login')
    
    // Fill in credentials (you may need to adapt this for Keycloak)
    await page.fill('[name="username"]', 'testuser')
    await page.fill('[name="password"]', 'testpass123')
    
    // Submit login
    await page.click('button[type="submit"]')
    
    // Wait for redirect to dashboard
    await page.waitForURL('/dashboard', { timeout: 10000 })
    
    // Verify user is authenticated
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible()
  })

  test('displays dashboard after successful login', async ({ page }) => {
    // Login first (you might extract this to a test helper)
    await page.goto('/login')
    await page.fill('[name="username"]', 'testuser')
    await page.fill('[name="password"]', 'testpass123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')
    
    // Check dashboard elements
    await expect(page.locator('h1')).toContainText('Dashboard')
    await expect(page.locator('[data-testid="welcome-message"]')).toBeVisible()
  })

  test('handles logout correctly', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.fill('[name="username"]', 'testuser')
    await page.fill('[name="password"]', 'testpass123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')
    
    // Click logout
    await page.click('[data-testid="user-menu"]')
    await page.click('[data-testid="logout-button"]')
    
    // Should redirect to login
    await page.waitForURL(/.*(login|auth).*/)
    
    // Verify not authenticated
    await expect(page.locator('[data-testid="dashboard-link"]')).not.toBeVisible()
  })

  test('maintains session on page refresh', async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('[name="username"]', 'testuser')
    await page.fill('[name="password"]', 'testpass123')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')
    
    // Refresh page
    await page.reload()
    
    // Should still be authenticated
    await expect(page.locator('h1')).toContainText('Dashboard')
  })
})
```

---

## 6. GitHub Actions Example

### .github/workflows/frontend-tests.yml

```yaml
name: Frontend Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  PNPM_VERSION: 9

jobs:
  lint-and-typecheck:
    name: Lint & Typecheck
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        
      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: ${{ env.PNPM_VERSION }}
          
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 21
          cache: 'pnpm'
          
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
        working-directory: ./frontend
        
      - name: Run linter
        run: pnpm run lint
        working-directory: ./frontend
        
      - name: Run typecheck
        run: pnpm run typecheck
        working-directory: ./frontend

  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        
      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: ${{ env.PNPM_VERSION }}
          
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 21
          cache: 'pnpm'
          
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
        working-directory: ./frontend
        
      - name: Run unit tests with coverage
        run: pnpm run test:unit:coverage
        working-directory: ./frontend
        
      - name: Upload coverage reports
        uses: codecov/codecov-action@v4
        with:
          files: ./frontend/coverage/lcov.info
          flags: frontend-unit
          name: frontend-unit-coverage
          fail_ci_if_error: false

  e2e-tests:
    name: E2E Tests
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        
      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: ${{ env.PNPM_VERSION }}
          
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 21
          cache: 'pnpm'
          
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
        working-directory: ./frontend
        
      - name: Install Playwright
        run: pnpm exec playwright install --with-deps
        working-directory: ./frontend
        
      - name: Build application
        run: pnpm run build
        working-directory: ./frontend
        
      - name: Run E2E tests
        run: pnpm run test:e2e
        working-directory: ./frontend
        
      - name: Upload test artifacts
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
          retention-days: 30

  test-summary:
    name: Test Summary
    needs: [unit-tests, e2e-tests]
    runs-on: ubuntu-latest
    if: always()
    
    steps:
      - name: Test Results
        run: |
          echo "Unit tests: ${{ needs.unit-tests.result }}"
          echo "E2E tests: ${{ needs.e2e-tests.result }}"
          if [[ "${{ needs.unit-tests.result }}" == "failure" || "${{ needs.e2e-tests.result }}" == "failure" ]]; then
            echo "Some tests failed!"
            exit 1
          fi
```

---

## 7. Pre-commit Hooks Example

### .pre-commit-config.yaml

```yaml
# Pre-commit configuration file
# See https://pre-commit.com for more information

repos:
  # Built-in hooks
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace
        exclude: '\.md$'
      - id: end-of-file-fixer
        exclude: '\.md$'
      - id: check-yaml
      - id: check-json
      - id: check-toml
      - id: check-added-large-files
        args: ['--maxkb=1000']

  # TypeScript check
  - repo: https://github.com/pre-commit/mirrors-typescript
    rev: v5.3.3
    hooks:
      - id: typecheck
        files: '^(app|tests)/.*\.(ts|tsx)$'
        entry: pnpm run typecheck
        language: system
        pass_filenames: false

  # ESLint
  - repo: local
    hooks:
      - id: eslint
        name: ESLint
        entry: pnpm run lint
        language: system
        files: '^(app|tests)/.*\.(ts|tsx|vue)$'
        pass_filenames: false

  # Unit tests
  - repo: local
    hooks:
      - id: unit-tests
        name: Unit Tests
        entry: pnpm run test:unit:coverage
        language: system
        files: '^(app|tests)/.*\.(ts|tsx|vue)$'
        pass_filenames: false
        stages: [commit]
        
  # Playwright tests (optional, can be slow)
  - repo: local
    hooks:
      - id: e2e-tests
        name: E2E Tests
        entry: pnpm run test:e2e
        language: system
        files: '^(app|tests)/.*\.(ts|tsx|vue)$'
        pass_filenames: false
        stages: [push]
        always_run: false
```

---

## 8. Quick Reference Commands

### Testing Commands
```bash
# Run all unit tests
pnpm run test:unit

# Run with coverage
pnpm run test:unit:coverage

# Watch mode
pnpm run test:unit:watch

# Run specific test file
pnpm run test:unit -- customer

# Run tests with coverage in CI
vitest run --coverage

# Generate HTML coverage report
vitest run --coverage --reporter=html

# Run E2E tests
pnpm run test:e2e

# Run E2E tests with UI
pnpm exec playwright test --ui
```

### CI/CD Commands
```bash
# Test GitHub Actions locally (with act)
act -P ubuntu-latest=nektos/act-environments-ubuntu:18.04 push

# Install pre-commit
pip install pre-commit

# Install hooks
pre-commit install

# Run on all files
pre-commit run --all-files
```

### Debug Commands
```bash
# Verbose test output
pnpm run test:unit -- --reporter=verbose

# Show test runner info
pnpm run test:unit -- --reporter=basic

# List all tests
pnpm run test:unit -- --list

# Run tests matching pattern
pnpm run test:unit -- --grep="fetch customers"
```

---

