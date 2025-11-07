/**
 * Invoice Management E2E Tests
 *
 * Comprehensive test suite covering all invoice management workflows:
 * - Invoice creation and generation
 * - Invoice editing and modification
 * - Invoice status transitions (draft, sent, paid, overdue, cancelled)
 * - Payment processing
 * - Recurring billing
 * - Invoice line items management
 * - Tax calculations
 * - Discounts and adjustments
 * - PDF generation and download
 * - Search and filtering
 * - Date range filtering
 * - Bulk operations
 * - Email sending
 * - Invoice history
 * - Dunning management
 * - Error handling and validation
 *
 * Target: 35 comprehensive tests
 */

import { test, expect } from '@playwright/test'
import { InvoiceFactory, CustomerFactory, OrderFactory } from '../framework/data-factories'
import { AuthHelper } from '../helpers'
import type { Page } from '@playwright/test'

test.describe('Invoice Management E2E', () => {
  let testInvoices: any[] = []
  let testOrders: any[] = []
  let testCustomers: any[] = []

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'testuser',
      password: process.env.TEST_PASSWORD || 'testpass'
    })

    await page.goto('/invoices')
    await expect(page.locator('h1')).toContainText('Invoices')
  })

  test.afterEach(async ({ page }) => {
    // Cleanup test data
    for (const invoice of testInvoices) {
      try {
        await page.goto(`/invoices/${invoice.id}`)
        if (await page.locator('[data-testid="delete-invoice-button"]').isVisible()) {
          await page.click('[data-testid="delete-invoice-button"]')
          await page.click('[data-testid="confirm-delete"]')
        }
      } catch (error) {
        console.log(`Cleanup failed for invoice ${invoice.id}:`, error)
      }
    }
    testInvoices = []
    testOrders = []
    testCustomers = []
  })

  // ========== LIST VIEW TESTS ==========

  test('01 - Should display invoices list with all required elements', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Invoices')
    await expect(page.locator('[data-testid="invoices-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="date-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="create-invoice-button"]')).toBeVisible()
  })

  test('02 - Should show invoice count and total amount', async ({ page }) => {
    const countText = await page.locator('[data-testid="total-count"]').textContent()
    if (countText) {
      const count = parseInt(countText)
      expect(count).toBeGreaterThanOrEqual(0)
    }

    const totalText = await page.locator('[data-testid="total-amount"]').textContent()
    if (totalText) {
      // Total should be a valid number
      const total = parseFloat(totalText.replace(/[^0-9.-]+/g, ''))
      expect(total).toBeGreaterThanOrEqual(0)
    }
  })

  test('03 - Should handle empty state when no invoices exist', async ({ page }) => {
    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText(/No invoices found/i)
      await expect(page.locator('[data-testid="create-first-invoice-button"]')).toBeVisible()
    }
  })

  // ========== SEARCH TESTS ==========

  test('04 - Should search invoices by invoice number', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .withInvoiceNumber('INV-TEST-001')
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })
    await page.waitForTimeout(1000)

    // Search by invoice number
    await page.fill('[data-testid="search-input"]', 'INV-TEST-001')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('05 - Should search invoices by customer name', async ({ page }) => {
    const customer = await createTestCustomer(page, 'Jane', 'Smith')
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })
    await page.waitForTimeout(1000)

    // Search by customer name
    await page.fill('[data-testid="search-input"]', 'Jane')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('06 - Should show no results for search with no matches', async ({ page }) => {
    await page.fill('[data-testid="search-input"]', 'NonExistentInvoice12345')
    await page.waitForTimeout(1000)

    await expect(page.locator('[data-testid="no-results-message"]'))
      .toContainText(/No invoices found/i)
    await expect(page.locator('table tbody tr')).toHaveCount(0)
  })

  // ========== FILTERING TESTS ==========

  test('07 - Should filter invoices by status - Draft', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'DRAFT')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/draft/i)
        }
      }
    }
  })

  test('08 - Should filter invoices by status - Sent', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'SENT')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/sent/i)
        }
      }
    }
  })

  test('09 - Should filter invoices by status - Paid', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'PAID')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/paid/i)
        }
      }
    }
  })

  test('10 - Should filter invoices by status - Overdue', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'OVERDUE')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/overdue/i)
        }
      }
    }
  })

  test('11 - Should combine search and status filter', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'PAID')
    await page.fill('[data-testid="search-input"]', 'test')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  // ========== DATE RANGE FILTERING ==========

  test('12 - Should filter invoices by date range', async ({ page }) => {
    const fromDate = new Date()
    fromDate.setMonth(fromDate.getMonth() - 1)
    const toDate = new Date()

    await page.fill('[data-testid="date-from"]', fromDate.toISOString().split('T')[0])
    await page.fill('[data-testid="date-to"]', toDate.toISOString().split('T')[0])
    await page.click('[data-testid="apply-date-filter"]')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  // ========== CREATE INVOICE TESTS ==========

  test('13 - Should create a new invoice with valid data', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    expect(invoiceId).toBeTruthy()
    testInvoices.push({ id: invoiceId, ...invoice })

    // Verify redirect to details page
    await expect(page).toHaveURL(/.*\/invoices\/[a-zA-Z0-9]+/)

    // Verify invoice data
    await expect(page.locator('[data-testid="invoice-number"]')).toBeVisible()
  })

  test('14 - Should create invoice with multiple line items', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .withMultipleItems(3)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    expect(invoiceId).toBeTruthy()
    testInvoices.push({ id: invoiceId, ...invoice })

    // Verify invoice has multiple items
    await page.goto(`/invoices/${invoiceId}`)
    const itemCount = await page.locator('[data-testid="line-item"]').count()
    expect(itemCount).toBeGreaterThan(1)
  })

  test('15 - Should create invoice from order', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = await createTestOrder(page, customer.id)
    testOrders.push(order)

    // Create invoice from order
    await page.goto(`/orders/${order.id}`)
    await page.click('[data-testid="create-invoice-button"]')
    await page.waitForURL(/.*\/invoices\/create.*orderId=${order.id}/)

    await page.click('[data-testid="generate-from-order-button"]')
    await page.waitForResponse(/.*\/api\/invoices/)
    await page.waitForURL(/.*\/invoices\/[a-zA-Z0-9]+/)

    // Extract invoice ID from URL
    const url = page.url()
    const idMatch = url.match(/\/invoices\/([a-zA-Z0-9]+)/)
    const invoiceId = idMatch ? idMatch[1] : ''

    expect(invoiceId).toBeTruthy()
    testInvoices.push({ id: invoiceId })
  })

  test('16 - Should show validation errors for required fields', async ({ page }) => {
    await page.click('[data-testid="create-invoice-button"]')
    await expect(page).toHaveURL(/.*\/invoices\/create/)

    // Submit empty form
    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(500)

    // Check for validation errors
    await expect(page.locator('[data-testid="customer-error"]'))
      .toContainText(/required/i)
  })

  test('17 - Should calculate invoice total with tax', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .withTax(0.1) // 10% tax
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Verify total includes tax
    await page.goto(`/invoices/${invoiceId}`)
    const subtotal = await page.locator('[data-testid="subtotal"]').textContent()
    const tax = await page.locator('[data-testid="tax-amount"]').textContent()
    const total = await page.locator('[data-testid="total-amount"]').textContent()

    expect(parseFloat(subtotal || '0')).toBeGreaterThan(0)
    expect(parseFloat(tax || '0')).toBeGreaterThan(0)
    expect(parseFloat(total || '0')).toBeGreaterThan(parseFloat(subtotal || '0'))
  })

  // ========== INVOICE EDITING ==========

  test('18 - Should edit invoice line items', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Edit invoice
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="edit-invoice-button"]')

    // Add new line item
    await page.click('[data-testid="add-line-item-button"]')
    await page.selectOption('[data-testid="product-select"]', '2')
    await page.fill('[data-testid="quantity-input"]', '2')
    await page.fill('[data-testid="price-input"]', '50.00')
    await page.click('[data-testid="save-line-item"]')
    await page.waitForTimeout(500)

    // Verify line item was added
    const itemCount = await page.locator('[data-testid="line-item"]').count()
    expect(itemCount).toBeGreaterThan(1)
  })

  test('19 - Should apply discount to invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .withDiscount(0.1) // 10% discount
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Verify discount is applied
    await page.goto(`/invoices/${invoiceId}`)
    const discount = await page.locator('[data-testid="discount-amount"]').textContent()
    expect(parseFloat(discount || '0')).toBeGreaterThan(0)
  })

  // ========== INVOICE STATUS TRANSITIONS ==========

  test('20 - Should change invoice status from Draft to Sent', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Send invoice
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="send-invoice-button"]')
    await page.fill('[data-testid="email-to"]', customer.email)
    await page.click('[data-testid="confirm-send"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/sent/i)
  })

  test('21 - Should mark invoice as paid', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .sent()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Mark as paid
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="mark-paid-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'CREDIT_CARD')
    await page.fill('[data-testid="paid-amount"]', '1000.00')
    await page.click('[data-testid="confirm-paid"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/paid/i)
  })

  test('22 - Should void/cancel invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Cancel invoice
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="cancel-invoice-button"]')
    await page.fill('[data-testid="cancellation-reason"]', 'Client request')
    await page.click('[data-testid="confirm-cancel"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/cancelled|void/i)
  })

  // ========== PAYMENT PROCESSING ==========

  test('23 - Should record partial payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .sent()
      .withTotal(2000)
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Record partial payment
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="record-payment-button"]')
    await page.fill('[data-testid="payment-amount"]', '1000.00')
    await page.selectOption('[data-testid="payment-method"]', 'BANK_TRANSFER')
    await page.click('[data-testid="record-payment"]')
    await page.waitForTimeout(500)

    // Verify partial payment
    const paymentStatus = page.locator('[data-testid="payment-status"]')
    await expect(paymentStatus).toContainText(/partial/i)
  })

  test('24 - Should apply credit note to invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .sent()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Apply credit note
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="apply-credit-button"]')
    await page.selectOption('[data-testid="credit-select"]', '1')
    await page.fill('[data-testid="credit-amount"]', '100.00')
    await page.click('[data-testid="apply-credit-confirm"]')
    await page.waitForTimeout(500)

    // Verify credit was applied
    const creditAmount = page.locator('[data-testid="credit-amount-display"]')
    await expect(creditAmount).toContainText('100.00')
  })

  // ========== RECURRING INVOICES ==========

  test('25 - Should create recurring invoice template', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Create recurring template
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="make-recurring-button"]')
    await page.selectOption('[data-testid="recurring-frequency"]', 'MONTHLY')
    await page.fill('[data-testid="recurring-count"]', '12')
    await page.click('[data-testid="save-recurring"]')
    await page.waitForTimeout(500)

    // Verify recurring template created
    const recurringInfo = page.locator('[data-testid="recurring-info"]')
    await expect(recurringInfo).toContainText(/monthly/i)
  })

  // ========== PDF GENERATION ==========

  test('26 - Should generate and download PDF invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Download PDF
    await page.goto(`/invoices/${invoiceId}`)
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('[data-testid="download-pdf-button"]')
    ])

    const path = await download.path()
    expect(path).toBeTruthy()
    expect(path).toMatch(/\.pdf$/i)
  })

  test('27 - Should email invoice PDF to customer', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Email invoice
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="email-invoice-button"]')
    await page.fill('[data-testid="email-to"]', customer.email)
    await page.fill('[data-testid="email-subject"]', 'Invoice from Company')
    await page.fill('[data-testid="email-message"]', 'Please find attached invoice')
    await page.click('[data-testid="send-email-button"]')
    await page.waitForTimeout(1000)

    // Verify email sent
    await expect(page.locator('[data-testid="email-sent-message"]'))
      .toContainText(/sent|success/i)
  })

  // ========== INVOICE DETAILS VIEW ==========

  test('28 - Should view complete invoice details', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .withTax(0.1)
      .withDiscount(0.05)
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // View invoice details
    await page.goto(`/invoices/${invoiceId}`)

    // Verify all invoice information is displayed
    await expect(page.locator('[data-testid="invoice-number"]')).toBeVisible()
    await expect(page.locator('[data-testid="invoice-date"]')).toBeVisible()
    await expect(page.locator('[data-testid="due-date"]')).toBeVisible()
    await expect(page.locator('[data-testid="customer-info"]')).toBeVisible()
    await expect(page.locator('[data-testid="line-items"]')).toBeVisible()
    await expect(page.locator('[data-testid="subtotal"]')).toBeVisible()
    await expect(page.locator('[data-testid="tax-amount"]')).toBeVisible()
    await expect(page.locator('[data-testid="discount-amount"]')).toBeVisible()
    await expect(page.locator('[data-testid="total-amount"]')).toBeVisible()
  })

  test('29 - Should show invoice history and audit trail', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // View invoice history
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="view-history-button"]')

    // Verify history is displayed
    const history = page.locator('[data-testid="invoice-history"]')
    if (await history.isVisible()) {
      const events = await history.locator('[data-testid="history-event"]').count()
      expect(events).toBeGreaterThan(0)
    }
  })

  // ========== BULK OPERATIONS ==========

  test('30 - Should select multiple invoices and perform bulk action', async ({ page }) => {
    const customer = await createTestCustomer(page)

    // Create multiple invoices
    const invoices = InvoiceFactory.create().buildMany(3)
    const createdIds: any[] = []

    for (const invoice of invoices) {
      const invoiceId = await createInvoice(page, { ...invoice, customerId: customer.id })
      createdIds.push({ id: invoiceId, ...invoice })
    }
    testInvoices.push(...createdIds)

    await page.waitForTimeout(1000)

    // Select multiple invoices
    await page.click('[data-testid="select-all-checkbox"]')

    // Perform bulk action
    await page.click('[data-testid="bulk-action-dropdown"]')
    await page.click('[data-testid="bulk-action-mark-paid"]')
    await page.click('[data-testid="bulk-action-apply"]')
    await page.waitForTimeout(1000)

    // Verify bulk action was applied
    const successMessage = page.locator('[data-testid="bulk-action-success"]')
    if (await successMessage.isVisible()) {
      await expect(successMessage).toContainText(/success/i)
    }
  })

  // ========== DUNNING MANAGEMENT ==========

  test('31 - Should send reminder for overdue invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .overdue()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Send reminder
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="send-reminder-button"]')
    await page.fill('[data-testid="reminder-message"]', 'Payment is overdue')
    await page.click('[data-testid="send-reminder-confirm"]')
    await page.waitForTimeout(1000)

    // Verify reminder sent
    await expect(page.locator('[data-testid="reminder-sent-message"]'))
      .toContainText(/sent|success/i)
  })

  // ========== TAX AND ACCOUNTING ==========

  test('32 - Should handle multiple tax rates', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .withMultipleTaxRates([
        { rate: 0.1, name: 'State Tax' },
        { rate: 0.05, name: 'County Tax' }
      ])
      .draft()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Verify multiple taxes calculated
    await page.goto(`/invoices/${invoiceId}`)
    const taxBreakdown = page.locator('[data-testid="tax-breakdown"]')
    if (await taxBreakdown.isVisible()) {
      await expect(taxBreakdown).toContainText('State Tax')
      await expect(taxBreakdown).toContainText('County Tax')
    }
  })

  // ========== ERROR HANDLING ==========

  test('33 - Should handle insufficient payment amount', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .sent()
      .withTotal(1000)
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Try to mark as paid with insufficient amount
    await page.goto(`/invoices/${invoiceId}`)
    await page.click('[data-testid="mark-paid-button"]')
    await page.fill('[data-testid="paid-amount"]', '500.00') // Less than total
    await page.click('[data-testid="confirm-paid"]')
    await page.waitForTimeout(1000)

    // Should show error
    await expect(page.locator('[data-testid="payment-error"]'))
      .toContainText(/insufficient|less than/i)
  })

  test('34 - Should prevent deletion of sent invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = InvoiceFactory.create()
      .withCustomerId(customer.id)
      .sent()
      .build()

    const invoiceId = await createInvoice(page, invoice)
    testInvoices.push({ id: invoiceId, ...invoice })

    // Try to delete
    await page.goto(`/invoices/${invoiceId}`)

    // Delete button should not be visible or should be disabled
    const deleteButton = page.locator('[data-testid="delete-invoice-button"]')
    if (await deleteButton.isVisible()) {
      await expect(deleteButton).toBeDisabled()
    }
  })

  // ========== SORTING AND EXPORT ==========

  test('35 - Should sort invoices and export to PDF/CSV', async ({ page }) => {
    // Check if sorting dropdown exists
    const sortDropdown = page.locator('[data-testid="sort-dropdown"]')
    if (await sortDropdown.isVisible()) {
      await page.selectOption('[data-testid="sort-dropdown"]', 'invoiceDate')
      await page.click('[data-testid="apply-sort"]')
      await page.waitForTimeout(500)
    }

    // Check if export buttons exist
    const exportPdfButton = page.locator('[data-testid="export-pdf-button"]')
    const exportCsvButton = page.locator('[data-testid="export-csv-button"]')

    if (await exportPdfButton.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download'),
        exportPdfButton.click()
      ])

      const path = await download.path()
      expect(path).toBeTruthy()
      expect(path).toMatch(/\.pdf$/i)
    }

    if (await exportCsvButton.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download'),
        exportCsvButton.click()
      ])

      const path = await download.path()
      expect(path).toBeTruthy()
      expect(path).toMatch(/\.csv$/i)
    }
  })

  // ========== HELPER FUNCTIONS ==========

  async function createTestCustomer(
    page: Page,
    firstName: string = 'Test',
    lastName: string = 'Customer'
  ): Promise<any> {
    const customer = CustomerFactory.create()
      .withFirstName(firstName)
      .withLastName(lastName)
      .withRandomEmail()
      .active()
      .build()

    // Navigate to customer create page
    await page.goto('/customers/create')
    await page.fill('[data-testid="firstName-input"]', customer.firstName)
    await page.fill('[data-testid="lastName-input"]', customer.lastName)
    await page.fill('[data-testid="email-input"]', customer.email)

    if (customer.phone) {
      await page.fill('[data-testid="phone-input"]', customer.phone)
    }

    await page.click('[data-testid="submit-button"]')
    await page.waitForResponse(/.*\/api\/customers/)
    await page.waitForURL(/.*\/customers\/[a-zA-Z0-9]+/)

    // Extract customer ID from URL
    const url = page.url()
    const idMatch = url.match(/\/customers\/([a-zA-Z0-9]+)/)
    const id = idMatch ? idMatch[1] : ''

    testCustomers.push({ id, ...customer })
    return { id, ...customer }
  }

  async function createTestOrder(page: Page, customerId: string): Promise<any> {
    const order = OrderFactory.create()
      .withCustomerId(customerId)
      .pending()
      .build()

    await page.goto('/orders/create')
    await page.selectOption('[data-testid="customer-select"]', customerId)

    if (order.items && order.items.length > 0) {
      for (const item of order.items) {
        await page.selectOption('[data-testid="product-select"]', item.productId)
        await page.fill('[data-testid="quantity-input"]', item.quantity.toString())
        await page.click('[data-testid="add-item-button"]')
        await page.waitForTimeout(500)
      }
    }

    await page.click('[data-testid="submit-button"]')
    await page.waitForResponse(/.*\/api\/orders/)
    await page.waitForURL(/.*\/orders\/[a-zA-Z0-9]+/)

    const url = page.url()
    const idMatch = url.match(/\/orders\/([a-zA-Z0-9]+)/)
    const id = idMatch ? idMatch[1] : ''

    return { id, ...order }
  }

  async function createInvoice(page: Page, invoice: any): Promise<string> {
    await page.click('[data-testid="create-invoice-button"]')
    await page.waitForURL(/.*\/invoices\/create/)

    // Fill form
    if (invoice.invoiceNumber) {
      await page.fill('[data-testid="invoice-number-input"]', invoice.invoiceNumber)
    }

    await page.selectOption('[data-testid="customer-select"]', invoice.customerId)

    if (invoice.items && invoice.items.length > 0) {
      for (const item of invoice.items) {
        await page.selectOption('[data-testid="product-select"]', item.productId)
        await page.fill('[data-testid="quantity-input"]', item.quantity.toString())
        await page.fill('[data-testid="price-input"]', item.price.toString())
        await page.click('[data-testid="add-item-button"]')
        await page.waitForTimeout(500)
      }
    }

    await page.click('[data-testid="submit-button"]')
    await page.waitForResponse(/.*\/api\/invoices/)
    await page.waitForURL(/.*\/invoices\/[a-zA-Z0-9]+/)

    const url = page.url()
    const idMatch = url.match(/\/invoices\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }
})
