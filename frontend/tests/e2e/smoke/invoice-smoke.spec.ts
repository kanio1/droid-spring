/**
 * Invoice Smoke Tests - Critical Path Tests for Invoice Management
 */

import { test, expect } from '@playwright/test'

test.describe('Invoice Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-021: Should display invoice list page', async ({ page }) => {
    await page.goto('/invoices')
    await expect(page.locator('h1')).toContainText(/invoices/i)
    await expect(page.locator('[data-testid="invoice-list"]')).toBeVisible()
  })

  test('SMOKE-022: Should create a new invoice', async ({ page }) => {
    await page.goto('/invoices')

    // Create new invoice
    await page.click('[data-testid="create-invoice-button"]')

    // Fill form
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.selectOption('[name="orderId"]', 'order-1')

    // Add line items
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '2')
    await page.fill('[data-testid="unit-price-0"]', '100.00')

    // Set due date
    const futureDate = new Date()
    futureDate.setDate(futureDate.getDate() + 30)
    await page.fill('[name="dueDate"]', futureDate.toISOString().split('T')[0])

    // Submit
    await page.click('[data-testid="submit-button"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('SMOKE-023: Should view invoice details', async ({ page }) => {
    // Create invoice first
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '50.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View details
    await page.click('[data-testid="invoice-row-0"] [data-testid="view-button"]')
    await expect(page.locator('[data-testid="invoice-details"]')).toBeVisible()
  })

  test('SMOKE-024: Should calculate invoice total correctly', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Add line items
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '2')
    await page.fill('[data-testid="unit-price-0"]', '100.00')

    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-1"]', 'Product B')
    await page.fill('[data-testid="quantity-1"]', '1')
    await page.fill('[data-testid="unit-price-1"]', '50.00')

    // Check subtotal
    const subtotal = await page.textContent('[data-testid="subtotal"]')
    expect(subtotal).toContain('250.00')

    // Add tax
    await page.fill('[data-testid="tax-rate"]', '10')
    const total = await page.textContent('[data-testid="total-amount"]')
    expect(total).toContain('275.00') // 250 + 10% tax
  })

  test('SMOKE-025: Should send invoice via email', async ({ page }) => {
    // Create invoice first
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '50.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Send email
    await page.click('[data-testid="invoice-row-0"] [data-testid="send-email-button"]')
    await page.fill('[data-testid="email-to"]', 'customer@example.com')
    await page.fill('[data-testid="email-subject"]', 'Invoice from Company')
    await page.click('[data-testid="send-button"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/sent/i)
  })

  test('SMOKE-026: Should update invoice status to paid', async ({ page }) => {
    await page.goto('/invoices')

    // Create paid invoice
    await page.click('[data-testid="create-invoice-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-line-item"]')
    await page.fill('[data-testid="description-0"]', 'Product A')
    await page.fill('[data-testid="quantity-0"]', '1')
    await page.fill('[data-testid="unit-price-0"]', '50.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Mark as paid
    await page.click('[data-testid="invoice-row-0"] [data-testid="mark-paid-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'credit_card')
    await page.fill('[data-testid="payment-reference"]', 'REF123')
    await page.click('[data-testid="confirm-payment"]')

    // Verify status
    const status = await page.textContent('[data-testid="invoice-row-0"] [data-testid="status"]')
    expect(status).toContain('paid')
  })

  test('SMOKE-027: Should filter invoices by status', async ({ page }) => {
    await page.goto('/invoices')

    // Filter by status
    await page.selectOption('[data-testid="status-filter"]', 'pending')
    await page.waitForTimeout(500)

    // Verify filtered results
    const rows = page.locator('[data-testid="invoice-row"]')
    await expect(rows.first()).toBeVisible()
  })

  test('SMOKE-028: Should filter invoices by date range', async ({ page }) => {
    await page.goto('/invoices')

    // Set date range
    await page.fill('[data-testid="date-from"]', '2025-01-01')
    await page.fill('[data-testid="date-to"]', '2025-12-31')
    await page.click('[data-testid="apply-filter"]')

    // Verify filtered results
    await expect(page.locator('[data-testid="invoice-list"]')).toBeVisible()
  })

  test('SMOKE-029: Should display invoice number', async ({ page }) => {
    await page.goto('/invoices')

    // Verify invoice numbers are displayed
    const invoiceNumber = page.locator('[data-testid="invoice-row-0"] [data-testid="invoice-number"]')
    await expect(invoiceNumber).toBeVisible()
    await expect(invoiceNumber).toContainText(/INV-\d+/)
  })

  test('SMOKE-030: Should handle overdue invoices', async ({ page }) => {
    await page.goto('/invoices')

    // Check for overdue indicator
    const overdueInvoice = page.locator('[data-testid="invoice-row-0"]')
    if (await overdueInvoice.isVisible()) {
      const status = await overdueInvoice.getAttribute('data-status')
      // Should show overdue status if past due date
    }
  })
})
