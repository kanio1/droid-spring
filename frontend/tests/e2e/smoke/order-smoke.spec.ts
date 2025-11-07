/**
 * Order Smoke Tests - Critical Path Tests for Order Management
 */

import { test, expect } from '@playwright/test'

test.describe('Order Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-011: Should display order list page', async ({ page }) => {
    await page.goto('/orders')
    await expect(page.locator('h1')).toContainText(/orders/i)
    await expect(page.locator('[data-testid="order-list"]')).toBeVisible()
  })

  test('SMOKE-012: Should create a new order', async ({ page }) => {
    await page.goto('/orders')

    // Create new order
    await page.click('[data-testid="create-order-button"]')

    // Select customer
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Add line items
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '2')
    await page.fill('[data-testid="price-input-0"]', '100.00')

    // Submit
    await page.click('[data-testid="submit-button"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('SMOKE-013: Should view order details', async ({ page }) => {
    // Create an order first if none exists
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '1')
    await page.fill('[data-testid="price-input-0"]', '50.00')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // View details
    await page.click('[data-testid="order-row-0"] [data-testid="view-button"]')
    await expect(page.locator('[data-testid="order-details"]')).toBeVisible()
    await expect(page.locator('[data-testid="order-items"]')).toBeVisible()
  })

  test('SMOKE-014: Should filter orders by status', async ({ page }) => {
    await page.goto('/orders')

    // Filter by status
    await page.selectOption('[data-testid="status-filter"]', 'pending')
    await page.waitForTimeout(500)

    // Verify filtered results show pending orders
    const rows = page.locator('[data-testid="order-row"]')
    await expect(rows.first()).toBeVisible()
  })

  test('SMOKE-015: Should update order status', async ({ page }) => {
    await page.goto('/orders')

    // Click status update on first order
    await page.click('[data-testid="order-row-0"] [data-testid="status-button"]')
    await page.selectOption('[data-testid="new-status"]', 'delivered')
    await page.click('[data-testid="confirm-status-update"]')

    // Verify update
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('SMOKE-016: Should calculate order total correctly', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Add two items
    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-0"]', 'product-1')
    await page.fill('[data-testid="quantity-input-0"]', '2')
    await page.fill('[data-testid="price-input-0"]', '100.00')

    await page.click('[data-testid="add-item-button"]')
    await page.selectOption('[data-testid="product-select-1"]', 'product-2')
    await page.fill('[data-testid="quantity-input-1"]', '1')
    await page.fill('[data-testid="price-input-1"]', '50.00')

    // Check total calculation
    const total = await page.textContent('[data-testid="order-total"]')
    expect(total).toContain('250.00') // (2 * 100) + (1 * 50)
  })

  test('SMOKE-017: Should handle order with no items', async ({ page }) => {
    await page.goto('/orders')
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[name="customerId"]', 'customer-1')

    // Try to submit without items
    await page.click('[data-testid="submit-button"]')

    // Verify error
    await expect(page.locator('[data-testid="error-items"]')).toContainText(/at least one item/i)
  })

  test('SMOKE-018: Should search orders', async ({ page }) => {
    await page.goto('/orders')

    // Search for order
    await page.fill('[data-testid="search-input"]', 'ORD')
    await page.press('[data-testid="search-input"]', 'Enter')

    // Verify results
    await expect(page.locator('[data-testid="order-list"]')).toBeVisible()
  })

  test('SMOKE-019: Should display order number', async ({ page }) => {
    await page.goto('/orders')

    // Verify order numbers are displayed
    const orderNumber = page.locator('[data-testid="order-row-0"] [data-testid="order-number"]')
    await expect(orderNumber).toBeVisible()
    await expect(orderNumber).toContainText(/ORD-\d+/)
  })

  test('SMOKE-020: Should filter orders by customer', async ({ page }) => {
    await page.goto('/orders')

    // Filter by customer
    await page.selectOption('[data-testid="customer-filter"]', 'customer-1')
    await page.waitForTimeout(500)

    // Verify filtered results
    const rows = page.locator('[data-testid="order-row"]')
    await expect(rows.first()).toBeVisible()
  })
})
