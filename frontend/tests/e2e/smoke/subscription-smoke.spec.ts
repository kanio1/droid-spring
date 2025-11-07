/**
 * Subscription Smoke Tests - Critical Path Tests for Subscription Management
 */

import { test, expect } from '@playwright/test'

test.describe('Subscription Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-041: Should display subscription list page', async ({ page }) => {
    await page.goto('/subscriptions')
    await expect(page.locator('h1')).toContainText(/subscriptions/i)
    await expect(page.locator('[data-testid="subscription-list"]')).toBeVisible()
  })

  test('SMOKE-042: Should create a new subscription', async ({ page }) => {
    await page.goto('/subscriptions')

    // Create new subscription
    await page.click('[data-testid="create-subscription-button"]')

    // Fill form
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.selectOption('[name="billingInterval"]', 'monthly')

    // Set trial period
    await page.fill('[name="trialDays"]', '14')

    // Submit
    await page.click('[data-testid="submit-button"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/subscription/i)
  })

  test('SMOKE-043: Should activate a trial subscription', async ({ page }) => {
    // Create trial subscription first
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '14')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Activate subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="activate-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'credit_card')
    await page.fill('[data-testid="card-token"]', 'payment_token')
    await page.click('[data-testid="confirm-activation"]')

    // Verify activation
    const status = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(status).toContain('active')
  })

  test('SMOKE-044: Should cancel a subscription', async ({ page }) => {
    // Create active subscription
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Cancel subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="cancel-button"]')
    await page.selectOption('[data-testid="cancellation-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-cancellation"]')

    // Verify cancellation
    const status = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(status).toContain('cancelled')
  })

  test('SMOKE-045: Should change subscription plan', async ({ page }) => {
    // Create subscription
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-basic')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Change plan
    await page.click('[data-testid="subscription-row-0"] [data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'plan-enterprise')
    await page.selectOption('[data-testid="upgrade-type"]', 'immediate')
    await page.click('[data-testid="confirm-plan-change"]')

    // Verify plan change
    const plan = await page.textContent('[data-testid="subscription-row-0"] [data-testid="plan"]')
    expect(plan).toContain('enterprise')
  })

  test('SMOKE-046: Should view subscription usage', async ({ page }) => {
    await page.goto('/subscriptions')

    // View usage for first subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="usage-button"]')

    // Verify usage metrics
    await expect(page.locator('[data-testid="usage-metrics"]')).toBeVisible()
    await expect(page.locator('[data-testid="usage-data"]')).toBeVisible()
  })

  test('SMOKE-047: Should display next billing date', async ({ page }) => {
    await page.goto('/subscriptions')

    // Verify next billing date is displayed
    const billingDate = page.locator('[data-testid="subscription-row-0"] [data-testid="next-billing"]')
    await expect(billingDate).toBeVisible()
  })

  test('SMOKE-048: Should filter subscriptions by status', async ({ page }) => {
    await page.goto('/subscriptions')

    // Filter by status
    await page.selectOption('[data-testid="status-filter"]', 'active')
    await page.waitForTimeout(500)

    // Verify filtered results
    const rows = page.locator('[data-testid="subscription-row"]')
    await expect(rows.first()).toBeVisible()
  })

  test('SMOKE-049: Should display subscription plan details', async ({ page }) => {
    await page.goto('/subscriptions')

    // Verify plan details are displayed
    const planName = page.locator('[data-testid="subscription-row-0"] [data-testid="plan-name"]')
    await expect(planName).toBeVisible()
    const planPrice = page.locator('[data-testid="subscription-row-0"] [data-testid="plan-price"]')
    await expect(planPrice).toBeVisible()
  })

  test('SMOKE-050: Should handle subscription trial period', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '30')
    await page.click('[data-testid="submit-button"]')

    // Verify trial status
    const status = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(status).toContain('trial')
  })
})
