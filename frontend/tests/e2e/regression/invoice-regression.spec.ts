/**
 * Invoice Regression Tests - Comprehensive scenarios
 */

import { test, expect } from '@playwright/test'
import { TestDataGenerator } from '@tests/framework/data-factories'

test.describe('Invoice Regression Tests - Comprehensive', () => {
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
  test('REGRESSION-036: Should handle invoice with many line items', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="orderId"]', 'order-1')

    // Add many line items
    const itemCount = 30
    for (let i = 0; i < itemCount; i++) {
      await page.click('[data-testid="add-line-item"]')
      await page.fill(`[data-testid="description-${i}"]`, `Item ${i}`)
      await page.fill(`[data-testid="quantity-${i}"]`, '1')
      await page.fill(`[data-testid="unit-price-${i}"]`, '10.00')
    }

    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-037: Should handle zero tax rate', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '2')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.fill('[data-testid="tax-rate"]', '0')

    const total = await page.textContent('[data-testid="total-amount"]')
    const subtotal = await page.textContent('[data-testid="subtotal"]')

    expect(total).toBe(subtotal) // No tax, so total should equal subtotal
  })

  test('REGRESSION-038: Should handle very high tax rate', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.fill('[data-testid="tax-rate"]', '100')

    const total = await page.textContent('[data-testid="total-amount"]')
    expect(total).toContain('200.00') // 100 + 100% tax
  })

  test('REGRESSION-039: Should handle past due date', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')

    // Set past due date
    const pastDate = new Date()
    pastDate.setDate(pastDate.getDate() - 30)
    await page.fill('[name="dueDate"]', pastDate.toISOString().split('T')[0])

    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // Should show as overdue
    const status = await page.textContent('[data-testid="invoice-row-0"] [data-testid="status"]')
    expect(status).toContain('overdue')
  })

  test('REGRESSION-040: Should handle very long line item description', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')

    const longDescription = 'This is a very long line item description that exceeds normal length limits and should be handled gracefully by the system'.repeat(5)
    await page.fill('[data-testid="description-0"]', longDescription)
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')

    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  // Negative Tests
  test('REGRESSION-041: Should reject invoice without line items', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Don't add any line items
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-items"]')).toContainText(/at least one/i)
  })

  test('REGRESSION-042: Should reject negative quantity', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '-5')
    await page.fill('[data-testid="unit-price-0"]', '100.00')

    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="error-quantity"]')).toContainText(/positive/i)
  })

  test('REGRESSION-043: Should reject paid invoice update', async ({ page }) => {
    // Create and mark invoice as paid
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Mark as paid
    await page.click('[data-testid="invoice-row-0"] [data-testid="mark-paid-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'credit_card')
    await page.click('[data-testid="confirm-payment"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to edit paid invoice
    await page.click('[data-testid="invoice-row-0"] [data-testid="edit-button"]')

    // Should show error or be disabled
    await expect(page.locator('[data-testid="error-message"], [data-testid="disabled-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-044: Should handle duplicate invoice number', async ({ page }) => {
    await page.goto('/invoices')

    // Create first invoice
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to create another invoice with same number (if manual entry is allowed)
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product B')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '50.00')

    // If invoice number is manually editable, try to use same number
    const invoiceNumberInput = page.locator('[name="invoiceNumber"]')
    if (await invoiceNumberInput.isVisible()) {
      const firstInvoiceNumber = await page.textContent('[data-testid="invoice-row-0"] [data-testid="invoice-number"]')
      await invoiceNumberInput.fill(firstInvoiceNumber || '')
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-invoiceNumber"]'))
        .toContainText(/duplicate|already exists/i)
    }
  })

  // Boundary Conditions
  test('REGRESSION-045: Should handle exact due date today', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')

    // Set due date to today
    const today = new Date()
    await page.fill('[name="dueDate"]', today.toISOString().split('T')[0])

    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // Should not be overdue
    const status = await page.textContent('[data-testid="invoice-row-0"] [data-testid="status"]')
    expect(status).not.toContain('overdue')
  })

  test('REGRESSION-046: Should handle date range filter with same start and end', async ({ page }) => {
    await page.goto('/invoices')

    // Set same date for from and to
    const date = new Date().toISOString().split('T')[0]
    await page.fill('[data-testid="date-from"]', date)
    await page.fill('[data-testid="date-to"]', date)
    await page.click('[data-testid="apply-filter"]')

    // Should work or show appropriate message
    await expect(page.locator('[data-testid="invoice-list"], [data-testid="no-results"]')).toBeVisible()
  })

  // Workflow Tests
  test('REGRESSION-047: Should complete full invoice lifecycle', async ({ page }) => {
    // 1. Create invoice
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '2')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // 2. Send invoice
    await page.click('[data-testid="invoice-row-0"] [data-testid="send-email-button"]')
    await page.fill('[data-testid="email-to"]', 'customer@example.com')
    await page.fill('[data-testid="email-subject"]', 'Invoice')
    await page.click('[data-testid="send-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/sent/i)

    // 3. Mark as paid
    await page.click('[data-testid="invoice-row-0"] [data-testid="mark-paid-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'credit_card')
    await page.fill('[data-testid="payment-reference"]', 'REF123')
    await page.click('[data-testid="confirm-payment"]')

    // Verify final status
    const status = await page.textContent('[data-testid="invoice-row-0"] [data-testid="status"]')
    expect(status).toContain('paid')
  })

  test('REGRESSION-048: Should calculate totals with multiple tax rates', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Add items with different tax rates
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Taxable Product')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.check('[data-testid="taxable-0"]')

    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-1"]', 'Non-taxable Product')
    await page.fill('[data-testid="quantity-1"]', '1')
    await page.fill('[data-testid="unit-price-1"]', '50.00')
    await page.uncheck('[data-testid="taxable-1"]')

    await page.fill('[data-testid="tax-rate"]', '10')

    // Check calculations
    const subtotal = await page.textContent('[data-testid="subtotal"]')
    const tax = await page.textContent('[data-testid="tax-amount"]')
    const total = await page.textContent('[data-testid="total-amount"]')

    expect(subtotal).toContain('150.00') // 100 + 50
    expect(tax).toContain('10.00') // 10% of 100 (only taxable item)
    expect(total).toContain('160.00') // 150 + 10
  })

  // Data Consistency
  test('REGRESSION-049: Should preserve invoice data during editing', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '2')
    await page.fill('[data-testid="unit-price-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Edit invoice
    await page.click('[data-testid="invoice-row-0"] [data-testid="edit-button"]')

    // Verify original data is preserved
    await expect(page.locator('[data-testid="description-0"]')).toHaveValue('Product A')
    await expect(page.locator('[data-testid="quantity-0"]')).toHaveValue('2')
  })

  // Performance
  test('REGRESSION-050: Should handle large invoice list', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/invoices')

    // Create 30 invoices
    for (let i = 0; i < 30; i++) {
      await page.click('[data-testid="create-invoice-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.click('[data-testid="add-line-item"]')
      await page.fill('[data-testid="description-0"]', `Product ${i}`)
      await page.fill('[data-testid="quantity-0"]', '1')
      await page.fill('[data-testid="unit-price-0"]', '100.00')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    const endTime = Date.now()
    const duration = endTime - startTime

    // Should handle 30 invoices in reasonable time
    expect(duration).toBeLessThan(180000) // 3 minutes
  })
})
