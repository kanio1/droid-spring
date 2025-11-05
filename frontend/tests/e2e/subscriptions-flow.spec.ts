import { test, expect } from '@playwright/test'

test.describe('Subscriptions Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Mock API responses
    await page.route('**/api/subscriptions**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0,
          size: 20
        })
      })
    })
  })

  test('should navigate to subscriptions page', async ({ page }) => {
    await page.goto('/subscriptions')

    await expect(page.locator('h1.page-title')).toContainText('Subscriptions')
    await expect(page.locator('text=Manage customer subscriptions')).toBeVisible()
  })

  test('should display subscription summary cards', async ({ page }) => {
    await page.goto('/subscriptions')

    await expect(page.locator('.summary-card')).toHaveCount(4)
    await expect(page.locator('text=Total Subscriptions')).toBeVisible()
    await expect(page.locator('text=Active')).toBeVisible()
    await expect(page.locator('text=Suspended')).toBeVisible()
    await expect(page.locator('text=Expired')).toBeVisible()
  })

  test('should filter subscriptions by status', async ({ page }) => {
    await page.goto('/subscriptions')

    // Mock filtered response
    await page.route('**/api/subscriptions**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: '1',
              subscriptionNumber: 'SUB-2024-001',
              customerId: 'cust-1',
              customerName: 'John Doe',
              productName: 'Premium Internet',
              status: 'ACTIVE',
              startDate: '2024-01-01',
              endDate: '2024-12-31',
              nextBillingDate: '2024-02-01',
              amount: 99.99,
              currency: 'USD',
              autoRenew: true
            }
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20
        })
      })
    })

    await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')
    await page.waitForTimeout(500)

    const tableRows = page.locator('.p-datatable-tbody tr')
    await expect(tableRows).toHaveCount(1)
  })

  test('should search subscriptions', async ({ page }) => {
    await page.goto('/subscriptions')

    const searchInput = page.locator('input[placeholder*="Search subscriptions"]')
    await searchInput.fill('SUB-2024-001')
    await page.keyboard.press('Enter')
    await page.waitForTimeout(500)

    // Should have triggered search API call
    const calls = page.request.allInterceptions()
    const searchCall = calls.find(c => c.request.url().includes('searchTerm'))
    expect(searchCall).toBeDefined()
  })

  test('should navigate to create subscription page', async ({ page }) => {
    await page.goto('/subscriptions')

    await page.click('button:has-text("Create Subscription")')
    await expect(page).toHaveURL('/subscriptions/create')

    await expect(page.locator('h1.page-title')).toContainText('Create New Subscription')
    await expect(page.locator('input[placeholder*="e.g., SUB-2024-001"]')).toBeVisible()
  })

  test('should navigate to subscription detail page', async ({ page }) => {
    // Mock single subscription response
    await page.route('**/api/subscriptions/1**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '1',
          subscriptionNumber: 'SUB-2024-001',
          customerId: 'cust-1',
          customerName: 'John Doe',
          productName: 'Premium Internet',
          productId: 'prod-1',
          status: 'ACTIVE',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          nextBillingDate: '2024-02-01T00:00:00Z',
          amount: 99.99,
          currency: 'USD',
          billingPeriod: 'MONTHLY',
          autoRenew: true,
          sendNotifications: true,
          features: ['Unlimited data', '24/7 support'],
          notes: 'Test subscription',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          version: 1,
          usage: {
            dataUsed: 5000000000,
            dataLimit: 10000000000,
            callMinutes: 250,
            smsCount: 50
          },
          billingHistory: []
        })
      })
    })

    await page.goto('/subscriptions/1')

    await expect(page.locator('h1.page-title')).toContainText('Subscription SUB-2024-001')
    await expect(page.locator('text=Premium Internet for John Doe')).toBeVisible()
  })

  test('should display subscription summary', async ({ page }) => {
    await page.route('**/api/subscriptions/1**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '1',
          subscriptionNumber: 'SUB-2024-001',
          customerId: 'cust-1',
          customerName: 'John Doe',
          productName: 'Premium Internet',
          productId: 'prod-1',
          status: 'ACTIVE',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          nextBillingDate: '2024-02-01T00:00:00Z',
          amount: 99.99,
          currency: 'USD',
          billingPeriod: 'MONTHLY',
          autoRenew: true,
          sendNotifications: true,
          features: ['Unlimited data', '24/7 support'],
          notes: 'Test subscription',
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          version: 1
        })
      })
    })

    await page.goto('/subscriptions/1')

    const summaryItems = page.locator('.summary-item')
    await expect(summaryItems.first()).toContainText('Subscription Number')
    await expect(summaryItems.nth(1)).toContainText('Status')
    await expect(summaryItems.nth(2)).toContainText('Customer')
    await expect(summaryItems.nth(3)).toContainText('Product')
  })

  test('should display product features', async ({ page }) => {
    await page.route('**/api/subscriptions/1**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '1',
          subscriptionNumber: 'SUB-2024-001',
          customerId: 'cust-1',
          customerName: 'John Doe',
          productName: 'Premium Internet',
          productId: 'prod-1',
          status: 'ACTIVE',
          startDate: '2024-01-01T00:00:00Z',
          endDate: '2024-12-31T23:59:59Z',
          nextBillingDate: '2024-02-01T00:00:00Z',
          amount: 99.99,
          currency: 'USD',
          billingPeriod: 'MONTHLY',
          autoRenew: true,
          sendNotifications: true,
          features: ['Unlimited data', '24/7 support'],
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          version: 1
        })
      })
    })

    await page.goto('/subscriptions/1')

    const featuresList = page.locator('.features-list .feature-item')
    await expect(featuresList).toHaveCount(2)
    await expect(featuresList.first()).toContainText('Unlimited data')
    await expect(featuresList.nth(1)).toContainText('24/7 support')
  })

  test('should handle empty state when no subscriptions', async ({ page }) => {
    await page.goto('/subscriptions')

    const emptyState = page.locator('.empty-state')
    await expect(emptyState).toBeVisible()
    await expect(emptyState.locator('h3')).toContainText('No subscriptions found')
  })
})
