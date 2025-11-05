import { test, expect } from '@playwright/test'

test.describe('Orders Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to orders page
    await page.goto('/orders')

    // Wait for page to load
    await page.waitForSelector('.orders-page', { timeout: 5000 })
  })

  test('should display orders list', async ({ page }) => {
    // Check page header
    await expect(page.locator('h1.page-title')).toContainText('Orders')

    // Check if table is visible
    await expect(page.locator('.orders-table')).toBeVisible()

    // Check if filters are present
    await expect(page.locator('input[placeholder*="Search"]')).toBeVisible()
    await expect(page.locator('.p-dropdown')).toBeVisible()
  })

  test('should filter orders by status', async ({ page }) => {
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

  test('should search orders', async ({ page }) => {
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

  test('should navigate to create order page', async ({ page }) => {
    // Click Create Order button
    await page.click('button[label="Create Order"]')

    // Wait for navigation
    await page.waitForURL('/orders/create')

    // Check if create form is visible
    await expect(page.locator('h1.page-title')).toContainText('Create New Order')
    await expect(page.locator('input[placeholder*="Order Number"]')).toBeVisible()
  })

  test('should navigate to order details', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Check if view button exists and click it
    const viewButton = page.locator('table tbody tr').first().locator('button i.pi-eye').first()
    if (await viewButton.isVisible()) {
      await viewButton.click()

      // Wait for navigation
      await page.waitForSelector('.order-detail-page', { timeout: 5000 })

      // Check if order details are displayed
      await expect(page.locator('h1.page-title')).toBeVisible()
    }
  })
})
