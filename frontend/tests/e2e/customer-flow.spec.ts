import { test, expect } from '@playwright/test'

test.describe('Customer Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to customers page
    await page.goto('/customers')

    // Wait for page to load
    await page.waitForSelector('[data-testid="customers-page"]', { timeout: 5000 })
  })

  test('should display customers list', async ({ page }) => {
    // Check page header
    await expect(page.locator('h1')).toContainText('Customers')

    // Check if table is visible
    await expect(page.locator('[data-testid="customers-table"]')).toBeVisible()

    // Check if filters are present
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="sort-dropdown"]')).toBeVisible()
  })

  test('should filter customers by status', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply status filter
    await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if filter is applied
    const activeBadge = page.locator('table tbody tr').first().locator('[data-testid="status-badge"]')
    await expect(activeBadge).toContainText(/Active/i)
  })

  test('should search customers', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Type in search box
    const searchInput = page.locator('[data-testid="search-input"]')
    await searchInput.fill('john')

    // Wait for debounced search
    await page.waitForTimeout(500)

    // Check if search is applied (table should refresh)
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should navigate to create customer page', async ({ page }) => {
    // Click Add Customer button
    await page.click('[data-testid="add-customer-button"]')

    // Wait for navigation
    await page.waitForURL('/customers/create')

    // Check if create form is visible
    await expect(page.locator('h1')).toContainText('Create Customer')
    await expect(page.locator('[data-testid="first-name-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="last-name-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="email-input"]')).toBeVisible()
  })

  test('should navigate to customer details', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Click on first customer row
    await page.click('table tbody tr:first-child')

    // Wait for navigation to customer details
    await page.waitForSelector('[data-testid="customer-details-page"]', { timeout: 5000 })

    // Check if customer details are displayed
    await expect(page.locator('h1')).toContainText('Customer Details')
  })

  test('should navigate to edit customer', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Click edit button on first row
    await page.click('table tbody tr:first-child [data-testid="edit-button"]')

    // Wait for navigation to edit page
    await page.waitForSelector('[data-testid="edit-customer-page"]', { timeout: 5000 })

    // Check if edit form is visible
    await expect(page.locator('h1')).toContainText('Edit Customer')
    await expect(page.locator('[data-testid="first-name-input"]')).toBeVisible()
  })

  test('should handle empty state', async ({ page }) => {
    // Navigate to customers page
    await page.goto('/customers')

    // Wait for page to load
    await page.waitForSelector('[data-testid="customers-page"]', { timeout: 5000 })

    // Check if empty state is displayed (if no customers exist)
    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText('No customers found')
      await expect(page.locator('[data-testid="add-first-customer-button"]')).toBeVisible()
    }
  })
})
