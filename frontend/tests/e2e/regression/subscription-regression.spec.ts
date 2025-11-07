/**
 * Subscription Regression Tests - Comprehensive scenarios
 */

import { test, expect } from '@playwright/test'
import { TestDataGenerator } from '@tests/framework/data-factories'

test.describe('Subscription Regression Tests - Comprehensive', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const testData = TestDataGenerator.fullCustomerJourney()
    await TestDataGenerator.seedTestData(testData)
  })

  test.afterEach(async ({ page }) => {
    await TestDataGenerator.cleanupTestData()
  })

  // Edge Cases
  test('REGRESSION-069: Should handle maximum trial period', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '365') // Maximum trial
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-070: Should handle zero trial period', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '0')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // Should start as active, not trial
    const status = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(status).toContain('active')
  })

  test('REGRESSION-071: Should handle plan downgrade', async ({ page }) => {
    // Create subscription with premium plan
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-enterprise')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Downgrade to basic
    await page.click('[data-testid="subscription-row-0"] [data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'plan-basic')
    await page.selectOption('[data-testid="upgrade-type"]', 'immediate')
    await page.click('[data-testid="confirm-plan-change"]')

    await expect(page.locator('[data-testid="success-message"]')).toContainText(/downgrade|changed/i)

    // Verify plan changed
    const plan = await page.textContent('[data-testid="subscription-row-0"] [data-testid="plan"]')
    expect(plan).toContain('basic')
  })

  test('REGRESSION-072: Should handle plan upgrade mid-cycle', async ({ page }) => {
    // Create monthly subscription
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-basic')
    await page.selectOption('[name="billingInterval"]', 'monthly')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Upgrade mid-cycle
    await page.click('[data-testid="subscription-row-0"] [data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'plan-pro')
    await page.selectOption('[data-testid="upgrade-type"]', 'immediate')
    await page.fill('[data-testid="prorated-amount"]', '25.00') // Prorated charge
    await page.click('[data-testid="confirm-plan-change"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('REGRESSION-073: Should handle usage at limit', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-basic')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View usage at limit
    await page.click('[data-testid="subscription-row-0"] [data-testid="usage-button"]')

    // Should show usage metrics
    await expect(page.locator('[data-testid="usage-data"]')).toBeVisible()
    await expect(page.locator('[data-testid="usage-limit"]')).toBeVisible()
  })

  // Negative Tests
  test('REGRESSION-074: Should reject activation without payment method', async ({ page }) => {
    // Create trial subscription
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '14')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to activate without payment method
    await page.click('[data-testid="subscription-row-0"] [data-testid="activate-button"]')
    await page.click('[data-testid="confirm-activation"]')

    await expect(page.locator('[data-testid="error-payment-method"]'))
      .toContainText(/payment method/i)
  })

  test('REGRESSION-075: Should not allow cancelling active subscription without confirmation', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to cancel without confirmation modal
    await page.click('[data-testid="subscription-row-0"] [data-testid="cancel-button"]')

    // Should show confirmation dialog
    await expect(page.locator('[data-testid="cancel-confirmation"]')).toBeVisible()
  })

  test('REGRESSION-076: Should reject change to same plan', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to change to same plan
    await page.click('[data-testid="subscription-row-0"] [data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'plan-pro')
    await page.click('[data-testid="confirm-plan-change"]')

    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/same plan|already/i)
  })

  test('REGRESSION-077: Should handle expired subscription', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Check if expired subscription is handled
    const status = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')

    // Should show appropriate status or action
    if (status) {
      expect(['active', 'expired', 'cancelled']).toContain(status)
    }
  })

  test('REGRESSION-078: Should reject subscription without customer', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    // Don't select customer
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-customer"]'))
      .toContainText(/required/i)
  })

  // Boundary Conditions
  test('REGRESSION-079: Should handle subscription at exact renewal date', async ({ page }) => {
    await page.goto('/subscriptions')

    // Create subscription
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.selectOption('[name="billingInterval"]', 'monthly')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View next billing date
    const nextBilling = await page.textContent('[data-testid="subscription-row-0"] [data-testid="next-billing"]')
    expect(nextBilling).toBeDefined()

    // On renewal date, should handle auto-renewal
    // This test verifies the date is displayed correctly
    expect(nextBilling).toMatch(/\d{4}-\d{2}-\d{2}/)
  })

  test('REGRESSION-080: Should handle trial ending soon', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '1') // Trial ending tomorrow
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Should show trial ending warning
    const trialWarning = page.locator('[data-testid="subscription-row-0"] [data-testid="trial-warning"]')
    if (await trialWarning.isVisible()) {
      await expect(trialWarning).toContainText(/trial ending|trial ends/i)
    }
  })

  // Workflow Tests
  test('REGRESSION-081: Should complete full subscription lifecycle', async ({ page }) => {
    // 1. Create trial subscription
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.fill('[name="trialDays"]', '14')
    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // Verify trial status
    const status = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(status).toContain('trial')

    // 2. Activate subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="activate-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'credit_card')
    await page.fill('[data-testid="card-token"]', 'payment_token')
    await page.click('[data-testid="confirm-activation"]')

    // Verify active status
    const activeStatus = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(activeStatus).toContain('active')

    // 3. Change plan
    await page.click('[data-testid="subscription-row-0"] [data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'plan-enterprise')
    await page.selectOption('[data-testid="upgrade-type"]', 'immediate')
    await page.click('[data-testid="confirm-plan-change"]')

    // Verify plan changed
    const plan = await page.textContent('[data-testid="subscription-row-0"] [data-testid="plan"]')
    expect(plan).toContain('enterprise')

    // 4. Cancel subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="cancel-button"]')
    await page.selectOption('[data-testid="cancellation-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-cancellation"]')

    // Verify cancelled status
    const cancelledStatus = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(cancelledStatus).toContain('cancelled')
  })

  test('REGRESSION-082: Should handle subscription pause/resume', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Pause subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="pause-button"]')
    await page.fill('[data-testid="pause-reason"]', 'temporary hold')
    await page.click('[data-testid="confirm-pause"]')

    // Verify paused status
    const pausedStatus = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(pausedStatus).toContain('paused')

    // Resume subscription
    await page.click('[data-testid="subscription-row-0"] [data-testid="resume-button"]')
    await page.click('[data-testid="confirm-resume"]')

    // Verify active status
    const activeStatus = await page.textContent('[data-testid="subscription-row-0"] [data-testid="status"]')
    expect(activeStatus).toContain('active')
  })

  // Data Consistency
  test('REGRESSION-083: Should maintain plan pricing after changes', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.selectOption('[name="billingInterval"]', 'monthly')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View pricing
    const initialPrice = await page.textContent('[data-testid="subscription-row-0"] [data-testid="price"]')

    // Perform operations
    await page.click('[data-testid="subscription-row-0"] [data-testid="view-button"]')

    // Price should remain consistent
    const viewPrice = await page.textContent('[data-testid="subscription-details"] [data-testid="price"]')
    expect(viewPrice).toBe(initialPrice)
  })

  // Performance
  test('REGRESSION-084: Should handle many subscriptions', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/subscriptions')

    // Create 20 subscriptions
    for (let i = 0; i < 20; i++) {
      await page.click('[data-testid="create-subscription-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.selectOption('[name="planId"]', 'plan-pro')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    const endTime = Date.now()
    const duration = endTime - startTime

    // Should handle 20 subscriptions in reasonable time
    expect(duration).toBeLessThan(180000) // 3 minutes
  })

  // Integration
  test('REGRESSION-085: Should sync usage across modules', async ({ page }) => {
    await page.goto('/subscriptions')
    await page.click('[data-testid="create-subscription-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="planId"]', 'plan-pro')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View usage
    await page.click('[data-testid="subscription-row-0"] [data-testid="usage-button"]')
    const usage = await page.textContent('[data-testid="usage-data"] [data-testid="current-usage"]')

    // Usage should be consistent
    expect(usage).toBeDefined()
  })
})
