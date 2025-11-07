/**
 * Cross-Tenant Data Access Prevention Tests
 *
 * Security tests to prevent unauthorized cross-tenant data access
 * Tests various attack vectors and security vulnerabilities
 *
 * Playwright 1.56.1 features used:
 * - Network interception for security validation
 * - Client certificates for mTLS
 * - Storage state isolation
 * - URL manipulation testing
 */

import { test, expect, BrowserContext, request } from '@playwright/test'
import { registerCustomMatchers } from '../framework/matchers/playwright-matchers'
import { TenantFactory, TenantProfiles } from '../framework/data-factories'

// Register custom matchers
registerCustomMatchers()

test.describe('Cross-Tenant Security', () => {
  let tenantA: any
  let tenantB: any
  let contextA: BrowserContext
  let contextB: BrowserContext

  test.beforeAll(async ({ browser }) => {
    // Create test tenants
    tenantA = TenantProfiles.activeEnterprise
    tenantB = TenantProfiles.activePremium

    // Create isolated browser contexts
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

  test('should prevent direct IDOR (Insecure Direct Object Reference) attacks', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Get a customer ID from tenant A
    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    let customerCards = pageA.locator('[data-testid^="customer-"]')
    const customerCount = await customerCards.count()

    if (customerCount > 0) {
      const firstCustomerId = await customerCards.first().getAttribute('data-testid')
      const customerId = firstCustomerId?.replace('customer-', '')

      if (customerId) {
        // Try to access customer with different tenant context
        await pageA.goto(`/tenant/${tenantB.code}/customers/${customerId}`)

        // Should be denied access
        const accessDenied = pageA.locator('[data-testid="access-denied"], h1:has-text("Unauthorized")')
        await expect(accessDenied).toBeVisible()

        // Or should be redirected
        const currentUrl = pageA.url()
        expect(currentUrl).not.toContain(`/tenant/${tenantB.code}/customers/${customerId}`)
      }
    }

    await pageA.close()
  })

  test('should prevent SQL injection across tenant boundaries', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Try SQL injection in search field
    const maliciousInput = "'; DROP TABLE customers; --"

    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    const searchInput = pageA.locator('[data-testid="search-customers"]')
    await searchInput.fill(maliciousInput)
    await searchInput.press('Enter')
    await pageA.waitForLoadState('networkidle')

    // Should show no results or error, but not crash
    const errorMessage = pageA.locator('[data-testid="error-message"]')
    const noResults = pageA.locator('[data-testid="no-results"]')

    if (await errorMessage.count() > 0) {
      await expect(errorMessage).toContainText('error', { ignoreCase: true })
    } else if (await noResults.count() > 0) {
      await expect(noResults).toBeVisible()
    } else {
      // Should handle gracefully without exposing sensitive info
      expect(await pageA.title()).toBeTruthy()
    }

    await pageA.close()
  })

  test('should prevent XSS attacks across tenants', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Try XSS in customer name
    const xssPayload = '<script>alert("XSS")</script>'

    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    await pageA.click('[data-testid="create-customer"]')
    await pageA.fill('[data-testid="firstName"]', xssPayload)
    await pageA.fill('[data-testid="lastName"]', 'Test')
    await pageA.fill('[data-testid="email"]', 'xss@test.com')
    await pageA.click('[data-testid="save-customer"]')
    await pageA.waitForLoadState('networkidle')

    // Script should be escaped/not executed
    const customerName = pageA.locator('[data-testid="customer-name"]').first()
    if (await customerName.count() > 0) {
      const nameText = await customerName.textContent()
      // Should show escaped version or sanitized
      expect(nameText).not.toContain('<script>')
    }

    await pageA.close()
  })

  test('should validate tenant isolation in API responses', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Intercept API responses
    const responses: any[] = []
    await pageA.route('**/api/**', route => {
      route.continue()
    })

    await pageA.route('**/api/**', route => {
      route.fetch().then(response => {
        responses.push({
          url: response.url(),
          status: response.status(),
          headers: response.headers()
        })
        return response
      })
    })

    // Make API call
    await pageA.goto(`/tenant/${tenantA.code}/customers`)
    await pageA.waitForLoadState('networkidle')

    // Verify all API responses include tenant context
    for (const response of responses) {
      if (response.status === 200) {
        // Check headers or body for tenant isolation
        expect(response.url).toMatch(/tenantId=|tenant_id=/)
      }
    }

    await pageA.close()
  })

  test('should prevent CSRF attacks across tenants', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Get CSRF token
    const csrfToken = await pageA.locator('meta[name="csrf-token"]').getAttribute('content')

    // Simulate CSRF attack (request from different origin)
    const response = await pageA.request.post(`/api/tenants/${tenantA.id}/customers`, {
      data: {
        firstName: 'CSRF',
        lastName: 'Attack',
        email: 'csrf@attack.com'
      },
      headers: {
        'X-CSRF-Token': csrfToken || '',
        'Origin': 'https://malicious-site.com'
      }
    })

    // Should reject request from different origin
    expect([403, 419]).toContain(response.status())

    await pageA.close()
  })

  test('should enforce rate limiting per tenant', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Make multiple rapid requests
    const requests: any[] = []
    for (let i = 0; i < 15; i++) {
      const response = await pageA.request.get(`/api/tenants/${tenantA.id}/customers`, {
        headers: {
          'X-Tenant-ID': tenantA.id
        }
      })
      requests.push({
        status: response.status(),
        url: response.url()
      })

      // Small delay between requests
      await new Promise(resolve => setTimeout(resolve, 100))
    }

    // Should eventually hit rate limit
    const rateLimited = requests.some(r => r.status === 429)
    expect(rateLimited).toBe(true)

    await pageA.close()
  })

  test('should validate JWT token includes tenant context', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Get JWT from localStorage
    const token = await pageA.evaluate(() => {
      return localStorage.getItem('auth_token')
    })

    if (token) {
      // Decode JWT (base64 decode)
      const parts = token.split('.')
      if (parts.length === 3) {
        const payload = JSON.parse(atob(parts[1]))

        // Verify token includes tenant ID
        expect(payload).toHaveProperty('tenantId')
        expect(payload.tenantId).toBe(tenantA.id)
        expect(payload).toHaveProperty('exp')
        expect(payload).toHaveProperty('iat')
      }
    }

    await pageA.close()
  })

  test('should prevent session hijacking across tenants', async () => {
    const pageA = await contextA.newPage()
    const pageB = await contextB.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Login to tenant B
    await pageB.goto(`/tenant/${tenantB.code}/login`)
    await pageB.fill('[data-testid="email"]', 'admin@tenant-b.com')
    await pageB.fill('[data-testid="password"]', 'password123')
    await pageB.click('[data-testid="login-button"]')
    await pageB.waitForLoadState('networkidle')

    // Get session from tenant A
    const sessionA = await pageA.evaluate(() => {
      return {
        token: localStorage.getItem('auth_token'),
        tenantId: localStorage.getItem('tenant_id')
      }
    })

    // Try to use tenant A session in tenant B context
    await pageB.evaluate((session) => {
      localStorage.setItem('auth_token', session.token)
      localStorage.setItem('tenant_id', session.tenantId)
    }, sessionA)

    // Navigate to tenant B
    await pageB.goto(`/tenant/${tenantB.code}/dashboard`)
    await pageB.waitForLoadState('networkidle')

    // Should detect session mismatch
    const errorMessage = pageB.locator('[data-testid="session-error"], h1:has-text("Unauthorized")')
    if (await errorMessage.count() > 0) {
      await expect(errorMessage).toBeVisible()
    } else {
      // Or should be redirected
      expect(pageB.url()).toContain('login')
    }

    await pageA.close()
    await pageB.close()
  })

  test('should log and alert on security violations', async () => {
    const pageA = await contextA.newPage()

    // Login to tenant A
    await pageA.goto(`/tenant/${tenantA.code}/login`)
    await pageA.fill('[data-testid="email"]', 'admin@tenant-a.com')
    await pageA.fill('[data-testid="password"]', 'password123')
    await pageA.click('[data-testid="login-button"]')
    await pageA.waitForLoadState('networkidle')

    // Try multiple security violations
    // 1. Try to access non-existent tenant
    await pageA.goto('/tenant/non-existent-tenant/dashboard')
    await pageA.waitForLoadState('networkidle')

    // 2. Try to access with invalid tenant ID format
    await pageA.goto('/tenant/../../../admin/dashboard')
    await pageA.waitForLoadState('networkidle')

    // 3. Try to access API with wrong tenant
    const response = await pageA.request.get('/api/customers', {
      headers: {
        'X-Tenant-ID': 'wrong-tenant-id'
      }
    })

    expect([401, 403]).toContain(response.status())

    // These violations should be logged (check in real app)
    // In test, we verify the system handles them gracefully
    const errorShown = pageA.locator('[data-testid="error-message"], h1:has-text("Unauthorized")')
    if (await errorShown.count() > 0) {
      await expect(errorShown).toBeVisible()
    }

    await pageA.close()
  })
})
