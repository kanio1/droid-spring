/**
 * Multi-Tenant Isolation Tests
 *
 * Tests to verify proper tenant data isolation and security
 * Ensures no cross-tenant data leakage
 *
 * Playwright 1.56.1 features used:
 * - Multiple browser contexts for parallel tenant testing
 * - Client certificates authentication (mTLS)
 * - Network interception for data validation
 * - Storage state management per tenant
 */

import { test, expect, BrowserContext } from '@playwright/test'
import { registerCustomMatchers } from '../framework/matchers/playwright-matchers'
import { TenantFactory, TenantProfiles } from '../framework/data-factories'
import { CustomerFactory, OrderFactory } from '../framework/data-factories'

// Register custom matchers
registerCustomMatchers()

test.describe('Multi-Tenant Data Isolation', () => {
  let tenantA: any
  let tenantB: any
  let contextA: BrowserContext
  let contextB: BrowserContext

  test.beforeAll(async ({ browser }) => {
    // Create test tenants
    tenantA = TenantProfiles.activeEnterprise
    tenantB = TenantProfiles.activePremium

    // Create isolated browser contexts for each tenant
    contextA = await browser.newContext({
      storageState: `tenants/${tenantA.code}-auth.json`,
      viewport: { width: 1920, height: 1080 }
    })

    contextB = await browser.newContext({
      storageState: `tenants/${tenantB.code}-auth.json`,
      viewport: { width: 1920, height: 1080 }
    })
  })

  test.afterAll(async () => {
    await contextA.close()
    await contextB.close()
  })

  test('should isolate customer data between tenants', async () => {
    const pageA = await contextA.newPage()
    const pageB = await contextB.newPage()

    // Create customers for each tenant
    const customerA = CustomerFactory.create()
      .withEmail('customerA@tenant-a.com')
      .active()
      .build()

    const customerB = CustomerFactory.create()
      .withEmail('customerB@tenant-b.com')
      .active()
      .build()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Verify tenant A context
    await expect(pageA).toHaveTenantContext(tenantA.code)

    // Create customer in tenant A
    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    await pageA.click('[data-testid="create-customer"]')
    await pageA.fill('[data-testid="firstName"]', customerA.firstName)
    await pageA.fill('[data-testid="lastName"]', customerA.lastName)
    await pageA.fill('[data-testid="email"]', customerA.email)
    await pageA.click('[data-testid="save-customer"]')
    await pageA.waitForLoadState('networkidle')

    // Login to tenant B
    await pageB.goto(`/tenant/${tenantB.code}/login`)
    await pageB.fill('[data-testid="email"]', 'admin@tenant-b.com')
    await pageB.fill('[data-testid="password"]', 'password123')
    await pageB.click('[data-testid="login-button"]')
    await pageB.waitForLoadState('networkidle')

    // Verify tenant B context
    await expect(pageB).toHaveTenantContext(tenantB.code)

    // Create customer in tenant B
    await pageB.goto(`/tenant/${tenantB.code}/customers`)
    await pageB.click('[data-testid="create-customer"]')
    await pageB.fill('[data-testid="firstName"]', customerB.firstName)
    await pageB.fill('[data-testid="lastName"]', customerB.lastName)
    await pageB.fill('[data-testid="email"]', customerB.email)
    await pageB.click('[data-testid="save-customer"]')
    await pageB.waitForLoadState('networkidle')

    // Verify tenant isolation
    await expect(pageA).toHaveTenantIsolation(tenantA.id)
    await expect(pageB).toHaveTenantIsolation(tenantB.id)
    await expect(pageA).toNotLeakToOtherTenant(tenantA.id)
    await expect(pageB).toNotLeakToOtherTenant(tenantB.id)

    // Customer A should not be visible in tenant B
    await pageB.goto(`/tenant/${tenantB.code}/customers`)
    const customerListB = pageB.locator('[data-testid^="customer-"]')
    const countB = await customerListB.count()
    expect(countB).toBe(1) // Only customer B should be visible

    // Customer B should not be visible in tenant A
    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    const customerListA = pageA.locator('[data-testid^="customer-"]')
    const countA = await customerListA.count()
    expect(countA).toBe(1) // Only customer A should be visible

    await pageA.close()
    await pageB.close()
  })

  test('should prevent cross-tenant data access via API', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Intercept API requests
    const apiRequests: any[] = []
    await pageA.route('**/api/**', route => {
      apiRequests.push({
        url: route.request().url(),
        method: route.request().method()
      })
      route.continue()
    })

    // Create customer in tenant A
    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    await pageA.click('[data-testid="create-customer"]')
    const customer = CustomerFactory.create().active().build()
    await pageA.fill('[data-testid="firstName"]', customer.firstName)
    await pageA.fill('[data-testid="lastName"]', customer.lastName)
    await pageA.fill('[data-testid="email"]', customer.email)
    await pageA.click('[data-testid="save-customer"]')
    await pageA.waitForLoadState('networkidle')

    // Verify all API requests include tenant context
    for (const request of apiRequests) {
      // Check if request includes tenant ID header
      expect(request.url).toMatch(/tenantId=/)
    }

    await pageA.close()
  })

  test('should enforce tenant-specific RBAC', async () => {
    const pageA = await contextA.newPage()

    // Login as regular user (not admin)
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'user@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Verify user role
    await expect(pageA).toHaveTenantRole('user')

    // Try to access admin-only functionality
    await pageA.goto(`/tenant/${tenantA.code}/settings`)

    // Admin settings should be hidden for regular user
    const adminSettings = pageA.locator('[data-testid="admin-only-tenant-settings"]')
    await expect(adminSettings).toBeHidden()

    // Basic user settings should be visible
    const userSettings = pageA.locator('[data-testid="user-settings"]')
    if (await userSettings.count() > 0) {
      await expect(userSettings).toBeVisible()
    }

    await pageA.close()
  })

  test('should handle tenant context switching', async () => {
    const page = await contextA.newPage()

    // User with access to multiple tenants
    await page.goto('/login')
    await page.fill('[data-testid="email"]', 'multi-tenant@company.com')
    await page.fill('[data-testid="password"]', 'password123')
    await page.click('[data-testid="login-button"]')
    await page.waitForLoadState('networkidle')

    // Tenant switcher should be visible
    const tenantSwitcher = page.locator('[data-testid="tenant-switcher"]')
    await expect(tenantSwitcher).toBeVisible()

    // Switch to tenant A
    await page.click('[data-testid="tenant-switcher"]')
    await page.click(`[data-testid="tenant-option-${tenantA.code}"]`)
    await page.waitForLoadState('networkidle')

    // Verify context switch
    await expect(page).toHaveTenantContext(tenantA.code)
    await expect(page).toHaveTenantIsolation(tenantA.id)

    // Switch to tenant B
    await page.click('[data-testid="tenant-switcher"]')
    await page.click(`[data-testid="tenant-option-${tenantB.code}"]`)
    await page.waitForLoadState('networkidle')

    // Verify context switch
    await expect(page).toHaveTenantContext(tenantB.code)
    await expect(page).toHaveTenantIsolation(tenantB.id)

    // Data should be different
    const customerList = page.locator('[data-testid^="customer-"]')
    await expect(customerList).toHaveCount(0) // Different customers in different tenant

    await page.close()
  })

  test('should prevent data leakage via URL manipulation', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Try to access tenant B data by manipulating URL
    await pageA.goto(`/tenant/${tenantB.code}/customers`)

    // Should be redirected to access denied or tenant A dashboard
    const accessDenied = pageA.locator('[data-testid="access-denied"], h1:has-text("Unauthorized")')
    if (await accessDenied.count() > 0) {
      await expect(accessDenied).toBeVisible()
    } else {
      // Should be redirected back to tenant A
      await expect(pageA).toHaveTenantContext(tenantA.code)
    }

    await pageA.close()
  })

  test('should handle mTLS client certificates', async () => {
    const page = await contextA.newPage()

    // Try to access API with client certificate
    const response = await page.request.get(`/api/tenants/${tenantA.id}/customers`, {
      ignoreHTTPSErrors: true,
      certificates: {
        clientCert: './certs/client.crt',
        clientKey: './certs/client.key',
        ca: './certs/ca.crt'
      }
    })

    // Should succeed with valid certificate
    expect(response.status()).toBe(200)

    // Try to access without certificate
    const responseNoCert = await page.request.get(`/api/tenants/${tenantA.id}/customers`, {
      ignoreHTTPSErrors: true
    })

    // Should fail without certificate
    expect([401, 403]).toContain(responseNoCert.status())

    await page.close()
  })

  test('should isolate orders and invoices between tenants', async () => {
    const pageA = await contextA.newPage()
    const pageB = await contextB.newPage()

    // Create order in tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    const orderA = OrderFactory.create()
      .delivered()
      .build()

    await pageA.goto(`/tenant/${tenantA.code}/orders`)
    await pageA.click('[data-testid="create-order"]')
    await pageA.click('[data-testid="select-customer"]')
    await pageA.click('[data-testid="customer-option-0"]')
    await pageA.click('[data-testid="save-order"]')
    await pageA.waitForLoadState('networkidle')

    // Create order in tenant B
    await pageB.goto(`/tenant/${tenantB.code}/login`)
    await pageB.fill('[data-testid="email"]', 'admin@tenant-b.com')
    await pageB.fill('[data-testid="password"]', 'password123')
    await pageB.click('[data-testid="login-button"]')
    await pageB.waitForLoadState('networkidle')

    const orderB = OrderFactory.create()
      .delivered()
      .build()

    await pageB.goto(`/tenant/${tenantB.code}/orders`)
    await pageB.click('[data-testid="create-order"]')
    await pageB.click('[data-testid="select-customer"]')
    await pageB.click('[data-testid="customer-option-0"]')
    await pageB.click('[data-testid="save-order"]')
    await pageB.waitForLoadState('networkidle')

    // Verify isolation
    await pageA.goto(`/tenant/${tenantA.code}/orders`)
    const orderListA = pageA.locator('[data-testid^="order-"]')
    await expect(orderListA).toHaveCount(1) // Only order A

    await pageB.goto(`/tenant/${tenantB.code}/orders`)
    const orderListB = pageB.locator('[data-testid^="order-"]')
    await expect(orderListB).toHaveCount(1) // Only order B

    await expect(pageA).toNotLeakToOtherTenant(tenantA.id)
    await expect(pageB).toNotLeakToOtherTenant(tenantB.id)

    await pageA.close()
    await pageB.close()
  })
})
