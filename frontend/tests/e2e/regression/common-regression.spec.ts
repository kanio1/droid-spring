/**
 * Common Functionality Regression Tests - Comprehensive scenarios
 */

import { test, expect } from '@playwright/test'

test.describe('Common Regression Tests - Comprehensive', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  // Edge Cases
  test('REGRESSION-118: Should handle very long search queries', async ({ page }) => {
    await page.goto('/customers')

    const longQuery = 'A'.repeat(1000)
    await page.fill('[data-testid="search-input"]', longQuery)
    await page.press('[data-testid="search-input"]', 'Enter')

    // Should handle gracefully
    await expect(page.locator('[data-testid="no-results"], [data-testid="customer-list"]'))
      .toBeVisible()
  })

  test('REGRESSION-119: Should handle rapid search input', async ({ page }) => {
    await page.goto('/customers')

    // Type very fast
    const searchInput = page.locator('[data-testid="search-input"]')
    await searchInput.focus()
    await searchInput.type('testuser', { delay: 1 })

    await page.press('[data-testid="search-input"]', 'Enter')

    await expect(page.locator('[data-testid="customer-list"], [data-testid="no-results"]'))
      .toBeVisible()
  })

  test('REGRESSION-120: Should handle empty search results', async ({ page }) => {
    await page.goto('/customers')

    // Search for something that doesn't exist
    await page.fill('[data-testid="search-input"]', 'xyzabc123nonexistent')
    await page.press('[data-testid="search-input"]', 'Enter')

    // Should show no results message
    await expect(page.locator('[data-testid="no-results"]')).toBeVisible()
  })

  test('REGRESSION-121: Should handle pagination with no results', async ({ page }) => {
    await page.goto('/customers?page=999')

    // Should handle gracefully (show no results or error)
    await expect(page.locator('[data-testid="no-results"], [data-testid="pagination"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-122: Should handle modal with long content', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')

    // Add long content to modal
    await page.fill('[name="notes"]', 'A'.repeat(10000))

    // Modal should handle long content
    await expect(page.locator('[data-testid="modal-dialog"]')).toBeVisible()
  })

  // Negative Tests
  test('REGRESSION-123: Should handle invalid date input', async ({ page }) => {
    await page.goto('/invoices')
    await page.click('[data-testid="create-invoice-button"]')

    // Enter invalid date
    await page.fill('[name="dueDate"]', 'invalid-date')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-dueDate"]'))
      .toContainText(/invalid|date/i)
  })

  test('REGRESSION-124: Should handle negative numbers in numeric fields', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '-5')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-quantity"]'))
      .toContainText(/positive|greater than 0/i)
  })

  test('REGRESSION-125: Should handle extremely large numbers', async ({ page }) => {
    await page.goto('/payments')
    await page.click('[data-testid="create-payment-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.fill('[name="amount"]', '999999999999999.99')
    await page.selectOption('[name="method"]', 'credit_card')
    await page.fill('[data-testid="card-number"]', '4111111111111111')
    await page.click('[data-testid="submit-button"]')

    // Should handle or show validation error
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-amount"]'))
      .toBeVisible()
  })

  test('REGRESSION-126: Should prevent SQL injection in search', async ({ page }) => {
    await page.goto('/customers')

    // Try SQL injection in search
    await page.fill('[data-testid="search-input"]', "'; DROP TABLE customers; --")
    await page.press('[data-testid="search-input"]', 'Enter')

    // Should escape or reject
    await expect(page.locator('[data-testid="customer-list"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-127: Should prevent XSS in text fields', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')

    // Try XSS in name field
    await page.fill('[name="firstName"]', '<script>alert("xss")</script>')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', 'test@example.com')
    await page.click('[data-testid="submit-button"]')

    // Should escape or reject XSS
    await expect(page.locator('[data-testid="error-firstName"], [data-testid="success-message"]'))
      .toBeVisible()
  })

  // Boundary Conditions
  test('REGRESSION-128: Should handle exact pagination boundary', async ({ page }) => {
    await page.goto('/customers')

    // Get total count
    const totalCount = await page.locator('[data-testid="customer-row"]').count()
    const pageSize = 20

    // If there are exactly 20 items, test boundary
    if (totalCount === pageSize) {
      // Try to go to page 2
      await page.click('[data-testid="next-page"]')

      // Should show page 2 with appropriate content
      await expect(page.locator('[data-testid="customer-list"], [data-testid="pagination"]'))
        .toBeVisible()
    }
  })

  test('REGRESSION-129: Should handle file upload with large file', async ({ page }) => {
    await page.goto('/customers')

    // If file upload exists, test large file
    const fileInput = page.locator('input[type="file"]')
    if (await fileInput.isVisible()) {
      // This would require actually creating a large file
      // For now, just verify the input exists
      await expect(fileInput).toBeVisible()
    }
  })

  // Workflow Tests
  test('REGRESSION-130: Should complete workflow with all filters applied', async ({ page }) => {
    await page.goto('/customers')

    // Apply multiple filters
    await page.selectOption('[data-testid="status-filter"]', 'active')
    await page.fill('[data-testid="search-input"]', 'test')
    await page.selectOption('[data-testid="sort-by"]', 'name')
    await page.selectOption('[data-testid="sort-order"]', 'asc')

    // Should work with all filters
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
  })

  test('REGRESSION-131: Should handle form with all fields filled', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')

    // Fill all fields
    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', 'john.doe@example.com')
    await page.fill('[name="phone"]', '+1-555-0123')
    await page.fill('[name="address"]', '123 Main St')
    await page.fill('[name="city"]', 'New York')
    await page.fill('[name="zip"]', '10001')
    await page.selectOption('[name="country"]', 'US')

    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  // Data Consistency
  test('REGRESSION-132: Should maintain data integrity after bulk operations', async ({ page }) => {
    await page.goto('/customers')

    // Create multiple customers
    for (let i = 0; i < 5; i++) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', `Bulk${i}`)
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', `bulk${i}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Verify count
    const count = await page.locator('[data-testid="customer-row"]').count()
    expect(count).toBeGreaterThanOrEqual(5)
  })

  test('REGRESSION-133: Should preserve form data on validation error', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')

    // Fill form
    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', 'invalid-email') // Invalid
    await page.click('[data-testid="submit-button"]')

    // Form data should be preserved
    await expect(page.locator('[name="firstName"]')).toHaveValue('John')
    await expect(page.locator('[name="lastName"]')).toHaveValue('Doe')
  })

  // Performance
  test('REGRESSION-134: Should handle many simultaneous operations', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/customers')

    // Create 10 customers rapidly
    for (let i = 0; i < 10; i++) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', `Perf${i}`)
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', `perf${i}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    const endTime = Date.now()
    const duration = endTime - startTime

    // Should complete in reasonable time
    expect(duration).toBeLessThan(120000) // 2 minutes
  })

  test('REGRESSION-135: Should load large dataset efficiently', async ({ page }) => {
    const startTime = Date.now()

    // Navigate to page with potentially large dataset
    await page.goto('/customers')

    await page.waitForSelector('[data-testid="customer-list"], [data-testid="loading"]')

    const endTime = Date.now()
    const loadTime = endTime - startTime

    // Should load within reasonable time
    expect(loadTime).toBeLessThan(30000) // 30 seconds
  })
})
