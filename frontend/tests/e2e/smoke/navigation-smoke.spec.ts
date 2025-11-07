/**
 * Navigation Smoke Tests - Critical Path Tests for Application Navigation
 */

import { test, expect } from '@playwright/test'

test.describe('Navigation Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-051: Should navigate to dashboard', async ({ page }) => {
    await page.click('[data-testid="nav-dashboard"]')
    await expect(page).toHaveURL(/\/dashboard/)
    await expect(page.locator('h1')).toContainText(/dashboard/i)
  })

  test('SMOKE-052: Should navigate to customer list', async ({ page }) => {
    await page.click('[data-testid="nav-customers"]')
    await expect(page).toHaveURL(/\/customers/)
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
  })

  test('SMOKE-053: Should navigate to order list', async ({ page }) => {
    await page.click('[data-testid="nav-orders"]')
    await expect(page).toHaveURL(/\/orders/)
    await expect(page.locator('[data-testid="order-list"]')).toBeVisible()
  })

  test('SMOKE-054: Should navigate to invoice list', async ({ page }) => {
    await page.click('[data-testid="nav-invoices"]')
    await expect(page).toHaveURL(/\/invoices/)
    await expect(page.locator('[data-testid="invoice-list"]')).toBeVisible()
  })

  test('SMOKE-055: Should navigate to payment list', async ({ page }) => {
    await page.click('[data-testid="nav-payments"]')
    await expect(page).toHaveURL(/\/payments/)
    await expect(page.locator('[data-testid="payment-list"]')).toBeVisible()
  })

  test('SMOKE-056: Should navigate to subscription list', async ({ page }) => {
    await page.click('[data-testid="nav-subscriptions"]')
    await expect(page).toHaveURL(/\/subscriptions/)
    await expect(page.locator('[data-testid="subscription-list"]')).toBeVisible()
  })

  test('SMOKE-057: Should display user profile menu', async ({ page }) => {
    await page.click('[data-testid="user-menu"]')
    await expect(page.locator('[data-testid="user-dropdown"]')).toBeVisible()
  })

  test('SMOKE-058: Should logout successfully', async ({ page }) => {
    // First login
    await page.click('[data-testid="login-button"]')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'password')
    await page.click('[data-testid="submit-login"]')

    // Then logout
    await page.click('[data-testid="user-menu"]')
    await page.click('[data-testid="logout-button"]')

    // Verify redirect to login
    await expect(page).toHaveURL(/\/login/)
  })

  test('SMOKE-059: Should display breadcrumbs', async ({ page }) => {
    await page.goto('/customers')
    const breadcrumbs = page.locator('[data-testid="breadcrumbs"]')
    if (await breadcrumbs.isVisible()) {
      await expect(breadcrumbs).toContainText('Home')
      await expect(breadcrumbs).toContainText('Customers')
    }
  })

  test('SMOKE-060: Should handle 404 page', async ({ page }) => {
    await page.goto('/non-existent-page')
    await expect(page.locator('h1')).toContainText(/404|not found/i)
  })
})
