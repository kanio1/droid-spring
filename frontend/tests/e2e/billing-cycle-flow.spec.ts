import { test, expect } from '@playwright/test'

test.describe('Billing Cycle Management Flow', () => {
  const billingCycleData = {
    name: 'Monthly Billing Cycle',
    description: 'Standard monthly billing for all subscriptions',
    billingFrequency: 'MONTHLY',
    dayOfMonth: 1,
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().split('T')[0]
  }

  const usageRecordData = {
    subscriptionId: 'sub-1',
    customerName: 'John Doe',
    serviceName: 'Premium Internet',
    usageAmount: 100,
    unit: 'GB',
    periodStart: new Date().toISOString().split('T')[0],
    periodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  }

  test.beforeEach(async ({ page }) => {
    // Mock subscriptions API
    await page.route('**/api/subscriptions**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: usageRecordData.subscriptionId,
            customerId: 'cust-1',
            customerName: usageRecordData.customerName,
            productName: usageRecordData.serviceName,
            status: 'ACTIVE'
          }],
          totalElements: 1
        })
      })
    })
  })

  test('should create and manage billing cycle', async ({ page }) => {
    await test.step('Navigate to billing dashboard', async () => {
      await page.goto('/billing')

      // Wait for page to load
      await page.waitForSelector('[data-testid="billing-dashboard-page"]', { timeout: 5000 })

      // Check page header
      await expect(page.locator('h1')).toContainText('Billing Dashboard')
    })

    await test.step('Navigate to billing cycles', async () => {
      // Click on Billing Cycles tab/menu
      await page.click('[data-testid="billing-cycles-tab"]')

      // Wait for cycles page to load
      await page.waitForSelector('[data-testid="billing-cycles-page"]', { timeout: 5000 })
    })

    await test.step('Create new billing cycle', async () => {
      // Click Create Billing Cycle button
      await page.click('[data-testid="create-cycle-button"]')

      // Wait for create form to load
      await page.waitForSelector('[data-testid="create-cycle-form"]', { timeout: 5000 })

      // Fill form
      await page.fill('[data-testid="name-input"]', billingCycleData.name)
      await page.fill('[data-testid="description-input"]', billingCycleData.description)

      // Select frequency
      await page.selectOption('[data-testid="frequency-select"]', billingCycleData.billingFrequency)

      // Set day of month
      await page.fill('[data-testid="day-of-month-input"]', billingCycleData.dayOfMonth.toString())

      // Set dates
      await page.fill('[data-testid="start-date-input"]', billingCycleData.startDate)
      await page.fill('[data-testid="end-date-input"]', billingCycleData.endDate)

      // Mock cycle creation API
      await page.route('**/api/billing/cycles', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'cycle-1',
              ...billingCycleData,
              status: 'ACTIVE',
              createdAt: new Date().toISOString()
            })
          })
        }
      })

      // Submit form
      await page.click('[data-testid="submit-button"]')

      // Wait for success message
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })
      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Billing cycle created successfully')
    })

    await test.step('View billing cycle details', async () => {
      // Click on the created cycle
      await page.click('[data-testid="cycle-card"]')

      // Wait for details page
      await page.waitForSelector('[data-testid="cycle-details-page"]', { timeout: 5000 })

      // Verify cycle details
      await expect(page.locator('[data-testid="cycle-name"]')).toContainText(billingCycleData.name)
      await expect(page.locator('[data-testid="cycle-frequency"]')).toContainText('MONTHLY')
      await expect(page.locator('[data-testid="cycle-status"]')).toContainText('ACTIVE')
    })

    await test.step('Import usage records', async () => {
      // Navigate to usage records
      await page.click('[data-testid="usage-records-tab"]')

      // Wait for usage records page
      await page.waitForSelector('[data-testid="usage-records-page"]', { timeout: 5000 })

      // Click Import button
      await page.click('[data-testid="import-usage-button"]')

      // Wait for import modal
      await page.waitForSelector('[data-testid="import-usage-modal"]', { timeout: 5000 })

      // Upload file (mock file)
      const fileInput = page.locator('[data-testid="file-input"]')
      await fileInput.setInputFiles({
        name: 'usage-records.csv',
        mimeType: 'text/csv',
        buffer: Buffer.from('subscriptionId,usageAmount,unit\nsub-1,100,GB')
      })

      // Select billing cycle
      await page.selectOption('[data-testid="cycle-select"]', 'cycle-1')

      // Mock import API
      await page.route('**/api/billing/usage-records/import**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              imported: 1,
              failed: 0,
              totalProcessed: 1
            })
          })
        }
      })

      // Submit import
      await page.click('[data-testid="import-submit-button"]')

      // Wait for import results
      await page.waitForSelector('[data-testid="import-results"]', { timeout: 5000 })
      await expect(page.locator('[data-testid="import-results"]'))
        .toContainText('Imported: 1, Failed: 0')
    })

    await test.step('Generate invoice from usage', async () => {
      // Select usage record
      await page.click('[data-testid="usage-record-checkbox"]')

      // Click Generate Invoice button
      await page.click('[data-testid="generate-invoice-button"]')

      // Wait for confirmation dialog
      await page.waitForSelector('[data-testid="confirm-dialog"]', { timeout: 5000 })

      // Confirm generation
      await page.click('[data-testid="confirm-generate-button"]')

      // Wait for success message
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })
      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Invoice generated successfully')
    })

    await test.step('Process billing cycle', async () => {
      // Navigate back to billing cycles
      await page.click('[data-testid="billing-cycles-tab"]')

      // Wait for cycles page
      await page.waitForSelector('[data-testid="billing-cycles-page"]', { timeout: 5000 })

      // Click Process button on cycle
      await page.click('[data-testid="process-cycle-button"]')

      // Wait for processing dialog
      await page.waitForSelector('[data-testid="process-dialog"]', { timeout: 5000 })

      // Mock processing API
      await page.route('**/api/billing/cycles/cycle-1/process**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              invoicesGenerated: 5,
              totalAmount: 499.95,
              processedAt: new Date().toISOString()
            })
          })
        }
      })

      // Confirm processing
      await page.click('[data-testid="confirm-process-button"]')

      // Wait for processing results
      await page.waitForSelector('[data-testid="process-results"]', { timeout: 10000 })
      await expect(page.locator('[data-testid="process-results"]'))
        .toContainText('Invoices generated: 5')
    })
  })

  test('should view billing analytics', async ({ page }) => {
    await page.goto('/billing')
    await page.waitForSelector('[data-testid="billing-dashboard-page"]', { timeout: 5000 })

    // Mock analytics API
    await page.route('**/api/billing/analytics**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          totalRevenue: 50000,
          activeSubscriptions: 150,
          pendingInvoices: 25,
          overdueAmount: 3500,
          revenueGrowth: 15.5
        })
      })
    })

    // Refresh to load analytics
    await page.reload()

    // Wait for analytics to load
    await page.waitForSelector('[data-testid="analytics-cards"]', { timeout: 5000 })

    // Verify analytics data
    await expect(page.locator('[data-testid="total-revenue"]')).toContainText('50,000')
    await expect(page.locator('[data-testid="active-subscriptions"]')).toContainText('150')
    await expect(page.locator('[data-testid="pending-invoices"]')).toContainText('25')
    await expect(page.locator('[data-testid="overdue-amount"]')).toContainText('3,500')
  })

  test('should export billing data', async ({ page }) => {
    await page.goto('/billing')
    await page.waitForSelector('[data-testid="billing-dashboard-page"]', { timeout: 5000 })

    // Click Export button
    await page.click('[data-testid="export-button"]')

    // Wait for export dialog
    await page.waitForSelector('[data-testid="export-dialog"]', { timeout: 5000 })

    // Select date range
    await page.fill('[data-testid="start-date-input"]', '2024-01-01')
    await page.fill('[data-testid="end-date-input"]', '2024-12-31')

    // Select export format
    await page.selectOption('[data-testid="format-select"]', 'CSV')

    // Click Export
    await page.click('[data-testid="export-submit-button"]')

    // Wait for download to start
    await page.waitForTimeout(2000)

    // Note: In a real test, you would verify the file was downloaded
    // This is just checking the action was triggered
  })

  test('should handle usage record validation', async ({ page }) => {
    await page.goto('/billing/usage-records')
    await page.waitForSelector('[data-testid="usage-records-page"]', { timeout: 5000 })

    // Click Add Usage Record button
    await page.click('[data-testid="add-usage-button"]')

    // Wait for form modal
    await page.waitForSelector('[data-testid="usage-form-modal"]', { timeout: 5000 })

    // Try to submit without required fields
    await page.click('[data-testid="submit-button"]')

    // Check for validation errors
    await expect(page.locator('[data-testid="subscription-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="amount-error"]')).toBeVisible()

    // Fill only subscription
    await page.selectOption('[data-testid="subscription-select"]', usageRecordData.subscriptionId)

    // Try to submit again
    await page.click('[data-testid="submit-button"]')

    // Should still show amount error
    await expect(page.locator('[data-testid="amount-error"]')).toBeVisible()

    // Fill amount with invalid value
    await page.fill('[data-testid="amount-input"]', '-100')

    // Try to submit
    await page.click('[data-testid="submit-button"]')

    // Should show validation error for negative amount
    await expect(page.locator('[data-testid="amount-error"]'))
      .toContainText('must be positive')
  })
})
