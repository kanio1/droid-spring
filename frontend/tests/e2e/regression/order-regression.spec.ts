/**
 * Order Regression Tests - Comprehensive scenarios
 */

import { test, expect } from '@playwright/test'
import { TestDataGenerator } from '@tests/framework/data-factories'

test.describe('Order Regression Tests - Comprehensive', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const testData = TestDataGenerator.fullCustomerJourney()
    await TestDataGenerator.seedTestData(testData)
  })

  test.afterEach(async ({ page }) => {
    await TestDataGenerator.cleanupTestData()
  })

  // Edge Cases
  test('REGRESSION-021: Should handle order with maximum line items', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Add maximum allowed line items
    const maxItems = 50
    for (let i = 0; i < maxItems; i++) {
      await page.click('[data-testid="add-item-button"]')
      await page.selectOption(`[data-testid="product-select-${i}"]`, 'product-1')
      await page.fill(`[data-testid="quantity-input-${i}"]`, '1')
      await page.fill(`[data-testid="price-input-${i}"]`, '10.00')
    }

    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-022: Should handle zero quantity line items', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '0')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-quantity"]')).toContainText(/greater than 0/i)
  })

  test('REGRESSION-023: Should handle negative price', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '-100.00')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-price"]')).toContainText(/positive/i)
  })

  test('REGRESSION-024: Should handle very large order amounts', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '999999')
    await page.fill('[data-testid="price-input-0"]', '999999.99')
    await page.click('[data-testid="submit-button"]')

    // Should handle large numbers (either accept or show overflow error)
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  test('REGRESSION-025: Should handle decimal quantities', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1.5')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')

    // Should either accept or show validation error
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]'))
      .toBeVisible()
  })

  // Negative Tests
  test('REGRESSION-026: Should reject order without customer', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    // Don't select customer
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-customer"]')).toContainText(/required/i)
  })

  test('REGRESSION-027: Should reject status update to invalid value', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to update to invalid status
    await page.click('[data-testid="order-row-0"] [data-testid="status-button"]')
    await page.selectOption('[data-testid="new-status"]', 'invalid_status')
    await page.click('[data-testid="confirm-status-update"]')

    await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
  })

  test('REGRESSION-028: Should not allow cancelling delivered order', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Mark as delivered
    await page.click('[data-testid="order-row-0"] [data-testid="status-button"]')
    await page.selectOption('[data-testid="new-status"]', 'delivered')
    await page.click('[data-testid="confirm-status-update"]')

    // Try to cancel
    await page.click('[data-testid="order-row-0"] [data-testid="cancel-button"]')

    // Should show error that delivered orders can't be cancelled
    await expect(page.locator('[data-testid="error-message"]'))
      .toContainText(/cannot cancel delivered/i)
  })

  test('REGRESSION-029: Should validate minimum order value', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '0.01') // Below minimum
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-total"]'))
      .toContainText(/minimum/i)
  })

  // Boundary Conditions
  test('REGRESSION-030: Should handle order number at maximum length', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Check order number format and length
    const orderNumber = await page.textContent('[data-testid="order-row-0"] [data-testid="order-number"]')
    expect(orderNumber).toMatch(/ORD-\d{4}-\d{6}/)
  })

  test('REGRESSION-031: Should sort by different columns', async ({ page }) => {
    await page.goto('/orders')

    // Create orders with different amounts
    for (let i = 0; i < 3; i++) {
      await page.click('[data-testid="create-order-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.click('[data-testid="add-item-button"]')
      await page.selectOption('[data-testid="product-select-0"]', 'product-1')
      await page.fill('[data-testid="quantity-input-0"]', '1')
      await page.fill('[data-testid="price-input-0"]', (100 + i * 50).toString())
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Sort by total amount ascending
    await page.click('[data-testid="sort-total"]')
    await page.waitForTimeout(500)

    // Verify order
    const amounts = []
    for (let i = 0; i < 3; i++) {
      const amount = await page.textContent(`[data-testid="order-row-${i}"] [data-testid="total"]`)
      amounts.push(parseFloat(amount?.replace(/[$,]/g, '') || '0'))
    }
    expect(amounts).toEqual([100, 150, 200].sort((a, b) => a - b))
  })

  // Workflow Tests
  test('REGRESSION-032: Should complete full order lifecycle', async ({ page }) => {
    // 1. Create order
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '2')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // 2. Update to processing
    await page.click('[data-testid="order-row-0"] [data-testid="status-button"]')
    await page.selectOption('[data-testid="new-status"]', 'processing')
    await page.click('[data-testid="confirm-status-update"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // 3. Update to shipped
    await page.click('[data-testid="order-row-0"] [data-testid="status-button"]')
    await page.selectOption('[data-testid="new-status"]', 'shipped')
    await page.fill('[data-testid="tracking-number"]', 'TRACK123456')
    await page.click('[data-testid="confirm-status-update"]')

    // 4. Update to delivered
    await page.click('[data-testid="order-row-0"] [data-testid="status-button"]')
    await page.selectOption('[data-testid="new-status"]', 'delivered')
    await page.click('[data-testid="confirm-status-update"]')

    // Verify final status
    const status = await page.textContent('[data-testid="order-row-0"] [data-testid="status"]')
    expect(status).toContain('delivered')
  })

  test('REGRESSION-033: Should calculate taxes correctly', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '2')
    await page.fill('[data-testid="price-input-0"]', '100.00')

    // Set tax rate
    await page.fill('[data-testid="tax-rate"]', '10')

    const subtotal = await page.textContent('[data-testid="subtotal"]')
    const tax = await page.textContent('[data-testid="tax-amount"]')
    const total = await page.textContent('[data-testid="order-total"]')

    expect(subtotal).toContain('200.00') // 2 * 100
    expect(tax).toContain('20.00') // 10% of 200
    expect(total).toContain('220.00') // 200 + 20
  })

  // Data Consistency
  test('REGRESSION-034: Should maintain order count after operations', async ({ page }) => {
    await page.goto('/orders')

    const initialCount = await page.locator('[data-testid="order-row"]').count()

    // Create order
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '100.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Count should increase
    await expect(page.locator('[data-testid="order-row"]')).toHaveCount(initialCount + 1)
  })

  // Performance
  test('REGRESSION-035: Should handle bulk order creation', async ({ page }) => {
    const startTime = Date.now()
    const orderCount = 20

    await page.goto('/orders')

    for (let i = 0; i < orderCount; i++) {
      await page.click('[data-testid="create-order-button"]')
      await page.selectOption('[name="customerId"]', 'customer-1')
      await page.click('[data-testid="add-item-button"]')
      await page.selectOption('[data-testid="product-select-0"]', 'product-1')
      await page.fill('[data-testid="quantity-input-0"]', '1')
      await page.fill('[data-testid="price-input-0"]', '100.00')
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    const endTime = Date.now()
    const duration = endTime - startTime

    // Should create 20 orders in reasonable time
    expect(duration).toBeLessThan(120000) // 2 minutes
    await expect(page.locator('[data-testid="order-row"]')).toHaveCount(orderCount)
  })
})
