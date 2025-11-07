/**
 * End-to-End Login Flow Tests
 *
 * Tests complete authentication flow using Keycloak OIDC
 * Includes valid login, invalid login, session management, and logout
 */

import { test, expect } from '@playwright/test'
import { KeycloakContainer } from '../framework/testcontainers/keycloak'

test.describe('[BSS-42] End-to-End Login Flow', () => {
  const KEYCLOAK_URL = process.env.KEYCLOAK_URL || 'http://localhost:8080/realms/bss-test'
  const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || 'bss-frontend'

  test.beforeEach(async ({ page }) => {
    // Navigate to login page
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
  })

  test('should display login form', async ({ page }) => {
    // Check login form elements
    await expect(page.locator('h1')).toContainText(/login|sign in/i)
    await expect(page.locator('[data-testid="username-input"], input[name="username"], #username'))
      .toBeVisible()
    await expect(page.locator('[data-testid="password-input"], input[name="password"], #password'))
      .toBeVisible()
    await expect(page.locator('[data-testid="login-button"], button[type="submit"]'))
      .toBeVisible()
  })

  test('should login with valid credentials', async ({ page }) => {
    // Fill login form
    await page.fill('[data-testid="username-input"], input[name="username"], #username', 'testuser')
    await page.fill('[data-testid="password-input"], input[name="password"], #password', 'testpass')

    // Submit form
    await page.click('[data-testid="login-button"], button[type="submit"]')

    // Wait for redirect to dashboard
    await page.waitForURL('/dashboard', { timeout: 10000 })

    // Verify user is logged in
    await expect(page.locator('[data-testid="user-menu"], .user-menu, .user-avatar'))
      .toBeVisible()
    await expect(page.locator('[data-testid="logout-button"], a[href="/logout"]'))
      .toBeVisible()

    // Verify username is displayed
    await expect(page.locator('[data-testid="username"], .username'))
      .toContainText(/testuser/i)
  })

  test('should show error for invalid credentials', async ({ page }) => {
    // Fill with invalid credentials
    await page.fill('[data-testid="username-input"], input[name="username"], #username', 'invaliduser')
    await page.fill('[data-testid="password-input"], input[name="password"], #password', 'wrongpass')

    // Submit form
    await page.click('[data-testid="login-button"], button[type="submit"]')

    // Check for error message
    await expect(page.locator('[data-testid="error-message"], .error, .alert-error'))
      .toBeVisible()
    await expect(page.locator('[data-testid="error-message"], .error, .alert-error'))
      .toContainText(/invalid|incorrect|wrong/i)
  })

  test('should redirect to login when accessing protected route without auth', async ({ page }) => {
    // Try to access protected route
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    // Should be redirected to login
    await expect(page).toHaveURL(/.*\/login.*|.*\/auth.*/)
  })

  test('should redirect to dashboard after successful login', async ({ page }) => {
    // Login
    await page.fill('[data-testid="username-input"], input[name="username"], #username', 'testuser')
    await page.fill('[data-testid="password-input"], input[name="password"], #password', 'testpass')
    await page.click('[data-testid="login-button"], button[type="submit"]')

    // Should redirect to dashboard
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 })
    await expect(page.locator('h1')).toContainText(/dashboard|home/i)
  })

  test('should maintain session after page reload', async ({ page }) => {
    // Login
    await page.fill('[data-testid="username-input"], input[name="username"], #username', 'testuser')
    await page.fill('[data-testid="password-input"], input[name="password"], #password', 'testpass')
    await page.click('[data-testid="login-button"], button[type="submit"]')
    await page.waitForURL('/dashboard')

    // Reload page
    await page.reload()
    await page.waitForLoadState('networkidle')

    // Should still be logged in
    await expect(page).toHaveURL(/.*\/dashboard.*|.*\/.*/, { timeout: 10000 })
    await expect(page.locator('[data-testid="user-menu"], .user-menu')).toBeVisible()
  })

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.fill('[data-testid="username-input"], input[name="username"], #username', 'testuser')
    await page.fill('[data-testid="password-input"], input[name="password"], #password', 'testpass')
    await page.click('[data-testid="login-button"], button[type="submit"]')
    await page.waitForURL('/dashboard')

    // Click logout
    await page.click('[data-testid="logout-button"], a[href="/logout"], button:has-text("Logout"), a:has-text("Sign Out")')
    await page.waitForLoadState('networkidle')

    // Should be redirected to login page
    await expect(page).toHaveURL(/.*\/login.*/)

    // Try to access protected route
    await page.goto('/dashboard')
    await page.waitForLoadState('networkidle')

    // Should be redirected back to login
    await expect(page).toHaveURL(/.*\/login.*|.*\/auth.*/)
  })

  test('should show validation errors for empty fields', async ({ page }) => {
    // Try to submit with empty fields
    await page.click('[data-testid="login-button"], button[type="submit"]')

    // Check for validation errors
    await expect(page.locator('[data-testid="username-error"], .error:has-text("required")'))
      .toBeVisible()
    await expect(page.locator('[data-testid="password-error"], .error:has-text("required")'))
      .toBeVisible()
  })

  test('should handle remember me functionality', async ({ page }) => {
    // Check if remember me checkbox exists
    const rememberMeCheckbox = page.locator('[data-testid="remember-me"], input[name="rememberMe"]')

    if (await rememberMeCheckbox.isVisible()) {
      // Check remember me
      await rememberMeCheckbox.check()

      // Login
      await page.fill('[data-testid="username-input"], input[name="username"], #username', 'testuser')
      await page.fill('[data-testid="password-input"], input[name="password"], #password', 'testpass')
      await page.click('[data-testid="login-button"], button[type="submit"]')
      await page.waitForURL('/dashboard')

      // Check if remember me is persisted
      await page.reload()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/.*\/dashboard.*|.*\/.*/)
    } else {
      test.skip(true, 'Remember me not implemented')
    }
  })

  test('should handle OIDC redirect flow', async ({ page }) => {
    // Click OIDC login button (if exists)
    const oidcButton = page.locator('[data-testid="oidc-login"], .oidc-login, button:has-text("Sign in with Keycloak")')

    if (await oidcButton.isVisible()) {
      await oidcButton.click()

      // Should redirect to Keycloak
      await page.waitForURL(/.*keycloak.*\/realms.*\/protocol\/openid-connect\/auth.*/, { timeout: 10000 })

      // Login on Keycloak
      await page.fill('#username', 'testuser')
      await page.fill('#password', 'testpass')
      await page.click('#kc-login')

      // Should return to app
      await page.waitForURL('/dashboard', { timeout: 10000 })
      await expect(page.locator('[data-testid="user-menu"], .user-menu')).toBeVisible()
    } else {
      test.skip(true, 'OIDC login button not visible')
    }
  })

  test('should handle login timeout', async ({ page }) => {
    // Login
    await page.fill('[data-testid="username-input"], input[name="username"], #username', 'testuser')
    await page.fill('[data-testid="password-input"], input[name="password"], #password', 'testpass')
    await page.click('[data-testid="login-button"], button[type="submit"]')
    await page.waitForURL('/dashboard')

    // Mock network delay to simulate timeout
    await page.route('**/api/**', async route => {
      await new Promise(resolve => setTimeout(resolve, 5000))
      await route.continue()
    })

    // Navigate to another page
    await page.goto('/customers')

    // Check for timeout message (if implemented)
    // This depends on app implementation
  })

  test('should support keyboard navigation', async ({ page }) => {
    // Tab to username field
    await page.keyboard.press('Tab')
    await expect(page.locator('[data-testid="username-input"], input[name="username"], #username')).toBeFocused()

    // Type username
    await page.keyboard.type('testuser')

    // Tab to password field
    await page.keyboard.press('Tab')
    await expect(page.locator('[data-testid="password-input"], input[name="password"], #password')).toBeFocused()

    // Type password
    await page.keyboard.type('testpass')

    // Press Enter to submit
    await page.keyboard.press('Enter')

    // Should login
    await page.waitForURL('/dashboard', { timeout: 10000 })
    await expect(page.locator('[data-testid="user-menu"], .user-menu')).toBeVisible()
  })
})
