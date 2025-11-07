/**
 * Payment Smoke Tests - Critical Path Tests for Payment Processing
 */

import { test, expect } from '@playwright/test'

test.describe('Payment Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-031: Should display payment list page', async ({ page }) => {
    await page.goto('/payments')
    await expect(page.locator('h1')).toContainText(/payments/i)
    await expect(page.locator('[data-testid="payment-list"]')).toBeVisible()
  })

  test('SMOKE-032: Should process a payment successfully', async ({ page }) => {
    await page.goto('/payments')

    // Create new payment
    await page.click('[data-testid="create-payment-button"]')

    // Fill form
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="invoiceId"]', 'invoice-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')

    // Add payment method data
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.fill('[data-testid="expiry"]', '12/34')
    await page.fill('[data-testid="cvv"]', '123')

    // Submit
    await page.click('[data-testid="submit-button"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/payment/i)
  })

  test('SMOKE-033: Should view payment details', async ({ page }) => {
    // Create payment first
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '50.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View details
    await page.click('[data-testid="payment-row-0"] [data-testid="view-button"]')
    await expect(page.locator('[data-testid="payment-details"]')).toBeVisible()
  })

  test('SMOKE-034: Should refund a payment', async ({ page }) => {
    // Create payment first
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Refund payment
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '100.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.fill('[data-testid="refund-notes"]', 'Customer requested refund')
    await page.click('[data-testid="confirm-refund"]')

    // Verify refund
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/refund/i)
    const status = await page.textContent('[data-testid="payment-row-0"] [data-testid="status"]')
    expect(status).toContain('refunded')
  })

  test('SMOKE-035: Should view payment history', async ({ page }) => {
    await page.goto('/payments')

    // Click on first payment
    await page.click('[data-testid="payment-row-0"] [data-testid="view-button"]')

    // View history tab
    await page.click('[data-testid="history-tab"]')

    // Verify history is displayed
    await expect(page.locator('[data-testid="payment-history"]')).toBeVisible()
  })

  test('SMOKE-036: Should filter payments by status', async ({ page }) => {
    await page.goto('/payments')

    // Filter by status
    await page.selectOption('[data-testid="status-filter"]', 'completed')
    await page.waitForTimeout(500)

    // Verify filtered results
    const rows = page.locator('[data-testid="payment-row"]')
    await expect(rows.first()).toBeVisible()
  })

  test('SMOKE-037: Should filter payments by method', async ({ page }) => {
    await page.goto('/payments')

    // Filter by payment method
    await page.selectOption('[data-testid="method-filter"]', 'credit_card')
    await page.waitForTimeout(500)

    // Verify filtered results
    const rows = page.locator('[data-testid="payment-row"]')
    await expect(rows.first()).toBeVisible()
  })

  test('SMOKE-038: Should handle failed payment', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4000000000000002') // Declined card
    await page.click('[data-testid="submit-button"]')

    // Verify error
    await expect(page.locator('[data-testid="error-message"]')).toContainText(/declined/i)
  })

  test('SMOKE-039: Should display transaction ID', async ({ page }) => {
    await page.goto('/payments')

    // Verify transaction IDs are displayed
    const transactionId = page.locator('[data-testid="payment-row-0"] [data-testid="transaction-id"]')
    await expect(transactionId).toBeVisible()
    await expect(transactionId).toContainText(/txn_/)
  })

  test('SMOKE-040: Should handle payment method validation', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')

    // Try to submit without payment method
    await page.click('[data-testid="submit-button"]')

    // Verify validation error
    await expect(page.locator('[data-testid="error-method"]')).toContainText(/required/i)
  })
})
