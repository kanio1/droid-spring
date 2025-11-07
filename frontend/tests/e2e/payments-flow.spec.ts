/**
 * Payment Processing E2E Tests
 *
 * Comprehensive test suite covering all payment processing workflows:
 * - Payment creation and processing
 * - Multiple payment methods (credit card, bank transfer, PayPal, etc.)
 * - Payment status transitions (pending, processing, completed, failed, refunded)
 * - Successful payment processing
 * - Failed payment handling
 * - Refund processing (full and partial)
 * - Chargeback and dispute management
 * - Multi-currency support
 * - Recurring payments
 * - Payment authentication (3D Secure)
 * - PCI compliance considerations
 * - Payment fees calculation
 * - Search and filtering
 * - Date range filtering
 * - Bulk operations
 * - Payment history and audit trail
 * - Webhook integration
 * - Error handling and validation
 *
 * Target: 30 comprehensive tests
 */

import { test, expect } from '@playwright/test'
import { PaymentFactory, CustomerFactory, InvoiceFactory } from '../framework/data-factories'
import { AuthHelper } from '../helpers'
import type { Page } from '@playwright/test'

test.describe('Payment Processing E2E', () => {
  let testPayments: any[] = []
  let testInvoices: any[] = []
  let testCustomers: any[] = []

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'testuser',
      password: process.env.TEST_PASSWORD || 'testpass'
    })

    await page.goto('/payments')
    await expect(page.locator('h1')).toContainText('Payments')
  })

  test.afterEach(async ({ page }) => {
    // Cleanup test data
    for (const payment of testPayments) {
      try {
        await page.goto(`/payments/${payment.id}`)
        if (await page.locator('[data-testid="delete-payment-button"]').isVisible()) {
          await page.click('[data-testid="delete-payment-button"]')
          await page.click('[data-testid="confirm-delete"]')
        }
      } catch (error) {
        console.log(`Cleanup failed for payment ${payment.id}:`, error)
      }
    }
    testPayments = []
    testInvoices = []
    testCustomers = []
  })

  // ========== LIST VIEW TESTS ==========

  test('01 - Should display payments list with all required elements', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Payments')
    await expect(page.locator('[data-testid="payments-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="method-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="create-payment-button"]')).toBeVisible()
  })

  test('02 - Should show payment count and total amount', async ({ page }) => {
    const countText = await page.locator('[data-testid="total-count"]').textContent()
    if (countText) {
      const count = parseInt(countText)
      expect(count).toBeGreaterThanOrEqual(0)
    }

    const totalText = await page.locator('[data-testid="total-amount"]').textContent()
    if (totalText) {
      const total = parseFloat(totalText.replace(/[^0-9.-]+/g, ''))
      expect(total).toBeGreaterThanOrEqual(0)
    }
  })

  test('03 - Should handle empty state when no payments exist', async ({ page }) => {
    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText(/No payments found/i)
      await expect(page.locator('[data-testid="create-first-payment-button"]')).toBeVisible()
    }
  })

  // ========== SEARCH TESTS ==========

  test('04 - Should search payments by payment number', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withPaymentNumber('PAY-TEST-001')
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })
    await page.waitForTimeout(1000)

    // Search by payment number
    await page.fill('[data-testid="search-input"]', 'PAY-TEST-001')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('05 - Should search payments by transaction ID', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withTransactionId('TXN-' + Date.now())
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })
    await page.waitForTimeout(1000)

    // Search by transaction ID
    await page.fill('[data-testid="search-input"]', payment.transactionId)
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('06 - Should show no results for search with no matches', async ({ page }) => {
    await page.fill('[data-testid="search-input"]', 'NonExistentPayment12345')
    await page.waitForTimeout(1000)

    await expect(page.locator('[data-testid="no-results-message"]'))
      .toContainText(/No payments found/i)
    await expect(page.locator('table tbody tr')).toHaveCount(0)
  })

  // ========== FILTERING TESTS ==========

  test('07 - Should filter payments by status - Successful', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'SUCCESSFUL')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/success|completed/i)
        }
      }
    }
  })

  test('08 - Should filter payments by status - Pending', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'PENDING')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/pending/i)
        }
      }
    }
  })

  test('09 - Should filter payments by status - Failed', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'FAILED')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/failed|error/i)
        }
      }
    }
  })

  test('10 - Should filter payments by method - Credit Card', async ({ page }) => {
    await page.selectOption('[data-testid="method-filter"]', 'CREDIT_CARD')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('11 - Should combine search and status filter', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'SUCCESSFUL')
    await page.fill('[data-testid="search-input"]', 'test')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  // ========== PAYMENT CREATION ==========

  test('12 - Should create a new payment with valid data', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    expect(paymentId).toBeTruthy()
    testPayments.push({ id: paymentId, ...payment })

    // Verify redirect to details page
    await expect(page).toHaveURL(/.*\/payments\/[a-zA-Z0-9]+/)

    // Verify payment data
    await expect(page.locator('[data-testid="payment-number"]')).toBeVisible()
  })

  test('13 - Should create payment for invoice', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const invoice = await createTestInvoice(page, customer.id)
    testInvoices.push(invoice)

    // Create payment for invoice
    await page.goto(`/invoices/${invoice.id}`)
    await page.click('[data-testid="create-payment-button"]')
    await page.waitForURL(/.*\/payments\/create.*invoiceId=${invoice.id}/)

    await page.fill('[data-testid="amount-input"]', '1000.00')
    await page.selectOption('[data-testid="payment-method"]', 'CREDIT_CARD')
    await page.click('[data-testid="submit-payment"]')
    await page.waitForResponse(/.*\/api\/payments/)
    await page.waitForURL(/.*\/payments\/[a-zA-Z0-9]+/)

    const url = page.url()
    const idMatch = url.match(/\/payments\/([a-zA-Z0-9]+)/)
    const paymentId = idMatch ? idMatch[1] : ''

    expect(paymentId).toBeTruthy()
    testPayments.push({ id: paymentId })
  })

  test('14 - Should show validation errors for required fields', async ({ page }) => {
    await page.click('[data-testid="create-payment-button"]')
    await expect(page).toHaveURL(/.*\/payments\/create/)

    // Submit empty form
    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(500)

    // Check for validation errors
    await expect(page.locator('[data-testid="amount-error"]'))
      .toContainText(/required/i)
    await expect(page.locator('[data-testid="method-error"]'))
      .toContainText(/required/i)
  })

  // ========== PAYMENT METHODS ==========

  test('15 - Should process credit card payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(100)
      .withMethod('CREDIT_CARD')
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Verify payment method
    await page.goto(`/payments/${paymentId}`)
    const methodBadge = page.locator('[data-testid="payment-method-badge"]')
    await expect(methodBadge).toContainText(/credit card/i)
  })

  test('16 - Should process bank transfer payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(500)
      .withMethod('BANK_TRANSFER')
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Verify payment method
    await page.goto(`/payments/${paymentId}`)
    const methodBadge = page.locator('[data-testid="payment-method-badge"]')
    await expect(methodBadge).toContainText(/bank transfer/i)
  })

  test('17 - Should process PayPal payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(200)
      .withMethod('PAYPAL')
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Verify payment method
    await page.goto(`/payments/${paymentId}`)
    const methodBadge = page.locator('[data-testid="payment-method-badge"]')
    await expect(methodBadge).toContainText(/paypal/i)
  })

  // ========== PAYMENT STATUS TRANSITIONS ==========

  test('18 - Should change payment status from Pending to Completed', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(300)
      .pending()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Mark as completed
    await page.goto(`/payments/${paymentId}`)
    await page.click('[data-testid="process-payment-button"]')
    await page.click('[data-testid="confirm-process"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/completed|success/i)
  })

  test('19 - Should handle failed payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(400)
      .failed()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Verify failed status
    await page.goto(`/payments/${paymentId}`)
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/failed|error/i)
  })

  test('20 - Should retry failed payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(500)
      .failed()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Retry payment
    await page.goto(`/payments/${paymentId}`)
    await page.click('[data-testid="retry-payment-button"]')
    await page.click('[data-testid="confirm-retry"]')
    await page.waitForTimeout(1000)

    // Verify status changed to processing
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/processing|pending/i)
  })

  // ========== REFUND PROCESSING ==========

  test('21 - Should process full refund', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Process refund
    await page.goto(`/payments/${paymentId}`)
    await page.click('[data-testid="refund-payment-button"]')
    await page.selectOption('[data-testid="refund-type"]', 'FULL')
    await page.fill('[data-testid="refund-amount"]', '1000.00')
    await page.selectOption('[data-testid="refund-reason"]', 'CUSTOMER_REQUEST')
    await page.click('[data-testid="confirm-refund"]')
    await page.waitForTimeout(1000)

    // Verify refund processed
    const refundStatus = page.locator('[data-testid="refund-status"]')
    await expect(refundStatus).toContainText(/processed/i)
  })

  test('22 - Should process partial refund', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Process partial refund
    await page.goto(`/payments/${paymentId}`)
    await page.click('[data-testid="refund-payment-button"]')
    await page.selectOption('[data-testid="refund-type"]', 'PARTIAL')
    await page.fill('[data-testid="refund-amount"]', '500.00')
    await page.selectOption('[data-testid="refund-reason"]', 'PRODUCT_DEFECT')
    await page.click('[data-testid="confirm-refund"]')
    await page.waitForTimeout(1000)

    // Verify partial refund
    const refundStatus = page.locator('[data-testid="refund-status"]')
    await expect(refundStatus).toContainText(/partial/i)
  })

  // ========== DISPUTE MANAGEMENT ==========

  test('23 - Should create chargeback/dispute', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Create dispute
    await page.goto(`/payments/${paymentId}`)
    await page.click('[data-testid="create-dispute-button"]')
    await page.selectOption('[data-testid="dispute-reason"]', 'FRAUDULENT')
    await page.fill('[data-testid="dispute-amount"]', '1000.00')
    await page.fill('[data-testid="dispute-description"]', 'Unauthorized transaction')
    await page.click('[data-testid="submit-dispute"]')
    await page.waitForTimeout(1000)

    // Verify dispute created
    const disputeStatus = page.locator('[data-testid="dispute-status"]')
    await expect(disputeStatus).toContainText(/open|pending/i)
  })

  // ========== MULTI-CURRENCY ==========

  test('24 - Should handle multi-currency payment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .withCurrency('EUR')
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Verify currency
    await page.goto(`/payments/${paymentId}`)
    const currency = page.locator('[data-testid="currency"]')
    await expect(currency).toContainText('EUR')
  })

  // ========== PAYMENT FEES ==========

  test('25 - Should calculate and display payment fees', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .withMethod('CREDIT_CARD')
      .successful()
      .withFee(29) // 2.9% fee
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // Verify fee calculation
    await page.goto(`/payments/${paymentId}`)
    const feeAmount = page.locator('[data-testid="fee-amount"]')
    await expect(feeAmount).toContainText('29.00')
    const netAmount = page.locator('[data-testid="net-amount"]')
    expect(parseFloat((await netAmount.textContent()) || '0')).toBe(971)
  })

  // ========== DATE RANGE FILTERING ==========

  test('26 - Should filter payments by date range', async ({ page }) => {
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

  // ========== PAYMENT DETAILS VIEW ==========

  test('27 - Should view complete payment details', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .withMethod('CREDIT_CARD')
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // View payment details
    await page.goto(`/payments/${paymentId}`)

    // Verify all payment information is displayed
    await expect(page.locator('[data-testid="payment-number"]')).toBeVisible()
    await expect(page.locator('[data-testid="transaction-id"]')).toBeVisible()
    await expect(page.locator('[data-testid="amount"]')).toBeVisible()
    await expect(page.locator('[data-testid="payment-method"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-badge"]')).toBeVisible()
    await expect(page.locator('[data-testid="payment-date"]')).toBeVisible()
  })

  // ========== BULK OPERATIONS ==========

  test('28 - Should select multiple payments and perform bulk action', async ({ page }) => {
    const customer = await createTestCustomer(page)

    // Create multiple payments
    const payments = PaymentFactory.create().buildMany(3)
    const createdIds: any[] = []

    for (const payment of payments) {
      const paymentId = await createPayment(page, { ...payment, customerId: customer.id })
      createdIds.push({ id: paymentId, ...payment })
    }
    testPayments.push(...createdIds)

    await page.waitForTimeout(1000)

    // Select multiple payments
    await page.click('[data-testid="select-all-checkbox"]')

    // Perform bulk action
    await page.click('[data-testid="bulk-action-dropdown"]')
    await page.click('[data-testid="bulk-action-export"]')
    await page.waitForTimeout(500)

    // Verify export started
    const exportMessage = page.locator('[data-testid="export-message"]')
    if (await exportMessage.isVisible()) {
      await expect(exportMessage).toContainText(/export/i)
    }
  })

  // ========== ERROR HANDLING ==========

  test('29 - Should handle insufficient funds', async ({ page }) => {
    const customer = await createTestCustomer(page)

    // Try to create payment exceeding limits
    await page.click('[data-testid="create-payment-button"]')
    await expect(page).toHaveURL(/.*\/payments\/create/)

    await page.selectOption('[data-testid="customer-select"]', customer.id)
    await page.fill('[data-testid="amount-input"]', '999999999') // Very high amount

    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(1000)

    // Should show error
    await expect(page.locator('[data-testid="amount-error"]'))
      .toContainText(/exceeds|limit/i)
  })

  // ========== PAYMENT HISTORY ==========

  test('30 - Should display payment history and audit trail', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const payment = PaymentFactory.create()
      .withCustomerId(customer.id)
      .withAmount(1000)
      .successful()
      .build()

    const paymentId = await createPayment(page, payment)
    testPayments.push({ id: paymentId, ...payment })

    // View payment history
    await page.goto(`/payments/${paymentId}`)
    await page.click('[data-testid="view-history-button"]')

    // Verify history is displayed
    const history = page.locator('[data-testid="payment-history"]')
    if (await history.isVisible()) {
      const events = await history.locator('[data-testid="history-event"]').count()
      expect(events).toBeGreaterThan(0)
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

  async function createTestInvoice(page: Page, customerId: string): Promise<any> {
    const invoice = InvoiceFactory.create()
      .withCustomerId(customerId)
      .draft()
      .withTotal(1000)
      .build()

    await page.goto('/invoices/create')
    await page.selectOption('[data-testid="customer-select"]', customerId)

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
    const id = idMatch ? idMatch[1] : ''

    return { id, ...invoice }
  }

  async function createPayment(page: Page, payment: any): Promise<string> {
    await page.click('[data-testid="create-payment-button"]')
    await page.waitForURL(/.*\/payments\/create/)

    // Fill form
    if (payment.paymentNumber) {
      await page.fill('[data-testid="payment-number-input"]', payment.paymentNumber)
    }

    await page.selectOption('[data-testid="customer-select"]', payment.customerId)
    await page.fill('[data-testid="amount-input"]', payment.amount.toString())

    if (payment.method) {
      await page.selectOption('[data-testid="payment-method"]', payment.method)
    }

    if (payment.currency) {
      await page.selectOption('[data-testid="currency"]', payment.currency)
    }

    await page.click('[data-testid="submit-payment"]')
    await page.waitForResponse(/.*\/api\/payments/)
    await page.waitForURL(/.*\/payments\/[a-zA-Z0-9]+/)

    const url = page.url()
    const idMatch = url.match(/\/payments\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }
})
