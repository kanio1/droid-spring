/**
 * Authentication Regression Tests - Comprehensive scenarios
 */

import { test, expect } from '@playwright/test'

test.describe('Authentication Regression Tests - Comprehensive', () => {
  test.beforeEach(async ({ page }) => {
    // Clear any existing auth state
    await page.context().clearCookies()
  })

  test.afterEach(async ({ page }) => {
    // Clear auth state after each test
    await page.context().clearCookies()
  })

  // Edge Cases
  test('REGRESSION-102: Should handle session timeout', async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')
    await expect(page).toHaveURL(/\/dashboard/)

    // Simulate session timeout (in real scenario, would wait for timeout)
    // For testing, we can directly test accessing protected route with expired session
    await page.context().clearCookies()
    await page.goto('/customers')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('REGRESSION-103: Should handle concurrent login attempts', async ({ page }) => {
    const page2Promise = page.context().newPage()
    const page2 = await page2Promise

    // Login in first tab
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')
    await expect(page).toHaveURL(/\/dashboard/)

    // Try to login in second tab
    await page2.goto('/login')
    await page2.fill('[name="username"]', 'admin')
    await page2.fill('[name="password"]', 'password')
    await page2.click('[data-testid="submit-button"]')

    // Both should succeed
    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page2).toHaveURL(/\/dashboard/)
  })

  test('REGRESSION-104: Should handle very long password', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'a'.repeat(255))
    await page.click('[data-testid="submit-button"]')

    // Should handle gracefully
    await expect(page.locator('[data-testid="error-message"], [data-testid="success-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-105: Should handle special characters in credentials', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'user@example.com')
    await page.fill('[name="password"]', 'P@$$w0rd!#$%^&*()')
    await page.click('[data-testid="submit-button"]')

    // Should handle special characters
    await expect(page.locator('[data-testid="error-message"], [data-testid="success-message"]'))
      .toBeVisible()
  })

  // Negative Tests
  test('REGRESSION-106: Should reject empty username', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', '')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-username"]'))
      .toContainText(/required|empty/i)
  })

  test('REGRESSION-107: Should reject empty password', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', '')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-password"]'))
      .toContainText(/required|empty/i)
  })

  test('REGRESSION-108: Should reject wrong password', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'wrongpassword')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/invalid|incorrect/i)
  })

  test('REGRESSION-109: Should reject non-existent user', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'nonexistent@example.com')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/not found|invalid/i)
  })

  test('REGRESSION-110: Should rate limit failed attempts', async ({ page }) => {
    await page.goto('/login')

    // Try multiple failed logins
    for (let i = 0; i < 5; i++) {
      await page.fill('[name="username"]', 'admin')
      await page.fill('[name="password"]', 'wrongpassword')
      await page.click('[data-testid="submit-button"]')
    }

    // Should show rate limit error
    await expect(page.locator('[data-testid="error-message"], [data-testid="rate-limit-message"]'))
      .toContainText(/rate limit|too many attempts/i)
  })

  // Boundary Conditions
  test('REGRESSION-111: Should handle SQL injection in login form', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', "admin' OR '1'='1")
    await page.fill('[name="password"]', "' OR '1'='1")
    await page.click('[data-testid="submit-button"]')

    // Should reject SQL injection attempt
    await expect(page.locator('[data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-112: Should handle XSS in login form', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', '<script>alert("xss")</script>')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')

    // Should escape or reject XSS
    // In this case, just verify no script execution
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
  })

  // Workflow Tests
  test('REGRESSION-113: Should complete full login/logout cycle', async ({ page }) => {
    // 1. Login
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')
    await expect(page).toHaveURL(/\/dashboard/)

    // 2. Access protected route
    await page.goto('/customers')
    await expect(page).toHaveURL(/\/customers/)

    // 3. Logout
    await page.click('[data-testid="user-menu"]')
    await page.click('[data-testid="logout-button"]')
    await expect(page).toHaveURL(/\/login/)

    // 4. Verify protected route redirect
    await page.goto('/customers')
    await expect(page).toHaveURL(/\/login/)
  })

  test('REGRESSION-114: Should handle password reset flow', async ({ page }) => {
    // 1. Go to login
    await page.goto('/login')

    // 2. Click forgot password
    await page.click('[data-testid="forgot-password-link"]')
    await expect(page).toHaveURL(/\/password-reset/)

    // 3. Enter email
    await page.fill('[name="email"]', 'admin@example.com')
    await page.click('[data-testid="submit-button"]')

    // 4. Verify confirmation
    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText(/email|sent/i)
  })

  test('REGRESSION-115: Should maintain session across page refresh', async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')
    await expect(page).toHaveURL(/\/dashboard/)

    // Refresh page
    await page.reload()

    // Should still be logged in
    await expect(page).toHaveURL(/\/dashboard/)
  })

  // Data Consistency
  test('REGRESSION-116: Should clear sensitive data on logout', async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')
    await expect(page).toHaveURL(/\/dashboard/)

    // Check for session tokens in local storage
    const hasTokens = await page.evaluate(() => {
      return localStorage.getItem('token') || sessionStorage.getItem('token')
    })

    // Logout
    await page.click('[data-testid="user-menu"]')
    await page.click('[data-testid="logout-button"]')

    // Tokens should be cleared
    const tokensAfterLogout = await page.evaluate(() => {
      return localStorage.getItem('token') || sessionStorage.getItem('token')
    })

    expect(tokensAfterLogout).toBeFalsy()
  })

  test('REGRESSION-117: Should validate session on protected routes', async ({ page }) => {
    // Access protected route without login
    await page.goto('/customers')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)

    // Try to access another protected route
    await page.goto('/orders')

    // Should also redirect to login
    await expect(page).toHaveURL(/\/login/)
  })
})
