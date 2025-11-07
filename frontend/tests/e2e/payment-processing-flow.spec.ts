import { test, expect } from '@playwright/test'

test.describe('Payment Processing Flow', () => {
  const paymentData = {
    customerId: 'cust-1',
    customerName: 'Bob Wilson',
    invoiceId: 'inv-1',
    invoiceNumber: 'INV-2024-001',
    amount: 299.99,
    currency: 'USD',
    paymentMethod: 'CREDIT_CARD',
    description: 'Payment for premium subscription'
  }

  test.beforeEach(async ({ page }) => {
    // Mock customers API
    await page.route('**/api/customers**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: paymentData.customerId,
            firstName: 'Bob',
            lastName: 'Wilson',
            email: 'bob.wilson@example.com'
          }],
          totalElements: 1
        })
      })
    })

    // Mock invoices API
    await page.route('**/api/invoices**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: paymentData.invoiceId,
            invoiceNumber: paymentData.invoiceNumber,
            customerId: paymentData.customerId,
            customerName: paymentData.customerName,
            totalAmount: paymentData.amount,
            status: 'PENDING'
          }],
          totalElements: 1
        })
      })
    })
  })

  test('should process payment successfully', async ({ page }) => {
    await test.step('Navigate to create payment page', async () => {
      await page.goto('/payments/create')

      // Wait for page to load
      await page.waitForSelector('[data-testid="create-payment-page"]', { timeout: 5000 })

      // Check page header
      await expect(page.locator('h1')).toContainText('Create Payment')
    })

    await test.step('Fill payment form', async () => {
      // Select customer
      await page.selectOption('[data-testid="customer-select"]', paymentData.customerId)
      await expect(page.locator('[data-testid="customer-select"]'))
        .toContainText(paymentData.customerName)

      // Select invoice
      await page.selectOption('[data-testid="invoice-select"]', paymentData.invoiceId)
      await expect(page.locator('[data-testid="invoice-select"]'))
        .toContainText(paymentData.invoiceNumber)

      // Verify amount is auto-filled
      const amountField = page.locator('[data-testid="amount-input"]')
      await expect(amountField).toHaveValue(paymentData.amount.toString())

      // Select payment method
      await page.selectOption('[data-testid="payment-method-select"]', paymentData.paymentMethod)

      // Add description
      await page.fill('[data-testid="description-input"]', paymentData.description)
    })

    await test.step('Submit payment', async () => {
      // Mock payment creation API
      let requestBody: any = null
      await page.route('**/api/payments', async (route) => {
        requestBody = await route.request().postDataJSON()
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'pay-1',
            paymentNumber: `PAY-${Date.now()}`,
            ...requestBody,
            status: 'PENDING',
            createdAt: new Date().toISOString()
          })
        })
      })

      // Submit form
      await page.click('[data-testid="submit-button"]')

      // Wait for success message or navigation
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })

      // Verify success message
      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Payment created successfully')
    })

    await test.step('Verify payment in list', async () => {
      // Navigate to payments list
      await page.goto('/payments')

      // Wait for page to load
      await page.waitForSelector('[data-testid="payments-page"]', { timeout: 5000 })

      // Mock payments list API
      await page.route('**/api/payments**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [{
              id: 'pay-1',
              paymentNumber: `PAY-${Date.now()}`,
              customerId: paymentData.customerId,
              customerName: paymentData.customerName,
              amount: paymentData.amount,
              currency: paymentData.currency,
              status: 'PENDING',
              paymentMethod: paymentData.paymentMethod,
              createdAt: new Date().toISOString()
            }],
            totalElements: 1,
            totalPages: 1
          })
        })
      })

      // Refresh to load new data
      await page.reload()

      // Wait for table to load
      await page.waitForSelector('table tbody tr', { timeout: 5000 })

      // Verify payment appears in list
      const firstRow = page.locator('table tbody tr').first()
      await expect(firstRow).toContainText(paymentData.customerName)
      await expect(firstRow).toContainText(paymentData.amount.toString())
      await expect(firstRow).toContainText('PENDING')
    })

    await test.step('Process payment', async ({ page }) => {
      // Click on payment to view details
      await page.click('table tbody tr:first-child')

      // Wait for payment details page
      await page.waitForSelector('[data-testid="payment-details-page"]', { timeout: 5000 })

      // Mock payment processing API
      await page.route('**/api/payments/pay-1/process**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'pay-1',
              paymentNumber: `PAY-${Date.now()}`,
              status: 'PROCESSING',
              transactionId: `TXN-${Date.now()}`,
              processedAt: new Date().toISOString()
            })
          })
        }
      })

      // Click process payment button
      await page.click('[data-testid="process-payment-button"]')

      // Wait for processing
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })

      // Verify processing started
      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Payment processing started')
    })

    await test.step('Complete payment', async ({ page }) => {
      // Mock payment completion API
      await page.route('**/api/payments/pay-1/complete**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'pay-1',
              paymentNumber: `PAY-${Date.now()}`,
              status: 'COMPLETED',
              transactionId: `TXN-${Date.now()}`,
              receivedDate: new Date().toISOString().split('T')[0],
              completedAt: new Date().toISOString()
            })
          })
        }
      })

      // Click complete payment button
      await page.click('[data-testid="complete-payment-button"]')

      // Wait for completion
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })

      // Verify payment completed
      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Payment completed successfully')

      // Check status badge
      const statusBadge = page.locator('[data-testid="status-badge"]')
      await expect(statusBadge).toContainText(/Completed/i)
    })

    await test.step('Verify payment status in list', async () => {
      // Navigate back to payments list
      await page.goto('/payments')

      // Wait for page to load
      await page.waitForSelector('[data-testid="payments-page"]', { timeout: 5000 })

      // Mock updated payments list
      await page.route('**/api/payments**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [{
              id: 'pay-1',
              paymentNumber: `PAY-${Date.now()}`,
              customerName: paymentData.customerName,
              amount: paymentData.amount,
              status: 'COMPLETED',
              completedAt: new Date().toISOString()
            }],
            totalElements: 1
          })
        })
      })

      // Refresh to load updated data
      await page.reload()

      // Wait for table to load
      await page.waitForSelector('table tbody tr', { timeout: 5000 })

      // Verify payment status updated
      const firstRow = page.locator('table tbody tr').first()
      await expect(firstRow).toContainText('COMPLETED')

      // Check status badge color
      const statusBadge = firstRow.locator('[data-testid="status-badge"]')
      await expect(statusBadge).toHaveClass(/success|completed|green/i)
    })
  })

  test('should handle payment failure', async ({ page }) => {
    await page.goto('/payments/create')
    await page.waitForSelector('[data-testid="create-payment-page"]', { timeout: 5000 })

    // Fill form
    await page.selectOption('[data-testid="customer-select"]', paymentData.customerId)
    await page.selectOption('[data-testid="invoice-select"]', paymentData.invoiceId)
    await page.selectOption('[data-testid="payment-method-select"]', paymentData.paymentMethod)

    // Submit payment
    await page.click('[data-testid="submit-button"]')

    // Navigate to details
    await page.goto('/payments')
    await page.waitForSelector('[data-testid="payments-page"]', { timeout: 5000 })
    await page.click('table tbody tr:first-child')
    await page.waitForSelector('[data-testid="payment-details-page"]', { timeout: 5000 })

    // Mock payment failure API
    await page.route('**/api/payments/pay-1/fail**', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'pay-1',
            status: 'FAILED',
            failureReason: 'Insufficient funds',
            failedAt: new Date().toISOString()
          })
        })
      }
    })

    // Click fail payment button
    await page.click('[data-testid="fail-payment-button"]')

    // Wait for confirmation
    await page.waitForSelector('[data-testid="confirm-dialog"]', { timeout: 5000 })
    await page.click('[data-testid="confirm-fail-button"]')

    // Wait for error message
    await page.waitForSelector('[data-testid="error-message"]', { timeout: 5000 })
    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText('Payment failed: Insufficient funds')
  })

  test('should refund completed payment', async ({ page }) => {
    // Navigate to payments with completed payment
    await page.goto('/payments')
    await page.waitForSelector('[data-testid="payments-page"]', { timeout: 5000 })

    // Mock completed payment
    await page.route('**/api/payments**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: 'pay-1',
            paymentNumber: 'PAY-123',
            customerName: 'Test Customer',
            amount: 100,
            status: 'COMPLETED'
          }],
          totalElements: 1
        })
      })
    })

    await page.reload()
    await page.waitForSelector('table tbody tr', { timeout: 5000 })

    // Click on payment
    await page.click('table tbody tr:first-child')
    await page.waitForSelector('[data-testid="payment-details-page"]', { timeout: 5000 })

    // Mock refund API
    await page.route('**/api/payments/pay-1/refund**', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'pay-1',
            status: 'REFUNDED',
            refundReason: 'Customer request',
            refundedAt: new Date().toISOString()
          })
        })
      }
    })

    // Click refund button
    await page.click('[data-testid="refund-payment-button"]')

    // Wait for confirmation
    await page.waitForSelector('[data-testid="confirm-dialog"]', { timeout: 5000 })

    // Fill refund reason
    await page.fill('[data-testid="refund-reason-input"]', 'Customer request')
    await page.click('[data-testid="confirm-refund-button"]')

    // Wait for success
    await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })
    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText('Payment refunded successfully')
  })
})
