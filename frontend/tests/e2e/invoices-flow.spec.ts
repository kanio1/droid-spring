import { test, expect } from '@playwright/test'

test.describe('Invoices Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to invoices page
    await page.goto('/invoices')

    // Wait for page to load
    await page.waitForSelector('.invoices-page', { timeout: 5000 })
  })

  test('should display invoices list', async ({ page }) => {
    // Check page header
    await expect(page.locator('h1.page-title')).toContainText('Invoices')

    // Check if table is visible
    await expect(page.locator('.invoices-table')).toBeVisible()

    // Check if filters are present
    await expect(page.locator('input[placeholder*="Search"]')).toBeVisible()
    await expect(page.locator('.p-dropdown')).toBeVisible()
  })

  test('should filter invoices by status', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply status filter
    const statusFilter = page.locator('.p-dropdown').first()
    await statusFilter.click()
    await page.click('[pdropdownitem="DRAFT"]')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if filter is applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should search invoices', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Type in search box
    const searchInput = page.locator('input[placeholder*="Search"]')
    await searchInput.fill('test')

    // Wait for debounced search
    await page.waitForTimeout(500)

    // Check if search is applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should navigate to create invoice page', async ({ page }) => {
    // Click Create Invoice button
    await page.click('button[label="Create Invoice"]')

    // Wait for navigation
    await page.waitForURL('/invoices/create')

    // Check if create form is visible
    await expect(page.locator('h1.page-title')).toContainText('Create New Invoice')
    await expect(page.locator('input[placeholder*="Invoice Number"]')).toBeVisible()
  })

  test('should navigate to invoice details', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Check if view button exists and click it
    const viewButton = page.locator('table tbody tr').first().locator('button i.pi-eye').first()
    if (await viewButton.isVisible()) {
      await viewButton.click()

      // Wait for navigation
      await page.waitForSelector('.invoice-detail-page', { timeout: 5000 })

      // Check if invoice details are displayed
      await expect(page.locator('h1.page-title')).toBeVisible()
    }
  })

  test('should navigate to unpaid invoices page', async ({ page }) => {
    // Navigate to unpaid invoices page directly
    await page.goto('/invoices/unpaid')

    // Check if page loads
    await page.waitForSelector('.unpaid-invoices-page', { timeout: 5000 })

    // Check page header
    await expect(page.locator('h1.page-title')).toContainText('Unpaid Invoices')

    // Check if summary cards are visible
    await expect(page.locator('.summary-cards')).toBeVisible()
  })
})
