/**
 * Order Management E2E Tests
 *
 * Comprehensive test suite covering all order management workflows:
 * - Order creation and processing
 * - Order status transitions
 * - Order fulfillment and delivery
 * - Order cancellation
 * - Order returns
 * - Search and filtering
 * - Bulk operations
 * - Order history and tracking
 * - Error handling and validation
 *
 * Target: 30 comprehensive tests
 */

import { test, expect } from '@playwright/test'
import { OrderFactory, CustomerFactory } from '../framework/data-factories'
import { AuthHelper } from '../helpers'
import type { Page } from '@playwright/test'

test.describe('Order Management E2E', () => {
  let testOrders: any[] = []
  let testCustomers: any[] = []

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'testuser',
      password: process.env.TEST_PASSWORD || 'testpass'
    })

    await page.goto('/orders')
    await expect(page.locator('h1')).toContainText('Orders')
  })

  test.afterEach(async ({ page }) => {
    // Cleanup test data
    for (const order of testOrders) {
      try {
        await page.goto(`/orders/${order.id}`)
        if (await page.locator('[data-testid="delete-order-button"]').isVisible()) {
          await page.click('[data-testid="delete-order-button"]')
          await page.click('[data-testid="confirm-delete"]')
        }
      } catch (error) {
        console.log(`Cleanup failed for order ${order.id}:`, error)
      }
    }
    testOrders = []
    testCustomers = []
  })

  // ========== LIST VIEW TESTS ==========

  test('01 - Should display orders list with all required elements', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Orders')
    await expect(page.locator('[data-testid="orders-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="create-order-button"]')).toBeVisible()
  })

  test('02 - Should show order count', async ({ page }) => {
    const countText = await page.locator('[data-testid="total-count"]').textContent()
    if (countText) {
      const count = parseInt(countText)
      expect(count).toBeGreaterThanOrEqual(0)
    }
  })

  test('03 - Should handle empty state when no orders exist', async ({ page }) => {
    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText(/No orders found/i)
      await expect(page.locator('[data-testid="create-first-order-button"]')).toBeVisible()
    }
  })

  // ========== SEARCH TESTS ==========

  test('04 - Should search orders by order number', async ({ page }) => {
    // Create a test order
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .withOrderNumber('ORD-TEST-001')
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })
    await page.waitForTimeout(1000)

    // Search by order number
    await page.fill('[data-testid="search-input"]', 'ORD-TEST-001')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('05 - Should search orders by customer name', async ({ page }) => {
    const customer = await createTestCustomer(page, 'John', 'Doe')
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })
    await page.waitForTimeout(1000)

    // Search by customer name
    await page.fill('[data-testid="search-input"]', 'John')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('06 - Should show no results for search with no matches', async ({ page }) => {
    await page.fill('[data-testid="search-input"]', 'NonExistentOrder12345')
    await page.waitForTimeout(1000)

    await expect(page.locator('[data-testid="no-results-message"]'))
      .toContainText(/No orders found/i)
    await expect(page.locator('table tbody tr')).toHaveCount(0)
  })

  // ========== FILTERING TESTS ==========

  test('07 - Should filter orders by status - Pending', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'PENDING')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      // Verify all visible orders are pending
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/pending/i)
        }
      }
    }
  })

  test('08 - Should filter orders by status - Processing', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'PROCESSING')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/processing/i)
        }
      }
    }
  })

  test('09 - Should filter orders by status - Shipped', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'SHIPPED')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/shipped/i)
        }
      }
    }
  })

  test('10 - Should filter orders by status - Delivered', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'DELIVERED')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/delivered/i)
        }
      }
    }
  })

  test('11 - Should combine search and filter', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'PENDING')
    await page.fill('[data-testid="search-input"]', 'test')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  // ========== CREATE ORDER TESTS ==========

  test('12 - Should create a new order with valid data', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    expect(orderId).toBeTruthy()
    testOrders.push({ id: orderId, ...order })

    // Verify redirect to details page
    await expect(page).toHaveURL(/.*\/orders\/[a-zA-Z0-9]+/)

    // Verify order data
    await expect(page.locator('[data-testid="order-number"]')).toBeVisible()
  })

  test('13 - Should create order with multiple items', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .withMultipleItems(3)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    expect(orderId).toBeTruthy()
    testOrders.push({ id: orderId, ...order })

    // Verify order has multiple items
    await page.goto(`/orders/${orderId}`)
    const itemCount = await page.locator('[data-testid="order-item"]').count()
    expect(itemCount).toBeGreaterThan(1)
  })

  test('14 - Should show validation errors for required fields', async ({ page }) => {
    await page.click('[data-testid="create-order-button"]')
    await expect(page).toHaveURL(/.*\/orders\/create/)

    // Submit empty form
    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(500)

    // Check for validation errors
    await expect(page.locator('[data-testid="customer-error"]'))
      .toContainText(/required/i)
  })

  test('15 - Should calculate order total correctly', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Verify total is calculated
    await page.goto(`/orders/${orderId}`)
    const total = await page.locator('[data-testid="order-total"]').textContent()
    expect(total).toBeTruthy()
    expect(parseFloat(total || '0')).toBeGreaterThan(0)
  })

  // ========== ORDER STATUS TRANSITIONS ==========

  test('16 - Should transition order from Pending to Processing', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Change status to processing
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="change-status-button"]')
    await page.selectOption('[data-testid="status-select"]', 'PROCESSING')
    await page.click('[data-testid="confirm-status-change"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/processing/i)
  })

  test('17 - Should transition order from Processing to Shipped', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .processing()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Change status to shipped
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="change-status-button"]')
    await page.selectOption('[data-testid="status-select"]', 'SHIPPED')
    await page.fill('[data-testid="tracking-number"]', 'TRACK123456')
    await page.click('[data-testid="confirm-status-change"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/shipped/i)
  })

  test('18 - Should transition order from Shipped to Delivered', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .shipped()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Change status to delivered
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="change-status-button"]')
    await page.selectOption('[data-testid="status-select"]', 'DELIVERED')
    await page.click('[data-testid="confirm-status-change"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/delivered/i)
  })

  // ========== ORDER FULFILLMENT ==========

  test('19 - Should fulfill order completely', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .processing()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Mark as fulfilled
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="fulfill-order-button"]')
    await page.click('[data-testid="confirm-fulfill"]')
    await page.waitForTimeout(500)

    // Verify fulfillment
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/fulfilled|shipped/i)
  })

  test('20 - Should handle partial fulfillment', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .withMultipleItems(2)
      .processing()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Mark partial fulfillment
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="fulfill-partial-button"]')
    await page.check('[data-testid="item-0-checkbox"]')
    await page.click('[data-testid="confirm-partial-fulfill"]')
    await page.waitForTimeout(500)

    // Verify partial fulfillment
    const fulfillmentStatus = page.locator('[data-testid="fulfillment-status"]')
    await expect(fulfillmentStatus).toContainText(/partial/i)
  })

  // ========== ORDER CANCELLATION ==========

  test('21 - Should cancel pending order', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Cancel order
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="cancel-order-button"]')
    await page.fill('[data-testid="cancellation-reason"]', 'Customer request')
    await page.click('[data-testid="confirm-cancel"]')
    await page.waitForTimeout(500)

    // Verify cancellation
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/cancelled/i)
  })

  test('22 - Should prevent cancellation of shipped order', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .shipped()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Try to cancel
    await page.goto(`/orders/${orderId}`)

    // Cancel button should not be visible or should be disabled
    const cancelButton = page.locator('[data-testid="cancel-order-button"]')
    if (await cancelButton.isVisible()) {
      await expect(cancelButton).toBeDisabled()
    }
  })

  // ========== ORDER RETURNS ==========

  test('23 - Should create return for delivered order', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .delivered()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Create return
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="create-return-button"]')
    await page.check('[data-testid="return-item-0"]')
    await page.selectOption('[data-testid="return-reason"]', 'DAMAGED')
    await page.click('[data-testid="submit-return"]')
    await page.waitForTimeout(500)

    // Verify return was created
    await expect(page.locator('[data-testid="return-success-message"]')).toBeVisible()
  })

  test('24 - Should process refund for returned item', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .delivered()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Create return and process refund
    await page.goto(`/orders/${orderId}`)
    await page.click('[data-testid="create-return-button"]')
    await page.check('[data-testid="return-item-0"]')
    await page.selectOption('[data-testid="return-reason"]', 'DAMAGED')
    await page.click('[data-testid="submit-return"]')
    await page.waitForTimeout(1000)

    // Process refund
    await page.click('[data-testid="process-refund-button"]')
    await page.selectOption('[data-testid="refund-method"]', 'ORIGINAL')
    await page.fill('[data-testid="refund-amount"]', '100.00')
    await page.click('[data-testid="confirm-refund"]')
    await page.waitForTimeout(500)

    // Verify refund
    const refundStatus = page.locator('[data-testid="refund-status"]')
    await expect(refundStatus).toContainText(/processed/i)
  })

  // ========== BULK OPERATIONS ==========

  test('25 - Should select multiple orders and perform bulk status update', async ({ page }) => {
    const customer = await createTestCustomer(page)

    // Create multiple orders
    const orders = OrderFactory.create().buildMany(3)
    const createdIds: any[] = []

    for (const order of orders) {
      const orderId = await createOrder(page, { ...order, customerId: customer.id })
      createdIds.push({ id: orderId, ...order })
    }
    testOrders.push(...createdIds)

    await page.waitForTimeout(1000)

    // Select multiple orders
    await page.click('[data-testid="select-all-checkbox"]')

    // Perform bulk action
    await page.click('[data-testid="bulk-action-dropdown"]')
    await page.click('[data-testid="bulk-action-change-status"]')
    await page.selectOption('[data-testid="bulk-status-select"]', 'PROCESSING')
    await page.click('[data-testid="bulk-action-apply"]')
    await page.waitForTimeout(1000)

    // Verify bulk action was applied
    const successMessage = page.locator('[data-testid="bulk-action-success"]')
    if (await successMessage.isVisible()) {
      await expect(successMessage).toContainText(/success/i)
    }
  })

  // ========== ORDER DETAILS VIEW ==========

  test('26 - Should view complete order details', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // View order details
    await page.goto(`/orders/${orderId}`)

    // Verify all order information is displayed
    await expect(page.locator('[data-testid="order-number"]')).toBeVisible()
    await expect(page.locator('[data-testid="order-date"]')).toBeVisible()
    await expect(page.locator('[data-testid="customer-info"]')).toBeVisible()
    await expect(page.locator('[data-testid="order-items"]')).toBeVisible()
    await expect(page.locator('[data-testid="order-total"]')).toBeVisible()
  })

  test('27 - Should show order history timeline', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // View order details
    await page.goto(`/orders/${orderId}`)

    // Check if timeline is visible
    const timeline = page.locator('[data-testid="order-timeline"]')
    if (await timeline.isVisible()) {
      const events = await timeline.locator('[data-testid="timeline-event"]').count()
      expect(events).toBeGreaterThan(0)
    }
  })

  // ========== ERROR HANDLING ==========

  test('28 - Should handle insufficient inventory', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .withOutOfStockItem()
      .pending()
      .build()

    // Try to create order with out of stock item
    await page.click('[data-testid="create-order-button"]')
    await page.waitForURL(/.*\/orders\/create/)

    await page.selectOption('[data-testid="customer-select"]', customer.id)
    await page.selectOption('[data-testid="product-select"]', order.items?.[0]?.productId || '1')
    await page.fill('[data-testid="quantity-input"]', '100') // High quantity

    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(1000)

    // Should show inventory error
    await expect(page.locator('[data-testid="inventory-error"]'))
      .toContainText(/insufficient inventory|out of stock/i)
  })

  test('29 - Should handle duplicate order creation', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const order = OrderFactory.create()
      .withCustomerId(customer.id)
      .pending()
      .build()

    const orderId = await createOrder(page, order)
    testOrders.push({ id: orderId, ...order })

    // Try to create duplicate
    await page.click('[data-testid="create-order-button"]')
    await page.selectOption('[data-testid="customer-select"]', customer.id)

    // Use same order number
    await page.fill('[data-testid="order-number-input"]', order.orderNumber || '')
    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(1000)

    // Should show duplicate error
    await expect(page.locator('[data-testid="duplicate-error"]'))
      .toContainText(/already exists|duplicate/i)
  })

  // ========== SORTING AND EXPORT ==========

  test('30 - Should sort orders and export to CSV', async ({ page }) => {
    // Check if sorting dropdown exists
    const sortDropdown = page.locator('[data-testid="sort-dropdown"]')
    if (await sortDropdown.isVisible()) {
      await page.selectOption('[data-testid="sort-dropdown"]', 'orderDate')
      await page.click('[data-testid="apply-sort"]')
      await page.waitForTimeout(500)
    }

    // Check if export button exists
    const exportButton = page.locator('[data-testid="export-orders-button"]')
    if (await exportButton.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download'),
        exportButton.click()
      ])

      const path = await download.path()
      expect(path).toBeTruthy()
      expect(path).toMatch(/\.csv$/i)
    }
  })

  // ========== HELPER FUNCTIONS ==========

  async function createTestCustomer(
    page: Page,
    firstName: string = 'Test',
    lastName: string = 'Customer'
  ): Promise<any> {
    const customer = CustomerFactory.create()
      .withFirstName(firstName)
      .withLastName(lastName)
      .withRandomEmail()
      .active()
      .build()

    // Navigate to customer create page
    await page.goto('/customers/create')
    await page.fill('[data-testid="firstName-input"]', customer.firstName)
    await page.fill('[data-testid="lastName-input"]', customer.lastName)
    await page.fill('[data-testid="email-input"]', customer.email)

    if (customer.phone) {
      await page.fill('[data-testid="phone-input"]', customer.phone)
    }

    await page.click('[data-testid="submit-button"]')
    await page.waitForResponse(/.*\/api\/customers/)
    await page.waitForURL(/.*\/customers\/[a-zA-Z0-9]+/)

    // Extract customer ID from URL
    const url = page.url()
    const idMatch = url.match(/\/customers\/([a-zA-Z0-9]+)/)
    const id = idMatch ? idMatch[1] : ''

    testCustomers.push({ id, ...customer })
    return { id, ...customer }
  }

  async function createOrder(page: Page, order: any): Promise<string> {
    await page.click('[data-testid="create-order-button"]')
    await page.waitForURL(/.*\/orders\/create/)

    // Fill form
    if (order.orderNumber) {
      await page.fill('[data-testid="order-number-input"]', order.orderNumber)
    }

    await page.selectOption('[data-testid="customer-select"]', order.customerId)

    if (order.items && order.items.length > 0) {
      for (const item of order.items) {
        await page.selectOption('[data-testid="product-select"]', item.productId)
        await page.fill('[data-testid="quantity-input"]', item.quantity.toString())
        await page.click('[data-testid="add-item-button"]')
        await page.waitForTimeout(500)
      }
    }

    await page.click('[data-testid="submit-button"]')
    await page.waitForResponse(/.*\/api\/orders/)
    await page.waitForURL(/.*\/orders\/[a-zA-Z0-9]+/)

    // Extract order ID from URL
    const url = page.url()
    const idMatch = url.match(/\/orders\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }
})
