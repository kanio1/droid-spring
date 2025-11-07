import { test, expect } from '@playwright/test'

test.describe('Subscription Activation Flow', () => {
  const customerData = {
    firstName: 'Alice',
    lastName: 'Johnson',
    email: `alice.johnson.${Date.now()}@example.com`
  }

  const subscriptionData = {
    productId: 'prod-premium-internet',
    productName: 'Premium Internet 100Mbps',
    planId: 'plan-monthly',
    planName: 'Monthly Plan',
    amount: 99.99,
    currency: 'USD',
    startDate: new Date().toISOString().split('T')[0]
  }

  test.beforeEach(async ({ page }) => {
    // Mock customer API
    await page.route('**/api/customers**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: 'cust-1',
            ...customerData,
            status: 'ACTIVE',
            createdAt: new Date().toISOString()
          }],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20
        })
      })
    })

    // Mock products API
    await page.route('**/api/products**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: subscriptionData.productId,
            name: subscriptionData.productName,
            description: 'High-speed internet connection',
            price: 99.99,
            currency: 'USD',
            status: 'ACTIVE'
          }],
          totalElements: 1,
          totalPages: 1
        })
      })
    })
  })

  test('should create and activate subscription', async ({ page }) => {
    await test.step('Navigate to create subscription page', async () => {
      await page.goto('/subscriptions/create')

      // Wait for page to load
      await page.waitForSelector('[data-testid="create-subscription-page"]', { timeout: 5000 })

      // Check page header
      await expect(page.locator('h1')).toContainText('Create Subscription')
    })

    await test.step('Select customer', async () => {
      // Open customer dropdown
      await page.click('[data-testid="customer-select"]')

      // Wait for options to load
      await page.waitForSelector('[data-testid="customer-option-cust-1"]', { timeout: 5000 })

      // Select customer
      await page.click('[data-testid="customer-option-cust-1"]')

      // Verify selection
      await expect(page.locator('[data-testid="customer-select"]')).toContainText(customerData.firstName)
    })

    await test.step('Select product and plan', async () => {
      // Select product
      await page.selectOption('[data-testid="product-select"]', subscriptionData.productId)

      // Verify product selection
      await expect(page.locator('[data-testid="product-select"]')).toContainText(subscriptionData.productName)

      // Select plan
      await page.selectOption('[data-testid="plan-select"]', subscriptionData.planId)

      // Verify plan selection
      await expect(page.locator('[data-testid="plan-select"]')).toContainText(subscriptionData.planName)

      // Check if price is displayed
      await expect(page.locator('[data-testid="price-display"]')).toContainText('99.99')
    })

    await test.step('Configure subscription settings', async () => {
      // Set start date
      await page.fill('[data-testid="start-date-input"]', subscriptionData.startDate)

      // Enable auto-renew (if checkbox exists)
      const autoRenewCheckbox = page.locator('[data-testid="auto-renew-checkbox"]')
      if (await autoRenewCheckbox.isVisible()) {
        await autoRenewCheckbox.check()
      }

      // Add notes
      await page.fill('[data-testid="notes-input"]', 'Initial subscription activation')
    })

    await test.step('Create subscription', async () => {
      // Mock subscription creation API
      await page.route('**/api/subscriptions', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'sub-1',
              subscriptionNumber: `SUB-${Date.now()}`,
              customerId: 'cust-1',
              customerName: `${customerData.firstName} ${customerData.lastName}`,
              productId: subscriptionData.productId,
              productName: subscriptionData.productName,
              planId: subscriptionData.planId,
              planName: subscriptionData.planName,
              status: 'PENDING_ACTIVATION',
              startDate: subscriptionData.startDate,
              amount: subscriptionData.amount,
              currency: subscriptionData.currency,
              autoRenew: true,
              createdAt: new Date().toISOString()
            })
          })
        }
      })

      // Submit form
      await page.click('[data-testid="submit-button"]')

      // Wait for navigation or success message
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })

      // Verify success message
      await expect(page.locator('[data-testid="success-message"]')).toContainText('Subscription created successfully')
    })

    await test.step('Activate subscription', async () => {
      // Mock activation API
      await page.route('**/api/subscriptions/sub-1/activate**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'sub-1',
              subscriptionNumber: `SUB-${Date.now()}`,
              status: 'ACTIVE',
              activatedAt: new Date().toISOString()
            })
          })
        }
      })

      // Click activate button
      await page.click('[data-testid="activate-button"]')

      // Wait for activation to complete
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })

      // Verify activation success
      await expect(page.locator('[data-testid="success-message"]')).toContainText('Subscription activated successfully')

      // Check if status changed to ACTIVE
      const statusBadge = page.locator('[data-testid="status-badge"]')
      await expect(statusBadge).toContainText(/Active/i)
    })

    await test.step('Verify subscription in list', async () => {
      // Navigate to subscriptions list
      await page.goto('/subscriptions')

      // Wait for page to load
      await page.waitForSelector('[data-testid="subscriptions-page"]', { timeout: 5000 })

      // Mock subscriptions list API
      await page.route('**/api/subscriptions**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [{
              id: 'sub-1',
              subscriptionNumber: `SUB-${Date.now()}`,
              customerName: `${customerData.firstName} ${customerData.lastName}`,
              productName: subscriptionData.productName,
              status: 'ACTIVE',
              startDate: subscriptionData.startDate,
              amount: subscriptionData.amount,
              currency: subscriptionData.currency
            }],
            totalElements: 1,
            totalPages: 1
          })
        })
      })

      // Refresh page to load new data
      await page.reload()

      // Wait for table to load
      await page.waitForSelector('table tbody tr', { timeout: 5000 })

      // Verify subscription appears in list
      const firstRow = page.locator('table tbody tr').first()
      await expect(firstRow).toContainText(subscriptionData.productName)
      await expect(firstRow).toContainText(`${customerData.firstName} ${customerData.lastName}`)

      // Check status badge
      const statusBadge = firstRow.locator('[data-testid="status-badge"]')
      await expect(statusBadge).toContainText(/Active/i)
    })
  })

  test('should handle subscription activation errors', async ({ page }) => {
    await page.goto('/subscriptions/create')
    await page.waitForSelector('[data-testid="create-subscription-page"]', { timeout: 5000 })

    // Try to create subscription without selecting customer
    await page.click('[data-testid="submit-button"]')

    // Should show validation error
    await expect(page.locator('[data-testid="customer-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="customer-error"]')).toContainText('Customer is required')
  })

  test('should suspend active subscription', async ({ page }) => {
    // Mock active subscription
    await page.route('**/api/subscriptions**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [{
            id: 'sub-1',
            subscriptionNumber: 'SUB-123',
            customerName: 'Test Customer',
            productName: 'Premium Internet',
            status: 'ACTIVE',
            amount: 99.99,
            currency: 'USD'
          }],
          totalElements: 1,
          totalPages: 1
        })
      })
    })

    await page.goto('/subscriptions')
    await page.waitForSelector('[data-testid="subscriptions-page"]', { timeout: 5000 })

    // Click suspend button on first subscription
    await page.click('table tbody tr:first-child [data-testid="suspend-button"]')

    // Wait for confirmation dialog
    await page.waitForSelector('[data-testid="confirm-dialog"]', { timeout: 5000 })

    // Confirm suspension
    await page.click('[data-testid="confirm-suspend-button"]')

    // Wait for success message
    await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 })
    await expect(page.locator('[data-testid="success-message"]')).toContainText('Subscription suspended')
  })
})
