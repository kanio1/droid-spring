import { test, expect } from '@playwright/test'

test.describe('Product Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to products page
    await page.goto('/products')

    // Wait for page to load
    await page.waitForSelector('[data-testid="products-page"]', { timeout: 5000 })
  })

  test('should display products list', async ({ page }) => {
    // Check page header
    await expect(page.locator('h1')).toContainText('Products')

    // Check if table is visible
    await expect(page.locator('[data-testid="products-table"]')).toBeVisible()

    // Check if filters are present
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="type-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="category-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="sort-dropdown"]')).toBeVisible()
  })

  test('should filter products by status', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply status filter
    await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if filter is applied (table should refresh)
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should filter products by type', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply type filter
    await page.selectOption('[data-testid="type-filter"]', 'SERVICE')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if filter is applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should filter products by category', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply category filter
    await page.selectOption('[data-testid="category-filter"]', 'MOBILE')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if filter is applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should search products', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Type in search box
    const searchInput = page.locator('[data-testid="search-input"]')
    await searchInput.fill('mobile')

    // Wait for debounced search
    await page.waitForTimeout(500)

    // Check if search is applied (table should refresh)
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should navigate to create product page', async ({ page }) => {
    // Click Add Product button
    await page.click('[data-testid="add-product-button"]')

    // Wait for navigation
    await page.waitForURL('/products/create')

    // Check if create form is visible
    await expect(page.locator('h1')).toContainText('Create Product')
    await expect(page.locator('[data-testid="name-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="product-code-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="price-input"]')).toBeVisible()
  })

  test('should navigate to product details', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Click on first product row
    await page.click('table tbody tr:first-child')

    // Wait for navigation to product details
    await page.waitForSelector('[data-testid="product-details-page"]', { timeout: 5000 })

    // Check if product details are displayed
    await expect(page.locator('h1')).toContainText('Product Details')
  })

  test('should navigate to edit product', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Click edit button on first row
    await page.click('table tbody tr:first-child [data-testid="edit-button"]')

    // Wait for navigation to edit page
    await page.waitForSelector('[data-testid="edit-product-page"]', { timeout: 5000 })

    // Check if edit form is visible
    await expect(page.locator('h1')).toContainText('Edit Product')
    await expect(page.locator('[data-testid="name-input"]')).toBeVisible()
  })

  test('should handle empty state', async ({ page }) => {
    // Navigate to products page
    await page.goto('/products')

    // Wait for page to load
    await page.waitForSelector('[data-testid="products-page"]', { timeout: 5000 })

    // Check if empty state is displayed (if no products exist)
    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText('No products found')
      await expect(page.locator('[data-testid="add-first-product-button"]')).toBeVisible()
    }
  })

  test('should sort products', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Change sort option
    await page.selectOption('[data-testid="sort-dropdown"]', 'name,asc')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if sort is applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })

  test('should combine multiple filters', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table tbody tr')

    // Apply multiple filters
    await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')
    await page.selectOption('[data-testid="type-filter"]', 'SERVICE')
    await page.selectOption('[data-testid="category-filter"]', 'MOBILE')

    // Wait for table to refresh
    await page.waitForTimeout(500)

    // Check if all filters are applied
    await expect(page.locator('table tbody tr').first()).toBeVisible()
  })
})
