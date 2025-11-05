import { test, expect } from '@playwright/test'

test.describe('Billing Flow', () => {
  test.describe('Usage Records', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/billing/usage-records**', async (route) => {
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

    test('should navigate to usage records page', async ({ page }) => {
      await page.goto('/billing/usage-records')

      await expect(page.locator('h1.page-title')).toContainText('Usage Records')
      await expect(page.locator('text=CDR (Call Detail Records)')).toBeVisible()
    })

    test('should display usage records summary cards', async ({ page }) => {
      await page.goto('/billing/usage-records')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Records')).toBeVisible()
      await expect(page.locator('text=Unrated')).toBeVisible()
      await expect(page.locator('text=Rated')).toBeVisible()
      await expect(page.locator('text=Unrated Amount')).toBeVisible()
    })

    test('should filter by usage type', async ({ page }) => {
      await page.goto('/billing/usage-records')

      await page.selectOption('[data-testid="usage-type-filter"]', 'VOICE')
      await page.waitForTimeout(500)

      // Should have triggered filter API call
      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('usageType'))
      expect(filterCall).toBeDefined()
    })

    test('should filter by rated status', async ({ page }) => {
      await page.goto('/billing/usage-records')

      await page.selectOption('[data-testid="rated-filter"]', 'false')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('unrated=true'))
      expect(filterCall).toBeDefined()
    })
  })

  test.describe('Billing Cycles', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/billing/cycles**', async (route) => {
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

    test('should navigate to billing cycles page', async ({ page }) => {
      await page.goto('/billing/cycles')

      await expect(page.locator('h1.page-title')).toContainText('Billing Cycles')
      await expect(page.locator('text=Manage billing cycles')).toBeVisible()
    })

    test('should display billing cycles summary cards', async ({ page }) => {
      await page.goto('/billing/cycles')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Cycles')).toBeVisible()
      await expect(page.locator('text=Pending')).toBeVisible()
      await expect(page.locator('text=Processing')).toBeVisible()
      await expect(page.locator('text=Completed')).toBeVisible()
    })

    test('should navigate to create cycle page', async ({ page }) => {
      await page.goto('/billing/cycles')

      await page.click('button:has-text("Start New Cycle")')
      await expect(page).toHaveURL('/billing/cycles/create')

      await expect(page.locator('h1.page-title')).toContainText('Create New Billing Cycle')
    })

    test('should process billing cycle', async ({ page }) => {
      // Mock cycles with one pending
      await page.route('**/api/billing/cycles**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: '1',
                cycleName: 'January 2024',
                customerId: 'cust-1',
                customerName: 'John Doe',
                startDate: '2024-01-01T00:00:00Z',
                endDate: '2024-01-31T23:59:59Z',
                status: 'PENDING',
                totalAmount: 0,
                currency: 'USD',
                usageRecordCount: 10,
                invoiceCount: 0
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })

      await page.route('**/api/billing/cycles/1/process**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      })

      await page.goto('/billing/cycles')

      const processButton = page.locator('button[icon="pi pi-play"]').first()
      await processButton.click()

      // Handle confirmation dialog
      page.on('dialog', dialog => dialog.accept())
      await page.waitForTimeout(500)

      await expect(page.locator('text=Billing cycle January 2024 is being processed')).toBeVisible()
    })

    test('should view billing cycle details', async ({ page }) => {
      // Mock single cycle response
      await page.route('**/api/billing/cycles/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            cycleName: 'January 2024',
            customerId: 'cust-1',
            customerName: 'John Doe',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-01-31T23:59:59Z',
            status: 'PENDING',
            totalAmount: 299.99,
            currency: 'USD',
            usageRecordCount: 5,
            invoiceCount: 0,
            usageRecords: [
              {
                id: 'ur-1',
                recordId: 'CDR-2024-001',
                usageType: 'VOICE',
                usageAmount: 50,
                timestamp: '2024-01-15T10:30:00Z',
                destination: '+1234567890',
                isRated: false,
                ratedCost: null
              }
            ],
            invoices: []
          })
        })
      })

      await page.goto('/billing/cycles/1')

      await expect(page.locator('h1.page-title')).toContainText('Billing Cycle January 2024')
      await expect(page.locator('text=January 1, 2024 to January 31, 2024')).toBeVisible()
    })

    test('should display usage records in cycle details', async ({ page }) => {
      await page.route('**/api/billing/cycles/1**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: '1',
            cycleName: 'January 2024',
            customerId: 'cust-1',
            customerName: 'John Doe',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-01-31T23:59:59Z',
            status: 'PENDING',
            totalAmount: 299.99,
            currency: 'USD',
            usageRecordCount: 2,
            invoiceCount: 0,
            usageRecords: [
              {
                id: 'ur-1',
                recordId: 'CDR-2024-001',
                usageType: 'VOICE',
                usageAmount: 50,
                timestamp: '2024-01-15T10:30:00Z',
                destination: '+1234567890',
                isRated: false,
                ratedCost: null
              },
              {
                id: 'ur-2',
                recordId: 'CDR-2024-002',
                usageType: 'DATA',
                usageAmount: 1024000,
                timestamp: '2024-01-16T14:20:00Z',
                destination: 'internet',
                isRated: true,
                ratedCost: 25.50
              }
            ],
            invoices: []
          })
        })
      })

      await page.goto('/billing/cycles/1')

      const usageTable = page.locator('.cycle-usage')
      await expect(usageTable).toBeVisible()
      await expect(usageTable.locator('tr')).toHaveCount(3) // 2 data rows + header
    })

    test('should handle empty state when no cycles', async ({ page }) => {
      await page.goto('/billing/cycles')

      const emptyState = page.locator('.empty-state')
      await expect(emptyState).toBeVisible()
      await expect(emptyState.locator('h3')).toContainText('No billing cycles found')
    })
  })
})
