/**
 * Authentication Smoke Tests - Critical Path Tests for Auth and Security
 */

import { test, expect } from '@playwright/test'

test.describe('Authentication Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-067: Should display login form', async ({ page }) => {
    await page.goto('/login')
    await expect(page.locator('h1')).toContainText(/login|sign in/i)
    await expect(page.locator('[name="username"]')).toBeVisible()
    await expect(page.locator('[name="password"]')).toBeVisible()
  })

  test('SMOKE-068: Should login with valid credentials', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')

    // Should redirect to dashboard
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('SMOKE-069: Should reject invalid credentials', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'wrongpassword')
    await page.click('[data-testid="submit-button"]')

    // Should show error
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
  })

  test('SMOKE-070: Should redirect to login when not authenticated', async ({ page }) => {
    // Try to access protected route
    await page.goto('/customers')

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('SMOKE-071: Should remember session', async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-button"]')

    // Refresh page
    await page.reload()

    // Should still be logged in
    await expect(page).toHaveURL(/\/dashboard/)
  })

  test('SMOKE-072: Should handle password reset', async ({ page }) => {
    await page.goto('/login')
    await page.click('[data-testid="forgot-password-link"]')

    // Should go to password reset page
    await expect(page).toHaveURL(/\/password-reset/)
    await expect(page.locator('[name="email"]')).toBeVisible()
  })
})
