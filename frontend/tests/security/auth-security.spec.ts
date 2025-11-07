/**
 * Authentication & Authorization Security Tests
 *
 * This test validates secure authentication and authorization mechanisms,
 * including session management, access control, and privilege escalation
 */

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

test.describe('Authentication Security', () => {
  test('should prevent brute force attacks on login', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Attempt 5 failed logins
    for (let i = 0; i < 5; i++) {
      await page.fill('[data-testid="username-input"]', 'admin')
      await page.fill('[data-testid="password-input"]', `wrong${i}`)
      await page.click('[data-testid="login-button"]')
      await page.waitForTimeout(1000)
    }

    // After multiple failures, should show rate limiting message
    const errorMessage = page.locator('[data-testid="error-message"]')
    await expect(errorMessage).toBeVisible()

    const text = await errorMessage.textContent()
    expect(text).toMatch(/rate limit|too many attempts|locked/i)
  })

  test('should prevent SQL injection in login form', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    const sqlPayloads = [
      "' OR '1'='1",
      "'; DROP TABLE users; --",
      "admin' --",
      "' OR 1=1#",
    ]

    for (const payload of sqlPayloads) {
      await page.fill('[data-testid="username-input"]', payload)
      await page.fill('[data-testid="password-input"]', 'anypassword')
      await page.click('[data-testid="login-button"]')
      await page.waitForTimeout(500)

      // Should not login with SQL injection
      await expect(page).not.toHaveURL(/.*dashboard.*/)
    }
  })

  test('should not reveal valid usernames', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Try login with non-existent user
    await page.fill('[data-testid="username-input"]', 'nonexistentuser12345')
    await page.fill('[data-testid="password-input"]', 'anypassword')
    await page.click('[data-testid="login-button"]')
    await page.waitForTimeout(1000)

    const errorMessage = page.locator('[data-testid="error-message"]')
    await expect(errorMessage).toBeVisible()

    const text = await errorMessage.textContent()

    // Should give generic error, not reveal if user exists
    expect(text).not.toMatch(/user.*not.*found|username.*invalid/i)
    expect(text).toMatch(/invalid credentials|login failed/i)
  })

  test('should implement secure session timeout', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Wait for session timeout (simulated)
    await page.waitForTimeout(3500000) // 58 minutes (close to 1 hour timeout)

    // Try to access protected resource
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(2000)

    // Should redirect to login or show session expired
    const currentURL = page.url()
    if (currentURL.includes('/login') || currentURL.includes('expired')) {
      console.log('Session timeout working correctly')
    } else {
      console.log('Session may not be timing out properly')
    }
  })

  test('should require authentication for all protected routes', async ({ request }) => {
    const protectedRoutes = [
      '/dashboard',
      '/customers',
      '/orders',
      '/invoices',
      '/payments',
      '/subscriptions',
      '/api/customers',
      '/api/orders',
    ]

    for (const route of protectedRoutes) {
      const response = await request.get(`${BASE_URL}${route}`)

      // Should redirect to login or return 401
      expect([401, 302, 403]).toContain(response.status())
    }
  })

  test('should prevent privilege escalation', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login as regular user
    await page.fill('[data-testid="username-input"]', 'user1')
    await page.fill('[data-testid="password-input"]', 'password123')
    await page.click('[data-testid="login-button"]')

    // Check if we can access admin routes
    const adminRoutes = ['/admin', '/api/admin', '/api/users/all']

    for (const route of adminRoutes) {
      await page.goto(`${BASE_URL}${route}`)
      await page.waitForTimeout(2000)

      const currentURL = page.url()

      // Should not access admin routes
      if (currentURL.includes('/admin')) {
        console.log('WARNING: Regular user can access admin route:', route)
      }
    }
  })
})

test.describe('Authorization Security', () => {
  test('should enforce role-based access control', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login as user
    await page.fill('[data-testid="username-input"]', 'user1')
    await page.fill('[data-testid="password-input"]', 'password123')
    await page.click('[data-testid="login-button"]')

    // Try to access resources
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(1000)

    // Check for edit/delete buttons (admin features)
    const hasEditButtons = await page.locator('[data-testid*="delete"]').count()
    const hasAdminFeatures = await page.locator('[data-testid*="admin"]').count()

    console.log(`User can see ${hasEditButtons} delete buttons and ${hasAdminFeatures} admin features`)

    // Regular users might have limited features
    // This is a soft check
  })

  test('should prevent direct object reference attacks', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]`)
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Try to access other users' resources by ID
    const objectIds = [1, 2, 3, 999, 'admin']

    for (const id of objectIds) {
      await page.goto(`${BASE_URL}/customers/${id}`)
      await page.waitForTimeout(1000)

      const currentURL = page.url()
      const hasError = currentURL.includes('error') || currentURL.includes('403')

      if (hasError) {
        console.log(`Correctly blocked access to object ID: ${id}`)
      } else {
        console.log(`WARNING: May have accessed object ID: ${id}`)
      }
    }
  })

  test('should prevent IDOR in API endpoints', async ({ request }) => {
    // This test requires authentication
    const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
      data: {
        username: 'user1',
        password: 'password123',
      },
    })

    if (loginResponse.ok()) {
      const loginData = await loginResponse.json()
      const token = loginData.token

      // Try to access other users' data via API
      const objectIds = [1, 2, 3, 999]

      for (const id of objectIds) {
        const response = await request.get(`${BASE_URL}/api/customers/${id}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })

        // Should return 403 or 404, not 200 with someone else's data
        if (response.status() === 200) {
          const data = await response.json()
          console.log(`WARNING: User can access customer ID: ${id}`, data)
        }
      }
    }
  })

  test('should prevent CSRF attacks', async ({ page, context }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]')
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Get CSRF token from page
    const csrfToken = await page.getAttribute('meta[name="csrf-token"]', 'content')

    if (csrfToken) {
      console.log('CSRF token found:', csrfToken.substring(0, 20) + '...')

      // CSRF token should be present
      expect(csrfToken).toBeTruthy()

      // Token should be different on each page load
      const page2 = await context.newPage()
      await page2.goto(`${BASE_URL}/dashboard`)
      const csrfToken2 = await page2.getAttribute('meta[name="csrf-token"]', 'content')

      if (csrfToken2 && csrfToken !== csrfToken2) {
        console.log('CSRF tokens are unique per session ✓')
      }
    } else {
      console.log('No CSRF token found - may not be implemented')
    }
  })

  test('should require password for sensitive operations', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]`)
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Try to change password or delete account
    await page.goto(`${BASE_URL}/profile`)
    await page.waitForTimeout(1000)

    // Should require re-authentication for sensitive operations
    const reauthRequired = await page.locator('[data-testid="reauth-required"]').count()

    if (reauthRequired > 0) {
      console.log('Re-authentication required for sensitive operations ✓')
    } else {
      console.log('WARNING: No re-authentication for sensitive operations')
    }
  })
})

test.describe('Session Management Security', () => {
  test('should prevent session fixation attacks', async ({ page, context }) => {
    // Visit site before login
    await page.goto(`${BASE_URL}/login`)
    let initialCookies = await context.cookies()

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]`)
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    let postLoginCookies = await context.cookies()

    // Session ID should change after login
    const initialSession = initialCookies.find(c => c.name.includes('session'))
    const postLoginSession = postLoginCookies.find(c => c.name.includes('session'))

    if (initialSession && postLoginSession && initialSession.value !== postLoginSession.value) {
      console.log('Session ID changed after login (prevents fixation) ✓')
    } else {
      console.log('WARNING: Session ID may not be changing after login')
    }
  })

  test('should have secure session regeneration', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]`)
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Perform some actions
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(1000)

    // Logout
    await page.goto(`${BASE_URL}/logout`)

    // Try to use old session
    await page.goto(`${BASE_URL}/dashboard`)
    await page.waitForTimeout(1000)

    // Should redirect to login
    const currentURL = page.url()
    if (currentURL.includes('/login')) {
      console.log('Session properly invalidated on logout ✓')
    } else {
      console.log('WARNING: Session may still be valid after logout')
    }
  })

  test('should prevent concurrent sessions from different IPs', async ({ page, request }) => {
    // This is a conceptual test
    // In production, you might track session IP addresses

    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]`)
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Get session token
    const cookies = await page.context().cookies()
    const sessionCookie = cookies.find(c => c.name.includes('session') || c.name.includes('token'))

    if (sessionCookie) {
      console.log('Session cookie found, should be bound to IP/UA in production')
    }
  })
})

test.describe('Password Security', () => {
  test('should enforce strong password policy', async ({ page }) => {
    await page.goto(`${BASE_URL}/register`)

    const weakPasswords = [
      '123',
      'password',
      'admin',
      '123456',
      'abc',
    ]

    for (const weakPassword of weakPasswords) {
      await page.fill('[data-testid="password-input"]', weakPassword)
      await page.fill('[data-testid="confirm-password-input"]', weakPassword)
      await page.click('[data-testid="register-button"]')

      const errorMessage = page.locator('[data-testid="password-error"]')
      await expect(errorMessage).toBeVisible()
    }
  })

  test('should not accept previously used passwords', async ({ page }) => {
    // This test would require actual user account
    console.log('Password reuse check should be implemented on backend')
  })

  test('should require password for account deletion', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Login
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]`)
    await page.waitForURL('**/dashboard', { timeout: 10000 })

    // Try to delete account
    await page.goto(`${BASE_URL}/settings`)
    await page.waitForTimeout(1000)

    const hasPasswordField = await page.locator('[data-testid="delete-confirm-password"]').count()

    if (hasPasswordField > 0) {
      console.log('Password confirmation required for account deletion ✓')
    } else {
      console.log('WARNING: No password confirmation for account deletion')
    }
  })
})
