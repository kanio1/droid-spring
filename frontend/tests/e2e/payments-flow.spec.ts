import { test, expect } from '@playwright/test'

test.describe('Payments Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to payments page
    await page.goto('/payments')

    // Wait for page to load
    await page.waitForSelector('.payments-page', { timeout: 5000 })
  })

  test('should display payments list', async ({ page }) => {
    // Check page header
    await expect(page.locator('h1.page-title')).toContainText('Payments')

    // Check if table is visible
    await expect(page.locator('.payments-table')).toBeVisible()

    // Check if filters are present
    await expect(page.locator('input[placeholder*="Search"]')).toBeVisible()
    await expect(page.locator('.p-dropdown')).toBeVisible()
  })

  test('should filter payments by status', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply status filter
    const statusFilter = page.locator('.p-dropdown').first()
    await statusFilter.click()
    await page.click('[pdropdownitem="PENDING"]')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if filter is applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should search payments', async ({ page }) => {
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

  test('should navigate to create payment page', async ({ page }) => {
    // Click Create Payment button
    await page.click('button[label="Create Payment"]')

    // Wait for navigation
    await page.waitForURL('/payments/create')

    // Check if create form is visible
    await expect(page.locator('h1.page-title')).toContainText('Create New Payment')
    await expect(page.locator('input[placeholder*="Payment Number"]')).toBeVisible()
  })

  test('should navigate to payment details', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Check if view button exists and click it
    const viewButton = page.locator('table tbody tr').first().locator('button i.pi-eye').first()
    if (await viewButton.isVisible()) {
      await viewButton.click()

      // Wait for navigation
      await page.waitForSelector('.payment-detail-page', { timeout: 5000 })

      // Check if payment details are displayed
      await expect(page.locator('h1.page-title')).toBeVisible()
    }
  })
})
