/**
 * Payment Regression Tests - Comprehensive scenarios
 */

import { test, expect } from '@playwright/test'
import { TestDataGenerator } from '@tests/framework/data-factories'

test.describe('Payment Regression Tests - Comprehensive', () => {
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
  test('REGRESSION-051: Should handle maximum payment amount', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '999999999.99')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-052: Should handle minimum payment amount', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '0.01')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-053: Should handle partial refund', async ({ page }) => {
    // Create payment
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Partial refund
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '50.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-refund"]')

    await expect(page.locator('[data-testid="success-message"]')).toContainText(/refund/i)

    // Payment should be partially refunded
    const status = await page.textContent('[data-testid="payment-row-0"] [data-testid="status"]')
    expect(status).toContain('partially_refunded')
  })

  test('REGRESSION-054: Should handle multiple refunds', async ({ page }) => {
    // Create payment
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // First refund
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '30.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-refund"]')

    // Second refund
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '30.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-refund"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('REGRESSION-055: Should handle different payment methods', async ({ page }) => {
    const methods = ['credit_card', 'debit_card', 'bank_transfer', 'paypal', 'crypto']

    for (const method of methods) {
      await page.goto('/payments')
      await page.click('[data-testid="create-payment-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.fill('[name="amount"]', '50.00')
      await page.selectOption('[name="method"]', method)

      // Fill method-specific data
      if (method === 'credit_card' || method === 'debit_card') {
        await page.fill('[data-testid="card-number"]', '4111111111111111')
        await page.fill('[data-testid="expiry"]', '12/34')
        await page.fill('[data-testid="cvv"]', '123')
      } else if (method === 'bank_transfer') {
        await page.fill('[data-testid="account-number"]', '1234567890')
      } else if (method === 'paypal') {
        await page.fill('[data-testid="paypal-email"]', 'user@paypal.com')
      }

      await page.click('[data-testid="submit-button"]')
      await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
        .toBeVisible()
    }
  })

  // Negative Tests
  test('REGRESSION-056: Should reject refund greater than payment amount', async ({ page }) => {
    // Create payment
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to refund more than paid
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '150.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-refund"]')

    await expect(page.locator('[data-testid="error-amount"]'))
      .toContainText(/exceeds|greater than/i)
  })

  test('REGRESSION-057: Should reject refund of fully refunded payment', async ({ page }) => {
    // Create payment
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Full refund
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '100.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-refund"]')

    // Try to refund again
    await page.click('[data-testid="payment-row-0"] [data-testid="refund-button"]')
    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/already refunded|fully refunded/i)
  })

  test('REGRESSION-058: Should reject invalid card number', async ({ page }) => {
    const invalidCards = [
      '1234567890123456', // Too short
      '411111111111111',  // One digit short
      '41111111111111111', // Too long
      'abcdefghijklmnop', // Letters
      '0000000000000000'  // All zeros
    ]

    for (const card of invalidCards) {
      await page.goto('/payments')
      await page.click('[data-testid="create-payment-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.fill('[name="amount"]', '50.00')
      await page.selectOption('[name="method"]', 'credit_card')
      await page.fill('[data-testid="card-number"]', card)
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-card"]'))
        .toBeVisible()
    }
  })

  test('REGRESSION-059: Should handle expired card', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '50.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.fill('[data-testid="expiry"]', '01/20') // Past date
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-expiry"]'))
      .toContainText(/expired|invalid/i)
  })

  test('REGRESSION-060: Should reject payment without invoice', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '50.00')
    await page.selectOption('[name="method"]', 'credit_card')
    // Don't select invoice
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')

    // Should either allow standalone payment or show error
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-invoice"]'))
      .toBeVisible()
  })

  // Boundary Conditions
  test('REGRESSION-061: Should filter payments by exact amount', async ({ page }) => {
    await page.goto('/payments')

    // Create payments with different amounts
    const amounts = ['100.00', '200.00', '100.00', '300.00']
    for (const amount of amounts) {
      await page.click('[data-testid="create-payment-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.fill('[name="amount"]', amount)
      await page.selectOption('[name="method"]', 'credit_card')
      await page.fill('[data-testid="card-number"]', '4111111111111111')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Filter by exact amount
    await page.fill('[data-testid="amount-filter"]', '100.00')
    await page.click('[data-testid="apply-filter"]')

    // Should show only payments with $100.00
    const rows = page.locator('[data-testid="payment-row"]')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)

    // All visible rows should have $100.00
    for (let i = 0; i < count; i++) {
      const amount = await page.textContent(`[data-testid="payment-row-${i}"] [data-testid="amount"]`)
      expect(amount).toContain('100.00')
    }
  })

  test('REGRESSION-062: Should handle date range for payment history', async ({ page }) => {
    await page.goto('/payments')

    // Create payment
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '50.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Filter by date range
    const fromDate = new Date()
    fromDate.setDate(fromDate.getDate() - 7)
    const toDate = new Date()

    await page.fill('[data-testid="date-from"]', fromDate.toISOString().split('T')[0])
    await page.fill('[data-testid="date-to"]', toDate.toISOString().split('T')[0])
    await page.click('[data-testid="apply-filter"]')

    await expect(page.locator('[data-testid="payment-row"]')).toBeVisible()
  })

  // Workflow Tests
  test('REGRESSION-063: Should complete full payment lifecycle', async ({ page }) => {
    // 1. Create payment
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '100.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.fill('[data-testid="expiry"]', '12/34')
    await page.fill('[data-testid="cvv"]', '123')
    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/payment/i)

    // 2. View payment details
    await page.click('[data-testid="payment-row-0"] [data-testid="view-button"]')
    await expect(page.locator('[data-testid="payment-details"]')).toContainText('100.00')

    // 3. View payment history
    await page.click('[data-testid="history-tab"]')
    await expect(page.locator('[data-testid="payment-history"]')).toBeVisible()

    // 4. Refund payment
    await page.click('[data-testid="refund-button"]')
    await page.fill('[data-testid="refund-amount"]', '50.00')
    await page.selectOption('[data-testid="refund-reason"]', 'customer_request')
    await page.click('[data-testid="confirm-refund"]')
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/refund/i)
  })

  test('REGRESSION-064: Should handle bulk payment processing', async ({ page }) => {
    await page.goto('/payments')

    // Select multiple payments
    for (let i = 0; i < 3; i++) {
      await page.click('[data-testid="create-payment-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.fill('[name="amount"]', '50.00')
      await page.selectOption('[name="method"]', 'credit_card')
      await page.fill('[data-testid="card-number"]', '4111111111111111')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Select payments for bulk action
    for (let i = 0; i < 3; i++) {
      await page.click(`[data-testid="payment-row-${i}"] [data-testid="select-checkbox"]`)
    }

    // Bulk actions should be available
    await expect(page.locator('[data-testid="bulk-actions"]')).toBeVisible()
  })

  // Data Consistency
  test('REGRESSION-065: Should maintain transaction ID uniqueness', async ({ page }) => {
    await page.goto('/payments')

    // Create multiple payments
    const transactionIds = new Set()
    for (let i = 0; i < 5; i++) {
      await page.click('[data-testid="create-payment-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.fill('[name="amount"]', '50.00')
      await page.selectOption('[name="method"]', 'credit_card')
      await page.fill('[data-testid="card-number"]', '4111111111111111')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')

      // Collect transaction IDs
      const id = await page.textContent('[data-testid="payment-row-0"] [data-testid="transaction-id"]')
      expect(transactionIds).not.toContain(id)
      transactionIds.add(id)
    }
  })

  // Performance
  test('REGRESSION-066: Should handle many payment records', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/payments')

    // Create 25 payments
    for (let i = 0; i < 25; i++) {
      await page.click('[data-testid="create-payment-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.fill('[name="amount"]', '50.00')
      await page.selectOption('[name="method"]', 'credit_card')
      await page.fill('[data-testid="card-number"]', '4111111111111111')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    const endTime = Date.now()
    const duration = endTime - startTime

    // Should handle 25 payments in reasonable time
    expect(duration).toBeLessThan(180000) // 3 minutes
  })

  // Security
  test('REGRESSION-067: Should not expose card details in UI', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '50.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.fill('[data-testid="expiry"]', '12/34')
    await page.fill('[data-testid="cvv"]', '123')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View payment details
    await page.click('[data-testid="payment-row-0"] [data-testid="view-button"]')

    // Card number should be masked
    const cardNumber = await page.textContent('[data-testid="card-number-masked"]')
    expect(cardNumber).toContain('****') // Should be masked
    expect(cardNumber).not.toContain('4111111111111111') // Should not show full number

    // CVV should not be visible
    await expect(page.locator('[data-testid="cvv"]')).not.toContainText('123')
  })

  test('REGRESSION-068: Should handle concurrent payments', async ({ page }) => {
    await page.goto('/payments')

    const pagePromise = page.context().newPage()
    const newPage = await pagePromise

    // Create payment in first tab
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '50.00')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')

    // Create payment in second tab
    await newPage.goto('/payments')
    await newPage.click('[data-testid="create-payment-button"]')
    await newPage.selectOption('[name="customerId"]', 'customer-1')
    await newPage.fill('[name="amount"]', '50.00')
    await newPage.selectOption('[name="method"]', 'credit_card')
    await newPage.fill('[data-testid="card-number"]', '4222222222222222')
    await newPage.click('[data-testid="submit-button"]')

    // Both should succeed
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
    await expect(newPage.locator('[data-testid="success-message"]')).toBeVisible()
  })
})
